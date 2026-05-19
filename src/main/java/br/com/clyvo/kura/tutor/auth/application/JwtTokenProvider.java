package br.com.clyvo.kura.tutor.auth.application;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Provedor de tokens JWT para o domínio de autenticação do tutor.
 *
 * Algoritmo: HS512 (HMAC-SHA-512) — exige chave ≥ 64 bytes.
 * Validação de chave ocorre no boot via {@link #validarChaveNoBoot()}.
 *
 * Tokens NUNCA são logados — expor token em log equivale a expor senha.
 *
 * Substituição de JWTUtil com nova assinatura conforme plano v5:
 *   - gerarAccess(ContaTutor) — subject = idConta, exp 15 min
 *   - gerarRefresh(ContaTutor) — subject = idConta, exp 7 dias
 *   - validar(String) — Optional<Claims> (vazio = inválido ou expirado)
 *   - extrairIdConta(String) — Long do subject
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey chave;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${kura.jwt.secret}") String secret,
            @Value("${kura.jwt.access-expiration-minutes:15}") int accessMinutes,
            @Value("${kura.jwt.refresh-expiration-days:7}") int refreshDays) {

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT_SECRET deve ter no mínimo 64 bytes para HS512. Encontrado: " + bytes.length + " bytes.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.accessExpirationMs  = (long) accessMinutes * 60 * 1000;
        this.refreshExpirationMs = (long) refreshDays   * 24 * 60 * 60 * 1000;
    }

    /**
     * Validação explícita no boot — falha rápida antes de aceitar requests.
     * JJWT também valida, mas a mensagem aqui é mais clara para quem opera o serviço.
     */
    @PostConstruct
    void validarChaveNoBoot() {
        // chave já validada no construtor; este método documenta a intenção de fail-fast
        try {
            Jwts.parser().verifyWith(chave).build();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar JwtTokenProvider: " + e.getMessage(), e);
        }
    }

    /**
     * Gera access token JWT. Expira em {@code kura.jwt.access-expiration-minutes} (padrão: 15 min).
     * Subject = idConta (Long como String) para lookup eficiente sem join.
     */
    public String gerarAccess(ContaTutor conta) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(conta.getIdConta()))
                .claim(CLAIM_EMAIL, conta.getDsEmailLogin())
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + accessExpirationMs))
                .signWith(chave, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Gera refresh token JWT. Expira em {@code kura.jwt.refresh-expiration-days} (padrão: 7 dias).
     * Mesmo subject do access token — rotation feita em T11 via hash BCrypt.
     */
    public String gerarRefresh(ContaTutor conta) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(conta.getIdConta()))
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + refreshExpirationMs))
                .signWith(chave, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Valida e parseia o token.
     *
     * @return Optional com Claims se o token for válido e não expirado;
     *         Optional.empty() para token malformado, assinatura inválida ou expirado.
     */
    public Optional<Claims> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extrai o idConta (Long) do subject do token.
     * Assume que o token já foi validado por {@link #validar(String)}.
     *
     * @throws JwtException se o token for inválido ou o subject não for um Long
     */
    public Long extrairIdConta(String token) {
        return validar(token)
                .map(claims -> Long.parseLong(claims.getSubject()))
                .orElseThrow(() -> new JwtException("Token inválido ou expirado."));
    }
}
