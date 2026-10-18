// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.util.Map;

/** Item do catálogo como o domínio da Loja o enxerga. {@code preco} em centavos; {@code imagemChave}/{@code imagemAlt} nulos = item sem foto. */
public record ItemCatalogo(
        String slug,
        String nome,
        String descricao,
        CategoriaLoja categoria,
        long preco,
        String moeda,
        boolean exemplo,
        Map<String, Object> dados,
        String imagemChave,
        String imagemAlt) {
}
