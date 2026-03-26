#!/bin/bash
#
# KiteClass Frontend E2E Tests (Playwright)
#
# Chạy Playwright E2E tests cho kiteclass-frontend.
# Tests dùng mock APIs — không cần backend chạy.
# Playwright tự start Next.js dev server qua webServer config.
#
# Usage:
#   ./scripts/test-e2e-playwright.sh              # Run all tests (headless, chromium)
#   ./scripts/test-e2e-playwright.sh --ui         # Playwright interactive UI mode
#   ./scripts/test-e2e-playwright.sh --report     # Mở HTML report của lần chạy cuối
#   ./scripts/test-e2e-playwright.sh --spec <file> # Run specific spec file
#   ./scripts/test-e2e-playwright.sh --headed     # Run với browser visible
#   ./scripts/test-e2e-playwright.sh --debug      # Verbose output + pause on failure
#   ./scripts/test-e2e-playwright.sh --all-browsers  # Run chromium + firefox + webkit
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FE_DIR="$SCRIPT_DIR/../kiteclass-frontend"

# ── Colors ───────────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
BOLD="\033[1m"
NC="\033[0m"

# ── Parse args ────────────────────────────────────────────────────────────────
MODE="run"          # run | ui | report
SPEC=""
HEADED=false
DEBUG=false
ALL_BROWSERS=false

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
    --all-browsers)
      ALL_BROWSERS=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  (none)             Run all E2E tests (headless, chromium only)"
      echo "  --ui               Open Playwright interactive UI"
      echo "  --report           Open HTML report của lần chạy cuối"
      echo "  --spec <file>      Run specific spec (vd: e2e/auth.spec.ts)"
      echo "  --headed           Chạy browser visible (không headless)"
      echo "  --debug            Verbose output, pause on failure"
      echo "  --all-browsers     Run chromium + firefox + webkit (chậm hơn)"
      echo "  --help             Show this help"
      echo ""
      echo "Examples:"
      echo "  $0                               # Run all, headless"
      echo "  $0 --spec e2e/auth.spec.ts       # Chỉ chạy auth tests"
      echo "  $0 --headed --spec e2e/auth.spec.ts  # Xem browser trực tiếp"
      echo "  $0 --ui                          # Playwright UI để chọn từng test"
      echo "  $0 --report                      # Xem kết quả lần trước"
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
echo -e "${BOLD}================================================"
echo -e "  KiteClass Frontend E2E Tests (Playwright)"
echo -e "================================================${NC}"
echo ""

# ── Verify directory ──────────────────────────────────────────────────────────
if [ ! -d "$FE_DIR" ]; then
  echo -e "${RED}✗ kiteclass-frontend directory not found: $FE_DIR${NC}"
  exit 1
fi

cd "$FE_DIR"

# ── Check pnpm ────────────────────────────────────────────────────────────────
if ! command -v pnpm &>/dev/null; then
  echo -e "${RED}✗ pnpm not found. Install: npm install -g pnpm${NC}"
  exit 1
fi

# ── Install node_modules if missing ──────────────────────────────────────────
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
  echo -e "  ${YELLOW}Note: Dev server sẽ tự start tại http://localhost:3000${NC}"
  echo ""
  pnpm exec playwright test --ui
  exit 0
fi

# ── Mode: Run ─────────────────────────────────────────────────────────────────
echo -e "${CYAN}Mode:${NC} Headless run${HEADED:+ (headed)}"
if [ -n "$SPEC" ]; then
  echo -e "${CYAN}Spec:${NC} $SPEC"
fi
if [ "$ALL_BROWSERS" = "true" ]; then
  echo -e "${CYAN}Browsers:${NC} chromium + firefox + webkit"
else
  echo -e "${CYAN}Browser:${NC} chromium only (dùng --all-browsers để test đa browser)"
fi
echo ""

# ── [1/3] Check Playwright browsers ──────────────────────────────────────────
echo -e "${YELLOW}[1/3] Checking Playwright browsers...${NC}"
if [ "$ALL_BROWSERS" = "true" ]; then
  pnpm exec playwright install --with-deps 2>&1 | tail -3 || true
else
  pnpm exec playwright install chromium --with-deps 2>&1 | tail -3 || true
fi
echo -e "  ${GREEN}✓${NC} Browsers ready"
echo ""

# ── [2/3] Build playwright command ───────────────────────────────────────────
PLAYWRIGHT_ARGS=()

# Browser selection
if [ "$ALL_BROWSERS" = "false" ]; then
  PLAYWRIGHT_ARGS+=("--project=chromium")
fi

# Spec filter
if [ -n "$SPEC" ]; then
  PLAYWRIGHT_ARGS+=("$SPEC")
fi

# Headed mode
if [ "$HEADED" = "true" ]; then
  PLAYWRIGHT_ARGS+=("--headed")
fi

# Debug mode
if [ "$DEBUG" = "true" ]; then
  PLAYWRIGHT_ARGS+=("--debug")
  export PWDEBUG=1
fi

echo -e "${YELLOW}[2/3] Running E2E tests...${NC}"
echo -e "  ${CYAN}Note:${NC} Playwright sẽ tự start Next.js dev server (:3000) và stop sau khi xong"
echo ""

START_TIME=$(date +%s)

EXIT_CODE=0
NEXT_TELEMETRY_DISABLED=1 \
  pnpm exec playwright test "${PLAYWRIGHT_ARGS[@]}" || EXIT_CODE=$?

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# ── [3/3] Results ─────────────────────────────────────────────────────────────
echo ""
echo -e "${YELLOW}[3/3] Results${NC}"
echo ""

if [ $EXIT_CODE -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}✓ All E2E tests PASSED${NC} (${DURATION}s)"
  echo ""
  echo -e "  ${CYAN}HTML report:${NC} playwright-report/index.html"
  echo -e "  ${CYAN}View report:${NC} $0 --report"
else
  echo -e "  ${RED}${BOLD}✗ E2E tests FAILED${NC} (exit: $EXIT_CODE, time: ${DURATION}s)"
  echo ""
  echo -e "  ${CYAN}HTML report:${NC} playwright-report/index.html"
  echo -e "  ${CYAN}View report:${NC} $0 --report"
  echo -e "  ${CYAN}Debug mode:${NC}  $0 --debug${SPEC:+ --spec $SPEC}"
  echo -e "  ${CYAN}UI mode:${NC}     $0 --ui"
fi

echo ""
echo "================================================"
echo ""

exit $EXIT_CODE
