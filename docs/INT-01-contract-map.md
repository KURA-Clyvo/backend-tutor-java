# INT-01 — Mapa de contratos: mobile-tutor-rn ↔ backend-tutor-java

> TASK-08. Gerado validando cada chamada real de `mobile-tutor-rn/src/services/*.ts` contra os
> controllers Java reais (`bff/api/*`), rodando o backend em perfil `dev` (H2) via
> `scripts/test-e2e-tutor.sh`. Não assumido — todas as divergências de payload abaixo foram
> reproduzidas com curl contra o backend real (ver seções "prova a divergência" no script).

## Blockers de infraestrutura corrigidos nesta task

Estes dois bugs impediam o backend de sequer subir/autenticar em `dev` — corrigidos para viabilizar
o smoke test (fora do escopo original de TASK-08, mas bloqueantes; ver checkpoint de execução):

1. **Flyway/H2 incompatível.** `V2`, `V3`, `V5` continham blocos PL/SQL Oracle (`DECLARE...BEGIN...
   EXECUTE IMMEDIATE...END;/`) que o H2 rejeita — o contexto Spring **nunca subia** em `dev`
   (não era só falha de `@DataJpaTest`, como o ledger anterior registrava — era o boot inteiro).
   Fix: `V2/V3/V5` movidos para `db/migration-oracle/` (conteúdo idêntico, só roda em `prod`);
   equivalentes portáveis criados em `db/migration-h2/` (sem os checks defensivos de "já existe?",
   desnecessários num H2 sempre recriado do zero). `spring.flyway.locations` em
   `application-dev.yml`/`application-prod.yml` aponta cada profile para seu par. As duas pastas
   precisam ser *sibling* de `db/migration` (não subpasta) — Flyway descarta locations que são
   sub-diretório de uma já incluída, tratando-as como parte do scan recursivo da pai.
2. **Bug real em `OnboardingService.registrarPorInvite`** (não é infra de teste — afeta produção).
   O código normalizava o token recebido (`.replace("-", "").toUpperCase()`) antes de buscar por
   `NR_TOKEN`, mas essa coluna é populada pelo .NET com `Guid.ToString()` (hifenizado, minúsculo) —
   a normalização faz a busca nunca bater, em nenhum ambiente. **Registro de tutor por convite
   estava completamente quebrado**, incluindo produção. Fix: removida a normalização, comparação
   exata. Isso também corrigiu os 7 testes pré-existentes quebrados de `OnboardingServiceTest`
   (o mock stub já esperava o token exato, sem normalização — o teste "sabia" o comportamento certo).

Resultado: suíte completa `mvn test -Dspring.profiles.active=dev` foi de um estado onde a aplicação
não subia para **133/133 testes verdes**.

## Mapa de contratos

| # | Mobile (`mobile-tutor-rn/src/services`) | Java (`bff/api`) | Status | Divergência | Decisão |
|---|---|---|---|---|---|
| 1 | `POST /api/v1/auth/login` `{dsEmail, dsSenha}` | `POST /v1/auth/login` espera `{email, senha}` | ❌ payload | Campos JSON não batem — todo login do app falharia com 400 contra o Java real | **Ajustar o app**: renomear campos em `types/api.ts:LoginRequest`, `validators.ts`, `app/login.tsx` para `email`/`senha` |
| 2 | `POST /api/v1/auth/register` `{inviteToken, nmTutor, dsSenha, dsTelefone}` | Não existe — Java só tem `POST /v1/auth/register-invite` `{token, senha, aceites[]}` | ❌ rota + payload | Path diferente (`register` vs `register-invite`) **e** payload diferente; `nmTutor`/`dsTelefone` nem existem no DTO Java (vêm da `TUTOR` .NET-owned, não são enviados) | **Ajustar o app**: renomear chamada para `/register-invite`, remover `nmTutor`/`dsTelefone` do payload, adicionar `aceites: AceiteRequest[]` (tela de registro precisa coletar consentimentos LGPD) |
| 3 | `GET /api/v1/tutor/pets` | `GET /v1/tutor/pets` | ✅ | — | — |
| 4 | `GET /api/v1/tutor/pets/{id}` | Stub `501` (`TutorBffController.detalharPet`) | ⚠️ não implementado | Documentado desde TASK-03 | **Implementar** — próxima INT-01 sub-task |
| 5 | `GET /api/v1/tutor/pets/{idPet}/timeline` | Stub `501` (`TutorBffController.timelinePet`) | ⚠️ não implementado | Documentado desde TASK-03; `VW_TIMELINE_PET` (V6) já existe como fonte | **Implementar** — próxima INT-01 sub-task |
| 6 | `GET /api/v1/tutor/pets/{idPet}/timeline/{idEvento}` | Não existe (nem stub) | ❌ ausente | Sem rota alguma para detalhe de evento | **Implementar** (detalhe) — ou remover a tela de detalhe do app se não for crítico ao MVP |
| 7 | `GET /api/v1/tutor/pets/{id}/vacinas` | Não existe | ❌ ausente | `VW_VACINAS_VENCENDO` (V6) já existe como fonte de dado | **Implementar** — controller fino sobre a view já existente |
| 8 | `GET /api/v1/tutor/pets/{id}/vacinas/status` | Não existe | ❌ ausente | Idem #7 | **Implementar** junto com #7 |
| 9 | `GET /api/v1/tutor/agendamentos` | `GET /v1/tutor/agendamentos` | ✅ | — | — |
| 10 | `POST /api/v1/tutor/agendamentos` `{idPet, sgTipoConsulta, dsMotivo, dtPreferida, dtAlternativa?, idClinica?}` | `POST /v1/tutor/agendamentos` espera `{idPet, idClinica, dtAgendamento, tipo, duracaoMinutos, observacoes?}` | ❌ payload | Nomes de campo, obrigatoriedade de `idClinica` e enum de tipo divergem por completo (`sgTipoConsulta` valores `RETORNO\|ROTINA\|URGENCIA\|TELEORIENTACAO` vs `tipo` valores `CONSULTA\|RETORNO\|VACINA\|EXAME\|PROCEDIMENTO\|TELEORIENTACAO`) | **Decisão a registrar em sub-task**: alinhar o app ao contrato Java (mais simples, um único formulário) ou criar tradução no BFF (`dtPreferida`→`dtAgendamento`, mapa de enum) — recomendo alinhar o app, já que o conceito de "data alternativa" não tem equivalente no Java hoje |
| 11 | `DELETE /api/v1/tutor/agendamentos/{id}` | `DELETE /v1/tutor/agendamentos/{id}` | ✅ | — | — |
| 12 | `GET /api/v1/tutor/consentimentos` | `GET /v1/tutor/consentimentos` | ✅ | — | — |
| 13 | `POST /api/v1/tutor/consentimentos` (+ header `Idempotency-Key`) | `POST /v1/tutor/consentimentos` (+ header `Idempotency-Key` obrigatório) | ✅ | O app já envia o header corretamente | — |
| 14 | `DELETE /api/v1/tutor/consentimentos/{id}` | Stub `501` (`ConsentimentoBffController.deletar`) | ⚠️ não implementável | Consentimento é **insert-only** por design (histórico LGPD) — não existe "revogar por delete" | **Ajustar o app**: revogação deve ser um novo `POST` com tipo `REVOGACAO` (o endpoint #13 já suporta isso via `ConsentimentoRequest`), não um `DELETE` |
| 15 | `GET /api/v1/tutor/notificacoes` | Não existe | ❌ ausente | `NOTIFICACAO` é tabela .NET-owned; não há `NotificacaoBffController` | **Decisão a registrar**: implementar leitura read-only (`@Immutable`, mesmo padrão de `PET`/`TUTOR`) ou mover a feature para o app clínica |
| 16 | `PATCH /api/v1/tutor/notificacoes/{id}/lida` | Não existe | ❌ ausente | Idem #15 — e é escrita numa tabela .NET-owned (Java não pode escrever `NOTIFICACAO`) | **Decisão a registrar**: precisa de endpoint .NET (`PATCH /api/v1/notificacoes/{id}/lida`) chamado pelo app diretamente, ou uma tabela de "lida" espelho Java-owned |
| 17 | `PATCH /api/v1/tutor/notificacoes/lidas` | Não existe | ❌ ausente | Idem #16 | Mesma decisão de #16 |
| 18 | `PATCH /api/v1/tutor/me/push-token` `{dsPushToken, dsPlatform}` | `PATCH /v1/tutor/me/push-token` espera `{dsPushToken, dsPlatforma}` | ❌ payload (typo) | Campo Java tem um typo (`dsPlatforma` — falta o 'a' entre 't' e 'f'; nem é "Plataforma" PT nem "Platform" EN correto) que não bate com o `dsPlatform` do app | **Corrigir o typo Java** (`PushTokenRequest.dsPlatforma` → `dsPlatform`, alinhando ao app, que já usa o nome em inglês corretamente) — troca de 1 campo, sem migration (coluna `DS_PLATAFORMA_PUSH` no banco não muda, só o nome do DTO) |

## Resumo

- **✅ Funcionando de ponta a ponta:** `pets` (lista), `agendamentos` (lista/cancelar), `consentimentos` (lista/registrar) — 6 de 18 chamadas.
- **❌ Payload/rota divergente (bug de contrato, não falta de feature):** login, register, criar agendamento, push-token — 4 de 18. Todas com decisão registrada acima; nenhuma implementada nesta task (fora do escopo — ver TASK-03 "documentar, não corrigir").
- **⚠️ Stub `501` documentado (TASK-03):** detalhe de pet, timeline de pet — 2 de 18.
- **❌ Sem backing nenhum no Java:** timeline de evento, vacinas (×2), notificações (×3), delete de consentimento — 6 de 18.

Nenhuma dessas 12 divergências/pendências foi corrigida nesta task — smoke test e mapeamento apenas.
Os dois bugs de infraestrutura (Flyway/H2 e token de convite) foram corrigidos porque bloqueavam
qualquer teste autenticado, inclusive os 3 fluxos que hoje funcionam corretamente.
