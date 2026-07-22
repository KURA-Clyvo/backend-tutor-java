package br.com.clyvo.kura.tutor.tutor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Registro de token de push notification do tutor")
public record PushTokenRequest(

        @Schema(description = "Token Expo/FCM do dispositivo",
                example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        @NotBlank(message = "Push token é obrigatório")
        String dsPushToken,

        @Schema(description = "Plataforma do dispositivo", allowableValues = {"ios", "android"},
                example = "android")
        @NotBlank(message = "Plataforma é obrigatória")
        @Pattern(regexp = "ios|android", message = "Plataforma deve ser 'ios' ou 'android'")
        String dsPlatforma
) {}
