package com.av2.gestaodeacao.services;


import com.av2.gestaodeacao.config.JwtProperties;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;

import com.av2.gestaodeacao.domains.dtos.LoginRequest;
import com.av2.gestaodeacao.domains.dtos.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final UsuarioRepository usuarios;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            UsuarioRepository usuarios
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.usuarios = usuarios;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {

        // A senha e a versão devem pertencer ao mesmo estado da conta durante a emissão.
        var usuarioBloqueado = usuarios.findForUpdateByUsername(request.username());

        // 1. Autentica o usuário
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password())
        );
        // Mantém também para nomes inexistentes a verificação fictícia feita pelo provider.
        var usuario = usuarioBloqueado
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // 2. Define a emissão e expiração do token
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.expiration());

        // 3. Obtém as permissões do usuário
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // 4. Define as informações do JWT
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("uid", usuario.getId().toString())
                .claim("sv", usuario.getVersaoSessao().toString())
                .claim("authorities", authorities)
                .build();

        // 5. Configura a assinatura do JWT
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        // 6. Gera o token
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        // 7. Retorna o token ao cliente
        return new TokenResponse(
                token,
                "Bearer",
                jwtProperties.expiration().getSeconds()
        );
    }
}
