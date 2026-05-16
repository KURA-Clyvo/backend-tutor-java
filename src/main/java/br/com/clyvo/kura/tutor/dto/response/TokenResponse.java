package br.com.clyvo.kura.tutor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de saída após autenticação bem-sucedida.
 * O {@code accessToken} deve ser enviado em todas as requisições subsequentes
 * no header {@code Authorization: Bearer <accessToken>}.
 */
@Schema(description = "Token JWT retornado após login bem-sucedido")
public record TokenResponse(

        @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Tipo do token — sempre 'Bearer'", example = "Bearer")
        String tokenType,

        @Schema(description = "Expiração em milissegundos", example = "86400000")
        long expiresIn,

        @Schema(description = "ID da conta do tutor autenticado", example = "1")
        Long idConta,

        @Schema(description = "ID do tutor (prontuário)", example = "1")
        Long idTutor,

        @Schema(description = "Nome do tutor para exibição no app", example = "Felipe Ferrete")
        String nmTutor
) {
    /** Factory method — cria o response já com tokenType padrão. */
    public static TokenResponse of(String token, long expiresIn,
                                   Long idConta, Long idTutor, String nmTutor) {
        return new TokenResponse(token, "Bearer", expiresIn, idConta, idTutor, nmTutor);
    }
}
