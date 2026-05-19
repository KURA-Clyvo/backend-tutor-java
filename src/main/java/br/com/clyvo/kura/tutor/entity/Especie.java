package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Immutable
@Entity
@Table(name = "especie")
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie")
    private Long idEspecie;

    @Column(name = "nm_especie", nullable = false, unique = true, length = 50)
    private String nmEspecie;

    protected Especie() {}

    public Long getIdEspecie() { return idEspecie; }
    public String getNmEspecie() { return nmEspecie; }
}
