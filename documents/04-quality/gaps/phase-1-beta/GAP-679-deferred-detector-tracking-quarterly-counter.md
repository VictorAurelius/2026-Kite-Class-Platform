# GAP-679: Deferred-detector tracking + quarterly retro counter (META-META Steps 3-4)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 META-META
**Domain:** Meta (governance of governance — tracking mechanism)
**Found:** 2026-05-19 (Wave 99C META-META GAP-675 PR — Steps 3-4 deferred to follow-up)
**Affects:** `incident-to-rule-pipeline.md` §3.1 enforcement; HARD STOP transition for 3 docs scaling detectors shipped Wave 99C; quarterly retro process

## Problem

Wave 99C META-META GAP-675 PR closed Steps 1-2 of the deferred-detector audit:
- Step 1 ✅ Audited 6 deferred detectors → 3 SHIP NOW (`check-docs-archival-stale.sh` + `check-docs-folder-volume.sh` + `check-docs-subfolder-maturity.sh`) + 3 HONEST DEFER (`docs-filename-prefix-convention` / `diagram-format-selection` / `dev-readable-doc-language`)
- Step 2 ✅ Refined `incident-to-rule-pipeline.md` §3.1 with 3 explicit legitimate-deferral conditions + banned shortcut

Steps 3-4 deferred to this follow-up gap per `incident-to-rule-pipeline.md` §3.1 condition 3 (honest defer + concrete revisit trigger):
- **Step 3 — Tracking mechanism for overdue detector landings**: 3 SHIP-NOW detectors currently WARN-mode initially; need HARD STOP transition after grace period validated. Plus tracking column or sidecar mechanism to monitor when HONEST-deferred detectors' deferral conditions might lapse.
- **Step 4 — Quarterly retro counter**: `incident-to-rule-pipeline.md` quarterly retro adds metric "How many rules cited premature-rule guard in last 90 days? How many of those detectors have landed?" Target ratio: ≥70% land within 30 days post-grace.

## Root Cause

GAP-675 audit found 6/6 recent rules sit at E1-E2 enforcement tier (advisory/reviewer-checklist only). Wave 99C SHIP-NOW lands 3 detectors immediately closing the most actionable cases. Remaining governance work (tracking + retro counter) is lower priority than the audit itself + the §3 wording fix — deferred per `incident-to-rule-pipeline.md` §3.1 conditions:

1. ✅ **Non-trivial:** tracking sidecar requires CSV schema design + integration with `rules-index.csv` lifecycle columns; quarterly counter requires session-transcript scanning OR manual retro process
2. ✅ **Low recurrence:** 0 recurrence post-§3.1-tightening; counter measures rate over 90-day window not single incident
3. ✅ **Honest defer documented:** this gap file IS the tracking — explicit follow-up with ETA Wave 100+

## Proposed Fix

### Step 3a — HARD STOP transition for 3 SHIP-NOW detectors (Wave 100+)

After 30-day grace period from Wave 99C merge (2026-06-19):
- Verify real-repo scan results stabilized (no new over-cap folders / no stale audits after batch archive)
- Flip CI job `docs-scaling-detectors` mode from `--warn` → `--strict` for individual detector(s) where backlog cleared
- Per-detector promotion (not all-or-nothing): archival cadence may flip earliest (0 stale baseline) while folder-volume waits for triage PRs

### Step 3b — Tracking mechanism for HONEST-deferred detectors

Add column `detector_status` to `rules-index.csv`:
- Values: `shipped` | `honest-deferred` | `boilerplate-deferred` (last = anti-pattern, should trigger meta-review)
- Plus column `detector_revisit_trigger` (free-text concrete trigger condition)
- CI validator updates to check coverage parity (every rule has detector status set)

### Step 4 — Quarterly retro counter

Manual process initially:
- Quarterly retro session reads `rules-index.csv` → count rules with `detector_status=honest-deferred` aged ≥90 days
- Re-evaluate per §3.1 conditions (still genuinely deferrable? OR recurrence count grew? OR cost-benefit shifted?)
- Target ratio: ≥70% of detectors deferred ≤30 days post-grace eventually ship
- Pattern frequency >30% honest-deferred → meta-review of §3.1 conditions (may be too permissive)

Future automation: session-transcript scanning to auto-count incidents reported per rule scope, populate `recurrence_count` column.

## Acceptance Criteria

- [ ] Step 3a: 3 SHIP-NOW detectors flipped `--warn` → `--strict` post grace + backlog cleared
- [ ] Step 3b: `rules-index.csv` `detector_status` + `detector_revisit_trigger` columns added; 6 audited rules backfilled; CI validator updated
- [ ] Step 4: First quarterly retro run with detector-tracking metric logged (target ratio measured)
- [ ] §3.1 condition 3 honored: this gap closes when at least Step 3a + Step 3b shipped (Step 4 metric tracking ongoing)

## Related

- **Parent:** Wave 99C META-META PR (GAP-675 closure) — Steps 1+2 shipped same PR; Steps 3+4 deferred here
- **Sister gap:** GAP-675 (META-META audit closing detector debt — Steps 1+2 closed by Wave 99C PR)
- **Rule under tracking:** `incident-to-rule-pipeline.md` v1.1 §3.1 (3-condition legitimate-deferral test)
- **3 detectors shipped Wave 99C (tracking targets for Step 3a):**
  - `scripts/check-docs-archival-stale.sh` (per `docs-archival-cadence.md` v1.0.1 §4.3)
  - `scripts/check-docs-folder-volume.sh` (per `docs-folder-volume-budget.md` v1.0.1 §6.3)
  - `scripts/check-docs-subfolder-maturity.sh` (per `docs-subfolder-maturity.md` v1.0.1 §5.3)
- **3 rules HONEST-deferred Wave 99C (tracking targets for Step 3b):**
  - `docs-filename-prefix-convention.md` §7.2 (5-tier classification complexity)
  - `diagram-format-selection.md` §5.3 (box-drawing FP risk on tables)
  - `dev-readable-doc-language.md` §7.2 (mixed VN+EN inherently ambiguous)
- **Rule:** `meta-gap-priority.md` §3 — META-META P2 (governance of governance enforcement)
- **CI workflow:** `.github/workflows/script-quality.yml` job `docs-scaling-detectors` (paired wire same PR as 3 SHIP-NOW detectors)

## Log

- **2026-05-19 (created):** Filed Wave 99C META-META GAP-675 PR per `incident-to-rule-pipeline.md` v1.1 §3.1 honest-defer condition 3 (follow-up gap tracking deferred work). Steps 1+2 closed by parent PR; Steps 3-4 sized as P2 effort (not P1 because audit + §3 wording already shipped immediately closes the highest-leverage governance work). ETA Wave 100+ after 30-day grace period validation of 3 SHIP-NOW detectors.
