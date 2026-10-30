// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/** Item do pedido: snapshot de nome, categoria e preço do momento do pedido. */
public record ItemPedido(String slug, String nome, CategoriaLoja categoria, long precoUnitario, int quantidade,
                         boolean exemplo) {

    public long subtotal() {
        return precoUnitario * quantidade;
    }
}
