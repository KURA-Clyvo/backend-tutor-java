package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados pessoais do tutor (responsável pelo pet).
 *
 * <p><strong>Atenção:</strong> Esta entidade é <em>compartilhada</em>.
 * O cadastro é feito pelo backend .NET (Felipe). O backend Java (Nikolas) apenas
 * lê esta tabela — nunca faz INSERT ou UPDATE diretamente.
 *
 * <p>Credenciais de acesso ficam em {@link ContaTutor} (1:1, opcional).
 */
@Entity
@Table(name = "TUTOR")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tutor")
    @SequenceGenerator(name = "seq_tutor", sequenceName = "SEQ_TUTOR", allocationSize = 1)
    @Column(name = "ID_TUTOR")
    private Long idTutor;

    // ── Clínica de origem ─────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLINICA", nullable = false)
    private Clinica clinica;

    // ── Dados pessoais ────────────────────────────────────────────────────

    @Column(name = "NM_TUTOR", nullable = false, length = 120)
    private String nmTutor;

    @Column(name = "NR_CPF", nullable = false, unique = true, length = 14)
    private String nrCpf;

    @Column(name = "DT_NASCIMENTO")
    private LocalDate dtNascimento;

    @Column(name = "DS_EMAIL", nullable = false, unique = true, length = 120)
    private String dsEmail;

    @Column(name = "DS_TELEFONE", nullable = false, length = 20)
    private String dsTelefone;

    @Column(name = "DS_WHATSAPP", length = 20)
    private String dsWhatsapp;

    // ── Endereço ──────────────────────────────────────────────────────────

    @Column(name = "DS_ENDERECO", length = 200)
    private String dsEndereco;

    @Column(name = "NM_CIDADE", length = 80)
    private String nmCidade;

    @Column(name = "SG_UF", length = 2)
    private String sgUf;

    @Column(name = "NR_CEP", length = 9)
    private String nrCep;

    // ── LGPD ─────────────────────────────────────────────────────────────

    @Column(name = "ST_AVISO_PRIVACIDADE", nullable = false, length = 1)
    @Builder.Default
    private String stAvisoPrivacidade = "N";

    @Column(name = "DT_AVISO_PRIVACIDADE")
    private LocalDateTime dtAvisoPrivacidade;

    @Column(name = "DS_VERSAO_AVISO", length = 20)
    private String dsVersaoAviso;

    // ── Status ────────────────────────────────────────────────────────────

    @Column(name = "DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "ST_ATIVO", nullable = false, length = 1)
    @Builder.Default
    private String stAtivo = "S";

    // ── Relacionamentos (leitura) ─────────────────────────────────────────

    /**
     * Pets vinculados via tabela associativa {@code TUTOR_PET}.
     * Carregado via LAZY — use com cuidado dentro de transações.
     */
    @OneToMany(mappedBy = "tutor", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TutorPet> tutorPets = new ArrayList<>();

    /** Consentimentos LGPD — gerenciados pelo backend Java. */
    @OneToMany(mappedBy = "tutor", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Consentimento> consentimentos = new ArrayList<>();

    /** Agendamentos — gerenciados pelo backend Java. */
    @OneToMany(mappedBy = "tutor", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Agendamento> agendamentos = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────────────

    public boolean isAtivo() {
        return "S".equals(stAtivo);
    }
}
