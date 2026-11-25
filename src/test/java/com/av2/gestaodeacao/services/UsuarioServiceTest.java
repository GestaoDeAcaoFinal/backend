
package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.dtos.AlterarSenhaRequest;
import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.dtos.UsuarioCreateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioUpdateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioResponse;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    @Test
    void cpfDuplicadoImpedeCriacaoEEdicaoAntesDePersistir() {
        Usuario existente = criarUsuario(2L, "outro");
        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.of(existente));
        assertThatThrownBy(() -> usuarioService.create(new UsuarioCreateRequest(
                "novo", "senha-segura", Papel.USER, "529.982.247-25", "(11) 99999-9999")))
                .isInstanceOf(ConflitoException.class).hasMessage("CPF já cadastrado em outra conta");
        Usuario alvo = criarUsuario(1L, "ana");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alvo));
        assertThatThrownBy(() -> usuarioService.update(1L, new UsuarioUpdateRequest(
                "ana", Papel.USER, true, "52998224725", "11999999999")))
                .isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
        assertThat(alvo.getCpf()).isNull();
    }

    @Test
    void proprioCpfPodeSerMantidoEConflitoConcorrenteTemMensagemSegura() {
        Usuario alvo = criarUsuario(1L, "ana");
        alvo.setCpf("52998224725");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.of(alvo));
        when(usuarioRepository.saveAndFlush(alvo)).thenReturn(alvo);
        UsuarioResponse resposta = usuarioService.update(1L, new UsuarioUpdateRequest(
                "ana", Papel.USER, true, "529.982.247-25", "(11) 99999-9999"));
        assertThat(resposta.cpf()).isEqualTo("52998224725");
        assertThat(resposta.telefone()).isEqualTo("11999999999");
        assertThat(alvo.getVersaoSessao()).isZero();
        when(usuarioRepository.saveAndFlush(alvo)).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("SQL com dados internos"));
        assertThatThrownBy(() -> usuarioService.update(1L, new UsuarioUpdateRequest(
                "ana", Papel.USER, true, "52998224725", "11999999999")))
                .isExactlyInstanceOf(ConflitoException.class)
                .hasMessageNotContaining("SQL");
    }
    private AutoCloseable mocks;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void configurarMocks() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void liberarMocks() throws Exception {
        mocks.close();
    }

    @ParameterizedTest
    @CsvSource({"USER,true", "ADMIN,false", "USER,false"})
    void impedeRemoverUltimoAdminMesmoComOutrasContasInativasOuSemPermissao(Papel novoPapel, boolean novoAtivo) {
        Usuario admin = criarUsuario(1L, "ana");
        admin.setPapel(Papel.ADMIN);
        Usuario investidor = criarUsuario(2L, "investidor");
        Usuario adminInativo = criarUsuario(3L, "adminInativo");
        adminInativo.setPapel(Papel.ADMIN);
        adminInativo.setAtivo(false);
        when(usuarioRepository.findAllForUpdate()).thenReturn(List.of(admin, investidor, adminInativo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.update(1L, new UsuarioUpdateRequest("ana", novoPapel, novoAtivo, "52998224725", "11999999999")))
                .isExactlyInstanceOf(ConflitoException.class)
                .hasMessageContaining("pelo menos um administrador ativo");

        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(admin.getAtivo()).isTrue();
        assertThat(admin.getVersaoSessao()).isZero();
        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @CsvSource({"USER,true", "ADMIN,false", "USER,false"})
    void permiteAlteracaoQuandoExisteOutroAdminAtivoEInvalidaSessao(Papel novoPapel, boolean novoAtivo) {
        Usuario admin = criarUsuario(1L, "ana");
        admin.setPapel(Papel.ADMIN);
        Usuario investidor = criarUsuario(2L, "investidor");
        Usuario inativo = criarUsuario(3L, "inativo");
        inativo.setPapel(Papel.ADMIN);
        inativo.setAtivo(false);
        Usuario outroAdmin = criarUsuario(4L, "outroAdmin");
        outroAdmin.setPapel(Papel.ADMIN);
        when(usuarioRepository.findAllForUpdate()).thenReturn(List.of(admin, investidor, inativo, outroAdmin));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(admin));
        when(usuarioRepository.saveAndFlush(admin)).thenReturn(admin);

        UsuarioResponse resposta = usuarioService.update(1L, new UsuarioUpdateRequest("ana", novoPapel, novoAtivo, "52998224725", "11999999999"));

        assertThat(resposta.papel()).isEqualTo(novoPapel);
        assertThat(resposta.ativo()).isEqualTo(novoAtivo);
        assertThat(admin.getVersaoSessao()).isEqualTo(1);
        assertThat(outroAdmin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(outroAdmin.getAtivo()).isTrue();
        verify(usuarioRepository).findAllForUpdate();
        verify(usuarioRepository).saveAndFlush(admin);
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @CsvSource({
            "USER,true,ana,USER,true,0",
            "USER,true,anaNova,USER,true,1",
            "USER,true,ana,ADMIN,true,1",
            "USER,true,ana,USER,false,1",
            "ADMIN,true,ana,ADMIN,true,0",
            "ADMIN,true,anaNova,ADMIN,true,1",
            "ADMIN,false,ana,ADMIN,true,1",
            "ADMIN,false,ana,USER,false,1"
    })
    void versaoDeSessaoMudaSomenteQuandoDadosDeAcessoMudam(
            Papel papelAtual, boolean ativoAtual, String novoNome,
            Papel novoPapel, boolean novoAtivo, int incremento) {
        Usuario usuario = criarUsuario(1L, "ana");
        usuario.setPapel(papelAtual);
        usuario.setAtivo(ativoAtual);
        usuario.invalidarSessoes();
        usuario.invalidarSessoes();
        Usuario adminSistema = criarUsuario(99L, "adminSistema");
        adminSistema.setPapel(Papel.ADMIN);
        when(usuarioRepository.findAllForUpdate()).thenReturn(List.of(usuario, adminSistema));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByUsername(novoNome))
                .thenReturn(novoNome.equals("ana") ? Optional.of(usuario) : Optional.empty());
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        UsuarioResponse resposta = usuarioService.update(1L, new UsuarioUpdateRequest(novoNome, novoPapel, novoAtivo, "52998224725", "11999999999"));

        assertThat(resposta.username()).isEqualTo(novoNome);
        assertThat(resposta.papel()).isEqualTo(novoPapel);
        assertThat(resposta.ativo()).isEqualTo(novoAtivo);
        assertThat(usuario.getVersaoSessao()).isEqualTo(2L + incremento);
        assertThat(usuario.getSenha()).isEqualTo("hash-bcrypt");
        verify(usuarioRepository).saveAndFlush(usuario);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void senhaDeUsuarioInexistenteNaoCodificaNemPersiste() {
        when(usuarioRepository.findForUpdateById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarSenha(999L, new AlterarSenhaRequest("nova-senha-segura")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void deveCadastrarUsernameDisponivel() {
        // Arrange
        UsuarioCreateRequest request =
                new UsuarioCreateRequest("ana", "senha-segura", Papel.USER, "52998224725", "11999999999");

        when(usuarioRepository.findByUsername("ana"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("senha-segura"))
                .thenReturn("hash-bcrypt");

        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
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
                new UsuarioCreateRequest("ana", "outra-senha", Papel.USER, "52998224725", "11999999999");

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void devePermitirAtualizacaoMantendoProprioUsername() {
        // Arrange
        Usuario existente = criarUsuario(1L, "ana");

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        when(usuarioRepository.findByUsername("ana"))
                .thenReturn(Optional.of(existente));

        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioUpdateRequest request =
                new UsuarioUpdateRequest("ana", Papel.USER, true, "52998224725", "11999999999");

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
                new UsuarioUpdateRequest("bruno", Papel.USER, true, "52998224725", "11999999999");

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.update(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
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
