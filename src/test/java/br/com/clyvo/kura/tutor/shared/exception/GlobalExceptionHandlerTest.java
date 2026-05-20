package br.com.clyvo.kura.tutor.shared.exception;

import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica GlobalExceptionHandler — payload ApiError, código UPPER_SNAKE_CASE,
 * detalhes campo-a-campo para validação e ausência de stack trace no 500.
 *
 * Usa contexto completo (H2, dev) com AgendamentoService mockado para
 * controlar quais exceções são lançadas sem tocar no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    private static final String EMAIL = "tutor@clyvo.vet";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AgendamentoService agendamentoService;

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(agendamentoService);
    }

    // ─── notFoundDeveRetornar404 ──────────────────────────────────────────────

    @Test
    @DisplayName("notFoundDeveRetornar404 — NotFoundException → 404 com codigo NAO_ENCONTRADO")
    @WithMockUser(username = EMAIL)
    void notFoundDeveRetornar404() throws Exception {
        when(agendamentoService.listar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new NotFoundException("Agendamento não encontrado."));

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Agendamento não encontrado."))
                .andExpect(jsonPath("$.detalhes").doesNotExist());
    }

    // ─── validacaoDeveRetornar400ComMensagensPtBr ─────────────────────────────

    @Test
    @DisplayName("validacaoDeveRetornar400ComMensagensPtBr — @Future + @NotNull → 400 com detalhes PT-BR")
    @WithMockUser(username = EMAIL)
    void validacaoDeveRetornar400ComMensagensPtBr() throws Exception {
        // dtAgendamento no passado → @Future violation
        // idPet ausente → @NotNull violation
        mockMvc.perform(post("/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "idClinica": 1,
                              "dtAgendamento": "2020-01-01T10:00:00",
                              "tipo": "CONSULTA"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Campos inválidos na requisição."))
                .andExpect(jsonPath("$.detalhes").isArray())
                .andExpect(jsonPath("$.detalhes", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.detalhes[*]", hasItem(containsString("futuro"))));
    }

    // ─── optimisticLockDeveRetornar409 ────────────────────────────────────────

    @Test
    @DisplayName("optimisticLockDeveRetornar409 — OptimisticLockingFailureException → 409 VERSAO_DESATUALIZADA")
    @WithMockUser(username = EMAIL)
    void optimisticLockDeveRetornar409() throws Exception {
        when(agendamentoService.atualizar(any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Agendamento", 1L));

        mockMvc.perform(put("/agendamentos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dtAgendamento": "2030-06-01T10:00:00",
                              "nrVersion": 999
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("VERSAO_DESATUALIZADA"))
                .andExpect(jsonPath("$.mensagem").value(
                        containsString("Versão desatualizada")));
    }

    // ─── exceptionGenericaDeveRetornar500SemVazarStackTrace ──────────────────

    @Test
    @DisplayName("exceptionGenericaDeveRetornar500SemVazarStackTrace — RuntimeException → 500 sem stack trace no body")
    @WithMockUser(username = EMAIL)
    void exceptionGenericaDeveRetornar500SemVazarStackTrace() throws Exception {
        when(agendamentoService.listar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Erro grave de infra!"));

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Erro interno. Tente novamente."))
                // Stack trace never leaked to the response body
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist());
    }
}
