package br.com.clyvo.kura.tutor.exception;

/**
 * Lançada quando uma regra de negócio é violada (422 Unprocessable Entity).
 * Exemplos: tutor tentando revogar consentimento inexistente,
 *           e-mail já cadastrado, conta bloqueada.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String message) {
        super(message);
    }
}
