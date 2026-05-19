package br.com.clyvo.kura.tutor.dto.response;
import br.com.clyvo.kura.tutor.entity.Consentimento;
import java.time.LocalDateTime;
public record ConsentimentoResponse(
    Long idConsentimento, String tipo, String versaoTermo,
    boolean aceito, boolean ativo,
    LocalDateTime dtAceite, LocalDateTime dtRevogacao
) {
    public static ConsentimentoResponse fromEntity(Consentimento c) {
        return new ConsentimentoResponse(c.getIdConsentimento(), c.getDsTipo(),
            c.getDsVersaoTermo(), c.isAceito(), c.isAtivo(),
            c.getDtAceite(), c.getDtRevogacao());
    }
}
