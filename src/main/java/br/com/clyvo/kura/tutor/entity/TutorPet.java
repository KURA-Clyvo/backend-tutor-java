package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Vinculo N:N entre Tutor e Pet.
 * Um pet pode ter multiplos tutores (casal compartilhando guarda).
 * ST_PRINCIPAL='S' indica quem recebe notificacoes da Luna.
 */
@Entity
@Table(name = "tutor_pet")
public class TutorPet {

    @EmbeddedId
    private TutorPetId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idTutor")
    @JoinColumn(name = "id_tutor")
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPet")
    @JoinColumn(name = "id_pet")
    private Pet pet;

    @Column(name = "ds_vinculo", nullable = false, length = 40)
    private String dsVinculo = "PROPRIETARIO";

    @Column(name = "dt_vinculo", nullable = false)
    private LocalDateTime dtVinculo;

    @Column(name = "st_principal", nullable = false, length = 1)
    private String stPrincipal = "S";

    public TutorPet() {}

    public TutorPetId getId() { return id; }
    public Tutor getTutor() { return tutor; }
    public Pet getPet() { return pet; }
    public String getDsVinculo() { return dsVinculo; }
    public LocalDateTime getDtVinculo() { return dtVinculo; }
    public String getStPrincipal() { return stPrincipal; }
    public boolean isPrincipal() { return "S".equals(stPrincipal); }

    @Embeddable
    public static class TutorPetId implements Serializable {
        @Column(name = "id_tutor") private Long idTutor;
        @Column(name = "id_pet")   private Long idPet;
        public TutorPetId() {}
        public TutorPetId(Long idTutor, Long idPet) { this.idTutor = idTutor; this.idPet = idPet; }
        public Long getIdTutor() { return idTutor; }
        public Long getIdPet()   { return idPet; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TutorPetId)) return false;
            TutorPetId that = (TutorPetId) o;
            return java.util.Objects.equals(idTutor, that.idTutor) &&
                   java.util.Objects.equals(idPet, that.idPet);
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(idTutor, idPet);
        }
    }
}
