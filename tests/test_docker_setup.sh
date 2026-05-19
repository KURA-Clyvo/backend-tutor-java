#!/usr/bin/env bash
# ─── Test Suite: Docker Setup Validation (T03) ────────────────────────────────
# Valida Dockerfile multi-stage, .dockerignore, docker-compose.yml e override.
# Uso: bash tests/test_docker_setup.sh
# Requer Docker apenas para o Test 6 (build real); os demais rodam sem Docker.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
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

_assert_grep() {
  local desc="$1"
  local file="$2"
  local pattern="$3"
  _assert "$desc" grep -qE "$pattern" "$file"
}

_assert_not_grep() {
  local desc="$1"
  local file="$2"
  local pattern="$3"
  _assert "$desc" bash -c "! grep -qiE '$pattern' '$file'"
}

DF="$ROOT_DIR/Dockerfile"
COMPOSE="$ROOT_DIR/docker-compose.yml"
IGNORE="$ROOT_DIR/.dockerignore"
OVERRIDE_EX="$ROOT_DIR/docker-compose.override.yml.example"

echo "════════════════════════════════════════════"
echo " KURA · Docker Setup Validation (T03)"
echo "════════════════════════════════════════════"

# ── 1. Existência dos arquivos ────────────────────────────────────────────────
echo ""
echo "── 1. File existence ──"
_assert "Dockerfile presente"                      test -f "$DF"
_assert ".dockerignore presente"                   test -f "$IGNORE"
_assert "docker-compose.yml presente"              test -f "$COMPOSE"
_assert "docker-compose.override.yml.example"      test -f "$OVERRIDE_EX"

# ── 2. Dockerfile: builder stage ─────────────────────────────────────────────
echo ""
echo "── 2. Dockerfile — builder stage ──"
_assert_grep "Builder usa maven:3.9-eclipse-temurin-21" \
    "$DF" "FROM maven:3\.9-eclipse-temurin-21 AS builder"

_assert_grep "COPY pom.xml presente" "$DF" "COPY pom\.xml"

_assert_grep "dependency:go-offline presente" "$DF" "dependency:go-offline"

_assert_grep "COPY src presente" "$DF" "COPY src"

# Garante que pom.xml é copiado antes de src (cache layer ordering)
_assert "pom.xml copiado ANTES de src/ (cache ordering)" bash -c \
    "awk '/COPY pom\.xml/{p=NR} /COPY src/{s=NR} END{exit !(p>0 && s>0 && p<s)}' '$DF'"

_assert_grep "mvn package -DskipTests" "$DF" "mvn package.*-DskipTests"

# ── 3. Dockerfile: runtime stage ─────────────────────────────────────────────
echo ""
echo "── 3. Dockerfile — runtime stage ──"
_assert_grep "Runtime usa eclipse-temurin:21-jre-jammy" \
    "$DF" "FROM eclipse-temurin:21-jre-jammy AS runtime"

_assert_grep "Usuário spring uid=1000" "$DF" "\-\-uid 1000"

_assert_grep "groupadd spring gid=1000" "$DF" "\-\-gid 1000"

_assert_grep "USER spring definido" "$DF" "^USER spring"

_assert_grep "EXPOSE 8081" "$DF" "EXPOSE 8081"

_assert_grep "JVM flag MaxRAMPercentage=75.0" "$DF" "MaxRAMPercentage=75\.0"

_assert_grep "JVM flag UseG1GC" "$DF" "UseG1GC"

_assert_grep "HEALTHCHECK configurado" "$DF" "HEALTHCHECK"

_assert_grep "Healthcheck aponta para /actuator/health" "$DF" "/actuator/health"

_assert_grep "COPY usa --chown spring:spring" "$DF" "\-\-chown=spring:spring"

_assert_not_grep "SEM RUN chown separado (desnecessário com --chown)" \
    "$DF" "^RUN chown"

# ── 4. docker-compose.yml ────────────────────────────────────────────────────
echo ""
echo "── 4. docker-compose.yml ──"
_assert_grep "Porta 8081:8081 mapeada" "$COMPOSE" "8081:8081"

_assert_grep "SPRING_PROFILES_ACTIVE: prod" "$COMPOSE" "SPRING_PROFILES_ACTIVE.*prod"

_assert_grep "DB_URL via env" "$COMPOSE" "DB_URL"

_assert_grep "JWT_SECRET via env" "$COMPOSE" "JWT_SECRET"

_assert_grep "Network kura-net definida" "$COMPOSE" "kura-net"

_assert_grep "Driver bridge declarado" "$COMPOSE" "driver: bridge"

_assert_not_grep "SEM Oracle local (sem serviço oracle/xe)" \
    "$COMPOSE" "image:.*oracle|oracle.*:.*image"

# ── 5. .dockerignore ─────────────────────────────────────────────────────────
echo ""
echo "── 5. .dockerignore ──"
_assert_grep "target/ ignorado" "$IGNORE" "target/"

_assert_grep ".git ignorado"   "$IGNORE" "\.git"

_assert_grep ".env* ignorado"  "$IGNORE" "\.env"

_assert_grep ".idea ignorado"  "$IGNORE" "\.idea"

# ── 6. docker-compose.override.yml.example ───────────────────────────────────
echo ""
echo "── 6. docker-compose.override.yml.example ──"
_assert_grep "Oracle FIAP URL presente" "$OVERRIDE_EX" "oracle\.fiap\.com\.br"

_assert_grep "DB_USERNAME placeholder"  "$OVERRIDE_EX" "DB_USERNAME"

_assert_grep "DB_PASSWORD placeholder"  "$OVERRIDE_EX" "DB_PASSWORD"

_assert_grep "JWT_SECRET placeholder"   "$OVERRIDE_EX" "JWT_SECRET"

_assert_grep "JWT_ACCESS_EXPIRATION configurável" "$OVERRIDE_EX" "JWT_ACCESS_EXPIRATION_MINUTES"

# ── 7. Build real (requer Docker) ────────────────────────────────────────────
echo ""
echo "── 7. Validação com Docker (opcional) ──"
if ! command -v docker &>/dev/null; then
  echo "  SKIP: docker não encontrado no PATH"
elif ! docker info &>/dev/null 2>&1; then
  echo "  SKIP: Docker daemon não está rodando"
else
  _assert "docker compose config --quiet valida YAML" \
      docker compose -f "$COMPOSE" config --quiet

  if [[ "${DOCKER_BUILD:-false}" == "true" ]]; then
    echo "  INFO: DOCKER_BUILD=true — executando build real..."
    _assert "docker compose build conclui sem erro" \
        docker compose -f "$COMPOSE" build

    SPRING_UID=$(docker run --rm --entrypoint id kura-backend-tutor:latest \
        | grep -oP 'uid=\K[0-9]+')
    _assert "UID do usuário spring é 1000 (uid=$SPRING_UID)" \
        test "$SPRING_UID" -eq 1000
  else
    echo "  SKIP: build real (defina DOCKER_BUILD=true para ativar)"
  fi
fi

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════"
printf " Resultado: %d passed, %d failed\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
[[ "$FAIL" -eq 0 ]]
