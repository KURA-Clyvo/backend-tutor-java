// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seed idempotente do catálogo (D-LOJA-5, D-LOJA-10): upsert por slug, só itens exemplo=true.
 * EXCEÇÃO EXPLÍCITA à regra "dado fictício nunca em prod" (D-LOJA-10): só catalogo_loja, só
 * itens de exemplo, atrás de kura.loja.seed-exemplo.enabled, ligado só no compose/demo.
 */
public class SeedCatalogoExemplo {

    static final String ARQUIVO_SEED = "mongo/catalogo_loja.seed.json";

    private static final Set<String> CAMPOS_EDITORIAIS = Set.of("ativo", "ordem");

    private final MongoDatabase database;

    public SeedCatalogoExemplo(MongoDatabase database) {
        this.database = database;
    }

    /** @return quantidade de itens enviados ao upsert. */
    public int aplicar() {
        MongoCollection<Document> colecao = database.getCollection(SchemaCatalogoAplicador.COLECAO);
        Date agora = Date.from(Instant.now());
        int total = 0;
        for (Document item : lerItens()) {
            if (!Boolean.TRUE.equals(item.getBoolean("exemplo"))) {
                throw new IllegalStateException("Seed da Loja só admite itens exemplo=true: " + item.getString("slug"));
            }
            colecao.updateOne(Filters.eq("slug", item.getString("slug")), montarUpdate(item, agora),
                    new UpdateOptions().upsert(true));
            total++;
        }
        return total;
    }

    /**
     * Campos editoriais ({@code ativo}, {@code ordem}) só valem na criação: ajuste manual no Mongo
     * (ex.: {@code ativo:false}) sobrevive ao próximo boot. O conteúdo do exemplo segue em {@code $set}.
     */
    static Bson montarUpdate(Document item, Date agora) {
        List<Bson> mudancas = new ArrayList<>();
        for (Map.Entry<String, Object> campo : item.entrySet()) {
            String chave = campo.getKey();
            if ("slug".equals(chave)) {
                continue;
            }
            mudancas.add(CAMPOS_EDITORIAIS.contains(chave)
                    ? Updates.setOnInsert(chave, campo.getValue())
                    : Updates.set(chave, campo.getValue()));
        }
        mudancas.add(Updates.setOnInsert("criadoEm", agora));
        mudancas.add(Updates.currentDate("atualizadoEm"));
        return Updates.combine(mudancas);
    }

    static List<Document> lerItens() {
        try {
            JsonNode raiz = new ObjectMapper().readTree(RecursoClasspath.lerTexto(ARQUIVO_SEED));
            List<Document> itens = new ArrayList<>();
            for (JsonNode no : raiz) {
                itens.add(Document.parse(no.toString()));
            }
            return itens;
        } catch (IOException e) {
            throw new UncheckedIOException("Seed da Loja inválido: " + ARQUIVO_SEED, e);
        }
    }
}
