package br.com.clyvo.kura.tutor.shared.mongo;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * Esqueleto dos runners de Mongo: o Mongo fora do ar NÃO pode impedir o kura-tutor de subir.
 * Toda falha é capturada e vira 1 WARN sem URI nem credencial (só o nome da classe da exceção).
 */
@Slf4j
public abstract class RunnerMongoTolerante implements ApplicationRunner {

    private final MongoDatabaseFactory factory;

    protected RunnerMongoTolerante(MongoDatabaseFactory factory) {
        this.factory = factory;
    }

    /** Trabalho do runner; falhas propagam para o esqueleto. */
    protected abstract void executar(MongoDatabase database);

    /** Mensagem do WARN, com um único {} para o nome da classe da exceção. */
    protected abstract String mensagemDeFalha();

    @Override
    public void run(ApplicationArguments args) {
        try {
            executar(factory.getMongoDatabase());
        } catch (Exception e) {
            log.warn(mensagemDeFalha(), e.getClass().getSimpleName());
        }
    }
}
