
package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.config.JwtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "TWELVE_DATA_API_KEY=test-key")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthResourceIntegrationTest {

    private static final String USUARIO = "integracao_jwt_admin";
    private static final String SENHA = "SenhaForte123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void preflightDoLoginPermiteFrontendAngular() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void prepararUsuario() {
        removerUsuario();

        jdbcTemplate.update(
                "INSERT INTO usuario (id, username, senha, papel, ativo) " +
                        "VALUES (?, ?, ?, ?, ?)",
                999999L,
                USUARIO,
                passwordEncoder.encode(SENHA),
                "ADMIN",
                true
        );
    }
    @AfterEach
    void removerUsuario() {
        jdbcTemplate.update("DELETE FROM operacao_acao WHERE usuario_id = 999999");
        jdbcTemplate.update(
                "DELETE FROM usuario WHERE username = ?",
                USUARIO
        );
    }

    @Test
    void loginValidoRetornaJwt() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void tokenAdminPerdeAcessoDepoisDeRebaixamento() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);
        jdbcTemplate.update("UPDATE usuario SET papel = 'USER' WHERE username = ?", USUARIO);
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioDesativadoNaoAcessaRecursos() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);
        jdbcTemplate.update("UPDATE usuario SET ativo = false WHERE username = ?", USUARIO);
        mockMvc.perform(get("/acoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trocaDeSenhaInvalidaTokenAnteriorEMantemNovoLogin() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);
        mockMvc.perform(put("/api/usuarios/999999/senha")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NovaSenhaSomenteTeste123!\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/acoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        String novoToken = realizarLogin(USUARIO, "NovaSenhaSomenteTeste123!");
        mockMvc.perform(get("/acoes").header("Authorization", "Bearer " + novoToken))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void tokenAntigoNaoPodeAssumirOutraContaComMesmoUsername() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);
        jdbcTemplate.update("UPDATE usuario SET username = 'admin_renomeado' WHERE id = 999999");
        jdbcTemplate.update("INSERT INTO usuario (id, username, senha, papel, ativo) VALUES (999997, ?, ?, 'ADMIN', true)", USUARIO, passwordEncoder.encode(SENHA));
        mockMvc.perform(get("/acoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void naoPermiteRebaixarUltimoAdminAtivo() throws Exception {
        jdbcTemplate.update("UPDATE usuario SET papel = 'USER' WHERE username <> ?", USUARIO);
        String token = realizarLogin(USUARIO, SENHA);
        mockMvc.perform(put("/api/usuarios/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"integracao_jwt_admin","papel":"USER","ativo":true}
                            """))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT papel FROM usuario WHERE id = 999999", String.class))
                .isEqualTo("ADMIN");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void naoPermiteDesativarUltimoAdminAtivo() throws Exception {
        jdbcTemplate.update("UPDATE usuario SET papel = 'USER' WHERE username <> ?", USUARIO);
        String token = realizarLogin(USUARIO, SENHA);
        mockMvc.perform(put("/api/usuarios/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"integracao_jwt_admin","papel":"ADMIN","ativo":false}
                            """))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT ativo FROM usuario WHERE id = 999999", Boolean.class))
                .isTrue();
    }

    @Test
    void senhaIncorretaRetorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(USUARIO, "senha-incorreta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/acoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtValidoPermiteAcessoAoRecursoProtegido() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);

        mockMvc.perform(get("/acoes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void carteiraHistoricoEVendaSaoExclusivosDoDono() throws Exception {
        jdbcTemplate.update("INSERT INTO usuario (id, username, senha, papel, ativo) VALUES (999998, 'outro_investidor', ?, 'USER', true)", passwordEncoder.encode(SENHA));
        jdbcTemplate.update("INSERT INTO acoes (id, ticker, cotacao_atual, moeda) VALUES (999998, 'ISOL3', 10, 'BRL')");
        String tokenAdmin = realizarLogin(USUARIO, SENHA);
        String compra = mockMvc.perform(post("/operacoes/comprar/999998")
                        .param("quantidade", "10").param("precoCompra", "10")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long idCompra = jsonMapper.readTree(compra).get("id").asLong();
        String tokenOutro = realizarLogin("outro_investidor", SENHA);
        mockMvc.perform(get("/operacoes/carteira").header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicoes").isEmpty());
        for (String caminho : List.of("/operacoes", "/operacoes/999998", "/operacoes/compras", "/operacoes/vendas")) {
            mockMvc.perform(get(caminho).header("Authorization", "Bearer " + tokenOutro))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        }
        mockMvc.perform(get("/operacoes/buscar/" + idCompra).header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/operacoes/vender/999998").param("quantidade", "1").param("precoVenda", "12")
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/operacoes/comprar/999998").param("quantidade", "2").param("precoCompra", "20")
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precoMedio").value(20));
        mockMvc.perform(get("/operacoes/carteira").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeTotal").value(10));
    }

    @Test
    void jwtAdulteradoRetorna401() throws Exception {
        String token = realizarLogin(USUARIO, SENHA);

        String[] partes = token.split("\\.");

        char primeiro = partes[2].charAt(0);
        partes[2] = (primeiro == 'A' ? "B" : "A")
                + partes[2].substring(1);

        String adulterado = String.join(".", partes);

        mockMvc.perform(get("/acoes")
                        .header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSemPapelAdminNaoPodeCadastrarAcao() throws Exception {
        String tokenUser = gerarTokenComPapelUser();

        mockMvc.perform(post("/acoes")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ticker": "PETR4",
                              "corretoraId": 1
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    private String realizarLogin(String username, String password)
            throws Exception {

        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonMapper.readTree(resposta)
                .get("token")
                .asText();
    }

    private String gerarTokenComPapelUser() {
        jdbcTemplate.update("UPDATE usuario SET papel = 'USER' WHERE username = ?", USUARIO);
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .subject(USUARIO)
                .claim("uid", "999999")
                .claim("sv", "0")
                .claim("authorities", List.of("ROLE_USER"))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    private String jsonLogin(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }
}
