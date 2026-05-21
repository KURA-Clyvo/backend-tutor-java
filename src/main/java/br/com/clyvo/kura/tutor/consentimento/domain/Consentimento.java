package br.com.clyvo.kura.tutor.consentimento.domain;

import br.com.clyvo.kura.tutor.consentimento.lgpd.TipoConsentimento;
import br.com.clyvo.kura.tutor.entity.Tutor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro LGPD de consentimento do tutor.
 *
 * REGRA FUNDAMENTAL: nunca UPDATE — somente INSERT.
 * Cada aceite ou revogação = novo registro. Estado atual = mais recente por DS_TIPO.
 */
@Entity
@Table(name = "consentimento")
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

    @Column(name = "st_aceito", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String stAceito;

    @Column(name = "dt_aceite", nullable = false, updatable = false)
    private LocalDateTime dtAceite;

    @Column(name = "ds_ip_aceite", length = 45)
    private String dsIpAceite;

    @Column(name = "dt_revogacao")
    private LocalDateTime dtRevogacao;

    @Column(name = "ds_ip_revogacao", length = 45)
    private String dsIpRevogacao;

    protected Consentimento() {}

    public Long getIdConsentimento()      { return idConsentimento; }
    public Tutor getTutor()               { return tutor; }
    public String getDsTipo()             { return dsTipo; }
    public String getDsVersaoTermo()      { return dsVersaoTermo; }
    public String getDsTextoTermo()       { return dsTextoTermo; }
    public String getStAceito()           { return stAceito; }
    public LocalDateTime getDtAceite()    { return dtAceite; }
    public String getDsIpAceite()         { return dsIpAceite; }
    public LocalDateTime getDtRevogacao() { return dtRevogacao; }
    public String getDsIpRevogacao()      { return dsIpRevogacao; }

    public boolean isAceito()  { return "S".equals(stAceito); }
    public boolean isRevogado(){ return dtRevogacao != null; }
    public boolean isAtivo()   { return isAceito() && !isRevogado(); }

    // ── Factory methods (sempre INSERT — nunca UPDATE) ────────────────────────

    public static Consentimento novoAceite(Tutor tutor, TipoConsentimento tipo,
                                            String versaoTermo, String textoTermo, String ip) {
        Consentimento c = new Consentimento();
        c.tutor         = tutor;
        c.dsTipo        = tipo.toDbValue();
        c.dsVersaoTermo = versaoTermo;
        c.dsTextoTermo  = textoTermo;
        c.stAceito      = "S";
        c.dtAceite      = LocalDateTime.now();
        c.dsIpAceite    = ip;
        return c;
    }

    public static Consentimento revogacao(Tutor tutor, TipoConsentimento tipo,
                                           String versaoTermo, String ip) {
        Consentimento c = new Consentimento();
        c.tutor         = tutor;
        c.dsTipo        = tipo.toDbValue();
        c.dsVersaoTermo = versaoTermo;
        c.stAceito      = "N";
        c.dtAceite      = LocalDateTime.now();
        c.dsIpAceite    = ip;
        c.dtRevogacao   = LocalDateTime.now();
        c.dsIpRevogacao = ip;
        return c;
    }
}
