// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.security;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão do achado de gravação de 2026-09-11: <b>errar a senha no
 * formulário {@code /web/login} não travava a conta</b>. O
 * {@link WebAuthenticationProvider} era SÓ LEITURA — lia
 * {@code isBloqueada()} e nunca chamava {@code registrarLoginFalha()} — então
 * só as rotas REST (que passam por {@code AuthService}) contavam tentativa.
 * Duas portas de autenticação, uma regra de negócio, e só uma das portas
 * passava por ela.
 *
 * <p>Este teste FALHA contra o código anterior ao fix: sem a delegação, a 6ª
 * tentativa (com a senha CERTA) autenticava e redirecionava para
 * {@code /web/painel} em vez de {@code /web/login?bloqueada}.</p>
 *
 * <p>Controles positivos obrigatórios, na mesma classe: o login legítimo
 * continua entrando (a delegação não quebrou o caminho normal) e o logout
 * responde — ele não existia antes ({@code /logout} fica fora de
 * {@code securityMatcher("/web/**")}).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebLoginFormularioTravaContaTest {

    private static final String EMAIL = "web-trava-formulario@kura.demo";
    private static final String SENHA_CERTA = "Senha@123";
    private static final String SENHA_ERRADA = "senha-errada-de-proposito";

    @Autowired private MockMvc mockMvc;
    @Autowired private ContaTutorRepository contaTutorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @PersistenceContext private EntityManager em;

    @BeforeEach
    void criarContaLimpa() {
        Tutor tutor = em.createQuery("select t from Tutor t", Tutor.class).setMaxResults(1).getSingleResult();
        ContaTutor conta = new ContaTutor();
        conta.setTutor(tutor);
        conta.setDsEmailLogin(EMAIL);
        conta.setDsSenhaHash(passwordEncoder.encode(SENHA_CERTA));
        conta.setStAtiva("S");
        conta.setNrTentativasLogin(0);
        conta.setDtBloqueio(null);
        contaTutorRepository.saveAndFlush(conta);
        em.clear();
    }

    private void tentarLogin(String senha, String destinoEsperado) throws Exception {
        mockMvc.perform(post("/web/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", senha))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString(destinoEsperado)));
        // flush ANTES do clear: registrarLoginFalha() incrementa um objeto
        // GERENCIADO e save() não emite SQL na hora — um clear() sem flush
        // descartaria o incremento e o teste mediria 0 tentativas com o
        // código já corrigido (falso negativo do próprio detector).
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("5 erros no FORMULÁRIO travam a conta — a 6ª tentativa, com a senha CERTA, é barrada")
    void cincoErrosNoFormularioTravamAConta() throws Exception {
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            tentarLogin(SENHA_ERRADA, "/web/login?erro");
        }

        ContaTutor apos5 = contaTutorRepository.findByDsEmailLogin(EMAIL).orElseThrow();
        assertThat(apos5.getNrTentativasLogin())
                .as("o formulário precisa contar a tentativa, como a API conta")
                .isEqualTo(5);
        assertThat(apos5.getDtBloqueio())
                .as("a 5ª falha carimba o bloqueio")
                .isNotNull();
        em.clear();

        // A senha está CERTA aqui de propósito: o bloqueio é verificado ANTES
        // dela (AuthService, passo 3), então quem está travado não entra nem
        // acertando — é isso que a tela de suporte existe para resolver.
        tentarLogin(SENHA_CERTA, "/web/login?bloqueada");
    }

    @Test
    @DisplayName("Controle positivo — senha certa em conta limpa entra no painel")
    void senhaCertaEntra() throws Exception {
        tentarLogin(SENHA_CERTA, "/web/painel");
    }

    @Test
    @DisplayName("Controle positivo — o painel tem saída: POST /web/logout responde")
    void logoutResponde() throws Exception {
        mockMvc.perform(post("/web/logout")
                        .with(user(EMAIL).roles("TUTOR"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/web/login?sair")));
    }
}
