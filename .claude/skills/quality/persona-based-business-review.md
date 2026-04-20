---
description: "Dùng khi review business logic correctness, user nói 'persona review', 'end-user review', 'test từng đối tượng', 'nghiệp vụ đủ chưa', 'feature gap', 'role-play review'. Nhập vai từng persona → walk through nghiệp vụ → phát hiện missing features."
---

# Skill: Persona-Based Business Review

**Version:** 1.0
**Created:** 2026-04-14
**Purpose:** Review business coverage bằng cách nhập vai (role-play) từng persona sử dụng platform → phát hiện gaps về core features thiếu.

**Project principle:** "SAAS này phải tạo sân chơi chung cho TẤT CẢ đối tượng thỏa mãn nhu cầu core của quản lý và học trực tuyến."

---

## When to Use

- Review feature completeness sau mỗi milestone
- Trước launch GA
- Khi user raise "thiếu feature X"
- Quarterly business coverage check
- Onboarding specific persona type mới

## Process

### Step 1: Identify All Personas

Reference: `documents/00-brd/personas-catalog.md`

Canonical list (review quarterly):
1. Solo Teacher (gia sư tự do)
2. Small Tutoring Center (trung tâm nhỏ/học thêm)
3. Medium Education Center (trung tâm vừa)
4. Large Education Chain (chuỗi/franchise)
5. Public/Private K-12 School (trường cấp 1-3)
6. University/College (đại học — maybe out of scope)
7. Corporate Training Dept
8. Online Course Creator
9. International/Bilingual School
10. Special Education Center

Plus secondary personas (users within tenant):
- Admin/Director, Teacher, Student, Parent, Accountant, Receptionist

### Step 2: Role-Play Each Persona

Nhập vai từng persona với **realistic scale** và **walk through full workflow**:

```
Persona: Public K-12 School (500 học sinh, 30 giáo viên, 10 admin/staff)

Journey:
1. Discovery — find KiteClass via search
2. Signup — register as tenant
3. Onboarding — provision school instance
4. Setup — create classes, courses, academic year
5. Import users — add 500 students + 30 teachers + 10 staff
6. Assignments — assign teachers to classes, students to classes
7. Daily ops — attendance, grades, communication
8. Reporting — monthly reports, semester reports
9. Parent engagement — parent accounts, notifications
10. End-of-year — grade finalization, promotion, transcripts
```

At each step, ask:
- Can persona complete this trong realistic time?
- Có friction/manual work không?
- Missing feature nào?

### Step 3: Catalog Gaps Per Persona

For each persona, document:

```markdown
## Persona: [Name]

### Scale
- Users: {count}
- Data volume: {courses/classes/students}
- Usage pattern: {daily/weekly/seasonal}

### Critical Use Cases
1. ...
2. ...

### Coverage Analysis
| Use Case | Supported? | Gap |
|----------|:---------:|-----|
| Bulk import students | ❌ | Need xlsx import (GAP-051) |
| Parent portal | ❌ | Need parent accounts (GAP-052) |
| ... |

### Verdict
- Feasibility: ✅ Feasible / ⚠️ Partial / ❌ Not feasible
- Critical gaps: N (blocking launch)
- Nice-to-have: N (future)
```

### Step 3.5: State-Check Before Creating Gap Files (BẮT BUỘC)

Persona role-play is speculative — it generates candidates based on "what a persona would want", not "what's missing from code". Before Step 4, grep the actual code/infra/docs paths each candidate gap would touch:

- **Fully shipped** → do NOT file; note in persona report as "coverage confirmed"
- **Partial** → file as 🟡 PARTIAL with mandatory `## Current State (verified YYYY-MM-DD)` table
- **Nothing** → file as 🔵 OPEN normally

Reference: `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Skipping this step produces rewrite debt (xem incident GAP-190/197 2026-04-20).

### Step 4: Create Gap Files

For each critical gap found (that passed Step 3.5 state-check):
- Create `GAP-XXX-feature-name.md` using `documents/04-quality/gaps/_TEMPLATE.md`
- Reference persona that needs it
- Priority based on how many personas blocked

### Step 5: Prioritize by Persona Coverage

Priority formula:
```
priority = (# personas blocked) × (persona market size) × (blocking severity)
```

High priority: gap blocks multiple personas OR blocks large market persona (school 500 students).

---

## Role-Play Checklist per Persona

When nhập vai 1 persona:

- [ ] Scale realistic (100, 500, 5000 users)
- [ ] Timeline realistic (peak moments: school year start, enrollment period)
- [ ] Budget realistic (FREE tier vs enterprise)
- [ ] Technical skill realistic (admin có biết code? có IT department?)
- [ ] Integrations needed (payroll system, SMS gateway, parent app?)
- [ ] Regulatory requirements (MOE for schools, TCT for tax)
- [ ] Cultural fit (Vietnamese conventions)

---

## Example Gap (user raised)

### Gap Example: Bulk Student Import (GAP-051)

**Persona trigger:** Public K-12 School

**Role-play:**
```
New Year Sep 2026: Principal signs up KiteClass.
School has 500 students, 30 teachers, 15 classes.

Step: "Add students to system"
Expected: Upload xlsx → 500 accounts auto-created, sorted by class
Actual: 500 students must register individually → send credentials
         to teachers manually → teachers assign to classes manually

Impact:
- Week 1 lost to account management
- Parents frustrated (kids can't start)
- Teachers overwhelmed with admin
- Principal considers switching platforms

Verdict: 🔴 BLOCKING — school persona cannot launch school year on time
```

**Gap:** Need bulk import xlsx → auto-create accounts + class assignment.

---

## Output Format

```markdown
# Persona-Based Business Review Report

**Ngày:** YYYY-MM-DD
**Reviewer:** Claude (role-play) + Product Owner (sign-off required)

## Executive Summary

- Personas reviewed: X
- Personas fully supported: X
- Personas partially supported: X
- Personas NOT supported: X
- Critical gaps identified: X
- Total new gaps filed: X

## Per-Persona Analysis

### 1. Solo Teacher
...

### 2. Small Center
...

## Cross-Persona Gaps

Gaps that affect multiple personas:
- GAP-XXX: Bulk import (affects Schools, Large Centers)
- GAP-YYY: Parent portal (affects K-12 Schools, Language Centers for kids)

## Action Items

1. Create gap files for critical findings
2. Prioritize by persona coverage
3. Re-review after implementation
```

---

## Integration với Other Skills

| Skill | Relation |
|-------|----------|
| `business-gap-check` | Tech check; this skill is business check |
| `simulation-gap-finder` | 3-axis matrix; this skill adds persona-as-axis |
| `quality-audit` | Tech quality; this skill = business quality |
| `pre-flight-check project` | Include persona review mỗi quarterly check |

---

## Rules

- ✅ Nhập vai CỤ THỂ (500 students, 30 teachers — not vague)
- ✅ Walk through TOÀN BỘ journey (signup → daily → termination)
- ✅ Vietnamese context (MOE, TCT, VN law, cultural norms)
- ✅ Realistic personas (data from market research, not assumptions)
- ❌ Không stop ở happy path — include edge cases
- ❌ Không review code implementation — focus business coverage

## Mandatory Frequency

- **Before GA launch:** full review all personas
- **Quarterly:** refresh review per persona
- **Per major feature:** check impact cross-persona
- **On user complaint:** deep-dive specific persona

## Skill Contents

- This SKILL.md — methodology
- Reference: `documents/00-brd/personas-catalog.md` (canonical list)
- Reference: `documents/00-brd/persona-reviews/` (output reports)
- Integration: create gap files via `gap-to-pr-converter` after review
