package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.timeline.domain.repository.TimelinePetRepository;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetDetalheResponse;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import br.com.clyvo.kura.tutor.tutor.dto.PushTokenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final ContaTutorRepository contaTutorRepository;
    private final TimelinePetRepository timelinePetRepository;

    public TutorService(TutorRepository tutorRepository,
                        PetRepository petRepository,
                        ContaTutorRepository contaTutorRepository,
                        TimelinePetRepository timelinePetRepository) {
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
        this.contaTutorRepository = contaTutorRepository;
        this.timelinePetRepository = timelinePetRepository;
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

    /**
     * Detalhe de um pet do tutor autenticado (TASK-31 — antes stub 501).
     * idTutor sempre do JWT (via emailAutenticado); {@code idPet} vem do path
     * (identifica o recurso, não o tutor) e sua posse é verificada via
     * {@code PetRepository.countVinculo} antes de retornar qualquer dado.
     */
    @Transactional(readOnly = true)
    public PetDetalheResponse buscarPetDetalhe(Long idPet, String emailAutenticado) {
        Long idTutor = contaTutorRepository.findIdTutorByEmail(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta", emailAutenticado));

        if (petRepository.countVinculo(idPet, idTutor) == 0) {
            throw new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado.");
        }

        Pet pet = petRepository.findByIdPetAndStAtivo(idPet, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", idPet));

        long nrConsultas = timelinePetRepository.countByIdPet(idPet);
        return PetDetalheResponse.fromEntity(pet, nrConsultas);
    }

    @Transactional
    public void atualizarPushToken(Long idTutor, PushTokenRequest req) {
        // LGPD: nunca logar o valor do token — apenas metadados
        log.debug("Atualizando push token do tutor idTutor={} plataforma={}", idTutor, req.dsPlatforma());

        int atualizados = contaTutorRepository.atualizarPushToken(idTutor, req.dsPushToken(), req.dsPlatforma());
        if (atualizados == 0) {
            throw new RecursoNaoEncontradoException("ContaTutor", idTutor);
        }
    }
}
