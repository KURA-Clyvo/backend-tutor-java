package br.com.clyvo.kura.tutor.notificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * NOTIFICACAO é .NET owned (ver CLAUDE.md — Tabelas e ownership). O Java só lê.
 * {@code @Immutable} garante em nível de Hibernate que esta entidade nunca é
 * incluída em um flush — qualquer tentativa de save/merge falha em runtime.
 * Nenhum repositório desta feature deve declarar métodos {@code @Modifying}.
 */
@Immutable
@Entity
@Table(name = "NOTIFICACAO")
public class Notificacao {

    @Id
    @Column(name = "ID_NOTIFICACAO")
    private Long idNotificacao;

    @Column(name = "ID_TUTOR")
    private Long idTutor;

    @Column(name = "DS_TITULO")
    private String dsTitulo;

    @Column(name = "DS_MENSAGEM")
    private String dsMensagem;

    @Column(name = "ST_LIDA")
    private String stLida;

    @Column(name = "DT_LEITURA")
    private LocalDateTime dtLeitura;

    @Column(name = "DT_CRIACAO")
    private LocalDateTime dtCriacao;

    protected Notificacao() {}

    public Long getIdNotificacao()     { return idNotificacao; }
    public Long getIdTutor()           { return idTutor; }
    public String getDsTitulo()        { return dsTitulo; }
    public String getDsMensagem()      { return dsMensagem; }
    public String getStLida()          { return stLida; }
    public LocalDateTime getDtLeitura()  { return dtLeitura; }
    public LocalDateTime getDtCriacao()  { return dtCriacao; }
    public boolean isLida()            { return "S".equals(stLida); }
}
