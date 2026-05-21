package br.com.clyvo.kura.tutor.timeline.application;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.domain.repository.TimelinePetRepository;
import br.com.clyvo.kura.tutor.timeline.domain.repository.VacinaVencendoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Long idTutor = resolverIdTutor(emailAutenticado);
        if (petRepository.countVinculo(idPet, idTutor) == 0) {
            throw new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado.");
        }
        return timelinePetRepository.findByIdPet(idPet, pageable).map(TimelineEventoResponse::fromEntity);
    }

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

    private Long resolverIdTutor(String email) {
        return contaTutorRepository.findIdTutorByEmail(email)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o e-mail autenticado."));
    }
}
