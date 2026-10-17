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
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Seed idempotente do catálogo (D-LOJA-5, D-LOJA-10): upsert por slug, só itens exemplo=true.
 * EXCEÇÃO EXPLÍCITA à regra "dado fictício nunca em prod" (D-LOJA-10): só catalogo_loja, só
 * itens de exemplo, atrás de kura.loja.seed-exemplo.enabled, ligado só no compose/demo.
 */
public class SeedCatalogoExemplo {

    static final String ARQUIVO_SEED = "mongo/catalogo_loja.seed.json";

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
            String slug = item.getString("slug");
            List<Bson> mudancas = new ArrayList<>();
            for (Map.Entry<String, Object> campo : item.entrySet()) {
                if (!"slug".equals(campo.getKey())) {
                    mudancas.add(Updates.set(campo.getKey(), campo.getValue()));
                }
            }
            mudancas.add(Updates.setOnInsert("criadoEm", agora));
            mudancas.add(Updates.currentDate("atualizadoEm"));
            colecao.updateOne(Filters.eq("slug", slug), Updates.combine(mudancas), new UpdateOptions().upsert(true));
            total++;
        }
        return total;
    }

    static List<Document> lerItens() {
        try {
            JsonNode raiz = new ObjectMapper().readTree(new ClassPathResource(ARQUIVO_SEED).getInputStream());
            List<Document> itens = new ArrayList<>();
            for (JsonNode no : raiz) {
                itens.add(Document.parse(no.toString()));
            }
            return itens;
        } catch (IOException e) {
            throw new UncheckedIOException("Seed da Loja ausente: " + ARQUIVO_SEED, e);
        }
    }
}
