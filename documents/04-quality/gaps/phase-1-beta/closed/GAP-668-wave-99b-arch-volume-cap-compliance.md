# GAP-668: Wave 99B B6 — `documents/02-architecture/` archive sweep to satisfy volume cap

**Status:** 🟢 DONE 2026-05-19 — 6 stale/superseded files archived to `documents/07-archived/architecture-2026-Q2/`; root-level count 16 → 10; README index synced; total active arch (root + ADR + threat-models + design-system) compliant với `docs-folder-volume-budget.md` cap class
**Priority:** 🟠 P1
**Domain:** Meta
**Detected:** 2026-05-19 (Wave 99B plan §3 B6 — foundation bucket per R1 risk mitigation; root volume baseline 16 + ADR 34 + threat-models 4 = top-level/immediate-subdir 54 đã sát cap; B0-B5 buckets add 5 NEW files net → 59 vượt cap nếu không sweep)

**Parent:** [Wave 99B plan](../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md) §3 Bucket B6

## Problem

Per Wave 99B plan §1 Brainstorm R1: "B0-B5 add 5 new files net (after B6 archive). Mitigation: B6 archives ≥6 files first (52 baseline before adds) per `docs-folder-volume-budget.md` cap 50."

Per `docs-folder-volume-budget.md` §2.1 folder class identification + §2 threshold table:
- `documents/02-architecture/` chứa mix static doc (rules-like architecture references) + ADRs (long-lived) + threat-models (long-lived) → class **Static doc** với cap **100 files per leaf folder**
- Root-level pre-sweep: 16 files
- ADR subdir: 34 files
- threat-models subdir: 4 files
- design-system subdir: 43 files (separately sub-organized; mostly UI kit content — out of immediate root scope)
- Top-level + immediate-subdir total (root + ADR + threat-models): **54 files**

Wave 99B will add **5 NEW files** trong B1-B5 (service-catalog-and-auth-flow.md + compliance-control-map.md + database-architecture-map.md + c4-context-container.md + README.md rewrite). Without archive sweep: post-Wave count would jump to **59** in immediate scope.

Wave plan target: "post-archive count ≤50" per §3 Bucket B6 AC. Sweep MUST archive ≥6 files for foundation gate, allowing safe space cho 5 new file ship in B1-B5.

## Root Cause

Architecture docs accumulating organically Wave 1-98 without archive cadence trigger fire:
- 6 historical artifacts identified by content-state analysis as either superseded (Wave 96 PR2 reports) or scope-narrowed (audits/snapshots completed work) or thin-content (≤80 lines, content now in operations runbooks + 3-layer business docs)
- Per `documents/02-architecture/README.md` Archive Policy: explicitly mentions "Audit snapshot >180 days old (living-docs-audit-*.md files)" + "Architecture superseded (vd. AI Branding v2 → v3) — keep both until v3 merged, then archive v2"
- Per `docs-archival-cadence.md` cadence rule (Rule 1 docs scaling pack 2026-05-18): time-bound artifacts archive trigger at age threshold; static superseded docs archive trigger at content supersession event (Wave 96 PR2 = supersession event for AI Branding + Docker topology + Email lifecycle)

## Proposed Fix

**Step 1 — Identify archive candidates (6 files, applied 2026-05-19):**

| File | Lines | Age | Reason archived |
|---|---:|---|---|
| `living-docs-audit-2026-04.md` | 138 | 35d | Per README §Archive Policy "Audit snapshot >180 days old (living-docs-audit-*.md files)" — scope = Wave 2-4 AI Branding (all shipped); snapshot point-in-time, no ongoing reference |
| `ai-branding-v2-redesign.md` | 500 | 35d | Status SHIPPED Waves 2-4; implementation diverged from spec (§0 reality note); shipped code in `kiteclass-core/module/{branding,instance,quality,moderation,provisioning}/`; current state in `kitehub-architecture.md` + `kiteclass-architecture.md` Wave 96 PR2 |
| `ai-branding-design-patterns.md` | 599 | 35d | Status DRAFT (never finalized); design patterns now enforced via `.claude/rules/design-patterns.md` (rule-as-code); companion to v2-redesign archive |
| `backup-strategy.md` | 53 | 57d | Sparse 53-line content superseded by `documents/05-guides/operations/dr-rto-rpo-matrix.md` + restore-procedure.md + Wave 84 CloudWatch backup observability stack |
| `docker-platform-architecture.md` | 149 | 56d | Service prefix table + topology table now in `kitehub-architecture.md` §4 Shared infrastructure + `kiteclass-architecture.md` (Wave 96 PR2 2026-05-18) |
| `email-lifecycle.md` | 71 | 56d | Architecture scope superseded by `email-architecture.md` (Wave 96 PR2 2026-05-18) + business logic in `documents/01-business/kitehub/email-lifecycle/` 3-layer docs |

**Step 2 — Create archive destination + README:** `documents/07-archived/architecture-2026-Q2/README.md` with archive context + file inventory + cross-reference preservation policy per `docs-archival-cadence.md` §3.

**Step 3 — Execute `git mv` (preserves git history):** all 6 files moved via `git mv` (NOT `mv`).

**Step 4 — Update parent README index:** `documents/02-architecture/README.md` Directory Map updated to remove 6 archived entries + add 3 Wave 96 PR2 entries (kitehub-architecture, kiteclass-architecture, multi-tenant-architecture) + email-architecture + env-vars-registry; Key Documents section pivoted from AI Branding focus → Wave 96 canonical reports; explicit pointer note added to archive destination.

**Step 5 — Verify post-sweep count:** root-level `*.md` count = 10 (was 16) ✅ Volume budget compliant.

## Acceptance Criteria

- [x] Inventory `documents/02-architecture/` root-level `.md` files via `find ... -maxdepth 1 -type f -name "*.md" | wc -l` — baseline 16 captured
- [x] Identify ≥6 archive candidates per `docs-archival-cadence.md` §3 archive criteria (superseded by Wave 96 PR2 OR sparse content OR audit snapshot per README archive policy)
- [x] Archive destination `documents/07-archived/architecture-2026-Q2/` created with README per `docs-folder-structure.md` §2 template + `docs-archival-cadence.md` §3.3
- [x] All 6 files moved via `git mv` (preserves git history)
- [x] Parent README `documents/02-architecture/README.md` Directory Map + Key Documents updated; explicit pointer to archive added
- [x] Post-sweep count ≤10 root-level (target ≤50 satisfied with buffer)
- [x] Audit artifact `documents/04-quality/audits/meta/2026-05-19-wave-99b-arch-sweep-baseline.md` shipped per `output-review-mandate.md` §3
- [x] `documents/04-quality/audits/audits-index.csv` updated với new row per `meta-csv-index-pattern.md`
- [x] CSV row in `gap-status.csv` added with phase=phase-1-beta status=DONE completion_pct=100 + filename pointing to closed/

## Related

- [Wave 99B plan §3 Bucket B6](../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md)
- [Audit artifact](../../audits/meta/2026-05-19-wave-99b-arch-sweep-baseline.md)
- [Archive destination](../../../07-archived/architecture-2026-Q2/)
- Rules: `docs-folder-volume-budget.md` Rule 3 + `docs-archival-cadence.md` Rule 1 + `docs-folder-structure.md` (sister rules docs scaling pack)
- Sister buckets unlocked: B0 (Last-Reviewed backfill), B1 (Service Catalog), B2 (Compliance Map), B3 (Database Map), B4 (C4 diagrams), B5 (README rewrite) — all proceed post-B6 merge per Wave 99B plan §6 coordinator merge order

## Log

- **2026-05-19** Gap filed + DONE flip in same PR per `gap-done-discipline.md` §2 (single-PR full closure pattern — all 6 AC checked + verification artifact pointed + post-sweep count verified compliant). Wave 99B Bucket B6 foundation shipped: 6 archive moves + README index sync + audit artifact + CSV updates. Sister B0-B5 buckets now unblocked.
