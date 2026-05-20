package br.com.clyvo.kura.tutor.consentimento.domain.repository;

import br.com.clyvo.kura.tutor.consentimento.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByDsKeyAndNmResource(String dsKey, String nmResource);
}
