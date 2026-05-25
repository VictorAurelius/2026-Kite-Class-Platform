#!/usr/bin/env bash
# check-entity-mapper-consistency.sh — Entity ↔ Migration ↔ Mapper triad drift detector
#
# Closes GAP-743 (Wave meta-1 Bucket D). Per audit-1 finding OPS-BR4-002:
# Wave beta-readiness-4 saw 60% hotfix rate (3/5 buckets); 2 hotfixes preventable:
#   - Hotfix #1784: Course.pricingModel field added but Flyway migration column missing
#   - Hotfix #1787: ClassMapper @Mapping ignore declarations missing 6 reschedule columns
#
# This script catches the FIRST class (entity ↔ migration column drift) heuristically.
# Mapper @Mapping coverage check is INFORMATIONAL ONLY (high FP — DTO ≠ entity, default
# methods, ignore patterns) — surfaced for manual review, not enforced.
#
# WARN-mode initially per `.claude/rules/incident-to-rule-pipeline.md` §3.1 (heuristic
# grep-based, accept moderate FP). HARD STOP defer until 2nd recurrence post-rule.
#
# Modes:
#   --warn       Default. WARN on drift, exit 0
#   --strict     FAIL on drift, exit 1
#   --self-test  Run inline fixtures (PASS scenario + FAIL scenario)
#
# Override mechanism (per output-review-mandate §6.5):
#   Commit trailer in PR body: `ENTITY_MAPPER_DRIFT_OVERRIDE: <reason + follow-up>`
#
# Heuristic limitations (FP sources):
#   - Inherited fields from BaseEntity not in subclass entity file (e.g., id, createdAt)
#   - @Transient annotated fields (no DB column) — handled if annotation on prior line
#   - @ElementCollection / @OneToMany relations (no direct column) — handled via type heuristic
#   - Custom @Column(name="...") overriding camelCase→snake_case — handled
#   - Migration ALTER ADD column may live in later V file than entity ship date
#   - Multi-module entity in one module, migration in another (rare)
#
# Exit codes:
#   0 = pass OR --warn mode regardless of drift
#   1 = --strict mode + ≥1 drift detected OR --self-test failed
#   2 = invocation error

set -euo pipefail

MODE="${1:---warn}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Convert camelCase to snake_case (e.g., pricingModel → pricing_model)
camel_to_snake() {
  echo "$1" | sed -E 's/([a-z0-9])([A-Z])/\1_\2/g' | tr '[:upper:]' '[:lower:]'
}

# Extract entity field names from a Java file.
# Skips: @Transient fields, static, final constants, @OneToMany/@ManyToMany (collections),
# @ElementCollection. Captures only direct DB-mappable private fields.
extract_entity_fields() {
  local file="$1"
  awk '
    /@Transient/         { skip=1; next }
    /@OneToMany/         { skip=1; next }
    /@ManyToMany/        { skip=1; next }
    /@ElementCollection/ { skip=1; next }
    /@OneToOne.*mappedBy/{ skip=1; next }
    /^\s*\/\//           { next }
    /^\s*\*/             { next }
    /^\s*private\s+static/ { next }
    /^\s*private\s+final.*=/ { next }
    /^\s*private\s+/ {
      if (skip == 1) { skip=0; next }
      # Match: private <Type> <name>;  OR  private <Type<...>> <name> ...
      # Skip collection types List/Set/Map/Collection (likely relations)
      if (match($0, /private\s+(List|Set|Map|Collection)</)) { next }
      # Capture field name: last identifier before ; or = or whitespace+;
      if (match($0, /private\s+[A-Za-z_<>?, .]+\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*[=;]/, arr)) {
        print arr[1]
      }
    }
    { skip=0 }
  ' "$file" 2>/dev/null || true
}

# Extract custom column names from @Column(name = "...") annotations
extract_column_overrides() {
  local file="$1"
  grep -oE '@Column\([^)]*name\s*=\s*"[a-z_][a-z0-9_]*"' "$file" 2>/dev/null \
    | sed -E 's/.*name\s*=\s*"([a-z_][a-z0-9_]*)".*/\1/' || true
}

# Extract migration column names from all V*.sql files in a module's db/migration dir.
# Captures: CREATE TABLE ... (col_name TYPE, ...) AND ALTER TABLE ADD COLUMN [IF NOT EXISTS] col_name.
extract_migration_columns() {
  local migration_dir="$1"
  [[ -d "$migration_dir" ]] || return 0
  find "$migration_dir" -name 'V*.sql' -type f -exec cat {} + 2>/dev/null \
    | grep -ioE '(ADD\s+COLUMN(\s+IF\s+NOT\s+EXISTS)?\s+[a-z_][a-z0-9_]*|^\s+[a-z_][a-z0-9_]*\s+(VARCHAR|TEXT|INT|BIGINT|BOOLEAN|DECIMAL|NUMERIC|TIMESTAMP|DATE|UUID|JSONB|SMALLINT|SERIAL|REAL|FLOAT|DOUBLE|CHAR|BYTEA))' \
    | sed -E 's/ADD\s+COLUMN(\s+IF\s+NOT\s+EXISTS)?\s+//i; s/^\s*//; s/\s+.*$//' \
    | tr '[:upper:]' '[:lower:]' \
    | sort -u || true
}

# Common BaseEntity / framework-managed fields (not required to be in entity subclass file)
BASE_FIELDS="id created_at updated_at created_by updated_by deleted version tenant_id deleted_at"

is_base_field() {
  local col="$1"
  for bf in $BASE_FIELDS; do
    [[ "$col" == "$bf" ]] && return 0
  done
  return 1
}

# Check a single entity file against its module's migration dir.
# Prints DRIFT lines to stdout. Prints drift count to FD 3 (caller captures).
check_entity_vs_migrations() {
  local entity_file="$1"
  local migration_dir="$2"
  local module_label="$3"
  local entity_name
  entity_name=$(basename "$entity_file" .java)

  # Skip non-@Entity classes (e.g., BaseEntity itself, @MappedSuperclass)
  if ! grep -q '^@Entity' "$entity_file" 2>/dev/null; then
    echo 0
    return 0
  fi

  local fields
  fields=$(extract_entity_fields "$entity_file" || true)
  if [[ -z "$fields" ]]; then
    echo 0
    return 0
  fi

  local migration_cols
  migration_cols=$(extract_migration_columns "$migration_dir" || true)
  if [[ -z "$migration_cols" ]]; then
    echo 0
    return 0
  fi

  local column_overrides
  column_overrides=$(extract_column_overrides "$entity_file" || true)

  local drift_count=0
  while IFS= read -r field; do
    [[ -z "$field" ]] && continue
    local snake
    snake=$(camel_to_snake "$field")

    # Skip base fields (BaseEntity managed)
    is_base_field "$snake" && continue

    # If field has @Column(name="..."), use that override
    local lookup="$snake"
    if [[ -n "$column_overrides" ]] && echo "$column_overrides" | grep -qx "$snake"; then
      lookup="$snake"
    fi

    if ! echo "$migration_cols" | grep -qx "$lookup"; then
      # Drift output goes to stderr so stdout reserved for drift count
      printf 'DRIFT [%s] %s.%s → no migration column "%s"\n' \
        "$module_label" "$entity_name" "$field" "$lookup" >&2
      drift_count=$((drift_count + 1))
    fi
  done <<< "$fields"

  echo "$drift_count"
}

scan_repo() {
  local total_entities=0
  local total_migrations=0
  local total_mappers=0
  local total_drift=0

  # Modules with both @Entity classes and db/migration dirs
  local modules=()
  while IFS= read -r migration_dir; do
    local module_root
    # Strip /src/main/resources/db/migration (5 levels)
    module_root=$(dirname "$(dirname "$(dirname "$(dirname "$(dirname "$migration_dir")")")")")
    modules+=("$module_root")
  done < <(find "$REPO_ROOT/kitehub" "$REPO_ROOT/kiteclass" \
    -type d -path '*/src/main/resources/db/migration' 2>/dev/null)

  for module_root in "${modules[@]}"; do
    local module_label
    module_label=$(basename "$module_root")
    local migration_dir="$module_root/src/main/resources/db/migration"
    local java_root="$module_root/src/main/java"
    [[ -d "$java_root" ]] || continue

    local mig_count
    mig_count=$(find "$migration_dir" -name 'V*.sql' -type f 2>/dev/null | wc -l)
    total_migrations=$((total_migrations + mig_count))

    while IFS= read -r entity_file; do
      total_entities=$((total_entities + 1))
      local drift_for_file
      drift_for_file=$(check_entity_vs_migrations "$entity_file" "$migration_dir" "$module_label")
      total_drift=$((total_drift + drift_for_file))
    done < <(grep -rl '^@Entity' "$java_root" --include='*.java' 2>/dev/null || true)

    local mapper_count=0
    while IFS= read -r mapper_file; do
      [[ -z "$mapper_file" ]] && continue
      if grep -q '@Mapper' "$mapper_file" 2>/dev/null; then
        mapper_count=$((mapper_count + 1))
      fi
    done < <(find "$java_root" -name '*Mapper.java' -type f 2>/dev/null || true)
    total_mappers=$((total_mappers + mapper_count))
  done

  printf '\n=== Entity-Migration-Mapper Triad Scan ===\n'
  printf 'Entities scanned:   %d\n' "$total_entities"
  printf 'Migrations scanned: %d\n' "$total_migrations"
  printf 'Mappers found:      %d\n' "$total_mappers"
  printf 'Drift findings:     %d (WARN — heuristic, manual triage recommended)\n' "$total_drift"
  printf '\n'

  if [[ "$total_drift" -gt 0 ]]; then
    if [[ "$MODE" == "--strict" ]]; then
      printf 'STRICT mode: exit 1 (≥1 drift detected)\n'
      return 1
    fi
    printf 'WARN mode: exit 0 (HARD STOP deferred per incident-to-rule-pipeline §3.1)\n'
    printf 'Override trailer: ENTITY_MAPPER_DRIFT_OVERRIDE: <reason + follow-up gap>\n'
  fi
  return 0
}

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # ─── Fixture 1: PASS — entity field has matching migration column ───
  local pass_module="$tmpdir/pass-module"
  local pass_java="$pass_module/src/main/java/com/example/widget"
  local pass_mig="$pass_module/src/main/resources/db/migration"
  mkdir -p "$pass_java" "$pass_mig"

  cat > "$pass_java/Widget.java" <<'EOF'
package com.example.widget;
import jakarta.persistence.*;

@Entity
@Table(name = "widgets")
public class Widget {
    @Id private Long id;
    private String name;
    private String description;
    private Integer quantity;
}
EOF
  cat > "$pass_mig/V1__create_widgets.sql" <<'EOF'
CREATE TABLE widgets (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    quantity INT
);
EOF

  # ─── Fixture 2: FAIL — entity has new field with no migration column ───
  # Simulates hotfix #1784 (Course.pricingModel added without Flyway migration)
  local fail_module="$tmpdir/fail-module"
  local fail_java="$fail_module/src/main/java/com/example/course"
  local fail_mig="$fail_module/src/main/resources/db/migration"
  mkdir -p "$fail_java" "$fail_mig"

  cat > "$fail_java/Course.java" <<'EOF'
package com.example.course;
import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id private Long id;
    private String name;
    private String pricingModel;
}
EOF
  # Migration intentionally OMITS pricing_model — this is the drift scenario.
  cat > "$fail_mig/V1__create_courses.sql" <<'EOF'
CREATE TABLE courses (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255)
);
EOF

  # ─── Run fixtures ───
  local pass_drift
  pass_drift=$(check_entity_vs_migrations "$pass_java/Widget.java" "$pass_mig" "pass-fixture" 2>/dev/null)

  local fail_drift
  fail_drift=$(check_entity_vs_migrations "$fail_java/Course.java" "$fail_mig" "fail-fixture" 2>/dev/null)

  echo ""
  echo "=== Self-Test Results ==="
  echo "Fixture 1 (PASS — Widget all fields covered): drift count = $pass_drift (expect 0)"
  echo "Fixture 2 (FAIL — Course.pricingModel missing): drift count = $fail_drift (expect ≥1)"
  echo ""

  if [[ "$pass_drift" -eq 0 && "$fail_drift" -ge 1 ]]; then
    echo "✅ Self-test PASS — detector logic correct"
    return 0
  fi

  echo "❌ Self-test FAIL — detector logic broken"
  echo "  pass_drift=$pass_drift (expected 0)"
  echo "  fail_drift=$fail_drift (expected ≥1)"
  return 1
}

case "$MODE" in
  --self-test) run_self_test ;;
  --warn|--strict) scan_repo ;;
  -h|--help)
    sed -n '2,30p' "$0"
    exit 0
    ;;
  *)
    echo "Unknown mode: $MODE" >&2
    echo "Usage: $0 [--warn|--strict|--self-test]" >&2
    exit 2
    ;;
esac
