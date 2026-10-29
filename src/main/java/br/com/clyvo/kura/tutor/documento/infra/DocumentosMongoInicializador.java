package br.com.clyvo.kura.tutor.documento.infra;

import br.com.clyvo.kura.tutor.shared.mongo.RunnerMongoTolerante;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Component;

/**
 * Runner idempotente do schema de documentos_tutor, a cada subida. Desligado por padrão
 * ({@code kura.documentos.mongo.init-enabled}); ligado só no compose. Mongo fora do ar não
 * impede o kura-tutor de subir (esqueleto compartilhado: 1 WARN sem URI nem credencial).
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "kura.documentos.mongo.init-enabled", havingValue = "true")
public class DocumentosMongoInicializador extends RunnerMongoTolerante {

    public DocumentosMongoInicializador(MongoDatabaseFactory factory) {
        super(factory);
    }

    @Override
    protected void executar(MongoDatabase database) {
        new SchemaDocumentosAplicador(database).aplicar();
    }

    @Override
    protected String mensagemDeFalha() {
        return "Documentos: schema do Mongo não aplicado nesta subida ({}); será reaplicado no próximo boot";
    }
}
