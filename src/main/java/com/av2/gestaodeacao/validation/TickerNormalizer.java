package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;

import java.util.Locale;

public final class TickerNormalizer {

    public static final String FORMATO_TICKER = "^\\s*(?=[A-Za-z0-9.]{1,12}\\s*$)(?=[A-Za-z0-9.]*[A-Za-z])[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*\\s*$";
    public static final String MENSAGEM_FORMATO = "Ticker deve ter de 1 a 12 caracteres, conter letras e usar ponto somente entre letras ou números";

    private TickerNormalizer() {
    }

    public static String normalizar(String ticker) {
        if (ticker == null) {
            throw new EntradaOuOperacaoInvalidaException("Ticker é obrigatório");
        }

        String normalizado = ticker.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.matches(FORMATO_TICKER)) {
            throw new EntradaOuOperacaoInvalidaException(MENSAGEM_FORMATO);
        }
        return normalizado;
    }
}
