package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados pessoais do tutor.
 * Cadastrado pela clinica via .NET. Java apenas le esta tabela.
 *
 * Equivalente ao model/Usuario.java do projeto de aula — ambos sao entidades JPA
 * com relacionamentos e campos de auditoria.
 */
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

    @Column(name = "nm_tutor", nullable = false, length = 120)
    private String nmTutor;

    @Column(name = "nr_cpf", nullable = false, unique = true, length = 14)
    private String nrCpf;

    @Column(name = "dt_nascimento")
    private LocalDate dtNascimento;

    @Column(name = "ds_email", nullable = false, unique = true, length = 120)
    private String dsEmail;

    @Column(name = "ds_telefone", nullable = false, length = 20)
    private String dsTelefone;

    @Column(name = "ds_whatsapp", length = 20)
    private String dsWhatsapp;

    @Column(name = "ds_endereco", length = 200)
    private String dsEndereco;

    @Column(name = "nm_cidade", length = 80)
    private String nmCidade;

    @Column(name = "sg_uf", length = 2)
    private String sgUf;

    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "st_ativo", nullable = false, length = 1)
    private String stAtivo = "S";

    // LGPD — transparencia do aviso de privacidade (preenchido no balcao pelo .NET)
    @Column(name = "st_aviso_privacidade", nullable = false, length = 1)
    private String stAvisoPrivacidade = "N";

    @Column(name = "dt_aviso_privacidade")
    private LocalDateTime dtAvisoPrivacidade;

    @Column(name = "ds_versao_aviso", length = 20)
    private String dsVersaoAviso;

    @OneToMany(mappedBy = "tutor", fetch = FetchType.LAZY)
    private List<Consentimento> consentimentos = new ArrayList<>();

    @OneToMany(mappedBy = "tutor", fetch = FetchType.LAZY)
    private List<Agendamento> agendamentos = new ArrayList<>();

    public Tutor() {}

    public Long getIdTutor() { return idTutor; }
    public Clinica getClinica() { return clinica; }
    public String getNmTutor() { return nmTutor; }
    public String getNrCpf() { return nrCpf; }
    public LocalDate getDtNascimento() { return dtNascimento; }
    public String getDsEmail() { return dsEmail; }
    public String getDsTelefone() { return dsTelefone; }
    public String getDsWhatsapp() { return dsWhatsapp; }
    public String getDsEndereco() { return dsEndereco; }
    public String getNmCidade() { return nmCidade; }
    public String getSgUf() { return sgUf; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public String getStAtivo() { return stAtivo; }
    public String getStAvisoPrivacidade() { return stAvisoPrivacidade; }
    public LocalDateTime getDtAvisoPrivacidade() { return dtAvisoPrivacidade; }
    public String getDsVersaoAviso() { return dsVersaoAviso; }
    public List<Consentimento> getConsentimentos() { return consentimentos; }
    public List<Agendamento> getAgendamentos() { return agendamentos; }

    public boolean isAtivo() { return "S".equals(stAtivo); }
    public boolean temAvisoPrivacidade() { return "S".equals(stAvisoPrivacidade); }
}
