package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "veterinario")
public class Veterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_veterinario")
    private Long idVeterinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "nm_veterinario", nullable = false, length = 150)
    private String nmVeterinario;

    @Column(name = "nr_crmv", unique = true, length = 20)
    private String nrCrmv;

    @Column(name = "ds_email", length = 150)
    private String dsEmail;

    @Column(name = "nr_telefone", length = 20)
    private String nrTelefone;

    @Column(name = "st_ativo", nullable = false, length = 1)
    private String stAtivo;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    protected Veterinario() {}

    public Long getIdVeterinario() { return idVeterinario; }
    public Clinica getClinica() { return clinica; }
    public String getNmVeterinario() { return nmVeterinario; }
    public String getNrCrmv() { return nrCrmv; }
    public String getDsEmail() { return dsEmail; }
    public String getNrTelefone() { return nrTelefone; }
    public String getStAtivo() { return stAtivo; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public boolean isAtivo() { return "S".equals(stAtivo); }
}
