package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Registro LGPD de consentimento do tutor.
 *
 * <p>Cada aceite ou revogação gera uma nova linha — o histórico é imutável.
 * Nunca delete ou atualize registros desta tabela.
 *
 * <p>Design Pattern: <em>Strategy</em> — as regras de validação de cada tipo
 * de consentimento são implementadas em {@code ConsentimentoStrategy}.
 */
@Entity
@Table(name = "CONSENTIMENTO")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_consentimento")
    @SequenceGenerator(name = "seq_consentimento", sequenceName = "SEQ_CONSENTIMENTO",
            allocationSize = 1)
    @Column(name = "ID_CONSENTIMENTO")
    private Long idConsentimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TUTOR", nullable = false)
    private Tutor tutor;

    /**
     * Tipo de consentimento.
     * Valores válidos (CHECK constraint no Oracle):
     * TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS, COMPARTILHAR_SEGURADORA, MARKETING
     */
    @Column(name = "DS_TIPO", nullable = false, length = 40)
    private String dsTipo;

    /** Versão do termo aceito — permite rastrear alterações no texto. */
    @Column(name = "DS_VERSAO_TERMO", nullable = false, length = 20)
    private String dsVersaoTermo;

    /** Texto completo do termo (CLOB). Salvo no momento do aceite para imutabilidade. */
    @Lob
    @Column(name = "DS_TEXTO_TERMO")
    private String dsTextoTermo;

    /** S = aceite, N = recusado. */
    @Column(name = "ST_ACEITO", nullable = false, length = 1)
    private String stAceito;

    @CreatedDate
    @Column(name = "DT_ACEITE", nullable = false, updatable = false)
    private LocalDateTime dtAceite;

    @Column(name = "DS_IP_ACEITE", length = 45)
    private String dsIpAceite;

    /** Preenchido quando o tutor revoga este consentimento. */
    @Column(name = "DT_REVOGACAO")
    private LocalDateTime dtRevogacao;

    @Column(name = "DS_IP_REVOGACAO", length = 45)
    private String dsIpRevogacao;

    // ── Helpers ───────────────────────────────────────────────────────────

    public boolean isAceito() {
        return "S".equals(stAceito);
    }

    public boolean isRevogado() {
        return dtRevogacao != null;
    }

    public boolean isAtivo() {
        return isAceito() && !isRevogado();
    }
}
