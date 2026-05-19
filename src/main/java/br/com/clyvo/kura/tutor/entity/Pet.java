package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Immutable
@Entity
@Table(name = "pet")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pet")
    private Long idPet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_raca")
    private Raca raca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario_resp")
    private Veterinario veterinarioResponsavel;

    @Column(name = "nm_pet", nullable = false, length = 80)
    private String nmPet;

    @Column(name = "dt_nascimento")
    private LocalDate dtNascimento;

    @Column(name = "sg_sexo", length = 1)
    private String sgSexo;

    @Column(name = "sg_porte", length = 1)
    private String sgPorte;

    @Column(name = "st_ativo", nullable = false, length = 1)
    private String stAtivo;

    @Column(name = "dt_criacao")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY)
    private List<TutorPet> tutorPets = new ArrayList<>();

    protected Pet() {}

    public Long getIdPet() { return idPet; }
    public Clinica getClinica() { return clinica; }
    public Especie getEspecie() { return especie; }
    public Raca getRaca() { return raca; }
    public Veterinario getVeterinarioResponsavel() { return veterinarioResponsavel; }
    public String getNmPet() { return nmPet; }
    public LocalDate getDtNascimento() { return dtNascimento; }
    public String getSgSexo() { return sgSexo; }
    public String getSgPorte() { return sgPorte; }
    public String getStAtivo() { return stAtivo; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public LocalDateTime getDtAtualizacao() { return dtAtualizacao; }
    public List<TutorPet> getTutorPets() { return tutorPets; }
    public boolean isAtivo() { return "S".equals(stAtivo); }
}
