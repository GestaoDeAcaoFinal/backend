package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;

public final class DadosUsuarioNormalizer {
    private DadosUsuarioNormalizer() { }

    public static String cpf(String valor) {
        if (valor == null || !valor.trim().matches("(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})")) {
            throw new EntradaOuOperacaoInvalidaException("Informe um CPF válido com 11 dígitos");
        }
        String cpf = valor.trim().replaceAll("[.-]", "");
        if (cpf.matches("([0-9])\\1{10}")) {
            throw new EntradaOuOperacaoInvalidaException("Informe um CPF válido com 11 dígitos");
        }
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            if ((soma * 10 % 11) % 10 != cpf.charAt(tamanho) - '0') {
                throw new EntradaOuOperacaoInvalidaException("Informe um CPF válido com 11 dígitos");
            }
        }
        return cpf;
    }

    public static String telefone(String valor) {
        if (valor == null || !valor.trim().matches("(?:[0-9]{10,11}|\\([0-9]{2}\\) ?[0-9]{4,5}-[0-9]{4}|[0-9]{2} [0-9]{4,5}-?[0-9]{4})")) {
            throw new EntradaOuOperacaoInvalidaException("Informe um telefone fixo ou celular válido com DDD brasileiro");
        }
        String telefone = valor.trim().replaceAll("[() -]", "");
        if (!telefone.matches("(?:1[1-9]|2[12478]|3[1-578]|4[1-9]|5[1345]|6[1-9]|7[134579]|8[1-9]|9[1-9])(?:[2-5][0-9]{7}|9[0-9]{8})")) {
            throw new EntradaOuOperacaoInvalidaException("Informe um telefone fixo ou celular válido com DDD brasileiro");
        }
        return telefone;
    }
}
