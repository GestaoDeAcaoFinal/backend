package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.TwelveDataExchangeRateResponse;
import com.av2.gestaodeacao.cliente.dto.TwelveDataResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.cliente.support.TwelveDataLimitador;
import com.av2.gestaodeacao.exceptions.LimiteDeConsultasException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TwelveDataCliente {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;
    private final String apiKey;
    private final TwelveDataLimitador limitador;
    private final long cacheNanos;
    private final LongSupplier relogio;
    private final Map<String, Cambio> cambios = new HashMap<>();
    private final ReentrantLock cambioLock = new ReentrantLock(true);
    private record Cambio(BigDecimal taxa, long consultadoEm) { }

    @Autowired
    public TwelveDataCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.twelvedata.base-url}") String baseUrl,
            @Value("${twelvedata.api.key}") String apiKey,
            TwelveDataLimitador limitador,
            @Value("${integracoes.twelvedata.cambio-cache-segundos:300}") long cacheSegundos
    ) {
        this(builder, executor, baseUrl, apiKey, limitador, cacheSegundos, System::nanoTime);
    }

    TwelveDataCliente(RestClient.Builder builder, ProvedorHttpExecutor executor, String baseUrl, String apiKey, TwelveDataLimitador limitador, long cacheSegundos, LongSupplier relogio) {
        if (cacheSegundos < 0 || cacheSegundos > 3600) throw new IllegalArgumentException("Cache de câmbio deve estar entre 0 e 3600 segundos");
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
        this.apiKey = apiKey;
        this.limitador = limitador;
        this.cacheNanos = TimeUnit.SECONDS.toNanos(cacheSegundos);
        this.relogio = relogio;
    }

    public TwelveDataResponse buscarAcao(String ticker) {
        return limitador.executar(() -> executor.executar("Twelve Data", () -> validar(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(TwelveDataResponse.class))));
    }

    public BigDecimal buscarTaxaCambio(String moedaOrigem, String moedaDestino) {
        boolean adquirido = false;
        try {
            adquirido = cambioLock.tryLock(limitador.getEsperaMaximaMillis(), TimeUnit.MILLISECONDS);
            if (!adquirido) throw new ProvedorIndisponivelException("Twelve Data", "Há uma consulta de câmbio em andamento. Aguarde para tentar novamente.");
            String par = moedaOrigem.trim().toUpperCase(Locale.ROOT) + "/" + moedaDestino.trim().toUpperCase(Locale.ROOT);
            Cambio anterior = cambios.get(par);
            if (anterior != null && relogio.getAsLong() - anterior.consultadoEm() < cacheNanos) return anterior.taxa();
            BigDecimal taxa = limitador.executar(() -> executor.executar("Twelve Data", () -> validarTaxa(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/exchange_rate")
                        .queryParam("symbol", par)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(TwelveDataExchangeRateResponse.class))));
            cambios.put(par, new Cambio(taxa, relogio.getAsLong()));
            return taxa;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProvedorIndisponivelException("Twelve Data", "Consulta de câmbio interrompida. Tente novamente.");
        } finally {
            if (adquirido) cambioLock.unlock();
        }
    }

    private TwelveDataResponse validar(TwelveDataResponse response) {
        if (response == null) throw new FalhaDeProvedorException("Twelve Data");
        validarErro(response.code(), response.status(), true);
        if (vazio(response.name()) || response.close() == null || response.close().signum() <= 0 ||
                vazio(response.currency())) {
            throw new FalhaDeProvedorException("Twelve Data");
        }
        return response;
    }

    private BigDecimal validarTaxa(TwelveDataExchangeRateResponse response) {
        if (response == null) throw new FalhaDeProvedorException("Twelve Data");
        validarErro(response.code(), response.status(), false);
        if (response.rate() == null || response.rate().signum() <= 0) {
            throw new FalhaDeProvedorException("Twelve Data");
        }
        return response.rate();
    }

    private void validarErro(Integer code, String status, boolean ticker) {
        if (code == null && !"error".equalsIgnoreCase(status)) return;
        if (code != null && code == 429) throw new LimiteDeConsultasException("Twelve Data", 60);
        if (ticker && code != null && (code == 400 || code == 404)) throw new DadoExternoNaoEncontradoException("Ticker não encontrado ou não suportado pela Twelve Data");
        throw new FalhaDeProvedorException("Twelve Data", code == null ? 502 : code, null);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
