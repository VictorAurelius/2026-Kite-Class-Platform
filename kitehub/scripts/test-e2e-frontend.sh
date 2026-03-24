#!/bin/bash
#
# KiteHub Frontend E2E Tests (Playwright)
#
# Chạy toàn bộ Playwright E2E tests cho kitehub-frontend.
# Tests dùng mock APIs — không cần backend chạy.
#
# Usage:
#   ./scripts/test-e2e-frontend.sh              # Run all tests (headless)
#   ./scripts/test-e2e-frontend.sh --ui         # Playwright interactive UI mode
#   ./scripts/test-e2e-frontend.sh --report     # Mở HTML report của lần chạy cuối
#   ./scripts/test-e2e-frontend.sh --spec <file> # Run specific spec file
#   ./scripts/test-e2e-frontend.sh --headed     # Run với browser visible
#   ./scripts/test-e2e-frontend.sh --debug      # Verbose output + pause on failure
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FE_DIR="$SCRIPT_DIR/../kitehub-frontend"

# ── Colors ───────────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
BOLD="\033[1m"
NC="\033[0m"

# ── Parse args ────────────────────────────────────────────────────────────────
MODE="run"         # run | ui | report
SPEC=""
HEADED=false
DEBUG=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --ui)
      MODE="ui"
      shift
      ;;
    --report)
      MODE="report"
      shift
      ;;
    --spec)
      SPEC="${2:-}"
      shift 2
      ;;
    --headed)
      HEADED=true
      shift
      ;;
    --debug)
      DEBUG=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  (none)         Run all E2E tests (headless, with server auto-start)"
      echo "  --ui           Open Playwright interactive UI"
      echo "  --report       Open HTML report của lần chạy cuối"
      echo "  --spec <file>  Run specific spec (vd: e2e/auth.spec.ts)"
      echo "  --headed       Chạy browser visible (không headless)"
      echo "  --debug        Verbose output, pause on failure"
      echo "  --help         Show this help"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      echo "Run $0 --help for usage"
      exit 1
      ;;
  esac
done

# ── Header ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}=============================================="
echo -e "  KiteHub Frontend E2E Tests (Playwright)"
echo -e "==============================================${NC}"
echo ""

# ── Verify directory ──────────────────────────────────────────────────────────
if [ ! -d "$FE_DIR" ]; then
  echo -e "${RED}✗ kitehub-frontend directory not found: $FE_DIR${NC}"
  exit 1
fi

cd "$FE_DIR"

# ── Check pnpm ────────────────────────────────────────────────────────────────
if ! command -v pnpm &>/dev/null; then
  echo -e "${RED}✗ pnpm not found. Install: npm install -g pnpm${NC}"
  exit 1
fi

# ── Check node_modules ────────────────────────────────────────────────────────
if [ ! -d "node_modules" ]; then
  echo -e "${YELLOW}⚠ node_modules not found. Running pnpm install...${NC}"
  pnpm install --frozen-lockfile
fi

# ── Mode: Open existing report ────────────────────────────────────────────────
if [ "$MODE" = "report" ]; then
  if [ ! -d "playwright-report" ]; then
    echo -e "${RED}✗ No report found. Run tests first: $0${NC}"
    exit 1
  fi
  echo -e "${CYAN}Opening Playwright HTML report...${NC}"
  pnpm exec playwright show-report playwright-report
  exit 0
fi

# ── Mode: Interactive UI ──────────────────────────────────────────────────────
if [ "$MODE" = "ui" ]; then
  echo -e "${CYAN}Starting Playwright UI (interactive mode)...${NC}"
  echo -e "  ${YELLOW}Note: Server sẽ tự start tại http://localhost:3001${NC}"
  echo ""
  PLAYWRIGHT_BASE_URL="http://localhost:3001" \
    pnpm exec playwright test --ui
  exit 0
fi

# ── Mode: Run ─────────────────────────────────────────────────────────────────
echo -e "${CYAN}Mode:${NC} Headless run${HEADED:+ (headed)}"
if [ -n "$SPEC" ]; then
  echo -e "${CYAN}Spec:${NC} $SPEC"
fi
echo ""

# ── Check Playwright browsers ─────────────────────────────────────────────────
echo -e "${YELLOW}[1/3] Checking Playwright browsers...${NC}"
if ! pnpm exec playwright install --dry-run chromium &>/dev/null 2>&1; then
  echo -e "  Browser check inconclusive — attempting install..."
fi

# Install browsers if chromium missing
if ! pnpm exec playwright install chromium --with-deps 2>&1 | grep -q "Playwright Host validation warning\|chromium.*is already installed\|Already installed\|playwright" 2>/dev/null; then
  # Fallback: just run install silently (idempotent)
  pnpm exec playwright install chromium --with-deps 2>&1 | grep -v "^$" | head -5 || true
fi
echo -e "  ${GREEN}✓${NC} Browsers ready"
echo ""

# ── Build playwright command ──────────────────────────────────────────────────
PLAYWRIGHT_ARGS=()

if [ -n "$SPEC" ]; then
  PLAYWRIGHT_ARGS+=("$SPEC")
fi

if [ "$HEADED" = "true" ]; then
  PLAYWRIGHT_ARGS+=("--headed")
fi

if [ "$DEBUG" = "true" ]; then
  PLAYWRIGHT_ARGS+=("--debug")
  export PWDEBUG=1
fi

# ── Start server + run tests ──────────────────────────────────────────────────
echo -e "${YELLOW}[2/3] Starting dev server + running E2E tests...${NC}"
echo -e "  Server: http://localhost:3001 (auto-start via start-server-and-test)"
echo ""

START_TIME=$(date +%s)

# Use pnpm test:e2e:ci which uses start-server-and-test
# For custom args (spec/headed), call playwright directly after manual server check
if [ ${#PLAYWRIGHT_ARGS[@]} -eq 0 ]; then
  # Standard run: use the predefined CI script
  EXIT_CODE=0
  NEXT_TELEMETRY_DISABLED=1 \
  PLAYWRIGHT_BASE_URL="http://localhost:3001" \
    pnpm test:e2e:ci || EXIT_CODE=$?
else
  # Custom run: start server in background, run playwright with custom args
  echo -e "  ${YELLOW}Starting Next.js dev server in background...${NC}"

  # Check if server already running
  if curl -sf http://localhost:3001 &>/dev/null; then
    echo -e "  ${GREEN}✓${NC} Server already running at :3001"
    SERVER_PID=""
  else
    NEXT_TELEMETRY_DISABLED=1 pnpm dev --port 3001 &>/tmp/kitehub-fe-server.log &
    SERVER_PID=$!

    # Wait for server ready (max 60s)
    echo -n "  Waiting for server"
    WAITED=0
    while [ $WAITED -lt 60 ]; do
      if curl -sf http://localhost:3001 &>/dev/null; then
        echo -e " ${GREEN}✓${NC} (${WAITED}s)"
        break
      fi
      echo -n "."
      sleep 2
      WAITED=$((WAITED + 2))
    done

    if [ $WAITED -ge 60 ]; then
      echo ""
      echo -e "  ${RED}✗ Server did not start in 60s${NC}"
      kill "$SERVER_PID" 2>/dev/null || true
      exit 1
    fi
  fi

  # Run playwright with custom args
  EXIT_CODE=0
  PLAYWRIGHT_BASE_URL="http://localhost:3001" \
    pnpm exec playwright test "${PLAYWRIGHT_ARGS[@]}" || EXIT_CODE=$?

  # Kill dev server if we started it
  if [ -n "${SERVER_PID:-}" ]; then
    kill "$SERVER_PID" 2>/dev/null || true
  fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo -e "${YELLOW}[3/3] Results${NC}"
echo ""

# ── Summary ───────────────────────────────────────────────────────────────────
if [ $EXIT_CODE -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}✓ All E2E tests PASSED${NC} (${DURATION}s)"
  echo ""
  echo -e "  ${CYAN}HTML report:${NC} playwright-report/index.html"
  echo -e "  ${CYAN}View report:${NC} $0 --report"
else
  echo -e "  ${RED}${BOLD}✗ E2E tests FAILED${NC} (exit code: $EXIT_CODE, time: ${DURATION}s)"
  echo ""
  echo -e "  ${CYAN}HTML report:${NC} playwright-report/index.html"
  echo -e "  ${CYAN}View report:${NC} $0 --report"
  echo -e "  ${CYAN}Debug mode:${NC}  $0 --debug"
  echo -e "  ${CYAN}UI mode:${NC}     $0 --ui"
fi

echo ""
echo "=============================================="
echo ""

exit $EXIT_CODE
