package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.response.PetResponse;
import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.service.impl.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de leitura do tutor e seus pets.
 *
 * <p><strong>Lembra:</strong> este controller só lê — o backend .NET é quem
 * cria tutores e pets. Qualquer POST/PUT aqui viria via integração, não direto.
 */
@RestController
@RequestMapping("/tutores")
@Tag(name = "2. Tutores", description = "Consulta de tutores e seus pets (leitura — dados criados pelo .NET)")
@SecurityRequirement(name = "bearerAuth")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping
    @Operation(
            summary = "Listar tutores com filtros e paginação",
            description = """
                    Retorna lista paginada de tutores ativos.
                    Todos os filtros são opcionais — sem filtro retorna todos.
                    Paginação: ?page=0&size=10&sort=nmTutor,asc
                    """
    )
    public ResponseEntity<Page<TutorResponse>> listar(
            @Parameter(description = "Filtro por nome (parcial, case-insensitive)")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro por cidade")
            @RequestParam(required = false) String cidade,
            @Parameter(description = "Filtro por UF (2 letras)", example = "SP")
            @RequestParam(required = false) String uf,
            @Parameter(description = "Filtro por espécie do pet", example = "Cão")
            @RequestParam(required = false) String especie,
            @PageableDefault(size = 10, sort = "nmTutor", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                tutorService.listarComFiltros(nome, cidade, uf, especie, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tutor pelo ID")
    public ResponseEntity<TutorResponse> buscarPorId(
            @Parameter(description = "ID do tutor", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @GetMapping("/{id}/pets")
    @Operation(
            summary = "Listar pets do tutor",
            description = "Retorna os pets ativos vinculados ao tutor via TUTOR_PET."
    )
    public ResponseEntity<Page<PetResponse>> listarPets(
            @Parameter(description = "ID do tutor", example = "1")
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "nmPet") Pageable pageable
    ) {
        return ResponseEntity.ok(tutorService.listarPetsDeTutor(id, pageable));
    }
}
