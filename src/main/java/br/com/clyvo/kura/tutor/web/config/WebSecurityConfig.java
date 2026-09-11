// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.config;

import br.com.clyvo.kura.tutor.auth.application.AuthService;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.web.domain.WebUsuarioSuporteRepository;
import br.com.clyvo.kura.tutor.web.security.WebAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

import java.util.Map;

/**
 * Segundo {@link SecurityFilterChain}, exclusivo de {@code /web/**} — o
 * painel administrativo interno de Spring Security (SJ3-03, escopo isolado
 * desta branch). Ver {@code docs/ADR-web.md} para o porquê de dois chains
 * em vez de editar {@code SecurityConfig} de produto.
 *
 * Ao contrário do chain de produto ({@code SecurityConfig}: stateless, sem
 * sessão, CSRF desligado — é API JSON pura), este chain é o oposto nos três
 * pontos, porque é formulário HTML servido pelo próprio backend:
 *   - CSRF LIGADO ({@link Customizer#withDefaults()} — não chama
 *     {@code .disable()});
 *   - sessão HTTP normal ({@code IF_REQUIRED}, não {@code STATELESS});
 *   - {@code formLogin} em vez de JWT.
 *
 * {@code @Order(1)}: precisa ser avaliado ANTES do chain de produto, que não
 * declara {@code @Order} nenhum e por isso já fica em
 * {@code Ordered.LOWEST_PRECEDENCE} — condição exigida pelo Spring Security
 * para o chain "any request" ser sempre o ÚLTIMO. Medido nesta task
 * (§3.2 do brief): {@code grep -c "@Order" SecurityConfig.java} → 0, então
 * {@code @Order(2)} no arquivo de produto NÃO foi necessário — ver o
 * artefato desta task para a evidência de boot completa.
 */
@Configuration
public class WebSecurityConfig {

    private final ContaTutorRepository contaTutorRepository;
    private final WebUsuarioSuporteRepository suporteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public WebSecurityConfig(ContaTutorRepository contaTutorRepository,
                             WebUsuarioSuporteRepository suporteRepository,
                             PasswordEncoder passwordEncoder,
                             AuthService authService) {
        this.contaTutorRepository = contaTutorRepository;
        this.suporteRepository = suporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /**
     * Destino de falha por TIPO de exceção — o formulário precisa distinguir
     * "senha errada" (tente de novo) de "conta bloqueada" (procure o
     * suporte). Sem isto, o {@code failureUrl} único mandava as duas para
     * {@code ?erro} e a trava era invisível na tela, mesmo existindo no
     * banco: a pessoa via "login ou senha inválidos" e continuava tentando.
     */
    private static AuthenticationFailureHandler falhaDeLogin() {
        ExceptionMappingAuthenticationFailureHandler handler =
                new ExceptionMappingAuthenticationFailureHandler();
        handler.setExceptionMappings(Map.of(
                LockedException.class.getName(), "/web/login?bloqueada",
                DisabledException.class.getName(), "/web/login?desativada"));
        handler.setDefaultFailureUrl("/web/login?erro");
        return handler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        // WebAuthenticationProvider é instanciado aqui, NÃO como @Component/@Bean
        // à parte — ver o javadoc da classe para o porquê (evitar auto-detecção
        // global do AuthenticationManager de produto).
        WebAuthenticationProvider webAuthenticationProvider = new WebAuthenticationProvider(
                contaTutorRepository, suporteRepository, passwordEncoder, authService);

        // Fix do achado G2-1 (sj3-03-revisao.md, M4): SEM isto, o
        // AuthenticationManagerBuilder compartilhado que o Spring Boot
        // registra por padrão herda o AuthenticationManager GLOBAL como
        // parent. Quando webAuthenticationProvider lança
        // BadCredentialsException (que não é AccountStatusException), o
        // ProviderManager cai no parent — que contém o DaoAuthenticationProvider
        // autoconfigurado sobre UserDetailsServiceImpl (produto), e esse
        // resolve idConta usando ContaTutor.isBloqueada() SEM JANELA,
        // ressuscitando a trava permanente que a SJ3-02 fechou. Isolando o
        // parent como null, o AuthenticationManager deste chain SÓ conhece
        // webAuthenticationProvider — não há fall-through possível.
        // Regressão travada em WebAuthenticationManagerParentIsolationTest.
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .parentAuthenticationManager(null)
                .authenticationProvider(webAuthenticationProvider);
        AuthenticationManager webAuthenticationManager = authenticationManagerBuilder.build();

        http
            .securityMatcher("/web/**")
            .authenticationManager(webAuthenticationManager)
            .csrf(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(req -> req
                .requestMatchers("/web/suporte/**").hasRole("SUPORTE")
                .anyRequest().authenticated())
            // Achado medido durante a prova HTTP real da fix wave da SJ3-03
            // (não é G2-1 nem G2-2, achado à parte): o AccessDeniedHandler
            // default chama response.sendError(403), que o Tomcat resolve
            // com um FORWARD DE CONTAINER para "/error" — path que NÃO casa
            // com "/web/**" e por isso é servido pelo chain[1] (produto,
            // stateless), cujo JwtAuthenticationFilter não enxerga a sessão
            // HTTP autenticada deste chain e devolve 401 TOKEN_AUSENTE,
            // mascarando o 403 real. MockMvc não pega isso: não faz o
            // dispatch de erro do container.
            //
            // SJ3-05: o texto puro virou página renderizada (/web/403).
            // Handler explícito NÃO usa sendError() — em vez disso seta o
            // status e faz um FORWARD DE APLICAÇÃO (request.getRequestDispatcher,
            // chamado diretamente pelo nosso código, não pelo container) para
            // "/web/403", que casa com "/web/**" e permanece no chain[0].
            // Medido por HTTP real (não presumido): o dispatch FORWARD não é
            // filtrado de novo pelo Spring Security (registrado só para
            // REQUEST/ERROR/ASYNC), então não há recursão nem reavaliação de
            // autorização — e o SecurityContext (ThreadLocal) sobrevive ao
            // forward porque é a mesma thread/request, então sec:authorize e
            // sec:authentication em web/403.html enxergam o usuário
            // corretamente. Ver artefato da task (sj3-05-report.md) para a
            // prova HTTP literal.
            .exceptionHandling(ex -> ex.accessDeniedHandler(
                (request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    request.getRequestDispatcher("/web/403").forward(request, response);
                }))
            .formLogin(form -> form
                .loginPage("/web/login")
                .loginProcessingUrl("/web/login")
                // alwaysUse=true (fix wave da SJ3-05, achado G2-B): com
                // alwaysUse=false (o default), o RequestCache GANHA de
                // defaultSuccessUrl sempre que havia requisição salva (ex.:
                // TUTOR deslogado tenta /web/suporte/contas, é mandado pro
                // login, loga com sucesso) — o login volta pra rota salva em
                // vez de ir pro destino comum, e um TUTOR cai direto num 403
                // LOGO APÓS autenticação válida ("funcionalidade com erro").
                // Medido por HTTP real, 4 passos, com TUTOR: GET rota
                // protegida (deslogado) -> 302 /web/login -> login OK -> 302
                // /web/suporte/contas?continue -> 403. Trocar só o destino
                // para "/web/painel" (sem alwaysUse=true) NÃO fecha isso: o
                // RequestCache continua vencendo. Com alwaysUse=true, os 4
                // passos terminam em 200 no painel para os DOIS perfis — a
                // contrapartida medida é o SUPORTE perder o deep link
                // direto e cair no painel, que já tem o link "Gerenciar
                // contas" para a mesma rota (1 clique a mais, não um bug).
                // Ver sj3-05-revisao.md (frente C) e sj3-05-fixwave.md no
                // repo de planejamento.
                .defaultSuccessUrl("/web/painel", true)
                // failureHandler (não failureUrl): distingue conta bloqueada
                // de senha errada na tela — ver falhaDeLogin().
                .failureHandler(falhaDeLogin())
                .permitAll())
            // Sem isto NÃO HÁ COMO SAIR do painel: a URL de logout padrão do
            // Spring Security é "/logout", que não casa com
            // securityMatcher("/web/**") e por isso é servida pelo chain de
            // produto (stateless), que não enxerga nem invalida esta sessão.
            // Trocar de perfil exigia apagar cookie na mão — inviável numa
            // demonstração, e um painel sem "Sair" é funcionalidade faltando.
            // POST, não GET: o chain tem CSRF ligado (o botão "Sair" do
            // cabeçalho é um <form method="post">).
            .logout(logout -> logout
                .logoutUrl("/web/logout")
                .logoutSuccessUrl("/web/login?sair")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

        return http.build();
    }
}
