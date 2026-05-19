package br.com.clyvo.kura.tutor.service;

import br.com.clyvo.kura.tutor.dto.request.LoginRequest;
import br.com.clyvo.kura.tutor.dto.request.RegistroContaRequest;
import br.com.clyvo.kura.tutor.dto.response.TokenResponse;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Servico de autenticacao.
 *
 * Estrutura equivalente ao AutenticacaoController do projeto de aula,
 * porem com a logica movida para o service (separacao de responsabilidades).
 *
 * CORRECAO BUG-07 (QA cross-API): o registro agora exige validacao de que
 * o tutor existe no sistema antes de criar a conta — simula o fluxo de invite
 * do .NET. Em producao, validar o token UUID da tabela INVITE_TUTOR.
 */
@Service
public class AuthService {

    private static final int MAX_TENTATIVAS = 5;
    private static final long ACCESS_EXPIRES_SECONDS = 900L; // 15 minutos

    private final ContaTutorRepository contaTutorRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(ContaTutorRepository contaTutorRepository,
                       TutorRepository tutorRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.contaTutorRepository = contaTutorRepository;
        this.tutorRepository = tutorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        ContaTutor conta = contaTutorRepository.findByDsEmailLogin(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha invalidos."));

        if (conta.isBloqueada()) {
            throw new RegraDeNegocioException(
                "Conta bloqueada apos " + MAX_TENTATIVAS + " tentativas. Contate o suporte.");
        }

        if (!passwordEncoder.matches(request.senha(), conta.getDsSenhaHash())) {
            conta.incrementarTentativas();
            if (conta.getNrTentativasLogin() >= MAX_TENTATIVAS) {
                conta.setDtBloqueio(LocalDateTime.now());
            }
            contaTutorRepository.save(conta);
            throw new BadCredentialsException("E-mail ou senha invalidos.");
        }

        conta.resetarTentativas();
        conta.setDtUltimoLogin(LocalDateTime.now());
        contaTutorRepository.save(conta);

        String accessToken = jwtTokenProvider.gerarAccess(conta);
        return TokenResponse.of(accessToken, ACCESS_EXPIRES_SECONDS,
                conta.getIdConta(), conta.getTutor().getNmTutor());
    }

    @Transactional
    public TokenResponse registrar(RegistroContaRequest request) {
        Tutor tutor = tutorRepository.findById(request.idTutor())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", request.idTutor()));

        if (!tutor.isAtivo()) {
            throw new RegraDeNegocioException("Tutor inativo nao pode criar conta no portal.");
        }

        if (contaTutorRepository.existsByDsEmailLogin(request.email())) {
            throw new RegraDeNegocioException("E-mail ja cadastrado em outra conta.");
        }

        ContaTutor conta = ContaTutor.builder()
                .tutor(tutor)
                .dsEmailLogin(request.email())
                .dsSenhaHash(passwordEncoder.encode(request.senha()))
                .stAtiva("S")
                .stEmailVerificado("N")
                .nrTentativasLogin(0)
                .build();

        conta = contaTutorRepository.save(conta);

        String accessToken = jwtTokenProvider.gerarAccess(conta);
        return TokenResponse.of(accessToken, ACCESS_EXPIRES_SECONDS,
                conta.getIdConta(), tutor.getNmTutor());
    }
}
