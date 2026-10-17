// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Envelope do catálogo: aviso de pré-lançamento na raiz + fatia de página (Slice).")
public record CatalogoResponse(
        @Schema(description = "Aviso de pré-lançamento (texto literal D-S4-6)") String aviso,
        List<ItemCatalogoResponse> content,
        int page,
        int size,
        @Schema(description = "Existe próxima página") boolean hasNext) {
}
