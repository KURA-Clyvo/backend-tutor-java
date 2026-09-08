// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /web/agendamentos/{id}/cancelar} (SJ3-06) — validação de
 * formulário do §4 do brief: motivo obrigatório, erro renderizado NO
 * FORMULÁRIO via {@code BindingResult}/{@code th:errors}, nunca alerta
 * genérico. Também prova o caso válido (§6 critério 3 do brief: "submeta
 * um caso válido também, senão a validação não está discriminando").
 *
 * {@code @Transactional} + rollback: cada teste usa o {@code AGENDAMENTO}
 * seedado (2 ou 3), e cancelar é uma mudança de estado que não deve
 * vazar entre os métodos de teste desta classe nem para as outras.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebAgendamentoCancelarTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("motivoVazioMostraErroNoFormulario — POST sem motivo NÃO cancela e reexibe o formulário com o erro")
    void motivoVazioMostraErroNoFormulario() throws Exception {
        mockMvc.perform(post("/web/agendamentos/2/cancelar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("motivo", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe o motivo do cancelamento.")));
    }

    @Test
    @DisplayName("motivoValidoCancelaERedireciona — POST com motivo válido cancela e volta para a lista")
    void motivoValidoCancelaERedireciona() throws Exception {
        mockMvc.perform(post("/web/agendamentos/3/cancelar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("motivo", "Imprevisto pessoal, preciso cancelar esta consulta."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/agendamentos"));
    }
}
