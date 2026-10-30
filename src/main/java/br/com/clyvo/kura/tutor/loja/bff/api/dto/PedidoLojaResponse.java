// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api.dto;

import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Pedido da Loja. Não expõe idTutor, chave de idempotência nem versão do carrinho; "
        + "o id é opaco. Nenhum campo de pagamento.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PedidoLojaResponse(
        @Schema(description = "Aviso de pré-lançamento; só no pedido avulso, na lista vai na raiz do envelope") String aviso,
        @Schema(description = "Id opaco (hex)") String id,
        @Schema(example = "AGUARDANDO_PARCEIRO") String status,
        List<ItemPedidoResponse> itens,
        @Schema(description = "Total em centavos") long total,
        @Schema(example = "BRL") String moeda,
        Instant criadoEm,
        @Schema(description = "Omitido enquanto não cancelado") Instant canceladoEm) {

    /** {@code aviso} nulo é omitido (item da lista). */
    public static PedidoLojaResponse de(PedidoLoja p, String aviso) {
        return new PedidoLojaResponse(aviso, p.id(), p.status().name(),
                p.itens().stream().map(ItemPedidoResponse::de).toList(),
                p.total(), "BRL", p.criadoEm(), p.canceladoEm());
    }
}
