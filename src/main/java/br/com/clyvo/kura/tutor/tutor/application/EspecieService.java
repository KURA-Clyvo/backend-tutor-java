package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.entity.Especie;
import br.com.clyvo.kura.tutor.repository.EspecieRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspecieService {

    private final EspecieRepository especieRepository;

    public EspecieService(EspecieRepository especieRepository) {
        this.especieRepository = especieRepository;
    }

    @Cacheable("especies")
    @Transactional(readOnly = true)
    public List<Especie> listarTodas() {
        return especieRepository.findAll();
    }
}
