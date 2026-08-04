package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaStatusResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.application.TimelineService;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetDetalheResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BFF tutor — expõe /api/v1/tutor/** com idTutor derivado sempre do JWT.
 * Nunca aceita idTutor de path para evitar IDOR. {id}/{idPet} no path identificam
 * o recurso (pet), não o tutor — a posse é sempre verificada contra o idTutor do JWT.
 */
@RestController
@RequestMapping("/v1/tutor")
@Tag(name = "Tutor BFF", description = "Superfície /api/v1/tutor/** para mobile-tutor-rn")
@SecurityRequirement(name = "bearerAuth")
public class TutorBffController {

    private final TutorService tutorService;
    private final TimelineService timelineService;
    private final ContaTutorRepository contaTutorRepository;

    public TutorBffController(TutorService tutorService,
                               TimelineService timelineService,
                               ContaTutorRepository contaTutorRepository) {
        this.tutorService = tutorService;
        this.timelineService = timelineService;
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
    @Operation(
        summary = "Detalhe do pet (TASK-31)",
        description = "idTutor derivado do JWT; posse do pet verificada via vínculo tutor-pet."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet retornado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    public ResponseEntity<PetDetalheResponse> detalharPet(
            @Parameter(description = "ID do pet") @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(tutorService.buscarPetDetalhe(id, auth.getName()));
    }

    @GetMapping("/pets/{id}/timeline")
    @Operation(
        summary = "Linha do tempo de atendimentos do pet (TASK-31)",
        description = "Paginada, ordenada por dtEvento desc. idTutor derivado do JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeline retornada"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado")
    })
    public ResponseEntity<Page<TimelineEventoResponse>> timelinePet(
            @Parameter(description = "ID do pet") @PathVariable Long id,
            @PageableDefault(size = 20, sort = "dtEvento", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarTimeline(id, auth.getName(), pageable));
    }

    @GetMapping("/pets/{id}/timeline/{idEvento}")
    @Operation(
        summary = "Detalhe de um evento da timeline do pet (TASK-31)",
        description = "idTutor derivado do JWT. Evento identificado por ID_EVENTO (VW_TIMELINE_PET)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento retornado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento não encontrado para este pet")
    })
    public ResponseEntity<TimelineEventoResponse> detalharEventoTimeline(
            @Parameter(description = "ID do pet") @PathVariable Long id,
            @Parameter(description = "ID do evento") @PathVariable Long idEvento,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.buscarEventoDetalhe(id, idEvento, auth.getName()));
    }

    @GetMapping("/pets/{id}/vacinas")
    @Operation(
        summary = "Vacinas pendentes do pet nos próximos 30 dias (TASK-31)",
        description = "Leitura de VW_VACINAS_VENCENDO. idTutor derivado do JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de vacinas pendentes"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado")
    })
    public ResponseEntity<List<VacinaVencendoResponse>> vacinasPet(
            @Parameter(description = "ID do pet") @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarVacinasPet(id, auth.getName()));
    }

    @GetMapping("/pets/{id}/vacinas/status")
    @Operation(
        summary = "Resumo de vacinação pendente do pet (TASK-31)",
        description = "Leitura de VW_VACINAS_VENCENDO. idTutor derivado do JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retornado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado")
    })
    public ResponseEntity<VacinaStatusResponse> statusVacinasPet(
            @Parameter(description = "ID do pet") @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.statusVacinasPet(id, auth.getName()));
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
