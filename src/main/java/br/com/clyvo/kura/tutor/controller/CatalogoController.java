package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.entity.Especie;
import br.com.clyvo.kura.tutor.entity.Raca;
import br.com.clyvo.kura.tutor.repository.EspecieRepository;
import br.com.clyvo.kura.tutor.repository.RacaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "Catalogo", description = "Especies e racas — dados de referencia, sem autenticacao")
public class CatalogoController {

    private final EspecieRepository especieRepository;
    private final RacaRepository racaRepository;

    public CatalogoController(EspecieRepository especieRepository, RacaRepository racaRepository) {
        this.especieRepository = especieRepository;
        this.racaRepository = racaRepository;
    }

    @GetMapping("/especies")
    @Operation(summary = "Lista todas as especies (cacheado, publico)")
    @Cacheable("especies")
    public ResponseEntity<List<Especie>> listarEspecies() {
        return ResponseEntity.ok(especieRepository.findAll());
    }

    @GetMapping("/racas")
    @Operation(summary = "Lista racas, opcionalmente filtradas por especieId (publico)")
    public ResponseEntity<Page<Raca>> listarRacas(
            @RequestParam(required = false) Long especieId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Raca> resultado = (especieId != null)
                ? racaRepository.findByEspecie_IdEspecie(especieId, pageable)
                : racaRepository.findAll(pageable);
        return ResponseEntity.ok(resultado);
    }
}
