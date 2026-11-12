package com.av2.gestaodeacao.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // 8.3 - Cadeia de filtros
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/auth/login").permitAll()
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 8.4 - Criptografia de senhas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 8.4 - Chave secreta JWT
    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    // 8.4 - Geração de tokens JWT
    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    // 8.4 - Validação de tokens JWT
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey secretKey,
            JwtProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer()));

        return decoder;
    }

    // 9 - Gerenciador de autenticação
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 9 - Conversão das permissões do JWT
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(UsuarioRepository usuarios) {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var usuario = usuarios.findByUsername(jwt.getSubject())
                    .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                    .orElseThrow(() -> new InvalidBearerTokenException("Sessão inválida"));
            if (!usuario.getId().toString().equals(jwt.getClaimAsString("uid"))
                    || !usuario.getVersaoSessao().toString().equals(jwt.getClaimAsString("sv"))) {
                throw new InvalidBearerTokenException("Sessão inválida. Entre novamente");
            }
            var authorities = authoritiesConverter.convert(jwt).stream()
                    .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                    .toList();
            String papelAtual = "ROLE_" + usuario.getPapel().name();
            if (authorities.size() != 1 || authorities.stream()
                    .noneMatch(authority -> papelAtual.equals(authority.getAuthority()))) {
                throw new InvalidBearerTokenException("Permissões alteradas. Entre novamente");
            }
            return authorities;
        });

        return jwtConverter;
    }
}
