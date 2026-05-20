package br.com.clyvo.kura.tutor.timeline.domain;

import java.io.Serializable;
import java.util.Objects;

public class TimelinePetId implements Serializable {

    private Long idPet;
    private Long idEvento;

    public TimelinePetId() {}

    public TimelinePetId(Long idPet, Long idEvento) {
        this.idPet    = idPet;
        this.idEvento = idEvento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimelinePetId that)) return false;
        return Objects.equals(idPet, that.idPet) && Objects.equals(idEvento, that.idEvento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPet, idEvento);
    }
}
