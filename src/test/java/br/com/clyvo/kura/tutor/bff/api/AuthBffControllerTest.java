package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.auth.application.AuthService;
import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.onboarding.application.OnboardingService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthBffController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class AuthBffControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService           authService;
    @MockBean OnboardingService     onboardingService;
    @MockBean UserDetailsService    userDetailsService;
    @MockBean JwtTokenProvider      jwtTokenProvider;

    private static final TokenResponse TOKEN_RESP =
            new TokenResponse("acc.token", "ref.token", "Bearer", 900L, 1L, "Felipe");

    @Test
    @DisplayName("/v1/auth/login é público e retorna 200 com credenciais válidas")
    void loginPublico_retorna200() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(TOKEN_RESP);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tutor@clyvo.vet", "Senha@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("/v1/auth/login sem JWT não retorna 401 — rota pública")
    void loginPublico_semJwt_naoRetorna401() throws Exception {
        when(authService.login(any())).thenReturn(TOKEN_RESP);

        // Sem header Authorization → 200 (não 401), pois rota é pública
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tutor@clyvo.vet", "Senha@123"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/v1/auth/login com payload inválido retorna 400 (validação Bean)")
    void loginPayloadInvalido_retorna400() throws Exception {
        // email vazio → @NotBlank → 400
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/v1/auth/refresh é público e delega ao AuthService")
    void refreshPublico_retorna200() throws Exception {
        when(authService.refresh(any())).thenReturn(TOKEN_RESP);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid.refresh.token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc.token"));
    }
}
