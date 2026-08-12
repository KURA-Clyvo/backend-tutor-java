package br.com.clyvo.kura.tutor.agendamento.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TASK-87: prova de que o fuso do container afeta {@link Agendamento#criar}, que usa
 * {@code LocalDateTime.now()} (leitura de {@code ZoneId.systemDefault()}) para validar que a
 * data do agendamento é futura.
 *
 * <p>Sem {@code TZ=America/Sao_Paulo} no container Docker, a JVM cai em UTC — que fica ~3h
 * adiantada sobre o horário real de Brasília — e um agendamento genuinamente 1h no futuro (em
 * horário de Brasília) é rejeitado como se já tivesse passado. Este teste reproduz o bug sob
 * "UTC" (simulando o container ANTES do fix desta task) e prova a correção sob
 * "America/Sao_Paulo" (o valor que a TASK-87 adicionou ao serviço {@code kura-tutor} em
 * {@code docker-compose.yml}).
 *
 * <p>Mutar {@code TimeZone.setDefault()} é o único jeito de simular a variável de ambiente
 * {@code TZ} do container dentro de um teste JVM local sem subir Docker — {@code
 * ZoneId.systemDefault()} é resolvido a partir de {@code TimeZone.getDefault()}, exatamente o
 * mesmo mecanismo pelo qual {@code LocalDateTime.now()} herdaria o {@code TZ} do container em
 * produção. Nenhum teste Java existente no projeto fixa fuso explicitamente (confirmado no G0
 * desta task, `task-87-g0.md`, seção 5) — os testes de {@code Agendamento}/{@code
 * AgendamentoService} usam {@code LocalDateTime.now().plusDays(n)}, auto-referenciais ao
 * relógio do próprio processo, e por isso não provam nem o bug nem o fix.
 */
class AgendamentoTimezoneTest {

    private TimeZone timeZoneOriginal;

    @BeforeEach
    void salvarFusoOriginal() {
        timeZoneOriginal = TimeZone.getDefault();
    }

    @AfterEach
    void restaurarFusoOriginal() {
        TimeZone.setDefault(timeZoneOriginal);
    }

    @Test
    void semTzExplicitoAgendamentoFuturoEmBrtEhRejeitadoComoPassado() {
        // Simula o container ANTES do fix da TASK-87 (sem TZ=America/Sao_Paulo -> JVM cai em UTC,
        // que é o default de qualquer container Linux sem a variável setada).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LocalDateTime dtAgendamento = umaHoraNoFuturoEmBrt();

        // O bug: LocalDateTime.now() sob UTC fica ~3h adiantado sobre o BRT real, então um
        // horário genuinamente 1h no futuro (BRT) aparece "no passado" para a comparação de
        // Agendamento.criar() -- é exatamente o sintoma medido no achado original (6/16 slots
        // rejeitados às 09:32 BRT).
        assertThrows(IllegalArgumentException.class, () ->
            Agendamento.criar(null, null, null, null, dtAgendamento, "CONSULTA", null),
            "com a JVM em UTC (container sem TZ), um agendamento 1h no futuro em BRT deveria "
                + "ser rejeitado como passado -- reproduz o bug que o TZ da TASK-87 corrige");
    }

    @Test
    void comTzAmericaSaoPauloAgendamentoFuturoEmBrtEhAceito() {
        // Simula o container DEPOIS do fix da TASK-87 (TZ=America/Sao_Paulo, ver
        // DevOps-Cloud/docker-compose.yml, serviço kura-tutor).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));

        LocalDateTime dtAgendamento = umaHoraNoFuturoEmBrt();

        Agendamento ag = Agendamento.criar(null, null, null, null, dtAgendamento, "CONSULTA", null);

        assertEquals(StatusAgendamento.AGENDADO, ag.getStStatus());
    }

    /**
     * Constrói o wall-clock BRT (America/Sao_Paulo) de "agora + 1h", independente do fuso
     * default atual da JVM -- parte de {@link Instant#now()} (TZ-independente por definição) e
     * converte explicitamente para o fuso de Brasília, sem depender de nenhum offset fixo.
     */
    private static LocalDateTime umaHoraNoFuturoEmBrt() {
        Instant agora = Instant.now();
        return agora.plusSeconds(3600).atZone(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
    }
}
