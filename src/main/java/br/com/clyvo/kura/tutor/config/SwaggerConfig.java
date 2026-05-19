package br.com.clyvo.kura.tutor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Swagger — estrutura identica ao SwaggerConfig do projeto de aula.
 *
 * Adiciona o esquema bearerAuth igual ao projeto de aula para que
 * o botao "Authorize" apareca no Swagger UI.
 *
 * Acesse em: http://localhost:8081/api/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    final String AUTORIZACAO = "bearerAuth";

    @Bean
    OpenAPI configurarSwagger() {
        return new OpenAPI()

                .addSecurityItem(new SecurityRequirement().addList(AUTORIZACAO))

                .components(new Components().addSecuritySchemes(AUTORIZACAO,
                        new SecurityScheme()
                                .name(AUTORIZACAO)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))

                .info(new Info()
                        .title("KURA — Backend Tutor")
                        .description("API de identidade, consentimentos LGPD e agendamentos do tutor. "
                                + "Clyvo Vet · FIAP Challenge 2026")
                        .summary("Nikolas Brisola · Java Advanced")
                        .version("1.0.0")
                        .license(new License()
                                .name("FIAP Academic License")
                                .url("/licenses")));
    }
}
