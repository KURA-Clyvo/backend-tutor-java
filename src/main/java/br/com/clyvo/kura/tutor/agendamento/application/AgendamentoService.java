package br.com.clyvo.kura.tutor.agendamento.application;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoUpdateRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.repository.AgendamentoRepository;
import br.com.clyvo.kura.tutor.agendamento.domain.specification.AgendamentoSpecs;
import br.com.clyvo.kura.tutor.entity.Clinica;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ContaTutorRepository contaTutorRepository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final EntityManager entityManager;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                               ContaTutorRepository contaTutorRepository,
                               TutorRepository tutorRepository,
                               PetRepository petRepository,
                               EntityManager entityManager) {
        this.agendamentoRepository = agendamentoRepository;
        this.contaTutorRepository  = contaTutorRepository;
        this.tutorRepository       = tutorRepository;
        this.petRepository         = petRepository;
        this.entityManager         = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoResponse> listar(String emailAutenticado,
                                             String status,
                                             LocalDateTime dataInicio,
                                             LocalDateTime dataFim,
                                             String tipo,
                                             Pageable pageable) {
        Long idTutor = resolverIdTutor(emailAutenticado);
        StatusAgendamento statusEnum = parseStatus(status);
        Specification<Agendamento> spec =
            AgendamentoSpecs.construir(idTutor, statusEnum, dataInicio, dataFim, tipo);
        return agendamentoRepository.findAll(spec, pageable)
                .map(AgendamentoResponse::fromEntity);
    }

    @Transactional
    public AgendamentoResponse criar(String emailAutenticado, AgendamentoRequest request) {
        Long idTutor = resolverIdTutor(emailAutenticado);

        Tutor tutor = tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new NotFoundException("Tutor não encontrado ou inativo."));

        Pet pet = petRepository.findByIdPetAndStAtivo(request.idPet(), "S")
                .orElseThrow(() -> new NotFoundException("Pet não encontrado ou inativo."));

        boolean petDoTutor = pet.getTutorPets().stream()
                .anyMatch(tp -> tp.getTutor().getIdTutor().equals(idTutor));
        if (!petDoTutor) {
            throw new ForbiddenException("Pet não vinculado a este tutor.");
        }

        Clinica clinica = entityManager.getReference(Clinica.class, request.idClinica());

        Agendamento ag = Agendamento.criar(
                tutor, pet, clinica, request.idVeterinario(),
                request.dtAgendamento(), request.tipo(), request.observacoes())
                .comDuracao(request.duracaoMinutos() != null ? request.duracaoMinutos() : 30);

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }

    @Transactional
    public AgendamentoResponse atualizar(String emailAutenticado, Long idAgendamento,
                                          AgendamentoUpdateRequest request) {
        Long idTutor = resolverIdTutor(emailAutenticado);

        Agendamento ag = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (ag.getTutor() == null || !ag.getTutor().getIdTutor().equals(idTutor)) {
            throw new ForbiddenException("Agendamento não pertence a este tutor.");
        }

        if (!request.nrVersion().equals(ag.getNrVersion())) {
            throw new ObjectOptimisticLockingFailureException(Agendamento.class, idAgendamento);
        }

        ag.atualizar(request.dtAgendamento(), request.dsTipoConsulta(),
                     request.dsObservacoes(), request.idVeterinario());

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }

    @Transactional
    public void excluir(String emailAutenticado, Long idAgendamento) {
        Long idTutor = resolverIdTutor(emailAutenticado);

        Agendamento ag = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (ag.getTutor() == null || !ag.getTutor().getIdTutor().equals(idTutor)) {
            throw new ForbiddenException("Agendamento não pertence a este tutor.");
        }

        if (ag.getStStatus() == StatusAgendamento.REALIZADO
                || ag.getStStatus() == StatusAgendamento.CANCELADO) {
            throw new ConflictException(
                "Não é possível cancelar agendamento com status " + ag.getStStatus().name() + ".");
        }

        ag.cancelar("Cancelado pelo tutor");
        agendamentoRepository.save(ag);
    }

    @Transactional
    public AgendamentoResponse cancelar(String emailAutenticado, Long idAgendamento, String motivo) {
        Long idTutor = resolverIdTutor(emailAutenticado);

        Agendamento ag = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (ag.getTutor() == null || !ag.getTutor().getIdTutor().equals(idTutor)) {
            throw new ForbiddenException("Agendamento não pertence a este tutor.");
        }

        try {
            ag.cancelar(motivo);
        } catch (IllegalStateException e) {
            throw new RegraDeNegocioException(e.getMessage());
        }

        return AgendamentoResponse.fromEntity(agendamentoRepository.save(ag));
    }

    private Long resolverIdTutor(String email) {
        return contaTutorRepository.findIdTutorByEmail(email)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o e-mail autenticado."));
    }

    private static StatusAgendamento parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return StatusAgendamento.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
