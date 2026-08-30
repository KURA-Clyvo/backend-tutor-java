# KURA Backend Tutor — Architecture Decision Record

> **Stack:** Java 21 · Spring Boot 3.2.5 · Oracle 19c (shared) · JWT · Flyway · H2 (dev)
> **Versão:** 1.0 · **Sprint:** 1 · **Entrega FIAP:** 24/05/2026
> **Autores:** Nikolas Brisola (Java) · Felipe Ferrete (tech lead / .NET)

---

## 1. Visão Geral

### 1.1 Topologia do sistema

```
┌─────────────────────┐                       ┌─────────────────────┐
│  App Mobile Tutor   │                       │  Front da Clínica   │
│  (React Native)     │                       │  (Web)              │
└──────────┬──────────┘                       └──────────┬──────────┘
           │ JWT Java (tutor)                            │ JWT .NET (clínica)
           ▼                                             ▼
┌─────────────────────┐    GET /agenda        ┌─────────────────────┐
│  Backend Tutor      │◄─────────────────────►│  Backend Clínica    │
│  Spring Boot 3.2    │    PATCH /status      │  .NET 10            │
│  :8081/api          │    (NR_VERSION sync)  │  :8080              │
└──────────┬──────────┘                       └──────────┬──────────┘
           │                                             │
           └─────────────────────┬───────────────────────┘
                                 ▼
              ┌─────────────────────────────────────┐
              │  Oracle 19c — oracle.fiap.com.br    │
              │  :1521/orcl · user RM562999         │
              │  (schema compartilhado real)        │
              └─────────────────────────────────────┘
```

**Decisão:** dois backends distintos com audiências separadas — Java autentica tutores, .NET autentica clínicas — porque os tokens JWT não são interoperáveis (chaves diferentes) e os domínios de escrita não se sobrepõem, exceto em `AGENDAMENTO`.

O Java roda na porta **8081** com context-path `/api`. Em dev, usa H2 em memória no modo Oracle (`MODE=Oracle`). Em prod, aponta para o Oracle FIAP via variáveis de ambiente `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

### 1.2 Estrutura de pacotes (bounded contexts)

```
br.com.clyvo.kura.tutor
├── shared/          # SecurityConfig, GlobalExceptionHandler, ApiError, CorsConfig
├── auth/            # Login, refresh, logout · ContaTutor (entity write)
├── onboarding/      # POST /onboarding/register-invite (TASK-48; alias legado /auth/register-invite removido na TASK-82) · InviteTutor (read-only)
├── tutor/           # GET tutor, pet, catálogo · todas as entidades @Immutable
├── timeline/        # VW_TIMELINE_PET, VW_VACINAS_VENCENDO (views read-only)
├── consentimento/   # LGPD + IdempotencyKey · LgpdController
└── agendamento/     # AGENDAMENTO shared-write · @Version
```

**Decisão:** pacotes por contexto de negócio (não por camada técnica) porque isola ownership, facilita onboarding de novos devs e deixa explícito quem escreve onde.

---

## 2. Bounded Contexts e Ownership Java vs .NET

### 2.1 Tabela de ownership por tabela

| Tabela | Owner | Acesso Java | Motivo |
|---|---|---|---|
| `CLINICA` | .NET | `@Immutable` (leitura) | Gerenciada pelo painel da clínica |
| `ESPECIE` | .NET | `@Immutable` + cache | Catálogo estático — muda raramente |
| `TIPO_EVENTO` | .NET | `@Immutable` (leitura) | Catálogo controlado pela clínica |
| `RACA` | .NET | `@Immutable` + cache | Catálogo estático — muda raramente |
| `VETERINARIO` | .NET | `@Immutable` (leitura) | CRMV e dados gerenciados pela clínica |
| `TUTOR` | .NET | `@Immutable` (leitura) | Cadastrado no balcão pelo .NET |
| `PET` | .NET | `@Immutable` (leitura) | Cadastrado pelo vet via .NET |
| `TUTOR_PET` | .NET | `@Immutable` (leitura) | Vínculo criado no fluxo .NET |
| `INVITE_TUTOR` | .NET | lê 1x via `Repository` mínimo | .NET gera token, Java consome uma vez |
| `EVENTO_CLINICO` | .NET | stub FK (não lê dados) | Só existe para satisfazer FK de `AGENDAMENTO.ID_EVENTO_GERADO` |
| `CONTA_TUTOR` | **Java** | leitura + escrita | Criada no fluxo register-invite |
| `CONSENTIMENTO` | **Java** | INSERT-only (histórico LGPD) | Imutável por lei — nunca UPDATE |
| `IDEMPOTENCY_KEY` | **Java** | leitura + escrita | Exactly-once em POSTs sensíveis |
| `AGENDAMENTO` | **Java + .NET** | Java POST/PUT/DELETE · .NET PATCH status | Shared-write com `@Version` |

### 2.2 Bounded contexts por escrita

| Contexto | Tabelas escritas | Tabelas lidas |
|---|---|---|
| `auth` | `CONTA_TUTOR` | — |
| `onboarding` | `CONTA_TUTOR`, `CONSENTIMENTO` (mesma TX) | `INVITE_TUTOR`, `TUTOR` |
| `tutor` | — | `TUTOR`, `PET`, `TUTOR_PET`, `ESPECIE`, `RACA`, `VETERINARIO`, `CLINICA` |
| `timeline` | — | `VW_TIMELINE_PET`, `VW_VACINAS_VENCENDO` |
| `consentimento` | `CONSENTIMENTO`, `IDEMPOTENCY_KEY` | `TUTOR` |
| `agendamento` | `AGENDAMENTO` (com `@Version`) | `TUTOR`, `PET`, `CLINICA`, `VETERINARIO` |

**Decisão:** Java **nunca escreve** em tabelas de ownership `.NET` — `@Immutable` no Hibernate + `extends Repository<T,ID>` (sem `save()`) em vez de `JpaRepository` garantem isso em duas camadas.

---

## 3. Fluxo Invite-Based

### 3.1 Sequence diagram

```
Front Clínica      .NET          Tutor         App RN         Java
     │              │              │               │             │
     │ POST /tutores│              │               │             │
     │─────────────►│              │               │             │
     │              │ INSERT TUTOR │               │             │
     │              │ INSERT INVITE│               │             │
     │              │ (UUID, TTL7d)│               │             │
     │◄─────────────│              │               │             │
     │  {token,canal}              │               │             │
     │                             │               │             │
     │   envia link (WhatsApp/email/SMS)           │             │
     │────────────────────────────────────────────►│             │
     │                             │  abre link    │             │
     │                             │──────────────►│             │
     │                             │ define senha  │             │
     │                             │ + aceita LGPD │             │
     │                             │──────────────►│             │
     │                             │               │ POST /onboarding/register-invite
     │                             │               │ {token, senha, aceites[]}
     │                             │               │────────────►│
     │                             │               │        ┌────┴─────┐
     │                             │               │        │ TX BEGIN │
     │                             │               │        │ 1. busca │
     │                             │               │        │    invite│
     │                             │               │        │ 2. valida│
     │                             │               │        │ 3. INSERT│
     │                             │               │        │    CONTA │
     │                             │               │        │ 4. N x   │
     │                             │               │        │    INSERT│
     │                             │               │        │    CONS. │
     │                             │               │        │ 5. gera  │
     │                             │               │        │    tokens│
     │                             │               │        │ TX COMMIT│
     │                             │               │        └────┬─────┘
     │                             │               │◄────────────│
     │                             │               │ 201 {access,refresh}
```

### 3.2 Endpoint e códigos de resposta

```
POST /api/onboarding/register-invite
Content-Type: application/json
X-Forwarded-For: <ip_real>

{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "senha": "MinhaSenh@F0rte",
  "aceites": [
    { "tipo": "LEMBRETES",      "aceito": true,  "versaoTermo": "v1.0" },
    { "tipo": "TELEORIENTACAO", "aceito": true,  "versaoTermo": "v1.0" },
    { "tipo": "MARKETING",      "aceito": false, "versaoTermo": "v1.0" }
  ]
}
```

| Status | Condição |
|---|---|
| 201 | Sucesso — retorna `{ accessToken, refreshToken, tutor }` |
| 400 | Senha fraca / payload inválido / versão de termo desconhecida |
| 404 | Token não existe em `INVITE_TUTOR` |
| 409 | Invite já utilizado (`ST_UTILIZADO='S'`) ou race condition (`UK_CONTA_INVITE_USED`) |
| 410 | `DT_EXPIRACAO < NOW()` — token expirado (Gone, não 400, porque existiu) |
| 422 | Tutor inativo ou sem aviso de privacidade aceito no balcão |

> **TASK-48:** `/onboarding` é o prefixo primário (era `/auth`, colidindo com `AuthController`).
> **TASK-82 (2026-08-11):** o alias `@Deprecated` `/auth/register-invite` foi **removido**.
> `OnboardingController` agora mapeia só `/onboarding` — o alias, mantido desde a TASK-48
> durante uma transição, foi removido depois que G0 do backlog (varredura nos 6 repos)
> confirmou zero consumidores reais; hoje `/api/auth/register-invite` responde `404`
> (autenticado) ou `401` (anônimo, porque saiu de `ROTAS_PUBLICAS` e cai em
> `anyRequest().authenticated()`), coberto por
> `OnboardingControllerTest#postNoAliasLegadoDeveResponder404`. O app (`mobile-tutor-rn`) não é
> afetado: ele chama `POST /api/v1/auth/register-invite`, servido por `AuthBffController`, que já
> delegava direto ao `OnboardingService` e nunca dependeu deste mapeamento.

### 3.3 Defense-in-depth contra reuso de invite

O Java **nunca escreve** em `INVITE_TUTOR` — o anti-reuso opera em duas camadas independentes:

1. **Aplicação:** `findByNrToken` filtra `ST_UTILIZADO='N' AND ST_ATIVO='S'` → resposta amigável 409.
2. **Banco:** `UK_CONTA_INVITE_USED UNIQUE(ID_INVITE_USADO)` na `CONTA_TUTOR` — mesmo com race condition (dois requests simultâneos passando pelo check em memória), apenas um `INSERT` é aceito; o segundo viola a UK → `DataIntegrityViolationException` → HTTP 409.

**Decisão:** a checagem dupla existe porque a verificação na aplicação devolve mensagem de erro legível, enquanto a constraint do banco garante corretude mesmo sob concorrência.

---

## 4. Estratégia de Concorrência — Optimistic Locking

### 4.1 Por que optimistic locking em `AGENDAMENTO`

`AGENDAMENTO` é a única tabela com escrita simultânea Java e .NET:

- **Java escreve:** POST (criar), PUT (reagendar), DELETE soft (cancelar pelo tutor).
- **.NET escreve:** PATCH `ST_STATUS` apenas (REALIZADO, CANCELADO, NAO_COMPARECEU, CONFIRMADO — pelo vet), respeitando a maquina de estados da FD-06: `INTENCAO -> CANCELADO`; `AGENDADO -> CONFIRMADO, REALIZADO, CANCELADO, NAO_COMPARECEU`; `CONFIRMADO -> REALIZADO, CANCELADO, NAO_COMPARECEU`. `REALIZADO`, `CANCELADO` e `NAO_COMPARECEU` sao **finais** nos dois backends (`StatusAgendamento.isFinal()` aqui, `AgendaService.StatusFinais` no .NET).

Pessimistic locking (`SELECT FOR UPDATE`) bloquearia linhas entre os dois backends em sistemas separados, causando deadlocks e degradando throughput. Optimistic locking detecta conflito apenas no momento do commit, sem manter locks.

### 4.2 Implementação

```java
@Version
@Column(name = "NR_VERSION", nullable = false)
private Long nrVersion;
```

O Hibernate inclui automaticamente na cláusula `WHERE`:

```sql
UPDATE AGENDAMENTO
   SET ST_STATUS = ?, NR_VERSION = NR_VERSION + 1
 WHERE ID_AGENDAMENTO = ?
   AND NR_VERSION = ?   -- ← se diferente: 0 rows → OptimisticLockingFailureException
```

`OptimisticLockingFailureException` → `GlobalExceptionHandler` → **HTTP 409 Conflict**.

### 4.3 Contrato com o .NET

O .NET expõe `PATCH /api/v1/agendamentos/{id}/status` com `nrVersion` no body. Ambos os backends retornam a `nrVersion` atualizada na response. O frontend persiste e reenvia. Quem enviar uma version stale recebe 409 e deve recarregar o recurso antes de tentar novamente.

**Decisão:** `@Version` com `NR_VERSION` (inteiro no banco, não timestamp) foi escolhido porque é atômico, portável entre Oracle e H2, e o .NET lê e incrementa a mesma coluna — clock skew entre servidores não é um fator.

---

## 5. Estratégia de Idempotência

### 5.1 Problema

`POST /consentimento` e `POST /onboarding/register-invite` são operações que **não podem ser executadas duas vezes** — um clique duplo ou retry de rede geraria registros duplicados de consentimento LGPD ou tentativa de criar duas contas.

### 5.2 Solução: tabela `IDEMPOTENCY_KEY`

```
IDEMPOTENCY_KEY
├── DS_KEY          VARCHAR2(64)  -- UUID gerado pelo cliente (header Idempotency-Key)
├── NM_RESOURCE     VARCHAR2(60)  -- nome do recurso (ex: "CONSENTIMENTO")
├── ID_RESOURCE_CRIADO NUMBER(10) -- ID do recurso criado na primeira chamada
├── DT_CRIACAO      TIMESTAMP     -- início do TTL
└── DT_EXPIRACAO    TIMESTAMP     -- DT_CRIACAO + 24h
UK: (DS_KEY, NM_RESOURCE)
```

**Fluxo:**

1. Cliente envia `Idempotency-Key: <uuid>` no header.
2. Service busca `(DS_KEY, NM_RESOURCE)` na tabela.
3. Se já existe e `DT_EXPIRACAO > NOW()` → retorna o recurso original sem re-executar.
4. Se não existe → executa a operação, grava `IDEMPOTENCY_KEY`, retorna 201.
5. Job agendado limpa registros com `DT_EXPIRACAO < NOW()` (TTL 24h).

**Decisão:** tabela auxiliar no banco (em vez de cache Redis) porque a operação está dentro de uma transação — se o INSERT do consentimento falhar, o `IDEMPOTENCY_KEY` também faz rollback, garantindo atomicidade sem estado externo.

---

## 6. Estratégia de Cache

### 6.1 O que é cacheado e por quê

Somente dois tipos de entidade recebem cache:

| Entidade | Cache name | TTL | Justificativa |
|---|---|---|---|
| `Especie` | `especies` | infinito (expire-after-write: 1h) | Catálogo controlado pelo .NET, muda no máximo em deploy; invalidação manual via `/actuator/caches` |
| `Raca` | `racas` | infinito (expire-after-write: 1h) | Mesmo motivo — `ID_ESPECIE` como chave composta garante isolamento |

### 6.2 O que **não** é cacheado e por quê

| Entidade | Motivo para NÃO cachear |
|---|---|
| `Tutor` | Mutável pelo .NET a qualquer momento — cachear entregaria dados desatualizados em contexto de saúde |
| `Pet` | Mutável pelo .NET — peso, porte, status alteram no fluxo clínico; não cacheado |
| `Agendamento` | Shared-write — o próprio Java escreve; cache introduziria inconsistência com `@Version` |
| `ContaTutor` | Dado de segurança — stale cache poderia manter conta bloqueada acessível |

### 6.3 Configuração

```yaml
spring:
  cache:
    type: caffeine   # in-process, zero latência, sem infra extra
```

```java
@Cacheable("especies")
public List<EspecieDto> listarEspecies() { ... }

@Cacheable(value = "racas", key = "#idEspecie")
public List<RacaDto> listarRacas(Long idEspecie) { ... }
```

**Decisão:** Caffeine (in-process) em vez de Redis porque os dados cacheados são imutáveis de negócio (catálogo controlado pelo .NET), o volume é pequeno (dezenas de registros), e evitar infra de cache distribuído simplifica o deploy FIAP.

---

## 7. Estratégia de Autenticação

### 7.1 Tokens JWT: access + refresh

| Token | Expiração | Armazenamento | Uso |
|---|---|---|---|
| Access token | 15 minutos | Apenas no cliente (header `Authorization: Bearer`) | Autentica cada request à API |
| Refresh token | 7 dias | Hash BCrypt em `CONTA_TUTOR.DS_REFRESH_TOKEN_HASH` | Obtém novo access token em `POST /auth/refresh` |

**Decisão:** access token de 15 minutos minimiza a janela de abuso em caso de interceptação — se vazado, expira rápido sem precisar de revogação ativa.

### 7.2 Refresh rotation

A cada chamada a `POST /auth/refresh`:

1. Java valida o refresh token recebido contra o hash armazenado (BCrypt).
2. Invalida o token atual (`DS_REFRESH_TOKEN_HASH = NULL`, `DT_REFRESH_EXPIRA = NULL`).
3. Gera novo par access + refresh, grava novo hash, retorna ambos.

Se o token enviado não bater com o hash → **HTTP 401** e a conta é considerada potencialmente comprometida.

**Decisão:** rotation garante que um refresh token roubado só pode ser usado uma vez — na próxima tentativa, o hash do banco já mudou.

### 7.3 Estrutura do JWT (claims)

```json
{
  "sub":   "42",
  "email": "felipe@clyvo.vet",
  "iat":   1716120000,
  "exp":   1716120900
}
```

`sub` = `ID_CONTA` (Long) — identificador único e estável. O serviço carrega o `ContaTutor` por ID no filtro de autenticação.

### 7.4 Segurança de chave

```yaml
# dev: fallback com aviso (não usar em prod)
kura.jwt.secret: ${JWT_SECRET:dev-secret-trocar-em-prod-...}

# prod: sem fallback — startup falha se ausente
kura.jwt.secret: ${JWT_SECRET}
```

`JWT_SECRET` deve ter no mínimo 64 bytes de entropia. Em prod, injetado via variável de ambiente no `docker-compose.yml` (não commitado) ou secret manager.

### 7.5 Fluxo de autenticação

```
POST /api/auth/login  →  valida email + BCrypt  →  201 {access, refresh}
POST /api/auth/refresh →  valida refresh hash   →  200 {access, refresh}
POST /api/auth/logout  →  invalida refresh hash  →  204
```

---

## 8. Tratamento de Erros — RFC 7807 Simplificado

### 8.1 Estrutura `ApiError`

Todos os erros da API retornam o mesmo envelope JSON (subconjunto do RFC 7807 Problem Details):

```json
{
  "timestamp":     "2026-05-19T10:30:00Z",
  "status":        409,
  "error":         "Conflict",
  "message":       "Invite já utilizado.",
  "path":          "/api/onboarding/register-invite",
  "correlationId": "a1b2c3d4"
}
```

**Decisão:** RFC 7807 completo (`type`, `title`, `detail`, `instance`) foi simplificado para 6 campos porque a banca FIAP avalia consistência de formato, não aderência total ao RFC — campos extras adicionariam verbosidade sem valor para os clientes (mobile e web).

### 8.2 Mapeamento de exceções

| Exceção | HTTP | Uso |
|---|---|---|
| `EntityNotFoundException` | 404 Not Found | Recurso não encontrado por ID |
| `ConflictException` | 409 Conflict | Invite reusado, UK violada, version stale |
| `GoneException` | 410 Gone | Invite expirado (existiu, não existe mais) |
| `UnprocessableException` | 422 Unprocessable | Tutor inativo, aviso de privacidade pendente |
| `AccountLockedException` | 423 Locked | Conta bloqueada após 5 tentativas falhas |
| `OptimisticLockingFailureException` | 409 Conflict | Concorrência em `AGENDAMENTO` |
| `DataIntegrityViolationException` | 409 Conflict | UK/FK violada no banco |
| `MethodArgumentNotValidException` | 400 Bad Request | Falha em `@Valid` (Bean Validation) |
| `AccessDeniedException` | 403 Forbidden | JWT válido mas sem permissão |
| `AuthenticationException` | 401 Unauthorized | JWT ausente, inválido ou expirado |
| `Exception` (fallback) | 500 Internal Server Error | Erro não mapeado (loga stack trace) |

### 8.3 CorrelationId

Todo request recebe um `X-Correlation-Id` gerado pelo `CorrelationIdFilter` (UUID v4) se não vier no header. O ID aparece na resposta de erro e em todos os logs do request, permitindo rastrear um erro do cliente até o log do servidor.

---

## 9. Migrations Versionadas (Flyway V1–V5)

### 9.1 Estratégia

**Decisão:** todo DDL passa pelo Flyway — nenhum `ALTER TABLE` manual — porque o Oracle FIAP é compartilhado com o .NET (EF Migrations) e qualquer mudança não versionada quebraria o outro backend.

O Java versiona apenas:
- V1 (schema base comum, cópia do `kura_schema_v4.sql`)
- Tabelas Java-owned: `CONTA_TUTOR`, `CONSENTIMENTO`, `IDEMPOTENCY_KEY`
- Índices e constraints de responsabilidade Java

### 9.2 Inventário de migrations

| Migration | O que faz | Por quê existe |
|---|---|---|
| `V1__initial_schema.sql` | Cria as 14 tabelas, 4 sequences, 2 views, índices base | Linha de partida — permite devs Java rodarem H2 local sem acesso Oracle |
| `V2__concurrency_idempotency.sql` | `IDX_IDEMPOT_CRIACAO` em `IDEMPOTENCY_KEY` | Índice de limpeza do job TTL — sem ele, `DELETE WHERE DT_EXPIRACAO < NOW()` faz full scan |
| `V3__invite_based_compatibility.sql` | `UK_CONTA_INVITE_USED` + `IDX_INVITE_TOKEN_ATIVO` | Tolerância a schema parcial (Oracle FIAP pode ter sido criado antes do Flyway) — DDL puro (sem PL/SQL) para compatibilidade com H2 |
| `V4__lgpd_evidencia_consentimento.sql` | Placeholder `SELECT 1 FROM DUAL` | Decisão arquitetural pendente (Felipe Ferrete) — schema v4 removeu campos de evidência LGPD; se aprovado, substitui pelo `ALTER TABLE CONSENTIMENTO ADD (DS_TEXTO_TERMO CLOB, DS_IP_ACEITE VARCHAR2(45), ...)` |
| `V5__agendamento_observacoes.sql` | `ALTER TABLE AGENDAMENTO ADD` 6 colunas + FK `EVENTO_CLINICO` + índice | Campos Java não presentes no schema v4 original — o `.NET` não os escreve; preservá-los evita reescrita da entity |

### 9.3 Profile e localização

| Profile | Flyway locations | Callback `afterMigrate__seeds_dev.sql` |
|---|---|---|
| `dev` | `classpath:db/migration` + `classpath:db/callback` | **Executa** — seeds idempotentes (MERGE) para H2 local |
| `prod` | `classpath:db/migration` apenas | **Não executa** — Oracle FIAP já tem dados reais |

```yaml
# dev
spring.flyway.locations: classpath:db/migration,classpath:db/callback
spring.flyway.baseline-on-migrate: true

# prod
spring.flyway.locations: classpath:db/migration
spring.jpa.hibernate.ddl-auto: validate   # falha se schema divergir das entities
```

---

## 10. Como Rodar Localmente

### 10.1 Pré-requisitos

| Ferramenta | Versão mínima | Verificação |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven | 3.9 | `mvn -version` |
| Docker + Compose | qualquer recente | `docker compose version` |
| (Opcional) Oracle client | — | Necessário apenas para conectar ao Oracle FIAP |

### 10.2 Dev local com H2 (mais rápido)

```bash
# 1. Clone e entre no diretório
git clone <repo-url>
cd backend-tutor-java

# 2. Suba com profile dev (H2 em memória, sem Oracle)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Acesse
# API:         http://localhost:8081/api
# Swagger UI:  http://localhost:8081/api/swagger-ui.html
# H2 Console:  http://localhost:8081/api/h2-console
#              JDBC URL: jdbc:h2:mem:kuradb  user: sa  password: (vazio)
# Actuator:    http://localhost:8081/api/actuator/health
```

O Flyway cria o schema e executa o callback de seeds automaticamente. Tutor de teste: `felipe@clyvo.vet`, token de invite: `550e8400-e29b-41d4-a716-446655440000`.

### 10.3 Dev local com Docker (imagem prod-like)

```bash
# 1. Copie e edite o override de exemplo
cp docker-compose.override.yml.example docker-compose.override.yml
# Edite docker-compose.override.yml com as credenciais Oracle FIAP

# 2. Build e suba
docker compose up --build

# 3. Healthcheck (aguarde ~90s de start-period)
curl http://localhost:8081/api/actuator/health
```

### 10.4 Apontando para Oracle FIAP (prod)

```bash
export DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521/orcl"
export DB_USERNAME="RM562999"
export DB_PASSWORD="<senha>"
export JWT_SECRET="<mínimo-64-bytes-gerados-com-openssl-rand-base64-64>"

./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

> **Atenção:** em prod, `ddl-auto: validate` — o Hibernate valida o schema contra as entities na inicialização. Se houver drift (entity mapeando coluna inexistente), a aplicação não sobe. Isso é intencional.

### 10.5 Executar os testes de validação estática

```bash
# Migrations
bash tests/test_migrations.sh

# Docker setup
bash tests/test_docker_setup.sh

# DER diagrams
bash tests/test_diagrams.sh

# Architecture document
bash tests/test_architecture.sh
```

---

## 11. Dívida Técnica — Views Oracle (T16)

### 11.1 Risco de performance: VW_TIMELINE_PET e VW_VACINAS_VENCENDO

As duas views são lidas pelo `TimelineService` sem índices específicos além dos já existentes (`IDX_AGEND_PET`, `IDX_AGEND_TUTOR`, `IDX_AGEND_DT`). Em produção com alto volume de agendamentos, as seguintes situações podem degradar:

| Cenário | Risco | Mitigação sugerida |
|---|---|---|
| `VW_TIMELINE_PET` com muitos agendamentos por pet | Full-scan de `AGENDAMENTO` filtrado por `ID_PET` — `IDX_AGEND_PET` ameniza, mas sem paginação server-side a view retorna tudo | Avaliar índice composto `(ID_PET, DT_AGENDAMENTO DESC)` no Oracle |
| `VW_VACINAS_VENCENDO` com janela de 30 dias grande | `CURRENT_TIMESTAMP + INTERVAL '30' DAY` impede uso de índice se range scan não for possível | Testar `EXPLAIN PLAN` no Oracle FIAP antes do go-live |
| H2 em dev não reflete plano real Oracle | H2 não possui o otimizador Oracle — testes de repositório validam corretude, não performance | Executar testes de carga com Oracle FIAP em staging |

**Decisão atual:** aceitar o risco para o MVP FIAP. Antes do go-live em produção real:
1. Executar `EXPLAIN PLAN` nas duas views no Oracle 19c com dados representativos.
2. Adicionar índice composto `IDX_AGEND_PET_DT ON AGENDAMENTO(ID_PET, DT_AGENDAMENTO)` se necessário (via V7 migration).
3. Avaliar materialização das views se o volume de pets/agendamentos justificar (Oracle Materialized View com refresh periódico).

---

## Decisões Pendentes

| Decisão | Responsável | Impacto |
|---|---|---|
| **V4 LGPD:** adicionar `DS_TEXTO_TERMO`, `DS_IP_ACEITE`, `DT_REVOGACAO`, `DS_IP_REVOGACAO` em `CONSENTIMENTO` | Felipe Ferrete | Se aprovado: substituir placeholder em V4; se rejeitado: documentar risco ANPD art. 7º, I neste arquivo |
| **Schema versionado compartilhado:** quem é dono das migrations de tabelas comuns (V1) no Oracle FIAP quando .NET também usa EF Migrations | Felipe + Nikolas | Combinar antes do deploy em prod: sugestão = Java só migra tabelas Java-owned, .NET migra o restante |
| **Performance views T16:** índice composto `(ID_PET, DT_AGENDAMENTO)` em AGENDAMENTO | Felipe + Nikolas | Avaliar após testes de carga com dados Oracle FIAP reais (ver seção 11.1) |
