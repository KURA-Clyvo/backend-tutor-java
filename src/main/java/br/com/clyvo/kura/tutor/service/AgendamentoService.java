package br.com.clyvo.kura.tutor.service;

import br.com.clyvo.kura.tutor.dto.request.AgendamentoRequest;
import br.com.clyvo.kura.tutor.dto.response.AgendamentoResponse;
import br.com.clyvo.kura.tutor.entity.Agendamento;
import br.com.clyvo.kura.tutor.entity.Clinica;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.repository.AgendamentoRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Servico de agendamentos.
 * Agendamento = intencao futura. Quando realizado, .NET vincula ID_EVENTO_GERADO.
 */
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
        return agendamentoRepository
                .findByTutor_IdTutorAndStStatus(idTutor, status, pageable)
                .map(AgendamentoResponse::fromEntity);
    }

    @Transactional
    public AgendamentoResponse criar(Long idTutor, AgendamentoRequest request) {
        Tutor tutor = tutorRepository.findById(idTutor)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        Pet pet = petRepository.findById(request.idPet())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", request.idPet()));

        // Prevencao de IDOR: pet deve pertencer ao tutor autenticado
        boolean petDoTutor = pet.getTutorPets().stream()
                .anyMatch(tp -> tp.getTutor().getIdTutor().equals(idTutor));
        if (!petDoTutor) {
            throw new RegraDeNegocioException("Pet nao vinculado a este tutor.");
        }

        // getReference = proxy sem query — apenas FK necessaria
        Clinica clinica = entityManager.getReference(Clinica.class, request.idClinica());

        Agendamento ag = new Agendamento();
        ag.setTutor(tutor);
        ag.setPet(pet);
        ag.setClinica(clinica);
        ag.setIdVeterinario(request.idVeterinario());
        ag.setDtAgendamento(request.dtAgendamento());
        ag.setNrDuracaoMinutos(request.duracaoMinutos() != null ? request.duracaoMinutos() : 30);
        ag.setDsTipo(request.tipo());
        ag.setDsObservacoes(request.observacoes());
        ag.setStStatus("AGENDADO");
        ag.setDsOrigem("PORTAL");
        ag.setDtCriacao(LocalDateTime.now());

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }

    @Transactional
    public AgendamentoResponse cancelar(Long idTutor, Long idAgendamento, String motivo) {
        Agendamento ag = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", idAgendamento));

        if (!ag.getTutor().getIdTutor().equals(idTutor)) {
            throw new RegraDeNegocioException("Agendamento nao pertence a este tutor.");
        }
        if (!ag.isAtivo()) {
            throw new RegraDeNegocioException(
                "Agendamento ja esta " + ag.getStStatus() + " e nao pode ser cancelado.");
        }

        ag.setStStatus("CANCELADO");
        ag.setDtCancelamento(LocalDateTime.now());
        ag.setDsMotivoCancel(motivo);
        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }
}
