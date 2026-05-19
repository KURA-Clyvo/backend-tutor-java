package br.com.clyvo.kura.tutor.auth.application;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.RefreshRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.AccountInactiveException;
import br.com.clyvo.kura.tutor.shared.exception.AccountLockedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço de autenticação por credenciais.
 *
 * Semântica HTTP correta:
 *   401 — email inexistente ou senha errada (mensagem genérica — anti-enumeração)
 *   403 — conta desativada
 *   423 — conta bloqueada (≥5 falhas consecutivas)
 *
 * Sucesso → access + refresh token; refresh hasheado e armazenado em CONTA_TUTOR.
 */
@Service
public class AuthService {

    private static final long ACCESS_EXPIRES_SECONDS  = 900L;
    private static final int  REFRESH_EXPIRATION_DAYS = 7;

    private final ContaTutorRepository contaRepo;
    private final PasswordEncoder      encoder;
    private final JwtTokenProvider     jwt;

    public AuthService(ContaTutorRepository contaRepo,
                       PasswordEncoder encoder,
                       JwtTokenProvider jwt) {
        this.contaRepo = contaRepo;
        this.encoder   = encoder;
        this.jwt       = jwt;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 1. Email inexistente → 401 genérico (não revela se o e-mail existe no sistema)
        ContaTutor conta = contaRepo.findByDsEmailLogin(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha invalidos."));

        // 2. Conta desativada → 403
        if (!conta.isAtiva()) {
            throw new AccountInactiveException();
        }

        // 3. Conta bloqueada → 423 (verificado ANTES da senha para não processar BCrypt desnecessariamente)
        if (conta.isBloqueada()) {
            throw new AccountLockedException();
        }

        // 4. Senha incorreta → incrementa tentativas, persiste estado, 401
        if (!encoder.matches(request.senha(), conta.getDsSenhaHash())) {
            conta.registrarLoginFalha();
            contaRepo.save(conta);
            throw new BadCredentialsException("E-mail ou senha invalidos.");
        }

        // 5. Credenciais válidas — reseta contador, atualiza último login
        conta.registrarLoginSucesso();

        // 6. Gera par de tokens e rotaciona refresh hasheado
        String accessToken  = jwt.gerarAccess(conta);
        String refreshToken = jwt.gerarRefresh(conta);
        conta.rotacionarRefresh(
                encoder.encode(refreshToken),
                LocalDateTime.now().plusDays(REFRESH_EXPIRATION_DAYS));

        contaRepo.save(conta);

        return TokenResponse.of(
                accessToken,
                refreshToken,
                ACCESS_EXPIRES_SECONDS,
                conta.getIdConta(),
                conta.getTutor().getNmTutor());
    }

    /**
     * Renova o par de tokens a partir de um refresh token válido.
     *
     * ROTATION: o refresh anterior é imediatamente invalidado — o cliente
     * deve persistir e usar apenas o refresh retornado nesta resposta.
     *
     * Ordem de verificação (fail-fast, mais barata primeiro):
     *   1. Assinatura/expiração JWT  → 401
     *   2. Conta existente           → 401 (mensagem genérica, anti-enumeração)
     *   3. Conta inativa             → 403
     *   4. Conta bloqueada           → 423
     *   5. Hash nulo/expirado/errado → 401
     *   6. OK → rotaciona e retorna novo par
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {

        // 1. Valida assinatura e expiração do JWT
        Long idConta = jwt.validar(request.refreshToken())
                .map(claims -> Long.parseLong(claims.getSubject()))
                .orElseThrow(() -> new BadCredentialsException("Token de refresh invalido ou expirado."));

        // 2. Busca conta — mensagem genérica evita enumeração de ids
        ContaTutor conta = contaRepo.findById(idConta)
                .orElseThrow(() -> new BadCredentialsException("Token de refresh invalido ou expirado."));

        // 3-4. Estado da conta (antes do BCrypt — evita CPU desnecessário)
        if (!conta.isAtiva())    throw new AccountInactiveException();
        if (conta.isBloqueada()) throw new AccountLockedException();

        // 5. Valida hash armazenado — cobre: hash nulo, expiração DB e token rotacionado
        if (conta.getDsRefreshTokenHash() == null
                || conta.getDtRefreshExpira() == null
                || LocalDateTime.now().isAfter(conta.getDtRefreshExpira())
                || !encoder.matches(request.refreshToken(), conta.getDsRefreshTokenHash())) {
            throw new BadCredentialsException("Token de refresh invalido ou expirado.");
        }

        // 6. Gera novo par e rotaciona hash
        String accessToken  = jwt.gerarAccess(conta);
        String refreshToken = jwt.gerarRefresh(conta);
        conta.rotacionarRefresh(
                encoder.encode(refreshToken),
                LocalDateTime.now().plusDays(REFRESH_EXPIRATION_DAYS));
        contaRepo.save(conta);

        return TokenResponse.of(accessToken, refreshToken, ACCESS_EXPIRES_SECONDS,
                conta.getIdConta(), conta.getTutor().getNmTutor());
    }

    /**
     * Invalida o refresh token do tutor autenticado.
     * Idempotente: se o email não existir no banco (não deveria acontecer com token válido),
     * simplesmente não faz nada.
     */
    @Transactional
    public void logout(String email) {
        contaRepo.findByDsEmailLogin(email).ifPresent(conta -> {
            conta.invalidarRefresh();
            contaRepo.save(conta);
        });
    }
}
