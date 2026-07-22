package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * BFF agendamentos — expõe /api/v1/tutor/agendamentos/** com tutor derivado do JWT.
 * AgendamentoService resolve idTutor internamente a partir do email (auth.getName()).
 */
@RestController
@RequestMapping("/v1/tutor/agendamentos")
@Tag(name = "Agendamentos BFF", description = "Superfície /api/v1/tutor/agendamentos/** para mobile-tutor-rn")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoBffController {

    private final AgendamentoService agendamentoService;

    public AgendamentoBffController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    @Operation(
        summary = "Lista agendamentos do tutor autenticado",
        description = "idTutor derivado do JWT. Filtros opcionais: status, dataInicio, dataFim, tipo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista paginada de agendamentos"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<Page<AgendamentoResponse>> listar(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime dataInicio,
            @RequestParam(required = false) LocalDateTime dataFim,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 10, sort = "dtAgendamento", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(
            agendamentoService.listar(auth.getName(), status, dataInicio, dataFim, tipo, pageable));
    }

    @PostMapping
    @Operation(
        summary = "Cria agendamento para o tutor autenticado",
        description = "Pet deve pertencer ao tutor. Status inicial: AGENDADO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agendamento criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado")
    })
    public ResponseEntity<AgendamentoResponse> criar(
            Authentication auth,
            @Valid @RequestBody AgendamentoRequest request) {
        AgendamentoResponse response = agendamentoService.criar(auth.getName(), request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.idAgendamento())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Cancela agendamento (soft delete)",
        description = "Muda ST_STATUS para CANCELADO. Retorna 403 se não pertencer ao tutor."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agendamento cancelado"),
        @ApiResponse(responseCode = "403", description = "Agendamento não pertence ao tutor"),
        @ApiResponse(responseCode = "409", description = "Agendamento REALIZADO ou já CANCELADO")
    })
    public ResponseEntity<Void> excluir(
            Authentication auth,
            @Parameter(description = "ID do agendamento") @PathVariable Long id) {
        agendamentoService.excluir(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
