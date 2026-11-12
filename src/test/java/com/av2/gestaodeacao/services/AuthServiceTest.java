package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.config.JwtProperties;
import com.av2.gestaodeacao.domains.dtos.LoginRequest;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void usuarioInexistentePassaPelaProtecaoDeTempoDoAuthenticationManager() {
        var manager = mock(AuthenticationManager.class);
        var usuarios = mock(UsuarioRepository.class);
        var encoder = mock(JwtEncoder.class);
        var properties = new JwtProperties("chave-apenas-de-teste-com-32-bytes-ou-mais", Duration.ofMinutes(30), "test");
        when(usuarios.findForUpdateByUsername("inexistente")).thenReturn(Optional.empty());
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));
        var service = new AuthService(manager, encoder, properties, usuarios);

        assertThatThrownBy(() -> service.login(new LoginRequest("inexistente", "senha-invalida")))
                .isInstanceOf(BadCredentialsException.class);

        verify(manager).authenticate(any());
        verifyNoInteractions(encoder);
    }
}
