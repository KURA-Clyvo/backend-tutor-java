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
        @Schema(description = "Atributos por categoria") Map<String, Object> dados,
        @Schema(description = "Foto ilustrativa; omitida se o item não tem foto (o app usa o placeholder)")
        Imagem imagem) {

    /** Prefixo público das fotos: context-path {@code /api} + rota liberada em SecurityConfig ({@code /loja/imagens/**}). */
    static final String PREFIXO_IMAGENS = "/api/loja/imagens/";

    @Schema(description = "Caminhos relativos à base da API (o app junta a base); alt = texto alternativo")
    public record Imagem(
            @Schema(example = "/api/loja/imagens/shampoo-neutro-500ml.webp") String url,
            @Schema(example = "/api/loja/imagens/shampoo-neutro-500ml_256.webp") String urlThumb,
            String alt) {
    }

    public static ItemCatalogoResponse de(ItemCatalogo item) {
        Imagem imagem = item.imagemChave() == null ? null : new Imagem(
                PREFIXO_IMAGENS + item.imagemChave() + ".webp",
                PREFIXO_IMAGENS + item.imagemChave() + "_256.webp",
                item.imagemAlt());
        return new ItemCatalogoResponse(item.slug(), item.nome(), item.descricao(),
                item.categoria().name(), item.preco(), item.moeda(), item.exemplo(), item.dados(), imagem);
    }
}
