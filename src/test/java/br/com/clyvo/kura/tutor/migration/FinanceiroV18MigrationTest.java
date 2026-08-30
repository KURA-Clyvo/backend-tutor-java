package br.com.clyvo.kura.tutor.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FD-07 — prova da migration V18 (SERVICO_PRECO e COBRANCA) contra o H2 real do profile
 * dev, que é o mesmo banco em que o Flyway já aplicou V1..V18 (variante -h2) antes de
 * qualquer teste rodar.
 *
 * <p>O que esta classe prova e o que ela NÃO prova:
 * <ul>
 *   <li><b>Prova</b> que o DDL da variante -h2 aplica (se não aplicasse, o Flyway
 *       derrubaria o startup do contexto e nenhum teste desta suíte rodaria), que as
 *       constraints mordem, que a PK vem da sequence, que o dinheiro tem escala 2 de
 *       verdade e — o mais importante — que <b>mudar o preço de tabela NÃO reescreve
 *       cobrança já lançada</b>, que é a razão de existir de VL_COBRADO.</li>
 *   <li><b>NÃO prova</b> nada sobre a variante -oracle rodando em Oracle real: nenhum
 *       teste desta suíte toca Oracle (a conta da FIAP está bloqueada, ORA-28000). O que
 *       existe aqui é a garantia estrutural de que as duas variantes são idênticas exceto
 *       pelas duas linhas do DEFAULT da PK
 *       ({@link #variantesDiferemApenasNasLinhasDoDefaultDaPk()}) — portabilidade
 *       argumentada + travada, não medida em Oracle.</li>
 *   <li><b>NÃO prova</b> desempenho de índice. Prova apenas que os 2 índices declarados
 *       existem no schema, para que remover um deles quebre a suíte em vez de virar
 *       regressão silenciosa de plano.</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinanceiroV18MigrationTest {

    private static final String ARQUIVO_H2 = "db/migration-h2/V18__financeiro.sql";
    private static final String ARQUIVO_ORACLE = "db/migration-oracle/V18__financeiro.sql";

    @Autowired
    JdbcTemplate jdbc;

    // ─── A razão de existir de VL_COBRADO (override da ruling D-2) ───────────

    @Test
    @DisplayName("D-2 override — mudar o preço de tabela NÃO reescreve cobrança já lançada")
    void mudarPrecoDeTabelaNaoReescreveHistoricoFinanceiro() {
        long clinica = plantarClinica(8101, "Clinica financeiro A", "81010000000191");
        long evento = plantarEventoClinico(clinica, 8101);

        long servico = inserirServico(clinica, "Consulta de rotina", new BigDecimal("150.00"));
        long cobranca = inserirCobranca(evento, clinica, servico, new BigDecimal("150.00"), "PIX");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        // A asserção-alvo abaixo é "o valor NÃO mudou". Um "não mudou" só é interpretável
        // depois de provar que este mesmo instrumento ENXERGA uma mudança quando ela
        // existe: aqui, um UPDATE direto em VL_COBRADO é detectado.
        jdbc.update("UPDATE COBRANCA SET VL_COBRADO = 999.99 WHERE ID_COBRANCA = ?", cobranca);
        assertThat(valorCobrado(cobranca))
                .as("controle positivo: a leitura enxerga alteração de VL_COBRADO quando ela ocorre")
                .isEqualByComparingTo("999.99");
        jdbc.update("UPDATE COBRANCA SET VL_COBRADO = 150.00 WHERE ID_COBRANCA = ?", cobranca);
        assertThat(valorCobrado(cobranca)).isEqualByComparingTo("150.00");

        // ── A MORDIDA ────────────────────────────────────────────────────────
        jdbc.update("UPDATE SERVICO_PRECO SET VL_PRECO = 300.00 WHERE ID_SERVICO_PRECO = ?", servico);

        assertThat(precoDeTabela(servico))
                .as("o catálogo mudou de verdade — o UPDATE acima não foi no-op")
                .isEqualByComparingTo("300.00");
        assertThat(valorCobrado(cobranca))
                .as("o histórico financeiro NÃO se move: VL_COBRADO é cópia do momento do "
                        + "lançamento, não leitura por FK. Se esta asserção cair, um relatório "
                        + "de mês fechado passa a mudar de valor sozinho.")
                .isEqualByComparingTo("150.00");

        // A FK continua apontando para o serviço — ela é rastreabilidade de ORIGEM
        // (mix por serviço da FD-11), nunca fonte de valor.
        // Numero puro: o driver devolve Integer para NUMBER(10), entao compara-se o valor,
        // nao o box (assertThat(Object).isEqualTo(long) falharia com "103 != 103L").
        assertThat(((Number) colunaDaCobranca(cobranca, "ID_SERVICO_PRECO")).longValue())
                .isEqualTo(servico);
    }

    @Test
    @DisplayName("D-2 — cobrança avulsa: ID_SERVICO_PRECO nullable, VL_COBRADO obrigatório")
    void cobrancaAvulsaNaoPrecisaDeServicoTabeladoMasPrecisaDeValor() {
        long clinica = plantarClinica(8201, "Clinica financeiro B", "82010000000192");
        long evento = plantarEventoClinico(clinica, 8201);

        assertThatCode(() -> inserirCobranca(evento, clinica, null, new BigDecimal("80.00"), "DINHEIRO"))
                .as("valor avulso, sem serviço tabelado, é lançamento legítimo (D-2)")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO COBRANCA (ID_EVENTO_CLINICO, ID_CLINICA, VL_COBRADO) VALUES (?, ?, NULL)",
                evento, clinica))
                .as("não existe lançamento sem valor")
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatCode(() -> jdbc.update(
                "INSERT INTO COBRANCA (ID_EVENTO_CLINICO, ID_CLINICA, VL_COBRADO) VALUES (?, ?, 50.00)",
                evento, clinica))
                .as("DS_FORMA_PAGAMENTO é nullable: o veterinário não é obrigado a escolhê-la "
                        + "no meio do atendimento (princípio de desenho da FD-10)")
                .doesNotThrowAnyException();
    }

    // ─── Dinheiro ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NUMBER(10,2) — o H2 aceita e a escala de 2 decimais é real, não decorativa")
    void dinheiroTemEscalaDeDoisDecimaisDeVerdade() {
        long clinica = plantarClinica(8301, "Clinica dinheiro", "83010000000193");
        long servico = inserirServico(clinica, "Servico centavos", new BigDecimal("1234.56"));

        BigDecimal lido = precoDeTabela(servico);
        assertThat(lido)
                .as("centavos sobrevivem à ida e volta — coluna não é inteira")
                .isEqualByComparingTo("1234.56");
        assertThat(lido.scale())
                .as("o driver devolve escala 2, ou seja, a coluna é mesmo NUMERIC(10,2) "
                        + "(NUMBER(10,2) é aceito pelo H2 sob MODE=Oracle — medido, não assumido)")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CHECK de valor — negativo é rejeitado nas 2 tabelas; zero (cortesia) é aceito")
    void valorNegativoEhRejeitado() {
        long clinica = plantarClinica(8401, "Clinica valor", "84010000000194");
        long evento = plantarEventoClinico(clinica, 8401);

        assertThatCode(() -> inserirServico(clinica, "Cortesia", BigDecimal.ZERO))
                .as("zero é lançamento legítimo — cortesia")
                .doesNotThrowAnyException();
        assertThatCode(() -> inserirCobranca(evento, clinica, null, BigDecimal.ZERO, null))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> inserirServico(clinica, "Preco negativo", new BigDecimal("-1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserirCobranca(evento, clinica, null, new BigDecimal("-0.01"), null))
                .as("cobrança negativa é a forma natural de improvisar estorno sem migration — "
                        + "e estorno está fora de escopo (FD-10)")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── Integridade referencial e tenant ────────────────────────────────────

    @Test
    @DisplayName("FK — COBRANCA referencia EVENTO_CLINICO(ID_EVENTO), e a FK morde")
    void fkParaEventoClinicoApontaParaIdEvento() {
        long clinica = plantarClinica(8501, "Clinica fk", "85010000000195");
        long evento = plantarEventoClinico(clinica, 8501);

        assertThatCode(() -> inserirCobranca(evento, clinica, null, new BigDecimal("10.00"), null))
                .as("evento existente é aceito — a FK resolve contra ID_EVENTO, não contra um "
                        + "ID_EVENTO_CLINICO que não existe em EVENTO_CLINICO")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> inserirCobranca(987654L, clinica, null, new BigDecimal("10.00"), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ID_CLINICA — NOT NULL nas 2 tabelas: não existe dado financeiro sem tenant")
    void semClinicaNaoHaFinanceiro() {
        long clinica = plantarClinica(8601, "Clinica tenant", "86010000000196");
        long evento = plantarEventoClinico(clinica, 8601);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO SERVICO_PRECO (ID_CLINICA, NM_SERVICO, VL_PRECO) VALUES (NULL, 'x', 1.00)"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO COBRANCA (ID_EVENTO_CLINICO, ID_CLINICA, VL_COBRADO) VALUES (?, NULL, 1.00)",
                evento))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ST_ATIVA — soft delete no padrão do projeto (S/N), com DEFAULT 'S'")
    void softDeleteSegueOPadraoDoProjeto() {
        long clinica = plantarClinica(8701, "Clinica soft delete", "87010000000197");
        long servico = inserirServico(clinica, "Servico ativo", new BigDecimal("10.00"));

        assertThat(((String) colunaDoServico(servico, "ST_ATIVA")).trim()).isEqualTo("S");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO SERVICO_PRECO (ID_CLINICA, NM_SERVICO, VL_PRECO, ST_ATIVA) "
                        + "VALUES (?, 'x', 1.00, 'X')", clinica))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("PK — vem da sequence (padrão .NET-owned), nunca de IDENTITY")
    void pkVemDaSequence() {
        long clinica = plantarClinica(8801, "Clinica pk", "88010000000198");
        long evento = plantarEventoClinico(clinica, 8801);

        long servico = inserirServico(clinica, "Servico pk", new BigDecimal("10.00"));
        long cobranca = inserirCobranca(evento, clinica, servico, new BigDecimal("10.00"), null);

        assertThat(servico)
                .as("SEQ_SERVICO_PRECO começa em 100 — id abaixo disso denunciaria IDENTITY")
                .isGreaterThanOrEqualTo(100L);
        assertThat(cobranca).isGreaterThanOrEqualTo(100L);

        long outroServico = inserirServico(clinica, "Outro servico pk", new BigDecimal("20.00"));
        assertThat(outroServico).isNotEqualTo(servico);
    }

    // ─── Os índices declarados existem ───────────────────────────────────────

    @Test
    @DisplayName("índices — os 2 índices argumentados no cabeçalho da V18 existem no schema")
    void indicesDaV18Existem() {
        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        // Um "achei 0" é afirmação sobre o alcance do instrumento, não sobre o schema.
        // Antes de afirmar que os índices existem, provamos que este mesmo contador
        // devolve 0 para um índice que sabidamente NÃO existe — ou seja, ele discrimina.
        assertThat(contarIndice("IDX_QUE_NAO_EXISTE_FD07"))
                .as("controle positivo: o contador devolve 0 para índice inexistente, "
                        + "logo o 1 abaixo não é um match acidental de instrumento cego")
                .isZero();

        assertThat(contarIndice("IDX_COBRANCA_CLINICA_DATA"))
                .as("KPI da FD-11: clínica (igualdade) + período (faixa), nessa ordem")
                .isEqualTo(1);
        assertThat(contarIndice("IDX_COBRANCA_EVENTO"))
                .as("leitura \"cobranças deste atendimento\" (FD-10) e FK filha indexada")
                .isEqualTo(1);
    }

    // ─── O split -oracle/-h2 é mínimo, e isso fica travado ───────────────────

    @Test
    @DisplayName("split — as variantes da V18 diferem em EXATAMENTE 2 linhas: os DEFAULT das PKs")
    void variantesDiferemApenasNasLinhasDoDefaultDaPk() {
        List<String> h2 = linhasSqlSignificativas(lerRecurso(ARQUIVO_H2));
        List<String> oracle = linhasSqlSignificativas(lerRecurso(ARQUIVO_ORACLE));

        assertThat(h2).hasSameSizeAs(oracle);

        List<Integer> divergentes = java.util.stream.IntStream.range(0, h2.size())
                .filter(i -> !h2.get(i).equals(oracle.get(i)))
                .boxed()
                .toList();

        assertThat(divergentes)
                .as("qualquer terceira divergência é dívida de split silenciosa — h2=%s / oracle=%s",
                        divergentes.stream().map(h2::get).toList(),
                        divergentes.stream().map(oracle::get).toList())
                .hasSize(2);

        assertThat(h2.get(divergentes.get(0)))
                .contains("ID_SERVICO_PRECO")
                .contains("DEFAULT NEXT VALUE FOR SEQ_SERVICO_PRECO");
        assertThat(oracle.get(divergentes.get(0)))
                .contains("ID_SERVICO_PRECO")
                .contains("DEFAULT SEQ_SERVICO_PRECO.NEXTVAL");

        assertThat(h2.get(divergentes.get(1)))
                .contains("ID_COBRANCA")
                .contains("DEFAULT NEXT VALUE FOR SEQ_COBRANCA");
        assertThat(oracle.get(divergentes.get(1)))
                .contains("ID_COBRANCA")
                .contains("DEFAULT SEQ_COBRANCA.NEXTVAL");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private long plantarClinica(long id, String nome, String cnpj) {
        jdbc.update("INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ, ST_ATIVA) VALUES (?, ?, ?, 'S')",
                id, nome, cnpj);
        return id;
    }

    /** Cria VETERINARIO + TIPO_EVENTO + EVENTO_CLINICO mínimos e devolve o ID_EVENTO. */
    private long plantarEventoClinico(long idClinica, long semente) {
        long idVet = semente;
        long idTipo = semente;
        long idEvento = semente;
        jdbc.update("INSERT INTO VETERINARIO (ID_VETERINARIO, ID_CLINICA, NM_VETERINARIO) VALUES (?, ?, ?)",
                idVet, idClinica, "Vet FD07 " + semente);
        // CD_TIPO e NOT NULL + UNIQUE desde a V9 (chave de negocio lida pelo .NET); NM_TIPO
        // tambem e UNIQUE desde a V1. Ambos derivados da semente para nao colidir com o
        // catalogo semeado pela V14 nem entre os testes desta classe.
        jdbc.update("INSERT INTO TIPO_EVENTO (ID_TIPO_EVENTO, CD_TIPO, NM_TIPO) VALUES (?, ?, ?)",
                idTipo, "FD07T" + semente, "FD07-TIPO-" + semente);
        jdbc.update("INSERT INTO EVENTO_CLINICO (ID_EVENTO, ID_CLINICA, ID_VETERINARIO, "
                        + "ID_TIPO_EVENTO, DS_OBSERVACAO) VALUES (?, ?, ?, ?, ?)",
                idEvento, idClinica, idVet, idTipo, "Atendimento de teste FD-07");
        return idEvento;
    }

    private long inserirServico(long idClinica, String nome, BigDecimal preco) {
        jdbc.update("INSERT INTO SERVICO_PRECO (ID_CLINICA, NM_SERVICO, VL_PRECO) VALUES (?, ?, ?)",
                idClinica, nome, preco);
        Long id = jdbc.queryForObject(
                "SELECT ID_SERVICO_PRECO FROM SERVICO_PRECO WHERE ID_CLINICA = ? AND NM_SERVICO = ?",
                Long.class, idClinica, nome);
        return id == null ? -1L : id;
    }

    private long inserirCobranca(long idEvento, long idClinica, Long idServico,
                                 BigDecimal valor, String formaPagamento) {
        jdbc.update("INSERT INTO COBRANCA (ID_EVENTO_CLINICO, ID_CLINICA, ID_SERVICO_PRECO, "
                        + "VL_COBRADO, DS_FORMA_PAGAMENTO) VALUES (?, ?, ?, ?, ?)",
                idEvento, idClinica, idServico, valor, formaPagamento);
        Long id = jdbc.queryForObject(
                "SELECT MAX(ID_COBRANCA) FROM COBRANCA WHERE ID_EVENTO_CLINICO = ?", Long.class, idEvento);
        return id == null ? -1L : id;
    }

    private BigDecimal precoDeTabela(long idServico) {
        return jdbc.queryForObject("SELECT VL_PRECO FROM SERVICO_PRECO WHERE ID_SERVICO_PRECO = ?",
                BigDecimal.class, idServico);
    }

    private BigDecimal valorCobrado(long idCobranca) {
        return jdbc.queryForObject("SELECT VL_COBRADO FROM COBRANCA WHERE ID_COBRANCA = ?",
                BigDecimal.class, idCobranca);
    }

    private Object colunaDaCobranca(long idCobranca, String coluna) {
        Map<String, Object> linha = jdbc.queryForMap(
                "SELECT * FROM COBRANCA WHERE ID_COBRANCA = ?", idCobranca);
        return linha.get(coluna);
    }

    private Object colunaDoServico(long idServico, String coluna) {
        Map<String, Object> linha = jdbc.queryForMap(
                "SELECT * FROM SERVICO_PRECO WHERE ID_SERVICO_PRECO = ?", idServico);
        return linha.get(coluna);
    }

    private int contarIndice(String nome) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = ?",
                Integer.class, nome);
        return total == null ? 0 : total;
    }

    private static List<String> linhasSqlSignificativas(String conteudo) {
        return conteudo.lines()
                .map(String::trim)
                .filter(linha -> !linha.isEmpty() && !linha.startsWith("--"))
                .map(linha -> linha.replaceAll("\\s+", " "))
                .toList();
    }

    /**
     * Le o arquivo de migration <b>normalizando CRLF para LF</b>.
     *
     * <p>Sem isso a comparacao entre as variantes e byte a byte incluindo {@code \r}, e este repo
     * tem {@code core.autocrlf=true} e nenhum {@code .gitattributes}: basta um checkout no Windows
     * deixar um arquivo em CRLF e o outro em LF para a suite ficar VERMELHA sobre um blob correto.
     * A FD-01 mediu exatamente esse falso positivo; a V18 herda a defesa em vez de repetir o erro.
     *
     * <p>O proposito e detectar <b>divergencia semantica</b> entre as duas variantes, nao
     * divergencia de fim de linha.
     */
    private static String lerRecurso(String caminho) {
        try (InputStream in = new ClassPathResource(caminho).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + caminho, e);
        }
    }
}
