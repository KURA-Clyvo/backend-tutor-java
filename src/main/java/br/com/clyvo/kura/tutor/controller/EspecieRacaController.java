package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.entity.Especie;
import br.com.clyvo.kura.tutor.entity.Raca;
import br.com.clyvo.kura.tutor.repository.EspecieRepository;
import br.com.clyvo.kura.tutor.repository.RacaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints públicos de lookup — não requerem JWT.
 * Usados pelo app mobile para montar selects de espécie/raça.
 * Dados em cache pois raramente mudam.
 */
@RestController
@Tag(name = "5. Espécies e Raças", description = "Catálogos de lookup — públicos, sem JWT, com cache")
public class EspecieRacaController {

    private final EspecieRepository especieRepo;
    private final RacaRepository racaRepo;

    public EspecieRacaController(EspecieRepository especieRepo, RacaRepository racaRepo) {
        this.especieRepo = especieRepo;
        this.racaRepo = racaRepo;
    }

    @GetMapping("/especies")
    @Operation(
            summary = "Listar todas as espécies",
            description = "Retorna todas as espécies cadastradas. Endpoint público — sem JWT. Dados em cache."
    )
    @Cacheable("especies")
    public ResponseEntity<List<Especie>> listarEspecies() {
        return ResponseEntity.ok(especieRepo.findAll());
    }

    @GetMapping("/racas")
    @Operation(
            summary = "Listar raças",
            description = "Retorna raças paginadas, opcionalmente filtradas por espécie."
    )
    @Cacheable(value = "racas", key = "#especieId + '-' + #pageable.pageNumber")
    public ResponseEntity<Page<Raca>> listarRacas(
            @Parameter(description = "ID da espécie para filtrar (opcional)", example = "1")
            @RequestParam(required = false) Long especieId,
            @PageableDefault(size = 20, sort = "nmRaca") Pageable pageable
    ) {
        Page<Raca> resultado = especieId != null
                ? racaRepo.findByEspecie_IdEspecie(especieId, pageable)
                : racaRepo.findAll(pageable);

        return ResponseEntity.ok(resultado);
    }
}
