package br.com.clyvo.kura.tutor.shared.mongo;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;

/**
 * mongod 7.0.x real e embarcado (flapdoodle 4.20.0, API núcleo) para os testes da tag {@code mongo-real}.
 * Uma instância por JVM de teste; encerrada no shutdown hook.
 */
public final class MongodEmbarcado {

    /** Série 7.0 que a flapdoodle 4.20.0 oferece (a mesma da produção, 7.0.45). */
    public static final Version.Main VERSAO = Version.Main.V7_0;

    private static TransitionWalker.ReachedState<RunningMongodProcess> processo;

    private MongodEmbarcado() {
    }

    public static synchronized String uri() {
        if (processo == null) {
            processo = Mongod.instance()
                    .transitions(VERSAO)
                    .walker()
                    .initState(StateID.of(RunningMongodProcess.class));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> processo.close()));
        }
        return "mongodb://" + processo.current().getServerAddress().getHost() + ":"
                + processo.current().getServerAddress().getPort();
    }
}
