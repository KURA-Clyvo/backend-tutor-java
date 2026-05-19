package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Veterinario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface VeterinarioRepository extends Repository<Veterinario, Long>, PagingAndSortingRepository<Veterinario, Long> {

    Optional<Veterinario> findByIdVeterinarioAndStAtivo(Long idVeterinario, String stAtivo);
    Page<Veterinario> findByClinica_IdClinicaAndStAtivo(Long idClinica, String stAtivo, Pageable pageable);
}
