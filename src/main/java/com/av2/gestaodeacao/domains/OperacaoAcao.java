package com.av2.gestaodeacao.domains;

import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Data
@Entity
public class OperacaoAcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantidade;
    private Double precoUnitario;
    private Double precoMedio;
    private Double lucroPrejuizo;
    private LocalDateTime dataOperacao;
    private Double valorTotal;

    @Enumerated(EnumType.STRING)
    private TipoOperacao tipoOperacao;

    @ManyToOne
    @JoinColumn(name = "acao_id")
    private Acao acao;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
