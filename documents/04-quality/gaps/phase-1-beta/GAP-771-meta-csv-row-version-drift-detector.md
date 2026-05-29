---
audience: dev
---

# GAP-771 — META rules-index.csv Version field drift detector

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (META — recurrence tracking; rule extension defer until ≥2 recurrence)
**Domain:** Meta governance (CI validator)
**Found:** 2026-05-27 (Wave 106 PR #1897 meta-audit retroactive — finding M1)
**Affects:** Trust of `rules-index.csv` canonical metadata; CI enforcement gap
**Phase:** phase-1-beta

## Problem

Wave 106 PR #1897 included rule edit `vn-localization-audit-checklist.md` v1.0.0 → v1.1.0 (added §5 data roundtrip preservation). Frontmatter Version + Last-Reviewed updated correctly. BUT `rules-index.csv` row line 85 still showed `MANDATORY,1.0.0,2026-05-19,2026-05-19` — drift between rule frontmatter and CSV cache.

Discovery via retroactive meta-audit. Drift survived CI: `scripts/check-rules-index-csv.sh` validates `file existence` (every rule file has CSV row + file at expected path) but does NOT validate `Version/Last-Reviewed match between markdown frontmatter and CSV row`.

`meta-csv-index-pattern.md` §5 mandates "CSV beats markdown frontmatter" for canonical fields (Version, Last-Reviewed). But validator doesn't enforce this match — drift can silently land.

## Root Cause

`scripts/check-rules-index-csv.sh` validator scope:
- ✅ CSV well-formed (header + valid enums)
- ✅ Every CSV row's `file` column points to existing rule
- ✅ Every rule file has CSV row (100% coverage)
- ❌ NOT validate: CSV row Version matches `**Version:**` field trong rule frontmatter
- ❌ NOT validate: CSV row Last-Reviewed matches `**Last-Reviewed:**` field

Same gap exists cho `check-adrs-index-csv.sh` + `check-audits-index-csv.sh` (mirror pattern per `meta-csv-index-pattern.md`).

## Proposed Fix (defer until ≥2 recurrence per `incident-to-rule-pipeline.md` §3.1)

### Recurrence threshold gate

Per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions:
- **Detector complexity:** moderate — parse markdown frontmatter regex (`**Version:** N.N.N` + `**Last-Reviewed:** YYYY-MM-DD`) + diff against CSV columns. ~30-50 LOC bash.
- **Recurrence count:** 1 (Wave 106 PR #1897 this incident)
- **FP risk:** Low — Version + Last-Reviewed are structured fields, regex parse deterministic
- **Decision:** Defer detector wiring ≥7 ngày + ≥2 recurrence. Track via this gap.

### If recurrence ≥2 confirmed

Extend `scripts/check-rules-index-csv.sh` with new check function:

```bash
# Pseudocode
for each rule file in .claude/rules/*.md:
  md_version=$(grep -oP '^\*\*Version:\*\* \K[\d.]+' "$file")
  md_last_reviewed=$(grep -oP '^\*\*Last-Reviewed:\*\* \K\d{4}-\d{2}-\d{2}' "$file")
  csv_version=$(awk -F',' -v rule="$(basename "$file" .md)" '$1 == rule { print $3 }' rules-index.csv)
  csv_last_reviewed=$(awk -F',' -v rule="$(basename "$file" .md)" '$1 == rule { print $5 }' rules-index.csv)
  if [ "$md_version" != "$csv_version" ]; then
    echo "FAIL: $file Version drift — frontmatter $md_version vs CSV $csv_version"
  fi
  if [ "$md_last_reviewed" != "$csv_last_reviewed" ]; then
    echo "FAIL: $file Last-Reviewed drift — frontmatter $md_last_reviewed vs CSV $csv_last_reviewed"
  fi
done
```

Sister extensions cho `check-adrs-index-csv.sh` + `check-audits-index-csv.sh`.

### Alternative — rule-change-process.md §3 reinforcement (defer)

Could extend `rule-change-process.md` §3 frontmatter required fields với mandate "any version bump → CSV row sync TRONG SAME DIFF". Reviewer-checklist line.

## Acceptance Criteria

- [ ] Track recurrence count via comments on this gap
- [ ] If recurrence ≥2 (this + 1 more) → extend `check-rules-index-csv.sh` với Version/Last-Reviewed drift check
- [ ] Sister extensions cho ADRs + audits validators if scope applies
- [ ] PR template Output Review row update (mention drift detector active)
- [ ] Reviewer-checklist line added to `meta-csv-index-pattern.md` §8.2

## Related

- Wave 106 PR #1897 — meta-audit retroactive M1 finding
- Sister rule: `meta-csv-index-pattern.md` v1.0.2 §3 + §5 (CSV-canonical mandate)
- Sister rule: `rule-change-process.md` §3 frontmatter spec
- Pipeline: `incident-to-rule-pipeline.md` §3.1 premature-rule guard + recurrence threshold

## Log

- **2026-05-27 (OPEN):** Wave 106 PR #1897 meta-audit retroactive surfaced M1 finding — `vn-localization-audit-checklist` frontmatter v1.1.0 nhưng CSV row v1.0.0. Drift survived CI. Filed track recurrence per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence count 1, need ≥2 trước khi extend detector).
