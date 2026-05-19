package br.com.clyvo.kura.tutor.repository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ContaTutorRepository extends JpaRepository<ContaTutor, Long> {
    Optional<ContaTutor> findByDsEmailLogin(String dsEmailLogin);
    boolean existsByDsEmailLogin(String dsEmailLogin);
}
