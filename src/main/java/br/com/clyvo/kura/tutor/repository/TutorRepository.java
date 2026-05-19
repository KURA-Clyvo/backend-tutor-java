package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TutorRepository extends Repository<Tutor, Long>, PagingAndSortingRepository<Tutor, Long> {

    Optional<Tutor> findByIdTutorAndStAtivo(Long idTutor, String stAtivo);
    Optional<Tutor> findByDsEmailAndStAtivo(String dsEmail, String stAtivo);
    Optional<Tutor> findByNrCpfAndStAtivo(String nrCpf, String stAtivo);

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
