package br.com.clyvo.kura.tutor.consentimento.api.dto;

import br.com.clyvo.kura.tutor.consentimento.lgpd.TipoConsentimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Registro de consentimento LGPD — cada chamada = novo INSERT imutável")
public record ConsentimentoRequest(
    @Schema(description = "Tipo do consentimento", example = "LEMBRETES")
    @NotNull TipoConsentimento tipo,

    @Schema(description = "Versão do termo exibido ao tutor", example = "v1.0")
    @NotBlank String versaoTermo,

    @Schema(description = "S=aceite  N=revogação", example = "S")
    @NotNull @Pattern(regexp = "[SN]", message = "aceito deve ser S ou N")
    String aceito,

    @Schema(description = "Texto completo do termo no momento do aceite")
    String textoTermo
) {}
