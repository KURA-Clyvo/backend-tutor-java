package br.com.clyvo.kura.tutor.consentimento.api;

import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService.RegistroResult;
import br.com.clyvo.kura.tutor.consentimento.lgpd.AuditoriaSessao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutores/{idTutor}/consentimentos")
@Tag(name = "Consentimentos LGPD", description = "Gerenciamento de consentimentos do tutor (LGPD)")
@SecurityRequirement(name = "bearerAuth")
public class ConsentimentoController {

    private final ConsentimentoService consentimentoService;

    public ConsentimentoController(ConsentimentoService consentimentoService) {
        this.consentimentoService = consentimentoService;
    }

    /**
     * Retorna o último registro de cada tipo de consentimento para o tutor.
     * Representa o estado atual (não o histórico completo — ver LgpdController para auditoria).
     * Autorização: tutor autenticado só acessa seus próprios dados.
     */
    @GetMapping
    @Operation(
        summary = "Estado atual de cada tipo de consentimento",
        description = "Retorna o último registro por tipo — representa o estado vigente. " +
                      "Para o histórico completo use GET /lgpd/consentimentos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de consentimentos vigentes"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<List<ConsentimentoResponse>> listarUltimosPorTipo(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            Authentication auth) {
        return ResponseEntity.ok(
                consentimentoService.listarUltimosPorTipo(idTutor, auth.getName()));
    }

    /**
     * Registra aceite ou revogação de consentimento com idempotência obrigatória.
     *
     * Header Idempotency-Key (UUID v4) é OBRIGATÓRIO:
     *   - Sem header: 400
     *   - Mesma key, dentro do TTL de 24h: 200 com o registro original (sem duplicata)
     *   - Nova key: 201 com o novo registro criado
     */
    @PostMapping
    @Operation(
        summary = "Registra aceite ou revogação com idempotência obrigatória",
        description = "Header `Idempotency-Key` (UUID v4) é obrigatório. " +
                      "Reenviar a mesma key dentro de 24h retorna 200 sem duplicar o registro. " +
                      "Nova key → 201 com novo registro."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consentimento registrado"),
        @ApiResponse(responseCode = "200", description = "Idempotência — registro original retornado"),
        @ApiResponse(responseCode = "400", description = "Payload inválido ou Idempotency-Key ausente"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<ConsentimentoResponse> registrar(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest,
            Authentication auth) {

        AuditoriaSessao sessao = AuditoriaSessao.from(httpRequest);
        RegistroResult result = consentimentoService.registrarComIdempotencia(
                idTutor, request, sessao.ipCliente(), idempotencyKey, auth.getName());

        HttpStatus status = result.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }
}
