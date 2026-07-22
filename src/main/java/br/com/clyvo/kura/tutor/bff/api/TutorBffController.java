package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import br.com.clyvo.kura.tutor.tutor.application.TutorService;
import br.com.clyvo.kura.tutor.tutor.dto.PushTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * BFF tutor — expõe /api/v1/tutor/** com idTutor derivado sempre do JWT.
 * Nunca aceita idTutor de path para evitar IDOR.
 */
@RestController
@RequestMapping("/v1/tutor")
@Tag(name = "Tutor BFF", description = "Superfície /api/v1/tutor/** para mobile-tutor-rn")
@SecurityRequirement(name = "bearerAuth")
public class TutorBffController {

    private final TutorService tutorService;
    private final ContaTutorRepository contaTutorRepository;

    public TutorBffController(TutorService tutorService,
                               ContaTutorRepository contaTutorRepository) {
        this.tutorService = tutorService;
        this.contaTutorRepository = contaTutorRepository;
    }

    @GetMapping("/pets")
    @Operation(
        summary = "Lista pets ativos do tutor autenticado",
        description = "idTutor derivado do JWT. Paginado: size 20, sort nmPet asc."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pets retornados"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Tutor não encontrado ou inativo")
    })
    public ResponseEntity<Page<PetResponse>> listarPets(
            Authentication auth,
            @PageableDefault(size = 20, sort = "nmPet", direction = Sort.Direction.ASC) Pageable pageable) {
        Long idTutor = resolverIdTutor(auth);
        return ResponseEntity.ok(tutorService.listarPets(idTutor, auth.getName(), pageable));
    }

    @GetMapping("/pets/{id}")
    @Operation(summary = "Detalhe do pet (stub)", description = "Não implementado — pendente INT-01.")
    @ApiResponse(responseCode = "501", description = "Não implementado")
    public ResponseEntity<Void> detalharPet(
            @Parameter(description = "ID do pet") @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/pets/{id}/timeline")
    @Operation(summary = "Timeline do pet (stub)", description = "Não implementado — pendente INT-01.")
    @ApiResponse(responseCode = "501", description = "Não implementado")
    public ResponseEntity<Void> timelinePet(
            @Parameter(description = "ID do pet") @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PatchMapping("/me/push-token")
    @Operation(
        summary = "Atualiza push token do tutor autenticado",
        description = "LGPD: o valor do token nunca é logado. idTutor derivado do JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token atualizado"),
        @ApiResponse(responseCode = "400", description = "Payload inválido"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "ContaTutor não encontrada")
    })
    public ResponseEntity<Void> atualizarPushToken(
            Authentication auth,
            @Valid @RequestBody PushTokenRequest request) {
        Long idTutor = resolverIdTutor(auth);
        tutorService.atualizarPushToken(idTutor, request);
        return ResponseEntity.noContent().build();
    }

    private Long resolverIdTutor(Authentication auth) {
        return contaTutorRepository
                .findIdTutorByEmail(auth.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ContaTutor", auth.getName()));
    }
}
