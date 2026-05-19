package br.com.clyvo.kura.tutor.entity;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity 
@Table(name = "especie")
public class Especie {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id_especie")
    @JsonProperty("idEspecie")
    private Long id; 

    @Column(name = "nm_especie", nullable = false, unique = true, length = 50) 
    private String nmEspecie;

    @OneToMany(mappedBy = "especie", fetch = FetchType.LAZY) 
    @JsonManagedReference 
    private List<Raca> racas = new ArrayList<>();

    public Especie() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNmEspecie() { return nmEspecie; }
    public void setNmEspecie(String nmEspecie) { this.nmEspecie = nmEspecie; }
    
    public List<Raca> getRacas() { return racas; }
    public void setRacas(List<Raca> racas) { this.racas = racas; }
}