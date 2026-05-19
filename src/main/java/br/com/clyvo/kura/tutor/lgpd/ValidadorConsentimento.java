package br.com.clyvo.kura.tutor.lgpd;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import org.springframework.stereotype.Component;

/**
 * Valida regras de negócio LGPD antes de persistir consentimentos.
 *
 * Design Pattern: Strategy — cada regra e um metodo independente,
 * facilitando adicionar novas validacoes sem alterar o service.
 *
 * Regras implementadas:
 * 1. Tutor deve ter recebido aviso de privacidade antes de aceitar qualquer termo
 * 2. Versao do termo enviada deve ser a versao vigente
 * 3. Nao pode revogar o que nunca foi aceito (verificado no service)
 */
@Component
public class ValidadorConsentimento {

    /**
     * Regra 1: tutor so pode aceitar consentimentos apos ter recebido o aviso de privacidade.
     * O aviso e registrado pelo .NET no balcao (ST_AVISO_PRIVACIDADE='S' em TUTOR).
     *
     * Base legal: LGPD art. 6o, VI — transparencia.
     */
    public void validarAvisoPrivacidade(Tutor tutor) {
        if (!tutor.temAvisoPrivacidade()) {
            throw new RegraDeNegocioException(
                "Tutor nao recebeu o aviso de privacidade. " +
                "Entre em contato com a clinica para regularizar.");
        }
    }

    /**
     * Regra 2: versao do termo enviada deve ser a versao vigente.
     * Previne aceite de termos desatualizados que o frontend possa ter em cache.
     */
    public void validarVersaoTermo(ConsentimentoRequest request) {
        validarVersaoTermo(request.tipo(), request.versaoTermo());
    }

    /** Overload sem acoplamento a DTO — usado pelo OnboardingService com AceiteRequest. */
    public void validarVersaoTermo(TipoConsentimento tipo, String versaoTermo) {
        String vigente = TermoVigente.versaoPara(tipo);
        if (!vigente.equals(versaoTermo)) {
            throw new RegraDeNegocioException(
                "Versao do termo desatualizada. Versao vigente: " + vigente +
                ". Recarregue o aplicativo e tente novamente.");
        }
    }
}
