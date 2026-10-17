// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contexto COMPLETO com runner e seed LIGADOS e um Mongo inalcançável (porta 1): a aplicação
 * tem de subir, o /actuator/health seguir 200 e os dois runners falharem sem propagar.
 * É o teste que falha se a captura do runner for removida (o contexto não sobe).
 */
@SpringBootTest(properties = {
        "kura.loja.mongo.init-enabled=true",
        "kura.loja.seed-exemplo.enabled=true",
        "spring.data.mongodb.uri=mongodb://kura_java:senha-de-teste@localhost:1/kura_docs?serverSelectionTimeoutMS=300"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LojaMongoForaDoArContextoTest {

    @Autowired ApplicationContext contexto;
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Mongo fora + runner e seed ligados: contexto sobe, runners existem, health 200, 1 WARN de cada")
    void contextoSobeComMongoFora(CapturedOutput saida) throws Exception {
        assertThat(contexto.getBeansOfType(LojaMongoInicializador.class)).hasSize(1);
        assertThat(contexto.getBeansOfType(LojaSeedInicializador.class)).hasSize(1);

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        String log = saida.getAll();
        assertThat(count(log, "Loja: schema do Mongo não aplicado nesta subida")).isEqualTo(1);
        assertThat(count(log, "Loja: seed de exemplo não aplicado nesta subida")).isEqualTo(1);
        assertThat(log).doesNotContain("senha-de-teste");
    }

    private static int count(String texto, String trecho) {
        int n = 0;
        for (int i = texto.indexOf(trecho); i >= 0; i = texto.indexOf(trecho, i + 1)) {
            n++;
        }
        return n;
    }
}
