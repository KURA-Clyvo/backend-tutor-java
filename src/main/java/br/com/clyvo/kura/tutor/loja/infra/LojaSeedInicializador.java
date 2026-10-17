// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Component;

/** Dispara o seed de exemplo depois do runner de schema. Mesma tolerância a Mongo fora do ar. */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(name = "kura.loja.seed-exemplo.enabled", havingValue = "true")
public class LojaSeedInicializador extends LojaRunnerTolerante {

    public LojaSeedInicializador(MongoDatabaseFactory factory) {
        super(factory);
    }

    @Override
    void executar(MongoDatabase database) {
        int total = new SeedCatalogoExemplo(database).aplicar();
        log.info("Loja: seed de exemplo aplicado ({} itens, exemplo=true)", total);
    }

    @Override
    String mensagemDeFalha() {
        return "Loja: seed de exemplo não aplicado nesta subida ({})";
    }
}
