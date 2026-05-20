package br.com.clyvo.kura.tutor.service;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.repository.AgendamentoRepository;
import br.com.clyvo.kura.tutor.dto.request.AgendamentoRequest;
import br.com.clyvo.kura.tutor.dto.response.AgendamentoResponse;
import br.com.clyvo.kura.tutor.entity.Clinica;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final EntityManager entityManager;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                               TutorRepository tutorRepository,
                               PetRepository petRepository,
                               EntityManager entityManager) {
        this.agendamentoRepository = agendamentoRepository;
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoResponse> listar(Long idTutor, String status, Pageable pageable) {
        StatusAgendamento statusEnum;
        try {
            statusEnum = StatusAgendamento.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            statusEnum = StatusAgendamento.AGENDADO;
        }
        return agendamentoRepository
                .findByTutor_IdTutorAndStStatus(idTutor, statusEnum, pageable)
                .map(AgendamentoResponse::fromEntity);
    }

    @Transactional
    public AgendamentoResponse criar(Long idTutor, AgendamentoRequest request) {
        Tutor tutor = tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        Pet pet = petRepository.findByIdPetAndStAtivo(request.idPet(), "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", request.idPet()));

        boolean petDoTutor = pet.getTutorPets().stream()
                .anyMatch(tp -> tp.getTutor().getIdTutor().equals(idTutor));
        if (!petDoTutor) {
            throw new RegraDeNegocioException("Pet nao vinculado a este tutor.");
        }

        Clinica clinica = entityManager.getReference(Clinica.class, request.idClinica());

        Agendamento ag = Agendamento.criar(tutor, pet, clinica, request.idVeterinario(),
                request.dtAgendamento(), request.tipo(), request.observacoes())
                .comDuracao(request.duracaoMinutos() != null ? request.duracaoMinutos() : 30);

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }

    @Transactional
    public AgendamentoResponse cancelar(Long idTutor, Long idAgendamento, String motivo) {
        Agendamento ag = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", idAgendamento));

        if (!ag.getTutor().getIdTutor().equals(idTutor)) {
            throw new RegraDeNegocioException("Agendamento nao pertence a este tutor.");
        }

        try {
            ag.cancelar(motivo);
        } catch (IllegalStateException e) {
            throw new RegraDeNegocioException(e.getMessage());
        }

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }
}
