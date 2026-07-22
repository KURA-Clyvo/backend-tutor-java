package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
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

    // ─── Stubs 501 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/tutor/pets/{id} retorna 501 — stub pendente INT-01")
    @WithMockUser(username = EMAIL)
    void detalharPet_retorna501() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets/1"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("GET /v1/tutor/pets/{id}/timeline retorna 501 — stub pendente INT-01")
    @WithMockUser(username = EMAIL)
    void timelinePet_retorna501() throws Exception {
        mockMvc.perform(get("/v1/tutor/pets/1/timeline"))
                .andExpect(status().isNotImplemented());
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
