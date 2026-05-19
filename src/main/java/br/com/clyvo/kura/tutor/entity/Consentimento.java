package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Registro LGPD de consentimento do tutor.
 *
 * REGRA FUNDAMENTAL: nunca fazer UPDATE nesta tabela.
 * Cada aceite ou revogacao = novo INSERT.
 * O estado atual e sempre o registro mais recente por DS_TIPO.
 *
 * Tipos validos (CHECK constraint no Oracle):
 * TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS, COMPARTILHAR_SEGURADORA, MARKETING
 */
@Entity
@Table(name = "consentimento")
@EntityListeners(AuditingEntityListener.class)
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimento")
    private Long idConsentimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @Column(name = "ds_tipo", nullable = false, length = 40)
    private String dsTipo;

    @Column(name = "ds_versao_termo", nullable = false, length = 20)
    private String dsVersaoTermo;

    @Lob
    @Column(name = "ds_texto_termo")
    private String dsTextoTermo;

    // S = aceite, N = revogacao
    @Column(name = "st_aceito", nullable = false, length = 1)
    private String stAceito;

    @CreatedDate
    @Column(name = "dt_aceite", nullable = false, updatable = false)
    private LocalDateTime dtAceite;

    // IP do cliente — evidencia legal LGPD (art. 7o, I)
    @Column(name = "ds_ip_aceite", length = 45)
    private String dsIpAceite;

    @Column(name = "dt_revogacao")
    private LocalDateTime dtRevogacao;

    @Column(name = "ds_ip_revogacao", length = 45)
    private String dsIpRevogacao;

    public Consentimento() {}

    public Long getIdConsentimento() { return idConsentimento; }
    public Tutor getTutor() { return tutor; }
    public String getDsTipo() { return dsTipo; }
    public String getDsVersaoTermo() { return dsVersaoTermo; }
    public String getDsTextoTermo() { return dsTextoTermo; }
    public String getStAceito() { return stAceito; }
    public LocalDateTime getDtAceite() { return dtAceite; }
    public String getDsIpAceite() { return dsIpAceite; }
    public LocalDateTime getDtRevogacao() { return dtRevogacao; }
    public String getDsIpRevogacao() { return dsIpRevogacao; }

    public void setTutor(Tutor v) { this.tutor = v; }
    public void setDsTipo(String v) { this.dsTipo = v; }
    public void setDsVersaoTermo(String v) { this.dsVersaoTermo = v; }
    public void setDsTextoTermo(String v) { this.dsTextoTermo = v; }
    public void setStAceito(String v) { this.stAceito = v; }
    public void setDtAceite(LocalDateTime v) { this.dtAceite = v; }
    public void setDsIpAceite(String v) { this.dsIpAceite = v; }
    public void setDtRevogacao(LocalDateTime v) { this.dtRevogacao = v; }
    public void setDsIpRevogacao(String v) { this.dsIpRevogacao = v; }

    public boolean isAceito() { return "S".equals(stAceito); }
    public boolean isRevogado() { return dtRevogacao != null; }
    public boolean isAtivo() { return isAceito() && !isRevogado(); }
}
