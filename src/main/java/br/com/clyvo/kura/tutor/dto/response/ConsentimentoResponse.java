package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Consentimento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Status atual de um consentimento LGPD")
public record ConsentimentoResponse(
        @Schema(example = "1") Long idConsentimento,
        @Schema(example = "LEMBRETES") String tipo,
        @Schema(example = "v1.0") String versaoTermo,
        @Schema(example = "true") boolean ativo,
        @Schema(example = "2026-05-01T10:30:00") LocalDateTime dtAceite,
        @Schema(description = "Preenchido se revogado") LocalDateTime dtRevogacao
) {
    public static ConsentimentoResponse from(Consentimento c) {
        return new ConsentimentoResponse(
                c.getIdConsentimento(),
                c.getDsTipo(),
                c.getDsVersaoTermo(),
                c.isAtivo(),
                c.getDtAceite(),
                c.getDtRevogacao()
        );
    }
}
