// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Segundo perfil de acesso ao painel administrativo interno (SUPORTE) —
 * ver V20__web_usuario_suporte.sql. Sem relação de entidade
 * (sem {@code @ManyToOne}/{@code @OneToOne}) com nenhuma tabela de produto.
 */
@Entity
@Table(name = "web_usuario_suporte")
public class WebUsuarioSuporte {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_web_usuario_suporte")
    @SequenceGenerator(name = "seq_web_usuario_suporte",
            sequenceName = "SEQ_WEB_USUARIO_SUPORTE", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "ds_login", nullable = false, unique = true, length = 120)
    private String dsLogin;

    @Column(name = "ds_senha_hash", nullable = false, length = 256)
    private String dsSenhaHash;

    @Column(name = "st_ativa", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String stAtiva = "S";

    public WebUsuarioSuporte() {}

    public Long getId()               { return id; }
    public String getDsLogin()        { return dsLogin; }
    public String getDsSenhaHash()    { return dsSenhaHash; }
    public String getStAtiva()        { return stAtiva; }

    public void setId(Long v)             { this.id = v; }
    public void setDsLogin(String v)      { this.dsLogin = v; }
    public void setDsSenhaHash(String v)  { this.dsSenhaHash = v; }
    public void setStAtiva(String v)      { this.stAtiva = v; }

    public boolean isAtiva() { return "S".equals(stAtiva); }
}
