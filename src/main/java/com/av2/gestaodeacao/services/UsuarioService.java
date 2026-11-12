
package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.domains.dtos.AlterarSenhaRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioCreateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioResponse;
import com.av2.gestaodeacao.domains.dtos.UsuarioUpdateRequest;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 8.1 - Criar usuário
    @Transactional
    public UsuarioResponse create(UsuarioCreateRequest request) {
        validarUsernameDisponivel(request.username(), null);

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setSenha(passwordEncoder.encode(request.password()));
        usuario.setPapel(request.papel());
        usuario.setAtivo(true);

        return toResponse(usuarioRepository.save(usuario));
    }

    // 8.2 - Alterar cadastro
    @Transactional
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request) {
        // Bloqueia em ordem estável para impedir dois rebaixamentos simultâneos.
        List<Usuario> usuarios = usuarioRepository.findAllForUpdate();
        Usuario usuario = findEntity(id);

        validarUsernameDisponivel(request.username(), id);

        if (usuario.getPapel() == Papel.ADMIN && Boolean.TRUE.equals(usuario.getAtivo())
                && (request.papel() != Papel.ADMIN || !Boolean.TRUE.equals(request.ativo()))
                && usuarios.stream().noneMatch(outro -> !outro.getId().equals(id)
                    && outro.getPapel() == Papel.ADMIN && Boolean.TRUE.equals(outro.getAtivo()))) {
            throw new ConflitoException(
                    "É necessário manter pelo menos um administrador ativo no sistema");
        }

        if (!java.util.Objects.equals(usuario.getUsername(), request.username())
                || usuario.getPapel() != request.papel()
                || !java.util.Objects.equals(usuario.getAtivo(), request.ativo())) {
            usuario.invalidarSessoes();
        }
        usuario.setUsername(request.username());
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());

        return toResponse(usuarioRepository.save(usuario));
    }

    // 8.3 - Alterar senha
    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest request) {
        Usuario usuario = usuarioRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        usuario.setSenha(passwordEncoder.encode(request.password()));
        usuario.invalidarSessoes();

        usuarioRepository.save(usuario);
    }

    // Consultar todos os usuários
    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Consultar usuário por ID
    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    // Buscar entidade internamente
    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));
    }

    // Validar duplicidade de username
    private void validarUsernameDisponivel(String username, Long idAtual) {
        usuarioRepository.findByUsername(username)
                .ifPresent(usuarioExistente -> {
                    if (idAtual == null ||
                            !usuarioExistente.getId().equals(idAtual)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Nome de usuário já cadastrado"
                        );
                    }
                });
    }

    // Converter entidade para DTO seguro
    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPapel(),
                usuario.getAtivo()
        );
    }
}
