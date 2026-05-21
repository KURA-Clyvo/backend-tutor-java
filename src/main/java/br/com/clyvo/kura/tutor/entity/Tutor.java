package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "tutor")
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor")
    private Long idTutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "nm_tutor", nullable = false, length = 200)
    private String nmTutor;

    @Column(name = "nr_cpf", nullable = false, unique = true, length = 11)
    private String nrCpf;

    @Column(name = "ds_email", nullable = false, unique = true, length = 150)
    private String dsEmail;

    @Column(name = "ds_telefone", nullable = false, length = 20)
    private String nrTelefone;

    @Column(name = "ds_whatsapp", length = 20)
    private String dsWhatsapp;

    @Column(name = "dt_nascimento")
    private LocalDate dtNascimento;

    @Column(name = "ds_endereco", length = 200)
    private String dsEndereco;

    @Column(name = "nm_cidade", length = 80)
    private String nmCidade;

    @Column(name = "sg_uf", length = 2, columnDefinition = "CHAR(2)")
    private String sgUf;

    @Column(name = "nr_cep", length = 9)
    private String nrCep;

    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "st_ativo", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String stAtivo;

    @Column(name = "st_aviso_privacidade", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String stAvisoPrivacidade;

    @Column(name = "dt_aviso_privacidade")
    private LocalDateTime dtAvisoPrivacidade;

    @Column(name = "ds_versao_aviso", length = 20)
    private String dsVersaoAviso;

    protected Tutor() {}

    public Long getIdTutor() { return idTutor; }
    public Clinica getClinica() { return clinica; }
    public String getNmTutor() { return nmTutor; }
    public String getNrCpf() { return nrCpf; }
    public String getDsEmail() { return dsEmail; }
    public String getNrTelefone() { return nrTelefone; }
    public String getDsWhatsapp() { return dsWhatsapp; }
    public LocalDate getDtNascimento() { return dtNascimento; }
    public String getDsEndereco() { return dsEndereco; }
    public String getNmCidade() { return nmCidade; }
    public String getSgUf() { return sgUf; }
    public String getNrCep() { return nrCep; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public LocalDateTime getDtAtualizacao() { return dtAtualizacao; }
    public String getStAtivo() { return stAtivo; }
    public String getStAvisoPrivacidade() { return stAvisoPrivacidade; }
    public LocalDateTime getDtAvisoPrivacidade() { return dtAvisoPrivacidade; }
    public String getDsVersaoAviso() { return dsVersaoAviso; }

    public boolean isAtivo() { return "S".equals(stAtivo); }
    public boolean temAvisoPrivacidade() { return "S".equals(stAvisoPrivacidade); }
}
