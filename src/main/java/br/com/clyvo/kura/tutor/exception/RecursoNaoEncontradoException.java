package br.com.clyvo.kura.tutor.exception;

import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;

/** Alias de compatibilidade — estende NotFoundException para unificar o handler HTTP 404. */
public class RecursoNaoEncontradoException extends NotFoundException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " com id '" + id + "' nao encontrado.");
    }
    public RecursoNaoEncontradoException(String msg) { super(msg); }
}
