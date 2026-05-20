package br.com.clyvo.kura.tutor.timeline.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Immutable
@Entity
@IdClass(TimelinePetId.class)
@Table(name = "VW_TIMELINE_PET")
public class TimelinePet {

    @Id
    @Column(name = "ID_PET")
    private Long idPet;

    @Id
    @Column(name = "ID_EVENTO")
    private Long idEvento;

    @Column(name = "NM_PET")
    private String nmPet;

    @Column(name = "DT_EVENTO")
    private LocalDateTime dtEvento;

    @Column(name = "DS_TIPO_EVENTO")
    private String dsTipoEvento;

    @Column(name = "ST_STATUS")
    private String stStatus;

    @Column(name = "ID_CLINICA")
    private Long idClinica;

    @Column(name = "NM_CLINICA")
    private String nmClinica;

    protected TimelinePet() {}

    public Long getIdPet()          { return idPet; }
    public Long getIdEvento()       { return idEvento; }
    public String getNmPet()        { return nmPet; }
    public LocalDateTime getDtEvento()   { return dtEvento; }
    public String getDsTipoEvento() { return dsTipoEvento; }
    public String getStStatus()     { return stStatus; }
    public Long getIdClinica()      { return idClinica; }
    public String getNmClinica()    { return nmClinica; }
}
