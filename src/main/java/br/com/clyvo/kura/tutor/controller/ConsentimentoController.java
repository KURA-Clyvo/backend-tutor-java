package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.dto.response.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.service.ConsentimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    @Operation(summary = "Historico completo de consentimentos (mais recente primeiro)")
    public ResponseEntity<List<ConsentimentoResponse>> listar(@PathVariable Long idTutor) {
        return ResponseEntity.ok(consentimentoService.listar(idTutor));
    }

    @GetMapping("/{tipo}/ativo")
    @Operation(summary = "Estado atual de um tipo de consentimento",
               description = "Tipos validos: TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS, COMPARTILHAR_SEGURADORA, MARKETING")
    public ResponseEntity<ConsentimentoResponse> buscarAtivo(
            @PathVariable Long idTutor,
            @PathVariable String tipo) {
        return consentimentoService.buscarAtivo(idTutor, tipo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registra aceite ou revogacao de consentimento",
               description = "LGPD: cada chamada gera novo registro. Nunca atualiza. Header Idempotency-Key recomendado.")
    public ResponseEntity<ConsentimentoResponse> registrar(
            @PathVariable Long idTutor,
            @Parameter(description = "UUID unico por operacao — previne registro duplicado em retry")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest) {
        String ip = extrairIp(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentimentoService.registrar(idTutor, request, ip));
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
