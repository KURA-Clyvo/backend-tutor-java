package br.com.clyvo.kura.tutor.shared.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
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
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aplica o schema e os índices de uma coleção a partir de arquivos versionados em
 * {@code src/main/resources/mongo/} (catálogo da Loja, documentos do tutor).
 *
 * <p>Idempotente: coleção inexistente => createCollection com o validator; existente => collMod
 * (substitui o validator inteiro e reaplica validationLevel/validationAction, desfazendo qualquer
 * afrouxamento feito por fora).
 *
 * <p>E61: índice que o servidor recusa por divergência (85 {@code IndexOptionsConflict} ou 86
 * {@code IndexKeySpecsConflict}) não é mais engolido. O índice divergente é removido pelo NOME e
 * recriado como o arquivo manda, com WARN nominal; se o drop falhar, ERROR nominal. Não usa
 * {@code listIndexes} (a role de runtime não tem esse privilégio): o nome do índice conflitante
 * vem do próprio erro do servidor. Os logs trazem só o nome do índice e o código, nunca URI.
 *
 * <p>Falhas de conexão propagam daqui; quem decide capturá-las é o runner.
 */
@Slf4j
public class AplicadorSchemaColecao {

    private static final int INDEX_OPTIONS_CONFLICT = 85;
    private static final int INDEX_KEY_SPECS_CONFLICT = 86;
    private static final Pattern NOME_CONFLITANTE = Pattern.compile("different name: (\\S+)");

    private final MongoDatabase database;
    private final String colecao;
    private final String arquivoSchema;
    private final String arquivoIndices;
    private final String rotulo;

    public AplicadorSchemaColecao(MongoDatabase database, String colecao, String arquivoSchema,
                                  String arquivoIndices, String rotulo) {
        this.database = database;
        this.colecao = colecao;
        this.arquivoSchema = arquivoSchema;
        this.arquivoIndices = arquivoIndices;
        this.rotulo = rotulo;
    }

    public void aplicar() {
        BsonDocument config = BsonDocument.parse(RecursoClasspath.lerTexto(arquivoSchema));
        BsonDocument validator = config.getDocument("validator");
        String nivel = config.getString("validationLevel").getValue();
        String acao = config.getString("validationAction").getValue();

        List<String> existentes = database.listCollectionNames().into(new ArrayList<>());
        if (!existentes.contains(colecao)) {
            database.createCollection(colecao, new CreateCollectionOptions().validationOptions(
                    new ValidationOptions()
                            .validator(validator)
                            .validationLevel(ValidationLevel.fromString(nivel))
                            .validationAction(ValidationAction.fromString(acao))));
            log.info("{}: coleção {} criada com validator", rotulo, colecao);
        } else {
            database.runCommand(new BsonDocument("collMod", new BsonString(colecao))
                    .append("validator", validator)
                    .append("validationLevel", new BsonString(nivel))
                    .append("validationAction", new BsonString(acao)));
            log.info("{}: validator de {} reaplicado (collMod)", rotulo, colecao);
        }

        BsonArray indices = BsonDocument.parse("{\"i\":" + RecursoClasspath.lerTexto(arquivoIndices) + "}").getArray("i");
        MongoCollection<Document> alvo = database.getCollection(colecao);
        for (BsonValue valor : indices) {
            BsonDocument indice = valor.asDocument();
            aplicarIndice(alvo, indice.getString("name").getValue(), indice.getDocument("key"),
                    indice.getBoolean("unique").getValue());
        }
    }

    private void aplicarIndice(MongoCollection<Document> alvo, String nome, BsonDocument chave, boolean unico) {
        try {
            criar(alvo, nome, chave, unico);
        } catch (MongoCommandException e) {
            if (!divergencia(e)) {
                log.warn("{}: índice {} não aplicado (código {})", rotulo, nome, e.getErrorCode());
                return;
            }
            log.warn("{}: índice {} divergente do arquivo (código {}); removendo e recriando",
                    rotulo, nome, e.getErrorCode());
            recriar(alvo, nome, chave, unico, e);
        }
    }

    /**
     * Código 85 pode ser "mesma chave, OUTRO nome": nesse caso o índice a remover é o que o
     * servidor cita. Até duas remoções (o do mesmo nome e o de mesma chave podem coexistir).
     */
    private void recriar(MongoCollection<Document> alvo, String nome, BsonDocument chave, boolean unico,
                         MongoCommandException conflito) {
        MongoCommandException atual = conflito;
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            String outroNome = nomeConflitante(atual);
            if (!remover(alvo, outroNome != null ? outroNome : nome)) {
                return;
            }
            try {
                criar(alvo, nome, chave, unico);
                return;
            } catch (MongoCommandException e) {
                if (!divergencia(e)) {
                    break;
                }
                atual = e;
            }
        }
        log.error("{}: índice {} não recriado após o drop", rotulo, nome);
    }

    private boolean remover(MongoCollection<Document> alvo, String nome) {
        try {
            alvo.dropIndex(nome);
            log.warn("{}: índice {} removido para recriar conforme o arquivo", rotulo, nome);
            return true;
        } catch (MongoException e) {
            log.error("{}: índice {} divergente e NÃO removido ({}, código {}); o índice do arquivo não foi aplicado",
                    rotulo, nome, e.getClass().getSimpleName(), e.getCode());
            return false;
        }
    }

    private static void criar(MongoCollection<Document> alvo, String nome, BsonDocument chave, boolean unico) {
        alvo.createIndex(chave, new IndexOptions().name(nome).unique(unico));
    }

    private static boolean divergencia(MongoCommandException e) {
        return e.getErrorCode() == INDEX_OPTIONS_CONFLICT || e.getErrorCode() == INDEX_KEY_SPECS_CONFLICT;
    }

    private static String nomeConflitante(MongoCommandException e) {
        String msg = e.getErrorMessage();
        if (msg == null) {
            return null;
        }
        Matcher m = NOME_CONFLITANTE.matcher(msg);
        return m.find() ? m.group(1) : null;
    }
}
