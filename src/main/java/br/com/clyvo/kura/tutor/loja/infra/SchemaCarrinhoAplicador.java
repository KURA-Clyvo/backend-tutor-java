// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.shared.mongo.AplicadorSchemaColecao;
import com.mongodb.client.MongoDatabase;

/** Aplica o schema e o índice TTL de {@code carrinho} com o aplicador compartilhado. */
public class SchemaCarrinhoAplicador extends AplicadorSchemaColecao {

    public static final String COLECAO = "carrinho";
    static final String ARQUIVO_SCHEMA = "mongo/carrinho.schema.json";
    static final String ARQUIVO_INDICES = "mongo/carrinho.indexes.json";

    public SchemaCarrinhoAplicador(MongoDatabase database) {
        super(database, COLECAO, ARQUIVO_SCHEMA, ARQUIVO_INDICES, "Loja");
    }
}
