// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.LojaCarrinho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Definir a quantidade de um produto: valor absoluto, idempotente.")
public record QuantidadeRequest(@NotNull @Min(1) @Max(LojaCarrinho.MAX_QUANTIDADE) Integer quantidade) {
}
