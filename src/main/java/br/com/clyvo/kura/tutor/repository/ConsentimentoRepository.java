package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {

    List<Consentimento> findByTutor_IdTutorOrderByDtAceiteDesc(Long idTutor);

    // Busca o consentimento ativo mais recente de um tipo para um tutor
    @Query("""
            SELECT c FROM Consentimento c
            WHERE c.tutor.idTutor = :idTutor
              AND c.dsTipo = :tipo
              AND c.stAceito = 'S'
              AND c.dtRevogacao IS NULL
            ORDER BY c.dtAceite DESC
            """)
    Optional<Consentimento> buscarConsentimentoAtivo(
            @Param("idTutor") Long idTutor,
            @Param("tipo") String tipo);
}
