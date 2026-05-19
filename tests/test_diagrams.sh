#!/usr/bin/env bash
# ─── Test Suite: DER Diagram Validation (T06) ─────────────────────────────────
# Valida estrutura, entidades, notação Barker e relacionamentos do DER.
# Uso: bash tests/test_diagrams.sh
# Não requer banco nem PlantUML instalado — apenas análise estática dos arquivos.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DDIR="$ROOT_DIR/docs/diagrams"
PUML="$DDIR/der.puml"
DMD="$DDIR/der.dmd"
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
echo " KURA · DER Diagram Validation (T06)"
echo "════════════════════════════════════════════"

# ── 1. Arquivos existem ───────────────────────────────────────────────────────
echo ""
echo "── 1. File existence ──"
_assert "docs/diagrams/der.puml existe"  test -f "$PUML"
_assert "docs/diagrams/der.dmd existe"   test -f "$DMD"

# ── 2. der.puml — estrutura PlantUML ────────────────────────────────────────
echo ""
echo "── 2. der.puml — estrutura PlantUML ──"
_assert_grep "@startuml presente"    "$PUML" "@startuml"
_assert_grep "@enduml presente"      "$PUML" "@enduml"
_assert_grep "Notação Barker legend" "$PUML" "legend"
_assert_grep "endlegend presente"    "$PUML" "endlegend"

# ── 3. der.puml — notação Barker ────────────────────────────────────────────
echo ""
echo "── 3. der.puml — notação Barker (prefixos #* * o) ──"
_assert_grep "PK marker (#*) presente"        "$PUML" "#\*"
_assert_grep "Mandatory marker (*) presente"  "$PUML" "^\s+\*\s+"
_assert_grep "Optional marker (o) presente"   "$PUML" "^\s+o\s+"
_assert_grep "UK marker presente"             "$PUML" "<<UK"
_assert_grep "FK marker presente"             "$PUML" "<<FK"
_assert_grep "CHK marker presente"            "$PUML" "<<CHK"

# ── 4. der.puml — todas as 14 entidades ─────────────────────────────────────
echo ""
echo "── 4. der.puml — 14 entidades ──"
_assert_grep "CLINICA"          "$PUML" "entity.*CLINICA"
_assert_grep "ESPECIE"          "$PUML" "entity.*ESPECIE"
_assert_grep "TIPO_EVENTO"      "$PUML" "entity.*TIPO_EVENTO"
_assert_grep "RACA"             "$PUML" "entity.*RACA"
_assert_grep "VETERINARIO"      "$PUML" "entity.*VETERINARIO"
_assert_grep "TUTOR"            "$PUML" "entity.*TUTOR"
_assert_grep "PET"              "$PUML" "entity.*PET"
_assert_grep "TUTOR_PET"        "$PUML" "entity.*TUTOR_PET"
_assert_grep "INVITE_TUTOR"     "$PUML" "entity.*INVITE_TUTOR"
_assert_grep "CONTA_TUTOR"      "$PUML" "entity.*CONTA_TUTOR"
_assert_grep "CONSENTIMENTO"    "$PUML" "entity.*CONSENTIMENTO"
_assert_grep "EVENTO_CLINICO"   "$PUML" "entity.*EVENTO_CLINICO"
_assert_grep "AGENDAMENTO"      "$PUML" "entity.*AGENDAMENTO"
_assert_grep "IDEMPOTENCY_KEY"  "$PUML" "entity.*IDEMPOTENCY_KEY"

# ── 5. der.puml — color-coding de ownership ──────────────────────────────────
echo ""
echo "── 5. der.puml — cores de ownership ──"
_assert_grep "Cor cinza (.NET owns)"    "$PUML" "#E0E0E0"
_assert_grep "Cor amarela (invite)"     "$PUML" "#FFF9C4"
_assert_grep "Cor ciana (Java owns)"    "$PUML" "#B2EBF2"
_assert_grep "Cor verde (shared-write)" "$PUML" "#C8E6C9"

# ── 6. der.puml — constraints de negócio ────────────────────────────────────
echo ""
echo "── 6. der.puml — constraints de negócio ──"
_assert_grep "NR_CPF VARCHAR2(11)"               "$PUML" "NR_CPF.*VARCHAR2\(11\)"
_assert_grep "SG_PORTE CHECK P|M|G"              "$PUML" "CHK.*P\|M\|G"
_assert_grep "SG_SEXO CHECK M|F"                 "$PUML" "CHK.*M\|F"
_assert_grep "DS_CANAL CHECK WHATSAPP|EMAIL|SMS" "$PUML" "WHATSAPP\|EMAIL\|SMS"
_assert_grep "@Version em NR_VERSION"            "$PUML" "@Version"
_assert_grep "UK_CONTA_INVITE_USED mencionado"   "$PUML" "UK_CONTA_INVITE_USED"
_assert_grep "CLOB em DS_TEXTO_TERMO"            "$PUML" "CLOB"
_assert_grep "DS_IP_ACEITE LGPD"                 "$PUML" "DS_IP_ACEITE"

# ── 7. der.puml — campos adicionados em V5 ──────────────────────────────────
echo ""
echo "── 7. der.puml — campos V5 em AGENDAMENTO ──"
_assert_grep "DS_OBSERVACOES [V5]"              "$PUML" "DS_OBSERVACOES"
_assert_grep "DT_CONFIRMACAO [V5]"              "$PUML" "DT_CONFIRMACAO"
_assert_grep "DT_CANCELAMENTO [V5]"             "$PUML" "DT_CANCELAMENTO"
_assert_grep "DS_MOTIVO_CANCEL [V5]"            "$PUML" "DS_MOTIVO_CANCEL"
_assert_grep "ID_EVENTO_GERADO [V5]"            "$PUML" "ID_EVENTO_GERADO"
_assert_grep "V5 marker em campos"              "$PUML" "\[V5\]"

# ── 8. der.puml — relacionamentos principais ────────────────────────────────
echo ""
echo "── 8. der.puml — relacionamentos (FK) ──"
_assert_grep "ESPECIE → RACA"                   "$PUML" "ESPECIE.*RACA|RACA.*ESPECIE"
_assert_grep "CLINICA → VETERINARIO"            "$PUML" "CLINICA.*VETERINARIO"
_assert_grep "CLINICA → TUTOR"                  "$PUML" "CLINICA.*TUTOR"
_assert_grep "CLINICA → AGENDAMENTO"            "$PUML" "CLINICA.*AGENDAMENTO"
_assert_grep "TUTOR → CONTA_TUTOR"              "$PUML" "TUTOR.*CONTA_TUTOR"
_assert_grep "TUTOR → INVITE_TUTOR"             "$PUML" "TUTOR.*INVITE_TUTOR"
_assert_grep "TUTOR → CONSENTIMENTO"            "$PUML" "TUTOR.*CONSENTIMENTO"
_assert_grep "INVITE_TUTOR → CONTA_TUTOR"       "$PUML" "INVITE_TUTOR.*CONTA_TUTOR"
_assert_grep "PET → EVENTO_CLINICO"             "$PUML" "PET.*EVENTO_CLINICO|EVENTO_CLINICO.*PET"
_assert_grep "EVENTO_CLINICO → AGENDAMENTO (V5)" "$PUML" "EVENTO_CLINICO.*AGENDAMENTO"

# ── 9. der.dmd — estrutura XML válida ───────────────────────────────────────
echo ""
echo "── 9. der.dmd — estrutura XML ──"
_assert_grep "XML declaration"          "$DMD" "<\?xml"
_assert_grep "PhysicalModel root"       "$DMD" "<PhysicalModel"
_assert_grep "BARKER notation"          "$DMD" "BARKER"
_assert_grep "Oracle 19c rdbms"         "$DMD" "Oracle Database 19c"
_assert_grep "entities block"           "$DMD" "<entities>"
_assert_grep "relationships block"      "$DMD" "<relationships>"

# ── 10. der.dmd — todas as 14 entidades ─────────────────────────────────────
echo ""
echo "── 10. der.dmd — 14 entidades ──"
_assert_grep "ENT_CLINICA"          "$DMD" "ENT_CLINICA"
_assert_grep "ENT_ESPECIE"          "$DMD" "ENT_ESPECIE"
_assert_grep "ENT_TIPO_EVENTO"      "$DMD" "ENT_TIPO_EVENTO"
_assert_grep "ENT_RACA"             "$DMD" "ENT_RACA"
_assert_grep "ENT_VETERINARIO"      "$DMD" "ENT_VETERINARIO"
_assert_grep "ENT_TUTOR"            "$DMD" "ENT_TUTOR"
_assert_grep "ENT_PET"              "$DMD" "ENT_PET"
_assert_grep "ENT_TUTOR_PET"        "$DMD" "ENT_TUTOR_PET"
_assert_grep "ENT_INVITE_TUTOR"     "$DMD" "ENT_INVITE_TUTOR"
_assert_grep "ENT_CONTA_TUTOR"      "$DMD" "ENT_CONTA_TUTOR"
_assert_grep "ENT_CONSENTIMENTO"    "$DMD" "ENT_CONSENTIMENTO"
_assert_grep "ENT_EVENTO_CLINICO"   "$DMD" "ENT_EVENTO_CLINICO"
_assert_grep "ENT_AGENDAMENTO"      "$DMD" "ENT_AGENDAMENTO"
_assert_grep "ENT_IDEMPOTENCY_KEY"  "$DMD" "ENT_IDEMPOTENCY_KEY"

# ── 11. der.dmd — constraints e índices ──────────────────────────────────────
echo ""
echo "── 11. der.dmd — constraints e índices ──"
_assert_grep "UK_CONTA_INVITE_USED em dmd"   "$DMD" "UK_CONTA_INVITE_USED"
_assert_grep "IDX_INVITE_TOKEN_ATIVO (V3)"   "$DMD" "IDX_INVITE_TOKEN_ATIVO"
_assert_grep "IDX_IDEMPOT_CRIACAO (V2)"      "$DMD" "IDX_IDEMPOT_CRIACAO"
_assert_grep "IDX_AGEND_EVENTO (V5)"         "$DMD" "IDX_AGEND_EVENTO"
_assert_grep "FK_AGEND_EVENTO (V5)"          "$DMD" "FK_AGEND_EVENTO"
_assert_grep "addedBy V5 em dmd"             "$DMD" "addedBy.*V5"
_assert_grep "addedBy V3 em dmd"             "$DMD" "addedBy.*V3"
_assert_grep "addedBy V2 em dmd"             "$DMD" "addedBy.*V2"

# ── 12. der.dmd — ownership metadata ─────────────────────────────────────────
echo ""
echo "── 12. der.dmd — ownership metadata ──"
_assert_grep "owner .NET declarado"      "$DMD" "owner=\".NET\""
_assert_grep "owner Java declarado"      "$DMD" "owner=\"Java\""
_assert_grep "owner SHARED declarado"    "$DMD" "owner=\"SHARED\""
_assert_grep "javaAccess READ_ONLY"      "$DMD" "javaAccess=\"READ_ONLY\""
_assert_grep "javaAccess READ_WRITE"     "$DMD" "javaAccess=\"READ_WRITE\""
_assert_grep "javaAccess READ_ONCE"      "$DMD" "javaAccess=\"READ_ONCE\""

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════"
printf " Resultado: %d passed, %d failed\n" "$PASS" "$FAIL"
echo "════════════════════════════════════════════"
[[ "$FAIL" -eq 0 ]]
