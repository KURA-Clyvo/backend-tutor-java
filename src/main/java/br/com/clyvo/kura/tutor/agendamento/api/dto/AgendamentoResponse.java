package br.com.clyvo.kura.tutor.agendamento.api.dto;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados completos de um agendamento")
public record AgendamentoResponse(
    @Schema(description = "ID do agendamento", example = "1") Long idAgendamento,
    @Schema(description = "ID do tutor", example = "1") Long idTutor,
    @Schema(description = "ID do pet", example = "1") Long idPet,
    @Schema(description = "Nome do pet", example = "Marley") String nmPet,
    @Schema(description = "ID da clínica", example = "1") Long idClinica,
    @Schema(description = "ID do veterinário responsável", example = "2") Long idVeterinario,
    @Schema(description = "Data e hora do agendamento", example = "2026-07-15T10:30:00") LocalDateTime dtAgendamento,
    @Schema(description = "Duração em minutos", example = "30") Integer nrDuracaoMinutos,
    @Schema(description = "Tipo de consulta", example = "CONSULTA",
            allowableValues = {"CONSULTA","RETORNO","VACINA","EXAME","PROCEDIMENTO","TELEORIENTACAO"}) String tipo,
    @Schema(description = "Status do agendamento", example = "AGENDADO",
            allowableValues = {"INTENCAO","AGENDADO","CONFIRMADO","REALIZADO","CANCELADO","NAO_COMPARECEU"}) String status,
    @Schema(description = "Canal de origem", example = "PORTAL") String origem,
    @Schema(description = "Observações do tutor") String observacoes,
    @Schema(description = "Data/hora de criação") LocalDateTime dtCriacao,
    @Schema(description = "Data/hora de confirmação") LocalDateTime dtConfirmacao,
    @Schema(description = "Data/hora de cancelamento") LocalDateTime dtCancelamento,
    @Schema(description = "Versão para optimistic locking — enviar no PUT para evitar conflito 409", example = "0") Long nrVersion,
    @Schema(description = "URL da sala de teleconsulta (Daily.co), preenchida pelo .NET após a criação. Null se ainda não criada.")
        String dsSalaUrl
) {
    public static AgendamentoResponse fromEntity(Agendamento a) {
        return new AgendamentoResponse(
            a.getIdAgendamento(),
            a.getTutor() != null ? a.getTutor().getIdTutor() : null,
            a.getPet()   != null ? a.getPet().getIdPet()     : null,
            a.getPet()   != null ? a.getPet().getNmPet()     : null,
            a.getClinica() != null ? a.getClinica().getIdClinica() : null,
            a.getIdVeterinario(),
            a.getDtAgendamento(),
            a.getNrDuracaoMinutos(),
            a.getDsTipoConsulta(),
            a.getStStatus() != null ? a.getStStatus().name() : null,
            a.getDsOrigem(),
            a.getDsObservacoes(),
            a.getDtCriacao(),
            a.getDtConfirmacao(),
            a.getDtCancelamento(),
            a.getNrVersion(),
            a.getDsSalaUrl()
        );
    }
}
