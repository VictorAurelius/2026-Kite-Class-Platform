# GAP-674: Wave 99B B5 — Golden-path Onboarding Tour README (rewrite 02-architecture/README.md)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-19 (Wave 99B B5 — onboarding gap surfaced during 4-persona outside-in audit consensus)
**Affects:** New backend dev / new FE dev / SRE / Tech Lead onboarding velocity into Kite Platform architecture

## Problem

`documents/02-architecture/README.md` hiện tại (103 lines post-Wave-99B-B6 archive sweep) là **directory index stub** — liệt kê files + folders nhưng KHÔNG orchestrate reading order. New developer landing trên architecture root phải:

1. Đọc README → biết có 13 files + 4 subdirs
2. Phải đoán đọc file nào trước (kitehub-architecture vs multi-tenant vs deployment-strategy)
3. Mất 30-60 phút trial-error trước khi build mental model end-to-end của 1 user request

Wave 99B 4-persona outside-in audit (B0-B6 plan §1 Brainstorm Persona 2) surface concrete need: **"trace one request end-to-end from API call to database row, with link map to relevant architecture artifacts"**.

Wave 99B đã ship 4 NEW canonical architecture artifacts (B1 Service Catalog + B2 Compliance Map + B3 Database Map + B4 C4 Diagram) NHƯNG chưa có orchestrator surface chúng cho new-dev workflow. Current README liệt kê Key Documents bullet list (line 62-70) nhưng không có:
- 7-step reading order linking new artifacts trong logical flow
- Trace-one-request tutorial (hypothetical user action → which file to read at each architectural layer)
- Per-persona reading list (P1 backend / P2 frontend / P3 SRE / P4 tech lead)
- `audience: dev` frontmatter explicit

## Root Cause

- **Foundation buckets B1-B4 designed standalone** — mỗi gap focuses scope của 1 artifact (Service Catalog OR Database Map OR Compliance Map OR C4). Reading order orchestration explicitly deferred Wave 99B B5 bucket per wave plan §2 task breakdown.
- **README pattern: index vs guide split** — historical convention `documents/**/README.md` = directory map per `docs-folder-structure.md` §3 template. B5 is the upgrade từ "directory map" → "guide map" cho high-traffic architecture root.
- **Discoverability cost vs creation cost asymmetry** — 1 README rewrite (~30 min) eliminates 30-60 phút × N future devs onboarding ambiguity = clear force-multiplier per `meta-gap-priority.md` §3.

## Proposed Fix

Rewrite `documents/02-architecture/README.md` per Wave 99B plan §B5 scope:

1. **Add frontmatter** `audience: dev` (per `docs-filename-prefix-convention.md` §4 recommended)
2. **Replace stub bullet list** với **7-step reading order tour** (top-down architecture progression):
   - Step 1: [`c4-context-container.md`](c4-context-container.md) — system boundary L1 + container topology L2 (B4)
   - Step 2: [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) — 18 services + dependency graph + auth flow (B1)
   - Step 3: [`database-architecture-map.md`](database-architecture-map.md) — 91 entity catalog + FK graph + RLS map (B3)
   - Step 4: [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — tenant isolation strategy + RLS implementation
   - Step 5: [`compliance-control-map.md`](compliance-control-map.md) — PDPL/ANM/ISO27001 control × code mapping + SLO registry (B2)
   - Step 6: [`adr/README.md`](adr/README.md) — ADR index (rationale why-decisions)
   - Step 7: [`threat-models/`](threat-models/) — per-domain threat models
3. **"Trace one request" tutorial** — walk through concrete user action (e.g., "P2 Owner submits beta-access form"): show which file to read at each layer (FE → gateway → service → DB → audit).
4. **Per-persona index** — recommended reading list for:
   - **P1 — Backend Engineer:** B1 + B3 + multi-tenant + ADRs
   - **P2 — Frontend Engineer:** B4 L1+L2 + B1 (gateway endpoints) + design-system links
   - **P3 — SRE / DevOps:** B2 (SLO Registry) + deployment-strategy + ssl-automation
   - **P4 — Tech Lead / Architect:** B4 + B1 + B2 + ADR index + threat-models
5. **Preserve §File Placement Rules + §ADR Process + §Archive Policy + §Related** existing sections (not stub-replace; orchestrator layer adds, doesn't subtract).
6. **Cross-link verify** — all 7 step targets exist post-B1-B4 merges (verified before commit per `audit-to-gap-pipeline.md` §2.6 state-check evidence).

## Acceptance Criteria

- [ ] `documents/02-architecture/README.md` frontmatter `audience: dev` added (per `docs-filename-prefix-convention.md` §4)
- [ ] 7-step reading order tour section added (replaces stub bullet list line 62-70)
- [ ] All 7 step targets verified exist (`c4-context-container.md` + `service-catalog-and-auth-flow.md` + `database-architecture-map.md` + `multi-tenant-architecture.md` + `compliance-control-map.md` + `adr/README.md` + `threat-models/`)
- [ ] "Trace one request" tutorial section added (concrete user action walkthrough with per-layer file pointers)
- [ ] Per-persona index added (P1 backend + P2 frontend + P3 SRE + P4 tech lead recommended reading lists)
- [ ] Existing §Directory Map + §File Placement Rules + §ADR Process + §Archive Policy + §Related sections preserved (not stub-replaced)
- [ ] Total line count between ~150-220 (vs 103 baseline; per `docs-folder-volume-budget.md` static doc <100 file cap N/A — single file README, not folder count)
- [ ] CI checks all PASS (rule-frontmatter / rule-staleness / readme-freshness / wave-plan-completeness)

## Related

- **Wave plan:** [`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md`](../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md) §B5 (line 141-151)
- **Sister Wave 99B gaps (B1-B4 shipped):** GAP-670 Service Catalog · GAP-671 Compliance Map · GAP-672 Database Map · GAP-673 C4 Diagram
- **Wave 99B B6 archive sweep:** GAP-668 (foundation — README post-archive baseline 103 lines)
- **Wave 99B B0 Last-Reviewed backfill:** GAP-669
- **Rule references:** [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) §3 README template + [`docs-filename-prefix-convention.md`](../../../.claude/rules/docs-filename-prefix-convention.md) §4 audience frontmatter

## Log

- **2026-05-19** Gap filed (Wave 99B B5 — gap was referenced in wave plan frontmatter `gaps: [..., GAP-674]` line 7 but file never created when wave plan landed). Created now as prerequisite for B5 execution. Wave 99B 6/7 buckets shipped; B5 is last bucket.
