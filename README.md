# KURA · Backend Tutor

> **Bounded context:** Identidade, Consentimento e Agendamento do Tutor  
> **Disciplina:** Java Advanced · FIAP Challenge 2026  
> **Autor:** Nikolas Brisola  
> **Stack:** Java 21 · Spring Boot 3.2 · Spring Data JPA · Oracle 19c · JWT

---

## Arquitetura do serviço

```
kura-backend-tutor
└── src/main/java/br/com/clyvo/kura/tutor/
    ├── config/          # SecurityConfig, OpenApiConfig, RestClientConfig
    ├── controller/      # AuthController, TutorController, PetController,
    │                    # ConsentimentoController, AgendamentoController
    ├── dto/
    │   ├── request/     # DTOs de entrada (records imutáveis)
    │   └── response/    # DTOs de saída (records imutáveis)
    ├── entity/          # Entidades JPA mapeando o Oracle
    ├── exception/       # ApiExceptionHandler + exceções de domínio
    ├── repository/      # Spring Data JPA repositories
    ├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsService
    └── service/
        └── impl/        # Implementações dos services
```

### Separação de domínios

| Domínio | Backend | Tabelas |
|---------|---------|---------|
| **Identidade / Auth** | Java (este serviço) | `CONTA_TUTOR` |
| **Consentimento LGPD** | Java (este serviço) | `CONSENTIMENTO` |
| **Agendamentos** | Java (este serviço) | `AGENDAMENTO` |
| Clínico (eventos, vacinas...) | .NET (Felipe) | `EVENTO_CLINICO`, `VACINA`... |
| Compartilhado (leitura) | Ambos | `TUTOR`, `PET`, `ESPECIE`, `RACA` |

---

## Como rodar

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Oracle 19c/23c no Docker **OU** usar profile H2 para dev rápido

### Opção A — com H2 (sem Oracle, para desenvolver agora)

```bash
mvn spring-boot:run -Dspring.profiles.active=h2
```

Acesse:
- API: http://localhost:8081/api
- Swagger: http://localhost:8081/api/swagger-ui.html
- H2 Console: http://localhost:8081/api/h2-console

### Opção B — com Oracle (Docker do Clayton/Gustavo)

```bash
# Variáveis de ambiente (ou exporte no terminal)
export ORACLE_USER=kura_user
export ORACLE_PASS=kura_pass

mvn spring-boot:run
```

---

## Endpoints principais

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/auth/login` | Login → retorna JWT | Pública |
| POST | `/auth/registro` | Cria conta do tutor | Pública |
| GET | `/tutores?page=0&size=10&sort=nmTutor` | Lista tutores paginada | JWT |
| GET | `/tutores?nome=Felipe&cidade=SP&especie=cao` | Busca com filtros | JWT |
| GET | `/tutores/{id}` | Detalhe do tutor | JWT |
| GET | `/tutores/{id}/pets` | Pets do tutor (cached) | JWT |
| GET | `/tutores/{id}/timeline` | Timeline consolidada (Java + .NET) | JWT |
| GET | `/tutores/{id}/consentimentos` | Histórico LGPD | JWT |
| POST | `/tutores/{id}/consentimentos` | Registra aceite | JWT |
| DELETE | `/tutores/{id}/consentimentos/{tipo}` | Revoga consentimento | JWT |
| GET | `/agendamentos?tutorId=1&status=AGENDADO` | Lista agendamentos | JWT |
| POST | `/agendamentos` | Cria agendamento | JWT |
| PATCH | `/agendamentos/{id}/cancelar` | Cancela agendamento | JWT |
| GET | `/especies` | Lista espécies (cached) | Pública |
| GET | `/racas?especieId=1` | Lista raças por espécie (cached) | Pública |

---

## Design Patterns aplicados

| Pattern | Onde |
|---------|------|
| **Repository** | Todos os `*Repository` (Spring Data JPA) |
| **Strategy** | `ConsentimentoStrategy` — regras por tipo de consentimento |
| **DTO (Data Transfer Object)** | Records separados de request/response — entidade JPA nunca exposta |
| **Chain of Responsibility** | `@ControllerAdvice` — pipeline de tratamento de exceções |

---

## Tecnologias e bibliotecas

| Lib | Versão | Uso |
|-----|--------|-----|
| Spring Boot | 3.2.5 | Framework base |
| Spring Data JPA | (BOM) | Repositórios + Hibernate |
| Spring Security | (BOM) | Filtro HTTP + auth |
| jjwt | 0.12.5 | Geração/validação JWT |
| springdoc-openapi | 2.5.0 | Swagger UI |
| Lombok | (BOM) | Boilerplate |
| ojdbc11 | 23.4.0 | Driver Oracle |
| H2 | (BOM) | Banco em memória (dev) |

---

## Estrutura de commits

```
feat(auth): implementa POST /auth/login com JWT (#12)
feat(tutor): adiciona paginação e filtros em GET /tutores (#15)
fix(consentimento): corrige revogação quando já revogado (#18)
```
