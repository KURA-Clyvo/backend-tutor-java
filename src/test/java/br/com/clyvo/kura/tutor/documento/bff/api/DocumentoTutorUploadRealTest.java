package br.com.clyvo.kura.tutor.documento.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.documento.RepositorioDocumentosEmMemoria;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HTTP REAL (servidor embarcado em porta aleatória, context-path /api): o MockMvc não aplica o teto
 * do multipart, então o 413 de 5 MiB + 1 byte só se prova pela rede. Prova também que o handler
 * da MaxUploadSizeExceededException é alcançável (a exceção nasce antes de existir handler method).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DocumentoTutorUploadRealTest.Config.class)
class DocumentoTutorUploadRealTest {

    private static final long PET = 100L;
    private static final long CINCO_MIB = 5L * 1024 * 1024;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RepositorioDocumentosEmMemoria repositorioEmMemoria() {
            return new RepositorioDocumentosEmMemoria();
        }
    }

    @LocalServerPort int porta;
    @Autowired JwtTokenProvider jwt;
    @Autowired RepositorioDocumentosEmMemoria fake;
    @MockBean ContaTutorRepository contaTutorRepository;
    @MockBean PetRepository petRepository;
    @MockBean UserDetailsService userDetailsService;

    private String token;

    @BeforeEach
    void preparar() {
        fake.limpar();
        when(contaTutorRepository.findIdTutorByEmail("a@kura.test")).thenReturn(Optional.of(1L));
        when(petRepository.countVinculo(PET, 1L)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET, "S")).thenReturn(Optional.of(mock(Pet.class)));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(new User("a@kura.test", "x", List.of()));
        ContaTutor conta = new ContaTutor();
        conta.setIdConta(42L);
        conta.setDsEmailLogin("a@kura.test");
        token = jwt.gerarAccess(conta);
    }

    private HttpResponse<String> postar(byte[] arquivo) throws Exception {
        String limite = "----kura" + System.nanoTime();
        ByteArrayOutputStream corpo = new ByteArrayOutputStream();
        corpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"tipo\"\r\n\r\nRG_ANIMAL\r\n")
                .getBytes(StandardCharsets.UTF_8));
        corpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"rg.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        corpo.write(arquivo);
        corpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/tutor/pets/" + PET + "/documentos"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpo.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] jpegDeTamanho(long tamanho) {
        byte[] b = new byte[(int) tamanho];
        System.arraycopy(JPEG, 0, b, 0, JPEG.length);
        return b;
    }

    @Test
    @DisplayName("5 MiB + 1 byte pela rede: 413 ARQUIVO_MUITO_GRANDE, sem stack trace, nada gravado")
    void cincoMibMaisUmByte413() throws Exception {
        HttpResponse<String> resposta = postar(jpegDeTamanho(CINCO_MIB + 1));

        assertThat(resposta.statusCode()).isEqualTo(413);
        assertThat(resposta.body()).contains("\"codigo\":\"ARQUIVO_MUITO_GRANDE\"")
                .doesNotContain("Exception").doesNotContain("at org.springframework").doesNotContain("trace");
        assertThat(fake.total()).isZero();
    }

    @Test
    @DisplayName("8 MiB pela rede: 413 ARQUIVO_MUITO_GRANDE (e nao reset de conexao), nada gravado")
    void oitoMib413() throws Exception {
        HttpResponse<String> resposta = postar(jpegDeTamanho(8L * 1024 * 1024));

        assertThat(resposta.statusCode()).isEqualTo(413);
        assertThat(resposta.body()).contains("\"codigo\":\"ARQUIVO_MUITO_GRANDE\"");
        assertThat(fake.total()).isZero();
    }

    @Test
    @DisplayName("controle positivo: exatamente 5 MiB passa o teto do multipart e grava (201, urlArquivo com /api)")
    void exatamenteCincoMib201() throws Exception {
        HttpResponse<String> resposta = postar(jpegDeTamanho(CINCO_MIB));

        assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(resposta.headers().firstValue("Location").orElseThrow())
                .matches("/api/v1/tutor/pets/100/documentos/[0-9a-f]{24}/arquivo");
        assertThat(fake.total()).isEqualTo(1);
    }
}
