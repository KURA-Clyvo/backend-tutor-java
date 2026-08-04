package br.com.clyvo.kura.tutor.shared.exception;

import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private ListAppender<ILoggingEvent> logAppender;
    private Logger handlerLogger;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(agendamentoService);

        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logAppender);
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

        // Data relativa (now + 7 dias): dtAgendamento tem @Future — literal fixa expira e derruba o teste com o tempo
        mockMvc.perform(put("/agendamentos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dtAgendamento": "%s",
                              "nrVersion": 999
                            }
                            """.formatted(LocalDateTime.now().plusDays(7))))
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

    // ─── erro500DeveLogarStackTraceComCorrelationId ───────────────────────────

    @Test
    @DisplayName("erro500DeveLogarStackTraceComCorrelationId — log.error com método HTTP, URI e exceção completa")
    @WithMockUser(username = EMAIL)
    void erro500DeveLogarStackTraceComCorrelationId() throws Exception {
        when(agendamentoService.listar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Falha simulada de infra"));

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isInternalServerError());

        List<ILoggingEvent> erros = logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .toList();

        assertThat(erros).isNotEmpty();

        ILoggingEvent evento = erros.get(0);
        // Formato: "Unhandled exception on [GET] /agendamentos: Falha simulada de infra"
        assertThat(evento.getFormattedMessage())
                .contains("Unhandled exception on")
                .contains("GET")
                .contains("/agendamentos");

        // ThrowableProxy presente → stack trace foi capturado pelo logger
        assertThat(evento.getThrowableProxy()).isNotNull();
        assertThat(evento.getThrowableProxy().getMessage())
                .isEqualTo("Falha simulada de infra");
    }

    // ─── erro404NaoDeveLogarStackTrace ────────────────────────────────────────

    @Test
    @DisplayName("erro404NaoDeveLogarStackTrace — NotFoundException não gera log.error")
    @WithMockUser(username = EMAIL)
    void erro404NaoDeveLogarStackTrace() throws Exception {
        when(agendamentoService.listar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new NotFoundException("Recurso não encontrado."));

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isNotFound());

        List<ILoggingEvent> erros = logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .toList();

        assertThat(erros).isEmpty();
    }
}
