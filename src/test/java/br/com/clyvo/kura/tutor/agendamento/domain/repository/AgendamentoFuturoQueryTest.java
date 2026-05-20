package br.com.clyvo.kura.tutor.agendamento.domain.repository;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida findFuturosByTutorEStatus via H2.
 *
 * Seed: AGENDAMENTO id=1 pertence ao TUTOR id=1, data futura (+7 dias), status AGENDADO.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendamentoFuturoQueryTest {

    @Autowired
    AgendamentoRepository repo;

    private static final PageRequest PAGE = PageRequest.of(0, 10);

    // ─── happy path: status null devolve todos os futuros do tutor ──────────

    @Test
    @DisplayName("findFuturosByTutorEStatus — status null retorna todos os agendamentos futuros do tutor")
    void statusNull_retornaTodosOsFuturos() {
        Page<Agendamento> result = repo.findFuturosByTutorEStatus(1L, null, PAGE);

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allSatisfy(a ->
                assertThat(a.getTutor().getIdTutor()).isEqualTo(1L));
    }

    // ─── happy path: filtro por status retorna apenas o status pedido ───────

    @Test
    @DisplayName("findFuturosByTutorEStatus — status AGENDADO retorna agendamento do seed")
    void statusAgendado_retornaAgendamentoDoSeed() {
        Page<Agendamento> result =
                repo.findFuturosByTutorEStatus(1L, StatusAgendamento.AGENDADO, PAGE);

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allSatisfy(a ->
                assertThat(a.getStStatus()).isEqualTo(StatusAgendamento.AGENDADO));
    }

    // ─── edge case: status que não existe no seed retorna lista vazia ────────

    @Test
    @DisplayName("findFuturosByTutorEStatus — status CONFIRMADO retorna página vazia para o seed")
    void statusConfirmado_retornaVazio() {
        Page<Agendamento> result =
                repo.findFuturosByTutorEStatus(1L, StatusAgendamento.CONFIRMADO, PAGE);

        assertThat(result.getTotalElements()).isZero();
    }

    // ─── edge case: tutor inexistente retorna lista vazia ────────────────────

    @Test
    @DisplayName("findFuturosByTutorEStatus — tutorId inexistente retorna página vazia")
    void tutorInexistente_retornaVazio() {
        Page<Agendamento> result =
                repo.findFuturosByTutorEStatus(999L, null, PAGE);

        assertThat(result.getTotalElements()).isZero();
    }
}
