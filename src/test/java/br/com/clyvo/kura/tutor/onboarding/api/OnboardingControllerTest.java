package br.com.clyvo.kura.tutor.onboarding.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.onboarding.api.dto.RegisterInviteRequest;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TutorResumoResponse;
import br.com.clyvo.kura.tutor.onboarding.application.OnboardingService;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de camada web — valida serialização, validação Bean e mapeamento HTTP.
 *
 * UserDetailsService é mockado para satisfazer o JwtAuthenticationFilter
 * sem instanciar repositórios JPA (que não existem no contexto @WebMvcTest).
 */
@WebMvcTest(OnboardingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class OnboardingControllerTest {

    private static final String ENDPOINT    = "/auth/register-invite";
    private static final String TOKEN_SEED  = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SENHA_FORTE = "Senha@123";

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OnboardingService  onboardingService;
    @MockBean UserDetailsService userDetailsService;  // satisfaz JwtAuthenticationFilter
    @MockBean JwtTokenProvider   jwtTokenProvider;

    // ─── Caminho feliz ────────────────────────────────────────────────────────

    @Test
    @DisplayName("postValidoDeveRetornar201ComTokens")
    void postValidoDeveRetornar201ComTokens() throws Exception {
        TutorResumoResponse tutor = new TutorResumoResponse(1L, "Tutor Teste");
        TokenResponse resposta    = TokenResponse.of("access.jwt", "refresh.jwt", 900, tutor);
        when(onboardingService.registrarPorInvite(any(), any())).thenReturn(resposta);

        RegisterInviteRequest req = new RegisterInviteRequest(TOKEN_SEED, SENHA_FORTE, List.of());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.tutor.idTutor").value(1))
                .andExpect(jsonPath("$.tutor.nmTutor").value("Tutor Teste"));
    }

    // ─── Validação Bean — mensagens PT-BR ─────────────────────────────────────

    @Test
    @DisplayName("postSenhaFracaDeveRetornar400ComMensagensPtBr — tamanho mínimo")
    void postSenhaFracaDeveRetornar400ComMensagensPtBr() throws Exception {
        // "fraca" — menos de 8 chars, sem maiúscula, sem dígito
        RegisterInviteRequest req = new RegisterInviteRequest(TOKEN_SEED, "fraca", List.of());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senha").exists());
    }

    @Test
    @DisplayName("postSenhaSemMaiusculaDeveRetornar400 — regex falha")
    void postSenhaSemMaiusculaDeveRetornar400() throws Exception {
        // 8+ chars, tem dígito, mas não tem maiúscula
        RegisterInviteRequest req = new RegisterInviteRequest(TOKEN_SEED, "senha123", List.of());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senha").exists());
    }

    @Test
    @DisplayName("postTokenEmBrancoDeveRetornar400")
    void postTokenEmBrancoDeveRetornar400() throws Exception {
        RegisterInviteRequest req = new RegisterInviteRequest("", SENHA_FORTE, List.of());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("postAceitesNulosDeveRetornar400")
    void postAceitesNulosDeveRetornar400() throws Exception {
        // null em aceites viola @NotNull
        RegisterInviteRequest req = new RegisterInviteRequest(TOKEN_SEED, SENHA_FORTE, null);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.aceites").exists());
    }

    // ─── Erro de negócio mapeado para HTTP ────────────────────────────────────

    @Test
    @DisplayName("conflictExceptionDeveRetornar409")
    void conflictExceptionDeveRetornar409() throws Exception {
        when(onboardingService.registrarPorInvite(any(), any()))
                .thenThrow(new ConflictException("Convite já utilizado."));

        RegisterInviteRequest req = new RegisterInviteRequest(TOKEN_SEED, SENHA_FORTE, List.of());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Convite já utilizado."));
    }
}
