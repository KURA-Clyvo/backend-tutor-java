package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.dto.response.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.lgpd.AuditoriaSessao;
import br.com.clyvo.kura.tutor.lgpd.RelatorioLgpdService;
import br.com.clyvo.kura.tutor.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.service.ConsentimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller dedicado aos direitos do titular de dados (LGPD art. 18).
 *
 * Endpoints implementados:
 * - GET  /lgpd/relatorio          → Direito de acesso (art. 18, I)
 * - GET  /lgpd/consentimentos     → Historico de consentimentos (art. 18, IV)
 * - POST /lgpd/consentimentos     → Aceite ou revogacao (art. 18, IX)
 * - GET  /lgpd/consentimentos/{tipo}/ativo → Estado atual
 */
@RestController
@RequestMapping("/tutores/{idTutor}/lgpd")
@Tag(name = "LGPD — Direitos do Titular",
     description = "Endpoints de acesso, portabilidade e revogacao de dados pessoais (LGPD art. 18)")
@SecurityRequirement(name = "bearerAuth")
public class LgpdController {

    private final RelatorioLgpdService relatorioLgpdService;
    private final ConsentimentoService consentimentoService;

    public LgpdController(RelatorioLgpdService relatorioLgpdService,
                          ConsentimentoService consentimentoService) {
        this.relatorioLgpdService = relatorioLgpdService;
        this.consentimentoService = consentimentoService;
    }

    // ── Direito de acesso (art. 18, I) ───────────────────────────────────────

    @GetMapping("/relatorio")
    @Operation(
        summary = "Relatorio completo de dados pessoais",
        description = """
            Retorna todos os dados pessoais que o sistema armazena sobre o tutor.
            Implementa o direito de acesso (art. 18, I) e portabilidade (art. 18, V) da LGPD.
            CPF e IP sao mascarados na resposta por seguranca.
            """
    )
    public ResponseEntity<Map<String, Object>> relatorio(@PathVariable Long idTutor) {
        return ResponseEntity.ok(relatorioLgpdService.gerarRelatorio(idTutor));
    }

    // ── Historico de consentimentos (art. 18, IV) ────────────────────────────

    @GetMapping("/consentimentos")
    @Operation(
        summary = "Historico completo de consentimentos",
        description = "Lista todos os registros de aceite e revogacao, mais recente primeiro. " +
                      "Evidencia de auditoria para a ANPD."
    )
    public ResponseEntity<List<ConsentimentoResponse>> listarConsentimentos(
            @PathVariable Long idTutor) {
        return ResponseEntity.ok(consentimentoService.listar(idTutor));
    }

    @GetMapping("/consentimentos/{tipo}/ativo")
    @Operation(
        summary = "Estado atual de um tipo de consentimento",
        description = "Tipos: TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS, " +
                      "COMPARTILHAR_SEGURADORA, MARKETING"
    )
    public ResponseEntity<ConsentimentoResponse> estadoAtual(
            @PathVariable Long idTutor,
            @PathVariable String tipo) {
        return consentimentoService.buscarAtivo(idTutor, tipo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Aceite ou revogacao (art. 18, IX) ────────────────────────────────────

    @PostMapping("/consentimentos")
    @Operation(
        summary = "Registra aceite ou revogacao de consentimento",
        description = """
            LGPD: cada chamada = novo INSERT imutavel no historico.
            O estado atual e sempre o registro mais recente por tipo.

            Para aceitar: aceito = "S"
            Para revogar: aceito = "N" (deve existir aceite ativo anterior)

            Header Idempotency-Key recomendado para prevenir duplicatas em retry.
            O IP do cliente e registrado como evidencia legal.
            """
    )
    public ResponseEntity<ConsentimentoResponse> registrar(
            @PathVariable Long idTutor,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest) {

        AuditoriaSessao sessao = AuditoriaSessao.from(httpRequest);

        ConsentimentoResponse response =
                consentimentoService.registrar(idTutor, request, sessao.ipCliente());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
