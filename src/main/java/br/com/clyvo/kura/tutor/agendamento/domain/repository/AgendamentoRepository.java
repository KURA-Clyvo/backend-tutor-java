package br.com.clyvo.kura.tutor.agendamento.domain.repository;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository
        extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    Page<Agendamento> findByTutor_IdTutorAndStStatus(Long idTutor, StatusAgendamento stStatus,
                                                      Pageable pageable);

    /**
     * Returns future appointments (date >= now) for the given tutor, optionally
     * filtered by status. Ordered ascending by appointment date — used for the
     * "upcoming appointments" view on the tutor dashboard.
     *
     * @param idTutor  owner of the appointments
     * @param status   optional status filter; null returns all active statuses
     * @param pageable pagination and sort parameters
     */
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.tutor.idTutor = :idTutor
          AND a.dtAgendamento >= CURRENT_TIMESTAMP
          AND (:status IS NULL OR a.stStatus = :status)
        ORDER BY a.dtAgendamento ASC
    """)
    Page<Agendamento> findFuturosByTutorEStatus(@Param("idTutor") Long idTutor,
                                                @Param("status") StatusAgendamento status,
                                                Pageable pageable);

    List<Agendamento> findByPet_IdPetAndDtAgendamentoAfterOrderByDtAgendamentoAsc(
            Long idPet, LocalDateTime apartirDe);

    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.idVeterinario = :idVet
          AND a.dtAgendamento BETWEEN :inicio AND :fim
          AND a.stStatus NOT IN (
              br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento.CANCELADO,
              br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento.NAO_COMPARECEU
          )
        ORDER BY a.dtAgendamento ASC
    """)
    List<Agendamento> buscarPorVeterinarioEIntervalo(@Param("idVet") Long idVet,
                                                      @Param("inicio") LocalDateTime inicio,
                                                      @Param("fim") LocalDateTime fim);
}
