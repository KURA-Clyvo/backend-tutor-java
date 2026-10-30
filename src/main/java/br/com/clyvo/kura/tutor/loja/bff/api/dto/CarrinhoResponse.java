// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Carrinho do tutor. Pré-lançamento: nenhuma cobrança.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarrinhoResponse(
        @Schema(description = "Aviso de pré-lançamento (texto literal D-S4-6)") String aviso,
        List<ItemCarrinhoResponse> itens,
        @Schema(description = "Soma dos subtotais dos itens disponíveis, em centavos") long total,
        @Schema(example = "BRL") String moeda,
        @Schema(description = "Produtos diferentes (HLEN): o badge do ícone") int quantidadeProdutos,
        @Schema(description = "Soma das quantidades") int quantidadeTotal,
        @Schema(description = "atualizadoEm + 2 h; omitido no carrinho vazio") Instant expiraEm) {
}
