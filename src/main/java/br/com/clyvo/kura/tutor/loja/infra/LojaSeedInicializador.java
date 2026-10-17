// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Component;

/** Dispara o seed de exemplo depois do runner de schema. Mesma tolerância a Mongo fora do ar. */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(name = "kura.loja.seed-exemplo.enabled", havingValue = "true")
public class LojaSeedInicializador implements ApplicationRunner {

    private final MongoDatabaseFactory factory;

    public LojaSeedInicializador(MongoDatabaseFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int total = new SeedCatalogoExemplo(factory.getMongoDatabase()).aplicar();
            log.info("Loja: seed de exemplo aplicado ({} itens, exemplo=true)", total);
        } catch (Exception e) {
            log.warn("Loja: seed de exemplo não aplicado nesta subida ({})", e.getClass().getSimpleName());
        }
    }
}
