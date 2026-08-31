package br.com.clyvo.kura.tutor.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FD-14 — prova da migration V19 (UK_VET_CLINICA_ID + FK_USUARIO_CLINICA_VET composta)
 * contra o H2 real do profile dev, que é o mesmo banco em que o Flyway já aplicou
 * V1..V19 antes de qualquer teste desta suíte rodar.
 *
 * <p>O que esta classe prova e o que ela NÃO prova:
 * <ul>
 *   <li><b>Prova</b> que o DDL da V19 aplica no H2 (se não aplicasse, o Flyway derrubaria
 *       o startup do contexto e nenhum teste desta suíte rodaria), que o vínculo
 *       cross-tenant passa a ser recusado <b>pelo banco</b>, que o vínculo legítimo
 *       continua aceito (controle positivo — sem ele, "passou a recusar" é
 *       indistinguível de "quebrou tudo") e que o GESTOR sem veterinário
 *       (ID_VETERINARIO nulo) continua aceito, que é a regressão mais provável desta
 *       migration e a que nenhuma das outras asserções pegaria.</li>
 *   <li><b>NÃO prova</b> nada sobre Oracle: nenhum teste desta suíte toca Oracle (a conta
 *       da FIAP está bloqueada, ORA-28000). A prova em Oracle real é a execução
 *       {@code down -v && up -d} do compose, registrada em
 *       {@code .superpowers/sdd/KURA_BACKLOG_FIN/fd-14-report.md} — inclusive a recusa
 *       com {@code ORA-02291} e o seu controle positivo.</li>
 *   <li><b>NÃO prova</b> que o código C# do .NET continua checando o mesmo invariante. A
 *       V19 é defesa em profundidade <i>somada</i> à checagem em C#, não substituta.</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioClinicaV19FkCompostaTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("V19 — vínculo cross-tenant em USUARIO_CLINICA é RECUSADO pelo banco")
    void vinculoCrossTenantEhRecusadoPeloBanco() {
        long clinicaA = plantarClinica(9141, "Clinica FD14 A", "91410000000191");
        long clinicaB = plantarClinica(9142, "Clinica FD14 B", "91420000000191");
        long vetDaB = plantarVeterinario(9142, clinicaB, "Vet FD14 da clinica B");

        // ── CONTROLE POSITIVO ────────────────────────────────────────────────
        // "o INSERT falhou" só é interpretável depois de provar que este mesmo
        // instrumento ACEITA o caso legítimo. Sem isto, uma FK que recusasse TUDO
        // (ou um erro de digitação no INSERT) seria lido como sucesso.
        long vetDaA = plantarVeterinario(9141, clinicaA, "Vet FD14 da clinica A");
        assertThatCode(() -> inserirUsuario(clinicaA, vetDaA, "legitimo@fd14.test", "VETERINARIO"))
                .as("controle positivo: vinculo com veterinario DA PROPRIA clinica continua aceito")
                .doesNotThrowAnyException();
        assertThat(contarUsuario(clinicaA, "legitimo@fd14.test")).isEqualTo(1);

        // ── A MORDIDA ────────────────────────────────────────────────────────
        // Antes da V19 este INSERT era ACEITO (medido contra Oracle real: usuario da
        // clinica 100 apontando o veterinario 101, que pertence a clinica 101).
        assertThatThrownBy(() -> inserirUsuario(clinicaA, vetDaB, "cross@fd14.test", "VETERINARIO"))
                .as("usuario da clinica A apontando veterinario da clinica B tem que morrer no "
                        + "BANCO. Se esta assercao cair, o unico obstaculo volta a ser uma linha "
                        + "de C# que o backend Java nao executa.")
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(contarUsuario(clinicaA, "cross@fd14.test"))
                .as("a linha cross-tenant nao pode ter sido gravada")
                .isZero();
    }

    @Test
    @DisplayName("V19 — GESTOR sem veterinário (ID_VETERINARIO nulo) continua aceito")
    void gestorSemVeterinarioContinuaAceito() {
        long clinica = plantarClinica(9143, "Clinica FD14 C", "91430000000191");

        // Semântica de nulo parcial em FK composta: se qualquer coluna da FK for nula, a
        // constraint não é verificada. É isto que preserva a ruling da V17 ("um GESTOR pode
        // não ser veterinário"). Se o banco passasse a exigir MATCH FULL, todo GESTOR
        // deixaria de ser inserível — e nenhuma outra asserção desta classe veria isso.
        assertThatCode(() -> inserirUsuario(clinica, null, "gestor@fd14.test", "GESTOR"))
                .doesNotThrowAnyException();

        assertThat(contarUsuario(clinica, "gestor@fd14.test")).isEqualTo(1);
    }

    @Test
    @DisplayName("V19 — UK_VET_CLINICA_ID existe em VETERINARIO (alvo da FK composta)")
    void chaveUnicaAlvoDaFkExiste() {
        // Controle positivo do próprio instrumento: a mesma consulta enxerga uma
        // constraint que sabidamente existe desde a V1.
        assertThat(existeConstraint("UK_VET_CRMV"))
                .as("controle positivo: a consulta a INFORMATION_SCHEMA enxerga constraint existente")
                .isTrue();

        assertThat(existeConstraint("UK_VET_CLINICA_ID"))
                .as("sem esta chave unica o Oracle recusa criar a FK composta — remover a UK "
                        + "quebra a V19 inteira")
                .isTrue();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private long plantarClinica(long id, String nome, String cnpj) {
        jdbc.update("INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ, ST_ATIVA) VALUES (?, ?, ?, 'S')",
                id, nome, cnpj);
        return id;
    }

    private long plantarVeterinario(long id, long idClinica, String nome) {
        jdbc.update("INSERT INTO VETERINARIO (ID_VETERINARIO, ID_CLINICA, NM_VETERINARIO) VALUES (?, ?, ?)",
                id, idClinica, nome);
        return id;
    }

    private void inserirUsuario(long idClinica, Long idVeterinario, String email, String perfil) {
        jdbc.update("INSERT INTO USUARIO_CLINICA (ID_CLINICA, ID_VETERINARIO, DS_EMAIL, "
                        + "DS_SENHA_HASH, TP_PERFIL) VALUES (?, ?, ?, ?, ?)",
                idClinica, idVeterinario, email, "$2a$10$hashFalsoDeTesteFD14000000000000000000000", perfil);
    }

    private int contarUsuario(long idClinica, String email) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USUARIO_CLINICA WHERE ID_CLINICA = ? AND DS_EMAIL = ?",
                Integer.class, idClinica, email);
        return total == null ? 0 : total;
    }

    private boolean existeConstraint(String nome) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                Integer.class, nome);
        return total != null && total > 0;
    }
}
