// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Instant;
import java.util.List;

/** Dados do insert do pedido (passo 3); {@code total} já calculado em long. */
public record NovoPedido(String chaveIdempotencia, List<ItemPedido> itens, long total, Instant carrinhoVersao,
                         Instant agora) {
}
