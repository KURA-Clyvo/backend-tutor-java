package br.com.clyvo.kura.tutor.timeline.application;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaStatusResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.domain.TimelinePet;
import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendo;
import br.com.clyvo.kura.tutor.timeline.domain.repository.TimelinePetRepository;
import br.com.clyvo.kura.tutor.timeline.domain.repository.VacinaVencendoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TimelineService {

    private final TimelinePetRepository    timelinePetRepository;
    private final VacinaVencendoRepository vacinaVencendoRepository;
    private final ContaTutorRepository     contaTutorRepository;
    private final PetRepository            petRepository;

    public TimelineService(TimelinePetRepository timelinePetRepository,
                           VacinaVencendoRepository vacinaVencendoRepository,
                           ContaTutorRepository contaTutorRepository,
                           PetRepository petRepository) {
        this.timelinePetRepository    = timelinePetRepository;
        this.vacinaVencendoRepository = vacinaVencendoRepository;
        this.contaTutorRepository     = contaTutorRepository;
        this.petRepository            = petRepository;
    }

    @Transactional(readOnly = true)
    public Page<TimelineEventoResponse> listarTimeline(Long idPet, String emailAutenticado, Pageable pageable) {
        verificarVinculo(idPet, emailAutenticado);
        return timelinePetRepository.findByIdPet(idPet, pageable).map(TimelineEventoResponse::fromEntity);
    }

    /**
     * Detalhe de um evento específico da timeline (TASK-31 — antes ausente/sem rota).
     * Reaproveita TimelineEventoResponse: hoje a timeline é derivada de VW_TIMELINE_PET
     * (que por sua vez é AGENDAMENTO), não há dado clínico estruturado adicional
     * (SOAP/diagnóstico/prescrição) exposto por este endpoint.
     */
    @Transactional(readOnly = true)
    public TimelineEventoResponse buscarEventoDetalhe(Long idPet, Long idEvento, String emailAutenticado) {
        verificarVinculo(idPet, emailAutenticado);
        TimelinePet evento = timelinePetRepository.findByIdPetAndIdEvento(idPet, idEvento)
                .orElseThrow(() -> new NotFoundException("Evento", idEvento));
        return TimelineEventoResponse.fromEntity(evento);
    }

    /**
     * @deprecated Endpoint legado ({@code TimelineController}, fora de /v1/tutor,
     * idTutor via path) — não é usado pelo app real (ver INT-01-contract-map).
     * Mantido só para não quebrar o controller legado; TASK-31 substitui a
     * superfície pública por {@link #listarVacinasPet} / {@link #statusVacinasPet},
     * self-scoped e sob /v1/tutor/pets/{id}/vacinas.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<VacinaVencendoResponse> listarVacinasVencendo(Long idTutor, String emailAutenticado) {
        Long idTutorAutenticado = resolverIdTutor(emailAutenticado);
        if (!idTutorAutenticado.equals(idTutor)) {
            throw new ForbiddenException("Acesso negado: você só pode visualizar suas próprias vacinas.");
        }
        return vacinaVencendoRepository.findByIdTutor(idTutor).stream()
                .map(VacinaVencendoResponse::fromEntity)
                .toList();
    }

    /** Vacinas pendentes (próximos 30 dias) de um pet específico — TASK-31. */
    @Transactional(readOnly = true)
    public List<VacinaVencendoResponse> listarVacinasPet(Long idPet, String emailAutenticado) {
        verificarVinculo(idPet, emailAutenticado);
        return vacinaVencendoRepository.findByIdPet(idPet).stream()
                .map(VacinaVencendoResponse::fromEntity)
                .toList();
    }

    /** Resumo de vacinação pendente de um pet específico — TASK-31. */
    @Transactional(readOnly = true)
    public VacinaStatusResponse statusVacinasPet(Long idPet, String emailAutenticado) {
        verificarVinculo(idPet, emailAutenticado);
        List<VacinaVencendo> pendentes = vacinaVencendoRepository.findByIdPet(idPet);

        var dtProximaDose = pendentes.stream()
                .map(VacinaVencendo::getDtProximaDose)
                .min(Comparator.naturalOrder())
                .orElse(null);

        String status = pendentes.isEmpty() ? "EM_DIA" : "ALERTA";

        return new VacinaStatusResponse(idPet, pendentes.size(), dtProximaDose, status);
    }

    private void verificarVinculo(Long idPet, String emailAutenticado) {
        Long idTutor = resolverIdTutor(emailAutenticado);
        if (petRepository.countVinculo(idPet, idTutor) == 0) {
            throw new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado.");
        }
    }

    private Long resolverIdTutor(String email) {
        return contaTutorRepository.findIdTutorByEmail(email)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o e-mail autenticado."));
    }
}
