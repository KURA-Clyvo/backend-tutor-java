package br.com.clyvo.kura.tutor.timeline.api;

import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.application.TimelineService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Timeline", description = "Histórico clínico e vacinas vencendo — tutor só acessa seus próprios dados")
@SecurityRequirement(name = "bearerAuth")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/pets/{idPet}/timeline")
    @Operation(
        summary = "Linha do tempo de atendimentos do pet",
        description = "Paginada, ordenada por dtEvento desc. Tutor só acessa pets vinculados a ele."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeline retornada"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    public ResponseEntity<Page<TimelineEventoResponse>> listarTimeline(
            @Parameter(description = "ID do pet", example = "1") @PathVariable Long idPet,
            @PageableDefault(size = 20, sort = "dtEvento", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarTimeline(idPet, auth.getName(), pageable));
    }

    @GetMapping("/tutores/{idTutor}/vacinas-vencendo")
    @Operation(
        summary = "Vacinas agendadas nos próximos 30 dias para os pets do tutor",
        description = "Tutor só pode consultar seus próprios dados ({idTutor} deve corresponder ao token)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de vacinas vencendo"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<List<VacinaVencendoResponse>> listarVacinasVencendo(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarVacinasVencendo(idTutor, auth.getName()));
    }
}
