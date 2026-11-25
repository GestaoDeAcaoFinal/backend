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
        Papel papel,

        @NotBlank(message = "CPF é obrigatório")
        @Size(max = 14, message = "Informe um CPF válido com 11 dígitos")
        String cpf,

        @NotBlank(message = "Telefone com DDD é obrigatório")
        @Size(max = 15, message = "Informe um telefone válido com DDD")
        String telefone
) {

}
