// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.loja.application.FatiaPedidos;
import br.com.clyvo.kura.tutor.loja.application.LojaAviso;
import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import br.com.clyvo.kura.tutor.loja.application.PedidoLojaService;
import br.com.clyvo.kura.tutor.loja.application.ResultadoPedido;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.PedidoLojaResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.PedidosResponse;
import br.com.clyvo.kura.tutor.shared.security.TutorAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BFF dos pedidos da Loja, autenticado. idTutor SEMPRE do JWT. O id do pedido entra como String
 * (e não ObjectId) para que id malformado seja 404 idêntico ao de pedido alheio, nunca 400.
 */
@RestController
@RequestMapping("/v1/loja/pedidos")
@Validated
@Tag(name = "Loja BFF", description = "Pedidos da Loja (MongoDB) — pagamento e entrega simulados, nenhuma cobrança")
@SecurityRequirement(name = "bearerAuth")
public class LojaPedidosController {

    private final PedidoLojaService pedidoService;
    private final TutorAutenticado tutorAutenticado;

    public LojaPedidosController(PedidoLojaService pedidoService, TutorAutenticado tutorAutenticado) {
        this.pedidoService = pedidoService;
        this.tutorAutenticado = tutorAutenticado;
    }

    @PostMapping
    @Operation(summary = "Finaliza o carrinho em um pedido (idempotente)",
            description = "Header Idempotency-Key obrigatório (UUID). Regra para o app: 1 UUID v4 por toque em 'Finalizar'; "
                    + "reusar a mesma chave só no retry de falha de rede ou de 503; descartar depois de qualquer 2xx; "
                    + "depois de 422, gerar uma nova. O app confere status e id na resposta. "
                    + "201 = criado; 200 = a mesma chave de volta (devolve o pedido original, mesmo com carrinho diferente). "
                    + "O carrinho só é apagado se ninguém mexeu nele depois do pedido. "
                    + "Resíduos declarados (pré-lançamento, sem cobrança; o tutor cancela o duplicado): duas chaves diferentes "
                    + "ao mesmo tempo sobre o mesmo carrinho podem gerar dois pedidos, e se o último passo falhar e o app "
                    + "não repetir a chave, um novo 'Finalizar' com chave nova cria um segundo pedido idêntico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado"),
        @ApiResponse(responseCode = "200", description = "Chave já usada: pedido original"),
        @ApiResponse(responseCode = "400", description = "HEADER_AUSENTE ou IDEMPOTENCY_KEY_INVALIDA"),
        @ApiResponse(responseCode = "422", description = "CARRINHO_VAZIO ou ITEM_INDISPONIVEL (slugs em detalhes); nada é gravado"),
        @ApiResponse(responseCode = "503", description = "CATALOGO_INDISPONIVEL")
    })
    public ResponseEntity<PedidoLojaResponse> criar(
            Authentication auth, HttpServletRequest request,
            @RequestHeader("Idempotency-Key") String chave) {
        ResultadoPedido resultado = pedidoService.criar(tutorAutenticado.idTutor(auth), chave);
        PedidoLojaResponse corpo = PedidoLojaResponse.de(resultado.pedido(), LojaAviso.TEXTO);
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK)
                .header(HttpHeaders.LOCATION, request.getContextPath() + "/v1/loja/pedidos/" + corpo.id())
                .body(corpo);
    }

    @GetMapping
    @Operation(summary = "Meus pedidos",
            description = "Do mais novo ao mais antigo. Envelope Slice igual ao do catálogo; o aviso vai na raiz.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedidos retornados"),
        @ApiResponse(responseCode = "400", description = "page ou size inválidos")
    })
    public ResponseEntity<PedidosResponse> listar(
            Authentication auth,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        FatiaPedidos fatia = pedidoService.listar(tutorAutenticado.idTutor(auth), page, size);
        return ResponseEntity.ok(new PedidosResponse(LojaAviso.TEXTO,
                fatia.pedidos().stream().map(p -> PedidoLojaResponse.de(p, null)).toList(),
                page, size, fatia.temProxima()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do pedido",
            description = "Id malformado ou de outro tutor: 404 PEDIDO_NAO_ENCONTRADO, corpo idêntico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido retornado"),
        @ApiResponse(responseCode = "404", description = "PEDIDO_NAO_ENCONTRADO")
    })
    public ResponseEntity<PedidoLojaResponse> detalhar(Authentication auth, @PathVariable String id) {
        PedidoLoja pedido = pedidoService.detalhar(tutorAutenticado.idTutor(auth), id);
        return ResponseEntity.ok(PedidoLojaResponse.de(pedido, LojaAviso.TEXTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela o pedido",
            description = "Cancelamento lógico: o pedido continua consultável com status CANCELADO. Idempotente: "
                    + "cancelar um pedido já cancelado devolve 200.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
        @ApiResponse(responseCode = "404", description = "PEDIDO_NAO_ENCONTRADO")
    })
    public ResponseEntity<PedidoLojaResponse> cancelar(Authentication auth, @PathVariable String id) {
        PedidoLoja pedido = pedidoService.cancelar(tutorAutenticado.idTutor(auth), id);
        return ResponseEntity.ok(PedidoLojaResponse.de(pedido, LojaAviso.TEXTO));
    }
}
