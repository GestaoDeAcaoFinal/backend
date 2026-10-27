package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository  extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByUsername(String username);
}
