package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class DadosUsuarioNormalizerTest {
    @ParameterizedTest
    @CsvSource({"529.982.247-25,52998224725", "52998224725,52998224725", "111.444.777-35,11144477735"})
    void normalizaCpfValido(String entrada, String esperado) {
        assertThat(DadosUsuarioNormalizer.cpf(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "00000000000", "11111111111", "99999999999", "52998224724", "52998224715", "5299822472", "529982247250", "529982247-25", "abc52998224725", "529/982/247-25"})
    void rejeitaCpfInvalidoSemExporValor(String entrada) {
        assertThatThrownBy(() -> DadosUsuarioNormalizer.cpf(entrada))
                .isExactlyInstanceOf(EntradaOuOperacaoInvalidaException.class)
                .hasMessage("Informe um CPF válido com 11 dígitos");
    }

    @ParameterizedTest
    @CsvSource({"(11) 99999-9999,11999999999", "11999999999,11999999999", "(21) 3333-4444,2133334444", "21 3333-4444,2133334444", "99999999999,99999999999"})
    void normalizaCelularOuFixoComDdd(String entrada, String esperado) {
        assertThat(DadosUsuarioNormalizer.telefone(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "999999999", "00999999999", "20999999999", "1199999999", "11899999999", "1112345678", "+55 11 99999-9999", "(11 99999-9999", "11abc999999999", "11 99999--9999"})
    void rejeitaTelefoneInvalidoSemExporValor(String entrada) {
        assertThatThrownBy(() -> DadosUsuarioNormalizer.telefone(entrada))
                .isExactlyInstanceOf(EntradaOuOperacaoInvalidaException.class)
                .hasMessage("Informe um telefone fixo ou celular válido com DDD brasileiro");
    }
}
