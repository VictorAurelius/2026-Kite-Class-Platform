# GAP-151: Persona-Specific Acceptance Criteria — Template + Per-Persona AC Docs

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — blocks persona review execution)
**Domain:** Business / Persona / Governance
**Found:** 2026-04-20 (user raised: "mỗi loại đối tượng cần khởi tạo 1 bộ tiêu chí của họ")
**Affects:** GAP-050 execution, persona-based review skill, business-logic correctness quality

## Problem

`00-brd/personas-catalog.md` defines 10 personas with "Key needs" + "Critical Gaps" (Coverage Review Status). Nhưng:

- "Key needs" là bullet list, **không phải acceptance criteria**
- "Coverage %" (60%, 75%, 30%) là **subjective estimate**, không phải measurable against formal criteria
- **Không có formal AC checklist** per persona để review nghiệp vụ system

User's exact request:
> "mỗi loại đối tượng cần khởi tạo 1 bộ tiêu chí của họ và họ sẽ review nghiệp vụ của hệ thống xem có đúng tiêu chí chưa"

Current skill `.claude/skills/quality/persona-based-business-review.md` describes role-play methodology but **không có template AC per persona**. Reviewer phải improvise checklist mỗi lần → non-reproducible, score drift.

## Root Cause

Catalog v1 (2026-04-14) được tạo as classification doc, không extend thành review framework. Assumption: "reviewer sẽ derive AC from Key needs" — fails in practice because Key needs quá high-level (ví dụ: "Bulk import xlsx" không nói rõ: 500 rows? 5000 rows? validation errors handling? rollback on partial fail? all of these?).

Gap test: user's xlsx example → caught by initial scan (GAP-051 exists) nhưng AC cho GAP-051 phải suy ra, không có **trường-cấp-3 reviewer nhập vai** checklist để confirm đủ/thiếu.

## Proposed Fix

### Deliverable 1: AC Template

Create `documents/00-brd/persona-criteria/_TEMPLATE.md` — reusable AC template:

```markdown
---
title: Acceptance Criteria — P<N> <Persona Name>
status: draft | approved
created: 2026-MM-DD
persona: P<N>
scale_assumption: <numbers — students, teachers, classes>
reviewer: <role in real organization acting as this persona>
---

# AC — P<N> <Name>

## 0. Context
- Scale assumed: ...
- Organization archetype: ...
- Revenue tier mapping: ...

## 1. Onboarding AC
- [ ] AC-ONBOARD-001: <statement — must verify X condition>
  - **Test:** <concrete scenario>
  - **Fail signal:** <what reviewer would observe if system gaps>
- [ ] AC-ONBOARD-002: ...

## 2. Daily Operations AC
- [ ] AC-OPS-001: <bulk import 500 students via xlsx in <5 min>
  - **Test:** Upload valid xlsx với 500 rows, assert accounts created + email/SMS credentials sent
  - **Fail signal:** No xlsx upload UI OR upload fails OR credentials not distributed
- [ ] AC-OPS-002: ...

## 3. Financial / Admin AC
- [ ] AC-FIN-001: ...

## 4. Communication AC (stakeholders)
- [ ] AC-COMM-001: ...

## 5. Edge Cases AC
- [ ] AC-EDGE-001: ...

## 6. Exit / Termination AC
- [ ] AC-EXIT-001: ...

## Scoring

Total ACs: <N>
Each AC: PASS (meets) / PARTIAL (partial implementation) / FAIL (missing)
Coverage % = PASS-count / total × 100

## Gap Linkage

| AC Failed | Gap ID | Priority |
|-----------|--------|:--------:|
| AC-OPS-001 | GAP-051 | P0 |
...

## Log
- 2026-MM-DD — Initial AC set (author)
- 2026-MM-DD — Reviewer <name> completed review, score X%
```

### Deliverable 2: AC per Tier 1 persona

4 files:
- `documents/00-brd/persona-criteria/P1-solo-teacher.md`
- `documents/00-brd/persona-criteria/P2-small-center.md`
- `documents/00-brd/persona-criteria/P3-medium-center.md`
- `documents/00-brd/persona-criteria/P5-k12-school.md` (largest — user's priority example)

Each populated with 15-30 ACs derived from catalog Key needs + Pain points + real-world org workflows (onboarding → daily → admin → comm → edge → exit). Reference existing gaps (GAP-051..064) for AC-to-gap linkage.

### Deliverable 3: Persona review skill integration

Update `.claude/skills/quality/persona-based-business-review.md` to reference template + AC docs. Reviewer flow:
1. Load AC doc for persona
2. Role-play with real scale assumption
3. Mark each AC PASS/PARTIAL/FAIL with evidence
4. Output scored report to `00-brd/persona-reviews/`
5. File NEW gaps for NEW failures not already tracked

### Deliverable 4: README index

`documents/00-brd/persona-criteria/README.md` — list all per-persona AC docs + last-reviewed dates.

## Acceptance Criteria

- [ ] `00-brd/persona-criteria/_TEMPLATE.md` — reusable AC template with 6 categories (onboarding, ops, fin/admin, comm, edge, exit)
- [ ] 4 Tier 1 AC docs populated — P1, P2, P3, P5 (15-30 ACs each)
- [ ] Each AC has `AC-<CATEGORY>-<NUM>` ID + Test + Fail signal
- [ ] Gap linkage section populated (cross-reference GAP-051..064 where applicable)
- [ ] `persona-based-business-review.md` skill updated to consume AC docs
- [ ] `00-brd/persona-criteria/README.md` index created
- [ ] `00-brd/README.md` File Placement Rules section updated to reference persona-criteria/ subdir
- [ ] ROADMAP Epic 14 updated with GAP-151
- [ ] Tier 2/3 personas (P4, P7, P8, P9) AC docs deferred (follow-up gap)

## Out of Scope

- **Execute the reviews** (GAP-152 — sibling gap handles execution with these criteria)
- **Tier 2/3 tenant persona AC** — defer until Tier 1 reviews complete and stable
- **Secondary persona AC** (Student/Parent/Teacher/Admin/others × tenant contexts) → **GAP-153** (sibling, extends this gap's template to user-within-tenant journeys)
- **Automated AC testing** (future — could become E2E test suite)

## Dependencies

- `00-brd/personas-catalog.md` (already exists — source for persona attributes)
- Existing persona-based-business-review.md skill (already exists — integrates this gap's output)

## Related

- GAP-050 — persona-based review process (parent — this gap provides the missing AC framework)
- GAP-051..064 — feature gaps derived from initial persona scan (AC docs reference these)
- GAP-152 — execute persona review round 1 (consumes this gap's output + GAP-153's output)
- GAP-153 — secondary persona AC (sibling — extends scope to Student/Parent/Teacher/Admin × tenant contexts)
- Rule: `.claude/rules/meta-gap-priority.md` — business-logic tier

## Log

- 2026-04-20 — Created. User raised need for formal AC per persona to make review reproducible + measurable. Initial scan (GAP-050 2026-04-14) didn't create AC — catalog v1 stopped at classification.
