// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/** O validator do banco recusou a escrita (código 121): mais de 99 unidades de um produto ou o 51º produto. */
public class LimiteCarrinhoException extends LojaException {

    public LimiteCarrinhoException() {
        super(422, "LIMITE_CARRINHO",
                "Limite do carrinho: até " + LojaCarrinho.MAX_QUANTIDADE + " unidades por produto e "
                        + LojaCarrinho.MAX_PRODUTOS + " produtos diferentes.", null);
    }
}
