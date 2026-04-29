#!/usr/bin/env bash
# test-session-lock-guard.sh — smoke test for .claude/hooks/session-lock-guard.py
#
# Scenarios:
#   1. Foreign active lock on current branch  → exit 1 (BLOCK)
#   2. Only own lock                          → exit 0 (PASS)
#   3. Foreign lock, but stale (>4h old)      → exit 0 (PASS, lock auto-purged)
#
# Run from repo root or anywhere — script computes paths from its own location.

set -u

# Resolve repo root (script lives at .claude/hooks/tests/<this>).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
GUARD="$REPO_ROOT/.claude/hooks/session-lock-guard.py"
LOCK_DIR="$REPO_ROOT/.claude/session-locks"

if [[ ! -x "$GUARD" && ! -f "$GUARD" ]]; then
    echo "FAIL: guard script not found at $GUARD" >&2
    exit 1
fi

# Pin a known session_id + branch for deterministic results.
export CLAUDE_SESSION_ID="test-session-A-$$"
TEST_BRANCH="test/session-lock-guard-fixture"

# Pre-test: stash any existing locks so we don't clobber real state.
STASH_DIR="$LOCK_DIR/.test-stash-$$"
mkdir -p "$LOCK_DIR"
mkdir -p "$STASH_DIR"
if compgen -G "$LOCK_DIR/*.lock" > /dev/null; then
    mv "$LOCK_DIR"/*.lock "$STASH_DIR/" 2>/dev/null || true
fi

# Provide a stable branch for the guard to read.
# We override `git branch --show-current` by running the guard inside a tiny
# wrapper that stages a temporary git repo? Too heavy. Instead, we use a
# helper that injects branch via PATH shim.
SHIM_DIR="$(mktemp -d)"
cat > "$SHIM_DIR/git" <<EOF
#!/usr/bin/env bash
if [[ "\$1" == "branch" && "\$2" == "--show-current" ]]; then
    echo "$TEST_BRANCH"
    exit 0
fi
exec /usr/bin/env -u PATH bash -c 'PATH=/usr/bin:/bin exec git "\$@"' _ "\$@"
EOF
chmod +x "$SHIM_DIR/git"
ORIG_PATH="$PATH"
export PATH="$SHIM_DIR:$PATH"

# shellcheck disable=SC2317  # all statements run via trap; shellcheck flow-analysis can't see that
cleanup() {
    # Remove fixture locks, restore stashed locks, drop shim.
    rm -f "$LOCK_DIR"/test-fixture-*.lock 2>/dev/null || true
    if compgen -G "$STASH_DIR/*.lock" > /dev/null; then
        mv "$STASH_DIR"/*.lock "$LOCK_DIR/" 2>/dev/null || true
    fi
    rmdir "$STASH_DIR" 2>/dev/null || true
    rm -rf "$SHIM_DIR"
    export PATH="$ORIG_PATH"
}
trap cleanup EXIT

write_lock() {
    local fname="$1"
    local sid="$2"
    local started="$3"
    cat > "$LOCK_DIR/$fname" <<EOF
session_id: $sid
started: $started
branch: $TEST_BRANCH
gaps: [GAP-FIXTURE]
intent: "test fixture"
EOF
}

PASS=0
FAIL=0

assert_exit() {
    local expected="$1"
    local got="$2"
    local label="$3"
    if [[ "$expected" -eq "$got" ]]; then
        echo "  PASS — $label (exit $got)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $label: expected exit $expected, got $got" >&2
        FAIL=$((FAIL + 1))
    fi
}

# ── Scenario 1: foreign active lock on current branch → BLOCK (exit 1) ──
echo "Scenario 1 — foreign active lock"
rm -f "$LOCK_DIR"/test-fixture-*.lock
write_lock "test-fixture-foreign.lock" "test-session-B-other" "$(date -Iseconds)"
write_lock "test-fixture-own.lock" "$CLAUDE_SESSION_ID" "$(date -Iseconds)"
set +e
python3 "$GUARD" >/dev/null 2>&1
RC=$?
set -e
assert_exit 1 "$RC" "foreign lock blocks"

# ── Scenario 2: only own lock → PASS (exit 0) ──
echo "Scenario 2 — only own lock"
rm -f "$LOCK_DIR"/test-fixture-*.lock
write_lock "test-fixture-own.lock" "$CLAUDE_SESSION_ID" "$(date -Iseconds)"
set +e
python3 "$GUARD" >/dev/null 2>&1
RC=$?
set -e
assert_exit 0 "$RC" "only own lock passes"

# ── Scenario 3: stale foreign lock (>4h) → auto-purge + PASS (exit 0) ──
echo "Scenario 3 — stale foreign lock auto-purged"
rm -f "$LOCK_DIR"/test-fixture-*.lock
write_lock "test-fixture-stale.lock" "test-session-C-stale" "2020-01-01T00:00:00+00:00"
# touch -t YYYYMMDDhhmm — 5 hours ago
STALE_STAMP="$(date -d '5 hours ago' +%Y%m%d%H%M 2>/dev/null || date -v-5H +%Y%m%d%H%M)"
touch -t "$STALE_STAMP" "$LOCK_DIR/test-fixture-stale.lock"
set +e
python3 "$GUARD" 2>/dev/null
RC=$?
set -e
assert_exit 0 "$RC" "stale foreign lock auto-purged"
# Verify the file is gone after purge.
if [[ -f "$LOCK_DIR/test-fixture-stale.lock" ]]; then
    echo "  FAIL — stale lock should have been deleted but still exists" >&2
    FAIL=$((FAIL + 1))
else
    echo "  PASS — stale lock file removed"
    PASS=$((PASS + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
if [[ "$FAIL" -ne 0 ]]; then
    exit 1
fi
exit 0
