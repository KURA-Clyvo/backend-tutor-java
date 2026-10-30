package br.com.clyvo.kura.tutor.shared.mongo;

import org.bson.types.ObjectId;

import java.util.Optional;

/** Conversões de id compartilhadas pelos repositórios Mongo. */
public final class IdsMongo {

    private IdsMongo() {
    }

    /** Texto que não é um ObjectId válido (ou nulo) vira vazio: é "não existe", nunca erro. */
    public static Optional<ObjectId> objectId(String texto) {
        return texto != null && ObjectId.isValid(texto) ? Optional.of(new ObjectId(texto)) : Optional.empty();
    }
}
