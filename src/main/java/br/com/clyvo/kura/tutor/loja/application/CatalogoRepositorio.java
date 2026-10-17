// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/**
 * Porta de leitura do catálogo. O catálogo é global da plataforma (D-LOJA-12): não há
 * idTutor aqui. Repositórios de dado do tutor (carrinho, pedido) recebem idTutor na assinatura.
 */
public interface CatalogoRepositorio {

    /** Itens ativos, ordem editorial (ordem, slug); categoria nula = todas. */
    FatiaCatalogo listarAtivos(CategoriaLoja categoria, int page, int size);
}
