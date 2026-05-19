package br.com.clyvo.kura.tutor.onboarding.domain.repository;

import br.com.clyvo.kura.tutor.onboarding.domain.InviteTutor;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Acesso de leitura exclusivo ao INVITE_TUTOR — owned pelo .NET.
 *
 * Estende apenas Repository<T, ID> (não JpaRepository) para garantir
 * que o Java nunca salve ou delete convites: apenas lê.
 */
public interface InviteTutorRepository extends Repository<InviteTutor, Long> {
    Optional<InviteTutor> findByNrToken(String nrToken);
}
