#!/usr/bin/env bash
# Wave 75 Bucket E — wrapper running 6 hook test suites under coverage.py.
#
# Usage:
#   bash .claude/hooks/tests/run-coverage.sh           # text report
#   bash .claude/hooks/tests/run-coverage.sh --html    # + HTML report
#   bash .claude/hooks/tests/run-coverage.sh --xml     # + XML report
#
# Subprocess support: tests spawn hooks via `python3 hook.py`. We use
# COVERAGE_PROCESS_START + a sitecustomize.py shim (created in a temp dir on
# PYTHONPATH) so coverage.process_startup() runs in every child process.
# Per https://coverage.readthedocs.io/en/latest/subprocess.html
#
# Doc: documents/04-quality/audits/meta/2026-05-14-wave-75-hook-coverage-baseline.md
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

# Verify coverage is available (user-local install OK).
if ! python3 -m coverage --version >/dev/null 2>&1; then
    echo "ERROR: 'coverage' Python package not installed." >&2
    echo "Install: pip install --user coverage" >&2
    echo "Or:      apt install python3-coverage" >&2
    exit 2
fi

# Set up subprocess-coverage shim: a sitecustomize.py that auto-starts coverage
# in every Python child process. Place it in a temp dir and prepend to PYTHONPATH.
SHIM_DIR=$(mktemp -d)
trap 'rm -rf "$SHIM_DIR"' EXIT
cat > "$SHIM_DIR/sitecustomize.py" <<'PY'
import coverage
coverage.process_startup()
PY

export COVERAGE_PROCESS_START="$REPO_ROOT/.coveragerc"
export PYTHONPATH="$SHIM_DIR${PYTHONPATH:+:$PYTHONPATH}"

echo "=== coverage erase ==="
python3 -m coverage erase

# Each test suite runs under coverage. `--parallel-mode` + `--append` keeps
# distinct .coverage.* data files (one per suite + one per subprocess) which
# `combine` merges below.

run_suite() {
    local name=$1
    local path=$2
    if [ ! -f "$path" ]; then
        echo "=== SKIP ${name} (file not present: ${path}) ==="
        return 0
    fi
    echo "=== Running ${name} ==="
    # `|| true` lets us collect coverage even if a few tests fail; final exit
    # status driven by coverage report success.
    python3 -m coverage run --parallel-mode "$path" || echo "WARN: ${name} had failures (coverage still collected)"
}

run_suite "test-audit-gate"        ".claude/hooks/tests/test-audit-gate.py"
run_suite "test-pre-tool-guard"    ".claude/hooks/tests/test-pre-tool-guard.py"
run_suite "test-post-tool-guard"   ".claude/hooks/tests/test-post-tool-guard.py"
run_suite "test-stop-handoff-check" ".claude/hooks/tests/test-stop-handoff-check.py"
run_suite "test-inject-rule-digest" ".claude/hooks/tests/test-inject-rule-digest.py"

# Parallel-buckets (Wave 75 Bucket C / D) — run if present.
for extra in test-hook-ordering test-concurrent-fire; do
    run_suite "${extra}" ".claude/hooks/tests/${extra}.py"
done

echo "=== coverage combine ==="
python3 -m coverage combine

echo "=== coverage report ==="
python3 -m coverage report

if [ "${1:-}" = "--html" ]; then
    python3 -m coverage html
    echo "HTML report → .claude/hooks/tests/coverage-html/index.html"
fi
if [ "${1:-}" = "--xml" ]; then
    python3 -m coverage xml
    echo "XML report → .claude/hooks/tests/coverage.xml"
fi
