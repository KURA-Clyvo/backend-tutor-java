package br.com.clyvo.kura.tutor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "Dados para criação de agendamento pelo portal do tutor")
public record AgendamentoRequest(

        @Schema(example = "1") @NotNull Long idPet,
        @Schema(example = "1") @NotNull Long idClinica,
        @Schema(example = "2", description = "Opcional — veterinário preferencial") Long idVeterinario,

        @Schema(example = "2026-06-15T10:00:00")
        @NotNull(message = "A data/hora do agendamento é obrigatória.")
        @Future(message = "O agendamento deve ser em uma data futura.")
        LocalDateTime dtAgendamento,

        @Schema(example = "30", description = "Duração em minutos (5–480)")
        @Min(value = 5, message = "Duração mínima é 5 minutos.")
        @Max(value = 480, message = "Duração máxima é 480 minutos (8h).")
        Integer nrDuracaoMinutos,

        @Schema(example = "CONSULTA",
                allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"})
        @NotBlank(message = "O tipo do agendamento é obrigatório.")
        @Pattern(regexp = "CONSULTA|RETORNO|VACINA|EXAME|PROCEDIMENTO|TELEORIENTACAO",
                message = "Tipo de agendamento inválido.")
        String tipo,

        @Schema(example = "Rex está com tosse há 3 dias.")
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String observacoes
) {}
