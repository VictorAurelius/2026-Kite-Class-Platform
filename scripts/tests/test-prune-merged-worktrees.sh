#!/usr/bin/env bash
# test-prune-merged-worktrees.sh — fixture tests cho scripts/prune-merged-worktrees.sh
#
# CRITICAL: chỉ chạy --dry-run mode trong git repo TẠM (mktemp). Không touch real
# branches/worktrees của repo gốc — tránh data loss.
#
# Tests in isolated temp git repo:
#   1. --help output: exit 0
#   2. --dry-run trên repo sạch (no merged branches, no husks): exit 0 + "Nothing to prune"
#   3. --dry-run trên repo có merged branch + .claude/worktrees/ husk: exit 0
#      reports counts, no mutation
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/prune-merged-worktrees.sh"
PASS=0
FAIL=0

# Create isolated test repo (cleanup at exit)
TESTDIR=$(mktemp -d)
trap 'rm -rf "$TESTDIR"' EXIT

assert_exit() {
    local name="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS — $name (exit=$actual)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $name (expected exit $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}

assert_contains() {
    local name="$1" needle="$2" haystack="$3"
    if echo "$haystack" | grep -qF "$needle"; then
        echo "  PASS — $name (found '$needle')"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $name (missing '$needle' in output)"
        FAIL=$((FAIL + 1))
    fi
}

setup_clean_repo() {
    local dir="$1"
    rm -rf "$dir" && mkdir -p "$dir"
    (
        cd "$dir"
        git init -q -b main
        git config user.email "test@example.com"
        git config user.name "Test"
        # Set up dummy origin to mirror main (script does `git fetch origin`)
        local origin="$TESTDIR/origin.git"
        rm -rf "$origin"
        git init -q --bare "$origin"
        git remote add origin "$origin"
        echo "init" > README.md
        git add README.md
        git commit -q -m "init"
        git push -q origin main >/dev/null 2>&1
        # Configure origin/main tracking
        git fetch -q origin >/dev/null 2>&1
    )
}

echo "=== test-prune-merged-worktrees ==="

# Test 1 — --help should exit 0
set +e
output=$(bash "$SCRIPT" --help 2>&1)
rc=$?
set -e
assert_exit "--help should exit 0" 0 "$rc"

# Test 2 — clean repo, --dry-run → exit 0 + "Nothing to prune"
REPO1="$TESTDIR/repo-clean"
setup_clean_repo "$REPO1"
set +e
output=$(cd "$REPO1" && bash "$SCRIPT" --dry-run 2>&1)
rc=$?
set -e
assert_exit "clean repo --dry-run should exit 0" 0 "$rc"
assert_contains "clean repo --dry-run should report 'Nothing to prune'" "Nothing to prune" "$output"

# Test 3 — repo with merged branch + husk worktree, --dry-run → exit 0 (no mutation)
REPO2="$TESTDIR/repo-dirty"
setup_clean_repo "$REPO2"
(
    cd "$REPO2"
    # Create a merged feature branch (merge to main + push so origin/main contains it)
    git checkout -q -b feature-merged
    echo "x" >> README.md
    git commit -q -am "feature"
    git checkout -q main
    git merge -q --no-ff feature-merged -m "merge"
    git push -q origin main >/dev/null 2>&1
    git fetch -q origin >/dev/null 2>&1
    # Create a worktree husk under .claude/worktrees/
    mkdir -p .claude/worktrees
    git worktree add -q ".claude/worktrees/agent-test" -b agent-test-branch >/dev/null 2>&1
) >/dev/null 2>&1

# Snapshot branch list BEFORE dry-run
before=$(cd "$REPO2" && git branch -a | sort)

set +e
output=$(cd "$REPO2" && bash "$SCRIPT" --dry-run 2>&1)
rc=$?
set -e
assert_exit "dirty repo --dry-run should exit 0" 0 "$rc"
assert_contains "dry-run should report detection summary" "Post-wave cleanup detection" "$output"
assert_contains "dry-run should report DRY-RUN suffix" "DRY-RUN" "$output"

# Verify NO MUTATION: branch list unchanged
after=$(cd "$REPO2" && git branch -a | sort)
if [ "$before" = "$after" ]; then
    echo "  PASS — dry-run did NOT mutate branches"
    PASS=$((PASS + 1))
else
    echo "  FAIL — dry-run MUTATED branches (before: $before; after: $after)"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
