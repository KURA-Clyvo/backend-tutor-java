// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.ItemCatalogo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item do carrinho com preço e nome relidos do catálogo. Item indisponível: disponivel=false, "
        + "nome = slug, sem preço, subtotal, categoria, exemplo nem imagem, e fora do total.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemCarrinhoResponse(
        @Schema(example = "shampoo-neutro-500ml") String slug,
        String nome,
        @Schema(example = "HIGIENE") String categoria,
        @Schema(description = "Preço unitário em centavos") Long precoUnitario,
        int quantidade,
        @Schema(description = "precoUnitario x quantidade, em centavos") Long subtotal,
        boolean disponivel,
        Boolean exemplo,
        ItemCatalogoResponse.Imagem imagem) {

    /** {@code item} nulo = produto que saiu do catálogo ou está inativo. */
    public static ItemCarrinhoResponse de(String slug, int quantidade, ItemCatalogo item) {
        if (item == null) {
            return new ItemCarrinhoResponse(slug, slug, null, null, quantidade, null, false, null, null);
        }
        ItemCatalogoResponse.Imagem imagem = item.imagemChave() == null
                ? null : ItemCatalogoResponse.Imagem.de(item.imagemChave(), item.imagemAlt());
        return new ItemCarrinhoResponse(slug, item.nome(), item.categoria().name(), item.preco(), quantidade,
                item.preco() * quantidade, true, item.exemplo(), imagem);
    }
}
