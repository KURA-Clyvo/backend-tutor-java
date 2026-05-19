package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Tutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TutorRepositoryTest {

    @Autowired
    TutorRepository tutorRepository;

    @Test
    @DisplayName("findByIdTutorAndStAtivo — tutor ativo retorna present com dados corretos")
    void findByIdTutorAndStAtivo_ativo_deveRetornarPresent() {
        Optional<Tutor> result = tutorRepository.findByIdTutorAndStAtivo(1L, "S");

        assertThat(result).isPresent();
        assertThat(result.get().getNmTutor()).isEqualTo("Felipe Ferrete");
        assertThat(result.get().getNrCpf()).isEqualTo("12345678900");
        assertThat(result.get().getStAtivo()).isEqualTo("S");
    }

    @Test
    @DisplayName("findByIdTutorAndStAtivo — tutor inativo não é retornado pelo filtro ST_ATIVO='S'")
    @Sql(statements = {
        "INSERT INTO TUTOR (ID_TUTOR, ID_CLINICA, NM_TUTOR, NR_CPF, DS_EMAIL, DS_TELEFONE, ST_ATIVO) " +
        "VALUES (99, 1, 'Tutor Inativo', '00000000099', 'inativo99@test.com', '11000000099', 'N')"
    })
    void findByIdTutorAndStAtivo_inativo_deveRetornarEmpty() {
        Optional<Tutor> result = tutorRepository.findByIdTutorAndStAtivo(99L, "S");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByDsEmailAndStAtivo — email existente e ativo retorna present")
    void findByDsEmailAndStAtivo_emailAtivo_deveRetornarPresent() {
        Optional<Tutor> result = tutorRepository.findByDsEmailAndStAtivo("felipe@clyvo.vet", "S");

        assertThat(result).isPresent();
        assertThat(result.get().getIdTutor()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarComFiltros — filtrando por nome retorna apenas tutores ativos")
    void buscarComFiltros_nomeExistente_deveRetornarApenasAtivos() {
        Page<Tutor> result = tutorRepository.buscarComFiltros("Felipe", null, null, Pageable.ofSize(10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allMatch(t -> "S".equals(t.getStAtivo()));
    }

    @Test
    @DisplayName("buscarComFiltros — todos os filtros nulos retorna tutores ativos sem restrição adicional")
    void buscarComFiltros_semFiltros_deveRetornarAtivos() {
        Page<Tutor> result = tutorRepository.buscarComFiltros(null, null, null, Pageable.ofSize(10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allMatch(t -> "S".equals(t.getStAtivo()));
    }
}
