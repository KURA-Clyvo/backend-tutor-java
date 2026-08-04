package br.com.clyvo.kura.tutor.notificacao.domain.repository;

import br.com.clyvo.kura.tutor.notificacao.domain.Notificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-31 — valida leitura de NOTIFICACAO (.NET owned). Sem seed dev para esta
 * tabela; o @Sql insere as linhas necessárias para o teste. Nenhum teste desta
 * classe deve exercitar escrita — {@link Notificacao} é {@code @Immutable}.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificacaoRepositoryTest {

    @Autowired
    NotificacaoRepository notificacaoRepository;

    @Test
    @DisplayName("findByIdTutorOrderByDtCriacaoDesc — retorna só notificações do tutor, mais recente primeiro")
    @Sql(statements = {
        "INSERT INTO NOTIFICACAO (ID_NOTIFICACAO, ID_CLINICA, ID_TUTOR, DS_TITULO, DS_MENSAGEM, ST_LIDA, DT_CRIACAO) " +
        "VALUES (301, 1, 1, 'Comunicado antigo', 'Mensagem antiga', 'S', TIMESTAMP '2020-01-01 10:00:00')",
        "INSERT INTO NOTIFICACAO (ID_NOTIFICACAO, ID_CLINICA, ID_TUTOR, DS_TITULO, DS_MENSAGEM, ST_LIDA, DT_CRIACAO) " +
        "VALUES (302, 1, 1, 'Comunicado recente', 'Mensagem recente', 'N', TIMESTAMP '2026-08-01 10:00:00')",
        // ID_TUTOR NULL simula notificação de outro destinatário (ou só-clínica) — não deve
        // aparecer para o tutor 1. Evita depender de um segundo TUTOR inexistente no seed dev.
        "INSERT INTO NOTIFICACAO (ID_NOTIFICACAO, ID_CLINICA, ID_TUTOR, DS_TITULO, DS_MENSAGEM, ST_LIDA, DT_CRIACAO) " +
        "VALUES (303, 1, NULL, 'De outro destinatario', 'Nao deve aparecer', 'N', TIMESTAMP '2026-08-02 10:00:00')"
    })
    void findByIdTutorOrderByDtCriacaoDescDeveFiltrarEOrdenar() {
        Page<Notificacao> page = notificacaoRepository.findByIdTutorOrderByDtCriacaoDesc(1L, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getDsTitulo()).isEqualTo("Comunicado recente");
        assertThat(page.getContent().get(1).getDsTitulo()).isEqualTo("Comunicado antigo");
        assertThat(page.getContent().get(0).isLida()).isFalse();
        assertThat(page.getContent().get(1).isLida()).isTrue();
    }
}
