package br.com.clyvo.kura.tutor.auth.application;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários de JwtTokenProvider — sem Spring context, instanciação direta.
 *
 * Secret com 70 bytes (> 64) — satisfaz exigência HS512.
 * Access expiration: 15 min / Refresh expiration: 7 dias.
 */
class JwtTokenProviderTest {

    private static final String SECRET =
            "dev-secret-trocar-em-prod-com-no-minimo-64-bytes-gerado-localmente-aqui";

    private JwtTokenProvider provider;
    private ContaTutor contaTeste;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 15, 7);

        contaTeste = new ContaTutor();
        contaTeste.setIdConta(42L);
        contaTeste.setDsEmailLogin("felipe@clyvo.vet");
    }

    // ── gerarAccess ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("gerarAccess deve retornar token não-nulo e não-vazio")
    void deveGerarTokenNaoNulo() {
        String token = provider.gerarAccess(contaTeste);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("deveGerarTokenComClaimsCorretas — sub=idConta, email=dsEmailLogin")
    void deveGerarTokenComClaimsCorretas() {
        String token = provider.gerarAccess(contaTeste);

        Optional<Claims> claimsOpt = provider.validar(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("felipe@clyvo.vet");
    }

    @Test
    @DisplayName("gerarRefresh deve ter expiração maior que o access token")
    void refreshDeveExpirarDepoisDoAccess() {
        String access  = provider.gerarAccess(contaTeste);
        String refresh = provider.gerarRefresh(contaTeste);

        Date expAccess  = provider.validar(access).get().getExpiration();
        Date expRefresh = provider.validar(refresh).get().getExpiration();

        assertThat(expRefresh).isAfter(expAccess);
    }

    // ── validar ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deveValidarTokenValido — retorna Optional preenchido")
    void deveValidarTokenValido() {
        String token = provider.gerarAccess(contaTeste);
        assertThat(provider.validar(token)).isPresent();
    }

    @Test
    @DisplayName("deveRetornarEmptyParaTokenMalformado")
    void deveRetornarEmptyParaTokenMalformado() {
        assertThat(provider.validar("isso.nao.e.um.jwt")).isEmpty();
    }

    @Test
    @DisplayName("deveRetornarEmptyParaTokenExpirado")
    void deveRetornarEmptyParaTokenExpirado() {
        SecretKey chave = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String tokenExpirado = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(chave, Jwts.SIG.HS512)
                .compact();

        assertThat(provider.validar(tokenExpirado)).isEmpty();
    }

    @Test
    @DisplayName("deveRetornarEmptyParaTokenComAssinaturaInvalida")
    void deveRetornarEmptyParaAssinaturaInvalida() {
        String outroSecret = "outro-secret-diferente-para-simular-chave-errada-teste-kura";
        // Garante que o outro secret tem 64+ bytes
        String outroSecretPadded = outroSecret + "12345678901234567890";
        SecretKey outraChave = Keys.hmacShaKeyFor(
                outroSecretPadded.getBytes(StandardCharsets.UTF_8));

        String tokenAssinadoComOutraChave = Jwts.builder()
                .subject("99")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(outraChave, Jwts.SIG.HS512)
                .compact();

        assertThat(provider.validar(tokenAssinadoComOutraChave)).isEmpty();
    }

    // ── extrairIdConta ────────────────────────────────────────────────────────

    @Test
    @DisplayName("extrairIdConta deve retornar o idConta correto")
    void deveExtrairIdContaCorreto() {
        String token = provider.gerarAccess(contaTeste);
        Long idExtraido = provider.extrairIdConta(token);
        assertThat(idExtraido).isEqualTo(42L);
    }

    @Test
    @DisplayName("extrairIdConta deve lançar JwtException para token inválido")
    void deveExtrairIdContaLancaExcecaoParaTokenInvalido() {
        assertThatThrownBy(() -> provider.extrairIdConta("token.invalido"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    // ── validação de secret ───────────────────────────────────────────────────

    @Test
    @DisplayName("construtor deve lançar IllegalArgumentException para secret curto")
    void deveLancarExcecaoParaSecretCurto() {
        assertThatThrownBy(() -> new JwtTokenProvider("curto", 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 bytes");
    }
}
