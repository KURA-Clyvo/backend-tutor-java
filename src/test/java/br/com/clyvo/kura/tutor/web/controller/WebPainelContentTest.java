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
 * Prova de que {@code /web/painel} (SJ3-05) mostra conteúdo condicional por
 * perfil via {@code sec:authorize} — cada perfil enxerga só o bloco que lhe
 * pertence. Reproduz por {@code MockMvc} o que a prova 3/5 do §3.2 do brief
 * mediu por HTTP real (`sj3-05-report.md`, repo de planejamento).
 *
 * PROVA DE MUTAÇÃO (para o revisor reproduzir): trocar
 * {@code sec:authorize="hasRole('SUPORTE')"} por
 * {@code sec:authorize="hasRole('TUTOR')"} em {@code templates/web/painel.html}
 * no bloco {@code id="bloco-suporte"} deve fazer
 * {@link #suporteVeBlocoSuporteNaoVeBlocoTutor()} FALHAR (o bloco de suporte
 * deixa de aparecer para SUPORTE). Restaurar volta ao verde.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebPainelContentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("suporteVeBlocoSuporteNaoVeBlocoTutor — SUPORTE autenticado vê o bloco de suporte e não vê o de tutor")
    void suporteVeBlocoSuporteNaoVeBlocoTutor() throws Exception {
        mockMvc.perform(get("/web/painel").with(user("suporte@teste.com").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"bloco-suporte\"")))
                .andExpect(content().string(not(containsString("id=\"bloco-tutor\""))));
    }

    @Test
    @DisplayName("tutorVeBlocoTutorNaoVeBlocoSuporte — TUTOR autenticado vê o bloco de tutor e não vê o de suporte")
    void tutorVeBlocoTutorNaoVeBlocoSuporte() throws Exception {
        mockMvc.perform(get("/web/painel").with(user("tutor@teste.com").roles("TUTOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"bloco-tutor\"")))
                .andExpect(content().string(not(containsString("id=\"bloco-suporte\""))));
    }
}
