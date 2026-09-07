// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova de que {@code .requestMatchers("/web/suporte/**").hasRole("SUPORTE")}
 * (em {@link WebSecurityConfig}) governa de verdade a rota
 * {@code GET /web/suporte/contas} — SJ3-03, §5 do brief.
 *
 * PROVA DE MUTAÇÃO (registrada aqui para o revisor reproduzir, e no artefato
 * da task com as duas contagens): comentar/remover a linha
 * {@code .requestMatchers("/web/suporte/**").hasRole("SUPORTE")} de
 * {@code WebSecurityConfig.webSecurityFilterChain(...)} e rodar esta classe —
 * {@link #tutorAutenticadoRecebeQuandoRoleExigida()} deve FALHAR (o TUTOR
 * passa a alcançar a rota, 200 em vez de 403). Restaurar a linha faz o teste
 * voltar a passar. Setup verificado como o CERTO: a regra removida é a MESMA
 * regra que protege exatamente a rota testada aqui — não uma regra de outro
 * método que a rota testada herdaria por acidente (armadilha já registrada
 * neste projeto, README.md regra 15).
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("tutorAutenticadoRecebeQuandoRoleExigida — TUTOR autenticado tenta /web/suporte/contas e recebe 403")
    void tutorAutenticadoRecebeQuandoRoleExigida() throws Exception {
        mockMvc.perform(get("/web/suporte/contas").with(user("tutor@teste.com").roles("TUTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("suporteAutenticadoAlcancaRota — SUPORTE autenticado tenta /web/suporte/contas e alcança (200)")
    void suporteAutenticadoAlcancaRota() throws Exception {
        mockMvc.perform(get("/web/suporte/contas").with(user("suporte@teste.com").roles("SUPORTE")))
                .andExpect(status().isOk());
    }
}
