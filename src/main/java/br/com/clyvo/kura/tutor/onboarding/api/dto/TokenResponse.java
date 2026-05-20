package br.com.clyvo.kura.tutor.onboarding.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens JWT retornados após cadastro por convite")
public record TokenResponse(
        @Schema(description = "Access token JWT — validade 15 min", example = "eyJhbGci...") String accessToken,
        @Schema(description = "Refresh token — validade 7 dias, rotacionado a cada uso", example = "eyJhbGci...") String refreshToken,
        @Schema(description = "Tipo do token", example = "Bearer") String tokenType,
        @Schema(description = "Tempo de expiração do access token em segundos", example = "900") long expiresIn,
        @Schema(description = "Dados resumidos do tutor recém-cadastrado") TutorResumoResponse tutor
) {
    public static TokenResponse of(String access, String refresh,
                                   long expSec, TutorResumoResponse tutor) {
        return new TokenResponse(access, refresh, "Bearer", expSec, tutor);
    }
}
