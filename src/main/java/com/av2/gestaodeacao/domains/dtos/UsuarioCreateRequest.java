package com.av2.gestaodeacao.domains.dtos;

import com.av2.gestaodeacao.domains.enums.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank
        @Size(max = 100)
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotNull
        Papel papel
) {

}
