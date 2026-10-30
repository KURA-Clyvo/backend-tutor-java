// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.util.List;

/**
 * Erro de regra da Loja (carrinho e pedidos). A mensagem é sempre texto fixo: nunca leva slug,
 * quantidade, chave nem idTutor (LGPD; o 121 e o 11000 do Mongo carregam o idTutor). O
 * {@code LojaExceptionHandler} traduz status e código para o envelope padrão.
 */
public class LojaException extends RuntimeException {

    private final int status;
    private final String codigo;
    private final List<String> detalhes;

    protected LojaException(int status, String codigo, String mensagem, List<String> detalhes) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.detalhes = detalhes;
    }

    public int status() {
        return status;
    }

    public String codigo() {
        return codigo;
    }

    /** Detalhes campo a campo; nulo quando não há. */
    public List<String> detalhes() {
        return detalhes;
    }

    public static LojaException produtoNaoEncontrado() {
        return new LojaException(404, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado no catálogo.", null);
    }

    /** Mesmo corpo para pedido inexistente, de outro tutor ou com id malformado: não revela que o pedido existe. */
    public static LojaException pedidoNaoEncontrado() {
        return new LojaException(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado.", null);
    }

    public static LojaException carrinhoVazio() {
        return new LojaException(422, "CARRINHO_VAZIO", "O carrinho está vazio ou expirou.", null);
    }

    public static LojaException itemIndisponivel(List<String> slugs) {
        return new LojaException(422, "ITEM_INDISPONIVEL",
                "Há itens indisponíveis no carrinho. Remova-os para finalizar.", slugs);
    }

    public static LojaException idempotencyKeyInvalida() {
        return new LojaException(400, "IDEMPOTENCY_KEY_INVALIDA", "O header Idempotency-Key deve ser um UUID.", null);
    }
}
