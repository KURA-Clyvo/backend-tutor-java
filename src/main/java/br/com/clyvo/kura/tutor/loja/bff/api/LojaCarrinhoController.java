// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.loja.application.CarrinhoService;
import br.com.clyvo.kura.tutor.loja.application.LojaCarrinho;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.CarrinhoResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.ItemCarrinhoRequest;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.QuantidadeRequest;
import br.com.clyvo.kura.tutor.shared.security.TutorAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BFF do carrinho da Loja, autenticado. idTutor SEMPRE do JWT; o preço nunca é guardado no carrinho. */
@RestController
@RequestMapping("/v1/loja/carrinho")
@Validated
@Tag(name = "Loja BFF", description = "Carrinho da Loja (MongoDB, TTL de 2 h) — pré-lançamento, sem pagamento")
@SecurityRequirement(name = "bearerAuth")
public class LojaCarrinhoController {

    private final CarrinhoService carrinhoService;
    private final TutorAutenticado tutorAutenticado;

    public LojaCarrinhoController(CarrinhoService carrinhoService, TutorAutenticado tutorAutenticado) {
        this.carrinhoService = carrinhoService;
        this.tutorAutenticado = tutorAutenticado;
    }

    @GetMapping
    @Operation(summary = "Vê o carrinho",
            description = "Sempre 200: carrinho ausente ou vencido (mais de 2 h sem mexer) vem vazio. "
                    + "Preço, nome e imagem são relidos do catálogo; item indisponível vem com disponivel=false e fora do total.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrinho retornado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "503", description = "CATALOGO_INDISPONIVEL (a Loja inteira é um serviço só)")
    })
    public ResponseEntity<CarrinhoResponse> ver(Authentication auth) {
        return ResponseEntity.ok(carrinhoService.ver(tutorAutenticado.idTutor(auth)));
    }

    @PostMapping("/itens")
    @Operation(summary = "Adiciona ao carrinho (soma)",
            description = "Soma a quantidade ao que já existe ($inc atômico). Até 99 unidades por produto e 50 produtos: "
                    + "acima disso, 422 LIMITE_CARRINHO e o carrinho fica como estava. Produto inexistente ou inativo: 404.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrinho atualizado"),
        @ApiResponse(responseCode = "400", description = "slug ou quantidade inválidos"),
        @ApiResponse(responseCode = "404", description = "PRODUTO_NAO_ENCONTRADO"),
        @ApiResponse(responseCode = "422", description = "LIMITE_CARRINHO")
    })
    public ResponseEntity<CarrinhoResponse> adicionar(Authentication auth, @Valid @RequestBody ItemCarrinhoRequest corpo) {
        return ResponseEntity.ok(carrinhoService.adicionar(
                tutorAutenticado.idTutor(auth), corpo.slug(), corpo.quantidade()));
    }

    @PutMapping("/itens/{slug}")
    @Operation(summary = "Define a quantidade de um produto",
            description = "Valor absoluto e idempotente (o seletor de quantidade do app). Produto inexistente ou inativo: 404.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrinho atualizado"),
        @ApiResponse(responseCode = "400", description = "slug ou quantidade inválidos"),
        @ApiResponse(responseCode = "404", description = "PRODUTO_NAO_ENCONTRADO"),
        @ApiResponse(responseCode = "422", description = "LIMITE_CARRINHO (51º produto)")
    })
    public ResponseEntity<CarrinhoResponse> definir(
            Authentication auth,
            @PathVariable @Size(max = LojaCarrinho.SLUG_MAX) @Pattern(regexp = LojaCarrinho.SLUG_REGEX) String slug,
            @Valid @RequestBody QuantidadeRequest corpo) {
        return ResponseEntity.ok(carrinhoService.definir(tutorAutenticado.idTutor(auth), slug, corpo.quantidade()));
    }

    @DeleteMapping("/itens/{slug}")
    @Operation(summary = "Remove um produto do carrinho",
            description = "Não consulta o catálogo (produto desativado precisa poder sair). Produto ausente: 200 com o carrinho atual.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrinho atualizado"),
        @ApiResponse(responseCode = "400", description = "slug inválido")
    })
    public ResponseEntity<CarrinhoResponse> remover(
            Authentication auth,
            @PathVariable @Size(max = LojaCarrinho.SLUG_MAX) @Pattern(regexp = LojaCarrinho.SLUG_REGEX) String slug) {
        return ResponseEntity.ok(carrinhoService.remover(tutorAutenticado.idTutor(auth), slug));
    }

    @DeleteMapping
    @Operation(summary = "Esvazia o carrinho")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Carrinho esvaziado")})
    public ResponseEntity<Void> esvaziar(Authentication auth) {
        carrinhoService.esvaziar(tutorAutenticado.idTutor(auth));
        return ResponseEntity.noContent().build();
    }
}
