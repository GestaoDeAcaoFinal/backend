package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.cliente.support.TwelveDataLimitador;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClientesValidacaoTest {

    private static final String BASE = "https://provedor.test";
    private static final String QUOTE = "/quote?symbol=AAPL&apikey=test-key";
    private static final String FX = "/exchange_rate?symbol=USD/BRL&apikey=test-key";
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProvedorHttpExecutor executor = new ProvedorHttpExecutor();

    @AfterEach
    void verificaTodasAsConsultasEsperadas() {
        server.verify();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acoesAusentes")
    void brapiDistingueTickerAusenteDeCotacaoIncompleta(String cenario, String json) {
        AcaoCliente cliente = new AcaoCliente(builder, executor, BASE, "test-token");
        server.expect(requestTo(BASE + "/api/quote/PETR4"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscarAcao("PETR4"))
                .isExactlyInstanceOf(DadoExternoNaoEncontradoException.class)
                .hasMessageContaining("Ticker não encontrado");
    }

    static Stream<Arguments> acoesAusentes() {
        return Stream.of(
                Arguments.of("corpo ausente", ""),
                Arguments.of("campo results ausente", "{}"),
                Arguments.of("results nulo", "{\"results\":null}"),
                Arguments.of("results vazio", "{\"results\":[]}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cotacoesBrapiInvalidas")
    void brapiRejeitaDadosQueImpedemUsarACotacao(String cenario, String json) {
        AcaoCliente cliente = new AcaoCliente(builder, executor, BASE, "test-token");
        responder("/api/quote/PETR4", json);

        assertThatThrownBy(() -> cliente.buscarAcao("PETR4"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageContaining("Brapi");
    }

    static Stream<Arguments> cotacoesBrapiInvalidas() {
        return Stream.of(
                Arguments.of("primeiro resultado nulo", "{\"results\":[null]}"),
                Arguments.of("nome nulo", "{\"results\":[{\"longName\":null,\"regularMarketPrice\":30,\"currency\":\"BRL\"}]}"),
                Arguments.of("nome vazio", "{\"results\":[{\"longName\":\"\",\"regularMarketPrice\":30,\"currency\":\"BRL\"}]}"),
                Arguments.of("nome em branco", "{\"results\":[{\"longName\":\"  \",\"regularMarketPrice\":30,\"currency\":\"BRL\"}]}"),
                Arguments.of("preco nulo", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":null,\"currency\":\"BRL\"}]}"),
                Arguments.of("preco zero", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":0,\"currency\":\"BRL\"}]}"),
                Arguments.of("preco negativo", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":-1,\"currency\":\"BRL\"}]}"),
                Arguments.of("moeda nula", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":30,\"currency\":null}]}"),
                Arguments.of("moeda vazia", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":30,\"currency\":\"\"}]}"),
                Arguments.of("moeda em branco", "{\"results\":[{\"longName\":\"Petrobras\",\"regularMarketPrice\":30,\"currency\":\"  \"}]}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cotacoesTwelveInvalidas")
    void twelveRejeitaCotacoesIncompletasOuNaoPositivas(String cenario, String json) {
        TwelveDataCliente cliente = twelve();
        responder(QUOTE, json);

        assertThatThrownBy(() -> cliente.buscarAcao("AAPL"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageContaining("Twelve Data");
    }

    static Stream<Arguments> cotacoesTwelveInvalidas() {
        return Stream.of(
                Arguments.of("corpo ausente", ""),
                Arguments.of("nome nulo", "{\"name\":null,\"close\":100,\"currency\":\"USD\"}"),
                Arguments.of("nome vazio", "{\"name\":\"\",\"close\":100,\"currency\":\"USD\"}"),
                Arguments.of("nome em branco", "{\"name\":\"  \",\"close\":100,\"currency\":\"USD\"}"),
                Arguments.of("preco nulo", "{\"name\":\"Apple\",\"close\":null,\"currency\":\"USD\"}"),
                Arguments.of("preco zero", "{\"name\":\"Apple\",\"close\":0,\"currency\":\"USD\"}"),
                Arguments.of("preco negativo", "{\"name\":\"Apple\",\"close\":-1,\"currency\":\"USD\"}"),
                Arguments.of("moeda nula", "{\"name\":\"Apple\",\"close\":100,\"currency\":null}"),
                Arguments.of("moeda vazia", "{\"name\":\"Apple\",\"close\":100,\"currency\":\"\"}"),
                Arguments.of("moeda em branco", "{\"name\":\"Apple\",\"close\":100,\"currency\":\"  \"}")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404})
    void twelveIdentificaSimboloInexistenteMesmoComHttp200(int code) {
        TwelveDataCliente cliente = twelve();
        responder(QUOTE, "{\"code\":" + code + ",\"status\":\"error\",\"message\":\"segredo-simulado\"}");

        assertThatThrownBy(() -> cliente.buscarAcao("AAPL"))
                .isExactlyInstanceOf(DadoExternoNaoEncontradoException.class)
                .hasMessageContaining("Ticker")
                .hasMessageNotContaining("segredo-simulado");
    }

    @ParameterizedTest
    @ValueSource(strings = {"error", "ERROR"})
    void twelveIdentificaErroSemCodigoSemExporMensagemExterna(String status) {
        TwelveDataCliente cliente = twelve();
        responder(QUOTE, "{\"status\":\"" + status + "\",\"message\":\"segredo-simulado\"}");

        assertThatThrownBy(() -> cliente.buscarAcao("AAPL"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageNotContaining("segredo-simulado");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cambiosInvalidos")
    void cambioInvalidoNaoContaminaCacheENovaRespostaValidaPodeSerUsada(String cenario, String json) {
        TwelveDataCliente cliente = twelve();
        responder(FX, json);
        responder(FX, "{\"rate\":5.25}");

        assertThatThrownBy(() -> cliente.buscarTaxaCambio("USD", "BRL"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageNotContaining("segredo-simulado");
        assertThat(cliente.buscarTaxaCambio("USD", "BRL")).isEqualByComparingTo("5.25");
        assertThat(cliente.buscarTaxaCambio("USD", "BRL")).isEqualByComparingTo("5.25");
    }

    static Stream<Arguments> cambiosInvalidos() {
        return Stream.of(
                Arguments.of("corpo ausente", ""),
                Arguments.of("taxa nula", "{\"rate\":null}"),
                Arguments.of("taxa zero", "{\"rate\":0}"),
                Arguments.of("taxa negativa", "{\"rate\":-1}"),
                Arguments.of("erro sem codigo", "{\"status\":\"error\",\"message\":\"segredo-simulado\"}"),
                Arguments.of("erro 400 de cambio nao e ticker ausente", "{\"code\":400,\"status\":\"error\"}"),
                Arguments.of("erro 404 de cambio nao e ticker ausente", "{\"code\":404,\"status\":\"error\"}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cnpjsInvalidos")
    void brasilApiRejeitaRespostaSemRazaoSocialUtilizavel(String cenario, String json) {
        CnpjCliente cliente = new CnpjCliente(builder, executor, BASE);
        responder("/api/cnpj/v1/191", json);

        assertThatThrownBy(() -> cliente.buscarCnpj("191"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageContaining("BrasilAPI");
    }

    static Stream<Arguments> cnpjsInvalidos() {
        return Stream.of(
                Arguments.of("corpo ausente", ""),
                Arguments.of("razao social ausente", "{}"),
                Arguments.of("razao social nula", "{\"razao_social\":null}"),
                Arguments.of("razao social vazia", "{\"razao_social\":\"\"}"),
                Arguments.of("razao social em branco", "{\"razao_social\":\"  \"}")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{\"erro\":true}"})
    void viaCepIdentificaCepAusente(String json) {
        ViaCepCliente cliente = new ViaCepCliente(builder, executor, BASE);
        responder("/ws/01001000/json/", json);

        assertThatThrownBy(() -> cliente.buscarCep("01001000"))
                .isExactlyInstanceOf(DadoExternoNaoEncontradoException.class)
                .hasMessageContaining("CEP não encontrado");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("enderecosInvalidos")
    void viaCepRejeitaEnderecoIncompleto(String cenario, String json) {
        ViaCepCliente cliente = new ViaCepCliente(builder, executor, BASE);
        responder("/ws/01001000/json/", json);

        assertThatThrownBy(() -> cliente.buscarCep("01001000"))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessageContaining("ViaCEP");
    }

    static Stream<Arguments> enderecosInvalidos() {
        return Stream.of(
                Arguments.of("logradouro nulo", "{\"logradouro\":null,\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}"),
                Arguments.of("logradouro em branco", "{\"logradouro\":\"  \",\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}"),
                Arguments.of("bairro nulo", "{\"logradouro\":\"Praca\",\"bairro\":null,\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}"),
                Arguments.of("bairro em branco", "{\"logradouro\":\"Praca\",\"bairro\":\"  \",\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}"),
                Arguments.of("localidade nula", "{\"logradouro\":\"Praca\",\"bairro\":\"Se\",\"localidade\":null,\"uf\":\"SP\"}"),
                Arguments.of("localidade em branco", "{\"logradouro\":\"Praca\",\"bairro\":\"Se\",\"localidade\":\"  \",\"uf\":\"SP\"}"),
                Arguments.of("uf nula", "{\"logradouro\":\"Praca\",\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":null}"),
                Arguments.of("uf em branco", "{\"logradouro\":\"Praca\",\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":\"  \"}")
        );
    }

    @Test
    void viaCepAceitaErroExplicitamenteFalsoComEnderecoValido() {
        ViaCepCliente cliente = new ViaCepCliente(builder, executor, BASE);
        responder("/ws/01001000/json/", "{\"erro\":false,\"logradouro\":\"Praca\",\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}");

        assertThat(cliente.buscarCep("01001000").uf()).isEqualTo("SP");
    }

    private void responder(String path, String json) {
        server.expect(requestTo(BASE + path)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private TwelveDataCliente twelve() {
        return new TwelveDataCliente(builder, executor, BASE, "test-key",
                new TwelveDataLimitador(8, 8, 0, 1000), 300);
    }
}
