package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Immutable
@Entity
@Table(name = "raca")
public class Raca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_raca")
    private Long idRaca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    @Column(name = "nm_raca", nullable = false, length = 80)
    private String nmRaca;

    @Column(name = "ds_predisposicao", length = 500)
    private String dsPredisposicao;

    protected Raca() {}

    public Long getIdRaca() { return idRaca; }
    public Especie getEspecie() { return especie; }
    public String getNmRaca() { return nmRaca; }
    public String getDsPredisposicao() { return dsPredisposicao; }
}
