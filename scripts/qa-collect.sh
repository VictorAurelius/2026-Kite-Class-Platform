#!/bin/bash
# qa-collect.sh — Thu thập metrics cho Quality Audit
#
# Usage:
#   ./scripts/qa-collect.sh [kitehub|kiteclass|all]  [--skip-backend] [--skip-frontend] [--skip-e2e]
#
# Output: Structured text, dễ copy vào quality-audit report
#
# Exit codes:
#   0 — tất cả checks pass (hoặc skip)
#   1 — có failures
#   2 — script error

set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# ── Defaults ────────────────────────────────────────────────
TARGET="${1:-all}"
SKIP_BACKEND=false
SKIP_FRONTEND=false
SKIP_E2E=true   # E2E mặc định skip (cần Docker up)
JAVA_HOME_PATH="${JAVA_HOME:-/home/vkiet/jdk/jdk-21}"

# ── Parse flags ─────────────────────────────────────────────
for arg in "$@"; do
  case "$arg" in
    --skip-backend)   SKIP_BACKEND=true ;;
    --skip-frontend)  SKIP_FRONTEND=true ;;
    --skip-e2e)       SKIP_E2E=true ;;
    --with-e2e)       SKIP_E2E=false ;;
    kitehub|kiteclass|all) TARGET="$arg" ;;
  esac
done

# ── Colors ───────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ── State ────────────────────────────────────────────────────
OVERALL_EXIT=0
REPORT_LINES=()

# ── Helpers ──────────────────────────────────────────────────
section() { echo -e "\n${CYAN}${BOLD}══ $1 ══${NC}"; }
ok()      { echo -e "  ${GREEN}✅${NC} $1"; REPORT_LINES+=("✅ $1"); }
warn()    { echo -e "  ${YELLOW}⚠️${NC}  $1"; REPORT_LINES+=("⚠️  $1"); }
fail()    { echo -e "  ${RED}❌${NC} $1"; REPORT_LINES+=("❌ $1"); OVERALL_EXIT=1; }
skip()    { echo -e "  ${YELLOW}⏭️${NC}  $1"; REPORT_LINES+=("⏭️  $1 [SKIPPED]"); }
info()    { echo -e "     $1"; REPORT_LINES+=("   $1"); }

# ── Parse maven test output ───────────────────────────────────
parse_mvn_results() {
  local output="$1"
  local label="$2"

  # "Tests run: 388, Failures: 0, Errors: 0, Skipped: 0"
  local summary
  summary=$(echo "$output" | grep -E "Tests run:.*Failures:.*Errors:" | tail -5)

  if [ -z "$summary" ]; then
    fail "$label — không parse được kết quả test"
    return
  fi

  local total=0 failures=0 errors=0 skipped=0
  while IFS= read -r line; do
    t=$(echo "$line"   | grep -oP 'Tests run: \K[0-9]+' || echo 0)
    f=$(echo "$line"   | grep -oP 'Failures: \K[0-9]+'  || echo 0)
    e=$(echo "$line"   | grep -oP 'Errors: \K[0-9]+'    || echo 0)
    s=$(echo "$line"   | grep -oP 'Skipped: \K[0-9]+'   || echo 0)
    total=$((total + t))
    failures=$((failures + f))
    errors=$((errors + e))
    skipped=$((skipped + s))
  done <<< "$summary"

  local result="${total} run, ${failures} failed, ${errors} errors, ${skipped} skipped"
  if [ "$failures" -gt 0 ] || [ "$errors" -gt 0 ]; then
    fail "$label — $result"
    # Show failed test names
    echo "$output" | grep -E "FAILED|ERROR" | grep -v "BUILD" | head -5 | while read -r l; do
      echo "     $l"
    done
  elif [ "$skipped" -gt 0 ]; then
    warn "$label — $result"
  else
    ok "$label — $result"
  fi
}

# ── Parse vitest output ───────────────────────────────────────
parse_vitest_results() {
  local output="$1"
  local label="$2"

  # Detect if vitest failed to start (node_modules issue, etc.)
  if echo "$output" | grep -qE "Cannot find module|ENOENT|command not found"; then
    local err_line
    err_line=$(echo "$output" | grep -E "Cannot find module|ENOENT" | head -1)
    fail "$label — vitest không start được: $err_line"
    info "Kiểm tra: node_modules đã cài đúng môi trường (WSL/Windows)?"
    return
  fi

  # Strip ANSI color codes trước khi parse
  local clean
  clean=$(echo "$output" | sed 's/\x1B\[[0-9;]*[mGKHF]//g')

  # "Test Files  33 passed (33)" or "Tests  443 passed (443)"
  local files_line tests_line
  files_line=$(echo "$clean" | grep -E "Test Files" | tail -1 || echo "")
  tests_line=$(echo "$clean" | grep -E "^\s*Tests " | tail -1 || echo "")

  if [ -z "$tests_line" ]; then
    warn "$label — không parse được kết quả (output không có 'Tests X passed')"
    return
  fi

  local passed failed skipped
  passed=$(echo "$tests_line" | grep -oP '\d+ passed' | grep -oP '\d+' || echo 0)
  failed=$(echo "$tests_line" | grep -oP '\d+ failed'  | grep -oP '\d+' || echo 0)
  skipped=$(echo "$tests_line" | grep -oP '\d+ skipped' | grep -oP '\d+' || echo 0)

  local files_info=""
  if [ -n "$files_line" ]; then
    local fpassed
    fpassed=$(echo "$files_line" | grep -oP '\d+ passed' | grep -oP '\d+' || echo "?")
    files_info=" | ${fpassed} test files"
  fi

  local result="${passed} passed, ${failed} failed, ${skipped} skipped${files_info}"
  if [ "$failed" -gt 0 ]; then
    fail "$label — $result"
  elif [ "$skipped" -gt 0 ]; then
    warn "$label — $result"
  else
    ok "$label — $result"
  fi
}

# ── Check Docker is up ────────────────────────────────────────
check_docker_up() {
  local compose_file="$1"
  if ! docker compose -f "$compose_file" ps --format json 2>/dev/null | grep -q '"State":"running"'; then
    return 1
  fi
  return 0
}

# ════════════════════════════════════════════════════════════
# BACKEND TESTS
# ════════════════════════════════════════════════════════════
run_backend_kitehub() {
  section "Backend Tests — KiteHub"
  if [ "$SKIP_BACKEND" = true ]; then
    skip "KiteHub backend tests (--skip-backend)"; return
  fi

  echo -e "  Chạy: JAVA_HOME=$JAVA_HOME_PATH ./mvnw test (multi-module)..."
  local output exit_code
  output=$(cd "$ROOT_DIR/kitehub" && \
    JAVA_HOME="$JAVA_HOME_PATH" ./mvnw test -q 2>&1) || exit_code=$?

  if echo "$output" | grep -q "BUILD FAILURE"; then
    fail "KiteHub backend — BUILD FAILURE"
    echo "$output" | grep -E "ERROR|FAILED" | head -5 | while read -r l; do info "$l"; done
  else
    parse_mvn_results "$output" "KiteHub backend"
  fi
}

run_backend_kiteclass() {
  section "Backend Tests — KiteClass"
  if [ "$SKIP_BACKEND" = true ]; then
    skip "KiteClass backend tests (--skip-backend)"; return
  fi

  echo -e "  Chạy: kiteclass-core..."
  local output exit_code=0

  output=$(cd "$ROOT_DIR/kiteclass/kiteclass-core" && ./mvnw test -q 2>&1) || exit_code=$?
  parse_mvn_results "$output" "KiteClass core"

  # dedicated gateway removed per ADR-032 / GAP-001
}

# ════════════════════════════════════════════════════════════
# FRONTEND TESTS
# ════════════════════════════════════════════════════════════
run_frontend_kitehub() {
  section "Frontend Tests — KiteHub"
  if [ "$SKIP_FRONTEND" = true ]; then
    skip "KiteHub frontend tests (--skip-frontend)"; return
  fi

  echo -e "  Chạy: vitest run..."
  local output
  output=$(cd "$ROOT_DIR/kitehub/kitehub-frontend" && npx vitest run 2>&1) || true
  parse_vitest_results "$output" "KiteHub frontend (vitest)"
}

run_frontend_kiteclass() {
  section "Frontend Tests — KiteClass"
  if [ "$SKIP_FRONTEND" = true ]; then
    skip "KiteClass frontend tests (--skip-frontend)"; return
  fi

  echo -e "  Chạy: vitest run..."
  local output
  output=$(cd "$ROOT_DIR/kiteclass/kiteclass-frontend" && npx vitest run 2>&1) || true
  parse_vitest_results "$output" "KiteClass frontend (vitest)"

  echo -e "  Chạy: next build..."
  local build_output
  build_output=$(cd "$ROOT_DIR/kiteclass/kiteclass-frontend" && npx next build 2>&1) || true
  if echo "$build_output" | grep -qE "Cannot find module|ENOENT"; then
    warn "KiteClass frontend build — node_modules lỗi (cài lại với đúng môi trường)"
    echo "$build_output" | grep -E "Cannot find module" | head -1 | while read -r l; do info "$l"; done
  elif echo "$build_output" | grep -qE "^Error|Failed to compile|Build error"; then
    fail "KiteClass frontend build — FAILED"
    echo "$build_output" | grep -E "^Error|Build error" | head -3 | while read -r l; do info "$l"; done
  else
    ok "KiteClass frontend build — OK"
  fi
}

# ════════════════════════════════════════════════════════════
# E2E TESTS
# ════════════════════════════════════════════════════════════
run_e2e_kitehub() {
  section "E2E Tests — KiteHub"
  if [ "$SKIP_E2E" = true ]; then
    skip "KiteHub E2E (thêm --with-e2e để chạy, yêu cầu Docker up)"; return
  fi

  local compose_file="$ROOT_DIR/kitehub/docker-compose.kitehub.yml"
  if ! check_docker_up "$compose_file"; then
    warn "KiteHub E2E — Docker stack không chạy, skip"
    info "Chạy: cd kitehub && ./scripts/up.sh để start"
    return
  fi

  echo -e "  Chạy: test-api-e2e.sh..."
  local output
  output=$(cd "$ROOT_DIR/kitehub" && bash scripts/test-api-e2e.sh 2>&1) || true

  local results_line
  results_line=$(echo "$output" | grep -E "Results:|PASS|FAIL" | tail -1 || echo "")
  if echo "$output" | grep -q "FAIL"; then
    fail "KiteHub API E2E — có failures"
    echo "$output" | grep "✗\|FAIL" | head -5 | while read -r l; do info "$l"; done
  else
    local pass_count
    pass_count=$(echo "$output" | grep -oP '\d+ passed' | grep -oP '\d+' || echo "?")
    ok "KiteHub API E2E — ${pass_count} tests passed"
  fi
}

# ════════════════════════════════════════════════════════════
# STATIC CHECKS (nhanh, không cần build)
# ════════════════════════════════════════════════════════════
run_static_checks() {
  section "Static Checks"

  # 1. CI status (last 5 runs on main)
  echo -e "  Kiểm tra CI status (main branch)..."
  local ci_output success_count failure_count
  ci_output=$(gh run list --branch main --limit 5 --json conclusion,name \
    --jq '.[] | "\(.conclusion)"' 2>/dev/null || echo "")
  success_count=$(echo "$ci_output" | grep -c "success" || true)
  failure_count=$(echo "$ci_output" | grep -c "failure" || true)

  if [ "$failure_count" -gt 0 ]; then
    fail "CI/CD — ${success_count}/5 success, ${failure_count} failure trên main"
  else
    ok "CI/CD — ${success_count}/5 latest runs success trên main"
  fi

  # 2. Stale branches
  local stale_branches stale_count
  stale_branches=$(git branch -r | grep -v "HEAD\|main" | sed 's/.*origin\///' | grep -v "^$" || echo "")
  stale_count=$(echo "$stale_branches" | grep -c "[a-z]" 2>/dev/null || echo 0)
  if [ "${stale_count:-0}" -eq 0 ]; then
    ok "Branches — 0 stale remote branches"
  else
    warn "Branches — ${stale_count} remote branch(es): $stale_branches"
  fi

  # 3. TODO/FIXME in production code
  local todo_count_hub todo_count_class
  todo_count_hub=$({ grep -r "TODO\|FIXME\|HACK\|XXX" \
    "$ROOT_DIR/kitehub" --include="*.java" \
    --exclude-dir=test 2>/dev/null || true; } | wc -l | tr -d '[:space:]')
  todo_count_class=$({ grep -r "TODO\|FIXME\|HACK\|XXX" \
    "$ROOT_DIR/kiteclass" --include="*.java" \
    --exclude-dir=test 2>/dev/null || true; } | wc -l | tr -d '[:space:]')
  todo_count_hub="${todo_count_hub:-0}"
  todo_count_class="${todo_count_class:-0}"
  local todo_total=$(( todo_count_hub + todo_count_class ))
  if [ "$todo_total" -eq 0 ]; then
    ok "Code Quality — 0 TODO/FIXME/HACK trong production Java"
  else
    warn "Code Quality — ${todo_total} TODO/FIXME (hub:${todo_count_hub}, class:${todo_count_class})"
  fi

  # 4. Swagger / OpenAPI
  local swagger_count
  swagger_count=$({ grep -r "springdoc\|swagger" \
    "$ROOT_DIR/kitehub" --include="pom.xml" 2>/dev/null || true; } | wc -l | tr -d '[:space:]')
  if [ "${swagger_count:-0}" -gt 0 ]; then
    ok "Documentation — Swagger/OpenAPI dependency found (kitehub)"
  else
    warn "Documentation — Swagger/OpenAPI chưa có trong kitehub"
  fi

  # 5. @Valid annotations
  local valid_count
  valid_count=$({ grep -r "@Valid" \
    "$ROOT_DIR/kitehub/kitehub-platform/src/main" --include="*.java" 2>/dev/null || true; } | \
    wc -l | tr -d '[:space:]')
  info "Security — @Valid annotations trong kitehub-platform: ${valid_count:-0}"

  # 6. Docker health (kitehub)
  if docker compose -f "$ROOT_DIR/kitehub/docker-compose.kitehub.yml" \
      ps --format json 2>/dev/null | grep -q '"State":"running"'; then
    local running_count
    running_count=$(docker compose -f "$ROOT_DIR/kitehub/docker-compose.kitehub.yml" \
      ps --format json 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(sum(1 for c in d if c.get('State')=='running'))" 2>/dev/null || echo "?")
    ok "Docker — KiteHub stack up (${running_count} containers running)"
  else
    warn "Docker — KiteHub stack không chạy"
  fi

  # 7. Code stats
  local java_src java_test fe_kiteclass fe_kitehub
  java_src=$(find "$ROOT_DIR/kitehub" -name "*.java" \
    ! -path "*/test/*" 2>/dev/null | wc -l | xargs)
  java_test=$(find "$ROOT_DIR/kitehub" \( -name "*Test.java" -o -name "*IT.java" \) 2>/dev/null | wc -l | xargs)
  fe_kiteclass=$(find "$ROOT_DIR/kiteclass/kiteclass-frontend/src" \
    \( -name "*.tsx" -o -name "*.ts" \) 2>/dev/null | wc -l | xargs)
  fe_kitehub=$(find "$ROOT_DIR/kitehub/kitehub-frontend/src" \
    \( -name "*.tsx" -o -name "*.ts" \) 2>/dev/null | wc -l | xargs)
  info "Stats — Java: ${java_src} src / ${java_test} test files"
  info "Stats — TS: kiteclass=${fe_kiteclass} files, kitehub=${fe_kitehub} files"
}

# ════════════════════════════════════════════════════════════
# MAIN
# ════════════════════════════════════════════════════════════
echo -e "${BOLD}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║         QA Collect — Quality Audit Data              ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo "  Target:   $TARGET"
echo "  Backend:  $([ "$SKIP_BACKEND" = true ] && echo "SKIP" || echo "RUN")"
echo "  Frontend: $([ "$SKIP_FRONTEND" = true ] && echo "SKIP" || echo "RUN")"
echo "  E2E:      $([ "$SKIP_E2E" = true ] && echo "SKIP (thêm --with-e2e)" || echo "RUN")"
echo "  Date:     $(date '+%Y-%m-%d %H:%M')"

# Run checks based on target
case "$TARGET" in
  kitehub)
    run_backend_kitehub
    run_frontend_kitehub
    run_e2e_kitehub
    ;;
  kiteclass)
    run_backend_kiteclass
    run_frontend_kiteclass
    ;;
  all)
    run_backend_kitehub
    run_backend_kiteclass
    run_frontend_kitehub
    run_frontend_kiteclass
    run_e2e_kitehub
    ;;
  *)
    echo -e "${RED}❌ Invalid target: $TARGET${NC}"
    echo "Usage: $0 [kitehub|kiteclass|all] [--skip-backend] [--skip-frontend] [--with-e2e]"
    exit 2
    ;;
esac

# Static checks luôn chạy
run_static_checks

# ── Summary Report ──────────────────────────────────────────
echo ""
echo -e "${BOLD}══════════════════════════════════════════════════════"
echo "  SUMMARY — $(date '+%Y-%m-%d %H:%M')"
echo -e "══════════════════════════════════════════════════════${NC}"
for line in "${REPORT_LINES[@]}"; do
  echo "  $line"
done

echo ""
if [ "$OVERALL_EXIT" -eq 0 ]; then
  echo -e "${GREEN}${BOLD}✅ QA Collect hoàn thành — Không có failures${NC}"
else
  echo -e "${RED}${BOLD}❌ QA Collect hoàn thành — Có failures, xem bên trên${NC}"
fi

exit "$OVERALL_EXIT"
