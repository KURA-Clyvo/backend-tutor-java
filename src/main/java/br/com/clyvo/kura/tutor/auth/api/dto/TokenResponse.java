package br.com.clyvo.kura.tutor.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Par de tokens retornado após login bem-sucedido.
 * Alinhado com o shape do onboarding para consistência da API.
 */
@Schema(description = "Tokens JWT retornados após login")
public record TokenResponse(
        @Schema(description = "Access token JWT — validade 15 min", example = "eyJhbGci...") String accessToken,
        @Schema(description = "Refresh token — validade 7 dias, rotacionado a cada uso", example = "eyJhbGci...") String refreshToken,
        @Schema(description = "Tipo do token", example = "Bearer") String tokenType,
        @Schema(description = "Tempo de expiração do access token em segundos", example = "900") long expiresIn,
        @Schema(description = "ID da conta do tutor", example = "1") Long idConta,
        @Schema(description = "Nome do tutor", example = "Felipe Ferretel") String nmTutor
) {
    public static TokenResponse of(String access, String refresh,
                                   long expSec, Long idConta, String nmTutor) {
        return new TokenResponse(access, refresh, "Bearer", expSec, idConta, nmTutor);
    }
}
