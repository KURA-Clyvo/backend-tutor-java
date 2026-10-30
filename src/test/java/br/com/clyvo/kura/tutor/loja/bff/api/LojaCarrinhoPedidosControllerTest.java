package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.loja.application.CarrinhoService;
import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.FatiaPedidos;
import br.com.clyvo.kura.tutor.loja.application.ItemPedido;
import br.com.clyvo.kura.tutor.loja.application.LimiteCarrinhoException;
import br.com.clyvo.kura.tutor.loja.application.LojaException;
import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import br.com.clyvo.kura.tutor.loja.application.PedidoLojaService;
import br.com.clyvo.kura.tutor.loja.application.ResultadoPedido;
import br.com.clyvo.kura.tutor.loja.application.StatusPedido;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.CarrinhoResponse;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
import br.com.clyvo.kura.tutor.shared.exception.GlobalExceptionHandler;
import br.com.clyvo.kura.tutor.shared.security.TutorAutenticado;
import com.mongodb.MongoTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc do carrinho e dos pedidos: cada linha da tabela de erros do design, 401 sem JWT, 503 com
 * Mongo fora, validação (o {@code @Validated} de classe) e o idTutor vindo só do JWT.
 */
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest({LojaCarrinhoController.class, LojaPedidosController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, GlobalExceptionHandler.class,
        LojaExceptionHandler.class})
@TestPropertySource(properties = {
    "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
    "kura.jwt.access-expiration-minutes=15",
    "kura.jwt.refresh-expiration-days=7"
})
class LojaCarrinhoPedidosControllerTest {

    private static final String AVISO_LITERAL =
            "Pré-lançamento: pagamento e entrega serão ativados quando os parceiros logísticos "
            + "concluírem a integração. Nenhuma cobrança é feita.";
    private static final String CHAVE = "3f2b8c1e-5d4a-4b6f-9a7c-1e2d3c4b5a69";
    private static final String ID_PEDIDO = "6720f0c1a2b3c4d5e6f70812";

    @Autowired MockMvc mockMvc;

    @MockBean CarrinhoService carrinhoService;
    @MockBean PedidoLojaService pedidoService;
    @MockBean TutorAutenticado tutorAutenticado;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void tutores() {
        when(tutorAutenticado.idTutor(any(Authentication.class))).thenAnswer(inv ->
                "b@kura.test".equals(((Authentication) inv.getArgument(0)).getName()) ? 2L : 1L);
    }

    private static PedidoLoja pedido(StatusPedido status) {
        return new PedidoLoja(ID_PEDIDO, status,
                List.of(new ItemPedido("shampoo-neutro-500ml", "Shampoo", CategoriaLoja.HIGIENE, 2990, 2, true)),
                5980, Instant.parse("2026-10-30T18:00:00Z"), Instant.parse("2026-10-30T18:00:01Z"), null);
    }

    private static CarrinhoResponse carrinhoVazio() {
        return new CarrinhoResponse(AVISO_LITERAL, List.of(), 0, "BRL", 0, 0, null);
    }

    // ------------------------------------------------------------------ 401

    @Test
    @DisplayName("sem JWT: 401 em todas as rotas, e nada é chamado")
    void semJwt() throws Exception {
        mockMvc.perform(get("/v1/loja/carrinho")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/loja/carrinho/itens").contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"abc\",\"quantidade\":1}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/v1/loja/carrinho/itens/abc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":1}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/loja/carrinho/itens/abc")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/loja/carrinho")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/loja/pedidos").header("Idempotency-Key", CHAVE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/loja/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/loja/pedidos/" + ID_PEDIDO)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/loja/pedidos/" + ID_PEDIDO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(carrinhoService, pedidoService);
    }

    // ------------------------------------------------------------------ carrinho: sucesso

    @Test
    @DisplayName("GET carrinho: 200 com o aviso literal; o idTutor vem do JWT")
    void verCarrinho() throws Exception {
        when(carrinhoService.ver(2L)).thenReturn(carrinhoVazio());

        mockMvc.perform(get("/v1/loja/carrinho").with(user("b@kura.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso").value(AVISO_LITERAL))
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.expiraEm").doesNotExist())
                .andExpect(jsonPath("$.moeda").value("BRL"));

        verify(carrinhoService).ver(2L);
    }

    @Test
    @DisplayName("POST itens, PUT itens/{slug}, DELETE itens/{slug} devolvem 200 com o carrinho; DELETE carrinho dá 204")
    void escritasDoCarrinho() throws Exception {
        when(carrinhoService.adicionar(1L, "shampoo-neutro-500ml", 2)).thenReturn(carrinhoVazio());
        when(carrinhoService.definir(1L, "shampoo-neutro-500ml", 5)).thenReturn(carrinhoVazio());
        when(carrinhoService.remover(1L, "shampoo-neutro-500ml")).thenReturn(carrinhoVazio());

        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"shampoo-neutro-500ml\",\"quantidade\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/v1/loja/carrinho/itens/shampoo-neutro-500ml").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":5}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/v1/loja/carrinho/itens/shampoo-neutro-500ml").with(user("a@kura.test")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/v1/loja/carrinho").with(user("a@kura.test"))).andExpect(status().isNoContent());

        verify(carrinhoService).esvaziar(1L);
    }

    // ------------------------------------------------------------------ carrinho: erros

    @Test
    @DisplayName("400 VALIDACAO_INVALIDA: quantidade 0 e 100, ausente, slug maiúsculo/com '.'/vazio/longo, no corpo e no path; o service nunca é chamado")
    void validacaoDoCarrinho() throws Exception {
        for (String corpo : new String[] {
                "{\"slug\":\"abc\",\"quantidade\":0}", "{\"slug\":\"abc\",\"quantidade\":100}",
                "{\"slug\":\"abc\"}", "{\"slug\":\"Shampoo\",\"quantidade\":1}", "{\"slug\":\"a.b\",\"quantidade\":1}",
                "{\"slug\":\"\",\"quantidade\":1}", "{\"quantidade\":1}",
                "{\"slug\":\"" + "a".repeat(81) + "\",\"quantidade\":1}"}) {
            mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
        for (String quantidade : new String[] {"0", "100"}) {
            mockMvc.perform(put("/v1/loja/carrinho/itens/abc").with(user("a@kura.test"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":" + quantidade + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
        for (String slug : new String[] {"Shampoo", "a.b", "a-", "-a", "a--b"}) {
            mockMvc.perform(put("/v1/loja/carrinho/itens/" + slug).with(user("a@kura.test"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
            mockMvc.perform(delete("/v1/loja/carrinho/itens/" + slug).with(user("a@kura.test")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
        verifyNoInteractions(carrinhoService);
    }

    @Test
    @DisplayName("404 PRODUTO_NAO_ENCONTRADO e 422 LIMITE_CARRINHO (mensagem fixa, sem slug nem quantidade)")
    void errosDeRegraDoCarrinho() throws Exception {
        when(carrinhoService.adicionar(anyLong(), eq("nao-existe"), anyInt())).thenThrow(LojaException.produtoNaoEncontrado());
        when(carrinhoService.adicionar(anyLong(), eq("shampoo-neutro-500ml"), anyInt())).thenThrow(new LimiteCarrinhoException());

        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"nao-existe\",\"quantidade\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PRODUTO_NAO_ENCONTRADO"));
        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"shampoo-neutro-500ml\",\"quantidade\":9}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("LIMITE_CARRINHO"))
                .andExpect(jsonPath("$.mensagem").value(
                        "Limite do carrinho: até 99 unidades por produto e 50 produtos diferentes."));
    }

    // ------------------------------------------------------------------ pedidos

    @Test
    @DisplayName("POST pedidos: 201 com Location e o aviso; a repetição da chave devolve 200 com o mesmo Location")
    void criarPedido() throws Exception {
        when(pedidoService.criar(1L, CHAVE)).thenReturn(new ResultadoPedido(pedido(StatusPedido.AGUARDANDO_PARCEIRO), true));
        when(pedidoService.criar(1L, "7f2b8c1e-5d4a-4b6f-9a7c-1e2d3c4b5a69"))
                .thenReturn(new ResultadoPedido(pedido(StatusPedido.AGUARDANDO_PARCEIRO), false));

        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test")).header("Idempotency-Key", CHAVE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/loja/pedidos/" + ID_PEDIDO))
                .andExpect(jsonPath("$.aviso").value(AVISO_LITERAL))
                .andExpect(jsonPath("$.id").value(ID_PEDIDO))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PARCEIRO"))
                .andExpect(jsonPath("$.total").value(5980))
                .andExpect(jsonPath("$.itens[0].subtotal").value(5980))
                .andExpect(jsonPath("$.canceladoEm").doesNotExist())
                .andExpect(jsonPath("$.idTutor").doesNotExist())
                .andExpect(jsonPath("$.chaveIdempotencia").doesNotExist())
                .andExpect(jsonPath("$.carrinhoVersao").doesNotExist());
        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test"))
                        .header("Idempotency-Key", "7f2b8c1e-5d4a-4b6f-9a7c-1e2d3c4b5a69"))
                .andExpect(status().isOk())
                .andExpect(header().string("Location", "/v1/loja/pedidos/" + ID_PEDIDO));
    }

    @Test
    @DisplayName("Idempotency-Key ausente: 400 HEADER_AUSENTE; não-UUID: 400 IDEMPOTENCY_KEY_INVALIDA")
    void chaveDeIdempotencia() throws Exception {
        when(pedidoService.criar(anyLong(), eq("abc"))).thenThrow(LojaException.idempotencyKeyInvalida());

        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("HEADER_AUSENTE"));
        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test")).header("Idempotency-Key", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IDEMPOTENCY_KEY_INVALIDA"));
    }

    @Test
    @DisplayName("422 CARRINHO_VAZIO e ITEM_INDISPONIVEL (slugs em detalhes)")
    void errosDoPedido() throws Exception {
        when(pedidoService.criar(1L, CHAVE)).thenThrow(LojaException.carrinhoVazio());
        when(pedidoService.criar(2L, CHAVE)).thenThrow(LojaException.itemIndisponivel(List.of("escova-cerdas-macias")));

        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test")).header("Idempotency-Key", CHAVE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CARRINHO_VAZIO"));
        mockMvc.perform(post("/v1/loja/pedidos").with(user("b@kura.test")).header("Idempotency-Key", CHAVE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ITEM_INDISPONIVEL"))
                .andExpect(jsonPath("$.detalhes[0]").value("escova-cerdas-macias"));
    }

    @Test
    @DisplayName("listar: envelope Slice com o aviso na raiz e NÃO em cada pedido")
    void listarPedidos() throws Exception {
        when(pedidoService.listar(1L, 0, 20)).thenReturn(new FatiaPedidos(List.of(pedido(StatusPedido.CANCELADO)), true));

        mockMvc.perform(get("/v1/loja/pedidos").with(user("a@kura.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso").value(AVISO_LITERAL))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].aviso").doesNotExist())
                .andExpect(jsonPath("$.content[0].status").value("CANCELADO"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("listar: page e size fora dos limites dão 400 VALIDACAO_INVALIDA (size=0, 51, page=-1, 10001)")
    void paginacaoInvalida() throws Exception {
        for (String[] par : new String[][] {{"size", "0"}, {"size", "51"}, {"page", "-1"}, {"page", "10001"}}) {
            mockMvc.perform(get("/v1/loja/pedidos").with(user("a@kura.test")).param(par[0], par[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
        verify(pedidoService, never()).listar(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("detalhe e cancelar: o idTutor do service vem do JWT (tutor B chama com 2L); 404 PEDIDO_NAO_ENCONTRADO com corpo igual para id alheio e malformado")
    void detalheECancelar() throws Exception {
        when(pedidoService.detalhar(1L, ID_PEDIDO)).thenReturn(pedido(StatusPedido.AGUARDANDO_PARCEIRO));
        when(pedidoService.detalhar(eq(2L), anyString())).thenThrow(LojaException.pedidoNaoEncontrado());
        when(pedidoService.cancelar(1L, ID_PEDIDO)).thenReturn(pedido(StatusPedido.CANCELADO));
        when(pedidoService.cancelar(eq(2L), anyString())).thenThrow(LojaException.pedidoNaoEncontrado());

        mockMvc.perform(get("/v1/loja/pedidos/" + ID_PEDIDO).with(user("a@kura.test")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID_PEDIDO));
        mockMvc.perform(delete("/v1/loja/pedidos/" + ID_PEDIDO).with(user("a@kura.test")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mockMvc.perform(get("/v1/loja/pedidos/" + ID_PEDIDO).with(user("b@kura.test")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PEDIDO_NAO_ENCONTRADO"));
        mockMvc.perform(get("/v1/loja/pedidos/nao-e-objectid").with(user("b@kura.test")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PEDIDO_NAO_ENCONTRADO"));
        mockMvc.perform(delete("/v1/loja/pedidos/" + ID_PEDIDO).with(user("b@kura.test")))
                .andExpect(status().isNotFound());
        verify(pedidoService).detalhar(2L, ID_PEDIDO);
        verify(pedidoService).cancelar(2L, ID_PEDIDO);
    }

    // ------------------------------------------------------------------ 503

    @Test
    @DisplayName("Mongo fora: 503 CATALOGO_INDISPONIVEL em todas as rotas de carrinho e pedidos, nunca 500, sem eco de URI")
    void mongoForaDa503() throws Exception {
        MongoTimeoutException timeout = new MongoTimeoutException("mongodb://kura_java:segredo@kura-mongo:27017");
        when(carrinhoService.ver(anyLong())).thenThrow(timeout);
        when(carrinhoService.adicionar(anyLong(), anyString(), anyInt())).thenThrow(new DataAccessResourceFailureException("x"));
        when(pedidoService.criar(anyLong(), anyString())).thenThrow(timeout);
        when(pedidoService.listar(anyLong(), anyInt(), anyInt())).thenThrow(timeout);
        when(pedidoService.detalhar(anyLong(), anyString())).thenThrow(timeout);
        when(pedidoService.cancelar(anyLong(), anyString())).thenThrow(timeout);

        mockMvc.perform(get("/v1/loja/carrinho").with(user("a@kura.test")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"abc\",\"quantidade\":1}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
        mockMvc.perform(post("/v1/loja/pedidos").with(user("a@kura.test")).header("Idempotency-Key", CHAVE))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
        mockMvc.perform(get("/v1/loja/pedidos").with(user("a@kura.test")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
        mockMvc.perform(get("/v1/loja/pedidos/" + ID_PEDIDO).with(user("a@kura.test")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
        mockMvc.perform(delete("/v1/loja/pedidos/" + ID_PEDIDO).with(user("a@kura.test")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("CATALOGO_INDISPONIVEL"));
    }

    // ------------------------------------------------------------------ corpo ilegivel (I-1)

    /** Só as linhas WARN/ERROR: o DEBUG do Spring MVC no profile dev ecoa a exceção e não existe em prod (INFO). */
    private static String avisosEErros(CapturedOutput saida) {
        return saida.getAll().lines().filter(l -> l.contains("[WARN]") || l.contains("[ERROR]"))
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    @Test
    @DisplayName("controle positivo: o capturador de log enxerga uma linha WARN escrita pelo teste")
    void capturadorEnxerga(CapturedOutput saida) {
        org.slf4j.LoggerFactory.getLogger("teste.controle").warn("ISCA-DE-CONTROLE-42");

        org.assertj.core.api.Assertions.assertThat(avisosEErros(saida)).contains("ISCA-DE-CONTROLE-42");
    }

    @Test
    @DisplayName("corpo ilegivel: JSON malformado, ausente, quantidade texto, acima de int e vazio dão 400 VALIDACAO_INVALIDA; text/plain dá 415; nunca 500; o log não traz o valor do corpo")
    void corpoIlegivel(CapturedOutput saida) throws Exception {
        String isca = "SEGREDO77";
        String[] corpos = {"{", "{\"slug\":\"abc\",\"quantidade\":\"" + isca + "\"}", "{\"slug\":\"abc\",\"quantidade\":99999999999}"};
        for (String corpo : corpos) {
            mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                    .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido."));
        }
        mockMvc.perform(put("/v1/loja/carrinho/itens/abc").with(user("a@kura.test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":99999999999}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        mockMvc.perform(post("/v1/loja/carrinho/itens").with(user("a@kura.test"))
                        .contentType(MediaType.TEXT_PLAIN).content("{\"slug\":\"abc\",\"quantidade\":1}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("CONTENT_TYPE_NAO_SUPORTADO"));

        org.assertj.core.api.Assertions.assertThat(avisosEErros(saida)).contains("corpo da requisição ilegível")
                .doesNotContain(isca).doesNotContain("99999999999").doesNotContain("Unhandled exception");
        verifyNoInteractions(carrinhoService);
    }
}
