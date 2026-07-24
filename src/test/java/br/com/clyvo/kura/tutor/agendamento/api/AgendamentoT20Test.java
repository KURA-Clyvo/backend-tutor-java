package br.com.clyvo.kura.tutor.agendamento.api;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para PUT e DELETE do AgendamentoController (T20).
 *
 * AgendamentoService é mockado via @MockBean para isolar a camada HTTP.
 * Contexto completo Spring Boot com H2 (profile dev padrão).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgendamentoT20Test {

    private static final String EMAIL = "tutor@clyvo.vet";
    private static final Long   ID_AG = 1L;

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean AgendamentoService agendamentoService;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ─── putComVersionDesatualizadaLancaOptimisticLock ───────────────────────

    @Test
    @DisplayName("putComVersionDesatualizadaLancaOptimisticLock — versão divergente retorna 409")
    @WithMockUser(username = EMAIL)
    void putComVersionDesatualizadaLancaOptimisticLock() throws Exception {
        when(agendamentoService.atualizar(eq(EMAIL), eq(ID_AG), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Agendamento", ID_AG));

        String body = """
            {
              "dtAgendamento": "2030-06-01T10:00:00",
              "dsTipoConsulta": "RETORNO",
              "nrVersion": 999
            }
            """;

        mockMvc.perform(put("/agendamentos/{id}", ID_AG)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    // ─── putValidoIncrementaVersion ──────────────────────────────────────────

    @Test
    @DisplayName("putValidoIncrementaVersion — versão correta retorna 200 com nrVersion incrementada")
    @WithMockUser(username = EMAIL)
    void putValidoIncrementaVersion() throws Exception {
        AgendamentoResponse updated = agendamentoResponse(1L);
        when(agendamentoService.atualizar(eq(EMAIL), eq(ID_AG), any())).thenReturn(updated);

        String body = """
            {
              "dtAgendamento": "2030-06-01T10:00:00",
              "dsTipoConsulta": "RETORNO",
              "nrVersion": 0
            }
            """;

        mockMvc.perform(put("/agendamentos/{id}", ID_AG)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nrVersion").value(1));
    }

    // ─── deleteMudaStatusParaCANCELADO ───────────────────────────────────────

    @Test
    @DisplayName("deleteMudaStatusParaCANCELADO — retorna 204 e invoca excluir no service")
    @WithMockUser(username = EMAIL)
    void deleteMudaStatusParaCANCELADO() throws Exception {
        doNothing().when(agendamentoService).excluir(EMAIL, ID_AG);

        mockMvc.perform(delete("/agendamentos/{id}", ID_AG))
            .andExpect(status().isNoContent());

        verify(agendamentoService).excluir(EMAIL, ID_AG);
    }

    // ─── deleteDeAgendamentoREALIZADORetorna409 ──────────────────────────────

    @Test
    @DisplayName("deleteDeAgendamentoREALIZADORetorna409 — status REALIZADO levanta ConflictException → 409")
    @WithMockUser(username = EMAIL)
    void deleteDeAgendamentoREALIZADORetorna409() throws Exception {
        doThrow(new ConflictException("Não é possível cancelar agendamento com status REALIZADO."))
                .when(agendamentoService).excluir(EMAIL, ID_AG);

        mockMvc.perform(delete("/agendamentos/{id}", ID_AG))
            .andExpect(status().isConflict());
    }

    // ─── deleteDeOutroTutorRetorna403 ────────────────────────────────────────

    @Test
    @DisplayName("deleteDeOutroTutorRetorna403 — agendamento de outro tutor levanta ForbiddenException → 403")
    @WithMockUser(username = EMAIL)
    void deleteDeOutroTutorRetorna403() throws Exception {
        doThrow(new ForbiddenException("Agendamento não pertence a este tutor."))
                .when(agendamentoService).excluir(EMAIL, ID_AG);

        mockMvc.perform(delete("/agendamentos/{id}", ID_AG))
            .andExpect(status().isForbidden());
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private AgendamentoResponse agendamentoResponse(Long nrVersion) {
        return new AgendamentoResponse(
            ID_AG, 10L, 5L, "Rex", 2L, null,
            LocalDateTime.now().plusDays(7), 30,
            "RETORNO", "AGENDADO", "PORTAL",
            null, LocalDateTime.now(), null, null, nrVersion, null
        );
    }
}
