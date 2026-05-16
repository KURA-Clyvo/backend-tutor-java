package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    // ── Spring JPA Query Methods ──────────────────────────────────────────

    // Query Method — #1
    Optional<Tutor> findByNrCpf(String nrCpf);

    // Query Method — #2
    Optional<Tutor> findByDsEmailAndStAtivo(String dsEmail, String stAtivo);

    // Query Method — #3
    Page<Tutor> findByNmTutorContainingIgnoreCaseAndStAtivo(
            String nmTutor, String stAtivo, Pageable pageable);

    // ── JPQL @Query customizado ───────────────────────────────────────────

    /**
     * Busca tutores com filtros opcionais: nome, cidade, estado e espécie do pet.
     * Os parâmetros NULL são ignorados na query (comportamento IS NULL OR ... IS NULL).
     *
     * <p>JPQL customizado #1 — exigido pelo briefing FIAP.
     */
    @Query("""
            SELECT DISTINCT t FROM Tutor t
            JOIN t.tutorPets tp
            JOIN tp.pet p
            JOIN p.especie e
            WHERE t.stAtivo = 'S'
              AND (:nome IS NULL OR LOWER(t.nmTutor) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:cidade IS NULL OR LOWER(t.nmCidade) LIKE LOWER(CONCAT('%', :cidade, '%')))
              AND (:uf IS NULL OR t.sgUf = :uf)
              AND (:especie IS NULL OR LOWER(e.nmEspecie) LIKE LOWER(CONCAT('%', :especie, '%')))
            """)
    Page<Tutor> buscarComFiltros(
            @Param("nome") String nome,
            @Param("cidade") String cidade,
            @Param("uf") String uf,
            @Param("especie") String especie,
            Pageable pageable);

    /**
     * Busca tutores que possuem vacinas vencendo nos próximos N dias.
     * Alimenta alertas da Luna.
     *
     * <p>JPQL customizado #2 — exigido pelo briefing FIAP.
     */
    @Query("""
            SELECT DISTINCT t FROM Tutor t
            JOIN t.tutorPets tp
            JOIN tp.pet p
            WHERE t.stAtivo = 'S'
              AND tp.stPrincipal = 'S'
              AND EXISTS (
                SELECT 1 FROM Agendamento a
                WHERE a.tutor = t
                  AND a.stStatus IN ('AGENDADO', 'CONFIRMADO')
                  AND a.dtAgendamento >= CURRENT_TIMESTAMP
              )
            """)
    Page<Tutor> buscarTutoresComAgendamentosAtivos(Pageable pageable);
}
