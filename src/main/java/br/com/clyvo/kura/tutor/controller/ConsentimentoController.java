package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.dto.response.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.service.impl.ConsentimentoService;
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

/**
 * Endpoints de gestão de consentimentos LGPD.
 *
 * <p>Regra de ouro: sempre INSERT, nunca UPDATE.
 * O histórico de consentimentos é imutável por exigência legal.
 */
@RestController
@RequestMapping("/tutores/{idTutor}/consentimentos")
@Tag(name = "3. Consentimentos LGPD", description = "Gestão de consentimentos do tutor — histórico imutável")
@SecurityRequirement(name = "bearerAuth")
public class ConsentimentoController {

    private final ConsentimentoService consentimentoService;

    public ConsentimentoController(ConsentimentoService consentimentoService) {
        this.consentimentoService = consentimentoService;
    }

    @GetMapping
    @Operation(
            summary = "Histórico completo de consentimentos",
            description = "Retorna todos os registros de aceite/revogação, do mais recente ao mais antigo."
    )
    public ResponseEntity<List<ConsentimentoResponse>> listar(
            @Parameter(description = "ID do tutor", example = "1")
            @PathVariable Long idTutor) {
        return ResponseEntity.ok(consentimentoService.listarPorTutor(idTutor));
    }

    @GetMapping("/ativo/{tipo}")
    @Operation(
            summary = "Status atual de um tipo de consentimento",
            description = """
                    Retorna o consentimento ativo mais recente para o tipo informado.
                    Tipos válidos: TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS,
                    COMPARTILHAR_SEGURADORA, MARKETING
                    """
    )
    public ResponseEntity<ConsentimentoResponse> buscarAtivo(
            @PathVariable Long idTutor,
            @Parameter(example = "LEMBRETES") @PathVariable String tipo) {
        return ResponseEntity.ok(consentimentoService.buscarAtivoPorTipo(idTutor, tipo));
    }

    @PostMapping
    @Operation(
            summary = "Registrar aceite ou revogação",
            description = """
                    Registra um aceite (ST_ACEITO=S) ou revogação (ST_ACEITO=N) de consentimento.
                    SEMPRE cria uma nova linha — não atualiza registros existentes.
                    O IP do tutor é capturado automaticamente como evidência legal (LGPD).
                    """
    )
    public ResponseEntity<ConsentimentoResponse> registrar(
            @PathVariable Long idTutor,
            @Valid @RequestBody ConsentimentoRequest request,
            HttpServletRequest httpRequest) {

        var response = consentimentoService.registrar(idTutor, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
