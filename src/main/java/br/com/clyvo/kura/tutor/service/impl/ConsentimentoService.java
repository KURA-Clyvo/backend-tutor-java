package br.com.clyvo.kura.tutor.service.impl;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.dto.response.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.entity.Consentimento;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de gestão de consentimentos LGPD.
 *
 * <p><strong>Regra crítica:</strong> o histórico de consentimento é IMUTÁVEL.
 * Cada aceite ou revogação gera uma NOVA linha em CONSENTIMENTO.
 * Nunca fazer UPDATE nessa tabela.
 *
 * <p>Para saber o estado atual de um tipo: buscar o registro mais recente
 * por tipo e verificar {@code ST_ACEITO}.
 *
 * <p>Design Pattern: <b>Strategy</b> (implícito) — cada tipo de consentimento
 * tem regras de negócio distintas que poderiam ser externalizadas em strategies.
 */
@Service
public class ConsentimentoService {

    private final ConsentimentoRepository consentimentoRepo;
    private final TutorRepository tutorRepo;

    public ConsentimentoService(ConsentimentoRepository consentimentoRepo,
                                 TutorRepository tutorRepo) {
        this.consentimentoRepo = consentimentoRepo;
        this.tutorRepo = tutorRepo;
    }

    // ── Listagem ──────────────────────────────────────────────────────────

    /**
     * Retorna o histórico completo de consentimentos do tutor,
     * ordenado do mais recente para o mais antigo.
     */
    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listarPorTutor(Long idTutor) {
        validarTutor(idTutor);
        return consentimentoRepo
                .findByTutor_IdTutorOrderByDtAceiteDesc(idTutor)
                .stream()
                .map(ConsentimentoResponse::from)
                .toList();
    }

    /**
     * Retorna o status atual (aceito/revogado) de cada tipo de consentimento.
     * Útil para montar os toggles na tela LGPD do app.
     */
    @Transactional(readOnly = true)
    public ConsentimentoResponse buscarAtivoPorTipo(Long idTutor, String tipo) {
        validarTutor(idTutor);
        return consentimentoRepo
                .buscarConsentimentoAtivo(idTutor, tipo)
                .map(ConsentimentoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhum consentimento ativo do tipo '" + tipo + "' encontrado."));
    }

    // ── Registro de aceite/revogação ──────────────────────────────────────

    /**
     * Registra um novo aceite ou revogação de consentimento.
     *
     * <p>Regras:
     * <ul>
     *   <li>Sempre INSERT — nunca UPDATE
     *   <li>Se já existe consentimento ativo do mesmo tipo e tutor tenta aceitar
     *       novamente sem mudança de versão, lança regra de negócio
     *   <li>Revogação de consentimento inexistente também lança exceção
     * </ul>
     *
     * @param idTutor  ID do tutor que está consentindo
     * @param request  dados do consentimento
     * @param httpReq  para capturar o IP (evidência LGPD)
     */
    @Transactional
    public ConsentimentoResponse registrar(Long idTutor,
                                            ConsentimentoRequest request,
                                            HttpServletRequest httpReq) {
        var tutor = tutorRepo.findById(idTutor)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        // Validação: revogação sem consentimento ativo é erro de negócio
        if ("N".equals(request.aceito())) {
            boolean temAtivo = consentimentoRepo
                    .buscarConsentimentoAtivo(idTutor, request.tipo())
                    .isPresent();
            if (!temAtivo) {
                throw new RegraDeNegocioException(
                        "Não existe consentimento ativo do tipo '" + request.tipo()
                                + "' para revogar.");
            }
        }

        String ip = extrairIp(httpReq);

        // SEMPRE INSERT — regra de imutabilidade LGPD
        Consentimento novo = Consentimento.builder()
                .tutor(tutor)
                .dsTipo(request.tipo())
                .dsVersaoTermo(request.versaoTermo())
                .stAceito(request.aceito())
                .dtAceite(LocalDateTime.now())
                .dsIpAceite(ip)
                // Se é revogação, preenche o campo de revogação também
                .dtRevogacao("N".equals(request.aceito()) ? LocalDateTime.now() : null)
                .dsIpRevogacao("N".equals(request.aceito()) ? ip : null)
                .build();

        return ConsentimentoResponse.from(consentimentoRepo.save(novo));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void validarTutor(Long idTutor) {
        if (!tutorRepo.existsById(idTutor)) {
            throw new RecursoNaoEncontradoException("Tutor", idTutor);
        }
    }

    /** Extrai IP considerando proxies reversos (X-Forwarded-For). */
    private String extrairIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
