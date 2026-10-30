// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Instant;
import java.util.Map;

/** Carrinho como está no banco: só slug e quantidade (sem preço), mais a versão ({@code atualizadoEm}). */
public record CarrinhoSalvo(Map<String, Integer> itens, Instant atualizadoEm) {
}
