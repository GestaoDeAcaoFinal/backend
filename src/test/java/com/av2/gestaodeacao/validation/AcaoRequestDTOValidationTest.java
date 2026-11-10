
package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AcaoRequestDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void finalizar() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "PETR4, true",
            "aapl, true",
            "ABCDEFGHIJKL, true",
            "ABCDEFGHIJKLM, false",
            "123456789012, false",
            "PETR-4, false"
    })
    void deveValidarFormatoDoTicker(String ticker, boolean esperadoValido) {
        // Arrange
        AcaoRequestDTO dto = new AcaoRequestDTO();
        dto.setTicker(ticker);
        dto.setCorretoraId(1L);

        // Act
        boolean valido = validator.validate(dto).isEmpty();

        // Assert
        assertThat(valido).isEqualTo(esperadoValido);
    }
}
