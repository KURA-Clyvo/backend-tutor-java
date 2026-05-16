package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Agendamento futuro ou intenção de consulta.
 *
 * <p>Diferente de {@code EVENTO_CLINICO} (passado/realizado, domínio do .NET),
 * esta entidade representa intenções futuras.
 * Quando realizado, {@code idEventoGerado} aponta para o evento clínico correspondente.
 *
 * <p>Origens possíveis: PORTAL, WHATSAPP_LUNA, TELEFONE, BALCAO.
 */
@Entity
@Table(name = "AGENDAMENTO")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_agendamento")
    @SequenceGenerator(name = "seq_agendamento", sequenceName = "SEQ_AGENDAMENTO",
            allocationSize = 1)
    @Column(name = "ID_AGENDAMENTO")
    private Long idAgendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TUTOR", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PET", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLINICA", nullable = false)
    private Clinica clinica;

    /**
     * Veterinário preferencial — opcional.
     * ID referencia VETERINARIO do schema Oracle (escrito pelo .NET).
     */
    @Column(name = "ID_VETERINARIO")
    private Long idVeterinario;

    @Column(name = "DT_AGENDAMENTO", nullable = false)
    private LocalDateTime dtAgendamento;

    @Column(name = "NR_DURACAO_MINUTOS", nullable = false)
    @Builder.Default
    private Integer nrDuracaoMinutos = 30;

    /**
     * Tipo do agendamento.
     * Valores: CONSULTA, RETORNO, VACINA, EXAME, PROCEDIMENTO, TELEORIENTACAO
     */
    @Column(name = "DS_TIPO", nullable = false, length = 30)
    private String dsTipo;

    @Column(name = "DS_OBSERVACOES", length = 1000)
    private String dsObservacoes;

    /**
     * Status do agendamento.
     * Valores: INTENCAO, AGENDADO, CONFIRMADO, REALIZADO, CANCELADO, NAO_COMPARECEU
     */
    @Column(name = "ST_STATUS", nullable = false, length = 20)
    @Builder.Default
    private String stStatus = "AGENDADO";

    /**
     * Canal de origem do agendamento.
     * Valores: PORTAL, WHATSAPP_LUNA, TELEFONE, BALCAO
     */
    @Column(name = "DS_ORIGEM", nullable = false, length = 20)
    @Builder.Default
    private String dsOrigem = "PORTAL";

    @CreatedDate
    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_CONFIRMACAO")
    private LocalDateTime dtConfirmacao;

    @Column(name = "DT_CANCELAMENTO")
    private LocalDateTime dtCancelamento;

    @Column(name = "DS_MOTIVO_CANCEL", length = 500)
    private String dsMotivoCancel;

    /**
     * Quando o agendamento é realizado, aponta para o evento clínico gerado
     * no domínio .NET. Apenas o ID — sem FK de entidade (domínio separado).
     */
    @Column(name = "ID_EVENTO_GERADO")
    private Long idEventoGerado;

    // ── Helpers ───────────────────────────────────────────────────────────

    public boolean isAtivo() {
        return !("CANCELADO".equals(stStatus) || "REALIZADO".equals(stStatus));
    }

    public boolean isCancelado() {
        return "CANCELADO".equals(stStatus);
    }
}
