package br.com.clyvo.kura.tutor.dto.request;
import br.com.clyvo.kura.tutor.lgpd.TipoConsentimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
@Schema(description = "Registro de consentimento LGPD — cada chamada = novo INSERT imutavel")
public record ConsentimentoRequest(
    @Schema(description = "Tipo do consentimento", example = "LEMBRETES") @NotNull TipoConsentimento tipo,
    @Schema(description = "Versao do termo exibido ao tutor", example = "v1.0") @NotBlank String versaoTermo,
    @Schema(description = "S=aceite  N=revogacao", example = "S") @NotNull @Pattern(regexp = "[SN]", message = "aceito deve ser S ou N") String aceito,
    @Schema(description = "Texto completo do termo no momento do aceite") String textoTermo
) {}
