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
 * {@code GET /web/suporte/contas} (SJ3-07) — busca por pessoa, §2.2/§5 (critérios 3 e 4) do
 * brief. O seed de dev dá conta a um tutor e convite a outro (ver
 * {@code afterMigrate__seeds_dev.sql}, seções 6/7/12): {@code felipe@clyvo.vet} (ID_TUTOR=1) tem
 * {@code INVITE_TUTOR} e NÃO tem {@code CONTA_TUTOR}; {@code tutor@kura.demo} (login, ID_TUTOR=2)
 * tem {@code CONTA_TUTOR} e NÃO tem convite. Cada teste abaixo é o discriminante do outro —
 * nenhum dos dois sozinho prova que a busca aceita os DOIS caminhos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SuporteContasBuscaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("buscaPorLoginDeContaMostraContaSemConvitePendente — tutor@kura.demo (login) acha conta, não acha convite")
    void buscaPorLoginDeContaMostraContaSemConvitePendente() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "tutor@kura.demo")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Destravar agora")))
                .andExpect(content().string(containsString("Nenhum convite pendente")))
                .andExpect(content().string(not(containsString("Convite pendente —"))));
    }

    @Test
    @DisplayName("buscaPorEmailDeTutorMostraConviteSemConta — felipe@clyvo.vet (e-mail de tutor) acha convite, não acha conta")
    void buscaPorEmailDeTutorMostraConviteSemConta() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "felipe@clyvo.vet")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("550e8400-e29b-41d4-a716-446655440000")))
                .andExpect(content().string(containsString("WHATSAPP")))
                .andExpect(content().string(containsString("ainda não criou conta")))
                .andExpect(content().string(not(containsString("Destravar agora"))));
    }

    @Test
    @DisplayName("buscaPorEmailInexistenteMostraMensagemSemStackTrace — pessoa inexistente NÃO derruba a página")
    void buscaPorEmailInexistenteMostraMensagemSemStackTrace() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "naoexiste@teste.com")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma pessoa encontrada")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.com.clyvo"))));
    }

    @Test
    @DisplayName("buscaComEmailFormatoInvalidoMostraMensagem — formato inválido não vira query nem exceção")
    void buscaComEmailFormatoInvalidoMostraMensagem() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .param("email", "nao-e-email")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("formato válido")));
    }

    @Test
    @DisplayName("semParametroEmailMostraFormularioVazio — GET sem busca não tenta resolver pessoa nenhuma")
    void semParametroEmailMostraFormularioVazio() throws Exception {
        mockMvc.perform(get("/web/suporte/contas")
                        .with(user("suporte@kura.demo").roles("SUPORTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Nenhuma pessoa encontrada"))))
                .andExpect(content().string(not(containsString("Destravar agora"))));
    }
}
