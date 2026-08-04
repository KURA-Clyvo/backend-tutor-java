package br.com.clyvo.kura.tutor.timeline.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumo de vacinação pendente do pet (derivado de VW_VACINAS_VENCENDO)")
public record VacinaStatusResponse(
        @Schema(description = "ID do pet", example = "1") Long idPet,
        @Schema(description = "Quantidade de vacinas agendadas nos próximos 30 dias", example = "1") long qtdPendentes,
        @Schema(description = "Data da próxima dose pendente — null se nenhuma", example = "2026-08-20T09:00:00")
                LocalDateTime dtProximaDose,
        @Schema(description = "EM_DIA se nenhuma pendência nos próximos 30 dias, ALERTA caso contrário",
                example = "ALERTA", allowableValues = {"EM_DIA", "ALERTA"}) String dsStatusGeral
) {}
