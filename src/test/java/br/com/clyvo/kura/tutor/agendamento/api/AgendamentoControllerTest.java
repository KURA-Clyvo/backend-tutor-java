package br.com.clyvo.kura.tutor.agendamento.api;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de camada web para AgendamentoController.
 *
 * Foco:
 *  - POST retorna 201 + Location header
 *  - POST com data passada retorna 400 (Bean Validation @Future)
 *  - GET filtra por status e data
 *  - GET ignora qualquer tutorId de query param — usa SEMPRE o SecurityContext
 */
@WebMvcTest(AgendamentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
    "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
    "kura.jwt.access-expiration-minutes=15",
    "kura.jwt.refresh-expiration-days=7"
})
class AgendamentoControllerTest {

    private static final String EMAIL     = "tutor@clyvo.vet";
    private static final Long   ID_AG     = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AgendamentoService agendamentoService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider   jwtTokenProvider;

    @BeforeEach
    void setupObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ─── POST válido retorna 201 + Location ────────────────────────────────────

    @Test
    @DisplayName("postValidoRetorna201ComLocation")
    @WithMockUser(username = EMAIL)
    void postValidoRetorna201ComLocation() throws Exception {
        AgendamentoResponse response = agendamentoResponse();
        when(agendamentoService.criar(eq(EMAIL), any())).thenReturn(response);

        // Data relativa (now + 7 dias): dtAgendamento tem @Future — literal fixa expira e derruba o teste com o tempo
        String dtFutura = LocalDateTime.now().plusDays(7).toString();
        String body = """
            {
              "idPet": 1,
              "idClinica": 1,
              "dtAgendamento": "%s",
              "tipo": "CONSULTA"
            }
            """.formatted(dtFutura);

        mockMvc.perform(post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/agendamentos/1")))
            .andExpect(jsonPath("$.idAgendamento").value(1))
            .andExpect(jsonPath("$.status").value("AGENDADO"))
            .andExpect(jsonPath("$.nrVersion").value(0));
    }

    // ─── POST com data passada retorna 400 ────────────────────────────────────

    @Test
    @DisplayName("postDataPassadaRetorna400")
    @WithMockUser(username = EMAIL)
    void postDataPassadaRetorna400() throws Exception {
        String body = """
            {
              "idPet": 1,
              "idClinica": 1,
              "dtAgendamento": "2020-01-01T10:00:00",
              "tipo": "CONSULTA"
            }
            """;

        mockMvc.perform(post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    // ─── GET filtra por status e data ────────────────────────────────────────

    @Test
    @DisplayName("getFiltraPorStatusEData")
    @WithMockUser(username = EMAIL)
    void getFiltraPorStatusEData() throws Exception {
        Page<AgendamentoResponse> page = new PageImpl<>(List.of(agendamentoResponse()));
        when(agendamentoService.listar(eq(EMAIL), eq("AGENDADO"), any(), any(), isNull(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/agendamentos")
                .param("status", "AGENDADO")
                .param("dataInicio", "2026-06-01T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].idAgendamento").value(1));

        verify(agendamentoService).listar(eq(EMAIL), eq("AGENDADO"), any(), isNull(), isNull(), any());
    }

    // ─── GET ignora tutorId de query param ────────────────────────────────────

    @Test
    @DisplayName("getNaoAceitaTutorIdViaQuery — idTutor nunca vem de query param")
    @WithMockUser(username = EMAIL)
    void getNaoAceitaTutorIdViaQuery() throws Exception {
        Page<AgendamentoResponse> page = new PageImpl<>(List.of());
        when(agendamentoService.listar(eq(EMAIL), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        // tutorId=999 passado via query — deve ser IGNORADO
        mockMvc.perform(get("/agendamentos").param("tutorId", "999"))
            .andExpect(status().isOk());

        // Serviço foi chamado com o EMAIL do SecurityContext (não com 999)
        verify(agendamentoService).listar(eq(EMAIL), isNull(), isNull(), isNull(), isNull(), any());
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AgendamentoResponse agendamentoResponse() {
        return new AgendamentoResponse(
            ID_AG, 10L, 5L, "Rex", 2L, null,
            LocalDateTime.now().plusDays(7), 30,
            "CONSULTA", "AGENDADO", "PORTAL",
            null, LocalDateTime.now(), null, null, 0L, null
        );
    }
}
