package com.av2.gestaodeacao.domains.dtos;

import com.av2.gestaodeacao.domains.enums.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank
        @Size(max = 100) String username,

        @NotNull
        Papel papel,

        @NotNull
        Boolean ativo
) {
}
