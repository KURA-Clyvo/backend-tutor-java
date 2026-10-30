// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/** Pedido criado agora ({@code criado=true}, 201) ou devolvido pela repetição da mesma chave (200). */
public record ResultadoPedido(PedidoLoja pedido, boolean criado) {
}
