package br.com.clyvo.kura.tutor.laudo.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.laudo.RepositorioLaudosEmMemoria;
import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de GET .../laudos (G0 §5) com contexto completo, profile dev e repositório fake. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(LaudoTutorHttpTest.Config.class)
class LaudoTutorHttpTest {

    private static final String ROTA = "/v1/tutor/pets/{idPet}/laudos";
    private static final long PET_A = 100L;
    private static final long PET_B = 200L;
    private static final RequestPostProcessor TUTOR_A = user("a@kura.test");

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RepositorioLaudosEmMemoria repositorioLaudosEmMemoria() {
            return new RepositorioLaudosEmMemoria();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired RepositorioLaudosEmMemoria fake;
    @MockBean ContaTutorRepository contaTutorRepository;
    @MockBean PetRepository petRepository;

    @BeforeEach
    void preparar() {
        fake.limpar();
        when(contaTutorRepository.findIdTutorByEmail("a@kura.test")).thenReturn(Optional.of(1L));
        when(petRepository.countVinculo(PET_A, 1L)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.of(mock(Pet.class)));
    }

    private static LaudoTutor laudo(String chave) {
        return new LaudoTutor("66f000000000000000000001", "HEMOGRAMA", "Hemograma completo",
                Instant.parse("2026-09-10T12:00:00Z"), "Lab Vet", "application/pdf", 2048, chave);
    }

    @Test
    @DisplayName("200: itens com o shape do contrato, urlArquivo assinada e nenhum campo interno")
    void lista200() throws Exception {
        fake.semear(PET_A, laudo("clinica/1/pet/100/laudo/0a1b2c3d-0000-4000-8000-00000000abcd.pdf"));

        mockMvc.perform(get(ROTA, PET_A).with(TUTOR_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value("66f000000000000000000001"))
                .andExpect(jsonPath("$.itens[0].tipo").value("HEMOGRAMA"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Hemograma completo"))
                .andExpect(jsonPath("$.itens[0].dataExame").exists())
                .andExpect(jsonPath("$.itens[0].laboratorio").value("Lab Vet"))
                .andExpect(jsonPath("$.itens[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.itens[0].tamanhoBytes").value(2048))
                .andExpect(jsonPath("$.itens[0].urlArquivo").value(
                        org.hamcrest.Matchers.matchesPattern(
                                "http://localhost:8080/api/v1/laudos/arquivos/clinica/1/pet/100/laudo/[0-9a-f-]+\\.pdf\\?exp=\\d+&sig=[A-Za-z0-9_-]+")))
                .andExpect(jsonPath("$.itens[0].observacao").doesNotExist())
                .andExpect(jsonPath("$.itens[0].dados").doesNotExist())
                .andExpect(jsonPath("$.itens[0].idUsuarioAutor").doesNotExist())
                .andExpect(jsonPath("$.itens[0].idClinica").doesNotExist())
                .andExpect(jsonPath("$.itens[0].chaveArquivo").doesNotExist())
                .andExpect(jsonPath("$.itens[0].arquivo").doesNotExist());
    }

    @Test
    @DisplayName("200 com urlArquivo null quando a chave é de outro pet (documento malformado)")
    void chaveDeOutroPetViraNull() throws Exception {
        fake.semear(PET_A, laudo("clinica/1/pet/999/laudo/0a1b2c3d-0000-4000-8000-00000000abcd.pdf"));

        mockMvc.perform(get(ROTA, PET_A).with(TUTOR_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].urlArquivo").value((Object) null));
    }

    @Test
    @DisplayName("200 com lista vazia quando o pet não tem laudos")
    void vazio() throws Exception {
        mockMvc.perform(get(ROTA, PET_A).with(TUTOR_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    @DisplayName("401 sem token")
    void semToken401() throws Exception {
        mockMvc.perform(get(ROTA, PET_A)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403: pet de outro tutor")
    void petDeOutro403() throws Exception {
        mockMvc.perform(get(ROTA, PET_B).with(TUTOR_A)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404: pet inativo")
    void petInativo404() throws Exception {
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.empty());

        mockMvc.perform(get(ROTA, PET_A).with(TUTOR_A)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("400: idPet que não é número")
    void idPetInvalido400() throws Exception {
        mockMvc.perform(get(ROTA, "abc").with(TUTOR_A)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("503 LAUDOS_INDISPONIVEL quando o Mongo está fora")
    void mongoFora503() throws Exception {
        fake.indisponivel(true);

        mockMvc.perform(get(ROTA, PET_A).with(TUTOR_A))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("LAUDOS_INDISPONIVEL"));
    }
}
