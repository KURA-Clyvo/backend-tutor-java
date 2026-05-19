package br.com.clyvo.kura.tutor.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separado de SecurityConfig para evitar dependência circular:
 * SecurityConfig → JwtAuthenticationFilter → UserDetailsService → ContaTutorRepository
 * AuthService → PasswordEncoder
 *
 * Com PasswordEncoder em classe própria, AuthService não depende de SecurityConfig.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
