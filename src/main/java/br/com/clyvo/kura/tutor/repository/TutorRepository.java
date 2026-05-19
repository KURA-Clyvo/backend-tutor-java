package br.com.clyvo.kura.tutor.repository;
import br.com.clyvo.kura.tutor.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Optional<Tutor> findByNrCpf(String nrCpf);
    Optional<Tutor> findByDsEmailAndStAtivo(String dsEmail, String stAtivo);

    @Query("""
        SELECT DISTINCT t FROM Tutor t
        WHERE t.stAtivo = 'S'
          AND (:nome IS NULL OR LOWER(t.nmTutor) LIKE LOWER(CONCAT('%',:nome,'%')))
          AND (:cidade IS NULL OR LOWER(t.nmCidade) LIKE LOWER(CONCAT('%',:cidade,'%')))
          AND (:uf IS NULL OR t.sgUf = :uf)
    """)
    Page<Tutor> buscarComFiltros(@Param("nome") String nome,
                                  @Param("cidade") String cidade,
                                  @Param("uf") String uf,
                                  Pageable pageable);
}
