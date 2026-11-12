package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperacaoAcaoRepository extends JpaRepository<OperacaoAcao, Long> {
    List<OperacaoAcao> findByUsuarioId(Long usuarioId);
    List<OperacaoAcao> findByAcaoAndTipoOperacaoAndUsuarioId(Acao acao, TipoOperacao tipoOperacao, Long usuarioId);
    List<OperacaoAcao> findByAcaoAndUsuarioId(Acao acao, Long usuarioId);
    List<OperacaoAcao> findByTipoOperacaoAndUsuarioId(TipoOperacao tipoOperacao, Long usuarioId);
    Optional<OperacaoAcao> findByIdAndUsuarioId(Long id, Long usuarioId);
}
