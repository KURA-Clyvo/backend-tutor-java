package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Especie;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface EspecieRepository extends Repository<Especie, Long> {

    @Query("SELECT e FROM Especie e ORDER BY e.nmEspecie")
    List<Especie> findAll();
}
