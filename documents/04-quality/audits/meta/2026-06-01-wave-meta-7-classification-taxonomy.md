# Wave meta-7 — Classification Taxonomy + Agent Prompt Template

**Date:** 2026-06-01
**Wave:** meta-7
**Bucket:** 0 Foundation (sequential FIRST, MUST merge before agent spawn)
**Purpose:** Standardize 4 parallel agent output format for 172-gap stale-status audit

---

## 1. Classification Taxonomy (5 verdicts)

Each gap audited gets exactly ONE verdict:

| Verdict | Trigger | CSV update | File move |
|---|---|---|---|
| `SHIPPED→DONE` | Code fix shipped (grep evidence + commit ref) AND ≥80% AC checkboxes met | `status=DONE`, `completion_pct=100`, `last_verified=2026-06-01` | `git mv phase-X/GAP-NNN-*.md → phase-X/closed/` |
| `PARTIAL→adjust_pct` | Code partial shipped; current `completion_pct` inaccurate | `status=PARTIAL`, `completion_pct=<new>`, `last_verified=2026-06-01`, `notes=<≤80char>` | NO move |
| `OPEN→keep` | No code fix shipped yet; CSV correct | `last_verified=2026-06-01` only (refresh) | NO move |
| `SCOPE-REVISE` | Gap description outdated/misdiagnosis OR symptom no longer reproducible | `notes=SCOPE-REVISE: <reason>`, `last_verified=2026-06-01` | NO move; flag for next session re-write |
| `DROP` | Gap genuinely obsolete (feature deprecated/superseded) | `status=WONTFIX`, `notes=DROPPED: <rationale>`, `last_verified=2026-06-01` | NO move |

---

## 2. State-Check Methodology (per `audit-to-gap-pipeline.md` §2.8)

For each gap, agent runs:

### Step 1 — Read gap file
```bash
cat documents/04-quality/gaps/phase-1-beta/GAP-NNN-*.md  # OR closed/, unclassified/
```
Extract: Problem statement, Acceptance Criteria checkboxes, code locations referenced.

### Step 2 — Empirical state-check
For each code location/symbol mentioned in gap file:
```bash
grep -rn "<symbol>" <path> --include="*.java" --include="*.ts" --include="*.tsx"
```

Check for "GAP-NNN" cross-references in code comments (indicates fix shipped + cross-flow sweep done).

### Step 3 — Check recent commits referencing this gap
```bash
git log --oneline --all --grep="GAP-NNN" | head -5
```

If commits exist referencing this GAP-NNN with `fix(...)` or `feat(...)` prefix → likely SHIPPED.

### Step 4 — AC checkbox state check
Count `- [x]` vs `- [ ]` in gap file Acceptance Criteria section.

### Step 5 — Verdict decision matrix

| Code state | AC state | Verdict |
|---|---|---|
| Symbol exists + fix commits exist | ≥80% `- [x]` | `SHIPPED→DONE` |
| Symbol exists + fix commits exist | 30-79% `- [x]` | `PARTIAL→adjust_pct=50-80` |
| Symbol partially exists | <50% `- [x]` | `PARTIAL→adjust_pct=20-40` |
| Symbol does NOT exist | any | `OPEN→keep` |
| Gap description mentions feature now deprecated | n/a | `DROP` or `SCOPE-REVISE` |
| Gap description references symbols that don't exist | n/a | `SCOPE-REVISE` |

---

## 3. Agent Output Format (MANDATORY)

Each agent writes output to `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-{X}-{name}.md` with this structure:

```markdown
# Wave meta-7 Bucket {X} — {Scope description}

**Date:** 2026-06-01
**Agent:** Opus 4.7 background
**Gap count:** {N} from `bucket-{x}-*.txt`

## Verdict Summary

| Verdict | Count |
|---|---|
| SHIPPED→DONE | N |
| PARTIAL→adjust_pct | N |
| OPEN→keep | N |
| SCOPE-REVISE | N |
| DROP | N |

## Per-gap verdicts

### GAP-NNN — <title from gap file>

- **Verdict:** SHIPPED-DONE
- **Evidence:**
  - Commits: `<sha> fix(scope): description (#PR)`
  - Code: `path/to/file.java:line` symbol `xxx` present
  - AC: 4/4 checkboxes `- [x]`
- **New completion_pct:** 100
- **New notes:** Wave NN PR #NNNN shipped; IT verified; sister sweep clean

### GAP-NNN — <title>

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:** ...
- **New completion_pct:** 60
- **New notes:** Phase 1 BE shipped; FE consumer pending Wave N+1

...

## CSV update commands (coordinator applies in closure PR)

```bash
# SHIPPED→DONE entries
sed -i 's|^GAP-NNN,.*|GAP-NNN,phase-1-beta/closed/GAP-NNN-foo.md,...,DONE,...,100,...,2026-06-01,...|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-NNN-foo.md documents/04-quality/gaps/phase-1-beta/closed/

# PARTIAL adjustments
sed -i 's|...|...|' documents/04-quality/gaps/gap-status.csv
```
```

---

## 4. Banned shortcuts (per `audit-to-gap-pipeline.md` §2.5/§2.8)

| ❌ Don't | ✅ Do |
|---|---|
| `\| head` truncation on grep | Read FULL output |
| Trust gap description without empirical state-check | Run grep + git log + AC count |
| Single grep on entry name | Multi-pattern: class + method + i18n key + DB table |
| Flip DONE based on commits alone | Commits AND AC ≥80% AND no @Disabled tests |
| Mark SHIPPED-DONE if only Bucket A of multi-bucket gap shipped | Multi-bucket gap = PARTIAL unless ALL buckets shipped |
| Skip cross-flow sister sweep verification | Sister sweep evidence per `cross-flow-bug-class-sweep.md` §3 required for class-based gaps (cache/native-query/etc.) |

---

## 5. Agent prompt template (coordinator uses for spawn)

```
Task: Audit {N} gaps from bucket-{x}-*.txt to detect CSV stale-status drift.

Gap list: documents/04-quality/audits/meta/wave-meta-7-gap-lists/bucket-{x}-*.txt
Output: documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-{x}-{name}.md

Process per gap (per Section 2 methodology):
1. Read gap file at documents/04-quality/gaps/{phase}/GAP-NNN-*.md
2. Empirical state-check: grep symbols mentioned in gap file
3. Check git log --grep="GAP-NNN" for fix commits
4. Count AC checkboxes (- [x] vs - [ ])
5. Apply Section 2 Step 5 decision matrix
6. Emit verdict + evidence per Section 3 format

Rules:
- NO `| head` truncation on grep (Section 4 banned)
- Multi-pattern grep (class + method + DB table)
- Check @Disabled tests if AC mentions IT
- Verify cross-flow sweep evidence for class-based bugs (cache/native-query/auth-gap)
- Output to assigned audit artifact path; do NOT touch gap-status.csv (coordinator applies)
- Do NOT touch gap files (no description rewrite — flag SCOPE-REVISE if needed)
- Vietnamese narrative + English identifier per `dev-readable-doc-language.md`

Estimated time: ~30-45 min per bucket (parallel). Background spawn with model=opus.
```

---

## 6. Coordinator merge step (after all 4 buckets ship)

```bash
# Read all 4 audit artifacts
ls documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-*.md

# Apply CSV updates (manual sed batches OR helper script)
# For each SHIPPED→DONE gap:
#   1. sed CSV row: status,completion_pct,filename,last_verified
#   2. git mv file → closed/
# For each PARTIAL adjustment: sed CSV row only
# For SCOPE-REVISE: sed notes only
# For DROP: sed status→WONTFIX

# Run local CI parity
bash scripts/check-gap-status-csv.sh
bash scripts/check-gap-folder-location.sh
bash scripts/check-wave-plan-completeness.sh

# Closure PR per wave-closure-scope-completeness.md §3
gh pr create --title "chore(wave-meta-7-closure): apply 172-gap stale-audit verdicts + Scope-Completeness Reconciliation"
```

---

## 7. Audits-index.csv row (paired same PR)

Per `meta-csv-index-pattern.md` 100% coverage parity:

```csv
AUDIT-2026-06-01-wave-meta-7,2026-06-01-wave-meta-7-classification-taxonomy.md,meta,172,Wave meta-7 P0+P1 stale-status audit foundation taxonomy
```

(Bucket A-D audit artifacts each get separate row when their PR ships.)
