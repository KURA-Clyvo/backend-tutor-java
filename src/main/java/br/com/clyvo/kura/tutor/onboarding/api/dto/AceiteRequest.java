package br.com.clyvo.kura.tutor.onboarding.api.dto;

import br.com.clyvo.kura.tutor.lgpd.TipoConsentimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Aceite ou revogação de um termo LGPD no momento do cadastro")
public record AceiteRequest(

        @NotNull(message = "O tipo de consentimento é obrigatório")
        @Schema(description = "Tipo do consentimento", example = "LEMBRETES")
        TipoConsentimento tipo,

        @NotBlank(message = "A versão do termo é obrigatória")
        @Schema(description = "Versão do termo exibido ao tutor", example = "v1.0")
        String versaoTermo,

        @Schema(description = "true = aceite, false = revogação")
        boolean aceito,

        @Schema(description = "Snapshot do texto do termo — evidência ANPD (opcional)")
        String textoTermo
) {}
