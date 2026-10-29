package br.com.clyvo.kura.tutor.shared.security;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolve o id do tutor autenticado a partir do e-mail do JWT. Unico lugar dessa regra nos
 * controllers do BFF: idTutor NUNCA vem de path, query ou corpo.
 */
@Component
public class TutorAutenticado {

    private final ContaTutorRepository contaTutorRepository;

    public TutorAutenticado(ContaTutorRepository contaTutorRepository) {
        this.contaTutorRepository = contaTutorRepository;
    }

    public long idTutor(Authentication auth) {
        return contaTutorRepository
                .findIdTutorByEmail(auth.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ContaTutor", auth.getName()));
    }
}
