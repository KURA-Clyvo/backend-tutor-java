package br.com.clyvo.kura.tutor.consentimento.application;

import br.com.clyvo.kura.tutor.consentimento.api.dto.ConsentimentoRequest;
import br.com.clyvo.kura.tutor.consentimento.application.ConsentimentoService.RegistroResult;
import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import br.com.clyvo.kura.tutor.consentimento.domain.IdempotencyKey;
import br.com.clyvo.kura.tutor.consentimento.domain.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.consentimento.lgpd.TipoConsentimento;
import br.com.clyvo.kura.tutor.consentimento.lgpd.ValidadorConsentimento;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentimentoServiceTest {

    private static final Long   ID_TUTOR   = 1L;
    private static final String EMAIL      = "tutor@clyvo.vet";
    private static final String IP_CLIENTE = "192.168.1.1";
    private static final String KEY_A      = "key-uuid-aaaa-aaaa-aaaa";
    private static final String KEY_B      = "key-uuid-bbbb-bbbb-bbbb";
    private static final String RESOURCE   = "CONSENTIMENTO";

    @Mock ConsentimentoRepository consentimentoRepository;
    @Mock IdempotencyService      idempotencyService;
    @Mock TutorRepository         tutorRepository;
    @Mock ContaTutorRepository    contaTutorRepository;
    @Mock ValidadorConsentimento  validador;

    @InjectMocks ConsentimentoService consentimentoService;

    // ─── primeiraChamadaCriaERetorna201 ──────────────────────────────────────

    @Test
    @DisplayName("primeiraChamadaCriaERetorna201 — nova key cria consentimento, registra idempotência, criado=true")
    void primeiraChamadaCriaERetorna201() {
        // Arrange
        ConsentimentoRequest request = new ConsentimentoRequest(
                TipoConsentimento.LEMBRETES, "v1.0", "S", "texto do termo");

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(idempotencyService.buscar(KEY_A, RESOURCE)).thenReturn(Optional.empty());

        Tutor tutor = mockTutor();
        when(tutorRepository.findByIdTutorAndStAtivo(ID_TUTOR, "S")).thenReturn(Optional.of(tutor));

        Consentimento salvo = mockConsentimento(10L);
        when(consentimentoRepository.save(any(Consentimento.class))).thenReturn(salvo);

        IdempotencyKey keyRegistrada = mock(IdempotencyKey.class);
        when(idempotencyService.registrar(KEY_A, RESOURCE, 10L)).thenReturn(keyRegistrada);

        // Act
        RegistroResult result = consentimentoService.registrarComIdempotencia(
                ID_TUTOR, request, IP_CLIENTE, KEY_A, EMAIL);

        // Assert
        assertThat(result.criado()).isTrue();
        assertThat(result.response().idConsentimento()).isEqualTo(10L);
        verify(consentimentoRepository).save(any(Consentimento.class));
        verify(idempotencyService).registrar(KEY_A, RESOURCE, 10L);
    }

    // ─── segundaChamadaMesmaKeyRetorna200ComMesmoBody ────────────────────────

    @Test
    @DisplayName("segundaChamadaMesmaKeyRetorna200ComMesmoBody — key existente retorna original sem nova inserção")
    void segundaChamadaMesmaKeyRetorna200ComMesmoBody() {
        // Arrange
        ConsentimentoRequest request = new ConsentimentoRequest(
                TipoConsentimento.LEMBRETES, "v1.0", "S", "texto do termo");

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));

        IdempotencyKey chaveExistente = mock(IdempotencyKey.class);
        when(chaveExistente.getIdResourceCriado()).thenReturn(10L);
        when(idempotencyService.buscar(KEY_A, RESOURCE)).thenReturn(Optional.of(chaveExistente));

        Consentimento original = mockConsentimento(10L);
        when(consentimentoRepository.findById(10L)).thenReturn(Optional.of(original));

        // Act
        RegistroResult result = consentimentoService.registrarComIdempotencia(
                ID_TUTOR, request, IP_CLIENTE, KEY_A, EMAIL);

        // Assert
        assertThat(result.criado()).isFalse();
        assertThat(result.response().idConsentimento()).isEqualTo(10L);
        verify(consentimentoRepository, never()).save(any());
        verify(idempotencyService, never()).registrar(anyString(), anyString(), anyLong());
    }

    // ─── duasChamadasComKeysDiferentesCriamDoisRegistros ────────────────────

    @Test
    @DisplayName("duasChamadasComKeysDiferentesCriamDoisRegistros — keys distintas geram 2 consentimentos")
    void duasChamadasComKeysDiferentesCriamDoisRegistros() {
        // Arrange
        ConsentimentoRequest request = new ConsentimentoRequest(
                TipoConsentimento.LEMBRETES, "v1.0", "S", "texto");

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(idempotencyService.buscar(anyString(), eq(RESOURCE))).thenReturn(Optional.empty());

        Tutor tutor = mockTutor();
        when(tutorRepository.findByIdTutorAndStAtivo(ID_TUTOR, "S")).thenReturn(Optional.of(tutor));

        Consentimento c1 = mockConsentimento(10L);
        Consentimento c2 = mockConsentimento(11L);
        when(consentimentoRepository.save(any(Consentimento.class))).thenReturn(c1, c2);
        when(idempotencyService.registrar(anyString(), eq(RESOURCE), anyLong()))
                .thenReturn(mock(IdempotencyKey.class));

        // Act
        RegistroResult r1 = consentimentoService.registrarComIdempotencia(
                ID_TUTOR, request, IP_CLIENTE, KEY_A, EMAIL);
        RegistroResult r2 = consentimentoService.registrarComIdempotencia(
                ID_TUTOR, request, IP_CLIENTE, KEY_B, EMAIL);

        // Assert
        assertThat(r1.criado()).isTrue();
        assertThat(r2.criado()).isTrue();
        assertThat(r1.response().idConsentimento()).isNotEqualTo(r2.response().idConsentimento());
        verify(consentimentoRepository, times(2)).save(any(Consentimento.class));
        verify(idempotencyService).registrar(KEY_A, RESOURCE, 10L);
        verify(idempotencyService).registrar(KEY_B, RESOURCE, 11L);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Tutor mockTutor() {
        return mock(Tutor.class);
    }

    private Consentimento mockConsentimento(Long id) {
        Consentimento c = mock(Consentimento.class);
        when(c.getIdConsentimento()).thenReturn(id);
        when(c.getDsTipo()).thenReturn("LEMBRETES");
        when(c.getDsVersaoTermo()).thenReturn("v1.0");
        when(c.isAceito()).thenReturn(true);
        when(c.isAtivo()).thenReturn(true);
        when(c.getDtAceite()).thenReturn(LocalDateTime.now());
        when(c.getDtRevogacao()).thenReturn(null);
        return c;
    }
}
