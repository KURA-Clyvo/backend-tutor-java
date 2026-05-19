#!/usr/bin/env bash
# ─── Test Suite: Flyway Migration Validation (T04) ────────────────────────────
# Valida estrutura, nomes, conteúdo e consistência das 5 migrations + callback.
# Uso: bash tests/test_migrations.sh
# Não requer banco — apenas análise estática dos arquivos SQL.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MDIR="$ROOT_DIR/src/main/resources/db/migration"
CDIR="$ROOT_DIR/src/main/resources/db/callback"
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

V1="$MDIR/V1__initial_schema.sql"
V2="$MDIR/V2__concurrency_idempotency.sql"
V3="$MDIR/V3__invite_based_compatibility.sql"
V4="$MDIR/V4__lgpd_evidencia_consentimento.sql"
V5="$MDIR/V5__agendamento_observacoes.sql"
CB="$CDIR/afterMigrate__seeds_dev.sql"

echo "════════════════════════════════════════════"
echo " KURA · Migration Validation (T04)"
echo "════════════════════════════════════════════"

# ── 1. Arquivos existem com nomes EXATOS ─────────────────────────────────────
echo ""
echo "── 1. File existence (nomes exatos) ──"
_assert "V1__initial_schema.sql"               test -f "$V1"
_assert "V2__concurrency_idempotency.sql"       test -f "$V2"
_assert "V3__invite_based_compatibility.sql"    test -f "$V3"
_assert "V4__lgpd_evidencia_consentimento.sql"  test -f "$V4"
_assert "V5__agendamento_observacoes.sql"       test -f "$V5"
_assert "afterMigrate__seeds_dev.sql"           test -f "$CB"

# ── 2. V1 — tabelas obrigatórias ─────────────────────────────────────────────
echo ""
echo "── 2. V1 — tabelas criadas ──"
_assert_grep "CLINICA"          "$V1" "CREATE TABLE CLINICA"
_assert_grep "ESPECIE"          "$V1" "CREATE TABLE ESPECIE"
_assert_grep "TIPO_EVENTO"      "$V1" "CREATE TABLE TIPO_EVENTO"
_assert_grep "RACA"             "$V1" "CREATE TABLE RACA"
_assert_grep "VETERINARIO"      "$V1" "CREATE TABLE VETERINARIO"
_assert_grep "TUTOR"            "$V1" "CREATE TABLE TUTOR"
_assert_grep "PET"              "$V1" "CREATE TABLE PET"
_assert_grep "TUTOR_PET"        "$V1" "CREATE TABLE TUTOR_PET"
_assert_grep "INVITE_TUTOR"     "$V1" "CREATE TABLE INVITE_TUTOR"
_assert_grep "CONTA_TUTOR"      "$V1" "CREATE TABLE CONTA_TUTOR"
_assert_grep "CONSENTIMENTO"    "$V1" "CREATE TABLE CONSENTIMENTO"
_assert_grep "AGENDAMENTO"      "$V1" "CREATE TABLE AGENDAMENTO"
_assert_grep "IDEMPOTENCY_KEY"  "$V1" "CREATE TABLE IDEMPOTENCY_KEY"

# ── 3. V1 — constraints e regras de negócio ───────────────────────────────────
echo ""
echo "── 3. V1 — constraints ──"
_assert_grep "NR_CPF VARCHAR2(11)"          "$V1" "NR_CPF.*VARCHAR2\(11\)"
_assert_grep "SG_PORTE CHECK (P,M,G)"       "$V1" "SG_PORTE.*IN.*'P'.*'M'.*'G'"
_assert_grep "SG_SEXO CHECK (M,F)"          "$V1" "SG_SEXO.*IN.*'M'.*'F'"
_assert_grep "CONSENTIMENTO DS_TIPO CHECK"  "$V1" "TELEORIENTACAO.*LEMBRETES"
_assert_grep "INVITE_TUTOR DS_CANAL CHECK"  "$V1" "WHATSAPP.*EMAIL.*SMS"
_assert_grep "NR_VERSION DEFAULT 0"         "$V1" "NR_VERSION.*DEFAULT 0"
_assert_grep "UK_CONTA_TUTOR_TUTOR (ID_TUTOR)" "$V1" "UK_CONTA_TUTOR_TUTOR"
_assert_grep "Sequences criadas"            "$V1" "CREATE SEQUENCE SEQ_CONTA_TUTOR"
_assert_grep "View VW_TIMELINE_PET"         "$V1" "CREATE.*VIEW VW_TIMELINE_PET"
_assert_grep "View VW_VACINAS_VENCENDO"     "$V1" "CREATE.*VIEW VW_VACINAS_VENCENDO"

# ── 4. V1 — declaração CONSTRAINT UK_CONTA_INVITE_USED NÃO está em V1 ─────────
echo ""
echo "── 4. V1 — UK_CONTA_INVITE_USED ausente (V3 adiciona) ──"
_assert_not_grep "CONSTRAINT UK_CONTA_INVITE_USED não em V1" \
    "$V1" "CONSTRAINT UK_CONTA_INVITE_USED"

# ── 5. V2 — índice de limpeza ─────────────────────────────────────────────────
echo ""
echo "── 5. V2 — IDX_IDEMPOT_CRIACAO ──"
_assert_grep "IDX_IDEMPOT_CRIACAO criado"        "$V2" "IDX_IDEMPOT_CRIACAO"
_assert_grep "Sobre IDEMPOTENCY_KEY"             "$V2" "IDEMPOTENCY_KEY"

# ── 6. V3 — UK e índice de token ─────────────────────────────────────────────
echo ""
echo "── 6. V3 — UK_CONTA_INVITE_USED + IDX_INVITE_TOKEN_ATIVO ──"
_assert_grep "UK_CONTA_INVITE_USED add"          "$V3" "UK_CONTA_INVITE_USED"
_assert_grep "IDX_INVITE_TOKEN_ATIVO criado"     "$V3" "IDX_INVITE_TOKEN_ATIVO"
_assert_grep "V3 referencia INVITE_TUTOR"        "$V3" "INVITE_TUTOR"
_assert_grep "V3 referencia CONTA_TUTOR"         "$V3" "CONTA_TUTOR"
_assert_not_grep "V3 sem PL/SQL executável"      "$V3" "^DECLARE"
_assert_not_grep "V3 sem BEGIN executável"       "$V3" "^BEGIN"

# ── 7. V4 — placeholder correto ──────────────────────────────────────────────
echo ""
echo "── 7. V4 — placeholder LGPD ──"
_assert_grep "V4 tem SELECT 1 FROM DUAL"         "$V4" "SELECT 1 FROM DUAL"
_assert_grep "V4 menciona Felipe"                "$V4" "Felipe"
_assert_grep "V4 menciona LGPD"                  "$V4" "LGPD"
_assert_not_grep "V4 sem ALTER TABLE real"       "$V4" "^ALTER TABLE CONSENTIMENTO ADD"

# ── 8. V5 — campos do agendamento ────────────────────────────────────────────
echo ""
echo "── 8. V5 — campos AGENDAMENTO ──"
_assert_grep "DS_OBSERVACOES adicionado"         "$V5" "DS_OBSERVACOES"
_assert_grep "DT_CRIACAO adicionado"             "$V5" "DT_CRIACAO"
_assert_grep "DT_CONFIRMACAO adicionado"         "$V5" "DT_CONFIRMACAO"
_assert_grep "DT_CANCELAMENTO adicionado"        "$V5" "DT_CANCELAMENTO"
_assert_grep "DS_MOTIVO_CANCEL adicionado"       "$V5" "DS_MOTIVO_CANCEL"
_assert_grep "ID_EVENTO_GERADO adicionado"       "$V5" "ID_EVENTO_GERADO"
_assert_grep "FK para EVENTO_CLINICO"            "$V5" "EVENTO_CLINICO"
_assert_grep "IDX_AGEND_EVENTO criado"           "$V5" "IDX_AGEND_EVENTO"

# ── 9. Callback — seeds obrigatórios ─────────────────────────────────────────
echo ""
echo "── 9. afterMigrate__seeds_dev.sql — seeds ──"
_assert_grep "CLINICA seed"                      "$CB" "Clyvo Vet"
_assert_grep "ESPECIE Cao"                       "$CB" "'Cao'"
_assert_grep "ESPECIE Gato"                      "$CB" "'Gato'"
_assert_grep "TIPO_EVENTO CONSULTA"              "$CB" "'CONSULTA'"
_assert_grep "TIPO_EVENTO TELEORIENTACAO"        "$CB" "'TELEORIENTACAO'"
_assert_grep "TIPO_EVENTO VACINA"                "$CB" "'VACINA'"
_assert_grep "RACA Labrador"                     "$CB" "'Labrador'"
_assert_grep "RACA Poodle"                       "$CB" "'Poodle'"
_assert_grep "RACA Siames"                       "$CB" "'Siames'"
_assert_grep "RACA SRD-felino"                   "$CB" "SRD-felino"
_assert_grep "VETERINARIO seed"                  "$CB" "MERGE INTO VETERINARIO"
_assert_grep "TUTOR seed"                        "$CB" "'Felipe Ferrete'"
_assert_grep "NR_CPF 11 dígitos"                 "$CB" "'12345678900'"
_assert_grep "ST_AVISO_PRIVACIDADE='S'"          "$CB" "ST_AVISO_PRIVACIDADE.*'S'"
_assert_grep "INVITE_TUTOR token seed"           "$CB" "550e8400-e29b-41d4-a716-446655440000"
_assert_grep "INVITE_TUTOR ST_UTILIZADO='N'"     "$CB" "ST_UTILIZADO.*'N'"
_assert_grep "INVITE_TUTOR ST_ATIVO='S'"         "$CB" "ST_ATIVO.*'S'"
_assert_grep "INVITE_TUTOR DS_CANAL=WHATSAPP"    "$CB" "'WHATSAPP'"
_assert_grep "INVITE_TUTOR DT_EXPIRACAO +7 dias" "$CB" "INTERVAL '7' DAY"
_assert_grep "PET seed"                          "$CB" "'Marley'"
_assert_grep "PET SG_PORTE valido (G)"           "$CB" "'G'"
_assert_grep "TUTOR_PET seed"                    "$CB" "MERGE INTO TUTOR_PET"
_assert_grep "AGENDAMENTO seed futuro"           "$CB" "MERGE INTO AGENDAMENTO"
_assert_grep "AGENDAMENTO NR_VERSION=0"          "$CB" "NR_VERSION.*0"
_assert_grep "Seeds são idempotentes (MERGE)"    "$CB" "WHEN NOT MATCHED THEN INSERT"

# ── 10. H2 Oracle mode compatibility ─────────────────────────────────────────
echo ""
echo "── 10. H2 Oracle mode compatibility ──"
for f in "$V1" "$V2" "$V3" "$V4" "$V5" "$CB"; do
  fname=$(basename "$f")
  # Verifica apenas linhas não comentadas (não iniciadas por --)
  _assert "$fname: sem EXECUTE IMMEDIATE fora de comentário" \
      bash -c "! grep -E '^[[:space:]]*EXECUTE IMMEDIATE' '$f'"
done
_assert_not_grep "V1 sem NOCACHE/NOCYCLE não padrão" \
    "$V1" "NOCACHE|NOCYCLE"

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════"
printf " Resultado: %d passed, %d failed\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
[[ "$FAIL" -eq 0 ]]
