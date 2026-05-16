package br.com.clyvo.kura.tutor.exception;

/**
 * Lançada quando um recurso não é encontrado no banco (404).
 * Exemplos: tutor com ID inexistente, agendamento não encontrado.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s com identificador '%s' não encontrado.".formatted(recurso, id));
    }

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
