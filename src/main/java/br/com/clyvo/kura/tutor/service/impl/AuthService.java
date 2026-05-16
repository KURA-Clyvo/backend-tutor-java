package br.com.clyvo.kura.tutor.service.impl;

import br.com.clyvo.kura.tutor.dto.request.LoginRequest;
import br.com.clyvo.kura.tutor.dto.request.RegistroContaRequest;
import br.com.clyvo.kura.tutor.dto.response.TokenResponse;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Serviço responsável por autenticação e registro de conta do tutor.
 *
 * <p>Design Patterns aplicados:
 * <ul>
 *   <li><b>Facade</b> — abstrai AuthenticationManager, JwtService e PasswordEncoder
 *       em uma interface simples para o controller.
 *   <li><b>DTO Pattern</b> — nunca expõe entidades JPA. Retorna apenas {@link TokenResponse}.
 * </ul>
 */
@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final ContaTutorRepository contaTutorRepo;
    private final TutorRepository tutorRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${kura.jwt.expiration-ms}")
    private long expirationMs;

    // Limite de tentativas antes do bloqueio
    private static final int MAX_TENTATIVAS = 5;

    public AuthService(AuthenticationManager authManager,
                       JwtService jwtService,
                       ContaTutorRepository contaTutorRepo,
                       TutorRepository tutorRepo,
                       PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.contaTutorRepo = contaTutorRepo;
        this.tutorRepo = tutorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login ─────────────────────────────────────────────────────────────

    /**
     * Autentica o tutor e retorna o token JWT.
     *
     * <p>Fluxo:
     * <ol>
     *   <li>Verifica se conta existe e está ativa
     *   <li>Verifica bloqueio por tentativas
     *   <li>Delega autenticação ao Spring Security (valida senha BCrypt)
     *   <li>Reseta tentativas, atualiza último login
     *   <li>Gera e retorna JWT com claims do tutor
     * </ol>
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        var conta = contaTutorRepo.findByDsEmailLogin(request.email())
                .orElseThrow(() -> new RegraDeNegocioException("E-mail ou senha inválidos."));

        // Verifica bloqueio
        if (conta.isBloqueada()) {
            throw new RegraDeNegocioException(
                    "Conta bloqueada por excesso de tentativas. Contate o suporte.");
        }

        // Delega ao Spring Security — lança BadCredentialsException se senha errada
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
        } catch (Exception e) {
            // Incrementa tentativas e possivelmente bloqueia
            conta.incrementarTentativas();
            if (conta.getNrTentativasLogin() >= MAX_TENTATIVAS) {
                conta.setDtBloqueio(LocalDateTime.now());
            }
            contaTutorRepo.save(conta);
            throw e; // relança — o handler converte em 401
        }

        // Login bem-sucedido — reseta tentativas e atualiza último acesso
        conta.resetarTentativas();
        conta.setDtUltimoLogin(LocalDateTime.now());
        contaTutorRepo.save(conta);

        // Gera JWT com claims extras para o app não precisar de GET adicional
        var claims = Map.<String, Object>of(
                "idConta", conta.getIdConta(),
                "idTutor", conta.getTutor().getIdTutor()
        );
        String token = jwtService.gerarToken(conta.getDsEmailLogin(), claims);

        return TokenResponse.of(
                token,
                expirationMs,
                conta.getIdConta(),
                conta.getTutor().getIdTutor(),
                conta.getTutor().getNmTutor()
        );
    }

    // ── Registro de conta ──────────────────────────────────────────────────

    /**
     * Cria a CONTA_TUTOR para um tutor já cadastrado pela clínica (.NET).
     *
     * <p>Regras de negócio:
     * <ul>
     *   <li>Tutor deve existir em TUTOR (cadastrado pelo .NET)
     *   <li>Tutor não pode já ter conta (UK em ID_TUTOR)
     *   <li>E-mail não pode estar em uso em outra conta
     * </ul>
     */
    @Transactional
    public TokenResponse registrar(RegistroContaRequest request) {
        // Tutor deve existir (cadastrado pelo .NET)
        Tutor tutor = tutorRepo.findById(request.idTutor())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", request.idTutor()));

        if (!tutor.isAtivo()) {
            throw new RegraDeNegocioException("Tutor inativo. Contate a clínica.");
        }

        // Não pode já ter conta
        if (contaTutorRepo.existsByDsEmailLogin(request.email())) {
            throw new RegraDeNegocioException("Este e-mail já está em uso por outra conta.");
        }

        // Cria a conta com senha BCrypt
        ContaTutor novaConta = ContaTutor.builder()
                .tutor(tutor)
                .dsEmailLogin(request.email())
                .dsSenhaHash(passwordEncoder.encode(request.senha()))
                .dtCriacao(LocalDateTime.now())
                .stAtiva("S")
                .stEmailVerificado("N") // requer verificação de e-mail (fase 2)
                .nrTentativasLogin(0)
                .build();

        contaTutorRepo.save(novaConta);

        // Faz login automático após registro — retorna JWT direto
        return login(new LoginRequest(request.email(), request.senha()));
    }
}
