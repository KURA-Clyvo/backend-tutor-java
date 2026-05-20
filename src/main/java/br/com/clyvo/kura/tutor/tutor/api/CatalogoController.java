package br.com.clyvo.kura.tutor.tutor.api;

import br.com.clyvo.kura.tutor.entity.Especie;
import br.com.clyvo.kura.tutor.entity.Raca;
import br.com.clyvo.kura.tutor.tutor.application.EspecieService;
import br.com.clyvo.kura.tutor.tutor.application.RacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Catalogo", description = "Espécies e raças — dados de referência, sem autenticação")
public class CatalogoController {

    private final EspecieService especieService;
    private final RacaService    racaService;

    public CatalogoController(EspecieService especieService, RacaService racaService) {
        this.especieService = especieService;
        this.racaService    = racaService;
    }

    @GetMapping("/especies")
    @Operation(summary = "Lista todas as espécies (cacheado 6h, público)")
    public ResponseEntity<List<Especie>> listarEspecies() {
        return ResponseEntity.ok(especieService.listarTodas());
    }

    @GetMapping("/racas")
    @Operation(summary = "Lista raças, opcionalmente filtradas por especieId (público)",
               description = "Com especieId: resultado cacheado 6h, paginação em memória. Sem filtro: consulta paginada ao banco.")
    public ResponseEntity<Page<Raca>> listarRacas(
            @RequestParam(required = false) Long especieId,
            @PageableDefault(size = 20) Pageable pageable) {

        if (especieId != null) {
            List<Raca> todas = racaService.listarPorEspecie(especieId);
            return ResponseEntity.ok(paginarEmMemoria(todas, pageable));
        }
        return ResponseEntity.ok(racaService.listarTodas(pageable));
    }

    private Page<Raca> paginarEmMemoria(List<Raca> todas, Pageable pageable) {
        int inicio = (int) Math.min(pageable.getOffset(), todas.size());
        int fim    = Math.min(inicio + pageable.getPageSize(), todas.size());
        return new PageImpl<>(todas.subList(inicio, fim), pageable, todas.size());
    }
}
