package br.com.clyvo.kura.tutor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o Swagger UI com suporte a autenticação JWT.
 * <p>
 * Acesse em: http://localhost:8081/api/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KURA — Backend Tutor")
                        .description("""
                                API de identidade, consentimento e agendamento do tutor.
                                
                                **Bounded context:** Tutor (Java · Nikolas Brisola)
                                
                                **Autenticação:** JWT Bearer Token
                                1. `POST /api/auth/login` com e-mail e senha
                                2. Copie o `accessToken` da resposta
                                3. Clique em **Authorize** e cole `Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Nikolas Brisola")
                                .email("nikolas@clyvo.vet")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
