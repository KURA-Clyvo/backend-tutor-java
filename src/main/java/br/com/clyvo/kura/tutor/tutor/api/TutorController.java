package br.com.clyvo.kura.tutor.tutor.api;

import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import br.com.clyvo.kura.tutor.tutor.application.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    @Operation(summary = "Busca tutor por ID",
               description = "Retorna dados públicos do tutor. Requer autenticação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tutor encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Tutor não encontrado")
    })
    public ResponseEntity<TutorResponse> buscarPorId(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @GetMapping
    @Operation(
            summary = "Lista tutores com filtros opcionais",
            description = "Filtros: nome, cidade, uf. Paginado. Ex: ?nome=Felipe&uf=SP&page=0&size=10"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de tutores"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<Page<TutorResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String uf,
            @PageableDefault(size = 10, sort = "nmTutor", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(tutorService.buscarComFiltros(nome, cidade, uf, pageable));
    }

    @GetMapping("/{id}/pets")
    @Operation(
            summary = "Lista pets ativos do tutor autenticado",
            description = "O {id} do path DEVE ser o ID do tutor autenticado — caso contrário retorna 403. " +
                          "Paginado: size padrão 20, máximo 100, sort padrão nmPet asc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pets retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — {id} difere do tutor autenticado"),
            @ApiResponse(responseCode = "404", description = "Tutor não encontrado ou inativo")
    })
    public ResponseEntity<Page<PetResponse>> listarPets(
            @PathVariable Long id,
            Authentication auth,
            @PageableDefault(size = 20, sort = "nmPet", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarPets(id, auth.getName(), pageable));
    }
}
