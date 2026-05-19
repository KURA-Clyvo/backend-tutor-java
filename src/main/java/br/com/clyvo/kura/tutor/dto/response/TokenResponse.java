package br.com.clyvo.kura.tutor.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "Token JWT retornado apos login")
public record TokenResponse(
    @Schema(example = "eyJhbGci...") String accessToken,
    @Schema(example = "Bearer")      String tokenType,
    @Schema(example = "86400")       long expiresIn,
    Long idConta,
    String nmTutor
) {
    public static TokenResponse of(String token, long exp, Long idConta, String nmTutor) {
        return new TokenResponse(token, "Bearer", exp, idConta, nmTutor);
    }
}
