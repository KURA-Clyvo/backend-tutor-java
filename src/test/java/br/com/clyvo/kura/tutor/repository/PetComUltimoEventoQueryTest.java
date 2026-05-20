package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.tutor.api.dto.PetComUltimoEventoProjection;
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
 * Valida findPetsComUltimoEvento via H2.
 *
 * Seed (afterMigrate__seeds_dev.sql):
 *   Pet id=1 "Marley" — ativo, vinculado ao Tutor id=1.
 *   Agendamento id=1  — Pet 1, data futura (+7 dias), status AGENDADO.
 *   VW_TIMELINE_PET materializa esse agendamento como evento de Marley,
 *   logo dtUltimoEvento deve ser não-nulo.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetComUltimoEventoQueryTest {

    @Autowired
    PetRepository repo;

    private static final PageRequest PAGE = PageRequest.of(0, 10);

    // ─── happy path: pet com agendamento retorna dtUltimoEvento preenchido ───

    @Test
    @DisplayName("findPetsComUltimoEvento — pet com agendamento na timeline retorna dtUltimoEvento não-nulo")
    void petComAgendamento_retornaDtUltimoEventoPreenchido() {
        Page<PetComUltimoEventoProjection> result = repo.findPetsComUltimoEvento(1L, PAGE);

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);

        PetComUltimoEventoProjection marley = result.getContent().stream()
            .filter(p -> "Marley".equals(p.nmPet()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pet 'Marley' não encontrado na resposta"));

        assertThat(marley.idPet()).isEqualTo(1L);
        assertThat(marley.dtUltimoEvento()).isNotNull();
    }

    // ─── edge case: tutor inexistente retorna página vazia ───────────────────

    @Test
    @DisplayName("findPetsComUltimoEvento — tutor inexistente retorna página vazia")
    void tutorInexistente_retornaVazio() {
        Page<PetComUltimoEventoProjection> result = repo.findPetsComUltimoEvento(999L, PAGE);

        assertThat(result.getTotalElements()).isZero();
    }
}
