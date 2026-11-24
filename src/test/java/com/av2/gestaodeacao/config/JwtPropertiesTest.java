package com.av2.gestaodeacao.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SECRET = "s".repeat(32);
    private static final String ISSUER = "emissor-apenas-teste";

    @ParameterizedTest(name = "{0}")
    @MethodSource("segredosInvalidos")
    void rejeitaSegredoAusenteOuComMenosDe32Bytes(String cenario, String secret) {
        assertThatThrownBy(() -> new JwtProperties(secret, Duration.ofMinutes(60), ISSUER))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.jwt.secret deve possuir pelo menos 32 bytes");
    }

    static Stream<Arguments> segredosInvalidos() {
        return Stream.of(
                Arguments.of("nulo", null),
                Arguments.of("vazio", ""),
                Arguments.of("31 bytes ASCII", "s".repeat(31)),
                Arguments.of("15 caracteres UTF8 com 30 bytes", "é".repeat(15))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("segredosValidos")
    void aceitaLimiteEmBytesUtf8EPreservaConfiguracao(String cenario, String secret) {
        JwtProperties properties = new JwtProperties(secret, Duration.ofMinutes(60), ISSUER);

        assertThat(secret.getBytes(StandardCharsets.UTF_8).length).isGreaterThanOrEqualTo(32);
        assertThat(properties.secret()).isEqualTo(secret);
        assertThat(properties.expiration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(properties.issuer()).isEqualTo(ISSUER);
    }

    static Stream<Arguments> segredosValidos() {
        return Stream.of(
                Arguments.of("exatamente 32 bytes ASCII", SECRET),
                Arguments.of("64 bytes ASCII", "s".repeat(64)),
                Arguments.of("16 caracteres UTF8 com 32 bytes", "é".repeat(16))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expiracoesInvalidas")
    void rejeitaExpiracaoAusenteZeroOuNegativa(String cenario, Duration expiration) {
        assertThatThrownBy(() -> new JwtProperties(SECRET, expiration, ISSUER))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.jwt.expiration deve ser maior que zero");
    }

    static Stream<Arguments> expiracoesInvalidas() {
        return Stream.of(
                Arguments.of("nula", null),
                Arguments.of("zero", Duration.ZERO),
                Arguments.of("negativa", Duration.ofSeconds(-1))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emissoresInvalidos")
    void rejeitaEmissorAusenteOuEmBranco(String cenario, String issuer) {
        assertThatThrownBy(() -> new JwtProperties(SECRET, Duration.ofMinutes(60), issuer))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.jwt.issuer deve ser informado");
    }

    static Stream<Arguments> emissoresInvalidos() {
        return Stream.of(
                Arguments.of("nulo", null),
                Arguments.of("vazio", ""),
                Arguments.of("somente espacos", " \t\n")
        );
    }

    @Test
    void aceitaDuracaoPositivaCurtaSemSubstituirPorUmPadrao() {
        JwtProperties properties = new JwtProperties(SECRET, Duration.ofSeconds(1), ISSUER);

        assertThat(properties.expiration()).isEqualTo(Duration.ofSeconds(1));
    }
}
