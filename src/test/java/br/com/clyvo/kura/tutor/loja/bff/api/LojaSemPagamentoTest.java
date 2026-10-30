package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.ItemCatalogo;
import br.com.clyvo.kura.tutor.loja.application.ItemPedido;
import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import br.com.clyvo.kura.tutor.loja.application.StatusPedido;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.CarrinhoResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.ItemCarrinhoResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.PedidoLojaResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.PedidosResponse;
import br.com.clyvo.kura.tutor.shared.mongo.RecursoClasspath;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pagamento é simulado (D-S4-6): nenhum NOME DE CAMPO (do JSON do pedido, do carrinho e das
 * propriedades dos 2 schemas) sugere pagamento, cartão, Pix ou cobrança. A busca é por nome de
 * campo, não por texto (o aviso literal tem a palavra "pagamento"), e o padrão usa radicais
 * inteiros: o prefixo {@code pag} puro acusaria o campo {@code page} do envelope Slice.
 */
class LojaSemPagamentoTest {

    private static final Pattern PROIBIDO = Pattern.compile("pagament|pago|pagar|cart[aã]o|card|pix|cobran|cobrar", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    private static void campos(JsonNode no, List<String> acumulado) {
        if (no.isObject()) {
            no.fieldNames().forEachRemaining(acumulado::add);
            no.elements().forEachRemaining(filho -> campos(filho, acumulado));
        } else if (no.isArray()) {
            no.elements().forEachRemaining(filho -> campos(filho, acumulado));
        }
    }

    /** Só os nomes sob "properties" (nomes de campo do schema), não palavras-chave como "patternProperties". */
    private static void propriedadesDoSchema(JsonNode no, List<String> acumulado) {
        if (no.isObject()) {
            if (no.has("properties")) {
                no.get("properties").fieldNames().forEachRemaining(acumulado::add);
            }
            no.elements().forEachRemaining(filho -> propriedadesDoSchema(filho, acumulado));
        } else if (no.isArray()) {
            no.elements().forEachRemaining(filho -> propriedadesDoSchema(filho, acumulado));
        }
    }

    private static List<String> proibidos(List<String> nomes) {
        return nomes.stream().filter(n -> PROIBIDO.matcher(n).find()).toList();
    }

    @Test
    @DisplayName("nomes de campo do pedido, da lista, do carrinho e dos 2 schemas: 0 ocorrências de pag|cart|pix|cobr")
    void nenhumCampoDePagamento() throws Exception {
        PedidoLoja pedido = new PedidoLoja("6720f0c1a2b3c4d5e6f70812", StatusPedido.CANCELADO,
                List.of(new ItemPedido("shampoo-neutro-500ml", "Shampoo", CategoriaLoja.HIGIENE, 2990, 2, true)),
                5980, Instant.now(), Instant.now(), Instant.now());
        ItemCatalogo item = new ItemCatalogo("shampoo-neutro-500ml", "Shampoo", null, CategoriaLoja.HIGIENE, 2990,
                "BRL", true, Map.of(), "shampoo-neutro-500ml", "Foto ilustrativa");
        List<String> campos = new ArrayList<>();
        campos(mapper.valueToTree(PedidoLojaResponse.de(pedido, "aviso")), campos);
        campos(mapper.valueToTree(new PedidosResponse("aviso", List.of(PedidoLojaResponse.de(pedido, null)), 0, 20, false)), campos);
        campos(mapper.valueToTree(new CarrinhoResponse("aviso", List.of(ItemCarrinhoResponse.de("shampoo-neutro-500ml", 2, item),
                ItemCarrinhoResponse.de("indisponivel", 1, null)), 5980, "BRL", 2, 3, Instant.now())), campos);
        List<String> schemas = new ArrayList<>();
        for (String arquivo : new String[] {"mongo/pedidos_loja.schema.json", "mongo/carrinho.schema.json"}) {
            propriedadesDoSchema(mapper.readTree(RecursoClasspath.lerTexto(arquivo)), schemas);
        }

        assertThat(campos).contains("aviso", "status", "total", "quantidadeTotal", "page", "disponivel");
        assertThat(schemas).contains("status", "chaveIdempotencia", "itens", "carrinhoVersao");
        assertThat(proibidos(campos)).isEmpty();
        assertThat(proibidos(schemas)).isEmpty();
    }

    @Test
    @DisplayName("controle positivo: um campo 'pagamento' (ou 'cartao', 'pix', 'cobranca') injetado num JSON é achado")
    void controlePositivo() throws Exception {
        JsonNode injetado = mapper.readTree("{\"id\":\"x\",\"itens\":[{\"pagamento\":1}],\"cartao\":2,\"x\":{\"pixCopiaECola\":3,\"cobranca\":4}}");
        List<String> campos = new ArrayList<>();
        campos(injetado, campos);

        assertThat(proibidos(campos)).containsExactlyInAnyOrder("pagamento", "cartao", "pixCopiaECola", "cobranca");
        JsonNode schemaInjetado = mapper.readTree("{\"properties\":{\"formaDePagamento\":{}}}");
        List<String> props = new ArrayList<>();
        propriedadesDoSchema(schemaInjetado, props);
        assertThat(proibidos(props)).containsExactly("formaDePagamento");
    }
}
