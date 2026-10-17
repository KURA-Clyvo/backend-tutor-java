// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.ItemCatalogo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Item do catálogo da Loja. Sem _id, ordem, ativo nem datas; nenhum campo de pagamento.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemCatalogoResponse(
        @Schema(example = "alimento-seco-adulto-caes-1kg") String slug,
        String nome,
        @Schema(description = "Omitida se ausente") String descricao,
        @Schema(example = "ALIMENTACAO") String categoria,
        @Schema(description = "Preço de referência em centavos (inteiro)", example = "4990") long preco,
        @Schema(example = "BRL") String moeda,
        @Schema(description = "true = item de demonstração (selo Exemplo no app)") boolean exemplo,
        @Schema(description = "Atributos por categoria") Map<String, Object> dados) {

    public static ItemCatalogoResponse de(ItemCatalogo item) {
        return new ItemCatalogoResponse(item.slug(), item.nome(), item.descricao(),
                item.categoria().name(), item.preco(), item.moeda(), item.exemplo(), item.dados());
    }
}
