// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.LojaCarrinho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Adicionar ao carrinho: soma a quantidade ao que já existe.")
public record ItemCarrinhoRequest(
        @NotNull @Size(max = LojaCarrinho.SLUG_MAX) @Pattern(regexp = LojaCarrinho.SLUG_REGEX)
        @Schema(example = "shampoo-neutro-500ml") String slug,
        @NotNull @Min(1) @Max(LojaCarrinho.MAX_QUANTIDADE) Integer quantidade) {
}
