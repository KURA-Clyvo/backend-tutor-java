package br.com.clyvo.kura.tutor.agendamento.domain;

import br.com.clyvo.kura.tutor.entity.Clinica;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.entity.Tutor;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "AGENDAMENTO")
@EntityListeners(AuditingEntityListener.class)
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_agendamento")
    @SequenceGenerator(name = "seq_agendamento", sequenceName = "SEQ_AGENDAMENTO", allocationSize = 1)
    @Column(name = "ID_AGENDAMENTO")
    private Long idAgendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TUTOR")
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PET")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLINICA", nullable = false)
    private Clinica clinica;

    @Column(name = "ID_VETERINARIO")
    private Long idVeterinario;

    @Column(name = "NM_PACIENTE", length = 200)
    private String nmPaciente;

    @Column(name = "DS_SERVICO", length = 200)
    private String dsServico;

    @Column(name = "DT_AGENDAMENTO", nullable = false)
    private LocalDateTime dtAgendamento;

    @Column(name = "NR_DURACAO_MINUTOS")
    private Integer nrDuracaoMinutos = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "ST_STATUS", length = 50)
    private StatusAgendamento stStatus = StatusAgendamento.AGENDADO;

    @Column(name = "DS_TIPO", length = 30)
    private String dsTipoConsulta;

    @Column(name = "DS_ORIGEM", length = 100)
    private String dsOrigem = "PORTAL";

    @Column(name = "DS_OBSERVACOES", length = 1000)
    private String dsObservacoes;

    @CreatedDate
    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_CONFIRMACAO")
    private LocalDateTime dtConfirmacao;

    @Column(name = "DT_CANCELAMENTO")
    private LocalDateTime dtCancelamento;

    @Column(name = "DS_MOTIVO_CANCEL", length = 500)
    private String dsMotivoCancel;

    /** Preenchido pelo .NET quando ST_STATUS=REALIZADO */
    @Column(name = "ID_EVENTO_GERADO")
    private Long idEventoGerado;

    /** Optimistic locking — sincroniza Java POST/PUT com .NET PATCH /status */
    @Version
    @Column(name = "NR_VERSION", nullable = false)
    private Long nrVersion;

    protected Agendamento() {}

    public static Agendamento criar(Tutor tutor, Pet pet, Clinica clinica, Long idVeterinario,
                                     LocalDateTime dtAgendamento, String tipoConsulta, String obs) {
        if (dtAgendamento.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data de agendamento deve ser no futuro.");
        }
        Agendamento a = new Agendamento();
        a.tutor = tutor;
        a.pet = pet;
        a.clinica = clinica;
        a.idVeterinario = idVeterinario;
        a.dtAgendamento = dtAgendamento;
        a.dsTipoConsulta = tipoConsulta;
        a.dsObservacoes = obs;
        a.stStatus = StatusAgendamento.AGENDADO;
        a.dsOrigem = "PORTAL";
        a.nrDuracaoMinutos = 30;
        return a;
    }

    /** Fluent setter for duration — call after criar() when request supplies a custom value. */
    public Agendamento comDuracao(int minutos) {
        this.nrDuracaoMinutos = minutos;
        return this;
    }

    public void cancelar(String motivo) {
        if (stStatus == StatusAgendamento.REALIZADO || stStatus == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException(
                "Não é possível cancelar agendamento com status " + stStatus.name() + ".");
        }
        this.stStatus = StatusAgendamento.CANCELADO;
        this.dtCancelamento = LocalDateTime.now();
        this.dsMotivoCancel = motivo;
    }

    public void confirmar() {
        if (stStatus != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException(
                "Só é possível confirmar agendamento com status AGENDADO. Status atual: " + stStatus.name());
        }
        this.stStatus = StatusAgendamento.CONFIRMADO;
        this.dtConfirmacao = LocalDateTime.now();
    }

    public void atualizar(LocalDateTime novaData, String novoTipo, String novasObs, Long novoVet) {
        if (novaData != null) {
            if (novaData.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Data de reagendamento deve ser no futuro.");
            }
            this.dtAgendamento = novaData;
        }
        if (novoTipo != null && !novoTipo.isBlank()) {
            this.dsTipoConsulta = novoTipo;
        }
        if (novasObs != null) {
            this.dsObservacoes = novasObs;
        }
        if (novoVet != null) {
            this.idVeterinario = novoVet;
        }
    }

    public boolean isAtivo() {
        return stStatus != StatusAgendamento.CANCELADO && stStatus != StatusAgendamento.REALIZADO;
    }

    public Long getIdAgendamento()          { return idAgendamento; }
    public Tutor getTutor()                 { return tutor; }
    public Pet getPet()                     { return pet; }
    public Clinica getClinica()             { return clinica; }
    public Long getIdVeterinario()          { return idVeterinario; }
    public String getNmPaciente()           { return nmPaciente; }
    public String getDsServico()            { return dsServico; }
    public LocalDateTime getDtAgendamento() { return dtAgendamento; }
    public Integer getNrDuracaoMinutos()    { return nrDuracaoMinutos; }
    public StatusAgendamento getStStatus()  { return stStatus; }
    public String getDsTipoConsulta()       { return dsTipoConsulta; }
    public String getDsOrigem()             { return dsOrigem; }
    public String getDsObservacoes()        { return dsObservacoes; }
    public LocalDateTime getDtCriacao()     { return dtCriacao; }
    public LocalDateTime getDtConfirmacao() { return dtConfirmacao; }
    public LocalDateTime getDtCancelamento(){ return dtCancelamento; }
    public String getDsMotivoCancel()       { return dsMotivoCancel; }
    public Long getIdEventoGerado()         { return idEventoGerado; }
    public Long getNrVersion()              { return nrVersion; }
}
