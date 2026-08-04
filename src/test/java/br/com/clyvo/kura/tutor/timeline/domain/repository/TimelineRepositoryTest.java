package br.com.clyvo.kura.tutor.timeline.domain.repository;

import br.com.clyvo.kura.tutor.timeline.domain.TimelinePet;
import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida VW_TIMELINE_PET/VW_VACINAS_VENCENDO via TimelinePetRepository/VacinaVencendoRepository.
 * O seed (afterMigrate__seeds_dev.sql) garante ID_PET=1 com um agendamento futuro (+7 dias, tipo CONSULTA).
 * O @Sql insere agendamentos adicionais (passado, e tipo VACINA) para os demais cenários (TASK-31).
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TimelineRepositoryTest {

    @Autowired
    TimelinePetRepository timelinePetRepository;

    @Autowired
    VacinaVencendoRepository vacinaVencendoRepository;

    @Test
    @DisplayName("findByIdPetDeveRetornarEventosOrdenadosPorData — DESC, evento mais recente primeiro")
    @Sql(statements = {
        "INSERT INTO AGENDAMENTO " +
        "  (ID_AGENDAMENTO, ID_CLINICA, ID_TUTOR, ID_PET, DT_AGENDAMENTO, DS_TIPO, ST_STATUS, DS_ORIGEM, NR_VERSION) " +
        "VALUES " +
        "  (201, 1, 1, 1, TIMESTAMP '2020-06-15 10:00:00', 'CONSULTA', 'REALIZADO', 'PORTAL', 0)"
    })
    void findByIdPetDeveRetornarEventosOrdenadosPorData() {
        Pageable desc = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dtEvento"));
        Page<TimelinePet> page = timelinePetRepository.findByIdPet(1L, desc);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);

        List<TimelinePet> eventos = page.getContent();
        for (int i = 0; i < eventos.size() - 1; i++) {
            assertThat(eventos.get(i).getDtEvento())
                    .isAfterOrEqualTo(eventos.get(i + 1).getDtEvento());
        }
    }

    @Test
    @DisplayName("findByIdPetAndIdEvento — TASK-31, detalhe de um evento específico da timeline")
    void findByIdPetAndIdEventoDeveRetornarOEventoDoSeed() {
        Optional<TimelinePet> evento = timelinePetRepository.findByIdPetAndIdEvento(1L, 1L);

        assertThat(evento).isPresent();
        assertThat(evento.get().getIdPet()).isEqualTo(1L);
        assertThat(evento.get().getDsTipoEvento()).isEqualTo("CONSULTA");
    }

    @Test
    @DisplayName("findByIdPetAndIdEvento — evento inexistente retorna vazio")
    void findByIdPetAndIdEventoInexistenteDeveRetornarVazio() {
        assertThat(timelinePetRepository.findByIdPetAndIdEvento(1L, 999L)).isEmpty();
    }

    @Test
    @DisplayName("countByIdPet — TASK-31, conta eventos do pet (usado em nrConsultas do detalhe)")
    void countByIdPetDeveContarEventosDoSeed() {
        assertThat(timelinePetRepository.countByIdPet(1L)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("VacinaVencendoRepository.findByIdPet — TASK-31, só retorna VACINA pendente do pet")
    @Sql(statements = {
        "INSERT INTO AGENDAMENTO " +
        "  (ID_AGENDAMENTO, ID_CLINICA, ID_TUTOR, ID_PET, DT_AGENDAMENTO, DS_TIPO, ST_STATUS, DS_ORIGEM, NR_VERSION) " +
        "VALUES " +
        "  (202, 1, 1, 1, CURRENT_TIMESTAMP + INTERVAL '10' DAY, 'VACINA', 'AGENDADO', 'PORTAL', 0)"
    })
    void findByIdPetDeveRetornarApenasVacinasPendentes() {
        List<VacinaVencendo> vacinas = vacinaVencendoRepository.findByIdPet(1L);

        assertThat(vacinas).isNotEmpty();
        assertThat(vacinas).allMatch(v -> v.getIdPet().equals(1L));
    }
}
