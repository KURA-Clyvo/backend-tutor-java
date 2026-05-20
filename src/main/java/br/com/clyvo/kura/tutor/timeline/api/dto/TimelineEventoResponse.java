package br.com.clyvo.kura.tutor.timeline.api.dto;

import br.com.clyvo.kura.tutor.timeline.domain.TimelinePet;
import java.time.LocalDateTime;

public record TimelineEventoResponse(
        Long          idEvento,
        Long          idPet,
        String        nmPet,
        LocalDateTime dtEvento,
        String        dsTipoEvento,
        String        stStatus,
        Long          idClinica,
        String        nmClinica
) {
    public static TimelineEventoResponse fromEntity(TimelinePet t) {
        return new TimelineEventoResponse(
                t.getIdEvento(),
                t.getIdPet(),
                t.getNmPet(),
                t.getDtEvento(),
                t.getDsTipoEvento(),
                t.getStStatus(),
                t.getIdClinica(),
                t.getNmClinica());
    }
}
