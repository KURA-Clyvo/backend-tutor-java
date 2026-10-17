// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.client.MongoDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Component;

/**
 * Runner idempotente do schema da Loja (D-LOJA-4), a cada subida. Desligado por padrão
 * ({@code kura.loja.mongo.init-enabled}); ligado só no compose. O Mongo fora do ar NÃO pode
 * impedir o kura-tutor de subir: toda falha é capturada, 1 WARN sem URI nem credencial (só o
 * nome da classe da exceção), e a reaplicação acontece na próxima subida.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "kura.loja.mongo.init-enabled", havingValue = "true")
public class LojaMongoInicializador extends LojaRunnerTolerante {

    public LojaMongoInicializador(MongoDatabaseFactory factory) {
        super(factory);
    }

    @Override
    void executar(MongoDatabase database) {
        new SchemaCatalogoAplicador(database).aplicar();
    }

    @Override
    String mensagemDeFalha() {
        return "Loja: schema do Mongo não aplicado nesta subida ({}); será reaplicado no próximo boot";
    }
}
