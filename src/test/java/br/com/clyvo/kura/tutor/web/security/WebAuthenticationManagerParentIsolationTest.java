// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.security;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regressão do achado {@code G2-1} da revisão G2 (sj3-03-revisao.md, M4): o
 * {@link AuthenticationManager} do chain {@code /web/**} tinha PARENT — o
 * {@code AuthenticationManager} global de produto, com
 * {@code DaoAuthenticationProvider} sobre {@code UserDetailsServiceImpl} — e
 * por isso, quando {@link WebAuthenticationProvider} lançava
 * {@code BadCredentialsException} (não {@code AccountStatusException}), o
 * {@code ProviderManager} caía no parent, que resolve login por
 * {@code idConta} e usa {@code ContaTutor.isBloqueada()} SEM JANELA — a trava
 * PERMANENTE que a {@code SJ3-02} fechou.
 *
 * <p>Este teste falha CONTRA o código de antes do fix (idConta autentica pelo
 * parent, recebendo {@code LockedException} de uma conta com bloqueio já
 * EXPIRADO) e passa depois do fix
 * ({@code .parentAuthenticationManager(null)} em
 * {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig}), que isola o
 * {@code AuthenticationManager} do chain do painel do global de produto.</p>
 *
 * <p>Controle positivo OBRIGATÓRIO (mesma classe de teste): a autenticação
 * LEGÍTIMA continua funcionando pelo {@link WebAuthenticationProvider} — TUTOR
 * por e-mail com bloqueio expirado autentica, e SUPORTE autentica — provando
 * que o fix não quebrou o caminho normal.</p>
 */
@SpringBootTest
class WebAuthenticationManagerParentIsolationTest {

    @Autowired private FilterChainProxy filterChainProxy;
    @Autowired private ContaTutorRepository contaTutorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @PersistenceContext private EntityManager em;

    private static final String SENHA = "Senha@123";

    private static SecurityFilterChain webChain(FilterChainProxy proxy) {
        for (SecurityFilterChain c : proxy.getFilterChains()) {
            if (c.toString().contains("/web/**")) {
                return c;
            }
        }
        throw new IllegalStateException("chain /web/** não encontrado — mapa de chains mudou");
    }

    private static AuthenticationManager authenticationManagerDoChainWeb(SecurityFilterChain web) throws Exception {
        UsernamePasswordAuthenticationFilter upaf = null;
        for (Filter f : web.getFilters()) {
            if (f instanceof UsernamePasswordAuthenticationFilter u) {
                upaf = u;
            }
        }
        if (upaf == null) {
            throw new IllegalStateException("UsernamePasswordAuthenticationFilter ausente do chain /web/**");
        }
        Field field = AbstractAuthenticationProcessingFilter.class.getDeclaredField("authenticationManager");
        field.setAccessible(true);
        return (AuthenticationManager) field.get(upaf);
    }

    @Test
    @Transactional
    @DisplayName("G2-1 — idConta NÃO autentica mais via fall-through de parent (UserDetailsServiceImpl)")
    void idContaNaoAutenticaMaisPeloParent() throws Exception {
        Tutor tutor = em.createQuery("select t from Tutor t", Tutor.class).setMaxResults(1).getSingleResult();
        ContaTutor conta = new ContaTutor();
        conta.setTutor(tutor);
        conta.setDsEmailLogin("g2-1-regressao@kura.demo");
        conta.setDsSenhaHash(passwordEncoder.encode(SENHA));
        conta.setStAtiva("S");
        // bloqueio EXPIRADO há 20 min, janela de 15 — a checagem COM janela
        // (WebAuthenticationProvider) não bloquearia; só a SEM janela
        // (UserDetailsServiceImpl, via parent) bloquearia.
        conta.setDtBloqueio(LocalDateTime.now().minusMinutes(20));
        conta.setNrTentativasLogin(0);
        conta = contaTutorRepository.saveAndFlush(conta);

        AuthenticationManager authenticationManager = authenticationManagerDoChainWeb(webChain(filterChainProxy));

        // ANTES do fix: o parent (DaoAuthenticationProvider -> UserDetailsServiceImpl)
        // resolve "idConta" como username e usa isBloqueada() SEM JANELA -> LockedException,
        // mesmo com o bloqueio expirado. Esse é o achado G2-1.
        // DEPOIS do fix: sem parent, WebAuthenticationProvider não sabe resolver um
        // idConta puro (só resolve por e-mail/login) -> BadCredentialsException.
        String idContaComoUsername = String.valueOf(conta.getIdConta());
        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(idContaComoUsername, SENHA)))
                .as("idConta não pode mais alcançar UserDetailsServiceImpl (trava sem janela) pelo chain do painel")
                .isNotInstanceOf(LockedException.class)
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @Transactional
    @DisplayName("Controle positivo — TUTOR com bloqueio EXPIRADO continua autenticando pelo WebAuthenticationProvider")
    void tutorComBloqueioExpiradoContinuaAutenticando() throws Exception {
        Tutor tutor = em.createQuery("select t from Tutor t", Tutor.class).setMaxResults(1).getSingleResult();
        ContaTutor conta = new ContaTutor();
        conta.setTutor(tutor);
        conta.setDsEmailLogin("g2-1-controle-positivo@kura.demo");
        conta.setDsSenhaHash(passwordEncoder.encode(SENHA));
        conta.setStAtiva("S");
        conta.setDtBloqueio(LocalDateTime.now().minusMinutes(20)); // expirado, janela 15
        conta.setNrTentativasLogin(0);
        contaTutorRepository.saveAndFlush(conta);

        AuthenticationManager authenticationManager = authenticationManagerDoChainWeb(webChain(filterChainProxy));

        Authentication resultado = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("g2-1-controle-positivo@kura.demo", SENHA));

        assertThat(resultado.isAuthenticated()).isTrue();
        assertThat(resultado.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_TUTOR");
    }

    @Test
    @DisplayName("Controle positivo — SUPORTE continua autenticando pelo WebAuthenticationProvider")
    void suporteContinuaAutenticando() throws Exception {
        AuthenticationManager authenticationManager = authenticationManagerDoChainWeb(webChain(filterChainProxy));

        // Login/senha do seed de dev (db/callback/afterMigrate__seeds_dev.sql,
        // seção 11 — hash conferido em sj3-03-revisao.md, M3 caso A2.g).
        Authentication resultado = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("suporte@kura.demo", "Suporte@2026"));

        assertThat(resultado.isAuthenticated()).isTrue();
        assertThat(resultado.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SUPORTE");
    }
}
