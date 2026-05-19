package br.com.clyvo.kura.tutor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitario JWT — baseado no JWTUtil do projeto de aula (projeto-musica).
 *
 * DIFERENCAS EM RELACAO AO PROJETO AULA:
 * - Aula: gera chave randomica em memoria (Jwts.SIG.HS256.key().build())
 *         → chave muda a cada restart, tokens anteriores ficam invalidos
 * - KURA: chave fixa configurada via application.properties (kura.jwt.secret)
 *         → tokens sobrevivem a restarts (obrigatório para producao)
 *
 * - Aula: duracao em minutos como parametro do metodo
 * - KURA: duracao configurada em ms via kura.jwt.expiration-ms
 *
 * VERSAO JJWT: 0.12.6 (alinhada com o projeto de aula)
 */
@Component
public class JWTUtil {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JWTUtil(
            @Value("${kura.jwt.secret}") String secret,
            @Value("${kura.jwt.expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    /**
     * Gera um token JWT — equivalente ao gerarToken() do projeto de aula.
     */
    public String gerarToken(String username) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + expiracaoMs))
                .signWith(chave)
                .compact();
    }

    /**
     * Extrai o username (subject) do token — equivalente ao extrairUsername() da aula.
     */
    public String extrairUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    /**
     * Valida o token — equivalente ao validarToken() do projeto de aula.
     */
    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
