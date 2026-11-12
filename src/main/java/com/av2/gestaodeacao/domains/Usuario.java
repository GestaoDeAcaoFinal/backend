package com.av2.gestaodeacao.domains;

import com.av2.gestaodeacao.domains.enums.Papel;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
@SequenceGenerator(
        name = "seq_usuario",
        sequenceName = "seq_usuario",
        allocationSize = 1
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_usuario")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(nullable = false)
    private Long versaoSessao = 0L;

    public Long getVersaoSessao() { return versaoSessao; }

    public void invalidarSessoes() { versaoSessao++; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
