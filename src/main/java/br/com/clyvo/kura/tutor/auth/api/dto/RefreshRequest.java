package br.com.clyvo.kura.tutor.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para renovação de tokens")
public record RefreshRequest(
        @NotBlank(message = "O refresh token é obrigatório")
        @Schema(description = "Refresh token JWT obtido no login ou no último refresh")
        String refreshToken
) {}
