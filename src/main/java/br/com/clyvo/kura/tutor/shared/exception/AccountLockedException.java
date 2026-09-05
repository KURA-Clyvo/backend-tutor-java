package br.com.clyvo.kura.tutor.shared.exception;

/** Conta bloqueada por excesso de tentativas de login — HTTP 423 Locked. */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("Conta temporariamente bloqueada por excesso de tentativas. Tente novamente em alguns minutos.");
    }
}
