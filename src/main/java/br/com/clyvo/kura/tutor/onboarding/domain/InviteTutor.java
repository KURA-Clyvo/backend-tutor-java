package br.com.clyvo.kura.tutor.onboarding.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Convite enviado pela clínica ao tutor — gerado pelo .NET, consumido 1x pelo Java.
 *
 * @Immutable: Hibernate nunca emite UPDATE para esta entidade.
 * Leitura pura: InviteTutorRepository estende apenas Repository<T, ID>.
 */
@Entity
@Table(name = "invite_tutor")
@Immutable
public class InviteTutor {

    @Id
    @Column(name = "id_invite")
    private Long idInvite;

    @Column(name = "id_tutor", nullable = false)
    private Long idTutor;

    @Column(name = "nr_token", nullable = false, length = 36)
    private String nrToken;

    @Column(name = "dt_expiracao", nullable = false)
    private LocalDateTime dtExpiracao;

    @Column(name = "ds_canal", nullable = false, length = 20)
    private String dsCanal;

    @Column(name = "st_utilizado", nullable = false, length = 1)
    private String stUtilizado;

    @Column(name = "st_ativo", nullable = false, length = 1)
    private String stAtivo;

    protected InviteTutor() {}

    public Long getIdInvite()         { return idInvite; }
    public Long getIdTutor()          { return idTutor; }
    public String getNrToken()        { return nrToken; }
    public LocalDateTime getDtExpiracao() { return dtExpiracao; }
    public String getDsCanal()        { return dsCanal; }

    public boolean isAtivo()     { return "S".equals(stAtivo); }
    public boolean isUtilizado() { return "S".equals(stUtilizado); }
    public boolean isExpirado()  { return LocalDateTime.now().isAfter(dtExpiracao); }
}
