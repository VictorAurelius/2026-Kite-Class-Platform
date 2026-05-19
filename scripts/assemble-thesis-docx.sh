#!/usr/bin/env bash
# Thesis DOCX assembly script — Wave 100.7 Phase 3b validation mode
#
# Status: V1 = dry-run validation. Validates the thesis-report Java pipeline
# is wired correctly and pre-conditions exist. Production execute mode (full
# chapter content assembly + JAR generation) defers to a follow-up gap with
# Spring Boot CLI runner scope.
#
# Decision doc: documents/08-thesis/docx-pipeline-scoping.md
# Parent gap:   documents/04-quality/gaps/phase-1-beta/GAP-646-thesis-docx-pipeline.md
# Pipeline:     kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/docx/
#                 ThesisReportBuilder.java (Create pipeline, programmatic skeleton build)
#
# Usage:
#   scripts/assemble-thesis-docx.sh                  # dry-run validation (default)
#   scripts/assemble-thesis-docx.sh --dry-run        # explicit dry-run
#   scripts/assemble-thesis-docx.sh --execute        # NOT YET IMPLEMENTED (see follow-up gap)
#   scripts/assemble-thesis-docx.sh --help           # usage

set -euo pipefail

SCRIPT_NAME=$(basename "$0")
REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
THESIS_DIR="$REPO_ROOT/documents/08-thesis"
BUILDER_JAVA="$REPO_ROOT/kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/docx/ThesisReportBuilder.java"
BUILDER_TEST="$REPO_ROOT/kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/document/docx/ThesisReportBuilderTest.java"
CHAPTER_MAP="$THESIS_DIR/chapter-mapping.md"
BIBLIOGRAPHY="$THESIS_DIR/references/bibliography.md"
DECISION_DOC="$THESIS_DIR/docx-pipeline-scoping.md"

MODE="dry-run"
if [[ ${1:-} == "--help" ]]; then
  sed -n '2,18p' "$0" | sed 's/^# \?//'
  exit 0
fi
if [[ ${1:-} == "--execute" ]]; then
  cat <<EOF
[$SCRIPT_NAME] --execute mode NOT YET IMPLEMENTED.

Production execute requires:
- Spring Boot CLI runner in kiteclass-core (new ThesisAssemblyCLI main class)
- Chapter MD → XWPF body parser (V2 — V1 takes plain-text body via data map)
- Figure injection + numbering (V2)
- Bibliography text formatter from bibliography.md (V2)

Track follow-up via new gap referencing GAP-646. For V1 verification, use:
  scripts/assemble-thesis-docx.sh --dry-run

To exercise ThesisReportBuilder directly:
  cd kiteclass/kiteclass-core && ./mvnw test -Dtest=ThesisReportBuilderTest
EOF
  exit 1
fi
if [[ ${1:-} == "--dry-run" ]]; then
  MODE="dry-run"
fi

echo "[$SCRIPT_NAME] Mode: $MODE"
echo "[$SCRIPT_NAME] Repo root: $REPO_ROOT"
echo

FAIL=0
check() {
  local label="$1"
  local path="$2"
  if [[ -f "$path" ]]; then
    local size
    size=$(wc -c <"$path" 2>/dev/null || echo "?")
    echo "  ✅ $label: $path ($size bytes)"
  else
    echo "  ❌ MISSING $label: $path"
    FAIL=$((FAIL + 1))
  fi
}

echo "[$SCRIPT_NAME] Pre-condition check (Wave 100.7 Phase 3b artifacts):"
check "Java builder" "$BUILDER_JAVA"
check "Java test" "$BUILDER_TEST"
check "Chapter mapping" "$CHAPTER_MAP"
check "Bibliography" "$BIBLIOGRAPHY"
check "Scoping doc" "$DECISION_DOC"

echo
echo "[$SCRIPT_NAME] Phase 3b pipeline status:"
if [[ $FAIL -eq 0 ]]; then
  echo "  ✅ All artifacts present — ThesisReportBuilder pipeline ready for V1 execute mode (deferred)."
  echo "  ℹ️  Builder = Create pipeline (programmatic skeleton; no binary template needed)."
  echo "  ℹ️  Builder route: templateId='thesis-report' → ThesisReportBuilder.build(request)"
  echo "  ℹ️  Required data keys: title, studentName, studentId, supervisor, year, school"
  echo "  ℹ️  Optional: chapter.N.title, chapter.N.body, bibliography.entries"
  echo "  ℹ️  See decision doc: $DECISION_DOC"
  exit 0
else
  echo "  ❌ $FAIL pre-condition(s) failed — fix above before execute mode wires up."
  exit 1
fi
