// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaCatalogoAplicadorTest {

    private final MongoDatabase database = mock(MongoDatabase.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> colecao = mock(MongoCollection.class);

    @SuppressWarnings("unchecked")
    private void colecoesExistentes(String... nomes) {
        MongoIterable<String> iterable = mock(MongoIterable.class);
        when(iterable.into(any())).thenAnswer(inv -> {
            List<String> destino = inv.getArgument(0);
            destino.addAll(List.of(nomes));
            return destino;
        });
        when(database.listCollectionNames()).thenReturn(iterable);
        when(database.getCollection("catalogo_loja")).thenReturn(colecao);
    }

    @Test
    @DisplayName("coleção inexistente: createCollection com o validator do arquivo, sem collMod")
    void colecaoInexistenteCriaComValidator() {
        colecoesExistentes();

        new SchemaCatalogoAplicador(database).aplicar();

        ArgumentCaptor<CreateCollectionOptions> opcoes = ArgumentCaptor.forClass(CreateCollectionOptions.class);
        verify(database).createCollection(eq("catalogo_loja"), opcoes.capture());
        verify(database, never()).runCommand(any(Bson.class));
        var validacao = opcoes.getValue().getValidationOptions();
        BsonDocument jsonSchema = ((BsonDocument) validacao.getValidator()).getDocument("$jsonSchema");
        assertThat(jsonSchema.getString("title").getValue()).isEqualTo("catalogo_loja");
        assertThat(jsonSchema.getBoolean("additionalProperties").getValue()).isFalse();
        assertThat(validacao.getValidationLevel().getValue()).isEqualTo("strict");
        assertThat(validacao.getValidationAction().getValue()).isEqualTo("error");
    }

    @Test
    @DisplayName("coleção existente: collMod reaplica validator, level e action; não recria")
    void colecaoExistenteFazCollMod() {
        colecoesExistentes("catalogo_loja", "outra");

        new SchemaCatalogoAplicador(database).aplicar();

        verify(database, never()).createCollection(any(String.class), any(CreateCollectionOptions.class));
        ArgumentCaptor<Bson> comando = ArgumentCaptor.forClass(Bson.class);
        verify(database).runCommand(comando.capture());
        BsonDocument doc = (BsonDocument) comando.getValue();
        assertThat(doc.getString("collMod")).isEqualTo(new BsonString("catalogo_loja"));
        assertThat(doc.getDocument("validator").containsKey("$jsonSchema")).isTrue();
        assertThat(doc.getString("validationLevel").getValue()).isEqualTo("strict");
        assertThat(doc.getString("validationAction").getValue()).isEqualTo("error");
    }

    @Test
    @DisplayName("índices criados = exatamente a lista do LOJA-01 (nome, chave, unique), por lista")
    void indicesIguaisAListaDoLoja01() {
        colecoesExistentes("catalogo_loja");

        new SchemaCatalogoAplicador(database).aplicar();

        ArgumentCaptor<Bson> chaves = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<IndexOptions> opcoes = ArgumentCaptor.forClass(IndexOptions.class);
        verify(colecao, times(3)).createIndex(chaves.capture(), opcoes.capture());

        List<String> obtido = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            obtido.add(opcoes.getAllValues().get(i).getName() + "|" + chaves.getAllValues().get(i)
                    + "|" + opcoes.getAllValues().get(i).isUnique());
        }
        BsonDocument k1 = new BsonDocument("ativo", new BsonInt32(1)).append("categoria", new BsonInt32(1))
                .append("ordem", new BsonInt32(1)).append("slug", new BsonInt32(1));
        BsonDocument k2 = new BsonDocument("ativo", new BsonInt32(1)).append("ordem", new BsonInt32(1))
                .append("slug", new BsonInt32(1));
        BsonDocument k3 = new BsonDocument("slug", new BsonInt32(1));
        assertThat(obtido).containsExactly(
                "ix_cat_ativo_categoria_ordem_slug|" + k1 + "|false",
                "ix_cat_ativo_ordem_slug|" + k2 + "|false",
                "uk_cat_slug|" + k3 + "|true");
    }

    @Test
    @DisplayName("índice recusado pelo servidor vira WARN daquele índice e os demais seguem")
    void indiceRecusadoNaoInterrompe() {
        colecoesExistentes("catalogo_loja");
        MongoCommandException recusa = new MongoCommandException(
                new BsonDocument("code", new BsonInt32(85)).append("errmsg", new BsonString("IndexOptionsConflict")),
                new ServerAddress("kura-mongo", 27017));
        when(colecao.createIndex(any(Bson.class), any(IndexOptions.class)))
                .thenThrow(recusa).thenReturn("ok").thenReturn("ok");

        new SchemaCatalogoAplicador(database).aplicar();

        verify(colecao, times(3)).createIndex(any(Bson.class), any(IndexOptions.class));
    }
}
