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

    private static void setStatus(Agendamento ag, StatusAgendamento status) throws Exception {
        Field f = Agendamento.class.getDeclaredField("stStatus");
        f.setAccessible(true);
        f.set(ag, status);
    }
}
