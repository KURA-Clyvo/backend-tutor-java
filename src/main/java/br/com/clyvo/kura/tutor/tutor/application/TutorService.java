package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final ContaTutorRepository contaTutorRepository;

    public TutorService(TutorRepository tutorRepository,
                        PetRepository petRepository,
                        ContaTutorRepository contaTutorRepository) {
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
        this.contaTutorRepository = contaTutorRepository;
    }

    @Transactional(readOnly = true)
    public TutorResponse buscarPorId(Long idTutor) {
        return tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .map(TutorResponse::fromEntity)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));
    }

    @Transactional(readOnly = true)
    public Page<TutorResponse> buscarComFiltros(String nome, String cidade,
                                                String uf, Pageable pageable) {
        return tutorRepository.buscarComFiltros(nome, cidade, uf, pageable)
                .map(TutorResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PetResponse> listarPets(Long idTutor, String emailAutenticado, Pageable pageable) {
        Long idTutorAutenticado = contaTutorRepository.findIdTutorByEmail(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta", emailAutenticado));

        if (!idTutorAutenticado.equals(idTutor)) {
            throw new ForbiddenException("Acesso negado: você só pode visualizar seus próprios pets.");
        }

        tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        Pageable efetivo = pageable.getPageSize() > MAX_PAGE_SIZE
                ? PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                : pageable;

        return petRepository.findAtivosByIdTutor(idTutor, efetivo)
                .map(PetResponse::fromEntity);
    }
}
