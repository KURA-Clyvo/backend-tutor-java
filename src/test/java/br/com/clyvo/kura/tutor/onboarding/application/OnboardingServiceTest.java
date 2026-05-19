package br.com.clyvo.kura.tutor.onboarding.application;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.lgpd.TipoConsentimento;
import br.com.clyvo.kura.tutor.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.onboarding.api.dto.AceiteRequest;
import br.com.clyvo.kura.tutor.onboarding.api.dto.RegisterInviteRequest;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.onboarding.domain.InviteTutor;
import br.com.clyvo.kura.tutor.onboarding.domain.repository.InviteTutorRepository;
import br.com.clyvo.kura.tutor.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import br.com.clyvo.kura.tutor.shared.exception.GoneException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    private static final String TOKEN_VALIDO = "550e8400-e29b-41d4-a716-446655440000";

    @Mock InviteTutorRepository   inviteRepo;
    @Mock TutorRepository         tutorRepo;
    @Mock ContaTutorRepository    contaRepo;
    @Mock ConsentimentoRepository consentRepo;
    @Mock PasswordEncoder         encoder;
    @Mock JwtTokenProvider        jwt;
    @Mock ValidadorConsentimento  validador;
    @Mock HttpServletRequest      httpRequest;

    @InjectMocks
    OnboardingService service;

    private InviteTutor             inviteValido;
    private Tutor                   tutorAtivo;
    private RegisterInviteRequest   requestValido;

    @BeforeEach
    void setUp() {
        inviteValido  = stubInvite(TOKEN_VALIDO, false, true, LocalDateTime.now().plusDays(7));
        tutorAtivo    = stubTutor(1L, true, true);
        requestValido = new RegisterInviteRequest(TOKEN_VALIDO, "Senha@123", List.of());
    }

    // ─── Caminho feliz ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deveCriarContaEConsentimentosComInviteValido — fluxo feliz, 2 saves em conta")
    void deveCriarContaEConsentimentosComInviteValido() {
        ContaTutor contaSalva = contaComId(10L);
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(inviteValido));
        when(tutorRepo.findById(1L)).thenReturn(Optional.of(tutorAtivo));
        when(encoder.encode(anyString())).thenReturn("$2a$hash");
        when(contaRepo.save(any())).thenReturn(contaSalva);
        when(jwt.gerarAccess(any())).thenReturn("access.token.ok");
        when(jwt.gerarRefresh(any())).thenReturn("refresh.token.ok");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        TokenResponse resp = service.registrarPorInvite(requestValido, httpRequest);

        assertThat(resp.accessToken()).isEqualTo("access.token.ok");
        assertThat(resp.refreshToken()).isEqualTo("refresh.token.ok");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        assertThat(resp.expiresIn()).isEqualTo(900L);
        assertThat(resp.tutor().idTutor()).isEqualTo(1L);
        // 1 save (conta) + 1 save (refresh rotation)
        verify(contaRepo, times(2)).save(any(ContaTutor.class));
        verify(consentRepo, never()).save(any());
    }

    @Test
    @DisplayName("deveCriarConsentimentosQuandoAceitesPresentes")
    void deveCriarConsentimentosQuandoAceitesPresentes() {
        List<AceiteRequest> aceites = List.of(
                new AceiteRequest(TipoConsentimento.LEMBRETES, "v1.0", true, "texto do termo"),
                new AceiteRequest(TipoConsentimento.MARKETING, "v1.0", false, null)
        );
        RegisterInviteRequest reqComAceites = new RegisterInviteRequest(TOKEN_VALIDO, "Senha@123", aceites);
        ContaTutor contaSalva = contaComId(10L);

        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(inviteValido));
        when(tutorRepo.findById(1L)).thenReturn(Optional.of(tutorAtivo));
        when(encoder.encode(anyString())).thenReturn("$2a$hash");
        when(contaRepo.save(any())).thenReturn(contaSalva);
        when(jwt.gerarAccess(any())).thenReturn("access");
        when(jwt.gerarRefresh(any())).thenReturn("refresh");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        service.registrarPorInvite(reqComAceites, httpRequest);

        verify(consentRepo, times(2)).save(any());
        verify(validador, times(2)).validarVersaoTermo(any(TipoConsentimento.class), anyString());
    }

    // ─── Casos de erro ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deveLancar404SeTokenNaoExiste")
    void deveLancar404SeTokenNaoExiste() {
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarPorInvite(requestValido, httpRequest))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(contaRepo, consentRepo, jwt);
    }

    @Test
    @DisplayName("deveLancar409SeInviteUtilizado")
    void deveLancar409SeInviteUtilizado() {
        InviteTutor utilizado = stubInvite(TOKEN_VALIDO, true, true, LocalDateTime.now().plusDays(7));
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(utilizado));

        assertThatThrownBy(() -> service.registrarPorInvite(requestValido, httpRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("utilizado");
        verifyNoInteractions(contaRepo, consentRepo, jwt);
    }

    @Test
    @DisplayName("deveLancar409SeInviteCancelado")
    void deveLancar409SeInviteCancelado() {
        InviteTutor cancelado = stubInvite(TOKEN_VALIDO, false, false, LocalDateTime.now().plusDays(7));
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(cancelado));

        assertThatThrownBy(() -> service.registrarPorInvite(requestValido, httpRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    @DisplayName("deveLancar410SeInviteExpirado")
    void deveLancar410SeInviteExpirado() {
        InviteTutor expirado = stubInvite(TOKEN_VALIDO, false, true, LocalDateTime.now().minusDays(1));
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.registrarPorInvite(requestValido, httpRequest))
                .isInstanceOf(GoneException.class);
        verifyNoInteractions(contaRepo, consentRepo, jwt);
    }

    @Test
    @DisplayName("deveLancar409SeUKDispararNoSave — race condition simulada")
    void deveLancar409SeUKDispararNoSave() {
        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(inviteValido));
        when(tutorRepo.findById(1L)).thenReturn(Optional.of(tutorAtivo));
        when(encoder.encode(anyString())).thenReturn("$2a$hash");
        when(contaRepo.save(any())).thenThrow(new DataIntegrityViolationException("UK_CONTA_INVITE_USED"));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> service.registrarPorInvite(requestValido, httpRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("utilizado");
        verify(jwt, never()).gerarAccess(any());
    }

    @Test
    @DisplayName("deveFazerRollbackSeFalhaAoCriarConsentimento — exceção propaga, tokens não gerados")
    void deveFazerRollbackSeFalhaAoCriarConsentimento() {
        List<AceiteRequest> aceites = List.of(
                new AceiteRequest(TipoConsentimento.LEMBRETES, "v1.0", true, "texto"));
        RegisterInviteRequest reqComAceite = new RegisterInviteRequest(TOKEN_VALIDO, "Senha@123", aceites);
        ContaTutor contaSalva = contaComId(10L);

        when(inviteRepo.findByNrToken(TOKEN_VALIDO)).thenReturn(Optional.of(inviteValido));
        when(tutorRepo.findById(1L)).thenReturn(Optional.of(tutorAtivo));
        when(encoder.encode(anyString())).thenReturn("$2a$hash");
        when(contaRepo.save(any())).thenReturn(contaSalva);
        when(consentRepo.save(any())).thenThrow(new RuntimeException("DB indisponível"));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // @Transactional garante rollback real; aqui verificamos que a exceção propaga
        // e que os tokens nunca são gerados (passos 11-12 não são atingidos).
        assertThatThrownBy(() -> service.registrarPorInvite(reqComAceite, httpRequest))
                .isInstanceOf(RuntimeException.class);
        verify(jwt, never()).gerarAccess(any());
        verify(jwt, never()).gerarRefresh(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Cria mock de InviteTutor com lenient stubs para todos os métodos.
     * Lenient evita UnnecessaryStubbingException quando o serviço lança
     * exceção antes de consultar campos posteriores (ex: expiração).
     */
    private InviteTutor stubInvite(String token, boolean utilizado,
                                   boolean ativo, LocalDateTime expira) {
        InviteTutor invite = mock(InviteTutor.class);
        lenient().when(invite.getNrToken()).thenReturn(token);
        lenient().when(invite.isAtivo()).thenReturn(ativo);
        lenient().when(invite.isUtilizado()).thenReturn(utilizado);
        lenient().when(invite.isExpirado()).thenReturn(LocalDateTime.now().isAfter(expira));
        lenient().when(invite.getIdTutor()).thenReturn(1L);
        lenient().when(invite.getIdInvite()).thenReturn(99L);
        return invite;
    }

    private Tutor stubTutor(Long id, boolean ativo, boolean avisoPrivacidade) {
        Tutor t = mock(Tutor.class);
        when(t.getIdTutor()).thenReturn(id);
        when(t.getNmTutor()).thenReturn("Tutor Teste");
        when(t.getDsEmail()).thenReturn("tutor@test.com");
        when(t.isAtivo()).thenReturn(ativo);
        when(t.temAvisoPrivacidade()).thenReturn(avisoPrivacidade);
        return t;
    }

    private ContaTutor contaComId(Long id) {
        ContaTutor c = new ContaTutor();
        c.setIdConta(id);
        c.setDsEmailLogin("tutor@test.com");
        return c;
    }
}
