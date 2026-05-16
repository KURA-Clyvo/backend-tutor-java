package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Raça de um animal, vinculada a uma {@link Especie}.
 * Tabela de domínio — lida por ambos os backends.
 */
@Entity
@Table(name = "RACA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Raca {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_raca")
    @SequenceGenerator(name = "seq_raca", sequenceName = "SEQ_RACA", allocationSize = 1)
    @Column(name = "ID_RACA")
    private Long idRaca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESPECIE", nullable = false)
    private Especie especie;

    @Column(name = "NM_RACA", nullable = false, length = 80)
    private String nmRaca;

    @Column(name = "DS_PREDISPOSICAO", length = 500)
    private String dsPredisposicao;
}
