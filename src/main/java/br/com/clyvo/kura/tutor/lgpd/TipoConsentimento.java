package br.com.clyvo.kura.tutor.lgpd;

/**
 * Tipos de consentimento LGPD suportados pelo sistema.
 * Espelhados no CHECK constraint da tabela CONSENTIMENTO no Oracle.
 *
 * Usar este enum nos controllers garante que so valores validos
 * chegam ao banco — evita ORA-02290 (check constraint violated).
 */
public enum TipoConsentimento {
    TELEORIENTACAO,
    LEMBRETES,
    DADOS_ANONIMOS,
    COMPARTILHAR_SEGURADORA,
    MARKETING;

    public String toDbValue() { return this.name(); }
}
