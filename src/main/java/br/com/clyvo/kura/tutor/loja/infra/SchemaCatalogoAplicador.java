// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Aplica o schema e os índices de {@code catalogo_loja} (D-LOJA-4): arquivos versionados em
 * {@code src/main/resources/mongo/}. Idempotente: coleção inexistente => createCollection com o
 * validator; existente => collMod (substitui o validator inteiro, reaplicando validationLevel e
 * validationAction para desfazer qualquer afrouxamento feito por fora). Índice com opções
 * divergentes é recusado pelo servidor: vira WARN com o NOME do índice e os demais seguem.
 * Falhas de conexão propagam daqui; quem decide capturá-las é o runner.
 */
@Slf4j
public class SchemaCatalogoAplicador {

    public static final String COLECAO = "catalogo_loja";
    static final String ARQUIVO_SCHEMA = "mongo/catalogo_loja.schema.json";
    static final String ARQUIVO_INDICES = "mongo/catalogo_loja.indexes.json";

    private final MongoDatabase database;

    public SchemaCatalogoAplicador(MongoDatabase database) {
        this.database = database;
    }

    public void aplicar() {
        BsonDocument config = BsonDocument.parse(RecursoClasspath.lerTexto(ARQUIVO_SCHEMA));
        BsonDocument validator = config.getDocument("validator");
        String nivel = config.getString("validationLevel").getValue();
        String acao = config.getString("validationAction").getValue();

        List<String> existentes = database.listCollectionNames().into(new ArrayList<>());
        if (!existentes.contains(COLECAO)) {
            database.createCollection(COLECAO, new CreateCollectionOptions().validationOptions(
                    new ValidationOptions()
                            .validator(validator)
                            .validationLevel(ValidationLevel.fromString(nivel))
                            .validationAction(ValidationAction.fromString(acao))));
            log.info("Loja: coleção {} criada com validator", COLECAO);
        } else {
            database.runCommand(new BsonDocument("collMod", new BsonString(COLECAO))
                    .append("validator", validator)
                    .append("validationLevel", new BsonString(nivel))
                    .append("validationAction", new BsonString(acao)));
            log.info("Loja: validator de {} reaplicado (collMod)", COLECAO);
        }

        BsonArray indices = BsonDocument.parse("{\"i\":" + RecursoClasspath.lerTexto(ARQUIVO_INDICES) + "}").getArray("i");
        for (BsonValue valor : indices) {
            BsonDocument indice = valor.asDocument();
            String nome = indice.getString("name").getValue();
            boolean unico = indice.getBoolean("unique").getValue();
            try {
                database.getCollection(COLECAO).createIndex(indice.getDocument("key"),
                        new IndexOptions().name(nome).unique(unico));
            } catch (MongoCommandException e) {
                log.warn("Loja: índice {} não aplicado (código {})", nome, e.getErrorCode());
            }
        }
    }
}
