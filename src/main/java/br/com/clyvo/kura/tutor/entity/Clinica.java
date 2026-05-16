package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clínica veterinária.
 * Escrita pelo .NET (Felipe). Java apenas lê.
 */
@Entity
@Table(name = "CLINICA")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_clinica")
    @SequenceGenerator(name = "seq_clinica", sequenceName = "SEQ_CLINICA", allocationSize = 1)
    @Column(name = "ID_CLINICA")
    private Long idClinica;

    @Column(name = "NM_CLINICA", nullable = false, length = 120)
    private String nmClinica;

    @Column(name = "NR_CNPJ", nullable = false, unique = true, length = 18)
    private String nrCnpj;

    @Column(name = "NM_RAZAO_SOCIAL", length = 150)
    private String nmRazaoSocial;

    @Column(name = "DS_ENDERECO", nullable = false, length = 200)
    private String dsEndereco;

    @Column(name = "NM_CIDADE", nullable = false, length = 80)
    private String nmCidade;

    @Column(name = "SG_UF", nullable = false, length = 2)
    private String sgUf;

    @Column(name = "NR_CEP", nullable = false, length = 9)
    private String nrCep;

    @Column(name = "DS_TELEFONE", length = 20)
    private String dsTelefone;

    @Column(name = "DS_EMAIL", length = 120)
    private String dsEmail;

    @Column(name = "DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "ST_ATIVA", nullable = false, length = 1)
    private String stAtiva = "S";

    public Clinica() {}

    // Getters essenciais para o backend Java
    public Long getIdClinica() { return idClinica; }
    public String getNmClinica() { return nmClinica; }
    public String getNrCnpj() { return nrCnpj; }
    public String getNmCidade() { return nmCidade; }
    public String getSgUf() { return sgUf; }
    public String getDsTelefone() { return dsTelefone; }
    public String getDsEmail() { return dsEmail; }
    public boolean isAtiva() { return "S".equals(stAtiva); }
}
