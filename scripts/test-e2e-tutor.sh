#!/usr/bin/env bash
# TASK-08 (INT-01) — Smoke test E2E da superfície /api/v1/tutor/** e /api/v1/auth/**
# contra o Java em perfil dev (H2 in-memory, sem Oracle externo).
#
# Uso: bash scripts/test-e2e-tutor.sh
# Sai != 0 se qualquer asserção de status HTTP falhar.
#
# Não depende do mobile-tutor-rn rodando. Usa os dados do seed dev
# (afterMigrate__seeds_dev.sql): ID_TUTOR=1, ID_PET=1, ID_CLINICA=1,
# invite token 550e8400-e29b-41d4-a716-446655440000, e-mail felipe@clyvo.vet
# (herdado de TUTOR.DS_EMAIL, tabela .NET-owned/@Immutable).

set -uo pipefail

BASE_URL="http://localhost:8081/api"
LOG_FILE="$(mktemp)"
FAILURES=0

log()  { echo "[test-e2e-tutor] $*"; }
fail() { echo "[FAIL] $*"; FAILURES=$((FAILURES + 1)); }
pass() { echo "[ OK ] $*"; }

assert_status() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    pass "$desc → HTTP $actual"
  else
    fail "$desc → esperado HTTP $expected, obtido HTTP $actual"
  fi
}

# ─── Sobe o Java em background (perfil dev, H2) ────────────────────────────────
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ -x "$REPO_ROOT/mvnw" ]; then
  MVN_CMD="$REPO_ROOT/mvnw"
else
  MVN_CMD="mvn"
fi

log "Subindo backend-tutor-java (dev/H2) em background (usando '$MVN_CMD')..."
(cd "$REPO_ROOT" && "$MVN_CMD" -q spring-boot:run -Dspring-boot.run.profiles=dev >"$LOG_FILE" 2>&1) &
APP_PID=$!

cleanup() {
  log "Encerrando o processo Maven/Spring (PID $APP_PID)..."
  kill "$APP_PID" 2>/dev/null
  wait "$APP_PID" 2>/dev/null
}
trap cleanup EXIT

log "Aguardando health check (até 120s)..."
UP=0
for i in $(seq 1 60); do
  if curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    UP=1
    break
  fi
  sleep 2
done

if [ "$UP" -ne 1 ]; then
  fail "backend não respondeu em $BASE_URL/actuator/health após 120s — ver $LOG_FILE"
  cat "$LOG_FILE"
  exit 1
fi
pass "health check respondeu"

# ─── 1. Registro via invite (cria ContaTutor + retorna JWT) ───────────────────
log "POST /v1/auth/register-invite (BFF alias)"
REGISTER_BODY='{"token":"550e8400-e29b-41d4-a716-446655440000","senha":"Senha@123","aceites":[]}'
REGISTER_RESP="$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/v1/auth/register-invite" \
  -H 'Content-Type: application/json' -d "$REGISTER_BODY")"
REGISTER_STATUS="$(echo "$REGISTER_RESP" | tail -n1)"
REGISTER_JSON="$(echo "$REGISTER_RESP" | sed '$d')"

# 201 na primeira execução; 409 (convite já usado) em reruns locais — ambos aceitáveis aqui.
if [ "$REGISTER_STATUS" = "201" ]; then
  pass "POST /v1/auth/register-invite → HTTP 201 (conta criada)"
  ACCESS_TOKEN="$(echo "$REGISTER_JSON" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)"
elif [ "$REGISTER_STATUS" = "409" ]; then
  pass "POST /v1/auth/register-invite → HTTP 409 (convite já utilizado — rerun local, login abaixo cobre o fluxo)"
  ACCESS_TOKEN=""
else
  fail "POST /v1/auth/register-invite → esperado 201 ou 409, obtido HTTP $REGISTER_STATUS ($REGISTER_JSON)"
  ACCESS_TOKEN=""
fi

# ─── 2. Login (payload real do Java: email/senha) ─────────────────────────────
# NOTA (contrato divergente — ver docs/INT-01-contract-map.md): o mobile-tutor-rn
# envia {dsEmail, dsSenha}; o Java espera {email, senha}. Aqui usamos o contrato
# REAL do backend para validar o backend em si; o script também prova a divergência.
log "POST /v1/auth/login com payload correto do Java ({email, senha})"
LOGIN_RESP="$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"felipe@clyvo.vet","senha":"Senha@123"}')"
LOGIN_STATUS="$(echo "$LOGIN_RESP" | tail -n1)"
LOGIN_JSON="$(echo "$LOGIN_RESP" | sed '$d')"
assert_status "POST /v1/auth/login (payload correto)" "200" "$LOGIN_STATUS"
if [ "$LOGIN_STATUS" = "200" ]; then
  ACCESS_TOKEN="$(echo "$LOGIN_JSON" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)"
fi

log "POST /v1/auth/login com payload do mobile-tutor-rn ({dsEmail, dsSenha}) — prova a divergência D-2/contract-map"
MOBILE_LOGIN_STATUS="$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"dsEmail":"felipe@clyvo.vet","dsSenha":"Senha@123"}')"
assert_status "POST /v1/auth/login (payload mobile — esperado falhar, campos não batem)" "400" "$MOBILE_LOGIN_STATUS"

if [ -z "${ACCESS_TOKEN:-}" ]; then
  fail "Nenhum accessToken obtido (nem por register-invite nem por login) — abortando testes autenticados"
  exit 1
fi
AUTH_HEADER="Authorization: Bearer $ACCESS_TOKEN"

# ─── 3. GET /v1/tutor/pets ──────────────────────────────────────────────────────
log "GET /v1/tutor/pets"
STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/v1/tutor/pets" -H "$AUTH_HEADER")"
assert_status "GET /v1/tutor/pets" "200" "$STATUS"

# ─── 4. GET /v1/tutor/pets/{id} — stub 501 documentado ────────────────────────
log "GET /v1/tutor/pets/1 (stub — pendente INT-01)"
STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/v1/tutor/pets/1" -H "$AUTH_HEADER")"
assert_status "GET /v1/tutor/pets/1 (stub)" "501" "$STATUS"

# ─── 5. GET /v1/tutor/pets/{id}/timeline — stub 501 documentado ───────────────
log "GET /v1/tutor/pets/1/timeline (stub — pendente INT-01)"
STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/v1/tutor/pets/1/timeline" -H "$AUTH_HEADER")"
assert_status "GET /v1/tutor/pets/1/timeline (stub)" "501" "$STATUS"

# ─── 6. POST /v1/tutor/agendamentos ────────────────────────────────────────────
log "POST /v1/tutor/agendamentos"
FUTURE_DATE="$(date -u -d '+7 days' +'%Y-%m-%dT10:00:00' 2>/dev/null || date -u -v+7d +'%Y-%m-%dT10:00:00')"
AGENDAMENTO_BODY="{\"idPet\":1,\"idClinica\":1,\"dtAgendamento\":\"$FUTURE_DATE\",\"tipo\":\"CONSULTA\",\"duracaoMinutos\":30,\"observacoes\":\"smoke test\"}"
AG_RESP="$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/v1/tutor/agendamentos" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' -d "$AGENDAMENTO_BODY")"
AG_STATUS="$(echo "$AG_RESP" | tail -n1)"
AG_JSON="$(echo "$AG_RESP" | sed '$d')"
assert_status "POST /v1/tutor/agendamentos" "201" "$AG_STATUS"
AG_ID="$(echo "$AG_JSON" | grep -o '"idAgendamento":[0-9]*' | grep -o '[0-9]*')"

# ─── 7. GET /v1/tutor/agendamentos ─────────────────────────────────────────────
log "GET /v1/tutor/agendamentos"
STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/v1/tutor/agendamentos" -H "$AUTH_HEADER")"
assert_status "GET /v1/tutor/agendamentos" "200" "$STATUS"

# ─── 8. DELETE /v1/tutor/agendamentos/{id} ─────────────────────────────────────
if [ -n "${AG_ID:-}" ]; then
  log "DELETE /v1/tutor/agendamentos/$AG_ID"
  STATUS="$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE_URL/v1/tutor/agendamentos/$AG_ID" -H "$AUTH_HEADER")"
  assert_status "DELETE /v1/tutor/agendamentos/$AG_ID" "204" "$STATUS"
else
  fail "Não foi possível extrair idAgendamento da resposta do POST — pulando DELETE"
fi

# ─── 9. PATCH /v1/tutor/me/push-token ──────────────────────────────────────────
# NOTA (contrato divergente — ver docs/INT-01-contract-map.md): o campo real no
# Java é "dsPlatforma" (typo — falta o 'a' entre 't' e 'f', PushTokenRequest.java).
# O mobile-tutor-rn envia "dsPlatform" (inglês). Nenhuma das duas grafias é igual
# à outra. Testamos com o payload real do Java (deve passar) e o do mobile (falha).
log "PATCH /v1/tutor/me/push-token com payload correto do Java ({dsPushToken, dsPlatforma})"
STATUS="$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE_URL/v1/tutor/me/push-token" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"dsPushToken":"ExponentPushToken[smoke-test-token]","dsPlatforma":"android"}')"
assert_status "PATCH /v1/tutor/me/push-token (payload correto)" "204" "$STATUS"

log "PATCH /v1/tutor/me/push-token com payload do mobile-tutor-rn ({dsPlatform}) — prova a divergência"
MOBILE_PUSH_STATUS="$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE_URL/v1/tutor/me/push-token" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"dsPushToken":"ExponentPushToken[smoke-test-token]","dsPlatform":"android"}')"
assert_status "PATCH /v1/tutor/me/push-token (payload mobile — esperado falhar)" "400" "$MOBILE_PUSH_STATUS"

# ─── Resumo ─────────────────────────────────────────────────────────────────────
echo ""
if [ "$FAILURES" -eq 0 ]; then
  log "Todos os asserts passaram."
  exit 0
else
  log "$FAILURES asserção(ões) falharam."
  exit 1
fi
