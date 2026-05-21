package br.com.clyvo.kura.tutor.auth.domain.repository;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaTutorRepository extends JpaRepository<ContaTutor, Long> {

    Optional<ContaTutor> findByDsEmailLogin(String dsEmailLogin);

    boolean existsByDsEmailLogin(String dsEmailLogin);

    @Query("SELECT c.tutor.idTutor FROM ContaTutor c WHERE c.dsEmailLogin = :email")
    Optional<Long> findIdTutorByEmail(@Param("email") String email);
}
