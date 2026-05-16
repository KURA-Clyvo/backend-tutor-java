package br.com.clyvo.kura.tutor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Serviço responsável por gerar, validar e extrair dados de tokens JWT.
 *
 * <p>Algoritmo: HS256 com chave simétrica configurada em {@code kura.jwt.secret}.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${kura.jwt.secret}") String secret,
            @Value("${kura.jwt.expiration-ms}") long expirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Gera um token JWT para o e-mail fornecido.
     *
     * @param email    subject do token (e-mail de login)
     * @param claims   claims extras a incluir (ex: idConta, idTutor)
     * @return token JWT assinado
     */
    public String gerarToken(String email, Map<String, Object> claims) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrai o e-mail (subject) de um token válido.
     *
     * @throws JwtException se o token for inválido ou expirado
     */
    public String extrairEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    /** Valida assinatura e expiração do token. */
    public boolean isTokenValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Interno ──────────────────────────────────────────────────────────

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
