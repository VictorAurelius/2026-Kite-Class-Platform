#!/usr/bin/env bash
# check-rules-index-csv.sh — validate rules-index.csv ↔ rule file consistency
#
# Per `.claude/rules/meta-csv-index-pattern.md`: CSV is canonical for rule
# enumeration (name, priority, version, dates pointer). Every rule file MUST
# have a CSV row; every CSV row MUST point to an existing rule file.
#
# Schema: name,priority,version,created,last_reviewed,file,path_trigger
# Enums: CRITICAL | MANDATORY | ADVISORY
# path_trigger column added Wave 73 Bucket 0 (2026-05-14): comma-separated globs
#   for native Anthropic `paths:` frontmatter scoping. Quoted-CSV cells used when
#   value contains a comma (multi-glob). Empty = no path-scope (auto-load).
#
# Note: this validator covers the INDEX layer only. Rule frontmatter content
# (Last-Reviewed ≤ today, etc.) is enforced by `scripts/check-rule-frontmatter.sh`.
#
# Exit codes:
#   0 — pass
#   1 — coverage gap or file missing
#   2 — malformed CSV / invalid enum
#   3 — duplicate row

set -euo pipefail

CSV=".claude/rules/rules-index.csv"
RULES_DIR=".claude/rules"

VALID_PRIORITIES="CRITICAL MANDATORY ADVISORY"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

# Use Python csv module for robust quoted-CSV parsing (handles 7th column path_trigger
# with quoted multi-glob values containing commas).

python3 - "$CSV" "$RULES_DIR" <<'PYEOF'
import csv
import os
import sys

csv_path = sys.argv[1]
rules_dir = sys.argv[2]

valid_priorities = {"CRITICAL", "MANDATORY", "ADVISORY"}
errors = 0
seen_names = set()
records = []

with open(csv_path, newline="") as f:
    reader = csv.reader(f)
    for row in reader:
        if not row or row[0].startswith("#") or row[0] == "name":
            continue
        records.append(row)

print(f"Checking {len(records)} rule CSV rows...")

import re
date_re = re.compile(r"^\d{4}-\d{2}-\d{2}$")
ver_re = re.compile(r"^\d+\.\d+(\.\d+)?$")

for row in records:
    if len(row) < 6:
        print(f"FAIL: row too short: {row}")
        errors += 1
        continue
    if len(row) > 7:
        print(f"FAIL: row too long (expected 6 or 7 cols): {row}")
        errors += 1
        continue

    name, priority, version, created, reviewed, file_field = row[:6]
    # path_trigger is row[6] when present (Wave 73 Bucket 0+); empty allowed
    path_trigger = row[6] if len(row) >= 7 else ""

    if not name or not name[0].islower():
        continue

    if name in seen_names:
        print(f"FAIL: duplicate rule name {name} in CSV")
        errors += 1
        continue
    seen_names.add(name)

    file_path = os.path.join(rules_dir, file_field)
    if not os.path.isfile(file_path):
        print(f"FAIL: {name} — file not found ({file_field})")
        errors += 1
        continue

    if priority not in valid_priorities:
        print(f"FAIL: {name} — invalid priority '{priority}' (allowed: {sorted(valid_priorities)})")
        errors += 1

    if not ver_re.match(version):
        print(f"FAIL: {name} — bad version '{version}' (expect X.Y.Z)")
        errors += 1

    if not date_re.match(created):
        print(f"FAIL: {name} — bad created date '{created}'")
        errors += 1
    if not date_re.match(reviewed):
        print(f"FAIL: {name} — bad last_reviewed date '{reviewed}'")
        errors += 1

    if date_re.match(created) and date_re.match(reviewed) and reviewed < created:
        print(f"FAIL: {name} — last_reviewed ({reviewed}) before created ({created})")
        errors += 1

# Coverage check: every rule .md file (excluding README.md) has a CSV row
csv_files = {row[5] for row in records if len(row) >= 6 and row[0] and row[0][0].islower()}
EXCLUDE = {"README.md"}
for fname in os.listdir(rules_dir):
    if not fname.endswith(".md") or fname in EXCLUDE:
        continue
    full = os.path.join(rules_dir, fname)
    if not os.path.isfile(full):
        continue
    if fname not in csv_files:
        print(f"FAIL: {fname} missing CSV row (100%-coverage mode)")
        errors += 1

if errors:
    print(f"FAIL: {errors} error(s)")
    sys.exit(1)

print(f"PASS: {len(records)} rule rows validated")
PYEOF
