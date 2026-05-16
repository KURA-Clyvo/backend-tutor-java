package br.com.clyvo.kura.tutor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configura o {@link RestTemplate} usado para consumir a API clínica (.NET do Felipe).
 *
 * <p>URL base configurada em {@code kura.clinica-api.base-url} (application.yml).
 * O bean é injetado nos services que precisam de dados do domínio clínico,
 * como o endpoint de timeline consolidada.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
