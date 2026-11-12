
package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.dtos.AlterarSenhaRequest;
import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.dtos.UsuarioCreateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioUpdateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioResponse;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void configurarMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveCadastrarUsernameDisponivel() {
        // Arrange
        UsuarioCreateRequest request =
                new UsuarioCreateRequest("ana", "senha-segura", Papel.USER);

        when(usuarioRepository.findByUsername("ana"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("senha-segura"))
                .thenReturn("hash-bcrypt");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(1L);
                    return usuario;
                });

        // Act
        UsuarioResponse response = usuarioService.create(request);

        // Assert
        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.papel()).isEqualTo(Papel.USER);
        assertThat(response.ativo()).isTrue();
    }

    @Test
    void deveRecusarUsernameDuplicado() {
        // Arrange
        Usuario existente = criarUsuario(1L, "ana");

        when(usuarioRepository.findByUsername("ana"))
                .thenReturn(Optional.of(existente));

        UsuarioCreateRequest request =
                new UsuarioCreateRequest("ana", "outra-senha", Papel.USER);

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void devePermitirAtualizacaoMantendoProprioUsername() {
        // Arrange
        Usuario existente = criarUsuario(1L, "ana");

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        when(usuarioRepository.findByUsername("ana"))
                .thenReturn(Optional.of(existente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioUpdateRequest request =
                new UsuarioUpdateRequest("ana", Papel.USER, true);

        // Act
        UsuarioResponse response = usuarioService.update(1L, request);

        // Assert
        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.ativo()).isTrue();
    }

    @Test
    void deveRetornarNotFoundAoConsultarIdInexistente() {
        // Arrange
        when(usuarioRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.findById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(usuarioRepository).findById(999L);
    }

    @Test
    void deveAlterarSenhaArmazenandoHashEmVezDeTextoPuro() {
        // Arrange
        Usuario usuario = criarUsuario(1L, "ana");

        AlterarSenhaRequest request =
                new AlterarSenhaRequest("nova-senha-segura");

        when(usuarioRepository.findForUpdateById(1L))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.encode("nova-senha-segura"))
                .thenReturn("novo-hash-bcrypt");

        // Act
        usuarioService.alterarSenha(1L, request);

        // Assert
        assertThat(usuario.getSenha())
                .isEqualTo("novo-hash-bcrypt")
                .isNotEqualTo(request.password());
        assertThat(usuario.getVersaoSessao()).isEqualTo(1);

        verify(passwordEncoder).encode("nova-senha-segura");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveRecusarAtualizacaoComUsernameDeOutroUsuario() {
        // Arrange
        Usuario usuario = criarUsuario(1L, "ana");
        Usuario outroUsuario = criarUsuario(2L, "bruno");

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.findByUsername("bruno"))
                .thenReturn(Optional.of(outroUsuario));

        UsuarioUpdateRequest request =
                new UsuarioUpdateRequest("bruno", Papel.USER, true);

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.update(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private Usuario criarUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setSenha("hash-bcrypt");
        usuario.setPapel(Papel.USER);
        usuario.setAtivo(true);
        return usuario;
    }
}
