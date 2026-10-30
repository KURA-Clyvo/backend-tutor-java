// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/** Status do pedido. Pagamento e entrega são simulados (D-S4-6): nunca existe "pago". */
public enum StatusPedido {
    AGUARDANDO_PARCEIRO,
    CANCELADO
}
