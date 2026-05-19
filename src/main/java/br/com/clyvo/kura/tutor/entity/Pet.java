package br.com.clyvo.kura.tutor.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "pet")
public class Pet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_pet") private Long idPet;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_especie", nullable = false) private Especie especie;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_raca") private Raca raca;
    @Column(name = "nm_pet", nullable = false, length = 80) private String nmPet;
    @Column(name = "dt_nascimento") private LocalDate dtNascimento;
    @Column(name = "sg_sexo", nullable = false, length = 1) private String sgSexo;
    @Column(name = "nr_peso_kg", precision = 5, scale = 2) private BigDecimal nrPesoKg;
    @Column(name = "sg_porte", length = 1) private String sgPorte;
    @Column(name = "st_castrado", nullable = false, length = 1) private String stCastrado = "N";
    @Column(name = "ds_alergias", length = 500) private String dsAlergias;
    @Column(name = "ds_observacoes", length = 1000) private String dsObservacoes;
    @Column(name = "dt_cadastro", nullable = false, updatable = false) private LocalDateTime dtCadastro;
    @Column(name = "st_ativo", nullable = false, length = 1) private String stAtivo = "S";
    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY) private List<TutorPet> tutorPets = new ArrayList<>();
    public Pet() {}
    public Long getIdPet() { return idPet; }
    public Especie getEspecie() { return especie; }
    public Raca getRaca() { return raca; }
    public String getNmPet() { return nmPet; }
    public LocalDate getDtNascimento() { return dtNascimento; }
    public String getSgSexo() { return sgSexo; }
    public BigDecimal getNrPesoKg() { return nrPesoKg; }
    public String getSgPorte() { return sgPorte; }
    public String getStCastrado() { return stCastrado; }
    public String getDsAlergias() { return dsAlergias; }
    public String getDsObservacoes() { return dsObservacoes; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public List<TutorPet> getTutorPets() { return tutorPets; }
    public boolean isAtivo() { return "S".equals(stAtivo); }
}
