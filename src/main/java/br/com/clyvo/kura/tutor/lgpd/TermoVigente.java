package br.com.clyvo.kura.tutor.lgpd;

/**
 * Versoes dos termos de consentimento vigentes.
 *
 * Centraliza o controle de versao dos termos para que qualquer alteracao
 * seja rastreavel — LGPD exige que o tutor saiba exatamente qual versao
 * do termo aceitou (DS_VERSAO_TERMO em CONSENTIMENTO).
 *
 * Quando o texto de um termo mudar:
 *   1. Incrementar a versao aqui
 *   2. O frontend exibe o novo texto e solicita novo aceite
 *   3. Um novo INSERT em CONSENTIMENTO e gerado — historico intacto
 */
public final class TermoVigente {

    private TermoVigente() {}

    public static final String TELEORIENTACAO        = "v1.0";
    public static final String LEMBRETES             = "v1.0";
    public static final String DADOS_ANONIMOS        = "v1.0";
    public static final String COMPARTILHAR_SEGURADORA = "v1.0";
    public static final String MARKETING             = "v1.0";

    /** Retorna a versao vigente para um TipoConsentimento. */
    public static String versaoPara(TipoConsentimento tipo) {
        return switch (tipo) {
            case TELEORIENTACAO          -> TELEORIENTACAO;
            case LEMBRETES               -> LEMBRETES;
            case DADOS_ANONIMOS          -> DADOS_ANONIMOS;
            case COMPARTILHAR_SEGURADORA -> COMPARTILHAR_SEGURADORA;
            case MARKETING               -> MARKETING;
        };
    }
}
