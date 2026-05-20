package br.com.clyvo.kura.tutor.timeline.api.dto;

import br.com.clyvo.kura.tutor.timeline.domain.TimelinePet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Evento clínico na timeline do pet")
public record TimelineEventoResponse(
        @Schema(description = "ID do evento clínico", example = "5") Long idEvento,
        @Schema(description = "ID do pet", example = "1") Long idPet,
        @Schema(description = "Nome do pet", example = "Marley") String nmPet,
        @Schema(description = "Data/hora do evento", example = "2026-06-01T10:00:00") LocalDateTime dtEvento,
        @Schema(description = "Tipo do evento", example = "CONSULTA",
                allowableValues = {"CONSULTA","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"}) String dsTipoEvento,
        @Schema(description = "Status do evento", example = "REALIZADO") String stStatus,
        @Schema(description = "ID da clínica", example = "1") Long idClinica,
        @Schema(description = "Nome da clínica", example = "Clyvo Vet São Paulo") String nmClinica
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
