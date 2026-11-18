package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.cliente.support.TwelveDataLimitador;
import com.av2.gestaodeacao.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TwelveDataCacheTest {
    @Test
    void esperaDeCambioPodeSerInterrompidaDuranteFalhaConcorrente() throws Exception {
        CountDownLatch providerEntered = new CountDownLatch(1), releaseProvider = new CountDownLatch(1), finished = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean interrupted = new java.util.concurrent.atomic.AtomicBoolean();
        server.expect(requestTo(FX)).andRespond(request -> {
            providerEntered.countDown();
            try { releaseProvider.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return withServerError().createResponse(request);
        });
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> { try { cliente.buscarTaxaCambio("USD", "BRL"); } catch (RuntimeException ignored) { } });
        Thread second = new Thread(() -> {
            try { cliente.buscarTaxaCambio("USD", "BRL"); }
            catch (Throwable e) { error.set(e); interrupted.set(Thread.currentThread().isInterrupted()); }
            finally { finished.countDown(); }
        });
        try {
            assertThat(providerEntered.await(2, TimeUnit.SECONDS)).isTrue();
            second.start(); second.interrupt();
            assertThat(finished.await(2, TimeUnit.SECONDS)).as("Espera de câmbio interrompível sem nova chamada externa").isTrue();
            assertThat(error.get()).isInstanceOf(ProvedorIndisponivelException.class);
            assertThat(interrupted.get()).isTrue();
        } finally { releaseProvider.countDown(); first.get(2, TimeUnit.SECONDS); second.join(2000); pool.shutdownNow(); }
        server.verify();
    }

    @Test
    void esperaDeCambioExpiraSemEntrarEmFilaIlimitada() throws Exception {
        CountDownLatch providerEntered = new CountDownLatch(1), releaseProvider = new CountDownLatch(1);
        server.expect(requestTo(FX)).andRespond(request -> {
            providerEntered.countDown();
            try { releaseProvider.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return withServerError().createResponse(request);
        });
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> { try { cliente.buscarTaxaCambio("USD", "BRL"); } catch (RuntimeException ignored) { } });
        try {
            assertThat(providerEntered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOf(ProvedorIndisponivelException.class);
            assertThat(releaseProvider.getCount()).isEqualTo(1);
        } finally { releaseProvider.countDown(); first.get(2, TimeUnit.SECONDS); pool.shutdownNow(); }
        server.verify();
    }
    private final AtomicLong now = new AtomicLong();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TwelveDataCliente cliente = new TwelveDataCliente(builder, new ProvedorHttpExecutor(), "https://twelve.test", "test-key", new TwelveDataLimitador(8, 8, 0, 1000), 300, now::get);
    private static final String FX = "https://twelve.test/exchange_rate?symbol=USD/BRL&apikey=test-key";

    @Test
    void cacheExpiraEmCincoMinutosSemFallbackVencidoEmFalha() {
        server.expect(requestTo(FX)).andRespond(withSuccess("{\"rate\":5.25}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(FX)).andRespond(withServerError());
        assertThat(cliente.buscarTaxaCambio("usd", "brl")).isEqualByComparingTo("5.25");
        now.set(TimeUnit.SECONDS.toNanos(299));
        assertThat(cliente.buscarTaxaCambio("USD", "BRL")).isEqualByComparingTo("5.25");
        now.set(TimeUnit.SECONDS.toNanos(300));
        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOf(FalhaDeProvedorException.class);
        server.verify();
    }

    @Test
    void respostaInvalidaNaoEntraNoCache() {
        server.expect(requestTo(FX)).andRespond(withSuccess("{\"rate\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(FX)).andRespond(withSuccess("{\"rate\":5.5}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOf(FalhaDeProvedorException.class);
        assertThat(cliente.buscarTaxaCambio("USD", "BRL")).isEqualByComparingTo("5.5");
        server.verify();
    }

    @Test
    void limiteDeCambioTambemBloqueiaQuotesSemNovaChamada() {
        server.expect(requestTo(FX)).andRespond(withSuccess("{\"code\":429,\"status\":\"error\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOf(LimiteDeConsultasException.class);
        assertThatThrownBy(() -> cliente.buscarAcao("AAPL")).isInstanceOf(LimiteDeConsultasException.class);
        server.verify();
    }

    @Test
    void leiturasConcorrentesDoMesmoParCompartilhamUmaResposta() throws Exception {
        server.expect(requestTo(FX)).andRespond(withSuccess("{\"rate\":5.25}", MediaType.APPLICATION_JSON));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<java.math.BigDecimal> a = pool.submit(() -> cliente.buscarTaxaCambio("USD", "BRL"));
            Future<java.math.BigDecimal> b = pool.submit(() -> cliente.buscarTaxaCambio("USD", "BRL"));
            assertThat(a.get(2, TimeUnit.SECONDS)).isEqualByComparingTo("5.25");
            assertThat(b.get(2, TimeUnit.SECONDS)).isEqualByComparingTo("5.25");
        } finally { pool.shutdownNow(); }
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500})
    void codigoJsonDeAcessoOuFalhaNaoEhTickerInexistente(int code) {
        server.expect(requestTo("https://twelve.test/quote?symbol=AAPL&apikey=test-key"))
                .andRespond(withSuccess("{\"code\":" + code + ",\"status\":\"error\",\"message\":\"secret-test\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.buscarAcao("AAPL")).isInstanceOf(FalhaDeProvedorException.class).hasMessageNotContaining("secret-test");
        server.verify();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"5,5", "invalid,60", "0,1", "'Wed, 01 Jan 2020 00:00:00 GMT',1"})
    void respeitaRetryAfterSeguroSemExporCorpo(String retryAfter, long esperado) {
        server.expect(requestTo(FX)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter));
        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOfSatisfying(LimiteDeConsultasException.class, e -> assertThat(e.getTentarAposSegundos()).isEqualTo(esperado));
        server.verify();
    }

    @Test
    void retryAfterDataFuturaNaoEhTrocadoPorUmMinutoFixo() {
        String future = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(10).format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
        server.expect(requestTo(FX)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", future));
        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL")).isInstanceOfSatisfying(LimiteDeConsultasException.class, e -> assertThat(e.getTentarAposSegundos()).isBetween(595L, 601L));
        server.verify();
    }
}
