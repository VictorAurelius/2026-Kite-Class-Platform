# GAP-678: Wave 99B post-wave audit suite (Quality /110 + Business Logic /100 refresh)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-19 (Wave 99B closure obligation per `post-wave-audit-mandate.md` §2.2 + plan §7 explicit)
**Affects:** Wave 99B post-merge audit cadence; trust signal for architecture docs additions (B1-B4 canonical references)

## Problem

Wave 99B shipped 7/7 buckets adding 4 NEW canonical architecture artifacts (B1 Service Catalog + B2 Compliance Map + B3 Database Map + B4 C4 Diagram) plus B5 Onboarding Tour orchestrator. Per `post-wave-audit-mandate.md` §2.2 cadence (3-day window post-wave-merge ≤2026-05-22) + plan §7 explicit cadence requirement, audit suite must run:

- **Quality /100 refresh** — Cat 1 Rule Coverage (3-layer doc completeness via Wave 99C detector running WARN mode) + Cat 9 Architecture (4 NEW canonical artifacts shipped) likely positive deltas
- **Business Logic /100 refresh** — B2 Compliance × Code Map directly impacts compliance row scoring; 19 PDPL/ANM/ISO27001 rows now have status verdict (0 TBD vs prior baseline) → expect +N delta
- **UI /128** — N/A (docs-only wave, no FE changes)

Cadence breach risk: if audit suite không chạy ≤2026-05-22, audit trust signal degrades + future architecture additions chưa được benchmark against Wave 99B baseline.

## Root Cause

Wave 99B closure scoped 7 bucket execution (B0-B6) but explicit audit refresh was deferred per session pivot priority (user direction "Sequential B5 → closure focused; tasks 3+4 defer to next session"). Audit cadence obligation tracked here per `wave-closure-scope-completeness.md` §3 (every plan §3 + cadence obligation reconciled OR has follow-up gap link).

## Proposed Fix

Spawn 2 parallel background agents per `wave-pack-planner/SKILL.md` (Quality audit + Business Logic audit), each:

1. **Quality /110 audit agent** — apply `quality-audit/SKILL.md` 11-category framework; compare baseline 90/110 B+ (Wave 98 GAP-661, 2026-05-19) → expect Cat 1 (Rule Coverage) + Cat 9 (Architecture) positive deltas via 4 NEW B1-B4 canonical artifacts + B5 orchestrator + Wave 99C detectors (3-layer + cross-layer drift) closing recurrence #2 from Wave 98
2. **Business Logic /100 audit agent** — apply `business-logic-audit/SKILL.md`; compare baseline 73/100 C+ PARTIAL FAIL (Wave 98 GAP-661) → expect Cat 1 Rule Coverage delta via B2 compliance-control-map.md 19 row verdict (0 TBD) + B3 database-architecture-map.md RLS 51/91 coverage documented
3. **Report artifacts** in `documents/04-quality/audits/{quality,business}/2026-05-XX-wave-99b-post-wave-{quality,business-logic}.md` per `audits-index.csv` 100% coverage parity
4. **Update `output-review-mandate.md`** §3 matrix rows (Quality + Business Logic) with REFRESHED markers + post-Wave-99B verdict
5. **File new gaps** for any findings P0/P1 per `audit-to-gap-pipeline.md` Step 3 (expected: ≤2 findings given docs-only scope + 4 NEW canonical artifacts directly addressed prior Wave 98 P0 findings GAP-662/664)

## Acceptance Criteria

- [ ] Quality /110 audit report shipped to `documents/04-quality/audits/quality/2026-05-XX-wave-99b-post-wave-quality.md`
- [ ] Business Logic /100 audit report shipped to `documents/04-quality/audits/business/2026-05-XX-wave-99b-post-wave-business-logic.md`
- [ ] `audits-index.csv` 2 new rows added (AUDIT-2026-05-XX-wave-99b-{quality, business-logic})
- [ ] `output-review-mandate.md` §3 matrix rows updated với new scores + delta annotations (Quality + Business Logic)
- [ ] Any P0/P1 findings → gap files filed per `audit-to-gap-pipeline.md` Step 3
- [ ] Cadence deadline met (audit reports shipped ≤2026-05-22 = 3 days post Wave 99B closure)
- [ ] PASS Phase 1 BETA gate ≥80 verified Quality post-refresh; verdict on Business Logic FAIL → PASS path documented

## Related

- **Wave 99B plan §7 obligation:** [`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md`](../../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md) §7.1 row 8
- **Mandate rule:** [`post-wave-audit-mandate.md`](../../../../.claude/rules/post-wave-audit-mandate.md) §2.2 3-day cadence
- **Scope-completeness rule:** [`wave-closure-scope-completeness.md`](../../../../.claude/rules/wave-closure-scope-completeness.md) §3 (this gap satisfies row 8 reconciliation)
- **Prior baselines:** AUDIT-2026-05-19-wave-98-quality-refresh (90/110 B+) + AUDIT-2026-05-19-wave-98-business-logic-new (73/100 C+ PARTIAL FAIL) — both Wave 98 GAP-661 audit suite
- **Wave 99C detectors paired:** GAP-675 META-META detectors (3-layer completeness + cross-layer contract drift) — closing recurrence #2 expected to improve Cat 1 Rule Coverage scores in this audit

## Log

- **2026-05-19** Filed Wave 99B closure PR per `wave-closure-scope-completeness.md` §3 row 8 reconciliation. Cadence deadline ≤2026-05-22 (3 days). Spawn pattern recommend 2 bg-agents parallel (Quality + Business Logic) per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md` Rule 9 (max 5 concurrent; 2 is safe threshold per Wave 92 retro lesson).
