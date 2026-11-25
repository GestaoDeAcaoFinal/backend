package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UsuarioDetailsServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(usuarios);

    @Test
    void usuarioInexistenteResultaEmExcecaoDeAutenticacaoSemExporIdentificador() {
        when(usuarios.findByUsername("conta-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("conta-inexistente"))
                .isExactlyInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Usuario nao encontrado")
                .hasMessageNotContaining("conta-inexistente");
        verify(usuarios).findByUsername("conta-inexistente");
        verifyNoMoreInteractions(usuarios);
    }

    @ParameterizedTest(name = "{0}, ativo={1}, enabled={2}")
    @CsvSource({
            "ADMIN,true,true,ROLE_ADMIN",
            "USER,true,true,ROLE_USER",
            "ADMIN,false,false,ROLE_ADMIN",
            "USER,false,false,ROLE_USER"
    })
    void preservaIdentidadeHashPapelEEstadoDaConta(
            Papel papel, boolean ativo, boolean enabled, String autoridadeEsperada) {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setUsername("conta_teste");
        usuario.setSenha("hash-ficticio-somente-para-teste");
        usuario.setPapel(papel);
        usuario.setAtivo(ativo);
        usuario.invalidarSessoes();
        when(usuarios.findByUsername("conta_teste")).thenReturn(Optional.of(usuario));

        UserDetails detalhes = service.loadUserByUsername("conta_teste");

        assertThat(detalhes.getUsername()).isEqualTo("conta_teste");
        assertThat(detalhes.getPassword()).isEqualTo("hash-ficticio-somente-para-teste");
        assertThat(detalhes.isEnabled()).isEqualTo(enabled);
        assertThat(detalhes.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly(autoridadeEsperada);
        assertThat(usuario.getId()).isEqualTo(42L);
        assertThat(usuario.getUsername()).isEqualTo("conta_teste");
        assertThat(usuario.getSenha()).isEqualTo("hash-ficticio-somente-para-teste");
        assertThat(usuario.getPapel()).isEqualTo(papel);
        assertThat(usuario.getAtivo()).isEqualTo(ativo);
        assertThat(usuario.getVersaoSessao()).isEqualTo(1L);
        verify(usuarios).findByUsername("conta_teste");
        verifyNoMoreInteractions(usuarios);
    }
}
