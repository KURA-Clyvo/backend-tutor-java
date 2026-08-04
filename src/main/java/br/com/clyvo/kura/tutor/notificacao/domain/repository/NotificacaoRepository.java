package br.com.clyvo.kura.tutor.notificacao.domain.repository;

import br.com.clyvo.kura.tutor.notificacao.domain.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repositório somente-leitura de NOTIFICACAO (.NET owned).
 * Deliberadamente sem métodos {@code save}/{@code delete}/{@code @Modifying} —
 * nunca acrescentar nenhum. A entidade {@link Notificacao} é {@code @Immutable}
 * e rejeitaria qualquer escrita em runtime de qualquer forma.
 */
public interface NotificacaoRepository
        extends Repository<Notificacao, Long>, PagingAndSortingRepository<Notificacao, Long> {

    Page<Notificacao> findByIdTutorOrderByDtCriacaoDesc(@Param("idTutor") Long idTutor, Pageable pageable);
}
