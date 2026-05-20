package br.com.clyvo.kura.tutor.consentimento.lgpd;

public enum TipoConsentimento {
    TELEORIENTACAO,
    LEMBRETES,
    DADOS_ANONIMOS,
    COMPARTILHAR_SEGURADORA,
    MARKETING;

    public String toDbValue() { return this.name(); }
}
