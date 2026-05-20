package br.com.clyvo.kura.tutor.consentimento.lgpd;

import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import org.springframework.stereotype.Component;

@Component
public class ValidadorConsentimento {

    public void validarAvisoPrivacidade(Tutor tutor) {
        if (!tutor.temAvisoPrivacidade()) {
            throw new RegraDeNegocioException(
                "Tutor não recebeu o aviso de privacidade. " +
                "Entre em contato com a clínica para regularizar.");
        }
    }

    public void validarVersaoTermo(ConsentimentoRequest request) {
        validarVersaoTermo(request.tipo(), request.versaoTermo());
    }

    public void validarVersaoTermo(TipoConsentimento tipo, String versaoTermo) {
        String vigente = TermoVigente.versaoPara(tipo);
        if (!vigente.equals(versaoTermo)) {
            throw new RegraDeNegocioException(
                "Versão do termo desatualizada. Versão vigente: " + vigente +
                ". Recarregue o aplicativo e tente novamente.");
        }
    }
}
