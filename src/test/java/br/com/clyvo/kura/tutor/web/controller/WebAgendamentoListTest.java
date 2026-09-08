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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /web/agendamentos} (SJ3-06) mostra só os agendamentos do
 * TUTOR autenticado. Prova de discriminação: o seed do afterMigrate (seção
 * 13, KURA-WEB) dá ao tutor de demonstração ({@code tutor@kura.demo},
 * {@code ID_TUTOR=2}) os agendamentos {@code ID_AGENDAMENTO=2,3} —
 * {@code ID_AGENDAMENTO=1} pertence a {@code ID_TUTOR=1} (seção 10 do
 * mesmo seed) e NÃO pode aparecer.
 *
 * A sonda com dois logins reais diferentes (HTTP, não MockMvc) está no
 * artefato desta task ({@code sj3-06-report.md}, repo de planejamento) —
 * aqui é a regressão automatizada equivalente, sobre um único login.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebAgendamentoListTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("tutorVeSoOsProprios — tutor@kura.demo vê os agendamentos 2 e 3, não vê o 1 (de outro tutor)")
    void tutorVeSoOsProprios() throws Exception {
        // PET 1 (Marley) é compartilhado entre os dois tutores no seed (item 13, TUTOR_PET),
        // então o nome do pet NÃO discrimina — o discriminante é o ID_AGENDAMENTO embutido no
        // href de cada ação, único por linha da tabela.
        mockMvc.perform(get("/web/agendamentos").with(user("tutor@kura.demo").roles("TUTOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/web/agendamentos/2/remarcar")))
                .andExpect(content().string(containsString("/web/agendamentos/3/remarcar")))
                .andExpect(content().string(not(containsString("/web/agendamentos/1/remarcar"))))
                .andExpect(content().string(not(containsString("/web/agendamentos/1/cancelar"))))
                .andExpect(content().string(not(containsString("Nenhum agendamento encontrado"))));
    }
}
