package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Raca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

public interface RacaRepository extends Repository<Raca, Long>, PagingAndSortingRepository<Raca, Long> {

    Page<Raca> findByEspecie_IdEspecie(Long idEspecie, Pageable pageable);
}
