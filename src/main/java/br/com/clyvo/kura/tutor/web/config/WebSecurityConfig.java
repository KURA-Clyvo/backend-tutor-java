// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.config;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.web.domain.WebUsuarioSuporteRepository;
import br.com.clyvo.kura.tutor.web.security.WebAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segundo {@link SecurityFilterChain}, exclusivo de {@code /web/**} — o
 * painel Thymeleaf descartável da rubrica de Spring Security (SJ3-03,
 * branch {@code web-rubrica}). Ver {@code docs/ADR-web.md} para o porquê de
 * dois chains em vez de editar {@code SecurityConfig} de produto.
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
    private final int janelaBloqueioMinutos;

    public WebSecurityConfig(ContaTutorRepository contaTutorRepository,
                             WebUsuarioSuporteRepository suporteRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${kura.auth.janela-bloqueio-minutos:15}") int janelaBloqueioMinutos) {
        this.contaTutorRepository = contaTutorRepository;
        this.suporteRepository = suporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.janelaBloqueioMinutos = janelaBloqueioMinutos;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        // WebAuthenticationProvider é instanciado aqui, NÃO como @Component/@Bean
        // à parte — ver o javadoc da classe para o porquê (evitar auto-detecção
        // global do AuthenticationManager de produto).
        WebAuthenticationProvider webAuthenticationProvider = new WebAuthenticationProvider(
                contaTutorRepository, suporteRepository, passwordEncoder, janelaBloqueioMinutos);

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
            .formLogin(form -> form
                .loginPage("/web/login")
                .loginProcessingUrl("/web/login")
                // alwaysUse=false (default): se havia requisição salva pelo
                // RequestCache (ex.: tentou /web/suporte/contas sem sessão),
                // volta pra ela; senão cai aqui — a única rota protegida que
                // esta task entrega. Escopo: tela de painel de verdade é a
                // SJ3-05, não esta task.
                .defaultSuccessUrl("/web/suporte/contas")
                .failureUrl("/web/login?erro")
                .permitAll());

        return http.build();
    }
}
