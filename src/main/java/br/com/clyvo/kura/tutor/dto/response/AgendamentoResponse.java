package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;

import java.time.LocalDateTime;

public record AgendamentoResponse(
    Long idAgendamento, Long idPet, String nmPet,
    Long idClinica, Long idVeterinario,
    LocalDateTime dtAgendamento, Integer nrDuracaoMinutos,
    String tipo, String status, String origem,
    String observacoes, LocalDateTime dtCriacao,
    Long nrVersion
) {
    public static AgendamentoResponse fromEntity(Agendamento a) {
        return new AgendamentoResponse(a.getIdAgendamento(),
            a.getPet() != null ? a.getPet().getIdPet() : null,
            a.getPet() != null ? a.getPet().getNmPet() : null,
            a.getClinica() != null ? a.getClinica().getIdClinica() : null,
            a.getIdVeterinario(), a.getDtAgendamento(), a.getNrDuracaoMinutos(),
            a.getDsTipoConsulta(),
            a.getStStatus() != null ? a.getStStatus().name() : null,
            a.getDsOrigem(),
            a.getDsObservacoes(), a.getDtCriacao(), a.getNrVersion());
    }
}
