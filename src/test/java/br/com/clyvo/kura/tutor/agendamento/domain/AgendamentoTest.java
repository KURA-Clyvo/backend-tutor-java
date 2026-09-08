package br.com.clyvo.kura.tutor.agendamento.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    @Test
    void cancelarCANCELADODeveLancarIllegalState() throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, StatusAgendamento.CANCELADO);
        assertThrows(IllegalStateException.class, () -> ag.cancelar("motivo"));
    }

    /**
     * Controle positivo. Sem estes casos, um {@code isFinal()} que devolvesse {@code true} para
     * tudo deixaria os testes de recusa VERDES e quebraria o cancelamento inteiro sem ninguem ver.
     */
    @ParameterizedTest
    @EnumSource(value = StatusAgendamento.class, names = {"INTENCAO", "AGENDADO", "CONFIRMADO"})
    void cancelarAPartirDeEstadoNaoFinalDeveFuncionar(StatusAgendamento origem) throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, origem);

        ag.cancelar("motivo");

        assertEquals(StatusAgendamento.CANCELADO, ag.getStStatus());
        assertNotNull(ag.getDtCancelamento());
    }

    /**
     * A lista de estados finais e a mesma que o .NET deriva em {@code AgendaService.StatusFinais}.
     * Este teste e o que faz uma divergencia entre os dois donos da tabela compartilhada quebrar a
     * suite em vez de apodrecer em silencio.
     */
    @ParameterizedTest
    @EnumSource(StatusAgendamento.class)
    void isFinalDeveSerVerdadeiroExatamenteParaOsTresEstadosFinais(StatusAgendamento status) {
        boolean esperado = status == StatusAgendamento.REALIZADO
                        || status == StatusAgendamento.CANCELADO
                        || status == StatusAgendamento.NAO_COMPARECEU;
        assertEquals(esperado, status.isFinal(), "isFinal() divergiu para " + status.name());
    }

    /**
     * SJ3-06 (fix wave): remarcar um agendamento em estado FINAL movia a data e respondia
     * sucesso. A guarda existia em {@code cancelar} e {@code confirmar} e faltava em
     * {@code atualizar} — medido por HTTP em 2026-09-08 (cancelar 302, remarcar o MESMO
     * agendamento 302, data nova na linha ja cancelada) e reproduzido por sonda MockMvc na
     * revisao G2. Alcancavel tambem pelo {@code PUT /api/v1/agendamentos/{id}} que o app
     * mobile consome, entao o fix e de produto, nao da camada web.
     */
    @ParameterizedTest
    @EnumSource(value = StatusAgendamento.class, names = {"REALIZADO", "CANCELADO", "NAO_COMPARECEU"})
    void atualizarEmEstadoFinalDeveLancarIllegalState(StatusAgendamento finalStatus) throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, finalStatus);
        LocalDateTime dataOriginal = ag.getDtAgendamento();

        assertThrows(IllegalStateException.class,
                () -> ag.atualizar(LocalDateTime.now().plusDays(30), null, null, null));

        assertEquals(dataOriginal, ag.getDtAgendamento(),
                "a data nao pode ter sido movida em estado final " + finalStatus.name());
    }

    /**
     * Controle positivo do teste acima. Sem ele, uma guarda que recusasse TUDO deixaria o teste
     * de recusa verde e quebraria a remarcacao inteira — exatamente a armadilha que o controle
     * positivo de {@code cancelar} ja cobre para o outro lado.
     */
    @ParameterizedTest
    @EnumSource(value = StatusAgendamento.class, names = {"INTENCAO", "AGENDADO", "CONFIRMADO"})
    void atualizarAPartirDeEstadoNaoFinalDeveFuncionar(StatusAgendamento origem) throws Exception {
        LocalDateTime futuro = LocalDateTime.now().plusDays(1);
        Agendamento ag = Agendamento.criar(null, null, null, null, futuro, "CONSULTA", null);
        setStatus(ag, origem);
        LocalDateTime novaData = LocalDateTime.now().plusDays(30);

        ag.atualizar(novaData, null, null, null);

        assertEquals(novaData, ag.getDtAgendamento());
        assertEquals(origem, ag.getStStatus(), "atualizar nao pode mudar o status");
    }

    private static void setStatus(Agendamento ag, StatusAgendamento status) throws Exception {
        Field f = Agendamento.class.getDeclaredField("stStatus");
        f.setAccessible(true);
        f.set(ag, status);
    }
}
