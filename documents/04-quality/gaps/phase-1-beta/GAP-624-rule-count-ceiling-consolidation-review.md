# GAP-624: Rule count ceiling consolidation review (WARN tier trigger post-scaling-pack)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta
**Found:** 2026-05-18 (Wave 92 closure meta-improvements audit per `wave-closure-scope-completeness.md` §3)
**Affects:** `.claude/rules/` total count discipline per `.claude/rules/README.md` "Rule count ceiling policy"

## Problem

Rule count progression session 2026-05-18:

| Mốc | Count | Band | Action policy |
|---|---|---|---|
| Pre-session | 63 rules | INFO (51-75) | Quarterly review trigger; audit overlap candidates |
| Post `wave-closure-scope-completeness.md` ship (PR #1520) | 64 rules | INFO | (unchanged band) |
| Post docs scaling pack 4 rules ship (PR #1522/1523/1524 + Rule 1 pending) | **68 rules** | INFO sát ngưỡng WARN | Cần consolidation review trong 90 ngày |
| WARN tier ngưỡng | 76 rules | WARN | Consolidation review MANDATORY before adding next rule (CI surfaces warning; reviewer-checklist) |
| HARD STOP | 101 rules | HARD STOP | CI exit 1 — must consolidate trước add new rule |

Hiện 68/75 (91% INFO band). Trajectory:
- Session 2026-05-18: +5 rules (1 wave-closure + 4 docs scaling)
- Previous session pattern: ~3-5 rules/session
- Sau 1-2 sessions nữa với pace tương tự → WARN tier (76+)

Per `.claude/rules/README.md` §"Rule count ceiling policy":
> When count approaches 75: apply `meta-gap-priority.md` §3 force-multiplier logic — meta-rule consolidation = Meta-P0 (touches every future session); prefer merging overlapping rules over deprecating useful ones.

→ Cần proactive consolidation review TRƯỚC khi đụng WARN.

## Root Cause

Rules accumulated organically qua nhiều incidents. Per `incident-to-rule-pipeline.md` 5-stage applied mỗi recurrence → mỗi miss thường ship 1 rule. Pattern productive nhưng cumulative effect = rule count growth.

Potential overlap candidates (cần audit):

| Rule cluster | Overlap concern |
|---|---|
| `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` | Wave-level vs per-gap vs target-sync — overlap radii cần verify orthogonal |
| `docs-folder-structure.md` + `docs-archival-cadence.md` + `docs-subfolder-maturity.md` + `docs-folder-volume-budget.md` + `docs-filename-prefix-convention.md` + `planning-docs-structure.md` + `dev-readable-doc-language.md` + `readme-content-discipline.md` | 8 docs-related rules — potential consolidation thành 2-3 hierarchy rules |
| `audit-to-gap-pipeline.md` §2.5/2.6/2.7/2.8 (state-check family) | Already consolidated trong 1 rule với 4 sub-sections (good pattern) |
| `outside-in-coverage-trigger.md` + `inside-out-completeness-trigger.md` | Sister rules — keep separate (orthogonal axes) |

## Proposed Fix

### Phase 1: Audit overlap (~2-3h agent task)

Background agent đọc tất cả 68+ rules:

1. Build overlap matrix — mỗi rule × mỗi rule = overlap score (0/1/2)
2. Surface clusters ≥3 rules với overlap
3. Propose consolidation patterns:
   - **Merge** — rule A + rule B → rule C (with deprecation banner per `rule-change-process.md` §6.1)
   - **Hierarchy** — rule A becomes parent với §sections cho A1, A2, A3
   - **Reference** — rule A cross-link rule B; both stay

### Phase 2: Execute consolidation (~2-4h depending scope)

Per `rule-change-process.md` §6.1 Deprecation lifecycle:
- Mark deprecated rules với 60-day WARN window
- Update CSV `lifecycle_status=deprecated`
- Cross-link updates trong other rules

### Phase 3: Verify count drop + freshness audit

- Re-count: target 50-60 rules (INFO band low-end)
- Stale rules >180 days `Last-Reviewed` per `rule-change-process.md` §3.5: bump version OR mark deprecated

## Acceptance Criteria

- [ ] Overlap matrix shipped tại `documents/04-quality/audits/meta/2026-{XX}-rules-overlap-matrix.md`
- [ ] ≥3 consolidation proposals via PR (merge OR hierarchy OR reference)
- [ ] Rule count post-consolidation: <60 (INFO band low-end)
- [ ] `rules-index.csv` `lifecycle_status` reflects deprecation transitions
- [ ] No constraint loosening (consolidation = same semantic, less surface)
- [ ] Status flip DONE only sau Phase 3 verify

## Related

- `.claude/rules/README.md` §"Rule count ceiling policy" (free <50, INFO 51-75, WARN 76-100, HARD STOP >100)
- `rule-change-process.md` §3.5 staleness + §6.1 deprecation lifecycle
- `meta-gap-priority.md` §3 force-multiplier (meta-rule consolidation = Meta-P0)
- `wave-closure-scope-completeness.md` v1.0.0 (just shipped) — parallel scope discipline pattern
- 4 docs scaling rules shipping (PR #1522/1523/1524 + Rule 1 pending) — direct contributors to count rise

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (rule count ceiling consolidation review; meta).
- **2026-05-18 (filed):** Filed by Wave 92 closure meta-improvements audit. Top 3 improvement areas surfaced 2026-05-18 session: rule count ceiling consolidation review = #1 priority (P2 — preventive, soft deadline 90-day quarterly). Per user 2026-05-18 decision "File 3 gap files TOP 3 + defer execution" — execution defer Wave 94+ post-release-2-plan-lock. Tracking-only filing này tránh silent loss per `wave-closure-scope-completeness.md` recursion. Counter trajectory: pre-session 63 → post-session 68 → projected next sessions 73-78 (WARN tier) nếu không consolidate.
