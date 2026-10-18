// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeedCatalogoExemploTest {

    @Test
    @DisplayName("seed tem 10 itens, todos exemplo=true, ativos, slugs únicos, sem medicamento")
    void arquivoDoSeed() {
        List<Document> itens = SeedCatalogoExemplo.lerItens();
        assertThat(itens).hasSize(10);
        assertThat(itens).allSatisfy(i -> {
            assertThat(i.getBoolean("exemplo")).isTrue();
            assertThat(i.getBoolean("ativo")).isTrue();
        });
        Set<String> slugs = new HashSet<>();
        itens.forEach(i -> slugs.add(i.getString("slug")));
        assertThat(slugs).hasSize(10);
        assertThat(itens.stream().map(i -> i.getString("categoria")).distinct())
                .containsExactlyInAnyOrder("ALIMENTACAO", "HIGIENE", "ACESSORIOS", "BRINQUEDOS");
    }

    @Test
    @DisplayName("todo campo do seed está declarado nas propriedades da raiz do schema (additionalProperties=false)")
    void camposDoSeedExistemNoSchema() throws Exception {
        String json = new String(new ClassPathResource("mongo/catalogo_loja.schema.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        BsonDocument propriedades = BsonDocument.parse(json).getDocument("validator")
                .getDocument("$jsonSchema").getDocument("properties");
        for (Document item : SeedCatalogoExemplo.lerItens()) {
            assertThat(propriedades.keySet()).containsAll(item.keySet());
        }
    }

    @Test
    @DisplayName("NX-04: todo item do seed tem imagem {chave==slug, alt} e os dois arquivos (grande e _256) existem no classpath")
    void todoItemTemImagemComArquivos() {
        for (Document item : SeedCatalogoExemplo.lerItens()) {
            Document imagem = item.get("imagem", Document.class);
            assertThat(imagem).as("imagem de %s", item.getString("slug")).isNotNull();
            assertThat(imagem.keySet()).containsExactlyInAnyOrder("chave", "alt");
            String chave = imagem.getString("chave");
            assertThat(chave).isEqualTo(item.getString("slug"));
            assertThat(imagem.getString("alt")).startsWith("Foto ilustrativa:").hasSizeBetween(5, 160);
            assertThat(new ClassPathResource("static/loja/imagens/" + chave + ".webp").exists()).isTrue();
            assertThat(new ClassPathResource("static/loja/imagens/" + chave + "_256.webp").exists()).isTrue();
        }
    }

    @Test
    @DisplayName("NX-04: schema declara imagem opcional (fora do required), com chave e alt obrigatórios e sem campo extra")
    void schemaDeclaraImagemMalformadaRecusavel() throws Exception {
        String json = new String(new ClassPathResource("mongo/catalogo_loja.schema.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        BsonDocument raiz = BsonDocument.parse(json).getDocument("validator").getDocument("$jsonSchema");
        assertThat(raiz.getArray("required").stream().map(v -> v.asString().getValue())).doesNotContain("imagem");
        BsonDocument imagem = raiz.getDocument("properties").getDocument("imagem");
        assertThat(imagem.getArray("required").stream().map(v -> v.asString().getValue()))
                .containsExactlyInAnyOrder("chave", "alt");
        assertThat(imagem.getBoolean("additionalProperties").getValue()).isFalse();
        assertThat(imagem.getDocument("properties").getDocument("chave").getString("pattern").getValue())
                .isEqualTo("^[a-z0-9]+(-[a-z0-9]+)*\z");
    }

    @Test
    @DisplayName("aplicar: upsert por slug para cada item, com upsert=true e criadoEm só na inserção")
    void upsertPorSlug() {
        MongoDatabase database = mock(MongoDatabase.class);
        @SuppressWarnings("unchecked")
        MongoCollection<Document> colecao = mock(MongoCollection.class);
        when(database.getCollection("catalogo_loja")).thenReturn(colecao);

        int total = new SeedCatalogoExemplo(database).aplicar();

        assertThat(total).isEqualTo(10);
        ArgumentCaptor<Bson> filtros = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updates = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> opcoes = ArgumentCaptor.forClass(UpdateOptions.class);
        verify(colecao, times(10)).updateOne(filtros.capture(), updates.capture(), opcoes.capture());
        assertThat(opcoes.getAllValues()).allSatisfy(o -> assertThat(o.isUpsert()).isTrue());
        String primeiroFiltro = filtros.getAllValues().get(0).toBsonDocument().toJson();
        assertThat(primeiroFiltro).contains("alimento-seco-adulto-caes-1kg");
        BsonDocument upd = updates.getAllValues().get(0).toBsonDocument();
        assertThat(upd.containsKey("$setOnInsert")).isTrue();
        assertThat(upd.getDocument("$setOnInsert").containsKey("criadoEm")).isTrue();
        assertThat(upd.getDocument("$currentDate").containsKey("atualizadoEm")).isTrue();
        assertThat(upd.getDocument("$set").getBoolean("exemplo").getValue()).isTrue();
        assertThat(upd.getDocument("$set").containsKey("slug")).isFalse();
        // sem coleção nenhuma além de catalogo_loja
        verify(database, times(1)).getCollection(any());
    }

    @Test
    @DisplayName("m-1: ativo e ordem vão em $setOnInsert (edição editorial sobrevive ao boot); conteúdo segue em $set")
    void camposEditoriaisSoNaCriacao() {
        Document item = SeedCatalogoExemplo.lerItens().get(0);

        BsonDocument upd = SeedCatalogoExemplo.montarUpdate(item, new java.util.Date()).toBsonDocument();

        Set<String> setOnInsert = upd.getDocument("$setOnInsert").keySet();
        Set<String> set = upd.getDocument("$set").keySet();
        assertThat(setOnInsert).contains("ativo", "ordem", "criadoEm");
        assertThat(set).doesNotContain("ativo", "ordem", "criadoEm");
        assertThat(set).contains("nome", "preco", "categoria", "exemplo", "dados", "imagem");
    }
}
