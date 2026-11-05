
package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String novoUsername() {
        return "teste_" + UUID.randomUUID()
                .toString().replace("-", "");
    }

    private Usuario criarUsuarioDeTeste() {
        Usuario usuario = new Usuario();
        usuario.setUsername(novoUsername());
        usuario.setSenha(passwordEncoder.encode("SenhaAntiga123!"));
        usuario.setPapel(Papel.USER);
        usuario.setAtivo(true);

        return usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void adminDeveCriarUsuarioComSucesso() throws Exception {
        String username = novoUsername();

        mockMvc.perform(post("/api/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "SenhaForte123!",
                                  "papel": "USER"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());

        assertThat(usuarioRepository.findByUsername(username)).isPresent();
    }

    @Test
        // ALTERAÇÃO 1: removido @WithMockUser
    void deveArmazenarSenhaComBCrypt() throws Exception {
        String username = novoUsername();
        String senhaOriginal = "SenhaForte123!";

        mockMvc.perform(post("/api/usuarios")
                        // ALTERAÇÃO 2: autenticação ADMIN adicionada
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "papel": "USER"
                                }
                                """.formatted(username, senhaOriginal)))
                .andExpect(status().isCreated());

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow();

        assertThat(usuario.getSenha())
                .isNotEqualTo(senhaOriginal);

        assertThat(passwordEncoder.matches(
                senhaOriginal, usuario.getSenha()
        )).isTrue();
    }

    @Test
        // ALTERAÇÃO 3: removido @WithMockUser
    void respostasNaoDevemExporSenhaNemHash() throws Exception {
        Usuario usuario = criarUsuarioDeTeste();

        mockMvc.perform(get("/api/usuarios/{id}", usuario.getId())
                        // ALTERAÇÃO 4: autenticação ADMIN adicionada
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username")
                        .value(usuario.getUsername()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
        // ALTERAÇÃO 5: removido @WithMockUser
    void adminDeveListarUsuarios() throws Exception {
        Usuario usuario = criarUsuarioDeTeste();

        mockMvc.perform(get("/api/usuarios")
                        // ALTERAÇÃO 6: autenticação ADMIN adicionada
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.username == '%s')]"
                        .formatted(usuario.getUsername())).exists());
    }

    @Test
        // ALTERAÇÃO 7: removido @WithMockUser
    void adminDeveAlterarPapelESituacao() throws Exception {
        Usuario usuario = criarUsuarioDeTeste();

        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                        // ALTERAÇÃO 8: autenticação ADMIN adicionada
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "papel": "ADMIN",
                                  "ativo": false
                                }
                                """.formatted(usuario.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(false));

        Usuario atualizado = usuarioRepository
                .findById(usuario.getId())
                .orElseThrow();

        assertThat(atualizado.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(atualizado.getAtivo()).isFalse();
    }

    @Test
        // ALTERAÇÃO 9: removido @WithMockUser
    void adminDeveAlterarSenhaComSeguranca() throws Exception {
        Usuario usuario = criarUsuarioDeTeste();
        String hashAnterior = usuario.getSenha();

        mockMvc.perform(put("/api/usuarios/{id}/senha", usuario.getId())
                        // ALTERAÇÃO 10: autenticação ADMIN adicionada
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "NovaSenha456!"
                                }
                                """))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository
                .findById(usuario.getId())
                .orElseThrow();

        assertThat(atualizado.getSenha()).isNotEqualTo(hashAnterior);

        assertThat(passwordEncoder.matches(
                "NovaSenha456!", atualizado.getSenha()
        )).isTrue();

        assertThat(passwordEncoder.matches(
                "SenhaAntiga123!", atualizado.getSenha()
        )).isFalse();
    }

    @Test
    void userNaoDeveGerenciarUsuarios() throws Exception {

        mockMvc.perform(get("/api/usuarios")
                        .with(user("aluno").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/usuarios")
                        .with(user("aluno").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "usuario_sem_permissao",
                                  "password": "SenhaForte123!",
                                  "papel": "USER"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
