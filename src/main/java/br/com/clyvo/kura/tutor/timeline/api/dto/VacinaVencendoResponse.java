package br.com.clyvo.kura.tutor.timeline.api.dto;

import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendo;
import java.time.LocalDateTime;

public record VacinaVencendoResponse(
        Long          idPet,
        String        nmPet,
        String        nmVacina,
        LocalDateTime dtProximaDose,
        Long          idClinica,
        String        nmClinica
) {
    public static VacinaVencendoResponse fromEntity(VacinaVencendo v) {
        return new VacinaVencendoResponse(
                v.getIdPet(),
                v.getNmPet(),
                v.getNmVacina(),
                v.getDtProximaDose(),
                v.getIdClinica(),
                v.getNmClinica());
    }
}
