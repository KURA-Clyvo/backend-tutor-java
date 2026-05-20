package br.com.clyvo.kura.tutor.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Envelope de erro padrão RFC 7807")
public record ApiError(
        @Schema(description = "Timestamp UTC do erro", example = "2026-05-20T14:00:00Z") String timestamp,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "Código de erro UPPER_SNAKE_CASE", example = "VALIDACAO_INVALIDA") String codigo,
        @Schema(description = "Mensagem legível em PT-BR", example = "Campos inválidos na requisição.") String mensagem,
        @Schema(description = "URI da requisição que gerou o erro", example = "/api/agendamentos") String path,
        @Schema(description = "Erros campo-a-campo — presentes apenas em erros de validação (400)") List<String> detalhes
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
