package br.com.clyvo.kura.tutor.shared.pet;

import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Regra única "este pet é do tutor autenticado", compartilhada pelos módulos que leem o Mongo
 * (documentos, laudos). Roda no Oracle e, por contrato, ANTES de qualquer acesso ao Mongo.
 */
@Component
public class GuardaPetDoTutor {

    private final PetRepository petRepository;

    public GuardaPetDoTutor(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    /** 403 sem vínculo; 404 pet inativo. */
    public void garantir(long idTutor, long idPet) {
        if (petRepository.countVinculo(idPet, idTutor) == 0) {
            throw new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado.");
        }
        petRepository.findByIdPetAndStAtivo(idPet, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", idPet));
    }
}
