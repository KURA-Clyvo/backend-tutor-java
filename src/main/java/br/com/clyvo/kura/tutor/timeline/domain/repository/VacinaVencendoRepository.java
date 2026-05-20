package br.com.clyvo.kura.tutor.timeline.domain.repository;

import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendo;
import br.com.clyvo.kura.tutor.timeline.domain.VacinaVencendoId;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface VacinaVencendoRepository
        extends Repository<VacinaVencendo, VacinaVencendoId> {

    List<VacinaVencendo> findByIdTutor(Long idTutor);
}
