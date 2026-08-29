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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FD-01 — prova da migration V17 (USUARIO_CLINICA) contra o H2 real do profile dev,
 * que é o mesmo banco em que o Flyway já aplicou as migrations V1..V17 (variante -h2)
 * antes de qualquer teste rodar.
 *
 * <p>O que esta classe prova e o que ela NÃO prova:
 * <ul>
 *   <li><b>Prova</b> que o DDL da variante -h2 aplica (se não aplicasse, o contexto do
 *       Spring nem subiria — o Flyway falha o startup), que as constraints existem e
 *       mordem, e que o {@code INSERT ... SELECT} de conversão (D-10) converte o dado
 *       certo quando existe dado a converter.</li>
 *   <li><b>NÃO prova</b> nada sobre a variante -oracle rodando em Oracle real: nenhum
 *       teste desta suíte toca Oracle. O que existe aqui é a garantia estrutural de que
 *       as duas variantes são idênticas exceto pela linha do DEFAULT da PK
 *       ({@link #variantesDiferemApenasNaLinhaDoDefaultDaPk()}) — portabilidade
 *       argumentada + travada, não medida em Oracle.</li>
 *   <li><b>NÃO prova</b> que a conversão converte alguma coisa em ambiente do zero: ela
 *       converte ZERO linha, por construção — o seed do profile dev não popula
 *       CLINICA.DS_EMAIL_ACESSO, e os outros produtores dessa coluna rodam em runtime,
 *       depois das migrations. A conversão só tem efeito em base já existente, e é por
 *       isso que a prova abaixo PLANTA a clínica antes de executar o bloco.</li>
 * </ul>
 *
 * <p>O SQL de conversão exercitado aqui é <b>extraído do próprio arquivo de migration</b>
 * (entre os marcadores {@code >>> BEGIN CONVERSAO_D10} / {@code <<< END CONVERSAO_D10}),
 * nunca copiado à mão: SQL transcrito no teste provaria a cópia, não o que foi entregue.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioClinicaV17MigrationTest {

    private static final String ARQUIVO_H2 = "db/migration-h2/V17__usuario_clinica.sql";
    private static final String ARQUIVO_ORACLE = "db/migration-oracle/V17__usuario_clinica.sql";

    private static final String MARCADOR_INICIO = "-- >>> BEGIN CONVERSAO_D10";
    private static final String MARCADOR_FIM = "-- <<< END CONVERSAO_D10";

    /** Hash BCrypt real (60 chars) — comparado caractere a caractere para pegar truncamento. */
    private static final String HASH_A = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String HASH_C = "$2a$10$abcdefghijklmnopqrstuvALPHAbetaGammaDeltaEpsilonZetaEt";

    @Autowired
    JdbcTemplate jdbc;

    // ─── Prova principal: conversão do dado (ruling D-10) ────────────────────

    @Test
    @DisplayName("conversão D-10 — cria GESTOR com o mesmo e-mail e o MESMO hash, só para clínica com credencial")
    void conversaoCriaGestorComMesmoEmailEHash() {
        plantarClinica(9001, "Clinica A com credencial", "90010000000191",
                "gestor.a@kura.test", HASH_A, "S");
        plantarClinica(9002, "Clinica B sem credencial", "90020000000192",
                null, null, "S");
        plantarClinica(9003, "Clinica C inativa com credencial", "90030000000193",
                "gestor.c@kura.test", HASH_C, "N");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        // Um "0" só é interpretável depois de provar que o instrumento enxerga "1".
        // Passo 1: com uma linha plantada à mão, o contador devolve 1.
        jdbc.update("INSERT INTO USUARIO_CLINICA (ID_CLINICA, DS_EMAIL, DS_SENHA_HASH, TP_PERFIL) "
                + "VALUES (9002, 'controle.positivo@kura.test', 'hash-irrelevante', 'GESTOR')");
        assertThat(contarUsuarios(9002))
                .as("controle positivo: o contador enxerga a linha que existe")
                .isEqualTo(1);
        // Passo 2: removida a linha, o mesmo contador devolve 0 — ou seja, o 0 abaixo
        // significa ausência de linha, não instrumento cego.
        jdbc.update("DELETE FROM USUARIO_CLINICA WHERE ID_CLINICA = 9002");
        assertThat(contarUsuarios(9002)).isZero();
        assertThat(contarUsuarios(9001))
                .as("estado ANTES da conversão: nenhuma das clínicas plantadas tem usuário")
                .isZero();
        assertThat(contarUsuarios(9003)).isZero();

        // ── EXECUÇÃO do bloco literal extraído do arquivo de migration ───────
        int linhasConvertidas = jdbc.update(sqlDeConversaoDoArquivo(ARQUIVO_H2));

        assertThat(linhasConvertidas)
                .as("converte só as 2 clínicas com DS_EMAIL_ACESSO + DS_SENHA_HASH preenchidos")
                .isEqualTo(2);

        Map<String, Object> gestorA = usuarioDa(9001);
        assertThat(gestorA.get("DS_EMAIL")).isEqualTo("gestor.a@kura.test");
        assertThat(gestorA.get("DS_SENHA_HASH"))
                .as("mesmo hash, sem truncamento — é o que faz a senha atual continuar valendo")
                .isEqualTo(HASH_A);
        assertThat(gestorA.get("TP_PERFIL")).isEqualTo("GESTOR");
        assertThat(gestorA.get("ID_VETERINARIO"))
                .as("vínculo com VETERINARIO nunca é adivinhado na conversão")
                .isNull();
        assertThat(((String) gestorA.get("ST_ATIVA")).trim()).isEqualTo("S");
        assertThat(gestorA.get("DT_CRIACAO")).isNotNull();

        assertThat(contarUsuarios(9002))
                .as("clínica sem DS_EMAIL_ACESSO não gera usuário — o WHERE morde de verdade")
                .isZero();

        Map<String, Object> gestorC = usuarioDa(9003);
        assertThat(((String) gestorC.get("ST_ATIVA")).trim())
                .as("clínica desativada não vira usuário ativo: a conversão preserva o estado")
                .isEqualTo("N");

        // PK vem da sequence, avaliada POR LINHA num INSERT ... SELECT multi-linha.
        assertThat(gestorA.get("ID_USUARIO_CLINICA"))
                .isNotNull()
                .isNotEqualTo(gestorC.get("ID_USUARIO_CLINICA"));
    }

    @Test
    @DisplayName("conversão D-10 — reexecutar não duplica (guarda NOT EXISTS contra UK_USUARIO_CLINICA_EMAIL)")
    void conversaoEhIdempotente() {
        plantarClinica(9101, "Clinica idempotencia", "91010000000191",
                "gestor.idem@kura.test", HASH_A, "S");

        String conversao = sqlDeConversaoDoArquivo(ARQUIVO_H2);
        assertThat(jdbc.update(conversao)).isEqualTo(1);
        assertThat(jdbc.update(conversao))
                .as("segunda execução não insere nada — e não estoura a unique")
                .isZero();
        assertThat(contarUsuarios(9101)).isEqualTo(1);
    }

    @Test
    @DisplayName("conversão D-10 — em ambiente do zero (seed dev) converte ZERO linha, por construção")
    void conversaoConverteZeroEmAmbienteDoZero() {
        Integer clinicasComCredencial = jdbc.queryForObject(
                "SELECT COUNT(*) FROM CLINICA WHERE DS_EMAIL_ACESSO IS NOT NULL", Integer.class);

        assertThat(clinicasComCredencial)
                .as("o seed do profile dev não popula CLINICA.DS_EMAIL_ACESSO — logo a V17 "
                        + "converteu 0 linha nesta base, e isso NÃO é defeito: o par que cria o "
                        + "usuário no registro em runtime é responsabilidade da FD-03")
                .isZero();

        Integer usuarios = jdbc.queryForObject("SELECT COUNT(*) FROM USUARIO_CLINICA", Integer.class);
        assertThat(usuarios).isZero();
    }

    // ─── Constraints da tabela ───────────────────────────────────────────────

    @Test
    @DisplayName("TP_PERFIL — CHECK aceita GESTOR e VETERINARIO e rejeita qualquer outro papel")
    void checkDePerfilMorde() {
        plantarClinica(9201, "Clinica perfil", "92010000000191", null, null, "S");

        assertThatCode(() -> inserirUsuario(9201, "gestor@kura.test", "GESTOR"))
                .doesNotThrowAnyException();
        assertThatCode(() -> inserirUsuario(9201, "vet@kura.test", "VETERINARIO"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> inserirUsuario(9201, "recep@kura.test", "RECEPCIONISTA"))
                .as("papel novo exige migration que altere CHK_USUARIO_CLINICA_PERFIL — e só ela")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("e-mail é único POR CLÍNICA, não globalmente")
    void unicidadeDeEmailEhPorClinica() {
        plantarClinica(9301, "Clinica X", "93010000000191", null, null, "S");
        plantarClinica(9302, "Clinica Y", "93020000000192", null, null, "S");

        inserirUsuario(9301, "mesmo.email@kura.test", "VETERINARIO");

        assertThatCode(() -> inserirUsuario(9302, "mesmo.email@kura.test", "VETERINARIO"))
                .as("o mesmo profissional pode existir em duas clínicas")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> inserirUsuario(9301, "mesmo.email@kura.test", "GESTOR"))
                .as("duas pessoas da MESMA clínica não podem repetir e-mail")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ID_VETERINARIO — nullable, mas a FK morde quando preenchido com id inexistente")
    void vinculoComVeterinarioEhOpcionalMasIntegro() {
        plantarClinica(9401, "Clinica vinculo", "94010000000191", null, null, "S");

        assertThatCode(() -> inserirUsuario(9401, "gestor.sem.vet@kura.test", "GESTOR"))
                .as("um GESTOR pode não ser veterinário")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO USUARIO_CLINICA (ID_CLINICA, ID_VETERINARIO, DS_EMAIL, DS_SENHA_HASH, TP_PERFIL) "
                        + "VALUES (9401, 987654, 'vet.fantasma@kura.test', 'hash', 'VETERINARIO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ID_CLINICA — NOT NULL: não existe usuário sem tenant")
    void usuarioSemClinicaEhRejeitado() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO USUARIO_CLINICA (ID_CLINICA, DS_EMAIL, DS_SENHA_HASH, TP_PERFIL) "
                        + "VALUES (NULL, 'sem.tenant@kura.test', 'hash', 'GESTOR')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── O split -oracle/-h2 é mínimo, e isso fica travado ───────────────────

    @Test
    @DisplayName("split — as duas variantes da V17 diferem em EXATAMENTE uma linha: o DEFAULT da PK")
    void variantesDiferemApenasNaLinhaDoDefaultDaPk() {
        List<String> h2 = linhasSqlSignificativas(lerRecurso(ARQUIVO_H2));
        List<String> oracle = linhasSqlSignificativas(lerRecurso(ARQUIVO_ORACLE));

        assertThat(h2).hasSameSizeAs(oracle);

        List<Integer> divergentes = java.util.stream.IntStream.range(0, h2.size())
                .filter(i -> !h2.get(i).equals(oracle.get(i)))
                .boxed()
                .toList();

        assertThat(divergentes)
                .as("qualquer segunda divergência é dívida de split silenciosa — h2=%s / oracle=%s",
                        divergentes.stream().map(h2::get).toList(),
                        divergentes.stream().map(oracle::get).toList())
                .hasSize(1);

        assertThat(h2.get(divergentes.get(0)))
                .contains("ID_USUARIO_CLINICA")
                .contains("DEFAULT NEXT VALUE FOR SEQ_USUARIO_CLINICA");
        assertThat(oracle.get(divergentes.get(0)))
                .contains("ID_USUARIO_CLINICA")
                .contains("DEFAULT SEQ_USUARIO_CLINICA.NEXTVAL");
    }

    @Test
    @DisplayName("split — o bloco de conversão D-10 é byte a byte o mesmo nas duas variantes")
    void blocoDeConversaoEhIdenticoNasDuasVariantes() {
        assertThat(sqlDeConversaoDoArquivo(ARQUIVO_H2))
                .isEqualTo(sqlDeConversaoDoArquivo(ARQUIVO_ORACLE));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void plantarClinica(long id, String nome, String cnpj, String emailAcesso,
                                String senhaHash, String stAtiva) {
        jdbc.update("INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ, DS_EMAIL_ACESSO, "
                        + "DS_SENHA_HASH, ST_ATIVA) VALUES (?, ?, ?, ?, ?, ?)",
                id, nome, cnpj, emailAcesso, senhaHash, stAtiva);
    }

    private void inserirUsuario(long idClinica, String email, String perfil) {
        jdbc.update("INSERT INTO USUARIO_CLINICA (ID_CLINICA, DS_EMAIL, DS_SENHA_HASH, TP_PERFIL) "
                + "VALUES (?, ?, 'hash-de-teste', ?)", idClinica, email, perfil);
    }

    private int contarUsuarios(long idClinica) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USUARIO_CLINICA WHERE ID_CLINICA = ?", Integer.class, idClinica);
        return total == null ? 0 : total;
    }

    private Map<String, Object> usuarioDa(long idClinica) {
        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT * FROM USUARIO_CLINICA WHERE ID_CLINICA = ?", idClinica);
        assertThat(linhas).hasSize(1);
        return linhas.get(0);
    }

    /** Extrai o bloco de conversão do arquivo de migration entregue, sem transcrevê-lo. */
    private static String sqlDeConversaoDoArquivo(String recurso) {
        String conteudo = lerRecurso(recurso);
        int inicio = conteudo.indexOf(MARCADOR_INICIO);
        int fim = conteudo.indexOf(MARCADOR_FIM);
        assertThat(inicio).as("marcador de início não achado em %s", recurso).isNotNegative();
        assertThat(fim).as("marcador de fim não achado em %s", recurso).isGreaterThan(inicio);

        String bloco = conteudo.substring(inicio + MARCADOR_INICIO.length(), fim).trim();
        assertThat(bloco).as("bloco de conversão vazio em %s", recurso).contains("INSERT INTO USUARIO_CLINICA");
        return bloco.endsWith(";") ? bloco.substring(0, bloco.length() - 1) : bloco;
    }

    private static List<String> linhasSqlSignificativas(String conteudo) {
        return conteudo.lines()
                .map(String::trim)
                .filter(linha -> !linha.isEmpty() && !linha.startsWith("--"))
                .map(linha -> linha.replaceAll("\\s+", " "))
                .toList();
    }

    private static String lerRecurso(String caminho) {
        try (InputStream in = new ClassPathResource(caminho).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + caminho, e);
        }
    }
}
