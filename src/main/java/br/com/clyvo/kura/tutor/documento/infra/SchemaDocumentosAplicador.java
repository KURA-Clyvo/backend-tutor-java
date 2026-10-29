package br.com.clyvo.kura.tutor.documento.infra;

import br.com.clyvo.kura.tutor.shared.mongo.AplicadorSchemaColecao;
import com.mongodb.client.MongoDatabase;

/** Schema e índices de {@code documentos_tutor} (contrato v1), aplicados pelo aplicador compartilhado. */
public class SchemaDocumentosAplicador extends AplicadorSchemaColecao {

    public static final String COLECAO = "documentos_tutor";
    static final String ARQUIVO_SCHEMA = "mongo/documentos_tutor.schema.json";
    static final String ARQUIVO_INDICES = "mongo/documentos_tutor.indexes.json";

    public SchemaDocumentosAplicador(MongoDatabase database) {
        super(database, COLECAO, ARQUIVO_SCHEMA, ARQUIVO_INDICES, "Documentos");
    }
}
