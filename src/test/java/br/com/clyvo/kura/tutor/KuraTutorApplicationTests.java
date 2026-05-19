package br.com.clyvo.kura.tutor;

import br.com.clyvo.kura.tutor.security.JWTUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao basicos — equivalente ao ProjetoMusicaApplicationTests da aula.
 *
 * Cobre os cenarios criticos identificados na analise de QA:
 * - Contexto sobe sem erro
 * - Rotas protegidas exigem JWT
 * - Rotas publicas funcionam sem JWT
 * - Token invalido retorna 403
 * - Payload invalido retorna 400 com mapa de erros
 */
@SpringBootTest
@AutoConfigureMockMvc
class KuraTutorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("Contexto Spring deve subir sem erros")
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
        assertThat(jwtUtil).isNotNull();
    }

    @Test
    @DisplayName("GET /api/especies deve retornar 200 sem autenticacao (rota publica)")
    void especiesPublico() throws Exception {
        mockMvc.perform(get("/api/especies"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/tutores sem token deve retornar 403")
    void tutoresSemToken() throws Exception {
        mockMvc.perform(get("/api/tutores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/login com payload vazio deve retornar 400 com mapa de erros")
    void loginPayloadVazio() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.senha").exists());
    }

    @Test
    @DisplayName("GET /api/tutores com token invalido deve retornar 403")
    void tutoresTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/tutores")
                .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWTUtil deve gerar e validar token corretamente")
    void jwtGerarEValidar() {
        String token = jwtUtil.gerarToken("teste@clyvo.vet");
        assertThat(jwtUtil.validarToken(token)).isTrue();
        assertThat(jwtUtil.extrairUsername(token)).isEqualTo("teste@clyvo.vet");
    }

    @Test
    @DisplayName("JWTUtil deve rejeitar token malformado")
    void jwtTokenMalformado() {
        assertThat(jwtUtil.validarToken("isso.nao.e.um.jwt")).isFalse();
    }
}
