package br.com.clyvo.kura.tutor.shared.exception;

import java.time.Instant;

/**
 * Envelope padronizado para erros da API — subconjunto RFC 7807 Problem Details.
 *
 * Campos:
 *   timestamp  — ISO-8601 UTC do momento do erro
 *   status     — HTTP status code
 *   error      — nome canônico do status (ex: "Unauthorized")
 *   message    — mensagem legível para o cliente
 *   path       — request URI sem context-path
 *   codigo     — código máquina para auth errors (TOKEN_INVALIDO | TOKEN_AUSENTE); null nos demais
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String codigo
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path, null);
    }

    public static ApiError ofAuth(String message, String path, String codigo) {
        return new ApiError(Instant.now().toString(), 401, "Unauthorized", message, path, codigo);
    }
}
