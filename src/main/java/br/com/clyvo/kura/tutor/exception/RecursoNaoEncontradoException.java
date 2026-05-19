package br.com.clyvo.kura.tutor.exception;
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " com id '" + id + "' nao encontrado.");
    }
    public RecursoNaoEncontradoException(String msg) { super(msg); }
}
