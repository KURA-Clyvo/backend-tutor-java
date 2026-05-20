package br.com.clyvo.kura.tutor.timeline.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class VacinaVencendoId implements Serializable {

    private Long          idPet;
    private String        nmVacina;
    private LocalDateTime dtProximaDose;

    public VacinaVencendoId() {}

    public VacinaVencendoId(Long idPet, String nmVacina, LocalDateTime dtProximaDose) {
        this.idPet         = idPet;
        this.nmVacina      = nmVacina;
        this.dtProximaDose = dtProximaDose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VacinaVencendoId that)) return false;
        return Objects.equals(idPet, that.idPet)
            && Objects.equals(nmVacina, that.nmVacina)
            && Objects.equals(dtProximaDose, that.dtProximaDose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPet, nmVacina, dtProximaDose);
    }
}
