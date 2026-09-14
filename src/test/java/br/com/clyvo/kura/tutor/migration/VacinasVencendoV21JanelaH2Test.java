package br.com.clyvo.kura.tutor.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LU-02 fix wave 2 — achado IMPORTANTE da re-G2 (lu-02-revisao.md §Re-G2).
 *
 * <p>A fix wave 1 da migration V21 (variante H2, {@code MODE=Oracle}) usava
 * {@code CAST(dt AS DATE)} para comparar a janela e calcular {@code DIAS_RESTANTES}. Em H2
 * 2.2.224 sob {@code MODE=Oracle}, {@code CAST(TIMESTAMP AS DATE)} <b>preserva a hora</b>
 * (diferente do Oracle, onde {@code DATE} sempre trunca) — medido pela re-G2:
 * {@code CAST(TIMESTAMP '2026-10-14 23:30:00' AS DATE)} = {@code 2026-10-14 23:30:00}, não
 * meia-noite. Consequência: uma dose em {@code hoje+30} com hora depois de 00:00
 * <b>desaparecia</b> da view no H2 e <b>continuava aparecendo</b> no Oracle (onde
 * {@code TRUNC(dt)} zera a hora de verdade) — o cabeçalho da migration H2 afirmava
 * equivalência que nunca tinha sido medida.
 *
 * <p>Este teste prova, contra H2 real com a V21 aplicada, os 4 casos de fronteira da janela
 * (hoje..hoje+30, comparando por DATA civil, não por instante): dose em hoje+30 às 23:30 tem
 * que estar presente com {@code DIAS_RESTANTES=30}; dose de hoje às 00:00 tem que estar
 * presente com {@code DIAS_RESTANTES=0}; dose de ontem às 23:30 e dose em hoje+31 às 00:00 têm
 * que estar ausentes. Os 4 valores de {@code DT_PROXIMA_DOSE} são inseridos por expressão SQL
 * relativa a {@code CURRENT_DATE} (avaliada pela MESMA sessão H2 que a view usa na mesma
 * consulta) — o resultado não depende do fuso da JVM que roda o teste, só do fuso da sessão
 * H2, que é idêntico nos dois lados da comparação.
 *
 * <p><b>Mordida (registrada no ledger, não neste arquivo):</b> revertendo só a migration H2
 * para {@code CAST(... AS DATE)}, a asserção do caso hoje+30 23:30 falha (linha ausente onde
 * devia estar presente) — a suíte inteira roda (não reduzida) e só este teste morde.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VacinasVencendoV21JanelaH2Test {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("VW_VACINAS_VENCENDO (H2) — janela e DIAS_RESTANTES usam TRUNC, não CAST(...AS DATE) (re-G2, IMPORTANTE)")
    @Sql(statements = {
        // Seed de dev já tem PET 1 / CLINICA 1 / VETERINARIO 1 / TIPO_EVENTO 3='VACINA'
        // (afterMigrate__seeds_dev.sql). Os 4 DT_PROXIMA_DOSE abaixo são calculados por
        // expressão relativa a CURRENT_DATE, avaliada pela sessão H2 no momento do INSERT —
        // a mesma referência de "hoje" que a view usa quando a SELECT roda a seguir.
        "INSERT INTO EVENTO_CLINICO (ID_EVENTO, ID_PET, ID_CLINICA, ID_VETERINARIO, ID_TIPO_EVENTO, DS_OBSERVACAO) " +
        "  VALUES (960, 1, 1, 1, 3, 'LU-02 fix wave 2 - janela H2 (achado re-G2)')",
        "INSERT INTO VACINA (ID_VACINA, ID_EVENTO_CLINICO, NM_VACINA, NR_LOTE, DS_FABRICANTE, DT_PROXIMA_DOSE) " +
        "  VALUES (960, 960, 'LU02FW2-D30-2330', 'L1', 'Fabricante Teste', " +
        "          DATEADD('MINUTE', 1410, CAST(DATEADD('DAY', 30, CURRENT_DATE) AS TIMESTAMP)))",

        "INSERT INTO EVENTO_CLINICO (ID_EVENTO, ID_PET, ID_CLINICA, ID_VETERINARIO, ID_TIPO_EVENTO, DS_OBSERVACAO) " +
        "  VALUES (961, 1, 1, 1, 3, 'LU-02 fix wave 2 - janela H2 (achado re-G2)')",
        "INSERT INTO VACINA (ID_VACINA, ID_EVENTO_CLINICO, NM_VACINA, NR_LOTE, DS_FABRICANTE, DT_PROXIMA_DOSE) " +
        "  VALUES (961, 961, 'LU02FW2-HOJE-0000', 'L1', 'Fabricante Teste', " +
        "          CAST(CURRENT_DATE AS TIMESTAMP))",

        "INSERT INTO EVENTO_CLINICO (ID_EVENTO, ID_PET, ID_CLINICA, ID_VETERINARIO, ID_TIPO_EVENTO, DS_OBSERVACAO) " +
        "  VALUES (962, 1, 1, 1, 3, 'LU-02 fix wave 2 - janela H2 (achado re-G2)')",
        "INSERT INTO VACINA (ID_VACINA, ID_EVENTO_CLINICO, NM_VACINA, NR_LOTE, DS_FABRICANTE, DT_PROXIMA_DOSE) " +
        "  VALUES (962, 962, 'LU02FW2-ONTEM-2330', 'L1', 'Fabricante Teste', " +
        "          DATEADD('MINUTE', 1410, CAST(DATEADD('DAY', -1, CURRENT_DATE) AS TIMESTAMP)))",

        "INSERT INTO EVENTO_CLINICO (ID_EVENTO, ID_PET, ID_CLINICA, ID_VETERINARIO, ID_TIPO_EVENTO, DS_OBSERVACAO) " +
        "  VALUES (963, 1, 1, 1, 3, 'LU-02 fix wave 2 - janela H2 (achado re-G2)')",
        "INSERT INTO VACINA (ID_VACINA, ID_EVENTO_CLINICO, NM_VACINA, NR_LOTE, DS_FABRICANTE, DT_PROXIMA_DOSE) " +
        "  VALUES (963, 963, 'LU02FW2-D31-0000', 'L1', 'Fabricante Teste', " +
        "          CAST(DATEADD('DAY', 31, CURRENT_DATE) AS TIMESTAMP))"
    })
    void janelaEDiasRestantesUsamTruncNaoCastComoData() {
        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT NM_VACINA, DIAS_RESTANTES FROM VW_VACINAS_VENCENDO " +
                "WHERE NM_VACINA LIKE 'LU02FW2-%' ORDER BY NM_VACINA");

        assertThat(linhas)
                .as("controle: as 4 doses foram inseridas contra PET 1 — se a view não devolver nada, "
                        + "o resto do teste é inconclusivo (nem as ausências esperadas provariam a janela)")
                .isNotEmpty();

        assertThat(diasRestantesDe(linhas, "LU02FW2-D30-2330"))
                .as("dose em hoje+30 às 23:30 tem que estar PRESENTE com DIAS_RESTANTES=30 — "
                        + "achado IMPORTANTE da re-G2: com CAST(...AS DATE) esta linha desaparecia no H2 "
                        + "enquanto o Oracle a incluía (TRUNC zera a hora nos dois motores; CAST só zera no Oracle)")
                .isPresent()
                .get()
                .isEqualTo(30L);

        assertThat(diasRestantesDe(linhas, "LU02FW2-HOJE-0000"))
                .as("dose de hoje às 00:00 tem que estar presente com DIAS_RESTANTES=0")
                .isPresent()
                .get()
                .isEqualTo(0L);

        assertThat(nomesPresentes(linhas))
                .as("dose de ontem às 23:30 não pode aparecer (fora da janela hoje..hoje+30)")
                .doesNotContain("LU02FW2-ONTEM-2330");

        assertThat(nomesPresentes(linhas))
                .as("dose em hoje+31 às 00:00 não pode aparecer (janela é hoje..hoje+30, inclusive)")
                .doesNotContain("LU02FW2-D31-0000");
    }

    private static java.util.Optional<Long> diasRestantesDe(List<Map<String, Object>> linhas, String nmVacina) {
        return linhas.stream()
                .filter(l -> nmVacina.equals(l.get("NM_VACINA")))
                .map(l -> ((Number) l.get("DIAS_RESTANTES")).longValue())
                .findFirst();
    }

    private static List<String> nomesPresentes(List<Map<String, Object>> linhas) {
        return linhas.stream().map(l -> (String) l.get("NM_VACINA")).toList();
    }
}
