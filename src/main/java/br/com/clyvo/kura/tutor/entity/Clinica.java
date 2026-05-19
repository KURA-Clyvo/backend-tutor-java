package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clinica veterinaria.
 * Escrita pelo .NET (Felipe). Java apenas le via EntityManager.getReference.
 * CORRECAO: classe deve ser public para EntityManager funcionar corretamente.
 */
@Entity
@Table(name = "clinica")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clinica")
    private Long idClinica;

    @Column(name = "nm_clinica", nullable = false, length = 120) private String nmClinica;
    @Column(name = "nr_cnpj", nullable = false, unique = true, length = 18) private String nrCnpj;
    @Column(name = "ds_endereco", nullable = false, length = 200) private String dsEndereco;
    @Column(name = "nm_cidade", nullable = false, length = 80) private String nmCidade;
    @Column(name = "sg_uf", nullable = false, length = 2) private String sgUf;
    @Column(name = "nr_cep", nullable = false, length = 9) private String nrCep;
    @Column(name = "ds_telefone", length = 20) private String dsTelefone;
    @Column(name = "ds_email", length = 120) private String dsEmail;
    @Column(name = "dt_cadastro", nullable = false, updatable = false) private LocalDateTime dtCadastro;
    @Column(name = "st_ativa", nullable = false, length = 1) private String stAtiva = "S";

    public Clinica() {}

    public Long getIdClinica() { return idClinica; }
    public String getNmClinica() { return nmClinica; }
    public String getNrCnpj() { return nrCnpj; }
    public String getNmCidade() { return nmCidade; }
    public String getSgUf() { return sgUf; }
    public String getDsTelefone() { return dsTelefone; }
    public String getDsEmail() { return dsEmail; }
    public boolean isAtiva() { return "S".equals(stAtiva); }
}
