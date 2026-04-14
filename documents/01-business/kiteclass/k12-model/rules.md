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

## Log
- 2026-04-14 — Initial rules (GAP-054)
