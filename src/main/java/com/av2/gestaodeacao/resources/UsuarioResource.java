
package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.dtos.AlterarSenhaRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioCreateRequest;
import com.av2.gestaodeacao.domains.dtos.UsuarioResponse;
import com.av2.gestaodeacao.domains.dtos.UsuarioUpdateRequest;
import com.av2.gestaodeacao.services.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class UsuarioResource {

    private final UsuarioService usuarioService;

    public UsuarioResource(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Listar todos os usuários
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    // Consultar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @PathVariable
            @Positive(message = "Id deve ser positivo") Long id
    ) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    // Cadastrar usuário
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody UsuarioCreateRequest request
    ) {
        UsuarioResponse usuario = usuarioService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    // Alterar dados cadastrais
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable
            @Positive(message = "Id deve ser positivo") Long id,

            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    // Alterar senha
    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable
            @Positive(message = "Id deve ser positivo") Long id,

            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        usuarioService.alterarSenha(id, request);

        return ResponseEntity.noContent().build();
    }
}
