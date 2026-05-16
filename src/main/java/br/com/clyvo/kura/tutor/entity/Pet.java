package br.com.clyvo.kura.tutor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Animal atendido — prontuário único.
 *
 * <p>Lido pelo backend Java; escrito pelo .NET (Felipe).
 * Vinculado a tutores via tabela associativa {@link TutorPet}.
 */
@Entity
@Table(name = "PET")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pet")
    @SequenceGenerator(name = "seq_pet", sequenceName = "SEQ_PET", allocationSize = 1)
    @Column(name = "ID_PET")
    private Long idPet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESPECIE", nullable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_RACA")
    private Raca raca;

    @Column(name = "NM_PET", nullable = false, length = 80)
    private String nmPet;

    @Column(name = "DT_NASCIMENTO")
    private LocalDate dtNascimento;

    @Column(name = "SG_SEXO", nullable = false, length = 1)
    private String sgSexo;

    @Column(name = "NR_PESO_KG", precision = 5, scale = 2)
    private BigDecimal nrPesoKg;

    @Column(name = "SG_PORTE", length = 1)
    private String sgPorte;

    @Column(name = "ST_CASTRADO", nullable = false, length = 1)
    @Builder.Default
    private String stCastrado = "N";

    @Column(name = "DS_PELAGEM", length = 60)
    private String dsPelagem;

    @Column(name = "DS_ALERGIAS", length = 500)
    private String dsAlergias;

    @Column(name = "DS_OBSERVACOES", length = 1000)
    private String dsObservacoes;

    @Column(name = "DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "ST_ATIVO", nullable = false, length = 1)
    @Builder.Default
    private String stAtivo = "S";

    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TutorPet> tutorPets = new ArrayList<>();

    public boolean isAtivo() {
        return "S".equals(stAtivo);
    }
}
