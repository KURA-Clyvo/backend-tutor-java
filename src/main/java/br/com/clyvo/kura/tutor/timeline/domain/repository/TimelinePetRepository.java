package br.com.clyvo.kura.tutor.timeline.domain.repository;

import br.com.clyvo.kura.tutor.timeline.domain.TimelinePet;
import br.com.clyvo.kura.tutor.timeline.domain.TimelinePetId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TimelinePetRepository
        extends Repository<TimelinePet, TimelinePetId>,
                PagingAndSortingRepository<TimelinePet, TimelinePetId> {

    @Query(value      = "SELECT t FROM TimelinePet t WHERE t.idPet = :idPet",
           countQuery = "SELECT COUNT(t) FROM TimelinePet t WHERE t.idPet = :idPet")
    Page<TimelinePet> findByIdPet(@Param("idPet") Long idPet, Pageable pageable);

    Optional<TimelinePet> findByIdPetAndIdEvento(Long idPet, Long idEvento);

    long countByIdPet(Long idPet);
}
