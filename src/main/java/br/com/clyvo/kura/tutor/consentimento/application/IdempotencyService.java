package br.com.clyvo.kura.tutor.consentimento.application;

import br.com.clyvo.kura.tutor.consentimento.domain.IdempotencyKey;
import br.com.clyvo.kura.tutor.consentimento.domain.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    /** Retorna a chave somente se ela existe e ainda não expirou. */
    public Optional<IdempotencyKey> buscar(String key, String resource) {
        return idempotencyKeyRepository.findByDsKeyAndNmResource(key, resource)
                .filter(IdempotencyKey::isValido);
    }

    public IdempotencyKey registrar(String key, String resource, Long idResource) {
        return idempotencyKeyRepository.save(IdempotencyKey.criar(key, resource, idResource));
    }
}
