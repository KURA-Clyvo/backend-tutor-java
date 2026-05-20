package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends Repository<Pet, Long>, PagingAndSortingRepository<Pet, Long> {

    Optional<Pet> findByIdPetAndStAtivo(Long idPet, String stAtivo);

    @Query("""
        SELECT p FROM Pet p
        JOIN p.tutorPets tp
        WHERE tp.tutor.idTutor = :idTutor
          AND p.stAtivo = 'S'
    """)
    Page<Pet> findAtivosByIdTutor(@Param("idTutor") Long idTutor, Pageable pageable);

    @Query("""
        SELECT COUNT(p) FROM Pet p
        JOIN p.tutorPets tp
        WHERE p.idPet = :idPet AND tp.tutor.idTutor = :idTutor
    """)
    long countVinculo(@Param("idPet") Long idPet, @Param("idTutor") Long idTutor);
}
