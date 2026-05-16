package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Especie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EspecieRepository extends JpaRepository<Especie, Long> {
    Optional<Especie> findByNmEspecieIgnoreCase(String nmEspecie);
}
