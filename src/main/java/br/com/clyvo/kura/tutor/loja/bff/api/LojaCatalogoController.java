// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.loja.application.CatalogoService;
import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.CatalogoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BFF da Loja — /api/v1/loja/catalogo, autenticado (D-LOJA-3). Catálogo global da plataforma. */
@RestController
@RequestMapping("/v1/loja/catalogo")
@Validated
@Tag(name = "Loja BFF", description = "Catálogo da Loja (MongoDB) para mobile-tutor-rn — pré-lançamento, sem pagamento")
@SecurityRequirement(name = "bearerAuth")
public class LojaCatalogoController {

    private final CatalogoService catalogoService;

    public LojaCatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    @Operation(
        summary = "Lista o catálogo da Loja",
        description = "Itens ativos em ordem editorial. Todo item traz exemplo=true enquanto for dado de demonstração. "
                + "A resposta traz o aviso de pré-lançamento. Mongo fora do ar: 503 CATALOGO_INDISPONIVEL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catálogo retornado"),
        @ApiResponse(responseCode = "400", description = "categoria, page ou size inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "503", description = "Banco de documentos indisponível")
    })
    public ResponseEntity<CatalogoResponse> listar(
            @RequestParam(required = false) CategoriaLoja categoria,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(catalogoService.listar(categoria, page, size));
    }
}
