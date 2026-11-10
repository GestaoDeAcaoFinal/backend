
package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.domains.dtos.AlterarSenhaRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AlterarSenhaRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void configurarValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "7, false",
            "8, true",
            "72, true",
            "73, false"
    })
    void deveValidarTamanhoDaSenha(int tamanho, boolean esperadoValido) {
        // Arrange
        String senha = "a".repeat(tamanho);
        AlterarSenhaRequest request = new AlterarSenhaRequest(senha);

        // Act
        boolean valido = validator.validate(request).isEmpty();

        // Assert
        assertThat(valido).isEqualTo(esperadoValido);
    }
}
