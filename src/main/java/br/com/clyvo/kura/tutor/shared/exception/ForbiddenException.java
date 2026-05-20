package br.com.clyvo.kura.tutor.shared.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String mensagem) {
        super(mensagem);
    }
}
