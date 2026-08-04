package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaStatusResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.application.TimelineService;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetDetalheResponse;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import br.com.clyvo.kura.tutor.tutor.application.TutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TutorBffController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class TutorBffControllerTest {

    private static final String EMAIL     = "tutor@clyvo.vet";
    private static final Long   ID_TUTOR  = 42L;

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TutorService          tutorService;
    @MockBean TimelineService       timelineService;
    @MockBean ContaTutorRepository  contaTutorRepository;
    @MockBean UserDetailsService    userDetailsService;
    @MockBean JwtTokenProvider      jwtTokenProvider;

    // ─── GET /v1/tutor/pets ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets com JWT válido retorna 200 com lista de pets")
    @WithMockUser(username = EMAIL)
    void listarPets_comJwt_retorna200() throws Exception {
        PetResponse pet = new PetResponse(1L, "Rex", "Cachorro", "SRD", "M", LocalDate.of(2020, 3, 15), "M");
        Page<PetResponse> page = new PageImpl<>(List.of(pet));

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(tutorService.listarPets(eq(ID_TUTOR), eq(EMAIL), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/tutor/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idPet").value(1))
                .andExpect(jsonPath("$.content[0].nmPet").value("Rex"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /v1/tutor/pets sem JWT retorna 401")
    void listarPets_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/tutor/pets deriva idTutor do JWT (nunca do path)")
    @WithMockUser(username = EMAIL)
    void listarPets_idTutorDerivadoDoJwt() throws Exception {
        Page<PetResponse> page = new PageImpl<>(List.of());
        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(tutorService.listarPets(anyLong(), anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/tutor/pets")).andExpect(status().isOk());

        verify(contaTutorRepository).findIdTutorByEmail(EMAIL);
        verify(tutorService).listarPets(eq(ID_TUTOR), eq(EMAIL), any(Pageable.class));
    }

    // ─── GET /v1/tutor/pets/{id} — detalhe (TASK-31) ─────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id} com JWT válido retorna 200 com detalhe do pet")
    @WithMockUser(username = EMAIL)
    void detalharPet_comJwt_retorna200() throws Exception {
        PetDetalheResponse detalhe = new PetDetalheResponse(
                1L, "Rex", "Cachorro", "SRD", "M", LocalDate.of(2020, 3, 15), "M",
                "Clyvo Vet São Paulo", "Dra. Ana Souza", 3L);

        when(tutorService.buscarPetDetalhe(eq(1L), eq(EMAIL))).thenReturn(detalhe);

        mockMvc.perform(get("/v1/tutor/pets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPet").value(1))
                .andExpect(jsonPath("$.nmPet").value("Rex"))
                .andExpect(jsonPath("$.nrConsultas").value(3));
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id} sem JWT retorna 401")
    void detalharPet_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id} de pet de outro tutor retorna 403")
    @WithMockUser(username = EMAIL)
    void detalharPet_deOutroTutor_retorna403() throws Exception {
        when(tutorService.buscarPetDetalhe(eq(1L), eq(EMAIL)))
                .thenThrow(new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado."));

        mockMvc.perform(get("/v1/tutor/pets/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id} inexistente retorna 404")
    @WithMockUser(username = EMAIL)
    void detalharPet_inexistente_retorna404() throws Exception {
        when(tutorService.buscarPetDetalhe(eq(99L), eq(EMAIL)))
                .thenThrow(new RecursoNaoEncontradoException("Pet", 99L));

        mockMvc.perform(get("/v1/tutor/pets/99"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /v1/tutor/pets/{id}/timeline (TASK-31) ──────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/timeline com JWT válido retorna 200")
    @WithMockUser(username = EMAIL)
    void timelinePet_comJwt_retorna200() throws Exception {
        TimelineEventoResponse evento = new TimelineEventoResponse(
                10L, 1L, "Rex", LocalDateTime.now(), "CONSULTA", "REALIZADO", 5L, "Clyvo Vet São Paulo");
        Page<TimelineEventoResponse> page = new PageImpl<>(List.of(evento));

        when(timelineService.listarTimeline(eq(1L), eq(EMAIL), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/tutor/pets/1/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idEvento").value(10));
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/timeline sem JWT retorna 401")
    void timelinePet_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets/1/timeline"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /v1/tutor/pets/{id}/timeline/{idEvento} — detalhe (TASK-31) ─────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/timeline/{idEvento} com JWT válido retorna 200")
    @WithMockUser(username = EMAIL)
    void detalharEventoTimeline_comJwt_retorna200() throws Exception {
        TimelineEventoResponse evento = new TimelineEventoResponse(
                10L, 1L, "Rex", LocalDateTime.now(), "CONSULTA", "REALIZADO", 5L, "Clyvo Vet São Paulo");

        when(timelineService.buscarEventoDetalhe(eq(1L), eq(10L), eq(EMAIL))).thenReturn(evento);

        mockMvc.perform(get("/v1/tutor/pets/1/timeline/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(10))
                .andExpect(jsonPath("$.dsTipoEvento").value("CONSULTA"));
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/timeline/{idEvento} inexistente retorna 404")
    @WithMockUser(username = EMAIL)
    void detalharEventoTimeline_inexistente_retorna404() throws Exception {
        when(timelineService.buscarEventoDetalhe(eq(1L), eq(999L), eq(EMAIL)))
                .thenThrow(new br.com.clyvo.kura.tutor.shared.exception.NotFoundException("Evento", 999L));

        mockMvc.perform(get("/v1/tutor/pets/1/timeline/999"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /v1/tutor/pets/{id}/vacinas (TASK-31) ───────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/vacinas com JWT válido retorna 200")
    @WithMockUser(username = EMAIL)
    void vacinasPet_comJwt_retorna200() throws Exception {
        VacinaVencendoResponse vacina = new VacinaVencendoResponse(
                1L, "Rex", "Antirrábica", LocalDateTime.now().plusDays(10), 5L, "Clyvo Vet São Paulo");

        when(timelineService.listarVacinasPet(eq(1L), eq(EMAIL))).thenReturn(List.of(vacina));

        mockMvc.perform(get("/v1/tutor/pets/1/vacinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmVacina").value("Antirrábica"));
    }

    // ─── GET /v1/tutor/pets/{id}/vacinas/status (TASK-31) ────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/vacinas/status com JWT válido retorna 200")
    @WithMockUser(username = EMAIL)
    void statusVacinasPet_comJwt_retorna200() throws Exception {
        VacinaStatusResponse resumo = new VacinaStatusResponse(1L, 1, LocalDateTime.now().plusDays(10), "ALERTA");

        when(timelineService.statusVacinasPet(eq(1L), eq(EMAIL))).thenReturn(resumo);

        mockMvc.perform(get("/v1/tutor/pets/1/vacinas/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dsStatusGeral").value("ALERTA"))
                .andExpect(jsonPath("$.qtdPendentes").value(1));
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/vacinas/status sem JWT retorna 401")
    void statusVacinasPet_semJwt_retorna401() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets/1/vacinas/status"))
                .andExpect(status().isUnauthorized());
    }

    // ─── PATCH /me/push-token ─────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /v1/tutor/me/push-token com JWT válido retorna 204")
    @WithMockUser(username = EMAIL)
    void atualizarPushToken_comJwt_retorna204() throws Exception {
        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));

        mockMvc.perform(patch("/v1/tutor/me/push-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dsPushToken\":\"ExponentPushToken[abc]\",\"dsPlatforma\":\"android\"}"))
                .andExpect(status().isNoContent());

        verify(tutorService).atualizarPushToken(eq(ID_TUTOR), any());
    }

    @Test
    @DisplayName("PATCH /v1/tutor/me/push-token sem JWT retorna 401")
    void atualizarPushToken_semJwt_retorna401() throws Exception {
        mockMvc.perform(patch("/v1/tutor/me/push-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dsPushToken\":\"tok\",\"dsPlatforma\":\"android\"}"))
                .andExpect(status().isUnauthorized());
    }
}
