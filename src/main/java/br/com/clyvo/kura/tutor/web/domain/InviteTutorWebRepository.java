// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.domain;

import br.com.clyvo.kura.tutor.onboarding.domain.InviteTutor;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Acesso de leitura LOCAL DA BRANCH ao {@code INVITE_TUTOR}, para o painel de
 * suporte (SJ3-07) achar o convite de uma pessoa —
 * {@code br.com.clyvo.kura.tutor.onboarding.domain.repository.InviteTutorRepository}
 * (de produto) só expõe {@code findByNrToken}, que não serve para isso.
 *
 * Mesma entidade de produto ({@code InviteTutor}, {@code @Immutable}),
 * repositório NOVO: estende apenas {@code Repository<T, ID>} (não
 * {@code JpaRepository}), pela mesma razão do repositório de produto —
 * {@code INVITE_TUTOR} é owned pelo .NET, o Java só lê. Não há coluna
 * {@code UNIQUE(ID_TUTOR)} em {@code INVITE_TUTOR} (uma pessoa pode ter mais
 * de um convite ao longo do tempo), por isso o retorno é {@code List}, não
 * {@code Optional} — o controller filtra o convite pendente (ativo e não
 * utilizado) entre os resultados.
 */
public interface InviteTutorWebRepository extends Repository<InviteTutor, Long> {
    List<InviteTutor> findByIdTutor(Long idTutor);
}
