package br.com.clyvo.kura.tutor.agendamento.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AgendamentoTest {

    @Test
    void criarComDataPassadaDeveLancarIllegalArgument() {
        LocalDateTime passado = LocalDateTime.now().minusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
            Agendamento.criar(null, null, null, null, passado, "CONSULTA", null));
    }

    @Test
    void criarValidoDeveCriarComStatusAGENDADO() {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", "obs");
        assertEquals(StatusAgendamento.AGENDADO, ag.getStStatus());
    }

    @Test
    void cancelarREALIZADODeveLancarIllegalState() throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, StatusAgendamento.REALIZADO);
        assertThrows(IllegalStateException.class, () -> ag.cancelar("motivo"));
    }

    @Test
    void confirmarStatusInvalidoDeveLancarIllegalState() throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, StatusAgendamento.CANCELADO);
        assertThrows(IllegalStateException.class, ag::confirmar);
    }

    /**
     * FD-06 (ciclo FIN) - a mordida do lado Java, commitada VERMELHA de proposito.
     *
     * <p>Antes desta task {@code cancelar()} bloqueava apenas REALIZADO e CANCELADO, entao um
     * agendamento marcado como falta pela clinica podia ser cancelado pelo tutor logo depois,
     * apagando o registro da ausencia.
     *
     * <p>A permissividade era inalcancavel ate agora: o Java nunca escreve NAO_COMPARECEU e o
     * validator do .NET recusava o valor. A FD-06 e o que torna esse caminho alcancavel.
     */
    @Test
    void cancelarNAO_COMPARECEUDeveLancarIllegalState() throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, StatusAgendamento.NAO_COMPARECEU);
        assertThrows(IllegalStateException.class, () -> ag.cancelar("motivo"));
        assertEquals(StatusAgendamento.NAO_COMPARECEU, ag.getStStatus());
    }

    private static void setStatus(Agendamento ag, StatusAgendamento status) throws Exception {
        Field f = Agendamento.class.getDeclaredField("stStatus");
        f.setAccessible(true);
        f.set(ag, status);
    }
}
