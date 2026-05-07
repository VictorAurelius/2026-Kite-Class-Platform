# Grade & Assignment — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

### Assignment Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-ASG-001 | Types | HOMEWORK, PROJECT, QUIZ, ESSAY |
| BR-ASG-002 | Late penalty | Configurable % per day (default 10%/day, max 50%) |
| BR-ASG-003 | Max score positive | `max_score > 0`, default 100 |
| BR-ASG-004 | Deadline required | Must have `due_date` |
| BR-ASG-005 | Only class teachers create | MAIN_TEACHER or course CREATOR/INSTRUCTOR |
| BR-ASG-006 | No submit after close | Submissions blocked after `close_date` (if set) |
| BR-ASG-007 | Score <= max_score | Submission score cannot exceed assignment max_score |

**Assignment statuses:** DRAFT, PUBLISHED, CLOSED, GRADED

### Grade Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-GRD-001 | Weighted calculation | `final_grade = sum(component_score * component_weight)` |
| BR-GRD-002 | Weights must sum to 100% | All grade components for a course must total 100% |
| BR-GRD-003 | Components | Attendance (10%), Assignments (30%), Midterm (25%), Final (35%) — configurable |
| BR-GRD-004 | Pass threshold | Default 50/100 (configurable per course) |
| BR-GRD-005 | Letter grade mapping | A+: 95-100, A: 90-94, B+: 85-89, B: 80-84, C+: 75-79, C: 70-74, D: 60-69, F: <60 |
| BR-GRD-006 | GPA scale | A/A+: 4.0, B+: 3.3, B: 3.0, C+: 2.3, C: 2.0, D: 1.0, F: 0.0 |
| BR-GRD-007 | Teacher can override | Final grade can be manually overridden with reason |

---

## 2. Flow

### Assignment Workflow
1. Teacher creates assignment -> status = DRAFT
2. Teacher publishes -> status = PUBLISHED, students notified
3. Students submit before deadline (files, text, links)
4. Late submissions: penalty applied per day (BR-ASG-002)
5. Teacher grades submissions, provides feedback
6. All graded -> status = GRADED
7. Scores feed into grade calculation

### Grade Calculation Flow
1. System collects all component scores:
   - Attendance rate from Attendance Module
   - Assignment average from submissions
   - Midterm/Final from manual grade entry
2. Calculate weighted average (BR-GRD-001)
3. Determine letter grade (BR-GRD-005)
4. Calculate GPA (BR-GRD-006)
5. Determine pass/fail (BR-GRD-004)
6. Generate transcript entry

### Late Penalty Example
```
Due: 2026-02-05 23:59
Submitted: 2026-02-07 10:00 (2 days late)
Raw score: 85/100
Penalty: 10%/day * 2 = 20%
Final score: 85 * (1 - 0.20) = 68/100
```

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Assignment created | assignment-created | Enrolled students |
| (Planned) Deadline approaching | assignment-reminder | Students not yet submitted |
| (Planned) Submission graded | grade-notification | Student |
| (Planned) Final grade published | transcript-ready | Student |

> Email templates not yet implemented.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `assignment.late-penalty-percent-per-day` | `10` | Late submission penalty |
| `assignment.max-late-penalty-percent` | `50` | Maximum cumulative late penalty |
| `assignment.max-score.default` | `100` | Default max score |
| `grade.pass-threshold` | `50` | Minimum score to pass |
| `grade.components.attendance-weight` | `10` | Attendance weight % |
| `grade.components.assignment-weight` | `30` | Assignment weight % |
| `grade.components.midterm-weight` | `25` | Midterm weight % |
| `grade.components.final-weight` | `35` | Final exam weight % |

### Database Indexes
- `idx_assignments_class_id` — Assignments per class
- `idx_assignments_status` — Filter by status
- `idx_submissions_assignment_id` — Submissions per assignment
- `idx_submissions_student_id` — Submissions per student
- `idx_grades_student_class` — Grade per student per class

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật Giáo dục 2019 (grade record obligation); PDPL 2023 (student grade is sensitive PII per Decree 13/2023).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: MoET grading regulation update, parent-portal grade-visibility complaint pattern.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
