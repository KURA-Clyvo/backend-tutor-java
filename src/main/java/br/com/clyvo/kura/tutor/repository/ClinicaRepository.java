package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Clinica;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface ClinicaRepository extends Repository<Clinica, Long> {

    Optional<Clinica> findByIdClinicaAndStAtiva(Long idClinica, String stAtiva);
}
