package br.com.clyvo.kura.tutor.tutor.api;

import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.tutor.api.dto.PetResponse;
import br.com.clyvo.kura.tutor.tutor.application.TutorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de camada web para GET /tutores/{id}/pets.
 *
 * Foco: ownership check (403), happy path (200) e passagem correta de pageable.
 * UserDetailsService é mockado para satisfazer o JwtAuthenticationFilter no contexto slim.
 */
@WebMvcTest(TutorController.class)
@TestPropertySource(properties = {
        "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
        "kura.jwt.access-expiration-minutes=15",
        "kura.jwt.refresh-expiration-days=7"
})
class TutorControllerTest {

    private static final String EMAIL = "tutor@clyvo.vet";
    private static final Long   ID_PROPRIO = 42L;
    private static final Long   ID_ALHEIO  = 99L;

    @Autowired MockMvc        mockMvc;
    @MockBean  TutorService   tutorService;
    @MockBean  UserDetailsService userDetailsService;

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPetsDeTutorPropriosDeveRetornar200")
    @WithMockUser(username = EMAIL)
    void getPetsDeTutorPropriosDeveRetornar200() throws Exception {
        PetResponse pet = new PetResponse(
                1L, "Rex", "Cachorro", "SRD", "M", LocalDate.of(2020, 3, 15), "M");
        Page<PetResponse> page = new PageImpl<>(List.of(pet));

        when(tutorService.listarPets(eq(ID_PROPRIO), eq(EMAIL), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tutores/{id}/pets", ID_PROPRIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].idPet").value(1))
                .andExpect(jsonPath("$.content[0].nmPet").value("Rex"))
                .andExpect(jsonPath("$.content[0].nmEspecie").value("Cachorro"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─── Ownership check ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getPetsDeOutroTutorDeveRetornar403")
    @WithMockUser(username = EMAIL)
    void getPetsDeOutroTutorDeveRetornar403() throws Exception {
        when(tutorService.listarPets(eq(ID_ALHEIO), eq(EMAIL), any(Pageable.class)))
                .thenThrow(new ForbiddenException("Acesso negado: você só pode visualizar seus próprios pets."));

        mockMvc.perform(get("/tutores/{id}/pets", ID_ALHEIO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(
                        "Acesso negado: você só pode visualizar seus próprios pets."));
    }

    // ─── Paginação ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("paginacaoFunciona")
    @WithMockUser(username = EMAIL)
    void paginacaoFunciona() throws Exception {
        Page<PetResponse> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 10);

        when(tutorService.listarPets(eq(ID_PROPRIO), eq(EMAIL), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tutores/{id}/pets", ID_PROPRIO)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tutorService).listarPets(eq(ID_PROPRIO), eq(EMAIL), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }
}
