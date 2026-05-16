package br.com.clyvo.kura.tutor.service.impl;

import br.com.clyvo.kura.tutor.dto.response.PetResponse;
import br.com.clyvo.kura.tutor.dto.response.TutorResponse;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para leitura de dados do tutor e seus pets.
 *
 * <p><strong>Regra de domínio:</strong> este serviço NUNCA faz INSERT ou UPDATE
 * nas tabelas TUTOR, PET, TUTOR_PET — essas são propriedade do backend .NET.
 * Apenas leitura.
 *
 * <p>Design Patterns:
 * <ul>
 *   <li><b>Repository Pattern</b> — acesso via Spring Data JPA
 *   <li><b>DTO Pattern</b> — converte entidades JPA para records imutáveis
 *   <li><b>Cache-Aside</b> — @Cacheable em listas que mudam pouco (espécies/raças)
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class TutorService {

    private final TutorRepository tutorRepo;
    private final ContaTutorRepository contaTutorRepo;
    private final PetRepository petRepo;

    public TutorService(TutorRepository tutorRepo,
                        ContaTutorRepository contaTutorRepo,
                        PetRepository petRepo) {
        this.tutorRepo = tutorRepo;
        this.contaTutorRepo = contaTutorRepo;
        this.petRepo = petRepo;
    }

    // ── Busca de tutores ───────────────────────────────────────────────────

    /**
     * Listagem paginada com filtros opcionais.
     * Usa JPQL customizado do TutorRepository.
     */
    public Page<TutorResponse> listarComFiltros(String nome, String cidade,
                                                 String uf, String especie,
                                                 Pageable pageable) {
        return tutorRepo.buscarComFiltros(nome, cidade, uf, especie, pageable)
                .map(tutor -> {
                    boolean possuiConta = contaTutorRepo
                            .existsByDsEmailLogin(tutor.getDsEmail());
                    return TutorResponse.from(tutor, possuiConta);
                });
    }

    /**
     * Busca um tutor pelo ID. Lança 404 se não encontrado ou inativo.
     */
    public TutorResponse buscarPorId(Long id) {
        var tutor = tutorRepo.findById(id)
                .filter(t -> t.isAtivo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", id));

        boolean possuiConta = contaTutorRepo.existsByDsEmailLogin(tutor.getDsEmail());
        return TutorResponse.from(tutor, possuiConta);
    }

    // ── Pets do tutor ─────────────────────────────────────────────────────

    /**
     * Retorna os pets ativos vinculados ao tutor.
     * Usa @Cacheable para reduzir queries repetidas no app mobile.
     */
    @Cacheable(value = "petsPorTutor", key = "#idTutor + '-' + #pageable.pageNumber")
    public Page<PetResponse> listarPetsDeTutor(Long idTutor, Pageable pageable) {
        // Valida que o tutor existe
        if (!tutorRepo.existsById(idTutor)) {
            throw new RecursoNaoEncontradoException("Tutor", idTutor);
        }

        return petRepo.findPetsByTutorId(idTutor, pageable)
                .map(PetResponse::from);
    }
}
