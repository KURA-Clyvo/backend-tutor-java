package br.com.clyvo.kura.tutor.auth.api;

import br.com.clyvo.kura.tutor.auth.application.AuthService;
import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.shared.config.CorsConfig;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de camada web para AuthController — logout e CORS.
 *
 * CorsConfig é importado explicitamente para que o CorsConfigurationSource bean
 * esteja disponível no contexto @WebMvcTest e o CORS filter funcione corretamente.
 */
@WebMvcTest(AuthController.class)
@Import({CorsConfig.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7",
        "kura.cors.allowed-origins=http://localhost:3000,http://app.clyvo.vet"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthService        authService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider   jwtTokenProvider;

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logoutAutenticadoDeveRetornar204ELimparRefreshHash")
    @WithMockUser(username = "tutor@clyvo.vet")
    void logoutAutenticadoDeveRetornar204ELimparRefreshHash() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());

        // "limpar refresh hash" é responsabilidade de authService.logout() —
        // verificado aqui através da chamada ao método correto
        verify(authService).logout("tutor@clyvo.vet");
    }

    @Test
    @DisplayName("logoutSemAuthDeveRetornar401 — endpoint protegido rejeita request anônima")
    void logoutSemAuthDeveRetornar401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ─── CORS ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("corsDeveAceitarOriginConfigurada — preflight retorna Allow-Origin")
    void corsDeveAceitarOriginConfigurada() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("corsDeveRejeitarOriginNaoConfigurada — preflight retorna 403 sem Allow-Origin")
    void corsDeveRejeitarOriginNaoConfigurada() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://malicious.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
