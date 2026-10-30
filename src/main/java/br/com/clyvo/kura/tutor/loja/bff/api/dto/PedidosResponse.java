// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Envelope de Meus pedidos: aviso na raiz + fatia de página (Slice), do mais novo ao mais antigo.")
public record PedidosResponse(
        @Schema(description = "Aviso de pré-lançamento (texto literal D-S4-6)") String aviso,
        List<PedidoLojaResponse> content,
        int page,
        int size,
        @Schema(description = "Existe próxima página") boolean hasNext) {
}
