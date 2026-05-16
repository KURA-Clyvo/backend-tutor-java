package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Agendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Query Method — busca por tutor com paginação
    Page<Agendamento> findByTutor_IdTutorAndStStatus(Long idTutor, String stStatus, Pageable pageable);

    // Query Method — busca agendamentos futuros de um pet
    List<Agendamento> findByPet_IdPetAndDtAgendamentoAfterOrderByDtAgendamentoAsc(
            Long idPet, LocalDateTime apartirDe);

    // JPQL — agendamentos de um veterinário em um intervalo de datas
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.idVeterinario = :idVet
              AND a.dtAgendamento BETWEEN :inicio AND :fim
              AND a.stStatus NOT IN ('CANCELADO', 'NAO_COMPARECEU')
            ORDER BY a.dtAgendamento ASC
            """)
    List<Agendamento> buscarPorVeterinarioEIntervalo(
            @Param("idVet") Long idVeterinario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
