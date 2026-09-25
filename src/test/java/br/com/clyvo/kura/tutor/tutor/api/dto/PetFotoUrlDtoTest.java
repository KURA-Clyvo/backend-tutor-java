package br.com.clyvo.kura.tutor.tutor.api.dto;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.foto.ChaveFotoPet;
import br.com.clyvo.kura.tutor.shared.foto.GeradorUrlFotoPet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FT-05 (KURA_BACKLOG_FOTO_PET) — aceite: "DTO com as 2 URLs para pet com foto e null sem
 * foto (teste com o H2 do dev, gravando DS_FOTO_CHAVE por SQL como o PetFotoV22MigrationTest
 * já faz)".
 *
 * <p>Mesmo padrão de {@code br.com.clyvo.kura.tutor.migration.PetFotoV22MigrationTest} (FT-01):
 * {@code @DataJpaTest} contra o H2 real do profile {@code dev} (Flyway já aplicou V1..V22), sem
 * mockar nada da camada de persistência — só {@link GeradorUrlFotoPet} é instanciado à mão
 * (não é um bean JPA; {@code @DataJpaTest} não carrega {@code @Component} genéricos), com
 * segredo/base/relógio fixos para o teste ser determinístico.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetFotoUrlDtoTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PetRepository petRepository;

    private static final String SEGREDO = "kura-teste-segredo-com-32-bytes-ok!!";
    private static final String BASE = "https://kura-clinica.example";

    private static GeradorUrlFotoPet geradorHabilitado() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_800_000_000L), ZoneOffset.UTC);
        return new GeradorUrlFotoPet(SEGREDO, BASE, 24, clock);
    }

    @Test
    @DisplayName("pet COM foto — PetResponse (lista) ganha dsFotoThumbUrl (256), nunca dsFotoUrl")
    void petComFoto_petResponse_ganhaSoAThumb() {
        long idClinica = 960101;
        long idEspecie = plantarEspecie("Espécie-FT05-A");
        plantarClinica(idClinica, "Clinica FT-05 A", "96010100000191");
        long idPet = plantarPet(idClinica, idEspecie, "Rex com foto");
        String chave = "clinica/" + idClinica + "/pet/" + idPet + "/abc.webp";
        jdbc.update("UPDATE PET SET DS_FOTO_CHAVE = ? WHERE ID_PET = ?", chave, idPet);

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S").orElseThrow();
        PetResponse resposta = PetResponse.fromEntity(pet, geradorHabilitado());

        assertThat(resposta.dsFotoThumbUrl())
                .isNotNull()
                .contains("/api/v1/fotos/" + chave.replace(".webp", "_256.webp"))
                .contains("exp=")
                .contains("sig=");
    }

    @Test
    @DisplayName("pet COM foto — PetDetalheResponse ganha dsFotoUrl (1080) E dsFotoThumbUrl (256)")
    void petComFoto_petDetalheResponse_ganhaAsDuasVariantes() {
        long idClinica = 960102;
        long idEspecie = plantarEspecie("Espécie-FT05-B");
        plantarClinica(idClinica, "Clinica FT-05 B", "96010200000192");
        long idPet = plantarPet(idClinica, idEspecie, "Rex detalhe com foto");
        String chave = "clinica/" + idClinica + "/pet/" + idPet + "/xyz.jpg";
        jdbc.update("UPDATE PET SET DS_FOTO_CHAVE = ? WHERE ID_PET = ?", chave, idPet);

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S").orElseThrow();
        PetDetalheResponse detalhe = PetDetalheResponse.fromEntity(pet, 0L, geradorHabilitado());

        assertThat(detalhe.dsFotoUrl())
                .isNotNull()
                .contains("/api/v1/fotos/" + chave.replace(".jpg", "_1080.jpg"));
        assertThat(detalhe.dsFotoThumbUrl())
                .isNotNull()
                .contains("/api/v1/fotos/" + chave.replace(".jpg", "_256.jpg"));
        // As 2 variantes da MESMA chave nunca podem ser a mesma URL (sufixo/sig diferentes).
        assertThat(detalhe.dsFotoUrl()).isNotEqualTo(detalhe.dsFotoThumbUrl());
    }

    @Test
    @DisplayName("pet SEM foto — PetResponse.dsFotoThumbUrl é null (nunca lança)")
    void petSemFoto_petResponse_urlNull() {
        long idClinica = 960103;
        long idEspecie = plantarEspecie("Espécie-FT05-C");
        plantarClinica(idClinica, "Clinica FT-05 C", "96010300000193");
        long idPet = plantarPet(idClinica, idEspecie, "Rex sem foto");
        // DS_FOTO_CHAVE nunca foi setada — permanece NULL (comportamento padrão da V22/FT-01).

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S").orElseThrow();
        PetResponse resposta = PetResponse.fromEntity(pet, geradorHabilitado());

        assertThat(resposta.dsFotoThumbUrl()).isNull();
    }

    @Test
    @DisplayName("pet SEM foto — PetDetalheResponse: dsFotoUrl e dsFotoThumbUrl são null")
    void petSemFoto_petDetalheResponse_urlsNull() {
        long idClinica = 960104;
        long idEspecie = plantarEspecie("Espécie-FT05-D");
        plantarClinica(idClinica, "Clinica FT-05 D", "96010400000194");
        long idPet = plantarPet(idClinica, idEspecie, "Rex detalhe sem foto");

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S").orElseThrow();
        PetDetalheResponse detalhe = PetDetalheResponse.fromEntity(pet, 0L, geradorHabilitado());

        assertThat(detalhe.dsFotoUrl()).isNull();
        assertThat(detalhe.dsFotoThumbUrl()).isNull();
    }

    @Test
    @DisplayName("config de assinatura desabilitada (GeradorUrlFotoPet 'vazio') — URLs null mesmo com pet COM foto")
    void configDesabilitada_urlsNullMesmoComFoto() {
        long idClinica = 960105;
        long idEspecie = plantarEspecie("Espécie-FT05-E");
        plantarClinica(idClinica, "Clinica FT-05 E", "96010500000195");
        long idPet = plantarPet(idClinica, idEspecie, "Rex config desabilitada");
        String chave = "clinica/" + idClinica + "/pet/" + idPet + "/abc.webp";
        jdbc.update("UPDATE PET SET DS_FOTO_CHAVE = ? WHERE ID_PET = ?", chave, idPet);

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S").orElseThrow();
        GeradorUrlFotoPet geradorDesabilitado =
                new GeradorUrlFotoPet("", "", 24, Clock.fixed(Instant.ofEpochSecond(1_800_000_000L), ZoneOffset.UTC));

        PetResponse resposta = PetResponse.fromEntity(pet, geradorDesabilitado);
        PetDetalheResponse detalhe = PetDetalheResponse.fromEntity(pet, 0L, geradorDesabilitado);

        assertThat(resposta.dsFotoThumbUrl()).isNull();
        assertThat(detalhe.dsFotoUrl()).isNull();
        assertThat(detalhe.dsFotoThumbUrl()).isNull();
    }

    @Test
    @DisplayName("ChaveFotoPet.SUFIXO_THUMB/SUFIXO_MEDIA batem com os literais do backlog ('256'/'1080')")
    void sufixosBatemComOBacklog() {
        assertThat(ChaveFotoPet.SUFIXO_THUMB).isEqualTo("256");
        assertThat(ChaveFotoPet.SUFIXO_MEDIA).isEqualTo("1080");
    }

    // ─── helpers (mesmo padrão de PetFotoV22MigrationTest) ─────────────────────

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

    private long plantarPet(long idClinica, long idEspecie, String nome) {
        jdbc.update("INSERT INTO PET (ID_CLINICA, ID_ESPECIE, NM_PET) VALUES (?, ?, ?)",
                idClinica, idEspecie, nome);
        Long id = jdbc.queryForObject(
                "SELECT ID_PET FROM PET WHERE ID_CLINICA = ? AND NM_PET = ?", Long.class, idClinica, nome);
        assertThat(id).isNotNull();
        return id;
    }
}
