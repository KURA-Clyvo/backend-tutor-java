package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "clinica")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clinica")
    private Long idClinica;

    @Column(name = "nm_clinica", nullable = false, length = 120)
    private String nmClinica;

    @Column(name = "nr_cnpj", nullable = false, unique = true, length = 18)
    private String nrCnpj;

    @Column(name = "ds_endereco", length = 200)
    private String dsEndereco;

    @Column(name = "nm_cidade", length = 80)
    private String nmCidade;

    @Column(name = "sg_uf", length = 2, columnDefinition = "CHAR(2)")
    private String sgUf;

    @Column(name = "nr_cep", length = 9)
    private String nrCep;

    @Column(name = "ds_telefone", length = 20)
    private String dsTelefone;

    @Column(name = "ds_email", length = 120)
    private String dsEmail;

    @Column(name = "ds_email_acesso", unique = true, length = 120)
    private String dsEmailAcesso;

    @Column(name = "ds_senha_hash", length = 256)
    private String dsSenhaHash;

    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "st_ativa", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String stAtiva;

    protected Clinica() {}

    public Long getIdClinica() { return idClinica; }
    public String getNmClinica() { return nmClinica; }
    public String getNrCnpj() { return nrCnpj; }
    public String getDsEndereco() { return dsEndereco; }
    public String getNmCidade() { return nmCidade; }
    public String getSgUf() { return sgUf; }
    public String getNrCep() { return nrCep; }
    public String getDsTelefone() { return dsTelefone; }
    public String getDsEmail() { return dsEmail; }
    public String getDsEmailAcesso() { return dsEmailAcesso; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public String getStAtiva() { return stAtiva; }
    public boolean isAtiva() { return "S".equals(stAtiva); }
}
