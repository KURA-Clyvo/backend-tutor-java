// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.util.List;

/** Fatia de página (Slice): o conteúdo e se existe próxima, sem count extra. */
public record FatiaCatalogo(List<ItemCatalogo> itens, boolean temProxima) {
}
