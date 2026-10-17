// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/**
 * Aviso de pré-lançamento da Loja (D-S4-6). Constante única, reusada por catálogo e,
 * depois, por carrinho/pedido — o texto literal vale byte a byte.
 */
public final class LojaAviso {

    public static final String TEXTO =
            "Pré-lançamento: pagamento e entrega serão ativados quando os parceiros logísticos "
            + "concluírem a integração. Nenhuma cobrança é feita.";

    private LojaAviso() {
    }
}
