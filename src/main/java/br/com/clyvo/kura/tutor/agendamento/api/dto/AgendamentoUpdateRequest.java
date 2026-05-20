package br.com.clyvo.kura.tutor.agendamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Dados para atualização de agendamento. nrVersion obrigatório para optimistic locking.")
public record AgendamentoUpdateRequest(
    @Schema(description = "Nova data/hora do agendamento (deve ser futura)", example = "2026-08-01T14:00:00")
    @Future(message = "Nova data deve ser no futuro")
    LocalDateTime dtAgendamento,

    @Schema(description = "Tipo de consulta",
            allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"},
            example = "RETORNO")
    String dsTipoConsulta,

    @Schema(description = "Observações do tutor")
    String dsObservacoes,

    @Schema(description = "ID do veterinário (opcional)", example = "2")
    Long idVeterinario,

    @Schema(description = "Versão atual do agendamento — obrigatório para evitar conflito (409)", example = "0")
    @NotNull(message = "nrVersion é obrigatório para evitar conflitos de versão")
    Long nrVersion
) {}
