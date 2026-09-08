package br.com.clyvo.kura.tutor.agendamento.domain.repository;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SJ3-10 — prova de mordida do {@code @EntityGraph} em
 * {@link AgendamentoRepository#findAll(Specification, org.springframework.data.domain.Pageable)}.
 *
 * <p><b>Por que insere uma 2ª linha de CLINICA/PET em vez de usar só o seed.</b> O seed dev
 * ({@code afterMigrate__seeds_dev.sql}) tem só 1 Pet / 1 Clínica / 1 Agendamento. Com uma página
 * de 1 linha só o N+1 não aparece de forma discriminante — pior ainda, mesmo com 2 agendamentos
 * apontando para o MESMO pet/clínica, o cache de 1º nível do Hibernate dedupa a associação
 * (a 2ª navegação não bate no banco, mascarando o problema). Por isso a 2ª linha usa
 * espécie/raça/clínica DIFERENTES da linha 1 — cada associação navegada é uma entidade
 * distinta, então cada uma conta como um SELECT extra de verdade sem o {@code @EntityGraph}.
 *
 * <p>Contagem via {@link Statistics#getPrepareStatementCount()}: mede APENAS os statements
 * disparados entre o fetch da página (findAll) e a navegação das associações — a query de
 * paginação (SELECT + COUNT, que o Spring Data sempre emite, com ou sem EntityGraph) fica FORA
 * dessa janela de medição.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendamentoEntityGraphQueryCountTest {

    @Autowired
    AgendamentoRepository repo;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("findAll(Specification, Pageable) — zero SELECT extra ao navegar pet/especie/raca/clinica (com @EntityGraph)")
    void findAll_comEntityGraph_zeroQueriesExtrasAoNavegarAssociacoes() {
        inserirSegundaLinhaComAssociacoesDistintas();

        em.flush();
        em.clear(); // descarta o cache de 1º nível — força reload de tudo a partir do zero

        SessionFactory sf = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Page<Agendamento> page = repo.findAll(Specification.where(null), PageRequest.of(0, 10));
        long queriesAposFetch = stats.getPrepareStatementCount();

        assertThat(page.getContent().size())
                .as("página precisa ter >=2 linhas — com 1 linha só o N+1 não é discriminante (regra 13 do projeto)")
                .isGreaterThanOrEqualTo(2);

        // Navega TODAS as associações que AgendamentoResponse.fromEntity lê para os 3 campos novos
        // (mais o pet.getNmPet() que já era lido hoje, antes desta task).
        page.getContent().forEach(a -> {
            if (a.getPet() != null) {
                a.getPet().getNmPet();
                if (a.getPet().getEspecie() != null) {
                    a.getPet().getEspecie().getNmEspecie();
                }
                if (a.getPet().getRaca() != null) {
                    a.getPet().getRaca().getNmRaca();
                }
            }
            if (a.getClinica() != null) {
                a.getClinica().getNmClinica();
            }
        });

        long queriesAposNavegacao = stats.getPrepareStatementCount();
        long queriesExtras = queriesAposNavegacao - queriesAposFetch;

        assertThat(queriesExtras)
                .as("com @EntityGraph, navegar pet/especie/raca/clinica não deve disparar SELECT novo")
                .isZero();
    }

    /**
     * 2ª linha de CLINICA/PET/AGENDAMENTO com espécie(Gato)/raça(Siames)/clínica DIFERENTES da
     * linha 1 do seed (Cão/Labrador/Clyvo Vet São Paulo) — ver o porquê no javadoc da classe.
     * Inserção via SQL nativo, igual ao padrão do próprio {@code afterMigrate__seeds_dev.sql}
     * (essas 3 tabelas são {@code @Immutable} do lado Java — não têm setter para popular via
     * entidade JPA). Escopada à transação do teste, revertida automaticamente pelo
     * {@code @DataJpaTest}.
     */
    private void inserirSegundaLinhaComAssociacoesDistintas() {
        em.createNativeQuery("""
            INSERT INTO CLINICA (ID_CLINICA, NM_CLINICA, NR_CNPJ, DT_CADASTRO, ST_ATIVA)
            VALUES (2, 'Clyvo Vet Campinas', '99999999000199', CURRENT_TIMESTAMP, 'S')
        """).executeUpdate();

        // ID_ESPECIE=2 (Gato) / ID_RACA=3 (Siames) já existem no seed (afterMigrate__seeds_dev.sql)
        // — distintos de ID_ESPECIE=1 (Cão) / ID_RACA=1 (Labrador) do Pet 1.
        em.createNativeQuery("""
            INSERT INTO PET (ID_PET, ID_CLINICA, ID_ESPECIE, ID_RACA, ID_VETERINARIO_RESP, NM_PET, ST_ATIVO)
            VALUES (2, 2, 2, 3, 1, 'Bidu', 'S')
        """).executeUpdate();

        em.createNativeQuery("""
            INSERT INTO AGENDAMENTO (ID_AGENDAMENTO, ID_CLINICA, ID_TUTOR, ID_PET, ID_VETERINARIO,
                DT_AGENDAMENTO, NR_DURACAO_MINUTOS, DS_TIPO, ST_STATUS, DS_ORIGEM, NR_VERSION)
            VALUES (2, 2, 1, 2, 1, CURRENT_TIMESTAMP + INTERVAL '8' DAY, 30, 'CONSULTA', 'AGENDADO', 'PORTAL', 0)
        """).executeUpdate();
    }
}
