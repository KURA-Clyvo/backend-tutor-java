package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.notificacao.api.dto.NotificacaoResponse;
import br.com.clyvo.kura.tutor.notificacao.application.NotificacaoService;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-31 — NOTIFICACAO é .NET owned; este controller é estritamente leitura.
 * Não existe (e não deve existir) nenhum PATCH/POST/DELETE aqui — marcar como
 * lida foi avaliado e descartado (ver INT-01-contract-map e NotificacaoService).
 */
@WebMvcTest(NotificacaoBffController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class NotificacaoBffControllerTest {

    private static final String EMAIL    = "tutor@clyvo.vet";
    private static final Long   ID_TUTOR = 42L;

    @Autowired MockMvc mockMvc;

    @MockBean NotificacaoService    notificacaoService;
    @MockBean ContaTutorRepository  contaTutorRepository;
    @MockBean UserDetailsService    userDetailsService;
    @MockBean JwtTokenProvider      jwtTokenProvider;

    @Test
    @DisplayName("GET /v1/tutor/notificacoes com JWT válido retorna 200")
    @WithMockUser(username = EMAIL)
    void listar_comJwt_retorna200() throws Exception {
        NotificacaoResponse notif = new NotificacaoResponse(
                1L, "Vacina próxima do vencimento", "A vacina antirrábica vence em 5 dias.",
                LocalDateTime.now(), false);
        Page<NotificacaoResponse> page = new PageImpl<>(List.of(notif));

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(notificacaoService.listar(eq(ID_TUTOR), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/tutor/notificacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idNotificacao").value(1))
                .andExpect(jsonPath("$.content[0].flLida").value(false));
    }

    @Test
    @DisplayName("GET /v1/tutor/notificacoes sem JWT retorna 401")
    void listar_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/notificacoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /v1/tutor/notificacoes/{id}/lida não existe — 404 (sem PATCH; NOTIFICACAO é .NET owned)")
    @WithMockUser(username = EMAIL)
    void marcarLida_naoExiste_retorna404() throws Exception {
        mockMvc.perform(patch("/v1/tutor/notificacoes/1/lida"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /v1/tutor/notificacoes/{id} não existe — 404 (leitura only)")
    @WithMockUser(username = EMAIL)
    void deletar_naoExiste_retorna404() throws Exception {
        mockMvc.perform(delete("/v1/tutor/notificacoes/1"))
                .andExpect(status().isNotFound());
    }
}
