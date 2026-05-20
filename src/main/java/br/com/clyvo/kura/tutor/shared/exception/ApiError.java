package br.com.clyvo.kura.tutor.shared.exception;

import java.time.Instant;
import java.util.List;

/**
 * Envelope padronizado RFC 7807 para erros da API.
 *
 * Campos:
 *   timestamp — ISO-8601 UTC
 *   status    — HTTP status code
 *   codigo    — UPPER_SNAKE_CASE — código de erro para consumo programático
 *   mensagem  — mensagem PT-BR legível para o cliente
 *   path      — request URI
 *   detalhes  — erros campo-a-campo (Bean Validation); null em outros contextos
 */
public record ApiError(
        String timestamp,
        int status,
        String codigo,
        String mensagem,
        String path,
        List<String> detalhes
) {
    public static ApiError of(int status, String codigo, String mensagem, String path) {
        return new ApiError(Instant.now().toString(), status, codigo, mensagem, path, null);
    }

    public static ApiError of(int status, String codigo, String mensagem, String path,
                               List<String> detalhes) {
        return new ApiError(Instant.now().toString(), status, codigo, mensagem, path, detalhes);
    }

    /** Mantida para compatibilidade com JwtAuthenticationEntryPoint. */
    public static ApiError ofAuth(String mensagem, String path, String codigo) {
        return new ApiError(Instant.now().toString(), 401, codigo, mensagem, path, null);
    }
}
