#!/usr/bin/env bash
# verify-business-docs.sh
# Wave 12 — Phase A: Verification chain audit
# Chain: BR-xxx → UC-xxx → endpoint → @Mapping
#
# Usage:
#   ./scripts/verify-business-docs.sh [kiteclass|kitehub|all]

PROJECT="${1:-all}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOCS_ROOT="$REPO_ROOT/documents/01-business"

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

TOTAL_PASS=0
TOTAL_WARN=0
TOTAL_FAIL=0

log_pass() { echo -e "  ${GREEN}✅ PASS${NC} $1"; TOTAL_PASS=$((TOTAL_PASS + 1)); }
log_warn() { echo -e "  ${YELLOW}⚠️  WARN${NC} $1"; TOTAL_WARN=$((TOTAL_WARN + 1)); }
log_fail() { echo -e "  ${RED}❌ FAIL${NC} $1"; TOTAL_FAIL=$((TOTAL_FAIL + 1)); }
log_info() { echo -e "  ${CYAN}ℹ️  INFO${NC} $1"; }
log_section() { echo -e "\n${CYAN}${BOLD}$1${NC}"; }
log_domain() { echo -e "\n${BOLD}  Domain: $1${NC}"; }

# Extract UC numbers covered by a range notation like "UC-ATT-01 → UC-ATT-07"
# Returns a sorted list of UC-XXX-NN strings covered by all ranges
expand_uc_ranges() {
  local file="$1"
  local prefix="$2"  # e.g. UC-ATT
  # Find ranges like UC-ATT-01 → UC-ATT-07
  grep -oP "${prefix}-\d+\s*[→>-]+\s*${prefix}-\d+" "$file" 2>/dev/null | while IFS= read -r range; do
    local start end
    start=$(echo "$range" | grep -oP '\d+' | head -1)
    end=$(echo "$range" | grep -oP '\d+' | tail -1)
    for i in $(seq "$start" "$end"); do
      printf '%s-%02d\n' "$prefix" "$i"
    done
  done | sort -u
}

verify_domain() {
  local project="$1"
  local domain_dir="$2"
  local code_src="$3"
  local domain_name
  domain_name=$(basename "$domain_dir")

  local rules="$domain_dir/rules.md"
  local usecases="$domain_dir/use-cases.md"
  local apicontract="$domain_dir/api-contract.md"

  log_domain "$project/$domain_name"

  # Check 0: 3 files exist
  if [[ ! -f "$rules" || ! -f "$usecases" || ! -f "$apicontract" ]]; then
    log_fail "Missing 3-layer files (rules/use-cases/api-contract)"
    return
  fi

  # ── Check 1: BR-xxx → use-cases.md ─────────────────────────────────────
  local br_ids br_orphans br_count
  br_ids=$(grep -oP 'BR-[A-Z]+-\d+' "$rules" 2>/dev/null | sort -u || true)
  br_orphans=0
  br_count=0

  if [[ -z "$br_ids" ]]; then
    # Check if rules.md uses alternate format (RET-01, SUB-01 without BR- prefix)
    local alt_ids
    alt_ids=$(grep -oP '\b[A-Z]{2,6}-\d{2,3}\b' "$rules" 2>/dev/null | grep -v 'UC-\|BR-\|HTTP\|GET\|POST\|PUT\|DEL' | sort -u || true)
    if [[ -n "$alt_ids" ]]; then
      log_info "rules.md uses non-BR format IDs ($(echo "$alt_ids" | wc -l | tr -d ' ') IDs) — skip BR→UC check"
    else
      log_warn "No BR-xxx or rule IDs found in rules.md"
    fi
  else
    while IFS= read -r br_id; do
      [[ -z "$br_id" ]] && continue
      br_count=$((br_count + 1))
      if ! grep -q "$br_id" "$usecases" 2>/dev/null; then
        log_warn "BR orphan: $br_id (rules.md) not referenced in use-cases.md"
        br_orphans=$((br_orphans + 1))
      fi
    done <<< "$br_ids"
    if [[ "$br_orphans" -eq 0 ]]; then
      log_pass "BR → UC: all $br_count BR-ids referenced in use-cases.md"
    fi
  fi

  # ── Check 2: UC-xxx → api-contract.md ──────────────────────────────────
  local uc_prefixes uc_orphans uc_count
  uc_prefixes=$(grep -oP 'UC-[A-Z]+' "$usecases" 2>/dev/null | sort -u || true)
  uc_orphans=0
  uc_count=0

  while IFS= read -r uc_id; do
    [[ -z "$uc_id" ]] && continue
    uc_count=$((uc_count + 1))
    if grep -q "$uc_id" "$apicontract" 2>/dev/null; then
      continue  # Direct match
    fi
    # Check if covered by a range in api-contract.md
    local prefix
    prefix=$(echo "$uc_id" | grep -oP 'UC-[A-Z]+')
    local num
    num=$(echo "$uc_id" | grep -oP '\d+$')
    local covered=0
    while IFS= read -r range_line; do
      local rstart rend
      rstart=$(echo "$range_line" | grep -oP '\d+' | head -1)
      rend=$(echo "$range_line" | grep -oP '\d+' | tail -1)
      if [[ "10#$num" -ge "10#$rstart" && "10#$num" -le "10#$rend" ]]; then
        covered=1
        break
      fi
    done < <(grep -oP "${prefix}-\d+\s*[→>]+\s*${prefix}-\d+" "$apicontract" 2>/dev/null || true)

    if [[ "$covered" -eq 0 ]]; then
      log_warn "UC orphan: $uc_id (use-cases.md) not referenced in api-contract.md"
      uc_orphans=$((uc_orphans + 1))
    fi
  done < <(grep -oP 'UC-[A-Z]+-\d+' "$usecases" 2>/dev/null | sort -u || true)

  if [[ "$uc_count" -eq 0 ]]; then
    log_warn "No UC-xxx IDs found in use-cases.md"
  elif [[ "$uc_orphans" -eq 0 ]]; then
    log_pass "UC → API: all $uc_count UC-ids referenced in api-contract.md"
  fi

  # ── Check 3: endpoints in api-contract.md → Controller code ────────────
  # Handles both plain "POST /api/v1/..." and backtick "### POST `/api/v1/...`"
  local endpoints endpoint_orphans endpoint_count
  endpoints=$(grep -oP "(GET|POST|PUT|DELETE|PATCH)\s+\`?/[a-zA-Z0-9/_{}?=&.-]+" "$apicontract" 2>/dev/null | \
    sed "s/\`//g" | sort -u || true)
  endpoint_orphans=0
  endpoint_count=0

  while IFS= read -r endpoint; do
    [[ -z "$endpoint" ]] && continue
    endpoint_count=$((endpoint_count + 1))
    local path
    path=$(echo "$endpoint" | grep -oP '/[a-zA-Z0-9/_{}.-]+' | head -1 || true)
    [[ -z "$path" ]] && continue
    # Strip /api/vN and path vars, get first meaningful segment
    local search_term
    search_term=$(echo "$path" | sed 's|/api/v[0-9]*||g' | sed 's/{[^}]*}//g' | tr '/' '\n' | grep -v '^$' | head -1 || true)
    [[ -z "$search_term" ]] && continue

    if ! grep -rq "$search_term" "$code_src" --include="*.java" 2>/dev/null; then
      log_warn "Phantom endpoint: $endpoint — segment '$search_term' not found in controller code"
      endpoint_orphans=$((endpoint_orphans + 1))
    fi
  done <<< "$endpoints"

  if [[ "$endpoint_count" -eq 0 ]]; then
    log_warn "No endpoints found in api-contract.md (check format)"
  elif [[ "$endpoint_orphans" -eq 0 ]]; then
    log_pass "API → Code: all $endpoint_count endpoints have matching controller"
  fi
}

verify_project() {
  local project="$1"
  local code_src="$2"
  log_section "═══ $project ═══"
  local domain_dir
  for domain_dir in "$DOCS_ROOT/$project"/*/; do
    [[ -d "$domain_dir" ]] || continue
    verify_domain "$project" "$domain_dir" "$code_src"
  done
}

# ── Main ─────────────────────────────────────────────────────────────────────
echo -e "${BOLD}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║   Business Docs Verification — Wave 12 Phase A  ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════╝${NC}"
echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
echo "Chain: BR → UC → API → Controller"

case "$PROJECT" in
  kiteclass)
    verify_project "kiteclass" "$REPO_ROOT/kiteclass/kiteclass-core/src"
    ;;
  kitehub)
    verify_project "kitehub" "$REPO_ROOT/kitehub/kitehub-subscription/src"
    ;;
  all)
    verify_project "kiteclass" "$REPO_ROOT/kiteclass/kiteclass-core/src"
    verify_project "kitehub" "$REPO_ROOT/kitehub/kitehub-subscription/src"
    ;;
  *)
    echo "Usage: $0 [kiteclass|kitehub|all]"
    exit 1
    ;;
esac

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}═══════════════════════════════════════${NC}"
echo -e "${BOLD}Summary${NC}"
echo -e "  ${GREEN}✅ PASS: $TOTAL_PASS${NC}"
echo -e "  ${YELLOW}⚠️  WARN: $TOTAL_WARN${NC}"
echo -e "  ${RED}❌ FAIL: $TOTAL_FAIL${NC}"

if [[ "$TOTAL_FAIL" -gt 0 ]]; then
  echo -e "\n${RED}${BOLD}❌ Verification FAILED — $TOTAL_FAIL critical issues${NC}"
  exit 1
elif [[ "$TOTAL_WARN" -gt 0 ]]; then
  echo -e "\n${YELLOW}${BOLD}⚠️  Verification complete — $TOTAL_WARN warnings to review${NC}"
  exit 0
else
  echo -e "\n${GREEN}${BOLD}✅ Verification PASSED — 0 issues${NC}"
  exit 0
fi
