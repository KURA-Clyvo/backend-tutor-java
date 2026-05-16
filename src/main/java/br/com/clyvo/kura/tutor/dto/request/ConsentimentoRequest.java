package br.com.clyvo.kura.tutor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para registrar ou revogar um consentimento LGPD.
 * Regra: sempre INSERT — nunca UPDATE. Histórico é imutável.
 */
@Schema(description = "Registro de consentimento LGPD")
public record ConsentimentoRequest(

        @Schema(description = "Tipo do consentimento",
                example = "LEMBRETES",
                allowableValues = {"TELEORIENTACAO","LEMBRETES","DADOS_ANONIMOS","COMPARTILHAR_SEGURADORA","MARKETING"})
        @NotBlank(message = "O tipo de consentimento é obrigatório.")
        @Pattern(regexp = "TELEORIENTACAO|LEMBRETES|DADOS_ANONIMOS|COMPARTILHAR_SEGURADORA|MARKETING",
                message = "Tipo de consentimento inválido.")
        String tipo,

        @Schema(description = "S = aceita | N = revoga", example = "S")
        @NotBlank(message = "O status de aceite é obrigatório.")
        @Pattern(regexp = "[SN]", message = "Aceite deve ser 'S' ou 'N'.")
        String aceito,

        @Schema(description = "Versão do termo exibido ao tutor", example = "v1.0")
        @NotBlank(message = "A versão do termo é obrigatória.")
        String versaoTermo
) {}
