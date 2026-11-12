package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository  extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u order by u.id")
    List<Usuario> findAllForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.username = :username")
    Optional<Usuario> findForUpdateByUsername(@org.springframework.data.repository.query.Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findForUpdateById(@org.springframework.data.repository.query.Param("id") Long id);
}
