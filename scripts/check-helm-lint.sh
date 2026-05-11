#!/usr/bin/env bash
# check-helm-lint — validate infrastructure/helm/kitehub passes helm lint + template.
#
# Closes GAP-467 AC #5 — CI guard preventing regression of Go-templates-in-values.yaml
# pattern that broke `helm lint` on main (PR #984). Wave 57 chữa root cause via
# extraction to `templates/alertmanager-config.yaml`; this script guards against
# recurrence.
#
# Behavior:
#   - Detects helm binary; if missing in CI → FAIL with install instructions.
#   - If missing locally (CI=false), exit 0 with advisory (local dev not required).
#   - Runs `helm dependency update` (tolerant — failure non-blocking, may have no deps).
#   - Runs `helm lint <chart>` — FAIL on non-zero exit.
#   - Runs `helm template <chart>` — FAIL on non-zero exit (catches Go-template
#     errors in values.yaml that `helm lint` alone may miss).
#
# Exit codes:
#   0 = PASS (or helm missing in non-CI environment, advisory only)
#   1 = FAIL (helm lint OR helm template failed)
#   2 = FAIL (helm missing in CI environment)
#
# Used by: .github/workflows/script-quality.yml (helm-lint job) + manual local run.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
CHART_DIR="${REPO_ROOT}/infrastructure/helm/kitehub"

# ANSI color codes (skip when not a TTY)
if [[ -t 1 ]]; then
  RED=$'\033[0;31m'
  GREEN=$'\033[0;32m'
  YELLOW=$'\033[1;33m'
  RESET=$'\033[0m'
else
  RED="" ; GREEN="" ; YELLOW="" ; RESET=""
fi

pass()  { echo "${GREEN}[PASS]${RESET} $*"; }
fail()  { echo "${RED}[FAIL]${RESET} $*" >&2; }
warn()  { echo "${YELLOW}[WARN]${RESET} $*"; }
info()  { echo "[INFO] $*"; }

# Verify chart directory exists
if [[ ! -d "$CHART_DIR" ]]; then
  fail "Chart directory not found: $CHART_DIR"
  exit 1
fi

# Detect helm binary
if ! command -v helm >/dev/null 2>&1; then
  if [[ "${CI:-false}" == "true" ]]; then
    fail "helm binary not found in CI environment."
    echo ""
    echo "Install helm in workflow via 'azure/setup-helm@v4' before invoking this script."
    echo "See .github/workflows/script-quality.yml job 'helm-lint'."
    exit 2
  else
    warn "helm binary not found locally — skipping (CI=true would FAIL)."
    echo ""
    echo "To install helm locally:"
    echo "  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash"
    echo "OR via package manager (e.g., 'brew install helm', 'sudo snap install helm --classic')."
    echo ""
    info "Local dev not required to install helm; CI will run this check."
    exit 0
  fi
fi

info "helm version: $(helm version --short 2>/dev/null || echo unknown)"
info "Chart: $CHART_DIR"
echo ""

# Step 1: dependency update (tolerant — Chart.lock may already cover, or chart may have no deps)
info "Step 1: helm dependency update (tolerant)..."
if helm dependency update "$CHART_DIR" >/dev/null 2>&1; then
  pass "dependency update succeeded"
else
  warn "dependency update returned non-zero (may be expected if no external deps or offline)."
fi
echo ""

# Step 2: helm lint — FAIL on non-zero
info "Step 2: helm lint $CHART_DIR ..."
lint_output=$(helm lint "$CHART_DIR" 2>&1) || lint_exit=$?
lint_exit=${lint_exit:-0}

if (( lint_exit != 0 )); then
  fail "helm lint exited with code $lint_exit"
  echo "$lint_output" >&2
  exit 1
fi
pass "helm lint clean"
echo ""

# Step 3: helm template — FAIL on non-zero (catches Go-template errors that lint misses)
info "Step 3: helm template $CHART_DIR ..."
template_output_file="$(mktemp)"
# shellcheck disable=SC2064
trap "rm -f '$template_output_file'" EXIT

if helm template "$CHART_DIR" >"$template_output_file" 2>&1; then
  pass "helm template renders cleanly ($(wc -l <"$template_output_file") lines)"
else
  fail "helm template failed — see error below"
  cat "$template_output_file" >&2
  exit 1
fi
echo ""

pass "All helm-lint checks passed for $CHART_DIR"
exit 0
