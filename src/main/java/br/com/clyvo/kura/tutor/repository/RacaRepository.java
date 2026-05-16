package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Raca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RacaRepository extends JpaRepository<Raca, Long> {
    // Query Method — busca raças por espécie com paginação
    Page<Raca> findByEspecie_IdEspecie(Long idEspecie, Pageable pageable);
    // Query Method — busca por nome parcial
    Page<Raca> findByNmRacaContainingIgnoreCase(String nmRaca, Pageable pageable);
}
