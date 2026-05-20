package br.com.clyvo.kura.tutor.consentimento.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService;
import br.com.clyvo.kura.tutor.consentimento.lgpd.TipoConsentimento;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa camada web de ConsentimentoController.
 *
 * Foco:
 *  - POST sem header Idempotency-Key → 400 (MissingRequestHeaderException)
 *  - GET retorna apenas o último por tipo (via listarUltimosPorTipo)
 */
@WebMvcTest(ConsentimentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class ConsentimentoControllerTest {

    private static final Long   ID_TUTOR = 1L;
    private static final String EMAIL    = "tutor@clyvo.vet";

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean ConsentimentoService consentimentoService;
    @MockBean UserDetailsService   userDetailsService;
    @MockBean JwtTokenProvider     jwtTokenProvider;

    // ─── postSemHeaderIdempotencyDeveRetornar400 ─────────────────────────────

    @Test
    @DisplayName("postSemHeaderIdempotencyDeveRetornar400 — Idempotency-Key obrigatório")
    @WithMockUser(username = EMAIL)
    void postSemHeaderIdempotencyDeveRetornar400() throws Exception {
        String body = """
            {
              "tipo": "LEMBRETES",
              "versaoTermo": "v1.0",
              "aceito": "S",
              "textoTermo": "texto"
            }
            """;

        mockMvc.perform(post("/tutores/{id}/consentimentos", ID_TUTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Sem o header Idempotency-Key → MissingRequestHeaderException → 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ─── getDeveRetornarApenasUltimoPorTipo ──────────────────────────────────

    @Test
    @DisplayName("getDeveRetornarApenasUltimoPorTipo — retorna 1 item por tipo (estado atual)")
    @WithMockUser(username = EMAIL)
    void getDeveRetornarApenasUltimoPorTipo() throws Exception {
        ConsentimentoResponse r1 = new ConsentimentoResponse(
                1L, TipoConsentimento.LEMBRETES.toDbValue(), "v1.0",
                true, true, LocalDateTime.now(), null);
        ConsentimentoResponse r2 = new ConsentimentoResponse(
                2L, TipoConsentimento.MARKETING.toDbValue(), "v1.0",
                false, false, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));

        when(consentimentoService.listarUltimosPorTipo(eq(ID_TUTOR), eq(EMAIL)))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/tutores/{id}/consentimentos", ID_TUTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idConsentimento").value(1))
                .andExpect(jsonPath("$[0].tipo").value("LEMBRETES"))
                .andExpect(jsonPath("$[1].tipo").value("MARKETING"))
                .andExpect(jsonPath("$[1].aceito").value(false));
    }
}
