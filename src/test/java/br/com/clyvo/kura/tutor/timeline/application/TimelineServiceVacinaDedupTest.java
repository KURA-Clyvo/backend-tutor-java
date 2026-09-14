package br.com.clyvo.kura.tutor.timeline.application;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaStatusResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.domain.repository.TimelinePetRepository;
import br.com.clyvo.kura.tutor.timeline.domain.repository.VacinaVencendoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LU-02 fix wave 1 — achados 1/7 da revisão G2 (lu-02-revisao.md).
 * <p>
 * V21 faz fan-out do ramo VACINA por TUTOR_PET: um pet com 2 tutores e 1
 * aplicação real de vacina gera 2 linhas em VW_VACINAS_VENCENDO (mesma
 * vacina/data, um ID_TUTOR por linha). A G2 mediu que o endpoint pet-scoped
 * do tutor (GET /api/v1/tutor/pets/{id}/vacinas e /vacinas/status) exibia a
 * MESMA vacina 2 vezes e sobrecontava — o teste original do implementador
 * (diff de código + GET contra o seed de dev) não tinha nenhuma linha no
 * ramo VACINA, então não podia ver isso (regra 13 — setup que aprova por
 * construção).
 * <p>
 * Este teste insere exatamente esse cenário (evento clínico + VACINA real,
 * pet com 2 tutores) contra H2 real com V21 aplicada e prova que a lista
 * tem 1 item, não 2, e que qtdPendentes conta a pendência real, não as
 * linhas da view.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TimelineServiceVacinaDedupTest {

    @Autowired
    TimelinePetRepository timelinePetRepository;

    @Autowired
    VacinaVencendoRepository vacinaVencendoRepository;

    @Autowired
    ContaTutorRepository contaTutorRepository;

    @Autowired
    PetRepository petRepository;

    @Test
    @DisplayName("listarVacinasPet/statusVacinasPet — pet com 2 tutores e 1 vacina real não duplica (G2 achado 1, BLOQUEANTE)")
    @Sql(statements = {
        // Seed de dev já tem TUTOR 1 / PET 1 / TUTOR_PET(1,1) / CLINICA 1 / VETERINARIO 1
        // / TIPO_EVENTO 3='VACINA' (afterMigrate__seeds_dev.sql). Este @Sql acrescenta um
        // segundo tutor para o MESMO pet e a vacina real que exercita o fan-out da V21.
        "INSERT INTO CONTA_TUTOR (ID_TUTOR, DS_EMAIL_LOGIN, DS_SENHA_HASH) " +
        "  VALUES (1, 'felipe@clyvo.vet', 'hash-de-teste')",
        "INSERT INTO TUTOR " +
        "  (ID_TUTOR, ID_CLINICA, NM_TUTOR, NR_CPF, DS_EMAIL, DS_TELEFONE, DS_WHATSAPP, " +
        "   DT_CADASTRO, ST_AVISO_PRIVACIDADE, DT_AVISO_PRIVACIDADE, DS_VERSAO_AVISO, ST_ATIVO) " +
        "  VALUES " +
        "  (90, 1, 'Segundo Tutor Dedup', '90000000090', 'segundo90dedup@test.invalid', " +
        "   '11900000090', '11900000090', CURRENT_TIMESTAMP, 'S', CURRENT_TIMESTAMP, 'v1.0', 'S')",
        "INSERT INTO TUTOR_PET (ID_TUTOR, ID_PET, DS_VINCULO, DT_VINCULO, ST_PRINCIPAL) " +
        "  VALUES (90, 1, 'RESPONSAVEL', CURRENT_TIMESTAMP, 'N')",
        "INSERT INTO EVENTO_CLINICO " +
        "  (ID_EVENTO, ID_PET, ID_CLINICA, ID_VETERINARIO, ID_TIPO_EVENTO, DS_OBSERVACAO, DT_EVENTO) " +
        "  VALUES (900, 1, 1, 1, 3, 'Aplicacao de vacina — LU-02 dedup test', CURRENT_TIMESTAMP)",
        "INSERT INTO VACINA " +
        "  (ID_VACINA, ID_EVENTO_CLINICO, NM_VACINA, NR_LOTE, DS_FABRICANTE, DT_PROXIMA_DOSE) " +
        "  VALUES (900, 900, 'V-Dedup-Test', 'L1', 'Fabricante Teste', CURRENT_TIMESTAMP + INTERVAL '5' DAY)"
    })
    void petComDoisTutoresEVacinaRealNaoDuplicaNaListaDoTutor() {
        // Pré-condição: a view REALMENTE faz fan-out (2 linhas) para este cenário —
        // se isto falhar, o teste não está exercitando o fan-out e o resto é inconclusivo.
        List<?> linhasDaView = vacinaVencendoRepository.findByIdPet(1L);
        assertThat(linhasDaView)
                .as("VW_VACINAS_VENCENDO deve ter 2 linhas (fan-out por tutor) para o pet com 2 tutores")
                .hasSize(2);

        TimelineService service = new TimelineService(
                timelinePetRepository, vacinaVencendoRepository, contaTutorRepository, petRepository);

        List<VacinaVencendoResponse> lista = service.listarVacinasPet(1L, "felipe@clyvo.vet");
        assertThat(lista)
                .as("lista do tutor não pode expor a mesma vacina 2 vezes")
                .hasSize(1);
        assertThat(lista.get(0).nmVacina()).isEqualTo("V-Dedup-Test");

        VacinaStatusResponse status = service.statusVacinasPet(1L, "felipe@clyvo.vet");
        assertThat(status.qtdPendentes())
                .as("qtdPendentes tem que contar a pendência real (1), não as linhas da view (2)")
                .isEqualTo(1L);
        assertThat(status.dsStatusGeral()).isEqualTo("ALERTA");
    }
}
