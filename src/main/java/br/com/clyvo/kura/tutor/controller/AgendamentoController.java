package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.AgendamentoRequest;
import br.com.clyvo.kura.tutor.dto.response.AgendamentoResponse;
import br.com.clyvo.kura.tutor.service.impl.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de agendamento pelo portal do tutor.
 *
 * <p>Lembra: AGENDAMENTO = intenção futura (domínio Java).
 * EVENTO_CLINICO = histórico realizado (domínio .NET).
 */
@RestController
@RequestMapping("/tutores/{idTutor}/agendamentos")
@Tag(name = "4. Agendamentos", description = "Criação e gestão de agendamentos pelo portal do tutor")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    @Operation(
            summary = "Criar agendamento",
            description = """
                    Cria um agendamento com origem PORTAL.
                    O pet informado deve estar vinculado ao tutor via TUTOR_PET.
                    A data deve ser futura. Duração padrão: 30 minutos.
                    """
    )
    public ResponseEntity<AgendamentoResponse> criar(
            @Parameter(description = "ID do tutor autenticado", example = "1")
            @PathVariable Long idTutor,
            @Valid @RequestBody AgendamentoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(agendamentoService.criar(idTutor, request));
    }

    @GetMapping
    @Operation(
            summary = "Listar agendamentos do tutor",
            description = "Retorna agendamentos paginados. Filtro por status: AGENDADO (default), CONFIRMADO, CANCELADO, REALIZADO."
    )
    public ResponseEntity<Page<AgendamentoResponse>> listar(
            @PathVariable Long idTutor,
            @Parameter(description = "Filtro por status", example = "AGENDADO")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "dtAgendamento", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(
                agendamentoService.listarPorTutor(idTutor, status, pageable));
    }

    @PatchMapping("/{idAgendamento}/cancelar")
    @Operation(
            summary = "Cancelar agendamento",
            description = """
                    Cancela um agendamento do tutor.
                    Segurança: verifica se o agendamento pertence ao tutor informado (anti-IDOR).
                    Agendamentos já realizados ou cancelados não podem ser cancelados novamente.
                    """
    )
    public ResponseEntity<AgendamentoResponse> cancelar(
            @PathVariable Long idTutor,
            @PathVariable Long idAgendamento,
            @Parameter(description = "Motivo do cancelamento (opcional, máx. 500 chars)")
            @RequestParam(required = false)
            @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres.")
            String motivo) {

        return ResponseEntity.ok(
                agendamentoService.cancelar(idTutor, idAgendamento, motivo));
    }
}
