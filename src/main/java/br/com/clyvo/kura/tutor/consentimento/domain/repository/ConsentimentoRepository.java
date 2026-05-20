package br.com.clyvo.kura.tutor.consentimento.domain.repository;

import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {

    List<Consentimento> findByTutor_IdTutorOrderByDtAceiteDesc(Long idTutor);

    @Query("""
        SELECT c FROM Consentimento c
        WHERE c.tutor.idTutor = :idTutor
          AND c.dsTipo = :tipo
          AND c.stAceito = 'S'
          AND c.dtRevogacao IS NULL
        ORDER BY c.dtAceite DESC
    """)
    List<Consentimento> buscarAtivo(@Param("idTutor") Long idTutor,
                                    @Param("tipo") String tipo);

    // T21: retorna o registro mais recente de cada tipo para o tutor
    @Query("""
        SELECT c FROM Consentimento c
        WHERE c.tutor.idTutor = :idTutor
          AND c.dtAceite = (
            SELECT MAX(c2.dtAceite) FROM Consentimento c2
            WHERE c2.tutor.idTutor = c.tutor.idTutor AND c2.dsTipo = c.dsTipo
          )
    """)
    List<Consentimento> findUltimosPorTipo(@Param("idTutor") Long idTutor);
}
