package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario nao encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .roles(usuario.getPapel().name())
                .disabled(!usuario.getAtivo())
                .build();
    }
}