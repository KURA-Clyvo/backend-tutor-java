package br.com.clyvo.kura.tutor.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracao de seguranca HTTP.
 *
 * ESTRUTURA IDENTICA AO SegurancaConfig DO PROJETO DE AULA,
 * com as seguintes adaptacoes para o KURA:
 *
 * Aula liberava: /autenticacao/** /swagger-ui/** /v3/**
 * KURA libera:   /auth/**       /swagger-ui/** /v3/** /h2-console/** /especies/** /racas/**
 *
 * O AuthManager e o PasswordEncoder foram movidos para esta classe
 * (no projeto de aula ficavam em AuthManager.java e UsuarioConfig.java separados).
 * Aqui estao todos juntos para simplificar.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private final JWTAuthFilter authFilter;

    public SegurancaConfig(JWTAuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    private static final String[] ROTAS_PUBLICAS = {
        "/auth/**",
        "/swagger-ui/**",
        "/v3/**",
        "/h2-console/**",
        "/actuator/**"
    };

    @Bean
    public SecurityFilterChain filtrar(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            // Libera frames do H2 Console — igual ao projeto de aula
            .headers(header -> header.frameOptions(
                    HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(request -> request
                .requestMatchers(ROTAS_PUBLICAS).permitAll()
                .requestMatchers(HttpMethod.GET, "/especies/**", "/racas/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    // AuthenticationManager — equivalente ao AuthManager.java do projeto de aula
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // PasswordEncoder — equivalente ao UsuarioConfig.java do projeto de aula
    // BCrypt strength 12 para producao (aula usava default = 10)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
