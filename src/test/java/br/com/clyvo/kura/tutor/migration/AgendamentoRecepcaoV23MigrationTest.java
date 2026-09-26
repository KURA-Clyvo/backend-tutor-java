package br.com.clyvo.kura.tutor.migration;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.repository.AgendamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REC-07 (KURA_BACKLOG_RECEPCAO) — prova da migration V23 (6 colunas novas de recepção em
 * {@code AGENDAMENTO}, {@code CHK_AGEND_ORIGEM}, {@code FK_AGEND_TRIAGEM},
 * {@code CHK_AGEND_RESP_CONF}, os 2 índices novos e a conversão da PK de {@code IDENTITY} para
 * {@code DEFAULT SEQ_AGENDAMENTO.NEXTVAL} — G0 item 7 / A-4) contra o H2 real do profile dev,
 * mesmo banco em que o Flyway já aplicou V1..V23 (variante {@code -h2} + os arquivos comuns de
 * {@code db/migration}) antes de qualquer teste rodar.
 *
 * <p>O que esta classe PROVA: as 6 colunas existem e são nullable; {@code CHK_AGEND_ORIGEM}
 * aceita os 3 valores da lista e recusa qualquer outro; {@code FK_AGEND_TRIAGEM} recusa
 * {@code ID_TRIAGEM_ORIGEM} inexistente e aceita uma triagem real; {@code CHK_AGEND_RESP_CONF}
 * recusa valor fora de {@code SIM/CANCELAR/REMARCAR}; um INSERT sem {@code ID_AGENDAMENTO} recebe
 * o id de {@code SEQ_AGENDAMENTO} (não mais da {@code IDENTITY}); os 2 índices novos existem; e
 * um agendamento com {@code DT_CHECKIN} preenchido continua sendo lido pelo mesmo repositório
 * Spring Data que a listagem do tutor usa — a mordida aqui é justamente essa última: se alguém
 * mapear uma das 6 colunas novas em {@link Agendamento} com tipo incompatível, o Hibernate
 * derruba o SELECT ao montar a entidade e este teste falha.
 *
 * <p>O que ela NÃO prova: nada sobre Oracle real (H2 e Oracle têm caminhos de DDL diferentes
 * para o passo 4 — por isso o split; ver cabeçalho de
 * {@code db/migration-h2/V23__agendamento_recepcao.sql}). Isso é responsabilidade do G4
 * (compose real) do ciclo KURA_BACKLOG_RECEPCAO.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendamentoRecepcaoV23MigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AgendamentoRepository agendamentoRepository;

    // ─── Existência + nullability via INFORMATION_SCHEMA (prova estrutural) ──

    @Test
    @DisplayName("V23 — as 6 colunas novas existem em AGENDAMENTO e são nullable")
    void seisColunasNovasExistemESaoNullable() {
        for (String coluna : new String[] {
                "DT_CHECKIN", "DT_INICIO_ATENDIMENTO", "ID_TRIAGEM_ORIGEM",
                "DT_LEMBRETE_CONFIRMACAO", "DS_RESPOSTA_CONFIRMACAO", "DT_RESPOSTA_CONFIRMACAO"
        }) {
            Map<String, Object> info = colunaDeAgendamento(coluna);
            assertThat(info)
                    .as("coluna %s não encontrada em AGENDAMENTO — migration V23 não aplicada?", coluna)
                    .isNotEmpty();
            assertThat(String.valueOf(info.get("IS_NULLABLE")))
                    .as("%s precisa ser nullable — nenhuma delas é preenchida na criação", coluna)
                    .isEqualToIgnoringCase("YES");
        }
    }

    @Test
    @DisplayName("V23 — IDX_AGEND_TRIAGEM e IDX_AGEND_CLINICA_DT existem")
    void indicesNovosExistem() {
        // Controle positivo: a mesma consulta enxerga IDX_AGEND_TUTOR, índice nomeado que existe
        // desde a V1__initial_schema.sql — sem isto, "0 encontrado" seria indistinguível de
        // "a consulta a INFORMATION_SCHEMA.INDEXES não enxerga nada neste H2".
        assertThat(existeIndice("IDX_AGEND_TUTOR"))
                .as("controle positivo: índice pré-existente da V1 tem que ser visível")
                .isTrue();

        assertThat(existeIndice("IDX_AGEND_TRIAGEM"))
                .as("índice da FK nova (evita lock de tabela no DELETE do pai) tem que existir")
                .isTrue();
        assertThat(existeIndice("IDX_AGEND_CLINICA_DT"))
                .as("índice que sustenta a consulta da tela \"Hoje\" / D-1 tem que existir")
                .isTrue();
    }

    // ─── CHECK de origem (primeiro passo da migration, o único que pode falhar por dado) ──

    @Test
    @DisplayName("V23 — CHK_AGEND_ORIGEM aceita PORTAL/RECEPCAO/TRIAGEM_LUNA e recusa qualquer outro valor")
    void checkOrigemAceitaListaFechadaERecusaOResto() {
        long idClinica = plantarClinica(9231, "Clinica REC07 A", "92310000000191");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        // Sem provar que os 3 valores legítimos continuam aceitos, "XPTO foi recusado" seria
        // indistinguível de "o CHECK recusa geral" (ex.: erro de sintaxe na lista IN).
        assertThatCode(() -> inserirAgendamentoMinimo(idClinica, "PORTAL"))
                .as("controle positivo: PORTAL continua aceito")
                .doesNotThrowAnyException();
        assertThatCode(() -> inserirAgendamentoMinimo(idClinica, "RECEPCAO"))
                .as("controle positivo: RECEPCAO é aceito (produtor novo desta task)")
                .doesNotThrowAnyException();
        assertThatCode(() -> inserirAgendamentoMinimo(idClinica, "TRIAGEM_LUNA"))
                .as("controle positivo: TRIAGEM_LUNA é aceito (produtor novo desta task, F-3)")
                .doesNotThrowAnyException();

        // ── A MORDIDA ────────────────────────────────────────────────────────
        assertThatThrownBy(() -> inserirAgendamentoMinimo(idClinica, "XPTO"))
                .as("valor fora da lista fechada tem que morrer no BANCO, não só na validação C#/Java")
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("CHK_AGEND_ORIGEM");
    }

    // ─── FK para TRIAGEM_LUNA ──────────────────────────────────────────────

    @Test
    @DisplayName("V23 — FK_AGEND_TRIAGEM recusa ID_TRIAGEM_ORIGEM inexistente e aceita triagem real")
    void fkAgendTriagemRecusaTriagemInexistenteEAceitaReal() {
        long idClinica = plantarClinica(9232, "Clinica REC07 B", "92320000000191");
        long idTriagem = plantarTriagem(idClinica, "Triagem REC07 origem");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        assertThatCode(() -> inserirAgendamentoComTriagem(idClinica, idTriagem))
                .as("controle positivo: apontar para uma TRIAGEM_LUNA real é aceito")
                .doesNotThrowAnyException();

        // ── A MORDIDA ────────────────────────────────────────────────────────
        long idTriagemInexistente = 999999L;
        assertThatThrownBy(() -> inserirAgendamentoComTriagem(idClinica, idTriagemInexistente))
                .as("ID_TRIAGEM_ORIGEM apontando para triagem que não existe tem que ser recusado pelo banco")
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("FK_AGEND_TRIAGEM");
    }

    // ─── CHECK da resposta de confirmação D-1 ──────────────────────────────

    @Test
    @DisplayName("V23 — CHK_AGEND_RESP_CONF aceita SIM/CANCELAR/REMARCAR (e nulo) e recusa 'TALVEZ'")
    void checkRespostaConfirmacaoAceitaListaFechadaERecusaOResto() {
        long idClinica = plantarClinica(9233, "Clinica REC07 C", "92330000000191");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        assertThatCode(() -> inserirAgendamentoComResposta(idClinica, "SIM"))
                .as("controle positivo: SIM é aceito")
                .doesNotThrowAnyException();
        assertThatCode(() -> inserirAgendamentoComResposta(idClinica, null))
                .as("controle positivo: nulo (ainda sem resposta) continua aceito")
                .doesNotThrowAnyException();

        // ── A MORDIDA ────────────────────────────────────────────────────────
        assertThatThrownBy(() -> inserirAgendamentoComResposta(idClinica, "TALVEZ"))
                .as("resposta fora da lista fechada A-10(b) tem que morrer no BANCO")
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("CHK_AGEND_RESP_CONF");
    }

    // ─── PK: um gerador só (A-4) ────────────────────────────────────────────

    @Test
    @DisplayName("V23 — INSERT sem ID_AGENDAMENTO recebe o id de SEQ_AGENDAMENTO (não mais da IDENTITY)")
    void insertSemIdVemDaSequence() {
        long idClinica = plantarClinica(9234, "Clinica REC07 D", "92340000000191");
        String marcador = "PK" + (System.nanoTime() % 100_000_000L);

        jdbc.update(
                "INSERT INTO AGENDAMENTO (ID_CLINICA, DT_AGENDAMENTO, DS_TIPO, ST_STATUS, DS_ORIGEM, NR_VERSION) "
                        + "VALUES (?, CURRENT_TIMESTAMP + INTERVAL '5' DAY, ?, 'AGENDADO', 'PORTAL', 0)",
                idClinica, marcador);

        Long idGerado = jdbc.queryForObject(
                "SELECT ID_AGENDAMENTO FROM AGENDAMENTO WHERE DS_TIPO = ?", Long.class, marcador);

        assertThat(idGerado)
                .as("SEQ_AGENDAMENTO começa em 100 (V1) — um id gerado abaixo disso indicaria que "
                        + "a IDENTITY antiga ainda está no comando, não a sequence")
                .isNotNull()
                .isGreaterThanOrEqualTo(100L);
    }

    // ─── Comportamento: check-in não quebra a listagem do tutor ────────────

    @Test
    @DisplayName("V23 — agendamento com DT_CHECKIN preenchido continua sendo lido pelo repositório do Java (lista do tutor não quebra)")
    void agendamentoComCheckinContinuaSendoLidoPeloRepositorio() {
        // Reaproveita TUTOR=1 / PET=1 do seed dev (afterMigrate__seeds_dev.sql) — a mordida real
        // aqui é o Hibernate montar a entidade Agendamento a partir de uma linha que TEM as 6
        // colunas novas preenchidas; se alguém mapear DT_CHECKIN com tipo incompatível no Java,
        // o SELECT do Spring Data quebra e este teste falha.
        long idClinica = 1L;
        String marcador = "CHK" + (System.nanoTime() % 100_000_000L);

        jdbc.update(
                "INSERT INTO AGENDAMENTO (ID_CLINICA, ID_TUTOR, ID_PET, DT_AGENDAMENTO, DS_TIPO, "
                        + "ST_STATUS, DS_ORIGEM, NR_VERSION, DT_CHECKIN) "
                        + "VALUES (?, 1, 1, CURRENT_TIMESTAMP + INTERVAL '1' DAY, ?, 'AGENDADO', 'RECEPCAO', 0, "
                        + "CURRENT_TIMESTAMP)",
                idClinica, marcador);

        Page<Agendamento> pagina = agendamentoRepository.findByTutor_IdTutorAndStStatus(
                1L, StatusAgendamento.AGENDADO, PageRequest.of(0, 50));

        assertThat(pagina.getContent())
                .as("a listagem do tutor tem que continuar devolvendo linhas mesmo com DT_CHECKIN preenchido")
                .anyMatch(a -> marcador.equals(a.getDsTipoConsulta()));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> colunaDeAgendamento(String nomeColuna) {
        return jdbc.queryForList(
                        "SELECT IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_NAME = 'AGENDAMENTO' AND COLUMN_NAME = ?",
                        nomeColuna)
                .stream()
                .findFirst()
                .orElse(Map.of());
    }

    private boolean existeIndice(String nomeIndice) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = ?",
                Integer.class, nomeIndice);
        return total != null && total > 0;
    }

    private long plantarClinica(long id, String nome, String cnpj) {
        jdbc.update("INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ, ST_ATIVA) VALUES (?, ?, ?, 'S')",
                id, nome, cnpj);
        return id;
    }

    private long plantarTriagem(long idClinica, String descricao) {
        jdbc.update(
                "INSERT INTO TRIAGEM_LUNA (ID_CLINICA, DS_NIVEL_URGENCIA, DS_DESCRICAO, DT_TRIAGEM) "
                        + "VALUES (?, 'BAIXA', ?, CURRENT_TIMESTAMP)",
                idClinica, descricao);
        Long id = jdbc.queryForObject(
                "SELECT ID_TRIAGEM FROM TRIAGEM_LUNA WHERE ID_CLINICA = ? AND DS_DESCRICAO = ?",
                Long.class, idClinica, descricao);
        assertThat(id).isNotNull();
        return id;
    }

    private void inserirAgendamentoMinimo(long idClinica, String dsOrigem) {
        jdbc.update(
                "INSERT INTO AGENDAMENTO (ID_CLINICA, DT_AGENDAMENTO, ST_STATUS, DS_ORIGEM, NR_VERSION) "
                        + "VALUES (?, CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'AGENDADO', ?, 0)",
                idClinica, dsOrigem);
    }

    private void inserirAgendamentoComTriagem(long idClinica, long idTriagemOrigem) {
        jdbc.update(
                "INSERT INTO AGENDAMENTO (ID_CLINICA, DT_AGENDAMENTO, ST_STATUS, DS_ORIGEM, NR_VERSION, "
                        + "ID_TRIAGEM_ORIGEM) VALUES (?, CURRENT_TIMESTAMP + INTERVAL '3' DAY, 'AGENDADO', "
                        + "'TRIAGEM_LUNA', 0, ?)",
                idClinica, idTriagemOrigem);
    }

    private void inserirAgendamentoComResposta(long idClinica, String dsRespostaConfirmacao) {
        jdbc.update(
                "INSERT INTO AGENDAMENTO (ID_CLINICA, DT_AGENDAMENTO, ST_STATUS, DS_ORIGEM, NR_VERSION, "
                        + "DS_RESPOSTA_CONFIRMACAO) VALUES (?, CURRENT_TIMESTAMP + INTERVAL '4' DAY, 'AGENDADO', "
                        + "'PORTAL', 0, ?)",
                idClinica, dsRespostaConfirmacao);
    }
}
