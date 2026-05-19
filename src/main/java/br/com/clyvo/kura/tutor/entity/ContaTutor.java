package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Credenciais de acesso ao portal do tutor.
 * Tabela: CONTA_TUTOR — domínio exclusivo do Backend Java (Nikolas).
 *
 * NOTA: Getters/setters explícitos (sem Lombok) para compatibilidade
 * com Eclipse/STS sem o plugin de anotações instalado.
 */
@Entity
@Table(name = "conta_tutor")
@EntityListeners(AuditingEntityListener.class)
public class ContaTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta")
    private Long idConta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false, unique = true)
    private Tutor tutor;

    @Column(name = "ds_email_login", nullable = false, unique = true, length = 120)
    private String dsEmailLogin;

    @Column(name = "ds_senha_hash", nullable = false, length = 256)
    private String dsSenhaHash;

    // Rastreabilidade do convite que originou esta conta — UK_CONTA_INVITE_USED
    @Column(name = "id_invite_usado")
    private Long idInviteUsado;

    // Refresh token hasheado (BCrypt) — rotacionado a cada login
    @Column(name = "ds_refresh_token_hash", length = 256)
    private String dsRefreshTokenHash;

    @Column(name = "dt_refresh_expira")
    private LocalDateTime dtRefreshExpira;

    @CreatedDate
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_ultimo_login")
    private LocalDateTime dtUltimoLogin;

    @Column(name = "nr_tentativas_login", nullable = false)
    private Integer nrTentativasLogin = 0;

    @Column(name = "dt_bloqueio")
    private LocalDateTime dtBloqueio;

    @Column(name = "st_ativa", nullable = false, length = 1)
    private String stAtiva = "S";

    @Column(name = "st_email_verificado", nullable = false, length = 1)
    private String stEmailVerificado = "N";

    @Column(name = "ds_token_reset", length = 256)
    private String dsTokenReset;

    @Column(name = "dt_token_expira")
    private LocalDateTime dtTokenExpira;

    public ContaTutor() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public Long getIdConta()                  { return idConta; }
    public Tutor getTutor()                   { return tutor; }
    public String getDsEmailLogin()           { return dsEmailLogin; }
    public String getDsSenhaHash()            { return dsSenhaHash; }
    public Long getIdInviteUsado()            { return idInviteUsado; }
    public String getDsRefreshTokenHash()     { return dsRefreshTokenHash; }
    public LocalDateTime getDtRefreshExpira() { return dtRefreshExpira; }
    public LocalDateTime getDtCriacao()       { return dtCriacao; }
    public LocalDateTime getDtUltimoLogin()   { return dtUltimoLogin; }
    public Integer getNrTentativasLogin()     { return nrTentativasLogin; }
    public LocalDateTime getDtBloqueio()      { return dtBloqueio; }
    public String getStAtiva()                { return stAtiva; }
    public String getStEmailVerificado()      { return stEmailVerificado; }
    public String getDsTokenReset()           { return dsTokenReset; }
    public LocalDateTime getDtTokenExpira()   { return dtTokenExpira; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setIdConta(Long v)               { this.idConta = v; }
    public void setTutor(Tutor v)                { this.tutor = v; }
    public void setDsEmailLogin(String v)        { this.dsEmailLogin = v; }
    public void setDsSenhaHash(String v)         { this.dsSenhaHash = v; }
    public void setIdInviteUsado(Long v)         { this.idInviteUsado = v; }
    public void setDsRefreshTokenHash(String v)  { this.dsRefreshTokenHash = v; }
    public void setDtRefreshExpira(LocalDateTime v) { this.dtRefreshExpira = v; }
    public void setDtCriacao(LocalDateTime v)    { this.dtCriacao = v; }
    public void setDtUltimoLogin(LocalDateTime v){ this.dtUltimoLogin = v; }
    public void setNrTentativasLogin(Integer v)  { this.nrTentativasLogin = v; }
    public void setDtBloqueio(LocalDateTime v)   { this.dtBloqueio = v; }
    public void setStAtiva(String v)             { this.stAtiva = v; }
    public void setStEmailVerificado(String v)   { this.stEmailVerificado = v; }
    public void setDsTokenReset(String v)        { this.dsTokenReset = v; }
    public void setDtTokenExpira(LocalDateTime v){ this.dtTokenExpira = v; }

    // ── Helpers de domínio ────────────────────────────────────────────────────
    private static final int MAX_TENTATIVAS_LOGIN = 5;

    public boolean isAtiva()           { return "S".equals(stAtiva); }
    public boolean isBloqueada()       { return dtBloqueio != null; }
    public boolean isEmailVerificado() { return "S".equals(stEmailVerificado); }

    /** Registra uma tentativa de login bem-sucedida: reseta contador e desbloqueia. */
    public void registrarLoginSucesso() {
        this.nrTentativasLogin = 0;
        this.dtBloqueio        = null;
        this.dtUltimoLogin     = LocalDateTime.now();
    }

    /**
     * Registra uma tentativa de login com falha.
     * Ao atingir MAX_TENTATIVAS_LOGIN, seta dtBloqueio (423 Locked).
     */
    public void registrarLoginFalha() {
        this.nrTentativasLogin = (nrTentativasLogin == null ? 0 : nrTentativasLogin) + 1;
        if (this.nrTentativasLogin >= MAX_TENTATIVAS_LOGIN) {
            this.dtBloqueio = LocalDateTime.now();
        }
    }

    /** @deprecated Use {@link #registrarLoginFalha()} — mantido para migração incremental. */
    @Deprecated
    public void incrementarTentativas() {
        registrarLoginFalha();
    }

    /** @deprecated Use {@link #registrarLoginSucesso()} — mantido para migração incremental. */
    @Deprecated
    public void resetarTentativas() {
        this.nrTentativasLogin = 0;
        this.dtBloqueio = null;
    }

    /**
     * Atualiza o refresh token hasheado e sua expiração.
     * Chamado a cada login/registro para rotacionar o token.
     */
    public void rotacionarRefresh(String refreshTokenHash, LocalDateTime expira) {
        this.dsRefreshTokenHash = refreshTokenHash;
        this.dtRefreshExpira = expira;
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Cria conta a partir de um convite validado.
     * Email herdado do Tutor (fonte de verdade é o .NET).
     */
    public static ContaTutor criarPorInvite(Tutor tutor, String email,
                                             String senhaHash, Long idInvite) {
        ContaTutor c = new ContaTutor();
        c.tutor            = tutor;
        c.dsEmailLogin     = email;
        c.dsSenhaHash      = senhaHash;
        c.idInviteUsado    = idInvite;
        c.stAtiva          = "S";
        c.stEmailVerificado = "N";
        c.nrTentativasLogin = 0;
        return c;
    }

    // Builder estático (substitui @Builder do Lombok)
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ContaTutor o = new ContaTutor();
        public Builder tutor(Tutor v)              { o.tutor = v;              return this; }
        public Builder dsEmailLogin(String v)      { o.dsEmailLogin = v;       return this; }
        public Builder dsSenhaHash(String v)       { o.dsSenhaHash = v;        return this; }
        public Builder stAtiva(String v)           { o.stAtiva = v;            return this; }
        public Builder stEmailVerificado(String v) { o.stEmailVerificado = v;  return this; }
        public Builder nrTentativasLogin(Integer v){ o.nrTentativasLogin = v;  return this; }
        public ContaTutor build()                  { return o; }
    }
}
