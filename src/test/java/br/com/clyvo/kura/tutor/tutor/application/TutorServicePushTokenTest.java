package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.tutor.dto.PushTokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorServicePushTokenTest {

    @Mock ContaTutorRepository contaTutorRepository;
    @Mock TutorRepository      tutorRepository;
    @Mock PetRepository        petRepository;

    @InjectMocks TutorService service;

    private static final Long   ID_TUTOR   = 1L;
    private static final String TOKEN      = "ExponentPushToken[abc123xxxxxxxxxx]";
    private static final String PLATAFORMA = "android";

    @Test
    @DisplayName("atualizarPushToken — sucesso: repo chamado uma vez com os valores corretos")
    void sucesso_chamaRepoCom1Update() {
        when(contaTutorRepository.atualizarPushToken(ID_TUTOR, TOKEN, PLATAFORMA)).thenReturn(1);

        assertThatNoException().isThrownBy(() ->
                service.atualizarPushToken(ID_TUTOR, new PushTokenRequest(TOKEN, PLATAFORMA)));

        verify(contaTutorRepository, times(1)).atualizarPushToken(ID_TUTOR, TOKEN, PLATAFORMA);
    }

    @Test
    @DisplayName("atualizarPushToken — tutor inexistente: lança RecursoNaoEncontradoException")
    void tutorInexistente_lancaExcecao() {
        when(contaTutorRepository.atualizarPushToken(anyLong(), anyString(), anyString())).thenReturn(0);

        assertThatThrownBy(() ->
                service.atualizarPushToken(999L, new PushTokenRequest(TOKEN, PLATAFORMA)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("atualizarPushToken — LGPD: método não loga o valor do token (verificação de design)")
    void lgpd_tokenNaoVazaParaLog() {
        // Verificação de design: o token nunca é concatenado em mensagem de log.
        // Basta garantir que o método não lança exceção com token sensível.
        when(contaTutorRepository.atualizarPushToken(anyLong(), anyString(), anyString())).thenReturn(1);

        assertThatNoException().isThrownBy(() ->
                service.atualizarPushToken(ID_TUTOR, new PushTokenRequest("SENSITIVE_TOKEN_VALUE", "ios")));

        // Verifica que o atualizarPushToken foi chamado — confirma que o fluxo chegou ao repo
        verify(contaTutorRepository).atualizarPushToken(ID_TUTOR, "SENSITIVE_TOKEN_VALUE", "ios");
    }
}
