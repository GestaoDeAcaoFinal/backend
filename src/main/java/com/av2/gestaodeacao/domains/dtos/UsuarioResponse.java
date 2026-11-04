package com.av2.gestaodeacao.domains.dtos;

import com.av2.gestaodeacao.domains.enums.Papel;

public record UsuarioResponse(
        Long id,
        String username,
        Papel papel,
        Boolean ativo
) {
}
