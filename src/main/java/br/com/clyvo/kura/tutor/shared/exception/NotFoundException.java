package br.com.clyvo.kura.tutor.shared.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String recurso, Object id) {
        super(recurso + " com id '" + id + "' não encontrado.");
    }
    public NotFoundException(String msg) {
        super(msg);
    }
}
