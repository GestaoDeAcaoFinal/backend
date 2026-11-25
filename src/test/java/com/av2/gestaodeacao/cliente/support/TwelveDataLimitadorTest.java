package com.av2.gestaodeacao.cliente.support;

import com.av2.gestaodeacao.exceptions.LimiteDeConsultasException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;

class TwelveDataLimitadorTest {
    @Test
    void consultaConcorrenteExpiraSemExecutarProvedorETravaPodeSerReutilizada() throws Exception {
        AtomicLong now = new AtomicLong();
        TwelveDataLimitador gate = new TwelveDataLimitador(8, 8, 0, 200, now::get, now::addAndGet);
        CountDownLatch entrouNoProvedor = new CountDownLatch(1);
        CountDownLatch liberarProvedor = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean segundaConsultaExecutada =
                new java.util.concurrent.atomic.AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> primeira = pool.submit(() -> gate.executar(() -> {
            entrouNoProvedor.countDown();
            try {
                if (!liberarProvedor.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Primeiro provedor não foi liberado");
                }
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Primeiro provedor interrompido", erro);
            }
            return 1;
        }));
        try {
            assertThat(entrouNoProvedor.await(3, TimeUnit.SECONDS)).isTrue();
            Future<Integer> segunda = pool.submit(() -> gate.executar(() -> {
                segundaConsultaExecutada.set(true);
                return 2;
            }));

            assertThatThrownBy(() -> segunda.get(3, TimeUnit.SECONDS))
                    .isInstanceOfSatisfying(ExecutionException.class, erro ->
                            assertThat(erro.getCause())
                                    .isExactlyInstanceOf(ProvedorIndisponivelException.class)
                                    .hasMessageContaining("consultas em andamento"));
            assertThat(segundaConsultaExecutada.get()).isFalse();
            assertThat(primeira.isDone()).isFalse();
            assertThat(liberarProvedor.getCount()).isEqualTo(1);

            liberarProvedor.countDown();
            assertThat(primeira.get(3, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(gate.executar(() -> 42)).isEqualTo(42);
        } finally {
            liberarProvedor.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void esperaPelaCotaTambemEhLimitada() {
        AtomicLong now = new AtomicLong();
        TwelveDataLimitador gate = new TwelveDataLimitador(1, 60_000, 100, 1000, now::get, now::addAndGet);
        assertThat(gate.executar(() -> 1)).isEqualTo(1);
        assertThatThrownBy(() -> gate.executar(() -> 2)).isInstanceOf(ProvedorIndisponivelException.class);
        assertThat(now.get()).isZero();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,60000,100,1000", "1000001,60000,100,1000", "8,0,0,1000", "8,86400001,0,1000", "8,60000,-1,1000", "8,60000,60001,1000", "8,60000,100,0", "8,60000,100,60001"})
    void configuracaoInvalidaFalhaAntesDeLiberarConsultas(int credits, long window, long margin, long wait) {
        assertThatThrownBy(() -> new TwelveDataLimitador(credits, window, margin, wait)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void noveCotacoesMaisCambioCompartilhamOitoCreditosSemEstouroDaJanela() {
        AtomicLong now = new AtomicLong();
        TwelveDataLimitador gate = new TwelveDataLimitador(8, 60_000, 100, 15000, now::get, now::addAndGet);
        List<Long> starts = new ArrayList<>();
        for (int i = 0; i < 10; i++) gate.executar(() -> { starts.add(now.get()); return 1; });
        assertThat(starts).hasSize(10);
        assertThat(starts.get(8)).isEqualTo(TimeUnit.MILLISECONDS.toNanos(60_800));
        for (long end : starts) assertThat(starts.stream().filter(t -> t > end - TimeUnit.MINUTES.toNanos(1) && t <= end).count()).isLessThanOrEqualTo(8);
    }

    @Test
    void cooldownImpedeNovaChamadaSemDormirAteExpirar() {
        AtomicLong now = new AtomicLong();
        TwelveDataLimitador gate = new TwelveDataLimitador(8, 60_000, 100, 15000, now::get, now::addAndGet);
        assertThatThrownBy(() -> gate.executar(() -> { throw new LimiteDeConsultasException("Twelve Data", 60); })).isInstanceOf(LimiteDeConsultasException.class);
        assertThatThrownBy(() -> gate.executar(() -> { fail("Não deveria consultar o provedor em cooldown"); return 0; })).isInstanceOf(LimiteDeConsultasException.class);
        assertThat(now.get()).isZero();
        now.addAndGet(TimeUnit.SECONDS.toNanos(60));
        assertThat(gate.executar(() -> 42)).isEqualTo(42);
    }

    @Test
    void concorrenciaUsaMesmoOrcamento() throws Exception {
        AtomicLong now = new AtomicLong();
        TwelveDataLimitador gate = new TwelveDataLimitador(8, 60_000, 100, 15000, now::get, now::addAndGet);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Long> starts = new CopyOnWriteArrayList<>();
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) futures.add(pool.submit(() -> gate.executar(() -> { starts.add(now.get()); return 1; })));
            for (Future<Integer> future : futures) assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(starts).hasSize(12);
            for (int i = 1; i < starts.size(); i++) assertThat(starts.get(i) - starts.get(i - 1)).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(7600));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void preservaInterrupcaoSemFazerConsulta() {
        TwelveDataLimitador gate = new TwelveDataLimitador(8, 60_000, 100, 1000);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> gate.executar(() -> { fail("Não deve consultar quando interrompido"); return 0; })).isInstanceOf(ProvedorIndisponivelException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
}
