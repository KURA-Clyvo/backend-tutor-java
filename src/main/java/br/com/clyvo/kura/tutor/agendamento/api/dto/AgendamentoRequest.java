package br.com.clyvo.kura.tutor.agendamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "Dados para criação de agendamento")
public record AgendamentoRequest(
    @NotNull Long idPet,
    @NotNull Long idClinica,
    Long idVeterinario,
    @Schema(example = "2026-07-15T10:30:00")
    @NotNull @Future(message = "Agendamento deve ser no futuro")
    LocalDateTime dtAgendamento,
    @Schema(allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"})
    @NotBlank String tipo,
    @Min(5) @Max(480) Integer duracaoMinutos,
    String observacoes
) {}
