package br.com.clyvo.kura.tutor.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("endpointPublicoDeveRetornar200SemAuth — /actuator/health acessível sem token")
    void endpointPublicoDeveRetornar200SemAuth() throws Exception {
        mockMvc.perform(get("/api/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("endpointProtegidoSemTokenDeveRetornar401 — sem Authorization header retorna 401, NÃO 403")
    void endpointProtegidoSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/agendamentos").param("tutorId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("endpointProtegidoComTokenInvalidoDeveRetornar401ComApiError — JSON com campo 'codigo'")
    void endpointProtegidoComTokenInvalidoDeveRetornar401ComApiError() throws Exception {
        mockMvc.perform(get("/api/agendamentos")
                        .param("tutorId", "1")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }
}
