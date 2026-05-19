package br.com.clyvo.kura.tutor.service;

import br.com.clyvo.kura.tutor.dto.request.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.dto.response.ConsentimentoResponse;
import br.com.clyvo.kura.tutor.entity.Consentimento;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servico de consentimentos LGPD.
 *
 * REGRA ABSOLUTA: nunca UPDATE — sempre INSERT.
 * Historico e evidencia legal (ANPD) e deve ser imutavel.
 * Estado atual = registro mais recente com stAceito='S' e dtRevogacao NULL.
 */
@Service
public class ConsentimentoService {

    private final ConsentimentoRepository consentimentoRepository;
    private final TutorRepository tutorRepository;
    private final ValidadorConsentimento validador;

    public ConsentimentoService(ConsentimentoRepository consentimentoRepository,
                                TutorRepository tutorRepository,
                                ValidadorConsentimento validador) {
        this.consentimentoRepository = consentimentoRepository;
        this.tutorRepository = tutorRepository;
        this.validador = validador;
    }

    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listar(Long idTutor) {
        validarTutor(idTutor);
        return consentimentoRepository
                .findByTutor_IdTutorOrderByDtAceiteDesc(idTutor)
                .stream().map(ConsentimentoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ConsentimentoResponse> buscarAtivo(Long idTutor, String tipo) {
        return consentimentoRepository
                .buscarAtivo(idTutor, tipo)
                .map(ConsentimentoResponse::fromEntity);
    }

    @Transactional
    public ConsentimentoResponse registrar(Long idTutor,
                                           ConsentimentoRequest request,
                                           String ipCliente) {
        Tutor tutor = validarTutor(idTutor);

        // Validacoes LGPD
        validador.validarAvisoPrivacidade(tutor);
        validador.validarVersaoTermo(request);

        // Nao pode revogar o que nao existe
        if ("N".equals(request.aceito())) {
            boolean ativo = consentimentoRepository
                    .buscarAtivo(idTutor, request.tipo().toDbValue()).isPresent();
            if (!ativo) {
                throw new RegraDeNegocioException(
                    "Nao existe consentimento ativo do tipo "
                    + request.tipo() + " para revogar.");
            }
        }

        // SEMPRE INSERT — nunca UPDATE (LGPD: historico imutavel)
        Consentimento novo = new Consentimento();
        novo.setTutor(tutor);
        novo.setDsTipo(request.tipo().toDbValue());
        novo.setDsVersaoTermo(request.versaoTermo());
        novo.setDsTextoTermo(request.textoTermo());
        novo.setStAceito(request.aceito());
        novo.setDtAceite(LocalDateTime.now());
        novo.setDsIpAceite(ipCliente);

        if ("N".equals(request.aceito())) {
            novo.setDtRevogacao(LocalDateTime.now());
            novo.setDsIpRevogacao(ipCliente);
        }

        return ConsentimentoResponse.fromEntity(consentimentoRepository.save(novo));
    }

    private Tutor validarTutor(Long idTutor) {
        return tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));
    }
}
