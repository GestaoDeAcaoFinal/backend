package com.av2.gestaodeacao.domains.dtos;

public record TokenResponse(
        String token,
        String tipo,
        long expiresIn
) { }
