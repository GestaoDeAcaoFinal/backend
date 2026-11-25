package com.av2.gestaodeacao.cliente.support;

import com.av2.gestaodeacao.exceptions.LimiteDeConsultasException;
import com.av2.gestaodeacao.exceptions.AguardarCotaException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.ArrayDeque;
import java.util.Deque;

/** Orçamento por instância do backend, compartilhado por quotes e câmbio. */
@Component
public class TwelveDataLimitador {
    @FunctionalInterface interface Pausa { void aguardar(long nanos) throws InterruptedException; }
    private final ReentrantLock lock = new ReentrantLock(true);
    private final int creditos;
    private final long janelaNanos;
    private final Deque<Long> consultas = new ArrayDeque<>();
    private final long esperaMaximaMillis;
    private final LongSupplier relogio;
    private final Pausa pausa;
    private boolean emCooldown;
    private long fimCooldown;

    @Autowired
    public TwelveDataLimitador(
            @Value("${integracoes.twelvedata.creditos-por-janela:8}") int creditos,
            @Value("${integracoes.twelvedata.janela-ms:60000}") long janelaMillis,
            @Value("${integracoes.twelvedata.margem-ms:100}") long margemMillis,
            @Value("${integracoes.twelvedata.espera-maxima-ms:15000}") long esperaMaximaMillis) {
        this(creditos, janelaMillis, margemMillis, esperaMaximaMillis, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    TwelveDataLimitador(int creditos, long janelaMillis, long margemMillis, long esperaMaximaMillis, LongSupplier relogio, Pausa pausa) {
        if (creditos < 1 || creditos > 1_000_000 || janelaMillis < 1 || janelaMillis > 86_400_000 || margemMillis < 0 || margemMillis > janelaMillis || esperaMaximaMillis < 1 || esperaMaximaMillis > 60_000) throw new IllegalArgumentException("Configuração de limite Twelve Data inválida");
        this.creditos = creditos;
        this.janelaNanos = TimeUnit.MILLISECONDS.toNanos(janelaMillis + margemMillis);
        this.esperaMaximaMillis = esperaMaximaMillis;
        this.relogio = relogio; this.pausa = pausa;
    }

    public <T> T executar(Supplier<T> chamada) {
        boolean adquirido = false;
        try {
            adquirido = lock.tryLock(esperaMaximaMillis, TimeUnit.MILLISECONDS);
            if (!adquirido) throw new ProvedorIndisponivelException("Twelve Data", "Há consultas em andamento. Aguarde para tentar novamente.");
            long agora = relogio.getAsLong();
            if (emCooldown && fimCooldown - agora > 0) throw new LimiteDeConsultasException("Twelve Data", Math.max(1, TimeUnit.NANOSECONDS.toSeconds(fimCooldown - agora) + 1));
            emCooldown = false;
            removerExpiradas(agora);
            if (consultas.size() >= creditos) {
                long restante = janelaNanos - (agora - consultas.getFirst());
                if (restante > TimeUnit.MILLISECONDS.toNanos(esperaMaximaMillis)) {
                    throw new AguardarCotaException((restante + 999_999_999L) / 1_000_000_000L);
                }
                while (consultas.size() >= creditos) {
                    pausa.aguardar(janelaNanos - (agora - consultas.getFirst()));
                    agora = relogio.getAsLong();
                    removerExpiradas(agora);
                }
            }
            // Falhas também gastam crédito: a reserva acontece antes da chamada HTTP.
            consultas.addLast(agora);
            try { return chamada.get(); }
            catch (LimiteDeConsultasException e) { emCooldown = true; fimCooldown = relogio.getAsLong() + TimeUnit.SECONDS.toNanos(e.getTentarAposSegundos()); throw e; }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProvedorIndisponivelException("Twelve Data");
        } finally { if (adquirido) lock.unlock(); }
    }

    public long getEsperaMaximaMillis() { return esperaMaximaMillis; }

    private void removerExpiradas(long agora) {
        while (!consultas.isEmpty() && agora - consultas.getFirst() >= janelaNanos) consultas.removeFirst();
    }
}
