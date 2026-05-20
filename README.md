# KURA — Backend Tutor (Java/Spring)

API REST do **Portal do Tutor** do sistema KURA — solução de continuidade do cuidado veterinário desenvolvida no FIAP Challenge 2026 (parceiro Clyvo Vet).

---

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 3.2.5 | Framework base (LTS) |
| Spring Data JPA · Hibernate 6 | BOM | Persistência |
| Spring Security | BOM | Autenticação stateless |
| jjwt | 0.12.6 | Geração/validação JWT |
| Springdoc OpenAPI | 2.5.0 | Swagger UI |
| Caffeine cache | BOM | Cache em memória (espécies/raças) |
| Flyway | BOM | Migrações de schema (V1–V6) |
| Oracle 19c / 23c | — | Banco de dados (prod) |
| H2 | BOM | Banco em memória (dev) |
| JUnit 5 + Mockito + AssertJ | BOM | Testes |
| Lombok | BOM | Redução de boilerplate |
| Docker · Docker Compose | — | Containerização |

---

## Arquitetura

Bounded contexts isolados com estrutura **contexto primeiro, camada depois**. Detalhes completos em [`docs/architecture.md`](docs/architecture.md).

```
br.com.clyvo.kura.tutor/
├── auth/           Login · Refresh · Logout · JWT
├── onboarding/     Register-invite (consume token do .NET)
├── tutor/          Tutor · Pet · Espécie · Raça (read-only)
├── timeline/       VW_TIMELINE_PET · VW_VACINAS_VENCENDO (read-only)
├── consentimento/  LGPD · Idempotency
├── agendamento/    Scheduling (shared-write com .NET via @Version)
└── shared/         SecurityConfig · GlobalExceptionHandler · CorrelationIdFilter · CacheConfig
```

**Boundary Java ↔ .NET:**

| Tabela | Owner | Java faz |
|--------|-------|----------|
| `CONTA_TUTOR` | Java | escrita exclusiva |
| `CONSENTIMENTO` | Java | escrita exclusiva |
| `IDEMPOTENCY_KEY` | Java | escrita exclusiva |
| `AGENDAMENTO` | Shared | POST/PUT/DELETE (tutor); .NET faz PATCH ST_STATUS (vet) |
| `TUTOR`, `PET`, `INVITE_TUTOR`, `CLINICA`, `VETERINARIO` | .NET | somente leitura (`@Immutable`) |

Sincronização em `AGENDAMENTO` via `@Version` (`NR_VERSION`) — conflito retorna 409.

---

## Como rodar

### Via Docker

```bash
git clone https://github.com/FelipeFerrete/kura-backend-tutor.git
cd kura-backend-tutor
cp .env.example .env          # ajuste JWT_SECRET (mín 64 bytes) e DB_PASSWORD
docker compose up -d
```

- API: `http://localhost:8081/api`
- Swagger UI: `http://localhost:8081/api/swagger-ui/index.html`

### Sem Docker — profile dev (H2 em memória)

```bash
# Requer Java 21 e Maven 3.9
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: `http://localhost:8081/api`
- Swagger UI: `http://localhost:8081/api/swagger-ui/index.html`
- H2 Console: `http://localhost:8081/api/h2-console`

> **Token de convite para testes locais:** `550e8400-e29b-41d4-a716-446655440000`
> (inserido pelos seeds Flyway no perfil dev)

---

## Endpoints

Documentação interativa completa: **[Swagger UI](http://localhost:8081/api/swagger-ui/index.html)**

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| `POST` | `/auth/register-invite` | Pública | Onboarding por convite — cria conta + consentimentos + JWT |
| `POST` | `/auth/login` | Pública | Autenticação email/senha → access + refresh token |
| `POST` | `/auth/refresh` | Pública | Rotação de refresh token (invalida o anterior) |
| `POST` | `/auth/logout` | JWT | Invalida refresh hash |
| `GET` | `/tutores/{id}` | JWT | Perfil do tutor |
| `GET` | `/tutores` | JWT | Lista tutores com filtros (nome, cidade, uf) |
| `GET` | `/tutores/{id}/pets` | JWT | Pets ativos (paginado, ownership check) |
| `GET` | `/pets/{idPet}/timeline` | JWT | Linha do tempo de atendimentos do pet |
| `GET` | `/tutores/{idTutor}/vacinas-vencendo` | JWT | Vacinas nos próximos 30 dias |
| `GET` | `/tutores/{idTutor}/consentimentos` | JWT | Estado atual (último por tipo) |
| `POST` | `/tutores/{idTutor}/consentimentos` | JWT | Registrar aceite/revogação (header `Idempotency-Key` obrigatório) |
| `GET` | `/tutores/{idTutor}/lgpd/relatorio` | JWT | Relatório de dados pessoais (art. 18, I) |
| `GET` | `/tutores/{idTutor}/lgpd/consentimentos` | JWT | Histórico completo de consentimentos |
| `GET` | `/agendamentos` | JWT | Lista (filtros: status, dataInicio, dataFim, tipo) |
| `POST` | `/agendamentos` | JWT | Cria agendamento |
| `PUT` | `/agendamentos/{id}` | JWT | Atualiza (requer `nrVersion` para optimistic lock) |
| `DELETE` | `/agendamentos/{id}` | JWT | Cancela (soft delete) |
| `PATCH` | `/agendamentos/{id}/cancelar` | JWT | Cancela com motivo explícito |
| `GET` | `/especies` | Pública | Espécies (Caffeine cache 6h) |
| `GET` | `/racas` | Pública | Raças (Caffeine cache 6h, `?especieId=`) |

**Postman:** [`docs/postman/kura-tutor.postman_collection.json`](docs/postman/kura-tutor.postman_collection.json)

---

## Variáveis de Ambiente

| Variável | Default dev | Prod obrigatória |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | ✅ (`prod`) |
| `DB_URL` | H2 in-memory | ✅ (`jdbc:oracle:thin:@//host:1521/orcl`) |
| `DB_USERNAME` | `sa` | ✅ |
| `DB_PASSWORD` | (vazio) | ✅ |
| `JWT_SECRET` | string dev (inseguro) | ✅ mín 64 bytes — `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `15` | ❌ |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | ❌ |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8081,http://localhost:19006` | ✅ |

Copie `.env.example` para `.env` e preencha os valores de produção.

---

## Estrutura de Pastas

```
.
├── src/
│   ├── main/
│   │   ├── java/br/com/clyvo/kura/tutor/
│   │   │   ├── auth/         onboarding/  tutor/
│   │   │   ├── timeline/     consentimento/  agendamento/
│   │   │   └── shared/       (config, exception, audit)
│   │   └── resources/
│   │       ├── application.yml           # config base
│   │       ├── application-dev.yml       # H2, Flyway on, show-sql
│   │       ├── application-prod.yml      # Oracle via env vars
│   │       ├── logback-spring.xml        # MDC correlationId
│   │       └── db/
│   │           ├── migration/            # V1–V6 (Flyway)
│   │           └── callback/             # seeds dev
│   └── test/java/...                     # JUnit 5 + Mockito
├── docs/
│   ├── architecture.md                   # decisões arquiteturais detalhadas
│   ├── diagrams/                         # PlantUML class diagram + DER
│   └── postman/                          # collection + environments
├── Dockerfile                            # multi-stage (builder → jre-jammy)
├── docker-compose.yml
├── .env.example
└── pom.xml
```

---

## Contribuição

- Branch por feature: `feat/E<épico>-<slug>` (ex: `feat/E3-jwt-refresh`)
- Commits seguem **Conventional Commits** com referência à task:
  ```
  feat: add invite-based onboarding (T09)
  fix: correct 401 vs 403 on JWT expiry (T08)
  chore: update Flyway migration V5 (T04)
  ```
- PR só é mergeado com `mvn clean verify` verde
- `main` é branch protegida

---

## Testes

```bash
# Todos os testes
mvn test

# Classe específica
mvn test -Dtest=AgendamentoServiceTest

# Método específico
mvn test -Dtest=GlobalExceptionHandlerTest#erro500DeveLogarStackTraceComCorrelationId
```

---

## Equipe FIAP Challenge 2026

| Membro | Papel |
|---|---|
| **Felipe Ferrete** | Tech Lead · arquitetura cross-API · .NET Backend Clínica · IoT/IA |
| **Nikolas Brisola** | Java Backend Tutor (este repositório) |
| **Guilherme Sola** | Mobile Tutor · UX |
| **Gustavo Bosak** | Mobile Clínica · QA |
| **Clayton** | DevOps · Banco de Dados Oracle |

---

## Licença

Projeto acadêmico — FIAP Challenge 2026. Parceiro: **Clyvo Vet**.
