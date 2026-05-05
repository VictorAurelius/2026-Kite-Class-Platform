---
title: Multi-Subject Gradebook — Business Rules (K-12 schools, TT 22/2021)
status: draft
created: 2026-05-05
updated: 2026-05-05
domain: kiteclass.multi-subject-gradebook
gaps: [GAP-323c, GAP-054]
---

# Multi-Subject Gradebook — Business Rules (K-12 schools, TT 22/2021)

> Phase 1C v1 scope only — SubjectGrade entity extension (assessment typing
> + Tổ trưởng status fields) and `GradeFormulaService` backend implementing
> ĐTBmHK + ĐTBmCN per TT 22/2021/TT-BGDĐT Đ.7. State-machine enforcement,
> Tổ trưởng workflow, multi-subject gradebook UI, bulk publish action, and
> học bạ generation hook are deferred to a Phase 1C remainder follow-up gap
> per `gap-done-discipline.md` §3 PARTIAL exit ramp.

## Frontmatter (5-attribute review per `business-logic-review.md`)

- **Source:** TT 22/2021/TT-BGDĐT (Thông tư 22/2021 quy định về đánh giá học
  sinh THCS, THPT) Điều 7 (đánh giá thường xuyên + giữa kỳ + cuối kỳ;
  ĐTBmHK = (TB.TX + GK×2 + CK×3)/6; ĐTBmCN = (ĐTBmHK1 + 2×ĐTBmHK2)/3).
  TT 32/2018/TT-BGDĐT (Chương trình GDPT 2018) defines the 13–17 môn baseline.
  MOET reporting convention (HALF_EVEN scale=1) cross-referenced via
  P5 K-12 persona review Finding 3
  (`documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md`).
- **Rationale:** TT 22/2021 is the binding national regulation for K-12
  THCS/THPT grade computation. Implementing it as a Strategy interface
  (`GradeFormulaService` + default impl) future-proofs against TT amendments
  without code spread; per `design-patterns.md` §1.3 the Strategy abstraction
  earns its keep because MOET has historically amended this thông tư every
  ~5 years (TT 58/2011 → TT 26/2020 → TT 22/2021), each shifting weights or
  aggregation rules. A single bean swap accommodates the next amendment.
- **Reviewer:** @nguyenvankiet (acting Education domain expert + Product
  Owner, solo-dev, 2026-05-05). Phase 1C v1 scope = entity + formula service
  + business docs only; full review against MOET reviewers + 3 GVCN bộ môn
  trường công lập + Tổ trưởng workflow stakeholder review queued for Phase 1C
  remainder follow-up gap (state machine + UI + Tổ trưởng workflow).
- **Compliance check:** **Compliant** — TT 22/2021 Đ.7 ĐTBmHK + ĐTBmCN
  formulas implemented in `GradeFormulaServiceImpl`; HALF_EVEN scale=1
  precision matches MOET reporting convention. PDPL N/A — grade data
  retention falls under existing `data-retention` rules (academic records
  ≥10 years per Luật Lưu trữ 2011 + Luật Giáo dục 2019). State-machine
  enforcement (DRAFT→REVIEWED→PUBLISHED) PARTIAL — column persisted, full
  enforcement deferred to follow-up gap.
- **Review cadence:** **Annual + event-driven** on TT 22/2021 amendment, on
  MoET chương trình GDPT update, or whenever a Tổ trưởng workflow audit
  surfaces a transition gap. **Next review:** 2027-05-05 OR within 30 days
  of any TT 22 / TT 32 amending decree publication.

## 1. Scope

K-12 schools (`tenants.vertical_type = 'K12_SCHOOL'`) compute and persist
multi-subject grades per TT 22/2021. Centers (`vertical_type = 'CENTER'`,
default) keep their existing per-class grade flow — they do not interact
with this domain.

The gradebook surface bridges:
- `SubjectGrade` entity (extended Phase 1C with `type`, `weight`, `status`,
  `reviewedBy`, `publishedAt`).
- `GradeFormulaService` (NEW Phase 1C — Strategy Pattern interface +
  default `GradeFormulaServiceImpl`).
- `SubjectSection` (existing GAP-054).
- `Semester` (existing GAP-053).

## 2. Vocabulary

| Term | Vietnamese | Definition |
|------|------------|------------|
| TX | Điểm thường xuyên | Regular continuous assessment, weight 1. Multiple per semester. |
| GK | Điểm giữa kỳ | Midterm exam, weight 2. Typically 1 per semester. |
| CK | Điểm cuối kỳ | Final exam, weight 3. Typically 1 per semester. |
| ĐTBmHK | Điểm trung bình môn học kỳ | Subject semester average per Đ.7 formula. |
| ĐTBmCN | Điểm trung bình môn cả năm | Subject annual average across HK1 + HK2. |
| Tổ trưởng | Tổ trưởng chuyên môn | Subject-area lead who reviews + approves grades. |
| GV bộ môn | Giáo viên bộ môn | Subject teacher who enters grades (DRAFT). |
| Hiệu trưởng | Hiệu trưởng | Principal who publishes grades (final state). |

## 3. Business Rules

### BR-GRADEBOOK-001 — ĐTBmHK formula (TT 22/2021 Đ.7)

| Attribute | Value |
|-----------|-------|
| **Value** | `ĐTBmHK = (TB.TX + GK×2 + CK×3) / 6` where `TB.TX` is the arithmetic mean of all TX records for (student, subject_section, semester). |
| **Source** | TT 22/2021/TT-BGDĐT Điều 7 §3 (national regulation, binding on all THCS/THPT). |
| **Rationale** | TX captures continuous assessment (homework, quiz, oral participation), weight 1 reflects breadth-not-depth. GK is a single midterm exam, weight 2 balances continuity vs assessment depth. CK is the final, weight 3 dominates to reflect mastery at semester end. Sum of weights 1+2+3=6 = divisor. |
| **Reviewer** | @nguyenvankiet (acting Education domain expert + Product Owner). |
| **Compliance check** | **Compliant** — formula matches TT 22/2021 Đ.7 §3 verbatim. |
| **Review cadence** | Annual + event-driven on TT 22/2021 amendment. **Next review:** 2027-05-05. |
| **Code reference** | `GradeFormulaServiceImpl#computeDTBmHK` (config key: N/A — formula encoded in Strategy default impl, not config). |

Computation is **null-tolerant**: when no TX records exist, or no GK or CK
score is present, the result is `null` (not `0`). This preserves the
"unscored" vs "scored zero" distinction in UI rendering ("—" vs "0.0").

### BR-GRADEBOOK-002 — ĐTBmCN formula (TT 22/2021 Đ.7)

| Attribute | Value |
|-----------|-------|
| **Value** | `ĐTBmCN = (ĐTBmHK1 + 2×ĐTBmHK2) / 3` |
| **Source** | TT 22/2021/TT-BGDĐT Điều 7 §4. |
| **Rationale** | HK2 builds cumulatively on HK1 mastery; weight 2 reflects "later semester demonstrates retained learning". Sum 1+2=3 = divisor. |
| **Reviewer** | @nguyenvankiet (acting Education domain expert + Product Owner). |
| **Compliance check** | **Compliant** — matches TT 22/2021 Đ.7 §4. |
| **Review cadence** | Annual + event-driven on TT 22/2021 amendment. **Next review:** 2027-05-05. |
| **Code reference** | `GradeFormulaServiceImpl#computeDTBmCN`. |

Returns `null` when either ĐTBmHK is `null` (preserves partial-state
semantics across the academic-year boundary).

### BR-GRADEBOOK-003 — Status transition order

| Attribute | Value |
|-----------|-------|
| **Value** | `DRAFT → REVIEWED → PUBLISHED`. No skipping; no backwards transitions. |
| **Source** | TT 22/2021 Đ.10 (Tổ trưởng quyền duyệt) + MoET học bạ workflow standard. |
| **Rationale** | DRAFT = GV entry (editable). REVIEWED = Tổ trưởng quality-checked (locks GV edits). PUBLISHED = Hiệu trưởng signed off (permanent in học bạ). Skipping review = no quality gate; reverting = data integrity violation in học bạ. |
| **Reviewer** | @nguyenvankiet (acting Product Owner + Education domain expert). |
| **Compliance check** | **Compliant** — matches Tổ trưởng workflow per TT 22/2021. |
| **Review cadence** | Annual. **Next review:** 2027-05-05. |
| **Code reference** | `SubjectGradeStatus` enum. **Phase 1C v1 PARTIAL** — column persisted, transition enforcement deferred to follow-up gap. |

Phase 1C v1 ships the column + enum only. The actual transition guard
(no direct status setter — service-method-only mutators with State Pattern
per `design-patterns.md` §3.3) is deferred to the Phase 1C remainder
follow-up gap. Until enforcement lands, callers MUST NOT bypass via direct
`setStatus()`; reviewer-checklist enforces.

### BR-GRADEBOOK-004 — Weight by assessment type

| Attribute | Value |
|-----------|-------|
| **Value** | TX → 1.0; GK → 2.0; CK → 3.0. |
| **Source** | TT 22/2021 Đ.7 §3 (weights cited inline with formula). |
| **Rationale** | Per BR-GRADEBOOK-001 rationale; stored explicitly per row so future tenant-level overrides (e.g., a province issues guidance allowing TX weight=2 for an experimental cohort) can be applied without code change. |
| **Reviewer** | @nguyenvankiet (acting Product Owner + Education domain expert). |
| **Compliance check** | **Compliant** — matches TT 22/2021 Đ.7 §3 baseline. |
| **Review cadence** | Annual + event-driven on TT 22/2021 amendment. |
| **Code reference** | V55 migration sets `weight DEFAULT 1.0`; entity field `SubjectGrade.weight`. |

### BR-GRADEBOOK-005 — Decimal precision (HALF_EVEN scale=1)

| Attribute | Value |
|-----------|-------|
| **Value** | All computed averages (ĐTBmHK, ĐTBmCN, intermediate TX mean) round to **1 decimal place** using `HALF_EVEN` (banker's rounding). |
| **Source** | MOET reporting convention (học bạ standard form prints 1 decimal); `RoundingMode.HALF_EVEN` reduces systematic up-bias when a school has many borderline averages (e.g., 7.85 → 7.8 not 7.9). |
| **Rationale** | 1 decimal matches what GVCN writes by hand on paper báo cáo (no false precision). HALF_EVEN avoids the "always-round-up" bias of HALF_UP across thousands of records — at large scale (3000 HS × 12 môn × 2 HK = 72000 rounding events per school per year), bias accumulates into systematically inflated school averages. |
| **Reviewer** | @nguyenvankiet (acting Education domain expert). |
| **Compliance check** | **Compliant** — 1-decimal display matches MOET reporting form; HALF_EVEN choice within MOET-allowed range (regulation does not mandate HALF_UP). |
| **Review cadence** | Annual. **Next review:** 2027-05-05. |
| **Code reference** | `GradeFormulaServiceImpl.SCALE = 1`, `ROUNDING = HALF_EVEN`. |

## 4. Out-of-scope (tracked separately)

Per `gap-done-discipline.md` §3 PARTIAL exit ramp, the following Phase 1C
items are deferred to a follow-up gap (filed at GAP-323c PARTIAL closure):

| Item | Notes |
|------|-------|
| State-machine transition enforcement (no direct status-set; service-method mutators) | BR-GRADEBOOK-003 enforcement layer; depends on State Pattern impl per `design-patterns.md` §3.3 |
| Tổ trưởng approval workflow + notification | Depends on GAP-063b notification engine + GAP-058 role hierarchy |
| Multi-subject gradebook UI (4 view variants: Admin / Hiệu trưởng / GV / Tổ trưởng) | ~10–15 days FE work; deferred to follow-up gap |
| Bulk publish action for Hiệu trưởng | Depends on state-machine enforcement |
| Học bạ generation hook on year-end PUBLISHED | Depends on GAP-055 MOET học bạ format |
| Multi-tenant Tổ trưởng assignment per subject | Depends on GAP-058 role hierarchy |

## 5. Related

- **Domain peer:** `period-attendance/` (TT 22/2021 attendance side; Phase 1A
  shipped Wave 18b1).
- **Sister gap:** GAP-054 (SubjectGrade Phase 1 — entity skeleton); this
  domain extends the entity Phase 1C.
- **Downstream:** GAP-055 (học bạ MOET format), GAP-059 (conduct grade),
  GAP-327 (MOET subject taxonomy seed).
