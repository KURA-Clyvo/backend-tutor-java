package br.com.clyvo.kura.tutor.agendamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Dados para atualização de agendamento. nrVersion obrigatório para optimistic locking.")
public record AgendamentoUpdateRequest(
    @Schema(example = "2026-08-01T14:00:00")
    @Future(message = "Nova data deve ser no futuro")
    LocalDateTime dtAgendamento,

    @Schema(allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"})
    String dsTipoConsulta,

    String dsObservacoes,

    Long idVeterinario,

    @NotNull(message = "nrVersion é obrigatório para evitar conflitos de versão")
    Long nrVersion
) {}
