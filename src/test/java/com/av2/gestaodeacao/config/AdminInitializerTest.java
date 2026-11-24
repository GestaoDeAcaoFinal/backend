package com.av2.gestaodeacao.config;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminInitializerTest {

    private static final String USERNAME = "admin_inicial_teste";
    private static final String PASSWORD = "senha-inicial-apenas-teste";
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));
    private final AdminInitializer initializer = new AdminInitializer(repository, encoder, USERNAME, PASSWORD);

    @Test
    void criaAdministradorAtivoComHashQuandoUsernameAindaNaoExiste() {
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        initializer.criarAdministradorInicial();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getUsername()).isEqualTo(USERNAME);
        assertThat(salvo.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getVersaoSessao()).isZero();
        assertThat(salvo.getSenha()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, salvo.getSenha())).isTrue();
        verify(encoder).encode(PASSWORD);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,true", "ADMIN,false", "USER,true", "USER,false"})
    void contaExistenteNaoTemSenhaPermissoesStatusOuSessoesRedefinidos(Papel papel, boolean ativo) {
        Usuario existente = new Usuario();
        existente.setId(7L);
        existente.setUsername(USERNAME);
        existente.setSenha("hash-existente-preservado");
        existente.setPapel(papel);
        existente.setAtivo(ativo);
        existente.invalidarSessoes();
        existente.invalidarSessoes();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existente));

        initializer.criarAdministradorInicial();

        assertThat(existente.getId()).isEqualTo(7);
        assertThat(existente.getUsername()).isEqualTo(USERNAME);
        assertThat(existente.getSenha()).isEqualTo("hash-existente-preservado");
        assertThat(existente.getPapel()).isEqualTo(papel);
        assertThat(existente.getAtivo()).isEqualTo(ativo);
        assertThat(existente.getVersaoSessao()).isEqualTo(2);
        verify(repository).findByUsername(USERNAME);
        verify(repository, never()).save(any(Usuario.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void repetirInicializacaoNaoDuplicaContaNemRefazHash() {
        AtomicReference<Usuario> persistido = new AtomicReference<>();
        when(repository.findByUsername(USERNAME)).thenAnswer(invocation -> Optional.ofNullable(persistido.get()));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(1L);
            persistido.set(salvo);
            return salvo;
        });

        initializer.criarAdministradorInicial();
        Usuario primeiro = persistido.get();
        assertThat(primeiro).isNotNull();
        String primeiroHash = primeiro.getSenha();
        initializer.criarAdministradorInicial();

        assertThat(persistido.get()).isSameAs(primeiro);
        assertThat(persistido.get().getSenha()).isEqualTo(primeiroHash);
        assertThat(encoder.matches(PASSWORD, primeiroHash)).isTrue();
        verify(repository, times(2)).findByUsername(USERNAME);
        verify(repository, times(1)).save(any(Usuario.class));
        verify(encoder, times(1)).encode(PASSWORD);
    }
}
