package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.cliente.cvm.CadastroCvmGateway;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroCvmCacheTest {

    @ParameterizedTest
    @MethodSource("ttlsInvalidos")
    void rejeitaTtlNuloZeroOuNegativoAntesDeBaixarCadastro(Duration ttl) {
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            downloads.incrementAndGet();
            return zipAtivo("12.345.678/0001-90");
        };

        assertThatThrownBy(() -> new CadastroCvmCache(gateway, new CadastroCvmParser(), ttl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("O TTL do cadastro CVM deve ser positivo");
        assertThat(downloads).hasValue(0);
    }

    private static Stream<Duration> ttlsInvalidos() {
        return Stream.of(null, Duration.ZERO, Duration.ofNanos(-1));
    }

    @Test
    void conservaSnapshotAteUmNanossegundoAntesDoTtlEExpiraNoLimite() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> downloads.incrementAndGet() == 1
                ? zipAtivo("12.345.678/0001-90") : zipAtivo("98.765.432/0001-10");
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofSeconds(1), clock);
        Set<String> primeiro = cache.intermediariosAtivos();

        clock.avancar(Duration.ofSeconds(1).minusNanos(1));
        assertThat(cache.intermediariosAtivos()).isSameAs(primeiro);
        assertThat(downloads).hasValue(1);

        clock.avancar(Duration.ofNanos(1));
        assertThat(cache.intermediariosAtivos()).containsExactly("98765432000110");
        assertThat(downloads).hasValue(2);
        assertThat(primeiro).containsExactly("12345678000190");
    }

    @Test
    void recuperaPrimeiraCargaAposFalhaSemGuardarResultadoInvalido() {
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            if (downloads.incrementAndGet() == 1) {
                throw new IllegalStateException("detalhe-interno");
            }
            return zipAtivo("12.345.678/0001-90");
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isExactlyInstanceOf(ProvedorIndisponivelException.class)
                .hasMessage("O provedor CVM está indisponível")
                .hasMessageNotContaining("detalhe-interno");
        Set<String> recuperado = cache.intermediariosAtivos();
        assertThat(recuperado).containsExactly("12345678000190");
        assertThat(cache.intermediariosAtivos()).isSameAs(recuperado);
        assertThat(downloads).hasValue(2);
    }

    @Test
    void falhaDeParsingAposExpiracaoNaoRetornaCadastroAntigoEPermiteRecuperacao() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> switch (downloads.incrementAndGet()) {
            case 1 -> zipAtivo("12.345.678/0001-90");
            case 2 -> new byte[]{1, 2, 3};
            default -> zipAtivo("98.765.432/0001-10");
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24), clock);
        assertThat(cache.intermediarioAtivo("12.345.678/0001-90")).isTrue();
        clock.avancar(Duration.ofHours(24));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isExactlyInstanceOf(ProvedorIndisponivelException.class)
                .hasMessage("O provedor CVM está indisponível")
                .hasCauseInstanceOf(com.av2.gestaodeacao.exceptions.FalhaDeProvedorException.class);
        assertThat(cache.intermediariosAtivos()).containsExactly("98765432000110");
        assertThat(cache.intermediarioAtivo("12.345.678/0001-90")).isFalse();
        assertThat(cache.intermediarioAtivo("98.765.432/0001-10")).isTrue();
        assertThat(downloads).hasValue(3);
    }

    @Test
    void consultasConcorrentesCompartilhamUmaUnicaCargaEOMesmoSnapshot() throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            downloads.incrementAndGet();
            return zipAtivo("12.345.678/0001-90");
        };
        CadastroCvmCache cache = new CadastroCvmCache(gateway, new CadastroCvmParser(),
                Duration.ofHours(24), Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC));
        FutureTask<Set<String>> consulta1 = new FutureTask<>(cache::intermediariosAtivos);
        FutureTask<Set<String>> consulta2 = new FutureTask<>(cache::intermediariosAtivos);
        Thread leitor1 = new Thread(consulta1, "cvm-leitor-1");
        Thread leitor2 = new Thread(consulta2, "cvm-leitor-2");
        leitor1.setDaemon(true);
        leitor2.setDaemon(true);

        try {
            // Ambos passam pela verificação externa antes de disputar a carga sincronizada.
            synchronized (cache) {
                leitor1.start();
                leitor2.start();
                aguardarBloqueio(leitor1);
                aguardarBloqueio(leitor2);
                assertThat(downloads).hasValue(0);
            }
            Set<String> primeiro = consulta1.get(5, TimeUnit.SECONDS);
            Set<String> segundo = consulta2.get(5, TimeUnit.SECONDS);

            assertThat(primeiro).containsExactly("12345678000190");
            assertThat(segundo).isSameAs(primeiro);
            assertThat(cache.intermediariosAtivos()).isSameAs(primeiro);
            assertThat(downloads).hasValue(1);
            assertThatThrownBy(() -> segundo.add("98765432000110"))
                    .isInstanceOf(UnsupportedOperationException.class);
        } finally {
            leitor1.interrupt();
            leitor2.interrupt();
            leitor1.join(5_000);
            leitor2.join(5_000);
            assertThat(leitor1.isAlive()).isFalse();
            assertThat(leitor2.isAlive()).isFalse();
        }
    }

    private static void aguardarBloqueio(Thread leitor) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (leitor.getState() != Thread.State.BLOCKED && leitor.isAlive() && System.nanoTime() < limite) {
            Thread.yield();
        }
        assertThat(leitor.getState()).isEqualTo(Thread.State.BLOCKED);
    }

    @Test
    void reutilizaSnapshotImutavelAntesDoTtlEAtualizaNoVencimento() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        Queue<byte[]> respostas = new ArrayDeque<>();
        respostas.add(zipAtivo("12.345.678/0001-90"));
        respostas.add(zipAtivo("98.765.432/0001-10"));
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            downloads.incrementAndGet();
            return respostas.remove();
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24), clock);

        Set<String> primeiro = cache.intermediariosAtivos();
        assertThat(cache.intermediariosAtivos()).isSameAs(primeiro);
        assertThat(downloads).hasValue(1);
        assertThat(primeiro).containsExactly("12345678000190");
        assertThatThrownBy(() -> primeiro.add("11111111000111"))
                .isInstanceOf(UnsupportedOperationException.class);

        clock.avancar(Duration.ofHours(24));

        assertThat(cache.intermediariosAtivos()).containsExactly("98765432000110");
        assertThat(downloads).hasValue(2);
    }

    @Test
    void falhaFechadoSemSnapshotValidoInclusiveQuandoOAnteriorExpirou() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        AtomicInteger chamada = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            if (chamada.getAndIncrement() == 0) {
                return zipAtivo("12.345.678/0001-90");
            }
            throw new IllegalStateException("URL e conteúdo interno não devem vazar");
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24), clock);
        assertThat(cache.intermediarioAtivo("12.345.678/0001-90")).isTrue();

        clock.avancar(Duration.ofHours(24).plusSeconds(1));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("URL")
                .hasMessageNotContaining("conteúdo");
    }

    @Test
    void primeiraCargaComFalhaTambemResultaEmIndisponibilidade() {
        CadastroCvmGateway gateway = () -> { throw new IllegalStateException("segredo"); };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24),
                Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("segredo");
    }

    private static byte[] zipAtivo(String cnpj) {
        String csv = "CNPJ;SIT\n" + cnpj + ";EM FUNCIONAMENTO NORMAL\n";
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
                zip.write(csv.getBytes(Charset.forName("windows-1252")));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) { this.instant = instant; }
        private void avancar(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
