package br.com.clyvo.kura.tutor.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Par de tokens retornado após login bem-sucedido.
 * Alinhado com o shape do onboarding para consistência da API.
 */
@Schema(description = "Tokens JWT retornados após login")
public record TokenResponse(
        @Schema(example = "eyJhbGci...") String accessToken,
        @Schema(example = "eyJhbGci...") String refreshToken,
        @Schema(example = "Bearer")      String tokenType,
        @Schema(example = "900")         long   expiresIn,
        @Schema(example = "1")           Long   idConta,
        @Schema(example = "Felipe Ferretel") String nmTutor
) {
    public static TokenResponse of(String access, String refresh,
                                   long expSec, Long idConta, String nmTutor) {
        return new TokenResponse(access, refresh, "Bearer", expSec, idConta, nmTutor);
    }
}
