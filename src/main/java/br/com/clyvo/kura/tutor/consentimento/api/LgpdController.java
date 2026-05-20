package br.com.clyvo.kura.tutor.consentimento.api;

import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService;
import br.com.clyvo.kura.tutor.consentimento.application.RelatorioLgpdService;
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
import java.util.Map;

/**
 * Endpoints de direitos do titular de dados (LGPD art. 18).
 * Mantém o histórico completo de consentimentos para fins de auditoria ANPD.
 */
@RestController
@RequestMapping("/tutores/{idTutor}/lgpd")
@Tag(name = "LGPD — Direitos do Titular",
     description = "Acesso, portabilidade e revogação de dados pessoais (LGPD art. 18)")
@SecurityRequirement(name = "bearerAuth")
public class LgpdController {

    private final RelatorioLgpdService relatorioLgpdService;
    private final ConsentimentoService consentimentoService;

    public LgpdController(RelatorioLgpdService relatorioLgpdService,
                          ConsentimentoService consentimentoService) {
        this.relatorioLgpdService = relatorioLgpdService;
        this.consentimentoService = consentimentoService;
    }

    @GetMapping("/relatorio")
    @Operation(
        summary = "Relatório completo de dados pessoais (art. 18, I e V)",
        description = "Agrega todos os dados pessoais do tutor armazenados pela plataforma. " +
                      "Direito de acesso LGPD art. 18, I."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório gerado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<Map<String, Object>> relatorio(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            Authentication auth) {
        return ResponseEntity.ok(relatorioLgpdService.gerarRelatorio(idTutor, auth.getName()));
    }

    @GetMapping("/consentimentos")
    @Operation(
        summary = "Histórico completo de consentimentos — evidência de auditoria ANPD",
        description = "Retorna TODOS os registros (incluindo revogações), não só o mais recente por tipo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico de consentimentos"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<List<ConsentimentoResponse>> listarConsentimentos(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            Authentication auth) {
        return ResponseEntity.ok(consentimentoService.listar(idTutor, auth.getName()));
    }

    @GetMapping("/consentimentos/{tipo}/ativo")
    @Operation(
        summary = "Estado atual de um tipo de consentimento",
        description = "Retorna o consentimento ativo mais recente do tipo informado. 404 se não existe ou foi revogado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consentimento ativo encontrado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado"),
        @ApiResponse(responseCode = "404", description = "Consentimento não encontrado ou revogado")
    })
    public ResponseEntity<ConsentimentoResponse> estadoAtual(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            @Parameter(description = "Tipo do consentimento", example = "LEMBRETES") @PathVariable String tipo,
            Authentication auth) {
        return consentimentoService.buscarAtivo(idTutor, tipo, auth.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/consentimentos")
    @Operation(
        summary = "Registra aceite ou revogação (Idempotency-Key opcional para auditoria LGPD)",
        description = "Cada POST = novo INSERT imutável. Idempotency-Key opcional — sem garantia de deduplicação."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consentimento registrado"),
        @ApiResponse(responseCode = "400", description = "Payload inválido"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "{idTutor} difere do tutor autenticado")
    })
    public ResponseEntity<ConsentimentoResponse> registrar(
            @Parameter(description = "ID do tutor", example = "1") @PathVariable Long idTutor,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest,
            Authentication auth) {

        AuditoriaSessao sessao = AuditoriaSessao.from(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentimentoService.registrar(idTutor, request, sessao.ipCliente(), auth.getName()));
    }
}
