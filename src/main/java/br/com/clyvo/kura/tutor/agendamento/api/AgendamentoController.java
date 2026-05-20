package br.com.clyvo.kura.tutor.agendamento.api;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoUpdateRequest;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

@RestController
@RequestMapping("/agendamentos")
@Tag(name = "Agendamentos", description = "Criação e consulta de agendamentos do tutor autenticado")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    /**
     * Lista agendamentos do tutor autenticado com filtros opcionais.
     *
     * tutorId é SEMPRE resolvido do SecurityContext — nunca de query param.
     * Isso evita IDOR: um tutor não pode consultar agendamentos alheios.
     */
    @GetMapping
    @Operation(
        summary = "Lista agendamentos do tutor autenticado",
        description = "tutorId vem do token JWT — não é aceito como query param. " +
                      "Filtros opcionais: status, dataInicio, dataFim, tipo."
    )
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

    /**
     * Cria um novo agendamento para o tutor autenticado.
     *
     * Retorna 201 com header Location apontando para o recurso criado.
     * Pet deve pertencer ao tutor — violação retorna 403.
     */
    @PostMapping
    @Operation(
        summary = "Cria agendamento",
        description = "Pet deve estar vinculado ao tutor. Status inicial: AGENDADO. Origem: PORTAL."
    )
    @ApiResponse(responseCode = "201", description = "Agendamento criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou data no passado")
    @ApiResponse(responseCode = "403", description = "Pet não pertence ao tutor autenticado")
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

    /**
     * Atualiza dados de um agendamento do tutor autenticado.
     *
     * nrVersion obrigatório — divergência com versão atual retorna 409.
     * Retorna 403 se o agendamento não pertencer ao tutor.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Atualiza agendamento",
        description = "Requer nrVersion correto. Versão divergente retorna 409 (optimistic lock)."
    )
    @ApiResponse(responseCode = "200", description = "Agendamento atualizado")
    @ApiResponse(responseCode = "400", description = "nrVersion ausente ou data inválida")
    @ApiResponse(responseCode = "403", description = "Agendamento não pertence ao tutor")
    @ApiResponse(responseCode = "409", description = "Conflito de versão — recarregue e tente novamente")
    public ResponseEntity<AgendamentoResponse> atualizar(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoUpdateRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizar(auth.getName(), id, request));
    }

    /**
     * Soft-delete: muda status para CANCELADO.
     *
     * Retorna 403 se o agendamento não pertencer ao tutor.
     * Retorna 409 se o status for REALIZADO ou já CANCELADO.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Exclui agendamento (soft delete)",
        description = "Não executa DELETE SQL — muda ST_STATUS para CANCELADO."
    )
    @ApiResponse(responseCode = "204", description = "Agendamento cancelado (soft delete)")
    @ApiResponse(responseCode = "403", description = "Agendamento não pertence ao tutor")
    @ApiResponse(responseCode = "409", description = "Agendamento REALIZADO ou já CANCELADO")
    public ResponseEntity<Void> excluir(
            Authentication auth,
            @PathVariable Long id) {
        agendamentoService.excluir(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancela agendamento do tutor autenticado.
     *
     * Retorna 403 se o agendamento não pertencer ao tutor.
     * Retorna 422 se o status não permitir cancelamento (REALIZADO ou CANCELADO).
     */
    @PatchMapping("/{id}/cancelar")
    @Operation(
        summary = "Cancela agendamento",
        description = "Só cancela se status for AGENDADO, CONFIRMADO ou INTENCAO. Motivo obrigatório."
    )
    public ResponseEntity<AgendamentoResponse> cancelar(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam @NotBlank String motivo) {
        return ResponseEntity.ok(agendamentoService.cancelar(auth.getName(), id, motivo));
    }
}
