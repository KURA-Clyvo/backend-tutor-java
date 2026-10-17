// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LojaMongoConfigTest {

    static class EntidadeDeTeste {
        String slug = "x";
    }

    private static MongoMappingContext contexto() {
        MongoMappingContext ctx = new MongoMappingContext();
        ctx.setInitialEntitySet(java.util.Set.of(EntidadeDeTeste.class));
        ctx.afterPropertiesSet();
        return ctx;
    }

    @Test
    @DisplayName("conversor da Loja NÃO grava _class (o schema tem additionalProperties=false)")
    void conversorDaLojaNaoGravaClass() {
        MongoDatabaseFactory fabrica = mock(MongoDatabaseFactory.class);
        when(fabrica.getExceptionTranslator()).thenReturn(new MongoExceptionTranslator());
        MappingMongoConverter conversor = new LojaMongoConfig().mappingMongoConverter(
                fabrica, contexto(), new MongoCustomConversions(List.of()));
        conversor.afterPropertiesSet();

        Document saida = new Document();
        conversor.write(new EntidadeDeTeste(), saida);

        assertThat(saida).containsEntry("slug", "x");
        assertThat(saida).doesNotContainKey("_class");
    }

    @Test
    @DisplayName("controle positivo: o conversor padrão do Spring Data GRAVA _class")
    void conversorPadraoGravaClass() {
        MappingMongoConverter padrao = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto());
        padrao.afterPropertiesSet();

        Document saida = new Document();
        padrao.write(new EntidadeDeTeste(), saida);

        assertThat(saida).containsKey("_class");
    }
}
