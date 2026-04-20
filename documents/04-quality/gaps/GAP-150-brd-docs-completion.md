# GAP-150: BRD Documents Completion (5 Core Docs Missing)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier per `meta-gap-priority.md` — ranks above feature-P0)
**Domain:** Business / BRD / Governance
**Found:** 2026-04-20 (user flagged `documents/00-brd/` incomplete; no gap covering BRD content creation)
**Affects:** All per-domain rules.md (01-business/*), pricing decisions, compliance posture, GTM readiness
**Scope note (2026-04-20):** This gap covers **5 strategic BRD docs** (objectives/compliance/pricing/NFR/GTM). Simulation gap-finder 2026-04-20 found **22 additional BRD docs** needed (TOS, Privacy, AUP, Refund, Data Retention, Child Protection, etc.) — tracked by umbrella **GAP-154**. Total BRD scope: 27 docs, this gap = 5 of them (Phase 1 strategic).

## Problem

`documents/00-brd/README.md` lists 5 planned BRD docs ("Current Gaps (Planned)" section) — **none exist:**

| File (planned) | Purpose | Current Owner |
|----------------|---------|----------|
| `business-objectives.md` | OKRs, success metrics, north-star KPIs | PM |
| `compliance-scope.md` | VN PDPL, MoET circulars, labor law mapping | Legal + PM |
| `pricing-model.md` | Free/Pro/Premium/Enterprise tier definition, AI metering, discount policy | PM + Finance |
| `nfr-catalog.md` | SLA, uptime target, RTO/RPO, performance budgets | Architect + PM |
| `go-to-market.md` | Target schools, pilot strategy, sales funnel, launch timeline | PM |

Only `personas-catalog.md` exists (DRAFT v1, 2026-04-14) alongside README.

**Why this is Business-Logic tier (not Feature):**
- Per-domain rules.md depend on BRD (e.g. `payment-invoice/rules.md` encodes pricing decisions that should trace to `pricing-model.md`)
- GAP-049 (business-logic correctness review) claims "tracked in" by README but GAP-049 scope is REVIEW PROCESS, not CONTENT CREATION
- Without BRD, per-domain rules are **assumption-driven** — untraceable back to business intent

## Root Cause

README 00-brd ghi *"Engineering MVP runs without formal BRD (placeholder rules). Real driver là legal engagement (Wave 0 stakeholder sync blocker) + paying customer #1."*

→ Deferred intentionally. But:
- GAP-049 AC doesn't create the 5 files (only process)
- Master plan (PR #382) 12 waves không có wave target BRD content creation
- 4 out of 5 files have no gap + no wave → orphaned

## Proposed Fix

### Phase 1 (this gap — skeleton + frame, ship with placeholders)

Create 5 files với frontmatter + section structure + TODO markers. Each file follows template:

```markdown
---
title: <Title>
status: draft | skeleton | review | approved
created: 2026-04-20
updated: 2026-04-20
owner: <role>
reviewer: <role>
---

# <Title>

## 1. Scope / Context
TODO: <1 paragraph>

## 2. <Core Section A>
TODO: <content>

## 3. <Core Section B>
...

## 4. Dependencies / References
- Related BRD: ...
- Consumer domains (01-business/*): ...

## 5. Log
- 2026-04-20 — Skeleton created (GAP-150)
```

### Phase 2 (stakeholder engagement — not this gap)

Fill skeleton với real content. Requires:
- Legal engagement (compliance-scope)
- Finance engagement (pricing-model)
- PM stakeholder input (objectives, GTM)
- Architect sync (NFR)

Track as follow-up gaps (GAP-155, 156, etc.) after first stakeholder session. OUT OF SCOPE for GAP-150.

## Acceptance Criteria

- [ ] `documents/00-brd/business-objectives.md` — skeleton with OKR template, success metrics section, KPI placeholders
- [ ] `documents/00-brd/compliance-scope.md` — skeleton listing VN legal frameworks (PDPL 2023, MoET circulars, labor law, consumer protection) with TODO per framework
- [ ] `documents/00-brd/pricing-model.md` — skeleton with tier table (Free/Pro/Premium/Enterprise), AI quota placeholders, discount policy section
- [ ] `documents/00-brd/nfr-catalog.md` — skeleton with sections for uptime SLA, RTO/RPO, performance budgets, scalability targets
- [ ] `documents/00-brd/go-to-market.md` — skeleton with target persona priority, pilot strategy, funnel stages, launch timeline
- [ ] All 5 files have frontmatter per `planning-docs-structure.md` pattern
- [ ] Each file marked `status: skeleton` (not `draft` or `approved`)
- [ ] `00-brd/README.md` Current Gaps table updated — status per file (skeleton vs. approved)
- [ ] `00-brd/README.md` links to this gap for tracking content completion
- [ ] ROADMAP entry under Epic 14 Quality Governance
- [ ] GAP-049 AC expanded to reference GAP-150 for content (clearer scope boundary)

## Dependencies

- `00-brd/personas-catalog.md` (already exists — use as reference for pricing persona mapping)
- Wave 0 stakeholder sync status (if scheduled, inform Phase 2 timing)

## Related

- GAP-049 — business logic correctness review (process scope; this gap = content scope)
- GAP-050 — persona-based review (consumes BRD for validation criteria)
- GAP-151 — persona acceptance criteria template (sibling — same wave)
- GAP-152 — execute persona review round 1 (consumes this gap's output)
- GAP-153 — secondary persona AC (sibling — extends 151 scope to users within tenant)
- **GAP-154 — BRD scope expansion umbrella** (extends this gap's scope with 22 additional docs discovered via simulation 2026-04-20)
- Report: `documents/04-quality/audits/business/brd-simulation-gap-finder-2026-04-20.md` — simulation output
- Rule: `.claude/rules/meta-gap-priority.md` §3 — business-logic tier justification
- Rule: `.claude/rules/docs-folder-structure.md` — README + placement discipline
- Master plan Wave 8 (Business Governance) — consumes BRD skeleton for audit

## Out of Scope

- Filling content (Phase 2, separate gaps per BRD file)
- Stakeholder meetings (operational, not PR work)
- Legal review sign-off (requires external counsel)

## Log

- 2026-04-20 — Created. Driven by user flagging 00-brd incomplete + lack of gap coverage. Scoped to skeleton only to unblock per-domain rules.md traceability without blocking on stakeholder availability.
