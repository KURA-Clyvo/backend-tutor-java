package br.com.clyvo.kura.tutor.repository;
import br.com.clyvo.kura.tutor.entity.Raca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RacaRepository extends JpaRepository<Raca, Long> {
    Page<Raca> findByEspecieId(Long idEspecie, Pageable pageable);
    Page<Raca> findByNmRacaContainingIgnoreCase(String nmRaca, Pageable pageable);
}
