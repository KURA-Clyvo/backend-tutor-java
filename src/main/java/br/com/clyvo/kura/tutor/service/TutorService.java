package br.com.clyvo.kura.tutor.service;

import br.com.clyvo.kura.tutor.dto.response.PetResponse;
import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;

    public TutorService(TutorRepository tutorRepository, PetRepository petRepository) {
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
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
    @Cacheable(value = "petsPorTutor", key = "#idTutor + '-' + #pageable.pageNumber")
    public Page<PetResponse> listarPets(Long idTutor, Pageable pageable) {
        tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));
        return petRepository.findAtivosByIdTutor(idTutor, pageable)
                .map(PetResponse::fromEntity);
    }
}
