package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgendamentoBffController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class AgendamentoBffControllerTest {

    private static final String EMAIL = "tutor@clyvo.vet";

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean AgendamentoService agendamentoService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider   jwtTokenProvider;

    private AgendamentoResponse agendamentoFixture() {
        return new AgendamentoResponse(
                1L, 42L, 10L, "Rex", 2L, 3L,
                LocalDateTime.of(2026, 8, 1, 10, 0), 30,
                "CONSULTA", "AGENDADO", "PORTAL", null,
                LocalDateTime.now(), null, null, 0L, null);
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/agendamentos com JWT retorna 200")
    @WithMockUser(username = EMAIL)
    void listar_comJwt_retorna200() throws Exception {
        Page<AgendamentoResponse> page = new PageImpl<>(List.of(agendamentoFixture()));
        when(agendamentoService.listar(eq(EMAIL), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/tutor/agendamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idAgendamento").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /v1/tutor/agendamentos sem JWT retorna 401")
    void listar_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/agendamentos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/tutor/agendamentos delega email do JWT ao AgendamentoService")
    @WithMockUser(username = EMAIL)
    void listar_delegaEmailAoService() throws Exception {
        when(agendamentoService.listar(anyString(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/v1/tutor/agendamentos")).andExpect(status().isOk());

        verify(agendamentoService).listar(eq(EMAIL), any(), any(), any(), any(), any(Pageable.class));
    }

    // ─── POST ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/tutor/agendamentos com JWT retorna 201 com Location")
    @WithMockUser(username = EMAIL)
    void criar_comJwt_retorna201() throws Exception {
        AgendamentoResponse resp = agendamentoFixture();
        when(agendamentoService.criar(eq(EMAIL), any(AgendamentoRequest.class))).thenReturn(resp);

        // Data relativa (now + 7 dias): dtAgendamento tem @Future — literal fixa expira e derruba o teste com o tempo
        AgendamentoRequest request = new AgendamentoRequest(
                10L, 2L, 3L, LocalDateTime.now().plusDays(7), "CONSULTA", 30, null);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/v1/tutor/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idAgendamento").value(1));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/tutor/agendamentos/{id} com JWT retorna 204")
    @WithMockUser(username = EMAIL)
    void excluir_comJwt_retorna204() throws Exception {
        doNothing().when(agendamentoService).excluir(EMAIL, 1L);

        mockMvc.perform(delete("/v1/tutor/agendamentos/1"))
                .andExpect(status().isNoContent());

        verify(agendamentoService).excluir(EMAIL, 1L);
    }

    @Test
    @DisplayName("DELETE /v1/tutor/agendamentos/{id} sem JWT retorna 401")
    void excluir_semJwt_retorna401() throws Exception {
        mockMvc.perform(delete("/v1/tutor/agendamentos/1"))
                .andExpect(status().isUnauthorized());
    }
}
