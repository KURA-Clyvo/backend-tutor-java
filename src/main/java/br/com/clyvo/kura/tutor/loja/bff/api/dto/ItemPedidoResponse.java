// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.ItemPedido;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item do pedido: snapshot de nome, categoria e preço do momento do pedido.")
public record ItemPedidoResponse(
        String slug,
        String nome,
        String categoria,
        @Schema(description = "Preço unitário em centavos") long precoUnitario,
        int quantidade,
        long subtotal,
        boolean exemplo) {

    public static ItemPedidoResponse de(ItemPedido i) {
        return new ItemPedidoResponse(i.slug(), i.nome(), i.categoria().name(), i.precoUnitario(), i.quantidade(),
                i.subtotal(), i.exemplo());
    }
}
