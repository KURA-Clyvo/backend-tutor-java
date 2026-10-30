// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Duration;

/**
 * Constantes do carrinho. {@code VALIDADE} é coerente com o {@code expireAfterSeconds} de
 * {@code carrinho.indexes.json} (travado por teste). Mudar o TTL depois não é nesta sprint:
 * exigiria {@code collMod} como root (a role de runtime não tem {@code dropIndex}).
 */
public final class LojaCarrinho {

    public static final Duration VALIDADE = Duration.ofHours(2);
    public static final int MAX_QUANTIDADE = 99;
    public static final int MAX_PRODUTOS = 50;
    /** Formato do slug, o mesmo do catálogo e do validator do carrinho. */
    public static final String SLUG_REGEX = "^[a-z0-9]+(-[a-z0-9]+)*\\z";
    /** Tamanho máximo do slug (o do schema do catálogo). */
    public static final int SLUG_MAX = 80;

    private LojaCarrinho() {
    }
}
