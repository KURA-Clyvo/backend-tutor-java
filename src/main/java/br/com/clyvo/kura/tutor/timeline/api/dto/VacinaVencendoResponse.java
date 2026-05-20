package br.com.clyvo.kura.tutor.timeline.api.dto;

import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Vacina agendada vencendo nos próximos 30 dias")
public record VacinaVencendoResponse(
        @Schema(description = "ID do pet", example = "1") Long idPet,
        @Schema(description = "Nome do pet", example = "Marley") String nmPet,
        @Schema(description = "Nome da vacina", example = "Antirrábica") String nmVacina,
        @Schema(description = "Data/hora da próxima dose", example = "2026-06-15T09:00:00") LocalDateTime dtProximaDose,
        @Schema(description = "ID da clínica", example = "1") Long idClinica,
        @Schema(description = "Nome da clínica", example = "Clyvo Vet São Paulo") String nmClinica
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
