// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /web/agendamentos/{id}/remarcar} (SJ3-06) — a cena central da
 * task: o lock otimista via {@code NR_VERSION} é o que separa este fluxo de
 * um CRUD simples (backlog, seção {@code SJ3-06}).
 *
 * <p><b>Como o conflito é produzido aqui, sem tocar internals:</b> duas
 * submissões sequenciais do MESMO {@code nrVersion} (0) — a primeira sucede
 * e bumpa a versão real para 1 (comportamento padrão de {@code @Version} do
 * JPA); a segunda, com o {@code nrVersion} agora desatualizado, reproduz
 * exatamente o cenário descrito no javadoc de {@code RemarcarAgendamentoForm}:
 * "formulário carregado uma vez, outra escrita aconteceu, o hidden enviado
 * no submit ficou velho". É o mesmo efeito de duas abas — sem precisar de
 * duas abas.
 *
 * <p>🔴 <b>{@code entityManager.flush()+clear()} entre as duas submissões é
 * OBRIGATÓRIO aqui, e não é só estilo.</b> Medido nesta task: SEM isso, as
 * duas chamadas MockMvc participam da MESMA transação/persistence context
 * de teste (via {@code @Transactional}), e {@code EntityManager.find()} (por
 * trás de {@code AgendamentoRepository.findById}) devolve a MESMA instância
 * gerenciada da primeira chamada sem ir ao banco — o {@code UPDATE} da
 * primeira submissão fica só enfileirado, {@code nrVersion} nunca é
 * incrementado em memória, e a segunda submissão "sucede" de novo (302) em
 * vez de 409. `flush()` força o UPDATE a sair (incrementando nrVersion na
 * entidade); `clear()` derruba o cache de 1º nível para a segunda chamada
 * realmente reconsultar. Ambos dentro da MESMA transação de teste — o
 * rollback ao final continua intacto.
 *
 * {@code @Transactional}: cada método usa um agendamento próprio (2 ou 3)
 * para não interferir entre si nem com {@link WebAgendamentoCancelarTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebAgendamentoRemarcarTest {

    private static final DateTimeFormatter FORMATO_FORM = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("dataPassadaMostraErroNoFormulario — @Future rejeita data no passado e reexibe o formulário com o erro")
    void dataPassadaMostraErroNoFormulario() throws Exception {
        String dataPassada = LocalDateTime.now().minusDays(1).format(FORMATO_FORM);
        mockMvc.perform(post("/web/agendamentos/2/remarcar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("novaData", dataPassada)
                        .param("nrVersion", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A nova data deve ser no futuro.")));
    }

    @Test
    @DisplayName("versaoAtualRemarcaERedireciona — POST com nrVersion correto remarca e volta para a lista")
    void versaoAtualRemarcaERedireciona() throws Exception {
        String dataFutura = LocalDateTime.now().plusDays(20).format(FORMATO_FORM);
        mockMvc.perform(post("/web/agendamentos/3/remarcar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("novaData", dataFutura)
                        .param("nrVersion", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/agendamentos"));
    }

    @Test
    @DisplayName("versaoDesatualizadaDa409ComoPagina — segunda submissão com o mesmo nrVersion antigo dá 409 renderizado como HTML")
    void versaoDesatualizadaDa409ComoPagina() throws Exception {
        String primeiraData = LocalDateTime.now().plusDays(21).format(FORMATO_FORM);
        String segundaData = LocalDateTime.now().plusDays(22).format(FORMATO_FORM);

        // Primeira submissão: nrVersion=0 é o valor real -> sucede e bumpa para 1.
        mockMvc.perform(post("/web/agendamentos/2/remarcar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("novaData", primeiraData)
                        .param("nrVersion", "0"))
                .andExpect(status().is3xxRedirection());

        // Força o UPDATE pendente a sair para o H2 e derruba o cache de 1º nível —
        // sem isto a segunda chamada reveria a MESMA entidade em memória, com
        // nrVersion ainda 0 (ver javadoc da classe).
        entityManager.flush();
        entityManager.clear();

        // Segunda submissão: MESMO nrVersion=0, agora desatualizado (real é 1) -> 409.
        mockMvc.perform(post("/web/agendamentos/2/remarcar")
                        .with(user("tutor@kura.demo").roles("TUTOR"))
                        .with(csrf())
                        .param("novaData", segundaData)
                        .param("nrVersion", "0"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString(
                        "Alguém alterou este agendamento enquanto você editava")));
    }
}
