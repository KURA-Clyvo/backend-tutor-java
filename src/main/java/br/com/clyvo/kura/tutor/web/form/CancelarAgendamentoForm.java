// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Comando de formulário do {@code POST /web/agendamentos/{id}/cancelar}
 * (SJ3-06). Motivo obrigatório — reflete a assinatura de
 * {@code AgendamentoService.cancelar(String, Long, String)} — validado com
 * Bean Validation e renderizado no próprio formulário via
 * {@code BindingResult}/{@code th:errors} (texto literal do §4 do brief da
 * SJ3-06: nunca alerta genérico, nunca página de erro).
 */
public class CancelarAgendamentoForm {

    @NotBlank(message = "Informe o motivo do cancelamento.")
    @Size(min = 5, max = 500, message = "O motivo deve ter entre 5 e 500 caracteres.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
