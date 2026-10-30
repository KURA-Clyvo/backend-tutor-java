// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Instant;
import java.util.List;

/** Pedido como o domínio o vê. {@code carrinhoVersao} e a chave de idempotência não saem na API. */
public record PedidoLoja(String id, StatusPedido status, List<ItemPedido> itens, long total, Instant carrinhoVersao,
                         Instant criadoEm, Instant canceladoEm) {
}
