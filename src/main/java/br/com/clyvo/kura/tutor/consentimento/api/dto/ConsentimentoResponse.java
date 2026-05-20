package br.com.clyvo.kura.tutor.consentimento.api.dto;

import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado de um consentimento LGPD")
public record ConsentimentoResponse(
        @Schema(description = "ID do consentimento", example = "10") Long idConsentimento,
        @Schema(description = "Tipo do consentimento", example = "LEMBRETES") String tipo,
        @Schema(description = "Versão do termo aceito", example = "v1.0") String versaoTermo,
        @Schema(description = "true = aceito, false = revogado") boolean aceito,
        @Schema(description = "true = aceito e sem revogação posterior") boolean ativo,
        @Schema(description = "Data/hora do aceite") LocalDateTime dtAceite,
        @Schema(description = "Data/hora da revogação — null se não revogado") LocalDateTime dtRevogacao
) {
    public static ConsentimentoResponse fromEntity(Consentimento c) {
        return new ConsentimentoResponse(
                c.getIdConsentimento(),
                c.getDsTipo(),
                c.getDsVersaoTermo(),
                c.isAceito(),
                c.isAtivo(),
                c.getDtAceite(),
                c.getDtRevogacao());
    }
}
