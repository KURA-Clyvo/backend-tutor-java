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

## Atualização TASK-31 (2026-08-04) — fechamento dos stubs 501 e endpoints sem backing

Reexecutado o levantamento (`grep -rn "api/v1/tutor" mobile-tutor-rn/src/services/`) contra o código
real dos controllers Java. Duas descobertas importantes que corrigem o mapa original:

1. **A linha #3 (`GET /pets`) estava marcada ✅ incorretamente.** O DTO Java real
   (`PetResponse`: `idPet, nmPet, nmEspecie, nmRaca, sgSexo, dtNascimento, sgPorte`, dentro de um
   `Page`) não bate em nada com o tipo que o app consome (`PetTutorResponse`: `id, dsStatusGeral,
   nrAlertasAtivos, nrConsultas, chips, condicoes, ...`, array plano). O app já roda hoje contra esse
   contrato via `usePets()`/`mapPetDto` — ou seja, **a lista de pets em produção real quebraria** (ou
   está sendo consumida só via mock/`EXPO_PUBLIC_USE_MOCKS`). Isso não estava documentado antes.
   **Fora do escopo da TASK-31** (não é um dos 5 itens travados) — requer decisão de produto própria
   (de onde vêm `chips`/`condicoes`/`dsStatusGeral` — o mesmo problema das linhas #4-8, mas com
   superfície maior). Rastrear como nova task.
2. **As linhas #12/#13 (`GET`/`POST /consentimentos`) também estavam ✅ incorretas** pelo mesmo
   motivo: o Java real espera `{tipo, versaoTermo, aceito: 'S'|'N', textoTermo?}` com enum
   `TipoConsentimento` (`TELEORIENTACAO, LEMBRETES, DADOS_ANONIMOS, COMPARTILHAR_SEGURADORA,
   MARKETING`); o app manda/espera `{dsTipoConsentimento, dsAceite}` com um enum de negócio
   totalmente diferente (`COMUNICACAO_WHATSAPP, DADOS_CLINICOS_IA, COMPARTILHAMENTO_LABORATORIO`).
   A TASK-31 implementou a revogação como POST reaproveitando o **mesmo payload que o app já usa
   para assinar** (mecanismo correto: insert-only, sem DELETE) — mas isso herda o mesmo mismatch de
   payload do #13, então tanto assinar quanto revogar continuam falhando contra o Java real até essa
   divergência maior ser resolvida (fora do escopo desta task: mapear taxonomia de consentimento é
   decisão de produto, não técnica).

Dos 5 itens travados com o usuário para esta task, os 4 primeiros foram implementados no Java
(read-only onde aplicável, `idTutor` sempre do JWT) e o app foi ajustado para os contratos reais:

- `TutorBffController.detalharPet` (linha #4) e `.timelinePet` (linha #5) deixaram de ser stub —
  agora delegam para `TutorService.buscarPetDetalhe` / `TimelineService.listarTimeline`.
- `TutorBffController.detalharEventoTimeline` (linha #6), `.vacinasPet` e `.statusVacinasPet`
  (linhas #7/#8) são endpoints novos, todos self-scoped e read-only.
- `NotificacaoBffController` (linhas #15/#16/#17) é novo — só `GET`, leitura de `NOTIFICACAO`
  (entidade `@Immutable`, sem nenhum método de escrita no repositório).
- `ConsentimentoBffController.deletar` (linha #14) permanece como estava (stub 501, coberto por
  teste) — decisão foi **não implementar DELETE**; o app não o chama mais.

Achado extra (infra, não fazia parte da lista): `NoResourceFoundException` (Spring 6.1/Boot 3.2, uma
rota sem handler que também não bate com nenhum recurso estático) caía no catch-all `Exception.class`
de `GlobalExceptionHandler` e virava 500 em vez de 404 — descoberto testando que
`PATCH /notificacoes/{id}/lida` (deliberadamente inexistente) responde 404. Corrigido com um
`@ExceptionHandler(NoResourceFoundException.class)` dedicado.

Achado extra (dead code): existia um `TimelineController` (`timeline/api/TimelineController.java`,
fora de `/v1/tutor`, `GET /pets/{idPet}/timeline` e `GET /tutores/{idTutor}/vacinas-vencendo` com
`idTutor` **vindo do path** — violando a regra self-scoped) que não é chamado pelo app real (paths
não batem com o que `mobile-tutor-rn` usa). Não foi removido nesta task (fora do escopo autorizado),
mas `TimelineService.listarVacinasVencendo` (seu único caller) foi marcado `@Deprecated` com
javadoc explicando que a superfície pública correta agora é `listarVacinasPet`/`statusVacinasPet`.

## Atualização TASK-48 (2026-08-06) — `/auth` deixa de ser compartilhado por dois controllers

`auth/api/AuthController` (login/refresh/logout) e `onboarding/api/OnboardingController`
(`register-invite`) disputavam o mesmo prefixo `/auth` sem colidir apenas porque os paths de
método não se cruzavam — qualquer rota nova em um dos dois podia quebrar o outro silenciosamente.
`OnboardingController` ganhou prefixo próprio: **`/onboarding`** (`POST /onboarding/register-invite`).

**Isto não afeta o contrato consumido pelo app.** `mobile-tutor-rn` (pós-TASK-55) chama
`POST /api/v1/auth/register-invite`, servido por `bff/api/AuthBffController#registerInvite` — um
controller distinto que já delegava direto a `OnboardingService`, sem depender do mapeamento de
`OnboardingController`. A linha #2 abaixo (histórica, pré-TASK-55) estava desatualizada nesse
ponto — ver correção logo após a tabela.

O caminho legado `POST /api/auth/register-invite` (sem `/v1`, sem `/onboarding`) continua
respondendo via alias `@Deprecated` no `OnboardingController` (`@RequestMapping({"/onboarding",
"/auth"})`), coberto por teste (`OnboardingControllerTest#postNoAliasLegadoDeveContinuarFuncionando`).
Prazo sugerido de remoção do alias: ~30 dias após TASK-48 (revisar até 2026-09-06) — nenhum
consumidor confirmado depende dele hoje (nem o app, nem o script `scripts/test-e2e-tutor.sh`, que
já testava só a superfície `/v1/**`).

## Mapa de contratos

| # | Mobile (`mobile-tutor-rn/src/services`) | Java (`bff/api`) | Status | Divergência | Decisão |
|---|---|---|---|---|---|
| 1 | `POST /api/v1/auth/login` `{dsEmail, dsSenha}` | `POST /v1/auth/login` espera `{email, senha}` | ❌ payload | Campos JSON não batem — todo login do app falharia com 400 contra o Java real | **Ajustar o app**: renomear campos em `types/api.ts:LoginRequest`, `validators.ts`, `app/login.tsx` para `email`/`senha` — não tocado na TASK-31 (fora do escopo) |
| 2 | `POST /api/v1/auth/register-invite` `{inviteToken, ...}` | `POST /v1/auth/register-invite` (`AuthBffController`) `{token, senha, aceites[]}` | ✅ **corrigido na TASK-55** (repo `mobile-tutor-rn`, `d2731b9`) | Era `POST /api/v1/auth/register` (rota errada, 404) — corrigido para `register-invite`. Payload já estava certo (`inviteToken`) | **App corrigido** (TASK-55). Rota servida por `AuthBffController`, independente do prefixo de `OnboardingController` (que mudou de `/auth` para `/onboarding` na TASK-48, sem afetar este contrato) |
| 3 | `GET /api/v1/tutor/pets` | `GET /v1/tutor/pets` | ❌ payload (achado TASK-31 — marcado ✅ incorretamente antes) | `PetResponse` real (idPet/nmPet/nmEspecie/...) não bate com `PetTutorResponse` esperado pelo app (id/dsStatusGeral/chips/condicoes/...); também retorna `Page`, app espera array plano | **Fora do escopo da TASK-31** — precisa decisão de produto sobre `chips`/`condicoes`/`dsStatusGeral`. Rastrear como nova task |
| 4 | `GET /api/v1/tutor/pets/{id}` | `GET /v1/tutor/pets/{id}` (`TutorBffController.detalharPet`) | ✅ **implementado na TASK-31** | Era stub 501 | **Implementado no Java** (read-only, self-scoped) — DTO honesto (`PetDetalheResponse`); app usa `mapPetDetailDto` para preencher como ausente os campos que a UI aspiracional espera mas o backend não tem (chips/vitais/observações/condições) |
| 5 | `GET /api/v1/tutor/pets/{idPet}/timeline` | `GET /v1/tutor/pets/{id}/timeline` (`TutorBffController.timelinePet`) | ✅ **implementado na TASK-31** | Era stub 501 | **Implementado no Java** via `TimelineService.listarTimeline` (VW_TIMELINE_PET, paginado); app extrai `.content` e usa `mapTimelineEventoDto` |
| 6 | `GET /api/v1/tutor/pets/{idPet}/timeline/{idEvento}` | `GET /v1/tutor/pets/{id}/timeline/{idEvento}` (`TutorBffController.detalharEventoTimeline`) | ✅ **implementado na TASK-31** | Não existia rota alguma | **Implementado no Java** — mesma fonte de dado da lista (VW_TIMELINE_PET); sem SOAP/diagnóstico estruturado ainda, os campos ricos do detalhe (`dsDiagnostico`, `prescricoes`, etc.) ficam ausentes por ora |
| 7 | `GET /api/v1/tutor/pets/{id}/vacinas` | `GET /v1/tutor/pets/{id}/vacinas` (`TutorBffController.vacinasPet`) | ✅ **implementado na TASK-31** | Não existia | **Implementado no Java** sobre `VW_VACINAS_VENCENDO` — só retorna pendências futuras (nunca "aplicadas"); app mapeia `sgStatus` sempre como `VENCENDO` |
| 8 | `GET /api/v1/tutor/pets/{id}/vacinas/status` | `GET /v1/tutor/pets/{id}/vacinas/status` (`TutorBffController.statusVacinasPet`) | ✅ **implementado na TASK-31** | Não existia | **Implementado no Java** (decisão travada com o usuário) — resumo `{idPet, qtdPendentes, dtProximaDose, dsStatusGeral}` sobre a mesma view |
| 9 | `GET /api/v1/tutor/agendamentos` | `GET /v1/tutor/agendamentos` | ✅ | — | — |
| 10 | `POST /api/v1/tutor/agendamentos` `{idPet, sgTipoConsulta, dsMotivo, dtPreferida, dtAlternativa?, idClinica?}` | `POST /v1/tutor/agendamentos` espera `{idPet, idClinica, dtAgendamento, tipo, duracaoMinutos, observacoes?}` | ❌ payload | Nomes de campo, obrigatoriedade de `idClinica` e enum de tipo divergem por completo | **Decisão pendente** — não tocado na TASK-31 (fora do escopo) |
| 11 | `DELETE /api/v1/tutor/agendamentos/{id}` | `DELETE /v1/tutor/agendamentos/{id}` | ✅ | — | — |
| 12 | `GET /api/v1/tutor/consentimentos` | `GET /v1/tutor/consentimentos` | ❌ payload (achado TASK-31 — marcado ✅ incorretamente antes) | `ConsentimentoResponse` real (`idConsentimento, tipo, versaoTermo, aceito, ativo, dtAceite, dtRevogacao`) não bate com o tipo do app (`id, dsTipoConsentimento, sgStatus, dtConsentimento`) | **Fora do escopo da TASK-31** — mapear taxonomia de consentimento é decisão de produto. Rastrear como nova task |
| 13 | `POST /api/v1/tutor/consentimentos` (+ header `Idempotency-Key`) | `POST /v1/tutor/consentimentos` espera `{tipo, versaoTermo, aceito: 'S'\|'N'}` | ❌ payload (mesmo achado de #12) | App manda `{dsTipoConsentimento, dsAceite:'SIM'}` — campos e enum diferentes | **Fora do escopo da TASK-31** — mesma decisão de produto de #12 |
| 14 | `DELETE /api/v1/tutor/consentimentos/{id}` | Stub `501` (`ConsentimentoBffController.deletar`) — **mantido de propósito** | ✅ **decisão implementada na TASK-31**: app não chama mais DELETE | Consentimento é **insert-only** por design (histórico LGPD) | **Ajustado o app**: `consentimentos.service.ts#revogar` agora faz `POST /consentimentos` com `dsAceite:'NAO'` (mesmo endpoint/payload de "assinar", reaproveitando a semântica `aceito='N'` que `ConsentimentoService` já suporta). Herda o mesmo mismatch de payload de #12/#13 — mecanismo correto (insert-only), mas só funciona de ponta a ponta depois que #12/#13 forem corrigidos |
| 15 | `GET /api/v1/tutor/notificacoes` | `GET /v1/tutor/notificacoes` (`NotificacaoBffController`, novo) | ✅ **implementado na TASK-31** | Não existia `NotificacaoController` | **Implementado no Java** — leitura estritamente read-only de `NOTIFICACAO` (.NET owned; entidade `@Immutable`, repositório sem nenhum método de escrita) |
| 16 | `PATCH /api/v1/tutor/notificacoes/{id}/lida` | Não implementado (decisão: **não implementar**) | ✅ **decisão implementada na TASK-31**: sem 501/404 quebrando o app | Escrita numa tabela .NET-owned — Java nunca escreve `NOTIFICACAO` | **Decisão travada**: não implementar. `notifications.service.ts#marcarLida` resolve localmente (sem chamada de rede); o hook já faz update otimista da cache — "lida" agora é um estado só de sessão, não persistido no servidor |
| 17 | `PATCH /api/v1/tutor/notificacoes/lidas` | Não implementado (decisão: **não implementar**) | ✅ mesma solução de #16 | Idem #16 | Idem #16 — `marcarTodasLidas` também resolve localmente |
| 18 | `PATCH /api/v1/tutor/me/push-token` `{dsPushToken, dsPlatform}` | `PATCH /v1/tutor/me/push-token` espera `{dsPushToken, dsPlatforma}` | ❌ payload (typo) | Campo Java tem um typo (`dsPlatforma`) que não bate com `dsPlatform` do app | **Decisão pendente** — não tocado na TASK-31 (fora do escopo); `registerDeviceToken` já degrada com `try/catch` + `console.warn` |

## Resumo (após TASK-31)

- **✅ Funcionando de ponta a ponta:** `agendamentos` (lista/cancelar) — 2 de 18.
- **✅ Implementado nesta task (TASK-31), read-only, self-scoped:** detalhe de pet, timeline (lista
  e detalhe), vacinas (lista e status), notificações (lista) — 6 de 18.
- **✅ Decisão de produto implementada nesta task (sem quebrar o app):** revogação de consentimento
  (insert-only, sem DELETE), marcar notificação como lida/todas (estado local, sem PATCH ao
  backend) — 3 de 18.
- **❌ Payload/rota divergente, fora do escopo desta task:** login, register, criar agendamento,
  push-token (documentados, não implementados) — 4 de 18.
- **❌ Achado nesta task — marcado ✅ incorretamente antes, precisa de task própria:** `pets` (lista),
  `consentimentos` GET e POST (`assinar`) — 3 de 18. Note-se que a revogação (#14) reaproveita o
  mesmo endpoint de #13, então herda o mesmo bloqueio até essa divergência maior ser resolvida.

**Atualização pós-TASK-55/TASK-48:** a linha #2 (`register`/`register-invite`) saiu da contagem
"❌ payload/rota divergente" acima — o app foi corrigido na TASK-55 (repo `mobile-tutor-rn`) para
chamar `POST /api/v1/auth/register-invite` com o payload correto, e a TASK-48 (este repo) separou
o prefixo de `OnboardingController` (`/auth` → `/onboarding`) sem afetar essa rota, que é servida
por `AuthBffController`. Contagem atualizada: **✅ funcionando de ponta a ponta: 3 de 18**
(`agendamentos` lista/cancelar + `register-invite`); **❌ payload/rota divergente: 3 de 18**
(login, criar agendamento, push-token).

Histórico anterior (TASK-08/INT-01 original) preservado abaixo para contexto — os dois bugs de
infraestrutura (Flyway/H2 e token de convite) seguem corrigidos e não foram alterados nesta task.
