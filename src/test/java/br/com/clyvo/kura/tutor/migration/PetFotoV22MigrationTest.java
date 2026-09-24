package br.com.clyvo.kura.tutor.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FT-01 (KURA_BACKLOG_FOTO_PET) — prova da migration V22 (PET.DS_FOTO_CHAVE /
 * PET.DT_FOTO_ATUALIZACAO) contra o H2 real do profile dev, mesmo banco em que o Flyway já
 * aplicou V1..V22 (variante -h2 + os arquivos comuns de {@code db/migration}) antes de
 * qualquer teste rodar.
 *
 * <p>Sem o arquivo {@code V22__pet_foto.sql}, os 5 testes desta classe falham: os 2 de
 * {@code INFORMATION_SCHEMA} por assertion (coluna não encontrada), os 2 de escrita por
 * {@link org.springframework.jdbc.BadSqlGrammarException} ("column not found") e o de pet sem
 * foto por {@code containsKey} — um {@code Map.get} de coluna ausente devolveria {@code null}
 * e passaria sem a migration (achado F1-a da G2).
 *
 * <p>O que esta classe PROVA: as 2 colunas existem, são nullable, e aceitam gravação/leitura
 * de valor real (chave no formato do G0, timestamp). O que ela NÃO prova: nada sobre Oracle
 * real — este arquivo é único (sem split -oracle/-h2, ver cabeçalho da migration), então G4
 * (compose real) é quem prova o lado Oracle.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetFotoV22MigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    // ─── Existência + nullability via INFORMATION_SCHEMA (prova estrutural) ──

    @Test
    @DisplayName("DS_FOTO_CHAVE — existe em PET, é VARCHAR com tamanho 500 e é nullable")
    void dsFotoChaveExisteComTamanhoETipoCorretos() {
        Map<String, Object> coluna = colunaDaTabelaPet("DS_FOTO_CHAVE");

        assertThat(coluna)
                .as("coluna DS_FOTO_CHAVE não encontrada em PET — migration V22 não aplicada?")
                .isNotEmpty();
        assertThat(String.valueOf(coluna.get("IS_NULLABLE")))
                .as("DS_FOTO_CHAVE precisa ser nullable — pet sem foto é o caso majoritário")
                .isEqualToIgnoringCase("YES");
        assertThat(((Number) coluna.get("CHARACTER_MAXIMUM_LENGTH")).intValue())
                .as("tamanho tem que bater com VARCHAR2(500), mesmo padrão de DOCUMENTO.DS_CAMINHO")
                .isEqualTo(500);
    }

    @Test
    @DisplayName("DT_FOTO_ATUALIZACAO — existe em PET, é TIMESTAMP e é nullable")
    void dtFotoAtualizacaoExisteEhNullable() {
        Map<String, Object> coluna = colunaDaTabelaPet("DT_FOTO_ATUALIZACAO");

        assertThat(coluna)
                .as("coluna DT_FOTO_ATUALIZACAO não encontrada em PET — migration V22 não aplicada?")
                .isNotEmpty();
        assertThat(String.valueOf(coluna.get("IS_NULLABLE")))
                .as("DT_FOTO_ATUALIZACAO precisa ser nullable — nenhum upload ainda aconteceu")
                .isEqualToIgnoringCase("YES");
        assertThat(String.valueOf(coluna.get("DATA_TYPE")))
                .as("tipo tem que ser TIMESTAMP")
                .containsIgnoringCase("TIMESTAMP");
    }

    // ─── Comportamento: gravar, ler, e o caso "sem foto" continua funcionando ─

    @Test
    @DisplayName("pet sem foto — INSERT sem as 2 colunas continua funcionando, valores vêm NULL")
    void petSemFotoContinuaFuncionando() {
        long idClinica = 950101;
        long idEspecie = plantarEspecie("Espécie-teste-FT01-A");
        plantarClinica(idClinica, "Clinica FT-01 sem foto", "95010100000191");

        long idPet = plantarPetSemFoto(idClinica, idEspecie, "Rex sem foto");

        Map<String, Object> pet = petPorId(idPet);
        // containsKey primeiro: sem a V22, get() de coluna ausente também devolve null.
        assertThat(pet).containsKey("DS_FOTO_CHAVE").containsKey("DT_FOTO_ATUALIZACAO");
        assertThat(pet.get("DS_FOTO_CHAVE")).isNull();
        assertThat(pet.get("DT_FOTO_ATUALIZACAO")).isNull();
    }

    @Test
    @DisplayName("foto pode ser gravada e lida de volta — chave no formato do G0 + timestamp")
    void fotoEhGravadaELidaDeVolta() {
        long idClinica = 950102;
        long idEspecie = plantarEspecie("Espécie-teste-FT01-B");
        plantarClinica(idClinica, "Clinica FT-01 com foto", "95010200000192");
        long idPet = plantarPetSemFoto(idClinica, idEspecie, "Rex com foto");

        String chave = "clinica/" + idClinica + "/pet/" + idPet + "/550e8400-e29b-41d4-a716-446655440000.webp";
        LocalDateTime agora = LocalDateTime.now().withNano(0);

        int linhasAfetadas = jdbc.update(
                "UPDATE PET SET DS_FOTO_CHAVE = ?, DT_FOTO_ATUALIZACAO = ? WHERE ID_PET = ?",
                chave, Timestamp.valueOf(agora), idPet);
        assertThat(linhasAfetadas).isEqualTo(1);

        Map<String, Object> pet = petPorId(idPet);
        assertThat(pet.get("DS_FOTO_CHAVE")).isEqualTo(chave);
        assertThat(((Timestamp) pet.get("DT_FOTO_ATUALIZACAO")).toLocalDateTime()).isEqualTo(agora);
    }

    @Test
    @DisplayName("DS_FOTO_CHAVE aceita chave até o limite de 500 caracteres")
    void dsFotoChaveAceitaAteOLimite() {
        long idClinica = 950103;
        long idEspecie = plantarEspecie("Espécie-teste-FT01-C");
        plantarClinica(idClinica, "Clinica FT-01 limite", "95010300000193");
        long idPet = plantarPetSemFoto(idClinica, idEspecie, "Rex limite");

        String chave = "clinica/" + idClinica + "/pet/" + idPet + "/" + "a".repeat(500 - ("clinica/" + idClinica + "/pet/" + idPet + "/").length());
        assertThat(chave).hasSize(500);

        jdbc.update("UPDATE PET SET DS_FOTO_CHAVE = ? WHERE ID_PET = ?", chave, idPet);

        Map<String, Object> pet = petPorId(idPet);
        assertThat(pet.get("DS_FOTO_CHAVE")).isEqualTo(chave);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> colunaDaTabelaPet(String nomeColuna) {
        return jdbc.queryForList(
                        "SELECT IS_NULLABLE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH "
                                + "FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_NAME = 'PET' AND COLUMN_NAME = ?",
                        nomeColuna)
                .stream()
                .findFirst()
                .orElse(Map.of());
    }

    private void plantarClinica(long id, String nome, String cnpj) {
        jdbc.update("INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ) VALUES (?, ?, ?)",
                id, nome, cnpj);
    }

    private long plantarEspecie(String nome) {
        jdbc.update("INSERT INTO ESPECIE (NM_ESPECIE) VALUES (?)", nome);
        Long id = jdbc.queryForObject("SELECT ID_ESPECIE FROM ESPECIE WHERE NM_ESPECIE = ?", Long.class, nome);
        assertThat(id).isNotNull();
        return id;
    }

    private long plantarPetSemFoto(long idClinica, long idEspecie, String nome) {
        jdbc.update("INSERT INTO PET (ID_CLINICA, ID_ESPECIE, NM_PET) VALUES (?, ?, ?)",
                idClinica, idEspecie, nome);
        Long id = jdbc.queryForObject(
                "SELECT ID_PET FROM PET WHERE ID_CLINICA = ? AND NM_PET = ?", Long.class, idClinica, nome);
        assertThat(id).isNotNull();
        return id;
    }

    private Map<String, Object> petPorId(long idPet) {
        return jdbc.queryForList("SELECT * FROM PET WHERE ID_PET = ?", idPet)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PET " + idPet + " não encontrado"));
    }
}
