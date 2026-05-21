package br.com.clyvo.kura.tutor.auth.application;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.RefreshRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.AccountInactiveException;
import br.com.clyvo.kura.tutor.shared.exception.AccountLockedException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock ContaTutorRepository contaRepo;
    @Mock PasswordEncoder      encoder;
    @Mock JwtTokenProvider     jwt;

    @InjectMocks AuthService service;

    private static final String EMAIL = "tutor@clyvo.vet";
    private static final String SENHA = "Senha@123";
    private static final String HASH  = "$2a$12$hashedSenha";

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(EMAIL, SENHA);
    }

    // ─── Caminho feliz ────────────────────────────────────────────────────────

    @Test
    @DisplayName("loginValidoDeveRetornarTokensERotacionarRefresh")
    void loginValidoDeveRetornarTokensERotacionarRefresh() {
        ContaTutor conta = contaAtiva(0);
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));
        when(encoder.matches(SENHA, HASH)).thenReturn(true);
        when(encoder.encode(anyString())).thenReturn("$2a$12$hashedRefresh");
        when(jwt.gerarAccess(conta)).thenReturn("access.jwt");
        when(jwt.gerarRefresh(conta)).thenReturn("refresh.jwt");
        when(contaRepo.save(any())).thenReturn(conta);

        TokenResponse resp = service.login(loginRequest);

        assertThat(resp.accessToken()).isEqualTo("access.jwt");
        assertThat(resp.refreshToken()).isEqualTo("refresh.jwt");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        assertThat(resp.expiresIn()).isEqualTo(900L);

        // refresh deve ter sido hasheado e rotacionado
        assertThat(conta.getDsRefreshTokenHash()).isEqualTo("$2a$12$hashedRefresh");
        assertThat(conta.getDtRefreshExpira()).isNotNull();

        // tentativas resetadas, último login gravado
        assertThat(conta.getNrTentativasLogin()).isZero();
        assertThat(conta.getDtUltimoLogin()).isNotNull();

        verify(contaRepo).save(conta);
    }

    // ─── Email inexistente → 401 ──────────────────────────────────────────────

    @Test
    @DisplayName("loginEmailInexistenteDeveRetornar401 — anti-enumeração: não expõe 404")
    void loginEmailInexistenteDeveRetornar401() {
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("invalidos");

        verifyNoInteractions(encoder, jwt);
    }

    // ─── Senha errada → 401 + incrementa tentativas ──────────────────────────

    @Test
    @DisplayName("loginSenhaErradaDeveIncrementarTentativas")
    void loginSenhaErradaDeveIncrementarTentativas() {
        ContaTutor conta = contaAtiva(2);
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));
        when(encoder.matches(SENHA, HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(conta.getNrTentativasLogin()).isEqualTo(3);
        assertThat(conta.isBloqueada()).isFalse(); // ainda abaixo de 5
        verify(contaRepo).save(conta);
        verifyNoInteractions(jwt);
    }

    // ─── 5ª tentativa errada → bloqueia ──────────────────────────────────────

    @Test
    @DisplayName("apos5TentativasDeveBloquearConta — dtBloqueio preenchido no 5º erro")
    void apos5TentativasDeveBloquearConta() {
        ContaTutor conta = contaAtiva(4);  // já tem 4 falhas; a 5ª deve bloquear
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));
        when(encoder.matches(SENHA, HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(conta.getNrTentativasLogin()).isEqualTo(5);
        assertThat(conta.isBloqueada()).isTrue();
        assertThat(conta.getDtBloqueio()).isNotNull();
        // save é chamado para persistir o estado bloqueado
        verify(contaRepo).save(conta);
        verifyNoInteractions(jwt);
    }

    // ─── Conta bloqueada → 423 ────────────────────────────────────────────────

    @Test
    @DisplayName("loginContaBloqueadaDeveRetornar423")
    void loginContaBloqueadaDeveRetornar423() {
        ContaTutor conta = contaBloqueada();
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(AccountLockedException.class);

        // BCrypt NÃO deve ser chamado para conta bloqueada (evita CPU desnecessário)
        verifyNoInteractions(encoder, jwt);
        verify(contaRepo, never()).save(any());
    }

    // ─── Conta inativa → 403 ─────────────────────────────────────────────────

    @Test
    @DisplayName("loginContaInativaDeveRetornar403")
    void loginContaInativaDeveRetornar403() {
        ContaTutor conta = contaInativa();
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(AccountInactiveException.class);

        verifyNoInteractions(encoder, jwt);
        verify(contaRepo, never()).save(any());
    }

    // ─── Ordem de verificação ─────────────────────────────────────────────────

    @Test
    @DisplayName("contaInativaBloqueadaDeveRetornar403 — inativa tem precedência sobre bloqueada")
    void contaInativaBloqueadaDeveRetornar403() {
        // Conta simultaneamente inativa e bloqueada → 403 tem prioridade
        ContaTutor conta = contaInativaBloqueada();
        when(contaRepo.findByDsEmailLogin(EMAIL)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.login(loginRequest))
                .isInstanceOf(AccountInactiveException.class);
    }

    // ─── T11: Refresh token rotation ─────────────────────────────────────────

    @Test
    @DisplayName("refreshValidoDeveRotacionarERetornarNovoPar")
    void refreshValidoDeveRotacionarERetornarNovoPar() {
        ContaTutor conta = contaAtiva(0);
        conta.setDsRefreshTokenHash("$2a$12$oldRefreshHash");
        conta.setDtRefreshExpira(LocalDateTime.now().plusDays(7));

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("10");
        when(jwt.validar("old.refresh.jwt")).thenReturn(Optional.of(claims));
        when(contaRepo.findById(10L)).thenReturn(Optional.of(conta));
        when(encoder.matches("old.refresh.jwt", "$2a$12$oldRefreshHash")).thenReturn(true);
        when(jwt.gerarAccess(conta)).thenReturn("new.access.jwt");
        when(jwt.gerarRefresh(conta)).thenReturn("new.refresh.jwt");
        when(encoder.encode("new.refresh.jwt")).thenReturn("$2a$12$newRefreshHash");
        when(contaRepo.save(any())).thenReturn(conta);

        TokenResponse resp = service.refresh(new RefreshRequest("old.refresh.jwt"));

        assertThat(resp.accessToken()).isEqualTo("new.access.jwt");
        assertThat(resp.refreshToken()).isEqualTo("new.refresh.jwt");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        assertThat(resp.expiresIn()).isEqualTo(900L);
        // hash rotacionado para o novo refresh
        assertThat(conta.getDsRefreshTokenHash()).isEqualTo("$2a$12$newRefreshHash");
        assertThat(conta.getDtRefreshExpira()).isAfter(LocalDateTime.now());
        verify(contaRepo).save(conta);
    }

    @Test
    @DisplayName("refreshComTokenAntigoAposRotacaoDeveRetornar401 — token rotacionado invalida o anterior")
    void refreshComTokenAntigoAposRotacaoDeveRetornar401() {
        // DB já tem hash do novo token; o antigo chega aqui com BCrypt mismatch
        ContaTutor conta = contaAtiva(0);
        conta.setDsRefreshTokenHash("$2a$12$hashDoNovoToken");
        conta.setDtRefreshExpira(LocalDateTime.now().plusDays(7));

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("10");
        when(jwt.validar("stale.refresh.jwt")).thenReturn(Optional.of(claims));
        when(contaRepo.findById(10L)).thenReturn(Optional.of(conta));
        when(encoder.matches("stale.refresh.jwt", "$2a$12$hashDoNovoToken")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("stale.refresh.jwt")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("invalido");

        verify(jwt, never()).gerarAccess(any());
        verify(jwt, never()).gerarRefresh(any());
        verify(contaRepo, never()).save(any());
    }

    @Test
    @DisplayName("refreshContaInativaDeveRetornar403 — BCrypt não é invocado para conta inativa")
    void refreshContaInativaDeveRetornar403() {
        ContaTutor conta = contaInativa();

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("10");
        when(jwt.validar("refresh.jwt")).thenReturn(Optional.of(claims));
        when(contaRepo.findById(10L)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("refresh.jwt")))
                .isInstanceOf(AccountInactiveException.class);

        verifyNoInteractions(encoder);
        verify(contaRepo, never()).save(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ContaTutor contaAtiva(int tentativas) {
        Tutor tutor = mock(Tutor.class);
        lenient().when(tutor.getIdTutor()).thenReturn(1L);
        lenient().when(tutor.getNmTutor()).thenReturn("Tutor Teste");

        ContaTutor c = new ContaTutor();
        c.setIdConta(10L);
        c.setDsEmailLogin(EMAIL);
        c.setDsSenhaHash(HASH);
        c.setStAtiva("S");
        c.setNrTentativasLogin(tentativas);
        c.setTutor(tutor);
        return c;
    }

    private ContaTutor contaBloqueada() {
        ContaTutor c = contaAtiva(5);
        c.setDtBloqueio(java.time.LocalDateTime.now().minusMinutes(1));
        return c;
    }

    private ContaTutor contaInativa() {
        ContaTutor c = contaAtiva(0);
        c.setStAtiva("N");
        return c;
    }

    private ContaTutor contaInativaBloqueada() {
        ContaTutor c = contaAtiva(5);
        c.setStAtiva("N");
        c.setDtBloqueio(java.time.LocalDateTime.now().minusMinutes(1));
        return c;
    }
}
