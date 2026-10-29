package br.com.clyvo.kura.tutor.documento.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.documento.RepositorioDocumentosEmMemoria;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutorService;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP dos documentos do pet (G0 §5) com o contexto completo, profile dev, e o repositório
 * de documentos trocado por um fake em memória escopado por tutor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(DocumentoTutorHttpTest.Config.class)
class DocumentoTutorHttpTest {

    private static final String BASE = "/v1/tutor/pets/{idPet}/documentos";
    private static final long PET_A = 100L;
    private static final long PET_B = 200L;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 1, 2};
    private static final RequestPostProcessor TUTOR_A = user("a@kura.test");
    private static final RequestPostProcessor TUTOR_B = user("b@kura.test");

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RepositorioDocumentosEmMemoria repositorioEmMemoria() {
            return new RepositorioDocumentosEmMemoria();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired RepositorioDocumentosEmMemoria fake;
    @MockBean ContaTutorRepository contaTutorRepository;
    @MockBean PetRepository petRepository;

    @BeforeEach
    void preparar() {
        fake.limpar();
        when(contaTutorRepository.findIdTutorByEmail("a@kura.test")).thenReturn(Optional.of(1L));
        when(contaTutorRepository.findIdTutorByEmail("b@kura.test")).thenReturn(Optional.of(2L));
        when(petRepository.countVinculo(PET_A, 1L)).thenReturn(1L);
        when(petRepository.countVinculo(PET_B, 2L)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.of(mock(Pet.class)));
        when(petRepository.findByIdPetAndStAtivo(PET_B, "S")).thenReturn(Optional.of(mock(Pet.class)));
    }

    private MockHttpServletRequestBuilder envio(long idPet, byte[] bytes, String nome, String contentTypeDoCliente) {
        return multipart(BASE, idPet)
                .file(new MockMultipartFile("arquivo", nome, contentTypeDoCliente, bytes))
                .param("tipo", "RG_ANIMAL");
    }

    private String enviarEPegarId(RequestPostProcessor quem, long idPet) throws Exception {
        String corpo = mockMvc.perform(envio(idPet, JPEG, "rg.jpg", "image/jpeg").with(quem))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(corpo, "$.id");
    }

    @Test
    @DisplayName("201 + Location + corpo do contrato; contentType vem da assinatura, não do cliente")
    void cria201() throws Exception {
        mockMvc.perform(envio(PET_A, JPEG, "foto.pdf", "application/pdf")
                        .param("emissor", "SINPATINHAS").param("numero", "985-112.34").with(TUTOR_A))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "/v1/tutor/pets/100/documentos/[0-9a-f]{24}/arquivo")))
                .andExpect(jsonPath("$.idPet").value(PET_A))
                .andExpect(jsonPath("$.tipo").value("RG_ANIMAL"))
                .andExpect(jsonPath("$.emissor").value("SINPATINHAS"))
                .andExpect(jsonPath("$.numero").value("985-112.34"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.tamanhoBytes").value(JPEG.length))
                .andExpect(jsonPath("$.urlArquivo").exists())
                .andExpect(jsonPath("$.nomeArquivo").doesNotExist())
                .andExpect(jsonPath("$.arquivo").doesNotExist());
    }

    @Test
    @DisplayName("401 sem token")
    void semTokenE401() throws Exception {
        mockMvc.perform(envio(PET_A, JPEG, "rg.jpg", "image/jpeg")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400: sem tipo, tipo desconhecido, arquivo vazio, número com espaço")
    void validacao400() throws Exception {
        mockMvc.perform(multipart(BASE, PET_A).file(new MockMultipartFile("arquivo", "rg.jpg", "image/jpeg", JPEG))
                        .with(TUTOR_A)).andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE, PET_A)
                        .file(new MockMultipartFile("arquivo", "rg.jpg", "image/jpeg", JPEG))
                        .param("tipo", "CPF").with(TUTOR_A))
                .andExpect(status().isBadRequest());
        mockMvc.perform(envio(PET_A, new byte[0], "rg.jpg", "image/jpeg").with(TUTOR_A))
                .andExpect(status().isBadRequest());
        mockMvc.perform(envio(PET_A, JPEG, "rg.jpg", "image/jpeg").param("numero", "12 34").with(TUTOR_A))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE, PET_A).param("tipo", "RG_ANIMAL").with(TUTOR_A))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("M-3: POST com Content-Type nao-multipart (JSON, texto, sem corpo): 415, sem stack trace, nada gravado")
    void postNaoMultipart415() throws Exception {
        for (String tipo : new String[] {"application/json", "text/plain"}) {
            mockMvc.perform(post(BASE, PET_A).contentType(tipo).content("{\"tipo\":\"RG_ANIMAL\"}").with(TUTOR_A))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.codigo").value("CONTENT_TYPE_NAO_SUPORTADO"))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
        mockMvc.perform(post(BASE, PET_A).with(TUTOR_A)).andExpect(status().isUnsupportedMediaType());
        assertThat(fake.total()).isZero();
    }

    @Test
    @DisplayName("403: pet de outro tutor (envio, lista, arquivo); apagar vira 404 (M-4: sem checagem de vinculo)")
    void petDeOutroTutor403() throws Exception {
        mockMvc.perform(envio(PET_B, JPEG, "rg.jpg", "image/jpeg").with(TUTOR_A)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE, PET_B).with(TUTOR_A)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/{id}/arquivo", PET_B, "000000000000000000000001").with(TUTOR_A))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/{id}", PET_B, "000000000000000000000001").with(TUTOR_A))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("M-4: tutor sem vinculo ativo (e pet inativo) apaga o PROPRIO documento: 204; outro tutor sem vinculo: 404 e o documento continua")
    void apagarProprioDocumentoSemVinculo() throws Exception {
        String idDoA = enviarEPegarId(TUTOR_A, PET_A);
        when(petRepository.countVinculo(PET_A, 1L)).thenReturn(0L);
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE + "/{id}", PET_A, idDoA).with(TUTOR_B)).andExpect(status().isNotFound());
        assertThat(fake.total()).isEqualTo(1);
        mockMvc.perform(delete(BASE + "/{id}", PET_A, idDoA).with(TUTOR_A)).andExpect(status().isNoContent());
        assertThat(fake.total()).isZero();
        mockMvc.perform(delete(BASE + "/{id}", PET_A, idDoA).with(TUTOR_A)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404: pet inativo")
    void petInativo404() throws Exception {
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE, PET_A).with(TUTOR_A)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404 (e não 403): abrir e apagar documento de OUTRO tutor; o documento continua do dono")
    void documentoDeOutroTutor404() throws Exception {
        String idDoA = enviarEPegarId(TUTOR_A, PET_A);
        // B tem vínculo com o PET_B mas tenta alcançar o documento de A pelo id, no próprio pet
        when(petRepository.countVinculo(PET_A, 2L)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET_A, "S")).thenReturn(Optional.of(mock(Pet.class)));

        mockMvc.perform(get(BASE + "/{id}/arquivo", PET_A, idDoA).with(TUTOR_B)).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/{id}", PET_A, idDoA).with(TUTOR_B)).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE, PET_A).with(TUTOR_B)).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
        mockMvc.perform(get(BASE + "/{id}/arquivo", PET_A, idDoA).with(TUTOR_A)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("409 no 21o documento do mesmo pet")
    void teto409() throws Exception {
        for (int i = 0; i < DocumentoTutorService.TETO_POR_PET; i++) {
            enviarEPegarId(TUTOR_A, PET_A);
        }

        mockMvc.perform(envio(PET_A, JPEG, "rg.jpg", "image/jpeg").with(TUTOR_A))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("LIMITE_DOCUMENTOS"));
    }

    @Test
    @DisplayName("413 quando o service recebe mais de 5 MiB")
    void arquivoGrande413() throws Exception {
        byte[] grande = new byte[(int) DocumentoTutorService.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

        mockMvc.perform(envio(PET_A, grande, "rg.jpg", "image/jpeg").with(TUTOR_A))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("ARQUIVO_MUITO_GRANDE"));
        assertThat(fake.total()).isZero();
    }

    @Test
    @DisplayName("415: texto renomeado .jpg e cabeçalho de cliente image/jpeg não enganam a assinatura")
    void textoRenomeado415() throws Exception {
        mockMvc.perform(envio(PET_A, "so texto".getBytes(), "rg.jpg", "image/jpeg").with(TUTOR_A))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("TIPO_ARQUIVO_NAO_SUPORTADO"));
        assertThat(fake.total()).isZero();
    }

    @Test
    @DisplayName("503 DOCUMENTOS_INDISPONIVEL quando o Mongo está fora")
    void mongoFora503() throws Exception {
        fake.indisponivel(true);

        mockMvc.perform(get(BASE, PET_A).with(TUTOR_A))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTOS_INDISPONIVEL"));
        mockMvc.perform(envio(PET_A, JPEG, "rg.jpg", "image/jpeg").with(TUTOR_A))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("lista sem bytes; /arquivo com Content-Type gravado, nosniff e no-store/private; 204 ao apagar e depois 404")
    void listaArquivoEApagar() throws Exception {
        String id = enviarEPegarId(TUTOR_A, PET_A);

        mockMvc.perform(get(BASE, PET_A).with(TUTOR_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id))
                .andExpect(jsonPath("$.itens[0].urlArquivo")
                        .value("/v1/tutor/pets/100/documentos/" + id + "/arquivo"))
                .andExpect(jsonPath("$.itens[0].conteudo").doesNotExist());
        mockMvc.perform(get(BASE + "/{id}/arquivo", PET_A, id).with(TUTOR_A))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("no-store"),
                        org.hamcrest.Matchers.containsString("private"))));
        mockMvc.perform(delete(BASE + "/{id}", PET_A, id).with(TUTOR_A)).andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/{id}/arquivo", PET_A, id).with(TUTOR_A)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("LGPD: nenhum log com conteúdo, número do documento ou nome do arquivo enviado (com controle positivo)")
    void semPiiNoLog(CapturedOutput saida) throws Exception {
        byte[] conteudo = new byte[JPEG.length + 20];
        System.arraycopy(JPEG, 0, conteudo, 0, JPEG.length);
        byte[] marca = "MARCA-CONTEUDO-SECRETO".getBytes();
        System.arraycopy(marca, 0, conteudo, JPEG.length, Math.min(marca.length, 20));

        mockMvc.perform(envio(PET_A, conteudo, "rg-do-joao-silva.jpg", "image/jpeg")
                        .param("numero", "NUMERO-SECRETO-987").with(TUTOR_A))
                .andExpect(status().isCreated());
        mockMvc.perform(envio(PET_A, "texto".getBytes(), "outro-nome-privado.jpg", "image/jpeg")
                        .param("numero", "NUMERO-SECRETO-988").with(TUTOR_A))
                .andExpect(status().isUnsupportedMediaType());

        // controle positivo: a captura enxerga um log emitido por este teste e o log do próprio fluxo
        LoggerFactory.getLogger("controle.positivo").info("CONTROLE-POSITIVO-DO-LOG");
        assertThat(saida.getAll()).contains("CONTROLE-POSITIVO-DO-LOG");
        assertThat(saida.getAll()).contains("Documento do pet gravado");

        assertThat(saida.getAll())
                .doesNotContain("rg-do-joao-silva").doesNotContain("outro-nome-privado")
                .doesNotContain("NUMERO-SECRETO").doesNotContain("MARCA-CONTEUDO");
    }
}
