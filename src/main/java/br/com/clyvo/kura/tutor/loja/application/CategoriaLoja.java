// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

/** Categorias do catálogo (D-LOJA-5). Nenhuma de medicamento, antiparasitário ou vacina. */
public enum CategoriaLoja {
    ALIMENTACAO,
    HIGIENE,
    ACESSORIOS,
    BRINQUEDOS;

    /** Conversão tolerante: valor ausente ou fora do enum vira vazio, sem lançar nem vazar nome de classe. */
    public static java.util.Optional<CategoriaLoja> tentar(String valor) {
        for (CategoriaLoja c : values()) {
            if (c.name().equals(valor)) {
                return java.util.Optional.of(c);
            }
        }
        return java.util.Optional.empty();
    }
}
