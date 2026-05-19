package br.com.clyvo.kura.tutor.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity 
@Table(name = "raca")
public class Raca {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id_raca") 
    private Long idRaca;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "id_especie", nullable = false) 
    @JsonBackReference 
    private Especie especie;

    @Column(name = "nm_raca", nullable = false, length = 80) 
    private String nmRaca;

    @Column(name = "ds_predisposicao", length = 500) 
    private String dsPredisposicao;

    public Raca() {}

    public Long getIdRaca() { return idRaca; }
    public void setIdRaca(Long idRaca) { this.idRaca = idRaca; }

    public Especie getEspecie() { return especie; }
    public void setEspecie(Especie especie) { this.especie = especie; }

    public String getNmRaca() { return nmRaca; }
    public void setNmRaca(String nmRaca) { this.nmRaca = nmRaca; }

    public String getDsPredisposicao() { return dsPredisposicao; }
    public void setDsPredisposicao(String dsPredisposicao) { this.dsPredisposicao = dsPredisposicao; }
}