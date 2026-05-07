# K-12 Multi-Subject Model — Business Rules

**Domain:** k12-model
**Source:** GAP-054, ADR-001
**Feature flag:** ENABLE_K12_MODEL (per tenant opt-in)

## Rules

### HomeroomClass (Lớp chính)
| ID | Rule | Enforcement |
|----|------|-------------|
| BR-HRC-001 | Belongs to 1 AcademicYear | FK NOT NULL |
| BR-HRC-002 | Unique (year, grade, section) | Unique index |
| BR-HRC-003 | 1 homeroom teacher (GVCN) | homeroomTeacherId |
| BR-HRC-004 | Capacity 1+ students | CHECK constraint |
| BR-HRC-005 | currentEnrolled ≤ capacity | CHECK + service guard |

### SubjectSection (Lớp bộ môn)
| ID | Rule | Enforcement |
|----|------|-------------|
| BR-SSEC-001 | Belongs to 1 HomeroomClass + 1 Course | FK NOT NULL |
| BR-SSEC-002 | Unique (homeroomClass, course) | Unique index |
| BR-SSEC-003 | 1 subject teacher (can be null initially) | teacherId nullable |

### Curriculum (Chương trình học)
| ID | Rule | Enforcement |
|----|------|-------------|
| BR-CUR-001 | Unique grade per tenant | Unique index |
| BR-CUR-002 | subjects JSONB: courseId → {weeklyHours, weight} | Application validation |
| BR-CUR-003 | Weights used for weighted grade average | GradeCalculator |

### SubjectGrade (Điểm)
| ID | Rule | Value |
|----|------|-------|
| BR-SG-001 | Unique (student, section, semester) | Unique index |
| BR-SG-002 | Scores 0.0 - 10.0 | CHECK + entity validation |
| BR-SG-003 | Auto compute: (regular×1 + midterm×2 + final×3)/6 | `computeAverage()` |
| BR-SG-004 | Letter grade derivation | Giỏi≥8, Khá≥6.5, TB≥5, Yếu<5 |

## Grading Scale (VN 10-point)

| Average | Letter Grade |
|---------|-------------|
| ≥ 8.0 | Giỏi |
| 6.5 - 7.99 | Khá |
| 5.0 - 6.49 | Trung bình |
| < 5.0 | Yếu |

## Feature Flag Strategy (ADR-001 Strangler Fig)

- `ENABLE_K12_MODEL=false` (default) → use existing Class/Enrollment
- `ENABLE_K12_MODEL=true` → use HomeroomClass/SubjectSection/SubjectGrade

Coexistence: both models work independently. Tenant can't mix.

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Trẻ em 2016 (child protection); Luật Giáo dục 2019 K-12 chapter; PDPL Decree 13/2023 Art 17 (parental consent for under-16).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Luật Trẻ em amendment, MoET K-12 regulation update, parental-consent flow change.

## Log
- 2026-04-14 — Initial rules (GAP-054)
