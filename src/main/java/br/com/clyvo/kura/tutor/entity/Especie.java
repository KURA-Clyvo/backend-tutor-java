package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Espécie animal (Cão, Gato, Ave, Réptil...).
 * Tabela de domínio — lida por ambos os backends.
 */
@Entity
@Table(name = "ESPECIE")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_especie")
    @SequenceGenerator(name = "seq_especie", sequenceName = "SEQ_ESPECIE", allocationSize = 1)
    @Column(name = "ID_ESPECIE")
    private Long idEspecie;

    @Column(name = "NM_ESPECIE", nullable = false, unique = true, length = 50)
    private String nmEspecie;

    @OneToMany(mappedBy = "especie", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Raca> racas = new ArrayList<>();
}
