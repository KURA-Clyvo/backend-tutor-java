package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    // Query Method — pets ativos de um tutor (via tabela associativa)
    @Query("""
            SELECT p FROM Pet p
            JOIN p.tutorPets tp
            WHERE tp.tutor.idTutor = :idTutor
              AND p.stAtivo = 'S'
            """)
    Page<Pet> findPetsByTutorId(@Param("idTutor") Long idTutor, Pageable pageable);

    // Query Method — pets por espécie
    @Query("""
            SELECT p FROM Pet p
            JOIN p.tutorPets tp
            WHERE tp.tutor.idTutor = :idTutor
              AND p.especie.idEspecie = :idEspecie
              AND p.stAtivo = 'S'
            """)
    Page<Pet> findPetsByTutorIdAndEspecie(
            @Param("idTutor") Long idTutor,
            @Param("idEspecie") Long idEspecie,
            Pageable pageable);
}
