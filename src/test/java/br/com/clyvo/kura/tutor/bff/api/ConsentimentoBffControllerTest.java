package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService.RegistroResult;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsentimentoBffController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class ConsentimentoBffControllerTest {

    private static final String EMAIL    = "tutor@clyvo.vet";
    private static final Long   ID_TUTOR = 42L;

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean ConsentimentoService   consentimentoService;
    @MockBean ContaTutorRepository   contaTutorRepository;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean JwtTokenProvider       jwtTokenProvider;

    private ConsentimentoResponse consentimentoFixture() {
        return new ConsentimentoResponse(1L, "LEMBRETES", "v1.0", true, true,
                LocalDateTime.now(), null);
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/consentimentos com JWT retorna 200")
    @WithMockUser(username = EMAIL)
    void listar_comJwt_retorna200() throws Exception {
        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(consentimentoService.listarUltimosPorTipo(eq(ID_TUTOR), eq(EMAIL)))
                .thenReturn(List.of(consentimentoFixture()));

        mockMvc.perform(get("/v1/tutor/consentimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idConsentimento").value(1))
                .andExpect(jsonPath("$[0].tipo").value("LEMBRETES"))
                .andExpect(jsonPath("$[0].aceito").value(true));
    }

    @Test
    @DisplayName("GET /v1/tutor/consentimentos sem JWT retorna 401")
    void listar_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/consentimentos"))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/tutor/consentimentos com JWT e Idempotency-Key retorna 201")
    @WithMockUser(username = EMAIL)
    void registrar_comJwt_retorna201() throws Exception {
        ConsentimentoResponse resp = consentimentoFixture();
        RegistroResult result = new RegistroResult(resp, true);

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(consentimentoService.registrarComIdempotencia(
                anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(result);

        String body = "{\"tipo\":\"LEMBRETES\",\"versaoTermo\":\"v1.0\",\"aceito\":\"S\"}";

        mockMvc.perform(post("/v1/tutor/consentimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "550e8400-e29b-41d4-a716-446655440000")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idConsentimento").value(1));
    }

    @Test
    @DisplayName("POST /v1/tutor/consentimentos sem JWT retorna 401")
    void registrar_semJwt_retorna401() throws Exception {
        mockMvc.perform(post("/v1/tutor/consentimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "550e8400-e29b-41d4-a716-446655440000")
                        .content("{\"tipo\":\"LEMBRETES\",\"versaoTermo\":\"v1.0\",\"aceito\":true}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── DELETE stub ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/tutor/consentimentos/{id} retorna 501 — insert-only, sem suporte a revogação por DELETE")
    @WithMockUser(username = EMAIL)
    void deletar_retorna501() throws Exception {
        mockMvc.perform(delete("/v1/tutor/consentimentos/1"))
                .andExpect(status().isNotImplemented());
    }
}
