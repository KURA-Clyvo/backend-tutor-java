package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.AgendamentoRequest;
import br.com.clyvo.kura.tutor.dto.response.AgendamentoResponse;
import br.com.clyvo.kura.tutor.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agendamentos")
@Tag(name = "Agendamentos", description = "Criacao e gerenciamento de agendamentos do tutor")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos do tutor filtrado por status",
               description = "Ex: ?tutorId=1&status=AGENDADO&page=0&size=5")
    public ResponseEntity<Page<AgendamentoResponse>> listar(
            @RequestParam Long tutorId,
            @RequestParam(defaultValue = "AGENDADO") String status,
            @PageableDefault(size = 10, sort = "dtAgendamento", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(agendamentoService.listar(tutorId, status, pageable));
    }

    @PostMapping
    @Operation(summary = "Cria novo agendamento",
               description = "Pet deve estar vinculado ao tutor. Status inicial: AGENDADO. Origem: PORTAL.")
    public ResponseEntity<AgendamentoResponse> criar(
            @RequestParam Long tutorId,
            @Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agendamentoService.criar(tutorId, request));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela agendamento",
               description = "So cancela se status for AGENDADO ou CONFIRMADO. Motivo obrigatorio.")
    public ResponseEntity<AgendamentoResponse> cancelar(
            @PathVariable Long id,
            @RequestParam Long tutorId,
            @RequestParam @NotBlank String motivo) {
        return ResponseEntity.ok(agendamentoService.cancelar(tutorId, id, motivo));
    }
}
