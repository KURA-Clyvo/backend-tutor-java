// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LojaMongoInicializadorTest {

    @Test
    @DisplayName("MongoTimeoutException ao listar coleções: runner não propaga e loga 1 WARN sem URI")
    void timeoutNoRunnerNaoPropaga(CapturedOutput saida) {
        MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(factory.getMongoDatabase()).thenReturn(database);
        when(database.listCollectionNames())
                .thenThrow(new MongoTimeoutException("mongodb://kura_java:segredo@kura-mongo:27017"));

        assertThatCode(() -> new LojaMongoInicializador(factory).run(null)).doesNotThrowAnyException();

        assertThat(saida.getAll()).contains("Loja: schema do Mongo não aplicado nesta subida (MongoTimeoutException)");
        assertThat(saida.getAll()).doesNotContain("segredo").doesNotContain("mongodb://");
    }

    @Test
    @DisplayName("falha ao obter o database (Mongo fora na raiz) também não propaga")
    void falhaAoObterDatabaseNaoPropaga() {
        MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
        when(factory.getMongoDatabase()).thenThrow(new MongoTimeoutException("x"));

        assertThatCode(() -> new LojaMongoInicializador(factory).run(null)).doesNotThrowAnyException();
        verify(factory).getMongoDatabase();
    }

    @Test
    @DisplayName("seed: Mongo fora não propaga")
    void seedComMongoForaNaoPropaga() {
        MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(factory.getMongoDatabase()).thenReturn(database);
        when(database.getCollection(any())).thenThrow(new MongoTimeoutException("x"));

        assertThatCode(() -> new LojaSeedInicializador(factory).run(null)).doesNotThrowAnyException();
    }
}
