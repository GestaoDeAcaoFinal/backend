package com.av2.gestaodeacao.cliente.support;

import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.LimiteDeConsultasException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class ProvedorHttpExecutor {

    public <T> T executar(String provedor, Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (DadoExternoNaoEncontradoException | FalhaDeProvedorException |
                 ProvedorIndisponivelException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ProvedorIndisponivelException(provedor, e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new LimiteDeConsultasException(provedor, esperaAposLimite(e));
            }
            if (e.getStatusCode().value() == 404) {
                throw new DadoExternoNaoEncontradoException(
                        "Dado não encontrado no provedor " + provedor);
            }
            throw new FalhaDeProvedorException(provedor, e.getStatusCode().value(), e);
        } catch (RuntimeException e) {
            throw new FalhaDeProvedorException(provedor, e);
        }
    }

    private long esperaAposLimite(RestClientResponseException exception) {
        String header = exception.getResponseHeaders() == null ? null : exception.getResponseHeaders().getFirst("Retry-After");
        if (header == null) return 60;
        try {
            return Math.max(1, Math.min(86_400, Long.parseLong(header.trim())));
        } catch (NumberFormatException ignored) {
            try {
                long segundos = java.time.Duration.between(java.time.Instant.now(), java.time.ZonedDateTime.parse(header, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();
                return Math.max(1, Math.min(86_400, segundos + 1));
            } catch (java.time.DateTimeException invalidHeader) { return 60; }
        }
    }
}
