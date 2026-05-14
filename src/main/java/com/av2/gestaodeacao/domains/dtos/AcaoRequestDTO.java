package com.av2.gestaodeacao.domains.dtos;

import com.av2.gestaodeacao.domains.Acao;
import lombok.Data;

@Data
public class AcaoRequestDTO {

    private String ticker;
    private Long corretoraId;
}
