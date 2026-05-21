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
          AND (:nomePat IS NULL OR LOWER(t.nmTutor) LIKE :nomePat ESCAPE '/')
          AND (:cidadePat IS NULL OR LOWER(t.nmCidade) LIKE :cidadePat ESCAPE '/')
          AND (:uf IS NULL OR t.sgUf = :uf)
    """)
    Page<Tutor> buscarComFiltrosQuery(@Param("nomePat") String nomePat,
                                       @Param("cidadePat") String cidadePat,
                                       @Param("uf") String uf,
                                       Pageable pageable);

    default Page<Tutor> buscarComFiltros(String nome, String cidade, String uf, Pageable pageable) {
        String nomePat    = nome   != null ? "%" + nome.toLowerCase()   + "%" : null;
        String cidadePat  = cidade != null ? "%" + cidade.toLowerCase() + "%" : null;
        return buscarComFiltrosQuery(nomePat, cidadePat, uf, pageable);
    }
}
