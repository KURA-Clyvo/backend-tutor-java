package br.com.clyvo.kura.tutor.consentimento.lgpd;

public final class TermoVigente {

    private TermoVigente() {}

    public static final String TELEORIENTACAO          = "v1.0";
    public static final String LEMBRETES               = "v1.0";
    public static final String DADOS_ANONIMOS          = "v1.0";
    public static final String COMPARTILHAR_SEGURADORA = "v1.0";
    public static final String MARKETING               = "v1.0";

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
