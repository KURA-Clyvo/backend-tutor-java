package br.com.clyvo.kura.tutor.shared.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** E61 com o servidor simulado: o ramo "drop falhou" (role sem dropIndex) não se reproduz em mongod sem auth. */
@ExtendWith(OutputCaptureExtension.class)
class AplicadorSchemaColecaoTest {

    private final MongoDatabase database = mock(MongoDatabase.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> colecao = mock(MongoCollection.class);

    @SuppressWarnings("unchecked")
    private AplicadorSchemaColecao aplicador() {
        MongoIterable<String> iterable = mock(MongoIterable.class);
        when(iterable.into(any())).thenAnswer(inv -> {
            List<String> destino = inv.getArgument(0);
            destino.add("documentos_tutor");
            return destino;
        });
        when(database.listCollectionNames()).thenReturn(iterable);
        when(database.getCollection("documentos_tutor")).thenReturn(colecao);
        return new AplicadorSchemaColecao(database, "documentos_tutor", "mongo/documentos_tutor.schema.json",
                "mongo/documentos_tutor.indexes.json", "Documentos");
    }

    private static MongoCommandException erro(int codigo, String mensagem) {
        return new MongoCommandException(
                new BsonDocument("code", new BsonInt32(codigo)).append("errmsg", new BsonString(mensagem)),
                new ServerAddress("kura-mongo", 27017));
    }

    @Test
    @DisplayName("E61: se o drop falhar (sem privilégio), ERROR nominal e o índice do arquivo não é recriado")
    void dropFalhaViraErrorNominal(CapturedOutput saida) {
        AplicadorSchemaColecao aplicador = aplicador();
        when(colecao.createIndex(any(Bson.class), any(IndexOptions.class)))
                .thenThrow(erro(86, "mongodb://usuario:segredo@kura-mongo:27017 conflito"))
                .thenReturn("ok");
        doThrow(erro(13, "not authorized on kura_docs to execute command dropIndexes"))
                .when(colecao).dropIndex("ix_doc_tutor_pet_criado");

        aplicador.aplicar();

        assertThat(saida.getAll())
                .contains("Documentos: índice ix_doc_tutor_pet_criado divergente e NÃO removido")
                .contains("ERROR")
                .doesNotContain("segredo").doesNotContain("mongodb://");
        // o segundo índice do arquivo ainda é tentado; o primeiro não foi recriado
        verify(colecao, times(2)).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    @DisplayName("código que não é divergência (ex.: 13) segue como WARN genérico, sem drop")
    void outroCodigoNaoDropa(CapturedOutput saida) {
        AplicadorSchemaColecao aplicador = aplicador();
        when(colecao.createIndex(any(Bson.class), any(IndexOptions.class))).thenThrow(erro(13, "nao autorizado"));

        aplicador.aplicar();

        verify(colecao, never()).dropIndex(any(String.class));
        assertThat(saida.getAll()).contains("Documentos: índice ix_doc_tutor_pet_criado não aplicado (código 13)");
    }

    @Test
    @DisplayName("código 85 cita outro nome: o índice removido é o citado, não o do arquivo")
    void codigo85RemoveONomeCitado() {
        AplicadorSchemaColecao aplicador = aplicador();
        when(colecao.createIndex(any(Bson.class), any(IndexOptions.class)))
                .thenThrow(erro(85, "Index already exists with a different name: residuo_antigo"))
                .thenReturn("ok");

        aplicador.aplicar();

        verify(colecao).dropIndex("residuo_antigo");
        verify(colecao, never()).dropIndex("ix_doc_tutor_pet_criado");
    }
}
