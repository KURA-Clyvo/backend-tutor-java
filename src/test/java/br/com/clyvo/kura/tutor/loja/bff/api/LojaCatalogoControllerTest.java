// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.loja.application.CatalogoRepositorio;
import br.com.clyvo.kura.tutor.loja.application.CatalogoService;
import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.FatiaCatalogo;
import br.com.clyvo.kura.tutor.loja.application.ItemCatalogo;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import br.com.clyvo.kura.tutor.shared.exception.GlobalExceptionHandler;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MockMvc do catálogo da Loja com o repositório mockado (contrato S4-JAVA-DESIGN §5). */
@WebMvcTest(LojaCatalogoController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, GlobalExceptionHandler.class,
        LojaExceptionHandler.class, CatalogoService.class})
@TestPropertySource(properties = {
    "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
    "kura.jwt.access-expiration-minutes=15",
    "kura.jwt.refresh-expiration-days=7"
})
class LojaCatalogoControllerTest {

    /** Literal da D-S4-6, escrito aqui de propósito: não importa a constante de produção. */
    private static final String AVISO_LITERAL =
            "Pré-lançamento: pagamento e entrega serão ativados quando os parceiros logísticos "
            + "concluírem a integração. Nenhuma cobrança é feita.";

    @Autowired MockMvc mockMvc;

    @MockBean CatalogoRepositorio repositorio;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    private static ItemCatalogo item(String slug, CategoriaLoja categoria, String descricao) {
        return new ItemCatalogo(slug, "Nome " + slug, descricao, categoria, 4990L, "BRL", true,
                Map.of("tipoAlimento", "SECO"), null, null);
    }

    private static ItemCatalogo itemComFoto(String slug) {
        return new ItemCatalogo(slug, "Nome " + slug, "desc", CategoriaLoja.HIGIENE, 2990L, "BRL", true,
                Map.of("tipoHigiene", "SHAMPOO"), slug, "Foto ilustrativa: frasco de shampoo");
    }

    @Test
    @DisplayName("NX-04: item com imagem devolve url, urlThumb e alt; item sem imagem não traz o campo")
    @WithMockUser
    void itemComImagemDevolveUrlThumbEAlt() throws Exception {
        when(repositorio.listarAtivos(any(), eq(0), eq(20))).thenReturn(new FatiaCatalogo(
                List.of(itemComFoto("shampoo-neutro-500ml"), item("d-e-f", CategoriaLoja.HIGIENE, null)), false));

        mockMvc.perform(get("/v1/loja/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].imagem.url").value("/api/loja/imagens/shampoo-neutro-500ml.webp"))
                .andExpect(jsonPath("$.content[0].imagem.urlThumb").value("/api/loja/imagens/shampoo-neutro-500ml_256.webp"))
                .andExpect(jsonPath("$.content[0].imagem.alt").value("Foto ilustrativa: frasco de shampoo"))
                .andExpect(jsonPath("$.content[0].imagem.chave").doesNotExist())
                .andExpect(jsonPath("$.content[1].imagem").doesNotExist());
    }

    @Test
    @DisplayName("NX-04: foto da Loja é pública (200 image/webp sem token) e o catálogo segue 401 sem token")
    void imagemPublicaCatalogoProtegido() throws Exception {
        mockMvc.perform(get("/loja/imagens/shampoo-neutro-500ml_256.webp"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/webp"));
        mockMvc.perform(get("/loja/imagens/shampoo-neutro-500ml.webp")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/loja/catalogo")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NX-04: a rota pública não vira porta para outros estáticos: fora de /loja/imagens exige token")
    void foraDeLojaImagensExigeToken() throws Exception {
        mockMvc.perform(get("/loja/outra/x.webp")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("200 traz o aviso literal na raiz e exemplo por item, sem campos internos")
    @WithMockUser
    void respondeComAvisoLiteralEExemplo() throws Exception {
        when(repositorio.listarAtivos(any(), eq(0), eq(20))).thenReturn(new FatiaCatalogo(
                List.of(item("a-b-c", CategoriaLoja.ALIMENTACAO, "desc"), item("d-e-f", CategoriaLoja.HIGIENE, null)),
                false));

        mockMvc.perform(get("/v1/loja/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso").value(AVISO_LITERAL))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].exemplo").value(true))
                .andExpect(jsonPath("$.content[1].exemplo").value(true))
                .andExpect(jsonPath("$.content[0].preco").value(4990))
                .andExpect(jsonPath("$.content[0].categoria").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.content[0].dados.tipoAlimento").value("SECO"))
                .andExpect(jsonPath("$.content[1].descricao").doesNotExist())
                .andExpect(jsonPath("$.content[0]._id").doesNotExist())
                .andExpect(jsonPath("$.content[0].ordem").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("o filtro de categoria chega à consulta, com page e size")
    @WithMockUser
    void filtroDeCategoriaChegaAConsulta() throws Exception {
        when(repositorio.listarAtivos(eq(CategoriaLoja.BRINQUEDOS), eq(2), eq(5)))
                .thenReturn(new FatiaCatalogo(List.of(), true));

        mockMvc.perform(get("/v1/loja/catalogo").param("categoria", "BRINQUEDOS")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(repositorio).listarAtivos(CategoriaLoja.BRINQUEDOS, 2, 5);
    }

    @Test
    @DisplayName("sem JWT: 401")
    void semJwtRetorna401() throws Exception {
        mockMvc.perform(get("/v1/loja/catalogo")).andExpect(status().isUnauthorized());
        verify(repositorio, never()).listarAtivos(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("MongoTimeoutException: 503 CATALOGO_INDISPONIVEL, nunca 500, sem eco de URI")
    @WithMockUser
    void mongoTimeoutRetorna503() throws Exception {
        when(repositorio.listarAtivos(any(), anyInt(), anyInt()))
                .thenThrow(new MongoTimeoutException("mongodb://kura_java:segredo@kura-mongo:27017"));

        mockMvc.perform(get("/v1/loja/catalogo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"))
                .andExpect(jsonPath("$.path").value("/v1/loja/catalogo"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
    }

    @Test
    @DisplayName("DataAccessResourceFailureException: 503 CATALOGO_INDISPONIVEL")
    @WithMockUser
    void dataAccessResourceFailureRetorna503() throws Exception {
        when(repositorio.listarAtivos(any(), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("falha"));

        mockMvc.perform(get("/v1/loja/catalogo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
    }

    @Test
    @DisplayName("MongoSocketException: 503 CATALOGO_INDISPONIVEL")
    @WithMockUser
    void mongoSocketRetorna503() throws Exception {
        when(repositorio.listarAtivos(any(), anyInt(), anyInt()))
                .thenThrow(new MongoSocketException("falha", new ServerAddress("kura-mongo", 27017)));

        mockMvc.perform(get("/v1/loja/catalogo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
    }

    @Test
    @DisplayName("categoria fora do enum: 400 CATEGORIA_INVALIDA")
    @WithMockUser
    void categoriaInvalidaRetorna400() throws Exception {
        mockMvc.perform(get("/v1/loja/catalogo").param("categoria", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INVALIDA"));
        verify(repositorio, never()).listarAtivos(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("page e size fora dos limites: 400 VALIDACAO_INVALIDA (não limita em silêncio)")
    @WithMockUser
    void paginacaoForaDosLimitesRetorna400() throws Exception {
        for (String[] par : new String[][] {
                {"size", "0"}, {"size", "51"}, {"page", "-1"}, {"page", "10001"}}) {
            mockMvc.perform(get("/v1/loja/catalogo").param(par[0], par[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
        verify(repositorio, never()).listarAtivos(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("page não numérico: 400 VALIDACAO_INVALIDA")
    @WithMockUser
    void pageNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/v1/loja/catalogo").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
    }

    @Test
    @DisplayName("limites aceitos: page 10000 e size 50 passam")
    @WithMockUser
    void limitesAceitos() throws Exception {
        when(repositorio.listarAtivos(any(), eq(10000), eq(50))).thenReturn(new FatiaCatalogo(List.of(), false));
        mockMvc.perform(get("/v1/loja/catalogo").param("page", "10000").param("size", "50"))
                .andExpect(status().isOk());
    }
}
