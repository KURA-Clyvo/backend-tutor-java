// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Comando de formulário do {@code POST /web/agendamentos/{id}/remarcar}
 * (SJ3-06). {@code nrVersion} viaja como campo hidden, pré-carregado no GET
 * com o valor ATUAL do agendamento (ver
 * {@code WebAgendamentoController.formRemarcar}) — é essa cópia que fica
 * desatualizada e produz 409 ({@code ObjectOptimisticLockingFailureException},
 * lançada em {@code AgendamentoService.atualizar}, linha 109) quando outra
 * escrita altera a linha entre o carregamento do formulário e o submit.
 *
 * {@code @Future} em vez de data literal (armadilha registrada no
 * `CLAUDE.md` — data cravada expira sozinha em teste); os testes usam
 * {@code LocalDateTime.now().plusDays(n)}.
 */
public class RemarcarAgendamentoForm {

    @NotNull(message = "Informe a nova data e hora.")
    @Future(message = "A nova data deve ser no futuro.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dtAgendamento;

    @NotNull
    private Long nrVersion;

    public LocalDateTime getDtAgendamento() {
        return dtAgendamento;
    }

    public void setDtAgendamento(LocalDateTime dtAgendamento) {
        this.dtAgendamento = dtAgendamento;
    }

    public Long getNrVersion() {
        return nrVersion;
    }

    public void setNrVersion(Long nrVersion) {
        this.nrVersion = nrVersion;
    }
}
