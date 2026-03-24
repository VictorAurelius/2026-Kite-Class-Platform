#!/bin/bash
# test-kit.sh — Verify starter-kit integrity
#
# Chạy để đảm bảo kit hoàn chỉnh trước khi distribute
#
# Usage: ./test-kit.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PASS=0
FAIL=0

check() {
    local desc="$1"
    local condition="$2"
    if eval "$condition" > /dev/null 2>&1; then
        echo "  ✅ $desc"
        ((PASS++))
    else
        echo "  ❌ $desc"
        ((FAIL++))
    fi
}

echo "═══════════════════════════════════════════════"
echo "  Starter Kit Smoke Test"
echo "═══════════════════════════════════════════════"
echo ""

echo "📋 Required files:"
check "VERSION" "test -f VERSION"
check "CHANGELOG.md" "test -f CHANGELOG.md"
check "README.md" "test -f README.md"
check "init-project.sh" "test -f init-project.sh && test -x init-project.sh"
check "upgrade-project.sh" "test -f upgrade-project.sh && test -x upgrade-project.sh"
check "contribute.sh" "test -f contribute.sh && test -x contribute.sh"

echo ""
echo "📋 Skills (9 required):"
check "brainstorming-methodology" "test -f skills/core/brainstorming-methodology.md"
check "tdd-enforcement" "test -f skills/core/tdd-enforcement.md"
check "two-stage-code-review" "test -f skills/core/two-stage-code-review.md"
check "systematic-debugging" "test -f skills/core/systematic-debugging.md"
check "task-breakdown-guide" "test -f skills/core/task-breakdown-guide.md"
check "development-workflow" "test -f skills/workflow/development-workflow.md"
check "quality-audit" "test -f skills/quality/quality-audit.md"
check "business-docs-3-layer" "test -f skills/reference/business-docs-3-layer.md"
check "service-docs-standard" "test -f skills/reference/service-docs-standard.md"

echo ""
echo "📋 Scripts (3 required):"
check "check-ci.sh" "test -f scripts/check-ci.sh"
check "test-local.sh" "test -f scripts/test-local.sh"
check "pre-commit-check.sh" "test -f scripts/pre-commit-check.sh"

echo ""
echo "📋 Templates (2 required):"
check "CLAUDE.md.template" "test -f templates/CLAUDE.md.template"
check "README.md.template" "test -f templates/README.md.template"

echo ""
echo "📋 Seed memories (4 required):"
check "feedback_scripts_not_adhoc" "test -f memory/feedback_scripts_not_adhoc.md"
check "feedback_ci_before_scoring" "test -f memory/feedback_ci_before_scoring.md"
check "feedback_self_test_before_push" "test -f memory/feedback_self_test_before_push.md"
check "feedback_business_design_first" "test -f memory/feedback_business_design_first.md"

echo ""
echo "📋 Index + meta:"
check "Skills index" "test -f skills/_README-skills-index.md"
check ".gitignore" "test -f .gitignore"

echo ""
echo "📋 Content checks:"
check "No kiteclass references in skills" "! grep -ri 'kiteclass\|kitehub' skills/ --include='*.md'"
check "No hardcoded ports in skills" "! grep -rE ':(8080|8081|3000|5432)' skills/ --include='*.md'"
check "VERSION is semver" "grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$' VERSION"

echo ""
echo "📋 Init script test:"
TMPDIR=$(mktemp -d)
check "init-project.sh runs" "bash init-project.sh $TMPDIR > /dev/null 2>&1"
check "Creates .claude/skills/" "test -d $TMPDIR/.claude/skills/core"
check "Creates scripts/" "test -d $TMPDIR/scripts"
check "Creates CLAUDE.md" "test -f $TMPDIR/CLAUDE.md"
check "Tracks version" "test -f $TMPDIR/.claude/.starter-kit-version"
rm -rf "$TMPDIR"

echo ""
echo "═══════════════════════════════════════════════"
TOTAL=$((PASS + FAIL))
echo "  Results: ✅ $PASS/$TOTAL passed, ❌ $FAIL failed"
echo "═══════════════════════════════════════════════"

[ $FAIL -eq 0 ] && echo "  ✅ Kit is ready to distribute!" || echo "  ❌ Fix failures before distributing"
exit $FAIL
