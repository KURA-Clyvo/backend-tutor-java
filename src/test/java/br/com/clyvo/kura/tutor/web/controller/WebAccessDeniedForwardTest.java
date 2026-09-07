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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova de que {@code GET /web/suporte/contas} como TUTOR aciona o
 * {@code accessDeniedHandler} com {@code 403} e forward para
 * {@code /web/403} — SJ3-05, §3.3 do brief.
 *
 * 🔴 **MEDIDO, NÃO PRESUMIDO — o que este teste NÃO consegue observar, e por
 * quê (alcance do instrumento).** A hipótese inicial era que
 * {@code MockMvc} conseguiria simular o forward de aplicação explícito
 * ({@code request.getRequestDispatcher(...).forward(...)}, usado por este
 * handler) de um jeito que o forward de container do
 * {@code sendError()} default não permite. **Rodado e medido: FALSO.**
 * {@code MockMvc} registra QUE um forward foi pedido
 * ({@code forwardedUrl()} enxerga {@code "/web/403"}) mas **não o
 * executa** — não há segundo dispatch pelo {@code DispatcherServlet}, o
 * corpo da resposta fica vazio e o {@code Content-Type} fica {@code null}.
 * Uma assertiva de conteúdo (`content().string(containsString(...))`)
 * FALHA aqui com {@code "Content type not set"} mesmo com o forward
 * correto — não porque o código esteja errado, mas porque o ambiente de
 * servlet mocado do {@code MockMvc} não reexecuta NENHUM forward, seja ele
 * de container (o caso original da SJ3-03) ou de aplicação (este caso).
 *
 * ⇒ Este teste prova só o que {@code MockMvc} alcança: o {@code status}
 * (403) e o {@code forwardedUrl} pedido. **A prova de que o corpo
 * renderizado é a página 403 de verdade — com {@code sec:authorize}/
 * {@code sec:authentication} funcionando dentro do forward — só existe por
 * HTTP real** (§3.2 do brief, prova 6, `sj3-05-report.md` no repo de
 * planejamento): lá, `GET /api/web/suporte/contas` com cookie de TUTOR
 * devolveu {@code 403}, {@code Content-Type: text/html}, e o HTML de
 * {@code /web/403} com o nome do usuário renderizado corretamente.
 *
 * PROVA DE MUTAÇÃO (para o revisor reproduzir): remover a linha
 * {@code .requestMatchers("/web/suporte/**").hasRole("SUPORTE")} de
 * {@code WebSecurityConfig} faz {@link #tutorRecebe403EForwardParaPagina403()}
 * FALHAR (a rota deixa de barrar o TUTOR, status vira 200). Restaurar volta
 * ao verde — mesma mutação já registrada em
 * {@code WebSecurityConfigAuthorizationTest}, aplicada no MESMO lugar
 * (não num método vizinho que herdaria o atributo por acidente).
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebAccessDeniedForwardTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("tutorRecebe403EForwardParaPagina403 — TUTOR autenticado tenta /web/suporte/contas, recebe 403 com forward para /web/403")
    void tutorRecebe403EForwardParaPagina403() throws Exception {
        mockMvc.perform(get("/web/suporte/contas").with(user("tutor@teste.com").roles("TUTOR")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/web/403"));
    }
}
