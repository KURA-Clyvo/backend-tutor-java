package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.response.PetResponse;
import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tutores")
@Tag(name = "Tutores", description = "Consulta de dados do tutor e seus pets")
@SecurityRequirement(name = "bearerAuth")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca tutor por ID")
    public ResponseEntity<TutorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Lista tutores com filtros opcionais",
               description = "Filtros: nome, cidade, uf. Paginado. Ex: ?nome=Felipe&uf=SP&page=0&size=10")
    public ResponseEntity<Page<TutorResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String uf,
            @PageableDefault(size = 10, sort = "nmTutor", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(tutorService.buscarComFiltros(nome, cidade, uf, pageable));
    }

    @GetMapping("/{id}/pets")
    @Operation(summary = "Lista pets ativos do tutor (resultado cacheado)")
    public ResponseEntity<Page<PetResponse>> listarPets(
            @PathVariable Long id,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarPets(id, pageable));
    }
}
