package br.com.clyvo.kura.tutor.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração OpenAPI/Swagger — KURA Backend Tutor.
 *
 * O esquema "bearerAuth" é registrado globalmente; controllers públicos
 * sobrescrevem com @SecurityRequirement vazia ou omitem a anotação.
 *
 * UI: /api/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI kuraOpenApi() {
        return new OpenAPI()
                .info(buildInfo())
                .externalDocs(buildExternalDocs())
                .servers(buildServers())
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(buildTags());
    }

    private Info buildInfo() {
        return new Info()
                .title("KURA — Backend Tutor API")
                .description("""
                        API REST do tutor Kura Vet — plataforma de saúde pet (FIAP Challenge 2026).

                        **Autenticação:** Bearer JWT. Obtenha o token em `POST /auth/login`
                        e clique em **Authorize** para preencher.

                        **Contextos:**
                        - `Autenticação` — login, refresh, logout, cadastro por convite
                        - `Agendamentos` — CRUD com optimistic locking (NR_VERSION)
                        - `Consentimentos LGPD` — aceites e revogações com idempotência
                        - `LGPD — Direitos do Titular` — relatório e histórico de auditoria
                        - `Tutores` — consulta de dados e pets do tutor
                        - `Timeline` — histórico clínico e vacinas vencendo
                        - `Catalogo` — espécies e raças (público, cacheado)

                        **Códigos de erro padrão:** `ApiError { timestamp, status, codigo, mensagem, path, detalhes[] }`
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("KURA — FIAP Challenge 2026")
                        .email("kura@clyvo.vet"))
                .license(new License()
                        .name("FIAP Academic License")
                        .url("https://www.fiap.com.br"));
    }

    private ExternalDocumentation buildExternalDocs() {
        return new ExternalDocumentation()
                .description("Repositório — Backend Tutor Java")
                .url("https://github.com/NikolasBrisola/backend-tutor-java");
    }

    private List<Server> buildServers() {
        return List.of(
                new Server().url("http://localhost:8081/api").description("Dev (H2)"),
                new Server().url("https://kura.clyvo.vet/api").description("Prod (Oracle)")
        );
    }

    private Components buildComponents() {
        return new Components().addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Cole o access token obtido em `POST /auth/login`"));
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag().name("Autenticação")
                         .description("Login, refresh, logout e cadastro por convite"),
                new Tag().name("Agendamentos")
                         .description("Criação, consulta, atualização e cancelamento de agendamentos"),
                new Tag().name("Consentimentos LGPD")
                         .description("Registro de aceites e revogações com idempotência obrigatória"),
                new Tag().name("LGPD — Direitos do Titular")
                         .description("Acesso, portabilidade e revogação (LGPD art. 18)"),
                new Tag().name("Tutores")
                         .description("Consulta de dados do tutor e seus pets"),
                new Tag().name("Timeline")
                         .description("Histórico clínico e vacinas vencendo — somente dados próprios"),
                new Tag().name("Catalogo")
                         .description("Espécies e raças — dados de referência públicos, cacheados 6h")
        );
    }
}
