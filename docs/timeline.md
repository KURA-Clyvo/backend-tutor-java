# Cronograma de Atividades — KURA Tutor (Java Advanced · FIAP 2026)

## Identificação do Projeto

| Campo | Informação |
|---|---|
| **Projeto** | KURA — Backend do Portal do Tutor (API REST Java/Spring Boot) |
| **Disciplina** | Java Advanced |
| **Instituição** | FIAP |
| **Ano/Semestre** | 2026 — Sprint 1 |
| **Repositório** | [github.com/KURA-Clyvo/backend-tutor-java](https://github.com/KURA-Clyvo/backend-tutor-java) |

## Integrantes

| Nome | RM | Papel Principal |
|---|---|---|
| Nikolas Brisola | 564371 | Engenharia de Base — Setup, Arquitetura Inicial e Modelagem de Domínio |
| Felipe Ferrete | 562999 | Engenharia de Integração — Refatoração, Segurança, Documentação e Qualidade |

---

## Matriz de Responsabilidades

A tabela abaixo descreve os artefatos entregues e o responsável principal por cada um. A coluna **Revisão** indica quem realizou a verificação do artefato antes da entrega.

| Artefato / Atividade | Responsável Principal | Revisão |
|---|---|---|
| Setup do projeto Spring Boot (Maven, dependências, perfis dev/prod) | Nikolas Brisola | Felipe Ferrete |
| Modelagem inicial das entidades JPA (`Tutor`, `Pet`, `Especie`, `Raca`) | Nikolas Brisola | Felipe Ferrete |
| Rascunho inicial do banco de dados (versão 1 e versão 2 do schema) | Nikolas Brisola | Felipe Ferrete |
| Definição da arquitetura por bounded contexts (Java ↔ .NET) | Felipe Ferrete | Nikolas Brisola |
| Plano de refatoração para v5 (26 tasks documentadas em `plano.md`) | Felipe Ferrete | — |
| Migrations Flyway V1–V5 (idempotentes, compatíveis com Oracle 19c compartilhado) | Felipe Ferrete | — |
| Configuração multi-perfil (dev/H2, prod/Oracle) e segredos via env vars | Felipe Ferrete | — |
| Containerização (Dockerfile multi-stage + Docker Compose) | Felipe Ferrete | — |
| Camada de segurança JWT (access token 15 min + refresh rotation 7 dias) | Felipe Ferrete | — |
| Proteção contra brute-force (lock após 5 tentativas, HTTP 423) | Felipe Ferrete | — |
| Onboarding por convite (consumo atômico de `INVITE_TUTOR` do .NET) | Felipe Ferrete | — |
| Entidades `@Immutable` para tabelas .NET (leitura sem escrita acidental) | Felipe Ferrete | — |
| Controle de concorrência otimista em `Agendamento` (`@Version`) | Felipe Ferrete | — |
| CRUD de Agendamento com verificação de posse e soft-delete | Felipe Ferrete | — |
| Endpoint de consentimento LGPD com chave de idempotência | Felipe Ferrete | — |
| Cache Caffeine para catálogos imutáveis (`/especies`, `/racas`) | Felipe Ferrete | — |
| Endpoints de timeline e vacinas via views Oracle | Felipe Ferrete | — |
| Queries JPQL customizadas (relatórios de negócio) | Felipe Ferrete | — |
| Handler global de exceções (RFC 7807) e mensagens de validação em PT-BR | Felipe Ferrete | — |
| Filtro MDC de correlation ID e logging estruturado para erros 500 | Felipe Ferrete | — |
| Anotações Swagger completas em todos os endpoints e DTOs | Felipe Ferrete | — |
| Diagrama de Entidade-Relacionamento (DER — notação Barker, Oracle Data Modeler) | Felipe Ferrete | — |
| Diagrama PlantUML do banco (`docs/diagrams/der.puml`) | Felipe Ferrete | — |
| Documentação de arquitetura (`docs/architecture.md`) | Felipe Ferrete | — |
| Collection Postman + ambientes dev e prod (`docs/postman/`) | Felipe Ferrete | Nikolas Brisola |
| Testes de scripts de validação (`tests/`) | Felipe Ferrete | — |
| README completo com instruções de execução e variáveis de ambiente | Felipe Ferrete | — |
| Fixes de integração com .NET (normalização de token, observabilidade 4xx) | Felipe Ferrete | — |
| Release tag `v0.1.0-sprint1` | Felipe Ferrete | — |

---

## Cronograma de Atividades (Timeline)

O desenvolvimento foi organizado em cinco semanas. As datas abaixo refletem o histórico real de commits e atividades de planejamento.

### Semana 1 — Kick-off e Definição de Escopo
**Período:** 21 abr – 27 abr 2026

| Data | Atividade | Responsável |
|---|---|---|
| 25/04 | Commit inicial — estrutura base do projeto Spring Boot | Nikolas Brisola |
| 25/04 | Definição do escopo: Portal do Tutor como contexto Java; demais contextos no .NET | Ambos |
| 25/04 | Alinhamento sobre o banco Oracle compartilhado (FIAP) e divisão de ownership de tabelas | Ambos |

**Entregáveis:** Repositório criado, projeto compilando, escopo definido.

---

### Semana 2 — Modelagem e Arquitetura Base
**Período:** 28 abr – 11 mai 2026

| Data | Atividade | Responsável |
|---|---|---|
| Início mai | Modelagem das entidades de domínio: `Tutor`, `Pet`, `Especie`, `Raca`, `Agendamento` | Nikolas Brisola |
| Início mai | Rascunho do schema banco v1 e v2 (sem Flyway formal) | Nikolas Brisola |
| Início mai | Revisão da modelagem e identificação de inconsistências com o schema .NET | Felipe Ferrete |
| Início mai | Decisão de adotar Flyway com migrations idempotentes para ambiente compartilhado | Felipe Ferrete |

**Entregáveis:** Entidades iniciais, rascunho do banco, decisão de arquitetura sobre Flyway.

---

### Semana 3 — Refatoração de Base e Infraestrutura
**Período:** 16 mai – 18 mai 2026

| Data | Atividade | Responsável |
|---|---|---|
| 16/05 | Refatoração para v2 do banco — alinhamento de nomes de coluna com o .NET | Nikolas Brisola |
| 18/05 | Elaboração do Plano de Refatoração v5 (26 tasks priorizadas em `plano.md`) | Felipe Ferrete |
| 19/05 | T01 — Atualização do `pom.xml`: Flyway Oracle, Caffeine, todas as dependências | Felipe Ferrete |
| 19/05 | T02 — Split de configuração em perfis dev (H2) e prod (Oracle + env vars) | Felipe Ferrete |
| 19/05 | T03 — Dockerfile multi-stage e Docker Compose com health checks | Felipe Ferrete |
| 19/05 | T04 — Migrations Flyway V1–V5 com seeds de dev e compatibilidade Oracle 19c | Felipe Ferrete |
| 19/05 | T06 — DER em notação Barker (Oracle Data Modeler) e PlantUML | Felipe Ferrete |
| 19/05 | T07 — Documento de arquitetura com bounded contexts e decisões técnicas | Felipe Ferrete |

**Entregáveis:** Infraestrutura de container, migrations, diagramas, documento de arquitetura.

---

### Semana 4 — Implementação das Funcionalidades Core
**Período:** 19 mai – 20 mai 2026

| Data | Atividade | Responsável |
|---|---|---|
| 19/05 | T08 — Segurança JWT stateless com entry point 401 e estrutura de pacotes v5 | Felipe Ferrete |
| 19/05 | T09 — Onboarding por convite com transação atômica e proteção anti-reuso (409) | Felipe Ferrete |
| 19/05 | T10 — Login com proteção brute-force (HTTP 423) e refresh token rotation | Felipe Ferrete |
| 19/05 | T11 — Endpoint `/auth/refresh` com rotação e invalidação do token anterior | Felipe Ferrete |
| 19/05 | T12 — Endpoint `/auth/logout` e configuração CORS por perfil | Felipe Ferrete |
| 19/05 | T13 — Realinhamento de entidades read-only com schema v4 (`@Immutable`) | Felipe Ferrete |
| 20/05 | T14 — GET `/pets` com verificação de posse e sem cache | Felipe Ferrete |
| 20/05 | T15 — Cache Caffeine para `/especies` e `/racas` | Felipe Ferrete |
| 20/05 | T16 — Endpoints de timeline e vacinas vencendo via views Oracle | Felipe Ferrete |
| 20/05 | T17 — Consentimento LGPD com idempotência e query `ultimo-por-tipo` | Felipe Ferrete |
| 20/05 | T18 — Realinhamento de `Agendamento` com schema v4/V5 e métodos de domínio | Felipe Ferrete |
| 20/05 | T19 — POST/GET de Agendamento com specifications e ownership check | Felipe Ferrete |
| 20/05 | T20 — PUT (lock otimista `@Version`) e soft-delete de Agendamento | Felipe Ferrete |
| 20/05 | T21 — 3 queries JPQL customizadas para relatórios de negócio | Felipe Ferrete |

**Entregáveis:** Todas as funcionalidades de negócio implementadas e funcionais.

---

### Semana 5 — Qualidade, Documentação e Release
**Período:** 20 mai – 21 mai 2026

| Data | Atividade | Responsável |
|---|---|---|
| 20/05 | T22 — Handler global de exceções (RFC 7807) e mensagens de validação PT-BR | Felipe Ferrete |
| 20/05 | T23 — Anotações Swagger completas em todos os endpoints e DTOs | Felipe Ferrete |
| 20/05 | T24 — Filtro MDC de correlation ID e logging estruturado de erros 500 | Felipe Ferrete |
| 20/05 | T25 — README completo, Collection Postman (dev + prod) | Felipe Ferrete |
| 21/05 | Fixes de integração com .NET: normalização de token, observabilidade 4xx | Felipe Ferrete |
| 21/05 | Fixes de produção: mapeamento de colunas CHAR, ddl-auto validate, HikariCP timeouts | Felipe Ferrete |
| 21/05 | T26 — Tag de release `v0.1.0-sprint1`, gitignore final | Felipe Ferrete |
| 21/05 | Elaboração do Cronograma e Gestão de Configuração para entrega FIAP | Felipe Ferrete |

**Entregáveis:** Projeto estável, documentado, tagueado e pronto para avaliação.

---

## Resumo de Esforço por Integrante

| Integrante | Fases de Maior Contribuição | Artefatos Principais |
|---|---|---|
| **Nikolas Brisola** | Semanas 1–3 (fundação) | Setup Maven, entidades de domínio, rascunho do schema, refatoração v2 |
| **Felipe Ferrete** | Semanas 3–5 (entrega) | Infraestrutura, segurança, todas as features, diagramas, documentação, release |

> O histórico completo e auditável de cada contribuição está disponível no log de commits do repositório GitHub.
