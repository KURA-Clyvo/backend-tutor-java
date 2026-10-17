// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * Esqueleto dos runners da Loja: o Mongo fora do ar NÃO pode impedir o kura-tutor de subir.
 * Toda falha é capturada e vira 1 WARN sem URI nem credencial (só o nome da classe da exceção).
 */
@Slf4j
abstract class LojaRunnerTolerante implements ApplicationRunner {

    private final MongoDatabaseFactory factory;

    LojaRunnerTolerante(MongoDatabaseFactory factory) {
        this.factory = factory;
    }

    /** Trabalho do runner; falhas propagam para o esqueleto. */
    abstract void executar(MongoDatabase database);

    /** Mensagem do WARN, com um único {} para o nome da classe da exceção. */
    abstract String mensagemDeFalha();

    @Override
    public void run(ApplicationArguments args) {
        try {
            executar(factory.getMongoDatabase());
        } catch (Exception e) {
            log.warn(mensagemDeFalha(), e.getClass().getSimpleName());
        }
    }
}
