# GAP-675: META-META audit `incident-to-rule-pipeline.md` premature-rule-guard usage

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 META-META
**Domain:** Meta (governance of governance)
**Found:** 2026-05-19 (Wave 98 META audit 3-agent outside-in — Failure-Mode Matrix systemic finding)
**Affects:** `incident-to-rule-pipeline.md` §3 premature-rule guard application across 6+ recent rules; project enforcement-parity health

## Problem

Wave 98 META audit Failure-Mode Matrix agent surfaced systemic anti-pattern: **`incident-to-rule-pipeline.md` premature-rule-guard (≥7 days OR ≥2 recurrence) is being weaponized to defer DETECTOR implementation indefinitely.**

Evidence — 6 recent rules cite "detector deferred ≥7 days per premature-rule guard" but detector never landed:

| Rule | Section | Deferred since |
|---|---|---|
| `docs-archival-cadence.md` | §4.3 detector script | 2026-05-18 |
| `docs-folder-volume-budget.md` | §6.3 monitoring script | 2026-05-18 |
| `docs-subfolder-maturity.md` | §5.3 CI grep detector | 2026-05-18 |
| `docs-filename-prefix-convention.md` | §7.2 CI grep detector | 2026-05-18 |
| `diagram-format-selection.md` | §5.3 CI grep detector | 2026-05-18 |
| `dev-readable-doc-language.md` | §7.2 CI grep detector | 2026-05-14 |

Result: 6/6 most recent rules sit at **E1-E2 enforcement tier** (advisory / reviewer-checklist only) — what `rule-change-process.md` §6 explicitly bans as "advisory fiction."

Per `output-review-mandate.md` §6.5 Enforcement Parity Mandate: rule + detection MUST land same PR. Premature-rule-guard provides escape hatch ("defer detector"), and the escape hatch is becoming the default.

## Root Cause

`incident-to-rule-pipeline.md` §3 premature-rule guard intended for genuine cases where detector cost > value at recurrence #1. But "defer detector" became boilerplate copy-paste in recent rule landings — agents reaching for the escape hatch reflexively rather than evaluating cost-benefit per rule.

Compounding factor: no tracking mechanism for "deferred detectors that should now land" — once detector deferred, it falls off radar permanently.

## Proposed Fix

### Step 1: Audit all rules citing "detector deferred"

```bash
grep -rln "detector deferred\|deferred ≥7 days\|deferred per premature-rule guard" .claude/rules/
```

For each rule, evaluate:
- Has detector LANDED post-grace period? (check git log + CI workflow file)
- If NOT, is detector still warranted? (cost-benefit at current state)
- If warranted, file follow-up: ship detector OR remove guard text honestly

### Step 2: Tighten `incident-to-rule-pipeline.md` §3 premature-rule guard

Add explicit conditions for legitimate deferral:
- Detector requires non-trivial AST walk / parser → defer OK
- Detector requires external tool not yet integrated → defer OK
- Detector is trivial grep/file-exist check (<50 LOC bash) → **NO defer; ship same PR**

Rule extension v1.1.0+ — refined guard wording prevents boilerplate misuse.

### Step 3: Tracking mechanism

Add `deferred_detectors` column or sidecar tracking in `rules-index.csv`:
- Rule ships with detector deferred → CSV row gets `deferred_detector_target_date` field
- CI script `check-deferred-detectors.sh` lists overdue detector landings

OR simpler: file follow-up gap per deferred detector at landing time. Forces tracking via existing gap system.

### Step 4: Quarterly retro counter

`incident-to-rule-pipeline.md` quarterly retro adds: "How many rules cited premature-rule guard in last 90 days? How many of those detectors have landed?" Target ratio: ≥70% land within 30 days post-grace.

## Acceptance Criteria

- [ ] Audit run of 6+ deferred detectors per Step 1 — verdict per rule (land / honest defer / remove rule)
- [ ] `incident-to-rule-pipeline.md` §3 v1.1.0 — refined guard wording per Step 2
- [ ] Tracking mechanism shipped per Step 3 (gap-based OR CSV column)
- [ ] First quarterly retro counter logged per Step 4 (target ratio defined)
- [ ] At least 2 of 6 deferred detectors landed OR honest-defer rationale documented

## Related

- **Parent:** Wave 98 META audit 3-agent outside-in 2026-05-19 (Failure-Mode Matrix systemic finding)
- **Sister gap:** GAP-676 (Quality Gate vs Issue Tracking classification — also Wave 98 META audit)
- **Rule under audit:** `incident-to-rule-pipeline.md` §3 premature-rule guard
- **Rule extended this PR (Wave 99C):** `audit-to-gap-pipeline.md` §2.5 + `contract-first-for-cross-layer.md` §6.2 — Wave 99C ships 2 detectors that CLOSE deferred-detector debt as worked example
- **External reference:** ESLint Rule Lifecycle (qualitative, no count threshold) per `rule-change-process.md` §5.1 atomic-unique bar Wave 76 Bucket C
- **Rule:** `meta-gap-priority.md` §3 — META-META P1 force-multiplier
