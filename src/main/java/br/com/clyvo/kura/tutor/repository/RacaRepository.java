package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Raca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RacaRepository extends Repository<Raca, Long>, PagingAndSortingRepository<Raca, Long> {

    // JOIN FETCH especie evita LazyInitializationException com open-in-view=false
    @Query("SELECT r FROM Raca r JOIN FETCH r.especie e WHERE e.idEspecie = :id ORDER BY r.nmRaca")
    List<Raca> findByEspecie_IdEspecie(@Param("id") Long id);

    @Query(value      = "SELECT r FROM Raca r JOIN FETCH r.especie ORDER BY r.especie.nmEspecie, r.nmRaca",
           countQuery = "SELECT COUNT(r) FROM Raca r")
    Page<Raca> findAll(Pageable pageable);
}
