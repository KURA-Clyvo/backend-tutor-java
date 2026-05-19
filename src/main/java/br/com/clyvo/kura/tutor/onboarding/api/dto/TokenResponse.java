package br.com.clyvo.kura.tutor.onboarding.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens JWT retornados após cadastro por convite")
public record TokenResponse(
        @Schema(example = "eyJhbGci...") String accessToken,
        @Schema(example = "eyJhbGci...") String refreshToken,
        @Schema(example = "Bearer")      String tokenType,
        @Schema(example = "900")         long   expiresIn,
        TutorResumoResponse tutor
) {
    public static TokenResponse of(String access, String refresh,
                                   long expSec, TutorResumoResponse tutor) {
        return new TokenResponse(access, refresh, "Bearer", expSec, tutor);
    }
}
