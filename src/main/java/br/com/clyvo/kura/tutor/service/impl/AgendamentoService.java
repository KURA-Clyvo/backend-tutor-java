package br.com.clyvo.kura.tutor.service.impl;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço de criação e gestão de agendamentos pelo portal do tutor.
 *
 * <p><strong>Diferença importante:</strong> AGENDAMENTO é intenção futura.
 * EVENTO_CLINICO é histórico passado (domínio .NET). Quando o agendamento
 * é realizado, o .NET cria o evento e atualiza {@code ID_EVENTO_GERADO}.
 */
@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepo;
    private final TutorRepository tutorRepo;
    private final PetRepository petRepo;

    public AgendamentoService(AgendamentoRepository agendamentoRepo,
                               TutorRepository tutorRepo,
                               PetRepository petRepo) {
        this.agendamentoRepo = agendamentoRepo;
        this.tutorRepo = tutorRepo;
        this.petRepo = petRepo;
    }

    // ── Criação ───────────────────────────────────────────────────────────

    /**
     * Cria um novo agendamento pelo portal do tutor.
     *
     * <p>Regras de negócio:
     * <ul>
     *   <li>O pet deve pertencer ao tutor (via TUTOR_PET)
     *   <li>Data deve ser futura (validada no DTO com @Future)
     *   <li>Duração default: 30 minutos se não informado
     *   <li>Origem: sempre PORTAL quando criado aqui
     * </ul>
     */
    @Transactional
    public AgendamentoResponse criar(Long idTutor, AgendamentoRequest request) {
        Tutor tutor = tutorRepo.findById(idTutor)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        Pet pet = petRepo.findById(request.idPet())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", request.idPet()));

        // Verifica se o pet pertence ao tutor
        boolean petDeTutor = pet.getTutorPets().stream()
                .anyMatch(tp -> tp.getTutor().getIdTutor().equals(idTutor));
        if (!petDeTutor) {
            throw new RegraDeNegocioException(
                    "Este pet não está vinculado ao tutor informado.");
        }

        // Clinica é uma referência — não carregamos tudo, usamos proxy JPA
        Clinica clinica = new Clinica();
        clinica.setIdClinica(request.idClinica());

        Agendamento agendamento = Agendamento.builder()
                .tutor(tutor)
                .pet(pet)
                .clinica(clinica)
                .idVeterinario(request.idVeterinario())
                .dtAgendamento(request.dtAgendamento())
                .nrDuracaoMinutos(request.nrDuracaoMinutos() != null
                        ? request.nrDuracaoMinutos() : 30)
                .dsTipo(request.tipo())
                .dsObservacoes(request.observacoes())
                .stStatus("AGENDADO")
                .dsOrigem("PORTAL")
                .dtCriacao(LocalDateTime.now())
                .build();

        return AgendamentoResponse.from(agendamentoRepo.save(agendamento));
    }

    // ── Listagem ──────────────────────────────────────────────────────────

    /** Lista agendamentos do tutor, opcionalmente filtrando por status. */
    @Transactional(readOnly = true)
    public Page<AgendamentoResponse> listarPorTutor(Long idTutor,
                                                     String status,
                                                     Pageable pageable) {
        if (!tutorRepo.existsById(idTutor)) {
            throw new RecursoNaoEncontradoException("Tutor", idTutor);
        }

        // Status null = todos os status
        if (status == null || status.isBlank()) {
            status = "AGENDADO"; // default: só agendamentos futuros
        }

        return agendamentoRepo
                .findByTutor_IdTutorAndStStatus(idTutor, status, pageable)
                .map(AgendamentoResponse::from);
    }

    // ── Cancelamento ──────────────────────────────────────────────────────

    /**
     * Cancela um agendamento.
     *
     * <p>Regras:
     * <ul>
     *   <li>Agendamento deve pertencer ao tutor (segurança multi-tenant)
     *   <li>Não pode cancelar agendamento já realizado ou cancelado
     * </ul>
     */
    @Transactional
    public AgendamentoResponse cancelar(Long idTutor, Long idAgendamento, String motivo) {
        var agendamento = agendamentoRepo.findById(idAgendamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Agendamento", idAgendamento));

        // Segurança: verifica ownership (anti-IDOR)
        if (!agendamento.getTutor().getIdTutor().equals(idTutor)) {
            throw new RecursoNaoEncontradoException("Agendamento", idAgendamento);
        }

        if (agendamento.isCancelado()) {
            throw new RegraDeNegocioException("Este agendamento já está cancelado.");
        }
        if ("REALIZADO".equals(agendamento.getStStatus())) {
            throw new RegraDeNegocioException(
                    "Não é possível cancelar um agendamento já realizado.");
        }

        agendamento.setStStatus("CANCELADO");
        agendamento.setDtCancelamento(LocalDateTime.now());
        agendamento.setDsMotivoCancel(motivo);

        return AgendamentoResponse.from(agendamentoRepo.save(agendamento));
    }
}
