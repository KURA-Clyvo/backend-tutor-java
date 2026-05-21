package br.com.clyvo.kura.tutor;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração da aplicação — contexto completo com H2.
 *
 * Mudanças em relação à versão anterior:
 *   - JWTUtil substituído por JwtTokenProvider (T08)
 *   - 403 → 401 (JwtAuthenticationEntryPoint corrige o comportamento default)
 */
@SpringBootTest
@AutoConfigureMockMvc
class KuraTutorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Contexto Spring deve subir sem erros")
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
        assertThat(jwtTokenProvider).isNotNull();
    }

    @Test
    @DisplayName("GET /especies deve retornar 200 sem autenticacao (rota publica)")
    void especiesPublico() throws Exception {
        mockMvc.perform(get("/especies"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /agendamentos sem token deve retornar 401 (não 403)")
    void agendamentosSemToken() throws Exception {
        mockMvc.perform(get("/agendamentos")
                .param("tutorId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login com payload vazio deve retornar 400 VALIDACAO_INVALIDA")
    void loginPayloadVazio() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.detalhes").isArray());
    }

    @Test
    @DisplayName("GET /agendamentos com token invalido deve retornar 401 (não 403)")
    void agendamentosTokenInvalido() throws Exception {
        mockMvc.perform(get("/agendamentos")
                .param("tutorId", "1")
                .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/health deve retornar 200 sem autenticacao")
    void actuatorHealthPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
