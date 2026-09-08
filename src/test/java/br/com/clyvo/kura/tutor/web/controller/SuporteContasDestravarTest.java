// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /web/suporte/contas/destravar} (SJ3-07) — §2.3/§8.2/§5 (critério 2) do brief.
 *
 * <p><b>O bloqueio do setup é RECENTE, de propósito</b> ({@code dtBloqueio = LocalDateTime.now()}),
 * não expirado: o §8.2 do brief mediu que {@code ContaTutor.limparBloqueioExpirado(...)} é NO-OP
 * quando o bloqueio ainda não passou da janela — exatamente o único caso em que alguém chama o
 * suporte de verdade. Um destrave implementado com esse método passaria num setup de bloqueio
 * ANTIGO e ficaria silenciosamente inerte no caso real (regra 13 do {@code CLAUDE.md}: teste
 * verde sobre o setup errado).</p>
 *
 * <p>{@code dtUltimoLogin} recebe uma sentinela fixa antes do destrave e é conferido IDÊNTICO
 * depois — é o terceiro campo do critério 2, sem ele o bug do §2.3
 * ({@code registrarLoginSucesso()} carimbando login que não aconteceu) passaria despercebido.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuporteContasDestravarTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContaTutorRepository contaTutorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("destravaZeraTentativasEBloqueioSemTocarUltimoLogin — os 3 campos do critério 2")
    void destravaZeraTentativasEBloqueioSemTocarUltimoLogin() throws Exception {
        ContaTutor conta = contaTutorRepository.findByDsEmailLogin("tutor@kura.demo").orElseThrow();
        LocalDateTime ultimoLoginSentinela = LocalDateTime.of(2020, 1, 1, 0, 0);
        conta.setNrTentativasLogin(5);
        conta.setDtBloqueio(LocalDateTime.now()); // bloqueio RECENTE — ver javadoc da classe
        conta.setDtUltimoLogin(ultimoLoginSentinela);
        contaTutorRepository.save(conta);
        entityManager.flush();
        entityManager.clear();

        Long idConta = conta.getIdConta();

        mockMvc.perform(post("/web/suporte/contas/destravar")
                        .with(user("suporte@kura.demo").roles("SUPORTE"))
                        .with(csrf())
                        .param("idConta", String.valueOf(idConta))
                        .param("email", "tutor@kura.demo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/web/suporte/contas")))
                .andExpect(header().string("Location", containsString("email=tutor")));

        // flush() ANTES de clear(): sem isto, o UPDATE do controller (conta.save() dentro da
        // MESMA transação/persistence-context de teste) fica só ENFILEIRADO em memória — clear()
        // detacha sem persistir, e a releitura abaixo pegaria o nr_tentativas_login antigo do
        // banco. Mesma armadilha medida em WebAgendamentoRemarcarTest, vetor diferente (aqui é
        // dirty-checking descartado por clear() sem flush prévio, lá era cache de 1ª leitura).
        entityManager.flush();
        entityManager.clear();
        ContaTutor recarregada = contaTutorRepository.findById(idConta).orElseThrow();
        assertEquals(0, recarregada.getNrTentativasLogin());
        assertNull(recarregada.getDtBloqueio());
        assertEquals(ultimoLoginSentinela, recarregada.getDtUltimoLogin());
    }

    @Test
    @DisplayName("mensagemDeSucessoApareceAoVoltarParaBusca — flash attribute renderizado na página")
    void mensagemDeSucessoApareceAoVoltarParaBusca() throws Exception {
        ContaTutor conta = contaTutorRepository.findByDsEmailLogin("tutor@kura.demo").orElseThrow();
        conta.setNrTentativasLogin(3);
        conta.setDtBloqueio(LocalDateTime.now());
        contaTutorRepository.save(conta);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/web/suporte/contas/destravar")
                        .with(user("suporte@kura.demo").roles("SUPORTE"))
                        .with(csrf())
                        .param("idConta", String.valueOf(conta.getIdConta()))
                        .param("email", "tutor@kura.demo"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("idContaInexistenteRendaPaginaDeErro404 — nunca stack trace, nunca JSON")
    void idContaInexistenteRendaPaginaDeErro404() throws Exception {
        mockMvc.perform(post("/web/suporte/contas/destravar")
                        .with(user("suporte@kura.demo").roles("SUPORTE"))
                        .with(csrf())
                        .param("idConta", "999999")
                        .param("email", "x@x.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(not(containsString("\"codigo\""))));
    }
}
