package br.com.clyvo.kura.tutor.onboarding.application;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.entity.Consentimento;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.onboarding.api.dto.AceiteRequest;
import br.com.clyvo.kura.tutor.onboarding.api.dto.RegisterInviteRequest;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TutorResumoResponse;
import br.com.clyvo.kura.tutor.onboarding.domain.InviteTutor;
import br.com.clyvo.kura.tutor.onboarding.domain.repository.InviteTutorRepository;
import br.com.clyvo.kura.tutor.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import br.com.clyvo.kura.tutor.shared.exception.GoneException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Fluxo de onboarding por convite — cria conta, consentimentos e tokens em uma transação.
 *
 * UK_CONTA_INVITE_USED (banco) + verificação de ST_UTILIZADO (app) = defense-in-depth
 * contra race condition em POSTs duplicados concorrentes.
 */
@Service
public class OnboardingService {

    private static final long ACCESS_EXPIRES_SECONDS = 900L;
    private static final int  REFRESH_EXPIRATION_DAYS = 7;

    private final InviteTutorRepository    inviteRepo;
    private final TutorRepository          tutorRepo;
    private final ContaTutorRepository     contaRepo;
    private final ConsentimentoRepository  consentRepo;
    private final PasswordEncoder          encoder;
    private final JwtTokenProvider         jwt;
    private final ValidadorConsentimento   validador;

    public OnboardingService(InviteTutorRepository inviteRepo,
                              TutorRepository tutorRepo,
                              ContaTutorRepository contaRepo,
                              ConsentimentoRepository consentRepo,
                              PasswordEncoder encoder,
                              JwtTokenProvider jwt,
                              ValidadorConsentimento validador) {
        this.inviteRepo  = inviteRepo;
        this.tutorRepo   = tutorRepo;
        this.contaRepo   = contaRepo;
        this.consentRepo = consentRepo;
        this.encoder     = encoder;
        this.jwt         = jwt;
        this.validador   = validador;
    }

    @Transactional
    public TokenResponse registrarPorInvite(RegisterInviteRequest request,
                                             HttpServletRequest httpRequest) {
        // 1. Localiza o convite pelo token
        InviteTutor invite = inviteRepo.findByNrToken(request.token())
                .orElseThrow(() -> new NotFoundException("Convite não encontrado para o token informado."));

        // 2. Convite deve estar ativo (não cancelado pela clínica)
        if (!invite.isAtivo()) {
            throw new ConflictException("Convite cancelado.");
        }

        // 3. Verificação app-level de reuso (defense-in-depth com UK_CONTA_INVITE_USED)
        if (invite.isUtilizado()) {
            throw new ConflictException("Convite já utilizado.");
        }

        // 4. Convite dentro do prazo de validade
        if (invite.isExpirado()) {
            throw new GoneException("Convite expirado.");
        }

        // 5. Tutor ativo associado ao convite deve existir (ST_ATIVO='S')
        Tutor tutor = tutorRepo.findByIdTutorAndStAtivo(invite.getIdTutor(), "S")
                .orElseThrow(() -> new NotFoundException("Tutor", invite.getIdTutor()));

        // 6. Tutor deve ter recebido o aviso de privacidade (LGPD art. 6, VI)
        if (!tutor.temAvisoPrivacidade()) {
            throw new RegraDeNegocioException(
                    "Tutor não recebeu o aviso de privacidade. Entre em contato com a clínica.");
        }

        // 8. Monta a entidade conta
        ContaTutor conta = ContaTutor.criarPorInvite(
                tutor,
                tutor.getDsEmail(),
                encoder.encode(request.senha()),
                invite.getIdInvite());

        // 9. Persiste — UK_CONTA_INVITE_USED no banco bloqueia race condition
        try {
            conta = contaRepo.save(conta);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Convite já utilizado.");
        }

        // 10. Registra consentimentos LGPD dentro da mesma transação
        String ip = resolverIp(httpRequest);
        for (AceiteRequest aceite : request.aceites()) {
            validador.validarVersaoTermo(aceite.tipo(), aceite.versaoTermo());
            Consentimento c = aceite.aceito()
                    ? Consentimento.novoAceite(tutor, aceite.tipo(), aceite.versaoTermo(),
                                               aceite.textoTermo(), ip)
                    : Consentimento.revogacao(tutor, aceite.tipo(), aceite.versaoTermo(), ip);
            consentRepo.save(c);
        }

        // 11. Gera tokens e armazena refresh hasheado
        String accessToken  = jwt.gerarAccess(conta);
        String refreshToken = jwt.gerarRefresh(conta);
        conta.rotacionarRefresh(encoder.encode(refreshToken),
                LocalDateTime.now().plusDays(REFRESH_EXPIRATION_DAYS));
        contaRepo.save(conta);

        // 12. Resposta
        return TokenResponse.of(accessToken, refreshToken, ACCESS_EXPIRES_SECONDS,
                new TutorResumoResponse(tutor.getIdTutor(), tutor.getNmTutor()));
    }

    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
