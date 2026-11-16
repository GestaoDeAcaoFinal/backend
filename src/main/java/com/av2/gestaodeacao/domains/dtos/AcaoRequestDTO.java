package com.av2.gestaodeacao.domains.dtos;

import com.av2.gestaodeacao.validation.TickerNormalizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AcaoRequestDTO {

    @NotBlank(message = "Ticker é obrigatório")
    @Pattern(
            regexp = TickerNormalizer.FORMATO_TICKER,
            message = TickerNormalizer.MENSAGEM_FORMATO
    )
    private String ticker;

    @NotNull(message = "Corretora é obrigatória")
    @Positive(message = "Corretora deve possuir identificador positivo")
    private Long corretoraId;
}
