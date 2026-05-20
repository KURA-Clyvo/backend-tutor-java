package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetComUltimoEventoProjection;
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

    /**
     * Returns all active pets belonging to a tutor together with the date of
     * their most recent clinical event (from VW_TIMELINE_PET). Pets with no
     * recorded events are still returned with a null dtUltimoEvento — used by
     * the tutor dashboard to highlight pets that need attention.
     *
     * @param idTutor  owner tutor
     * @param pageable pagination parameters
     */
    @Query(
        value = """
            SELECT new br.com.clyvo.kura.tutor.tutor.api.dto.PetComUltimoEventoProjection(
                p.idPet, p.nmPet, MAX(t.dtEvento)
            )
            FROM Pet p
            JOIN p.tutorPets tp
            LEFT JOIN br.com.clyvo.kura.tutor.timeline.domain.TimelinePet t
                   ON t.idPet = p.idPet
            WHERE tp.tutor.idTutor = :idTutor AND p.stAtivo = 'S'
            GROUP BY p.idPet, p.nmPet
        """,
        countQuery = """
            SELECT COUNT(DISTINCT p.idPet)
            FROM Pet p
            JOIN p.tutorPets tp
            WHERE tp.tutor.idTutor = :idTutor AND p.stAtivo = 'S'
        """
    )
    Page<PetComUltimoEventoProjection> findPetsComUltimoEvento(@Param("idTutor") Long idTutor,
                                                                Pageable pageable);
}
