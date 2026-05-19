package br.com.clyvo.kura.tutor.shared.config;

import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationEntryPoint;
import br.com.clyvo.kura.tutor.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança HTTP — stateless, JWT, CSRF off, CORS via CorsConfig.
 *
 * Mudanças relevantes:
 *   - /auth/** substituído por rotas explícitas para que /auth/logout seja protegido
 *   - .cors(Customizer.withDefaults()) delega ao CorsConfigurationSource bean (CorsConfig)
 *   - exceptionHandling com JwtAuthenticationEntryPoint → 401 em vez de 403 default
 *   - PasswordEncoder em PasswordEncoderConfig (evita dependência circular)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
        "/auth/login",
        "/auth/refresh",
        "/auth/register-invite",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/health",
        "/h2-console/**",
        "/especies/**",
        "/racas/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filtrar(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            // Necessário para o H2 Console (renderiza em iframe)
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(req -> req
                .requestMatchers(ROTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
