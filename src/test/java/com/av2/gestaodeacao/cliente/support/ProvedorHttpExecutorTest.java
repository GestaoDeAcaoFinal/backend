package com.av2.gestaodeacao.cliente.support;

import com.av2.gestaodeacao.exceptions.LimiteDeConsultasException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProvedorHttpExecutorTest {

    @ParameterizedTest(name = "headers presentes: {0}")
    @ValueSource(booleans = {false, true})
    void limiteSemRetryAfterUsaUmMinutoSemExporCorpoOuRepetirChamada(boolean possuiHeaders) {
        HttpHeaders headers = possuiHeaders ? new HttpHeaders() : null;
        if (headers != null) {
            headers.set("X-Request-Id", "requisicao-apenas-teste");
        }
        RestClientResponseException resposta = new RestClientResponseException(
                "erro HTTP apenas de teste", HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests", headers,
                "segredo-simulado-do-provedor".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        if (possuiHeaders) {
            assertThat(resposta.getResponseHeaders()).isNotNull();
            assertThat(resposta.getResponseHeaders().getFirst("Retry-After")).isNull();
        } else {
            assertThat(resposta.getResponseHeaders()).isNull();
        }
        AtomicInteger chamadas = new AtomicInteger();
        ProvedorHttpExecutor executor = new ProvedorHttpExecutor();

        assertThatThrownBy(() -> executor.executar("Provedor de teste", () -> {
            chamadas.incrementAndGet();
            throw resposta;
        })).isExactlyInstanceOf(LimiteDeConsultasException.class)
                .hasMessageContaining("limite")
                .hasMessageNotContaining("segredo-simulado-do-provedor")
                .satisfies(erro -> assertThat(((LimiteDeConsultasException) erro)
                        .getTentarAposSegundos()).isEqualTo(60));
        assertThat(chamadas.get()).isEqualTo(1);
    }
}
