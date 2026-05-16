package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Associação N:N entre {@link Tutor} e {@link Pet}.
 * Suporta múltiplos tutores por pet (casal compartilhando guarda, p.ex.).
 */
@Entity
@Table(name = "TUTOR_PET")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutorPet {

    @EmbeddedId
    private TutorPetId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idTutor")
    @JoinColumn(name = "ID_TUTOR")
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPet")
    @JoinColumn(name = "ID_PET")
    private Pet pet;

    @Column(name = "DS_VINCULO", nullable = false, length = 40)
    @Builder.Default
    private String dsVinculo = "PROPRIETARIO";

    @Column(name = "DT_VINCULO", nullable = false)
    private LocalDateTime dtVinculo;

    /** S = tutor principal (recebe notificações da Luna). */
    @Column(name = "ST_PRINCIPAL", nullable = false, length = 1)
    @Builder.Default
    private String stPrincipal = "S";

    // ── Chave composta ────────────────────────────────────────────────────

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class TutorPetId implements Serializable {

        @Column(name = "ID_TUTOR")
        private Long idTutor;

        @Column(name = "ID_PET")
        private Long idPet;
    }
}
