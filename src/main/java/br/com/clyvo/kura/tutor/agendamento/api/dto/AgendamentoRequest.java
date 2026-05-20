package br.com.clyvo.kura.tutor.agendamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "Dados para criação de agendamento")
public record AgendamentoRequest(
    @Schema(description = "ID do pet do tutor", example = "1")
    @NotNull Long idPet,

    @Schema(description = "ID da clínica", example = "1")
    @NotNull Long idClinica,

    @Schema(description = "ID do veterinário (opcional)", example = "2")
    Long idVeterinario,

    @Schema(description = "Data e hora do agendamento (deve ser futura)", example = "2026-07-15T10:30:00")
    @NotNull @Future(message = "Agendamento deve ser no futuro")
    LocalDateTime dtAgendamento,

    @Schema(description = "Tipo de consulta",
            allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"},
            example = "CONSULTA")
    @NotBlank String tipo,

    @Schema(description = "Duração em minutos (5–480), padrão 30", example = "30")
    @Min(5) @Max(480) Integer duracaoMinutos,

    @Schema(description = "Observações adicionais do tutor")
    String observacoes
) {}
