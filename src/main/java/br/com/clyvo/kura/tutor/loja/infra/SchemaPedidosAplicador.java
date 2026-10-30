// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.shared.mongo.AplicadorSchemaColecao;
import com.mongodb.client.MongoDatabase;

/** Aplica o schema e os índices de {@code pedidos_loja} com o aplicador compartilhado. */
public class SchemaPedidosAplicador extends AplicadorSchemaColecao {

    public static final String COLECAO = "pedidos_loja";
    static final String ARQUIVO_SCHEMA = "mongo/pedidos_loja.schema.json";
    static final String ARQUIVO_INDICES = "mongo/pedidos_loja.indexes.json";

    public SchemaPedidosAplicador(MongoDatabase database) {
        super(database, COLECAO, ARQUIVO_SCHEMA, ARQUIVO_INDICES, "Loja");
    }
}
