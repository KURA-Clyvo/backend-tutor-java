package br.com.clyvo.kura.tutor.shared.exception;

/** Conta desativada por um administrador — HTTP 403 Forbidden. */
public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() {
        super("Conta desativada. Contate o suporte.");
    }
}
