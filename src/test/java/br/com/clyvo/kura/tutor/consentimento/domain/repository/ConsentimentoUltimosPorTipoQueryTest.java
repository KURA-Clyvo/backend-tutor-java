package br.com.clyvo.kura.tutor.consentimento.domain.repository;

import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida findUltimosPorTipo via H2.
 *
 * Seed: TUTOR id=1 já existente (afterMigrate__seeds_dev.sql).
 * Dados de consentimento inseridos via JdbcTemplate para controlar timestamps explicitamente.
 * A tabela CONSENTIMENTO não possui seed — está vazia no início de cada teste.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConsentimentoUltimosPorTipoQueryTest {

    @Autowired
    ConsentimentoRepository repo;

    @Autowired
    JdbcTemplate jdbc;

    // ─── happy path: dois registros LEMBRETES → apenas o mais recente retornado ─

    @Test
    @DisplayName("findUltimosPorTipo — retorna apenas o mais recente por tipo, ignorando versões anteriores")
    void retornaMaisRecentePorTipo() {
        LocalDateTime agora = LocalDateTime.now();

        jdbc.update(
            "INSERT INTO CONSENTIMENTO (ID_TUTOR, DS_TIPO, DS_VERSAO_TERMO, ST_ACEITO, DT_ACEITE) VALUES (?, ?, ?, ?, ?)",
            1L, "LEMBRETES", "v1.0", "S", agora.minusHours(2));
        jdbc.update(
            "INSERT INTO CONSENTIMENTO (ID_TUTOR, DS_TIPO, DS_VERSAO_TERMO, ST_ACEITO, DT_ACEITE) VALUES (?, ?, ?, ?, ?)",
            1L, "LEMBRETES", "v2.0", "S", agora.minusHours(1));
        jdbc.update(
            "INSERT INTO CONSENTIMENTO (ID_TUTOR, DS_TIPO, DS_VERSAO_TERMO, ST_ACEITO, DT_ACEITE) VALUES (?, ?, ?, ?, ?)",
            1L, "DADOS_ANONIMOS", "v1.0", "S", agora.minusHours(3));

        List<Consentimento> result = repo.findUltimosPorTipo(1L);

        assertThat(result).hasSize(2);

        Consentimento lembreteMaisRecente = result.stream()
            .filter(c -> "LEMBRETES".equals(c.getDsTipo()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Nenhum LEMBRETES retornado"));

        assertThat(lembreteMaisRecente.getDsVersaoTermo()).isEqualTo("v2.0");
    }

    // ─── edge case: tutor sem consentimentos retorna lista vazia ─────────────

    @Test
    @DisplayName("findUltimosPorTipo — tutor sem consentimentos retorna lista vazia")
    void tutorSemConsentimentos_retornaVazio() {
        assertThat(repo.findUltimosPorTipo(999L)).isEmpty();
    }
}
