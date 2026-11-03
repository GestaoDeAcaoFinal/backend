
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void usuarioSemPapelAdminNaoPodeExcluir() throws Exception {
        String tokenUser = gerarTokenComPapelUser();

        mockMvc.perform(delete("/acoes/1")
                        .header("Authorization", "Bearer " + tokenUser))
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
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .subject("usuario_comum")
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
