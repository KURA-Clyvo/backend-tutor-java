// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/**
 * Porta de leitura do catálogo. O catálogo é global da plataforma (D-LOJA-12): não há
 * idTutor aqui. Repositórios de dado do tutor (carrinho, pedido) recebem idTutor na assinatura.
 */
import java.util.Collection;
import java.util.List;

public interface CatalogoRepositorio {

    /** Itens ativos, ordem editorial (ordem, slug); categoria nula = todas. */
    FatiaCatalogo listarAtivos(CategoriaLoja categoria, int page, int size);

    /** Itens ATIVOS entre os slugs pedidos, num só {@code find}; slug ausente do resultado = indisponível. */
    List<ItemCatalogo> buscarAtivosPorSlugs(Collection<String> slugs);
}
