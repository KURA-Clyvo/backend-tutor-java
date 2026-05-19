package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Agendamento futuro / intencao de atendimento.
 *
 * CORRECAO BUG-04 (QA cross-API):
 * Campo NR_VERSION adicionado para controle de concorrencia otimista.
 * O .NET usa IsConcurrencyToken() neste campo — o Java precisa enviar
 * o valor atual ao fazer updates para evitar sobrescrita silenciosa.
 * Clayton deve adicionar via Flyway: NR_VERSION NUMBER(19) DEFAULT 0 NOT NULL
 *
 * Status possiveis: INTENCAO, AGENDADO, CONFIRMADO, REALIZADO, CANCELADO, NAO_COMPARECEU
 * Origem: PORTAL, WHATSAPP_LUNA, TELEFONE, BALCAO
 */
@Entity
@Table(name = "agendamento")
@EntityListeners(AuditingEntityListener.class)
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agendamento")
    private Long idAgendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    // ID do veterinario — referencia sem entidade (escrita pelo .NET)
    @Column(name = "id_veterinario")
    private Long idVeterinario;

    @Column(name = "dt_agendamento", nullable = false)
    private LocalDateTime dtAgendamento;

    @Column(name = "nr_duracao_minutos", nullable = false)
    private Integer nrDuracaoMinutos = 30;

    @Column(name = "ds_tipo", nullable = false, length = 30)
    private String dsTipo;

    @Column(name = "ds_observacoes", length = 1000)
    private String dsObservacoes;

    @Column(name = "st_status", nullable = false, length = 20)
    private String stStatus = "AGENDADO";

    @Column(name = "ds_origem", nullable = false, length = 20)
    private String dsOrigem = "PORTAL";

    @CreatedDate
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_confirmacao")
    private LocalDateTime dtConfirmacao;

    @Column(name = "dt_cancelamento")
    private LocalDateTime dtCancelamento;

    @Column(name = "ds_motivo_cancel", length = 500)
    private String dsMotivoCancel;

    // Vincula ao EVENTO_CLINICO quando realizado (preenchido pelo .NET)
    @Column(name = "id_evento_gerado")
    private Long idEventoGerado;

    // CORRECAO BUG-04: campo de versao para concorrencia otimista com o .NET
    @Version
    @Column(name = "nr_version")
    private Long nrVersion;

    public Agendamento() {}

    public Long getIdAgendamento() { return idAgendamento; }
    public Tutor getTutor() { return tutor; }
    public Pet getPet() { return pet; }
    public Clinica getClinica() { return clinica; }
    public Long getIdVeterinario() { return idVeterinario; }
    public LocalDateTime getDtAgendamento() { return dtAgendamento; }
    public Integer getNrDuracaoMinutos() { return nrDuracaoMinutos; }
    public String getDsTipo() { return dsTipo; }
    public String getDsObservacoes() { return dsObservacoes; }
    public String getStStatus() { return stStatus; }
    public String getDsOrigem() { return dsOrigem; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public LocalDateTime getDtConfirmacao() { return dtConfirmacao; }
    public LocalDateTime getDtCancelamento() { return dtCancelamento; }
    public String getDsMotivoCancel() { return dsMotivoCancel; }
    public Long getIdEventoGerado() { return idEventoGerado; }
    public Long getNrVersion() { return nrVersion; }

    public void setTutor(Tutor v) { this.tutor = v; }
    public void setPet(Pet v) { this.pet = v; }
    public void setClinica(Clinica v) { this.clinica = v; }
    public void setIdVeterinario(Long v) { this.idVeterinario = v; }
    public void setDtAgendamento(LocalDateTime v) { this.dtAgendamento = v; }
    public void setNrDuracaoMinutos(Integer v) { this.nrDuracaoMinutos = v; }
    public void setDsTipo(String v) { this.dsTipo = v; }
    public void setDsObservacoes(String v) { this.dsObservacoes = v; }
    public void setStStatus(String v) { this.stStatus = v; }
    public void setDsOrigem(String v) { this.dsOrigem = v; }
    public void setDtCriacao(LocalDateTime v) { this.dtCriacao = v; }
    public void setDtCancelamento(LocalDateTime v) { this.dtCancelamento = v; }
    public void setDsMotivoCancel(String v) { this.dsMotivoCancel = v; }
    public void setIdEventoGerado(Long v) { this.idEventoGerado = v; }

    public boolean isAtivo() {
        return !("CANCELADO".equals(stStatus) || "REALIZADO".equals(stStatus));
    }
}
