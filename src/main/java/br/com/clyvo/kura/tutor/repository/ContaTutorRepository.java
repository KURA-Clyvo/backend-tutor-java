package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaTutorRepository extends JpaRepository<ContaTutor, Long> {

    Optional<ContaTutor> findByDsEmailLogin(String dsEmailLogin);

    boolean existsByDsEmailLogin(String dsEmailLogin);

    // Hibernate 6 otimiza c.tutor.idTutor → lê id_tutor de conta_tutor sem JOIN
    @Query("SELECT c.tutor.idTutor FROM ContaTutor c WHERE c.dsEmailLogin = :email")
    Optional<Long> findIdTutorByEmail(@Param("email") String email);
}
