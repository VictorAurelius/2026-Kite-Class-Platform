#!/usr/bin/env bash
# run-all.sh — chạy tất cả script tests trong scripts/tests/test-*.sh sequentially.
#
# Exit 0 nếu toàn bộ pass; exit 1 nếu ≥1 test suite fail.
# Per Wave 76 Bucket B — wired to CI job `script-tests` trong script-quality.yml.
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
TESTS_DIR="$REPO_ROOT/scripts/tests"

# Collect test scripts (sorted for deterministic output)
TEST_SCRIPTS=()
while IFS= read -r -d '' f; do
    TEST_SCRIPTS+=("$f")
done < <(find "$TESTS_DIR" -maxdepth 1 -name "test-*.sh" -type f -print0 | sort -z)

if [ "${#TEST_SCRIPTS[@]}" = "0" ]; then
    echo "No test scripts found in $TESTS_DIR"
    exit 1
fi

TOTAL_SUITES=${#TEST_SCRIPTS[@]}
PASSED_SUITES=0
FAILED_SUITES=0
FAILED_NAMES=()

echo "Running $TOTAL_SUITES test suite(s)..."
echo ""

for script in "${TEST_SCRIPTS[@]}"; do
    name=$(basename "$script")
    echo "---"
    set +e
    bash "$script"
    rc=$?
    set -e
    if [ "$rc" = "0" ]; then
        PASSED_SUITES=$((PASSED_SUITES + 1))
    else
        FAILED_SUITES=$((FAILED_SUITES + 1))
        FAILED_NAMES+=("$name")
    fi
    echo ""
done

echo "========================================="
echo "Summary: $PASSED_SUITES/$TOTAL_SUITES suites passed"
if [ "$FAILED_SUITES" -gt "0" ]; then
    echo "Failed suites:"
    for n in "${FAILED_NAMES[@]}"; do
        echo "  - $n"
    done
    exit 1
fi
echo "All suites PASS."
exit 0
