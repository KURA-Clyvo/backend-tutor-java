#!/usr/bin/env bash
# ─── Test Suite: Architecture Document Validation (T07) ───────────────────────
# Valida estrutura, seções obrigatórias e termos técnicos de docs/architecture.md
# Uso: bash tests/test_architecture.sh
# Não requer nenhuma dependência além de grep — análise estática do Markdown.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC="$ROOT_DIR/docs/architecture.md"
PASS=0
FAIL=0

_assert() {
  local desc="$1"; shift
  if "$@" &>/dev/null; then
    printf "  PASS %s\n" "$desc"
    PASS=$((PASS + 1))
  else
    printf "  FAIL %s\n" "$desc"
    FAIL=$((FAIL + 1))
  fi
}

_assert_grep()     { _assert "$1" grep -qE "$3" "$2"; }
_assert_not_grep() { _assert "$1" bash -c "! grep -qE '$3' '$2'"; }

echo "════════════════════════════════════════════"
echo " KURA · Architecture Document Validation (T07)"
echo "════════════════════════════════════════════"

# ── 1. Arquivo existe ─────────────────────────────────────────────────────────
echo ""
echo "── 1. File existence ──"
_assert "docs/architecture.md existe" test -f "$DOC"

# ── 2. Cabeçalho e metadados ──────────────────────────────────────────────────
echo ""
echo "── 2. Cabeçalho ──"
_assert_grep "Título KURA presente"          "$DOC" "KURA"
_assert_grep "Spring Boot 3 mencionado"      "$DOC" "Spring Boot 3"
_assert_grep "Java 21 mencionado"            "$DOC" "Java 21"
_assert_grep "Oracle 19c mencionado"         "$DOC" "Oracle 19c"
_assert_grep "Flyway mencionado no header"   "$DOC" "Flyway"

# ── 3. Seção 1 — Visão Geral ──────────────────────────────────────────────────
echo ""
echo "── 3. Seção 1 — Visão Geral ──"
_assert_grep "Seção 1 presente"              "$DOC" "^## 1\."
_assert_grep "Topologia mencionada"          "$DOC" "[Tt]opologia|[Tt]opology"
_assert_grep "Porta 8081 mencionada"         "$DOC" "8081"
_assert_grep "oracle.fiap.com.br"            "$DOC" "oracle\.fiap\.com\.br"
_assert_grep "context-path /api"             "$DOC" "/api"
_assert_grep "H2 dev mode Oracle"            "$DOC" "MODE=Oracle|H2.*Oracle|Oracle.*H2"
_assert_grep "Estrutura de pacotes"          "$DOC" "pacotes|packages"
_assert_grep "Bounded contexts no pkg"       "$DOC" "auth.*onboarding|onboarding.*auth"

# ── 4. Seção 2 — Bounded Contexts e Ownership ────────────────────────────────
echo ""
echo "── 4. Seção 2 — Bounded Contexts ──"
_assert_grep "Seção 2 presente"              "$DOC" "^## 2\."
_assert_grep "Tabela ownership presente"     "$DOC" "Owner|Ownership|ownership"
_assert_grep "@Immutable mencionado"         "$DOC" "@Immutable"
_assert_grep "CONTA_TUTOR Java owns"         "$DOC" "CONTA_TUTOR.*Java|Java.*CONTA_TUTOR"
_assert_grep "AGENDAMENTO shared-write"      "$DOC" "AGENDAMENTO.*[Ss]hared|[Ss]hared.*AGENDAMENTO"
_assert_grep "INVITE_TUTOR .NET"             "$DOC" "INVITE_TUTOR"
_assert_grep "CONSENTIMENTO INSERT-only"     "$DOC" "CONSENTIMENTO"
_assert_grep "IDEMPOTENCY_KEY Java"          "$DOC" "IDEMPOTENCY_KEY"
_assert_grep "JpaRepository ausente (mínimo)" "$DOC" "Repository|JpaRepository"

# ── 5. Seção 3 — Fluxo Invite-Based ──────────────────────────────────────────
echo ""
echo "── 5. Seção 3 — Fluxo Invite-Based ──"
_assert_grep "Seção 3 presente"              "$DOC" "^## 3\."
_assert_grep "Sequence diagram (ASCII) presente" "$DOC" "POST /tutores"
_assert_grep "Endpoint primário /onboarding/register-invite documentado" "$DOC" "POST /onboarding/register-invite"
_assert_grep "Endpoint register-invite"      "$DOC" "register-invite"
# TASK-82: o assert acima ("Sequence diagram presente") era um OR onde o lado esquerdo
# ("POST /tutores", a arte ASCII da seção 3.1) sempre bate sozinho — trocar o lado direito
# de /auth para /onboarding só atualizava a string no teste, sem dar poder de detecção real.
# Separado em dois asserts independentes (acima) + os dois abaixo, que travam de verdade:
# (1) a doc tem que declarar a remoção do alias explicitamente, e (2) não pode voltar a
# descrever o alias como endpoint vigente com a frase que o fazia antes da TASK-82.
_assert_grep     "Remoção do alias documentada (TASK-82)" "$DOC" "TASK-82.*alias.*removid|alias.*removid.*TASK-82|removid.*TASK-82.*alias"
_assert_not_grep "Alias legado NÃO documentado como vigente (regressão pré-TASK-82)" "$DOC" "auth/register-invite.*segue respondendo"
_assert_grep "Token invite exemplo"          "$DOC" "550e8400"
_assert_grep "Status 201 documentado"        "$DOC" "201"
_assert_grep "Status 409 documentado"        "$DOC" "409"
_assert_grep "Status 410 documentado"        "$DOC" "410"
_assert_grep "Status 422 documentado"        "$DOC" "422"
_assert_grep "Defense-in-depth mencionado"   "$DOC" "[Dd]efense.in.[Dd]epth|[Dd]efesa.*camada"
_assert_grep "UK_CONTA_INVITE_USED"          "$DOC" "UK_CONTA_INVITE_USED"
_assert_grep "ST_UTILIZADO mencionado"       "$DOC" "ST_UTILIZADO"
_assert_grep "DataIntegrityViolation"        "$DOC" "DataIntegrityViolationException"

# ── 6. Seção 4 — Concorrência / Optimistic Locking ───────────────────────────
echo ""
echo "── 6. Seção 4 — Concorrência ──"
_assert_grep "Seção 4 presente"              "$DOC" "^## 4\."
_assert_grep "@Version mencionado"           "$DOC" "@Version"
_assert_grep "NR_VERSION mencionado"         "$DOC" "NR_VERSION"
_assert_grep "OptimisticLockingFailureException" "$DOC" "OptimisticLockingFailureException"
_assert_grep "HTTP 409 para lock"            "$DOC" "409.*[Cc]onflict|[Cc]onflict.*409"
_assert_grep "AND NR_VERSION no SQL"         "$DOC" "NR_VERSION|NR_VERSION = \?"
_assert_grep ".NET PATCH status"             "$DOC" "PATCH.*[Ss]tatus|[Ss]tatus.*PATCH"
_assert_grep "Por que não pessimistic"       "$DOC" "[Pp]essimistic|[Dd]eadlock|[Dd]eadlocks"

# ── 7. Seção 5 — Idempotência ─────────────────────────────────────────────────
echo ""
echo "── 7. Seção 5 — Idempotência ──"
_assert_grep "Seção 5 presente"              "$DOC" "^## 5\."
_assert_grep "Idempotency-Key header"        "$DOC" "Idempotency-Key|Idempotency.Key"
_assert_grep "DS_KEY mencionado"             "$DOC" "DS_KEY"
_assert_grep "NM_RESOURCE mencionado"        "$DOC" "NM_RESOURCE"
_assert_grep "TTL 24h mencionado"            "$DOC" "TTL.*24h|24h.*TTL|24 horas"
_assert_grep "DT_EXPIRACAO presente"         "$DOC" "DT_EXPIRACAO"
_assert_grep "Rollback transação mencionado" "$DOC" "rollback|[Tt]ransação|[Tt]ransaction"
_assert_grep "Sem Redis justificado"         "$DOC" "[Rr]edis|cache.*distribu|distribu.*cache"

# ── 8. Seção 6 — Cache ───────────────────────────────────────────────────────
echo ""
echo "── 8. Seção 6 — Cache ──"
_assert_grep "Seção 6 presente"              "$DOC" "^## 6\."
_assert_grep "ESPECIE cacheada"              "$DOC" "[Ee]specie|ESPECIE"
_assert_grep "RACA cacheada"                 "$DOC" "[Rr]aca|RACA"
_assert_grep "Caffeine mencionado"           "$DOC" "[Cc]affeine"
_assert_grep "Tutor NAO cacheado"            "$DOC" "[Tt]utor.*[Nn]ão|[Nn]ão.*[Tt]utor|[Tt]utor.*NOT|NOT.*[Tt]utor"
_assert_grep "Pet NAO cacheado"              "$DOC" "Pet.*[Mm]utável|Pet.*cach|não.*cach.*[Pp]et"
_assert_grep "@Cacheable mencionado"         "$DOC" "@Cacheable"
_assert_grep "Justificativa Caffeine vs Redis" "$DOC" "Redis|distribu"

# ── 9. Seção 7 — Autenticação JWT ────────────────────────────────────────────
echo ""
echo "── 9. Seção 7 — Autenticação ──"
_assert_grep "Seção 7 presente"              "$DOC" "^## 7\."
_assert_grep "Access token 15min"            "$DOC" "15.min|15 minutos|access.*15|15.*access"
_assert_grep "Refresh token 7d"              "$DOC" "7.d|7 dias|refresh.*7|7.*refresh"
_assert_grep "BCrypt refresh hash"           "$DOC" "BCrypt|bcrypt"
_assert_grep "Refresh rotation explicado"    "$DOC" "[Rr]otation|[Rr]otação"
_assert_grep "DS_REFRESH_TOKEN_HASH"         "$DOC" "DS_REFRESH_TOKEN_HASH"
_assert_grep "JWT_SECRET env var"            "$DOC" "JWT_SECRET"
_assert_grep "64 bytes minimo"               "$DOC" "64 bytes|64-bytes"
_assert_grep "POST /auth/login"              "$DOC" "/auth/login"
_assert_grep "POST /auth/refresh"            "$DOC" "/auth/refresh"
_assert_grep "POST /auth/logout"             "$DOC" "/auth/logout"

# ── 10. Seção 8 — Tratamento de Erros ────────────────────────────────────────
echo ""
echo "── 10. Seção 8 — Erros RFC 7807 ──"
_assert_grep "Seção 8 presente"              "$DOC" "^## 8\."
_assert_grep "RFC 7807 mencionado"           "$DOC" "RFC 7807"
_assert_grep "Campo timestamp no ApiError"   "$DOC" "timestamp"
_assert_grep "Campo correlationId"           "$DOC" "correlationId"
_assert_grep "Campo path presente"           "$DOC" '"path"'
_assert_grep "404 mapeado"                   "$DOC" "404"
_assert_grep "410 Gone mapeado"              "$DOC" "410.*Gone|Gone.*410"
_assert_grep "422 mapeado"                   "$DOC" "422"
_assert_grep "423 Locked mapeado"            "$DOC" "423.*[Ll]ocked|[Ll]ocked.*423"
_assert_grep "500 fallback mapeado"          "$DOC" "500"
_assert_grep "CorrelationId filter"          "$DOC" "CorrelationId|Correlation.Id"

# ── 11. Seção 9 — Migrations V1-V5 ───────────────────────────────────────────
echo ""
echo "── 11. Seção 9 — Migrations ──"
_assert_grep "Seção 9 presente"              "$DOC" "^## 9\."
_assert_grep "V1__initial_schema.sql"        "$DOC" "V1__initial_schema"
_assert_grep "V2__concurrency_idempotency"   "$DOC" "V2__concurrency_idempotency"
_assert_grep "V3__invite_based"              "$DOC" "V3__invite_based"
_assert_grep "V4__lgpd_evidencia"            "$DOC" "V4__lgpd_evidencia"
_assert_grep "V5__agendamento_observacoes"   "$DOC" "V5__agendamento_observacoes"
_assert_grep "afterMigrate seeds_dev"        "$DOC" "afterMigrate|seeds_dev"
_assert_grep "Profile dev inclui callback"   "$DOC" "callback"
_assert_grep "Profile prod sem callback"     "$DOC" "prod.*migration|migration.*prod"
_assert_grep "baseline-on-migrate"           "$DOC" "baseline.on.migrate|baseline_on_migrate"
_assert_grep "ddl-auto validate prod"        "$DOC" "validate"
_assert_grep "V4 pendente Felipe"            "$DOC" "[Pp]endente|[Pp]laceholder|Felipe"

# ── 12. Seção 10 — Como Rodar Localmente ─────────────────────────────────────
echo ""
echo "── 12. Seção 10 — Como Rodar ──"
_assert_grep "Seção 10 presente"             "$DOC" "^## 10\."
_assert_grep "mvnw spring-boot:run"          "$DOC" "spring-boot:run|mvnw"
_assert_grep "Profile dev mencionado"        "$DOC" "profiles=dev|profiles.*dev"
_assert_grep "swagger-ui.html"               "$DOC" "swagger-ui"
_assert_grep "H2 Console URL"                "$DOC" "h2-console"
_assert_grep "docker compose up"             "$DOC" "docker compose|docker-compose"
_assert_grep "docker-compose.override"       "$DOC" "docker-compose.override"
_assert_grep "actuator/health"               "$DOC" "actuator/health"
_assert_grep "openssl ou equivalente JWT"    "$DOC" "openssl|JWT_SECRET"
_assert_grep "test_migrations.sh"            "$DOC" "test_migrations"
_assert_grep "test_docker_setup.sh"          "$DOC" "test_docker_setup"

# ── 13. Decisões Pendentes ────────────────────────────────────────────────────
echo ""
echo "── 13. Decisões Pendentes documentadas ──"
_assert_grep "LGPD decisão pendente"         "$DOC" "LGPD|lgpd"
_assert_grep "Felipe Ferrete mencionado"     "$DOC" "Felipe Ferrete|Felipe"
_assert_grep "ANPD mencionada"               "$DOC" "ANPD"
_assert_grep "Art. 7 LGPD"                   "$DOC" "art.*7|7.*art|LGPD"

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════"
printf " Resultado: %d passed, %d failed\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
[[ "$FAIL" -eq 0 ]]
