// KURA-LOJA — catálogo MongoDB (Sprint 4). Fixa que o advice da Loja não vaza para outros pacotes.
package br.com.clyvo.kura.tutor.shared.exception;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.loja.bff.api.LojaExceptionHandler;
import br.com.clyvo.kura.tutor.shared.config.SecurityConfig;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O LojaExceptionHandler é restrito ao pacote loja (design §5.1): um controller de OUTRO pacote
 * continua recebendo o tratamento do GlobalExceptionHandler. Sem o basePackages, o advice da Loja
 * (HIGHEST_PRECEDENCE) responderia 503/400 no lugar.
 */
@WebMvcTest(LojaExceptionHandlerIsolamentoTest.OutroPacoteController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, GlobalExceptionHandler.class,
        LojaExceptionHandler.class, LojaExceptionHandlerIsolamentoTest.OutroPacoteController.class})
@TestPropertySource(properties = {
    "kura.jwt.secret=dev-secret-trocar-em-prod-com-no-minimo-64-bytes-aqui-para-test-kura",
    "kura.jwt.access-expiration-minutes=15",
    "kura.jwt.refresh-expiration-days=7"
})
class LojaExceptionHandlerIsolamentoTest {

    @RestController
    static class OutroPacoteController {
        @GetMapping("/teste-isolamento/banco")
        String banco() {
            throw new DataAccessResourceFailureException("falha de teste");
        }

        @GetMapping("/teste-isolamento/tipo")
        String tipo(@RequestParam int categoria) {
            return "ok";
        }
    }

    @Autowired MockMvc mockMvc;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("DataAccessResourceFailureException fora do pacote loja NÃO vira 503 CATALOGO_INDISPONIVEL")
    @WithMockUser
    void excecaoDeBancoForaDaLojaNaoViraCatalogoIndisponivel() throws Exception {
        mockMvc.perform(get("/teste-isolamento/banco"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));
    }

    @Test
    @DisplayName("parâmetro categoria de tipo errado fora do pacote loja NÃO vira 400 CATEGORIA_INVALIDA")
    @WithMockUser
    void tipoErradoForaDaLojaNaoViraCategoriaInvalida() throws Exception {
        mockMvc.perform(get("/teste-isolamento/tipo").param("categoria", "xyz"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));
    }
}
