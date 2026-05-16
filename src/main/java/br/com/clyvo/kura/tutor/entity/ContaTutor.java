package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Credenciais de acesso ao portal do tutor.
 * Tabela CONTA_TUTOR — domínio exclusivo do Backend Tutor (Java).
 *
 * Lombok removido para compatibilidade com Eclipse/STS sem plugin.
 * Getters/setters e Builder explícitos.
 */
@Entity
@Table(name = "CONTA_TUTOR")
@EntityListeners(AuditingEntityListener.class)
public class ContaTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_conta_tutor")
    @SequenceGenerator(name = "seq_conta_tutor", sequenceName = "SEQ_CONTA_TUTOR", allocationSize = 1)
    @Column(name = "ID_CONTA")
    private Long idConta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TUTOR", nullable = false, unique = true)
    private Tutor tutor;

    @Column(name = "DS_EMAIL_LOGIN", nullable = false, unique = true, length = 120)
    private String dsEmailLogin;

    @Column(name = "DS_SENHA_HASH", nullable = false, length = 256)
    private String dsSenhaHash;

    @Column(name = "DS_SALT", length = 64)
    private String dsSalt;

    @CreatedDate
    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_ULTIMO_LOGIN")
    private LocalDateTime dtUltimoLogin;

    @Column(name = "NR_TENTATIVAS_LOGIN", nullable = false)
    private Integer nrTentativasLogin = 0;

    @Column(name = "DT_BLOQUEIO")
    private LocalDateTime dtBloqueio;

    @Column(name = "ST_ATIVA", nullable = false, length = 1)
    private String stAtiva = "S";

    @Column(name = "ST_EMAIL_VERIFICADO", nullable = false, length = 1)
    private String stEmailVerificado = "N";

    @Column(name = "DS_TOKEN_RESET", length = 256)
    private String dsTokenReset;

    @Column(name = "DT_TOKEN_EXPIRA")
    private LocalDateTime dtTokenExpira;

    // ── Construtor padrão (JPA exige) ─────────────────────────────────────
    public ContaTutor() {}

    // ── Getters ───────────────────────────────────────────────────────────
    public Long getIdConta() { return idConta; }
    public Tutor getTutor() { return tutor; }
    public String getDsEmailLogin() { return dsEmailLogin; }
    public String getDsSenhaHash() { return dsSenhaHash; }
    public String getDsSalt() { return dsSalt; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public LocalDateTime getDtUltimoLogin() { return dtUltimoLogin; }
    public Integer getNrTentativasLogin() { return nrTentativasLogin; }
    public LocalDateTime getDtBloqueio() { return dtBloqueio; }
    public String getStAtiva() { return stAtiva; }
    public String getStEmailVerificado() { return stEmailVerificado; }
    public String getDsTokenReset() { return dsTokenReset; }
    public LocalDateTime getDtTokenExpira() { return dtTokenExpira; }

    // ── Setters ───────────────────────────────────────────────────────────
    public void setIdConta(Long idConta) { this.idConta = idConta; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }
    public void setDsEmailLogin(String v) { this.dsEmailLogin = v; }
    public void setDsSenhaHash(String v) { this.dsSenhaHash = v; }
    public void setDsSalt(String v) { this.dsSalt = v; }
    public void setDtCriacao(LocalDateTime v) { this.dtCriacao = v; }
    public void setDtUltimoLogin(LocalDateTime v) { this.dtUltimoLogin = v; }
    public void setNrTentativasLogin(Integer v) { this.nrTentativasLogin = v; }
    public void setDtBloqueio(LocalDateTime v) { this.dtBloqueio = v; }
    public void setStAtiva(String v) { this.stAtiva = v; }
    public void setStEmailVerificado(String v) { this.stEmailVerificado = v; }
    public void setDsTokenReset(String v) { this.dsTokenReset = v; }
    public void setDtTokenExpira(LocalDateTime v) { this.dtTokenExpira = v; }

    // ── Helpers de domínio ────────────────────────────────────────────────
    public boolean isAtiva() { return "S".equals(stAtiva); }
    public boolean isBloqueada() { return dtBloqueio != null; }
    public boolean isEmailVerificado() { return "S".equals(stEmailVerificado); }

    public void incrementarTentativas() {
        this.nrTentativasLogin = (this.nrTentativasLogin == null ? 0 : this.nrTentativasLogin) + 1;
    }
    public void resetarTentativas() {
        this.nrTentativasLogin = 0;
        this.dtBloqueio = null;
    }

    // ── Builder estático (substitui @Builder do Lombok) ───────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ContaTutor obj = new ContaTutor();
        public Builder tutor(Tutor v) { obj.tutor = v; return this; }
        public Builder dsEmailLogin(String v) { obj.dsEmailLogin = v; return this; }
        public Builder dsSenhaHash(String v) { obj.dsSenhaHash = v; return this; }
        public Builder dsSalt(String v) { obj.dsSalt = v; return this; }
        public Builder stAtiva(String v) { obj.stAtiva = v; return this; }
        public Builder stEmailVerificado(String v) { obj.stEmailVerificado = v; return this; }
        public Builder nrTentativasLogin(Integer v) { obj.nrTentativasLogin = v; return this; }
        public ContaTutor build() { return obj; }
    }
}
