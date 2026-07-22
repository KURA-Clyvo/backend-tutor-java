package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService.RegistroResult;
import br.com.clyvo.kura.tutor.consentimento.lgpd.AuditoriaSessao;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * BFF consentimentos — expõe /api/v1/tutor/consentimentos/** com idTutor do JWT.
 * DELETE não existe (consentimento é insert-only — ver INT-01).
 */
@RestController
@RequestMapping("/v1/tutor/consentimentos")
@Tag(name = "Consentimentos BFF", description = "Superfície /api/v1/tutor/consentimentos/** para mobile-tutor-rn")
@SecurityRequirement(name = "bearerAuth")
public class ConsentimentoBffController {

    private final ConsentimentoService consentimentoService;
    private final ContaTutorRepository contaTutorRepository;

    public ConsentimentoBffController(ConsentimentoService consentimentoService,
                                       ContaTutorRepository contaTutorRepository) {
        this.consentimentoService = consentimentoService;
        this.contaTutorRepository = contaTutorRepository;
    }

    @GetMapping
    @Operation(
        summary = "Estado atual de cada tipo de consentimento do tutor autenticado",
        description = "idTutor derivado do JWT. Retorna o último registro por tipo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de consentimentos vigentes"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<List<ConsentimentoResponse>> listar(Authentication auth) {
        Long idTutor = resolverIdTutor(auth);
        return ResponseEntity.ok(consentimentoService.listarUltimosPorTipo(idTutor, auth.getName()));
    }

    @PostMapping
    @Operation(
        summary = "Registra aceite ou revogação com idempotência obrigatória",
        description = "Header `Idempotency-Key` (UUID v4) obrigatório. idTutor derivado do JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consentimento registrado"),
        @ApiResponse(responseCode = "200", description = "Idempotência — registro original retornado"),
        @ApiResponse(responseCode = "400", description = "Payload inválido ou Idempotency-Key ausente"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<ConsentimentoResponse> registrar(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest,
            Authentication auth) {
        Long idTutor = resolverIdTutor(auth);
        AuditoriaSessao sessao = AuditoriaSessao.from(httpRequest);
        RegistroResult result = consentimentoService.registrarComIdempotencia(
                idTutor, request, sessao.ipCliente(), idempotencyKey, auth.getName());
        HttpStatus status = result.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Revogação por ID (stub)",
        description = "Consentimento é insert-only — DELETE não implementado. Pendente INT-01."
    )
    @ApiResponse(responseCode = "501", description = "Não implementado")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private Long resolverIdTutor(Authentication auth) {
        return contaTutorRepository
                .findIdTutorByEmail(auth.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ContaTutor", auth.getName()));
    }
}
