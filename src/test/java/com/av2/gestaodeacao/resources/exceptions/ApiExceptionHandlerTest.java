package com.av2.gestaodeacao.resources.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void agrupaErrosDeParametrosEPreservaMensagemPadraoQuandoNomeOuTextoAusentes(boolean possuiNome) {
        ParameterValidationResult quantidade = resultado(possuiNome ? "quantidade" : null,
                mensagem("deve ser maior que zero"), mensagem(null));
        ParameterValidationResult preco = resultado("preco", mensagem("deve ser positivo"));
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.getParameterValidationResults()).thenReturn(List.of(quantidade, preco));

        ProblemDetail problema = handler.handleParametrosInvalidos(exception);

        verificarProblemaDeValidacao(problema);
        assertThat(problema.getProperties().get("errors")).isEqualTo(Map.of(
                possuiNome ? "quantidade" : "parametro", List.of("deve ser maior que zero", "Valor inválido"),
                "preco", List.of("deve ser positivo")));
    }

    @Test
    void errosDoMesmoCampoNaoSobrescrevemMensagensAnteriores() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "pedido");
        binding.addError(new FieldError("pedido", "ticker", "formato inválido"));
        binding.addError(new FieldError("pedido", "ticker", null));
        binding.addError(new FieldError("pedido", "quantidade", "deve ser positiva"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), binding);

        ProblemDetail problema = handler.handleCamposInvalidos(exception);

        verificarProblemaDeValidacao(problema);
        assertThat(problema.getProperties().get("errors")).isEqualTo(Map.of(
                "ticker", List.of("formato inválido", "Valor inválido"),
                "quantidade", List.of("deve ser positiva")));
    }

    private static void verificarProblemaDeValidacao(ProblemDetail problema) {
        assertThat(problema.getStatus()).isEqualTo(400);
        assertThat(problema.getTitle()).isEqualTo("Requisição inválida");
        assertThat(problema.getDetail()).isEqualTo("Um ou mais campos da requisição são inválidos");
        assertThat(problema.getProperties()).containsKey("errors");
    }

    private static ParameterValidationResult resultado(String nome, MessageSourceResolvable... erros) {
        MethodParameter parametro = mock(MethodParameter.class);
        when(parametro.getParameterName()).thenReturn(nome);
        ParameterValidationResult resultado = mock(ParameterValidationResult.class);
        when(resultado.getMethodParameter()).thenReturn(parametro);
        when(resultado.getResolvableErrors()).thenReturn(List.of(erros));
        return resultado;
    }

    private static MessageSourceResolvable mensagem(String texto) {
        MessageSourceResolvable erro = mock(MessageSourceResolvable.class);
        when(erro.getDefaultMessage()).thenReturn(texto);
        return erro;
    }
}
