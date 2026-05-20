package br.com.clyvo.kura.tutor.consentimento.api.dto;

import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import java.time.LocalDateTime;

public record ConsentimentoResponse(
        Long          idConsentimento,
        String        tipo,
        String        versaoTermo,
        boolean       aceito,
        boolean       ativo,
        LocalDateTime dtAceite,
        LocalDateTime dtRevogacao
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
