package com.av2.gestaodeacao.config;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void criarAdministradorInicial() {

        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }

        Usuario admin = new Usuario();

        admin.setUsername(username);
        admin.setSenha(passwordEncoder.encode(password));
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);
    }
}