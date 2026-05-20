package br.com.clyvo.kura.tutor.timeline.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Immutable
@Entity
@IdClass(VacinaVencendoId.class)
@Table(name = "VW_VACINAS_VENCENDO")
public class VacinaVencendo {

    @Id
    @Column(name = "ID_PET")
    private Long idPet;

    @Id
    @Column(name = "NM_VACINA")
    private String nmVacina;

    @Id
    @Column(name = "DT_PROXIMA_DOSE")
    private LocalDateTime dtProximaDose;

    @Column(name = "ID_TUTOR")
    private Long idTutor;

    @Column(name = "NM_PET")
    private String nmPet;

    @Column(name = "ID_CLINICA")
    private Long idClinica;

    @Column(name = "NM_CLINICA")
    private String nmClinica;

    protected VacinaVencendo() {}

    public Long getIdPet()                  { return idPet; }
    public String getNmVacina()             { return nmVacina; }
    public LocalDateTime getDtProximaDose() { return dtProximaDose; }
    public Long getIdTutor()                { return idTutor; }
    public String getNmPet()               { return nmPet; }
    public Long getIdClinica()              { return idClinica; }
    public String getNmClinica()            { return nmClinica; }
}
