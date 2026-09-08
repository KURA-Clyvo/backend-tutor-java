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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reconfirmação da autorização de {@code /web/suporte/**} (§3 do brief da SJ3-07) para as DUAS
 * rotas novas desta task — o primeiro {@code POST} de suporte do projeto.
 * {@code WebSecurityConfigAuthorizationTest} (SJ3-03) já prova isso para o {@code GET} original;
 * esta classe estende a prova para {@code GET} com resultado real (busca) e para o {@code POST}
 * de destravar, com {@code _csrf} legítimo (via {@code SecurityMockMvcRequestPostProcessors.csrf()},
 * não vazio — a armadilha do §3 é medir rejeição de CSRF em vez de autorização de papel).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SuporteContasAutorizacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("tutorGetComBuscaRecebe403 — TUTOR autenticado, GET com busca, recebe 403")
    void tutorGetComBuscaRecebe403() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "tutor@kura.demo")
                        .with(user("tutor@kura.demo").roles("TUTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tutorPostDestravarRecebe403 — TUTOR autenticado, POST destravar com CSRF válido, recebe 403 (não 405, não 500)")
    void tutorPostDestravarRecebe403() throws Exception {
        mockMvc.perform(post("/web/suporte/contas/destravar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("idConta", "1")
                        .param("email", "tutor@kura.demo"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("suporteGetComBuscaAlcancaRota — SUPORTE autenticado alcança GET com busca (200)")
    void suporteGetComBuscaAlcancaRota() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "tutor@kura.demo")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk());
    }
}
