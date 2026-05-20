package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.entity.Raca;
import br.com.clyvo.kura.tutor.repository.RacaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RacaService {

    private final RacaRepository racaRepository;

    public RacaService(RacaRepository racaRepository) {
        this.racaRepository = racaRepository;
    }

    /**
     * Retorna todas as raças de uma espécie. Resultado cacheado por idEspecie.
     * A lista completa é cacheada para que a paginação subsequente seja em memória
     * e não gere consultas adicionais ao banco.
     */
    @Cacheable(value = "racas", key = "#idEspecie")
    @Transactional(readOnly = true)
    public List<Raca> listarPorEspecie(Long idEspecie) {
        return racaRepository.findByEspecie_IdEspecie(idEspecie);
    }

    @Transactional(readOnly = true)
    public Page<Raca> listarTodas(Pageable pageable) {
        return racaRepository.findAll(pageable);
    }
}
