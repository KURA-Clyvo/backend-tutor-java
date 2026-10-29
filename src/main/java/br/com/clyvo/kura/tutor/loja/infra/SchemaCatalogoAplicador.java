// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.shared.mongo.AplicadorSchemaColecao;
import com.mongodb.client.MongoDatabase;

/**
 * Aplica o schema e os índices de {@code catalogo_loja} (D-LOJA-4) com o aplicador compartilhado
 * ({@link AplicadorSchemaColecao}); aqui só ficam os nomes dos arquivos versionados.
 */
public class SchemaCatalogoAplicador extends AplicadorSchemaColecao {

    public static final String COLECAO = "catalogo_loja";
    static final String ARQUIVO_SCHEMA = "mongo/catalogo_loja.schema.json";
    static final String ARQUIVO_INDICES = "mongo/catalogo_loja.indexes.json";

    public SchemaCatalogoAplicador(MongoDatabase database) {
        super(database, COLECAO, ARQUIVO_SCHEMA, ARQUIVO_INDICES, "Loja");
    }
}
