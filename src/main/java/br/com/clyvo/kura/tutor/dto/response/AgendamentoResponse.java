package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Agendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Agendamento criado ou consultado pelo tutor")
public record AgendamentoResponse(
        @Schema(example = "1") Long idAgendamento,
        @Schema(example = "Rex") String nmPet,
        @Schema(example = "Clyvo Vet São Paulo") String nmClinica,
        @Schema(example = "2026-06-15T10:00:00") LocalDateTime dtAgendamento,
        @Schema(example = "30") Integer nrDuracaoMinutos,
        @Schema(example = "CONSULTA") String tipo,
        @Schema(example = "AGENDADO") String status,
        @Schema(example = "PORTAL") String origem,
        String observacoes,
        LocalDateTime dtCriacao
) {
    public static AgendamentoResponse from(Agendamento a) {
        String nmPet = a.getPet() != null ? a.getPet().getNmPet() : null;
        String nmClinica = a.getClinica() != null ? a.getClinica().getNmClinica() : null;

        return new AgendamentoResponse(
                a.getIdAgendamento(), nmPet, nmClinica,
                a.getDtAgendamento(), a.getNrDuracaoMinutos(),
                a.getDsTipo(), a.getStStatus(), a.getDsOrigem(),
                a.getDsObservacoes(), a.getDtCriacao()
        );
    }
}
