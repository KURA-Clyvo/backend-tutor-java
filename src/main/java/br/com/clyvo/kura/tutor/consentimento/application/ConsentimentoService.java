package br.com.clyvo.kura.tutor.consentimento.application;

import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import br.com.clyvo.kura.tutor.consentimento.domain.IdempotencyKey;
import br.com.clyvo.kura.tutor.consentimento.domain.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.consentimento.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de consentimentos LGPD.
 *
 * REGRA ABSOLUTA: nunca UPDATE — sempre INSERT.
 * Histórico é evidência legal (ANPD) e deve ser imutável.
 * Estado atual = registro mais recente com stAceito='S' e dtRevogacao IS NULL.
 */
@Service
public class ConsentimentoService {

    private static final String RESOURCE_CONSENTIMENTO = "CONSENTIMENTO";

    private final ConsentimentoRepository consentimentoRepository;
    private final TutorRepository         tutorRepository;
    private final ContaTutorRepository    contaTutorRepository;
    private final IdempotencyService      idempotencyService;
    private final ValidadorConsentimento  validador;

    public ConsentimentoService(ConsentimentoRepository consentimentoRepository,
                                TutorRepository tutorRepository,
                                ContaTutorRepository contaTutorRepository,
                                IdempotencyService idempotencyService,
                                ValidadorConsentimento validador) {
        this.consentimentoRepository = consentimentoRepository;
        this.tutorRepository         = tutorRepository;
        this.contaTutorRepository    = contaTutorRepository;
        this.idempotencyService      = idempotencyService;
        this.validador               = validador;
    }

    /** Resultado de registrarComIdempotencia — indica se foi criado (201) ou retornado do cache (200). */
    public record RegistroResult(ConsentimentoResponse response, boolean criado) {}

    // ── Métodos para ConsentimentoController ─────────────────────────────────

    /**
     * Retorna o último consentimento de cada tipo para o tutor (estado atual por tipo).
     * Faz verificação de propriedade: usuário autenticado só acessa seus próprios dados.
     */
    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listarUltimosPorTipo(Long idTutor, String emailAutenticado) {
        verificarOwnership(idTutor, emailAutenticado);
        return consentimentoRepository.findUltimosPorTipo(idTutor)
                .stream().map(ConsentimentoResponse::fromEntity).toList();
    }

    /**
     * Registra aceite ou revogação com idempotência obrigatória.
     * Mesma TX: INSERT em CONSENTIMENTO + INSERT em IDEMPOTENCY_KEY.
     * Se a key já existe e não expirou: retorna o registro original sem nova inserção.
     */
    @Transactional
    public RegistroResult registrarComIdempotencia(Long idTutor, ConsentimentoRequest request,
                                                   String ipCliente, String idempotencyKey,
                                                   String emailAutenticado) {
        verificarOwnership(idTutor, emailAutenticado);

        // Verificação de idempotência — busca chave válida (não expirada)
        Optional<IdempotencyKey> chaveExistente =
                idempotencyService.buscar(idempotencyKey, RESOURCE_CONSENTIMENTO);

        if (chaveExistente.isPresent()) {
            Consentimento original = consentimentoRepository
                    .findById(chaveExistente.get().getIdResourceCriado())
                    .orElseThrow(() -> new NotFoundException(
                            "Consentimento", chaveExistente.get().getIdResourceCriado()));
            return new RegistroResult(ConsentimentoResponse.fromEntity(original), false);
        }

        // Criação do novo consentimento
        Consentimento novo = criarConsentimento(idTutor, request, ipCliente);

        // Registro da chave de idempotência na mesma transação
        idempotencyService.registrar(idempotencyKey, RESOURCE_CONSENTIMENTO, novo.getIdConsentimento());

        return new RegistroResult(ConsentimentoResponse.fromEntity(novo), true);
    }

    // ── Métodos para LgpdController (histórico completo — sem ownership check) ──

    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listar(Long idTutor, String emailAutenticado) {
        verificarOwnership(idTutor, emailAutenticado);
        validarTutor(idTutor);
        return consentimentoRepository
                .findByTutor_IdTutorOrderByDtAceiteDesc(idTutor)
                .stream().map(ConsentimentoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ConsentimentoResponse> buscarAtivo(Long idTutor, String tipo,
                                                        String emailAutenticado) {
        verificarOwnership(idTutor, emailAutenticado);
        return consentimentoRepository.buscarAtivo(idTutor, tipo)
                .stream().findFirst()
                .map(ConsentimentoResponse::fromEntity);
    }

    /** Usado pelo LgpdController — sem idempotência obrigatória. */
    @Transactional
    public ConsentimentoResponse registrar(Long idTutor, ConsentimentoRequest request,
                                           String ipCliente, String emailAutenticado) {
        verificarOwnership(idTutor, emailAutenticado);
        return ConsentimentoResponse.fromEntity(criarConsentimento(idTutor, request, ipCliente));
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Consentimento criarConsentimento(Long idTutor, ConsentimentoRequest request,
                                             String ipCliente) {
        Tutor tutor = validarTutor(idTutor);
        validador.validarAvisoPrivacidade(tutor);
        validador.validarVersaoTermo(request);

        if ("N".equals(request.aceito())) {
            boolean ativo = !consentimentoRepository
                    .buscarAtivo(idTutor, request.tipo().toDbValue()).isEmpty();
            if (!ativo) {
                throw new RegraDeNegocioException(
                    "Não existe consentimento ativo do tipo " + request.tipo() + " para revogar.");
            }
        }

        Consentimento novo = "S".equals(request.aceito())
                ? Consentimento.novoAceite(tutor, request.tipo(), request.versaoTermo(),
                                           request.textoTermo(), ipCliente)
                : Consentimento.revogacao(tutor, request.tipo(), request.versaoTermo(), ipCliente);

        return consentimentoRepository.save(novo);
    }

    private Tutor validarTutor(Long idTutor) {
        return tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));
    }

    private void verificarOwnership(Long idTutor, String emailAutenticado) {
        Long idTutorAutenticado = contaTutorRepository.findIdTutorByEmail(emailAutenticado)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o e-mail autenticado."));
        if (!idTutorAutenticado.equals(idTutor)) {
            throw new ForbiddenException("Acesso negado: você só pode acessar seus próprios consentimentos.");
        }
    }
}
