---
title: Wave Secondary-Persona-AC — Full ship GAP-153 Phase 1 (8 P0 secondary persona AC docs, unblocks GAP-152)
status: active
created: 2026-04-30
updated: 2026-04-30
gaps: [GAP-153]
deferred_to_next_wave: []
deferred_separate_track: [GAP-281, GAP-282]
sister_waves: [wave-2026-04-29-business-correctness, wave-2026-04-29-legal-brd-phase1, wave-2026-04-29-legal-brd-phase1-5, wave-2026-04-30-persona-ac-template]
unblocks: [GAP-152]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave Secondary-Persona-AC — Cluster Pack 16 (12th wave-pack)

**Wave date:** 2026-04-30 (same-day continuation of Wave 15 Persona-AC-Template)
**Cluster theme:** Full ship GAP-153 Phase 1 — 8 P0 secondary persona AC docs (Student/Parent/Teacher/Admin per tenant context). Path B chosen over Path A để comply với `gap-done-discipline.md` §3 (avoid PARTIAL closure-loop on GAP-152 P5 review).
**Strategy reference:** `meta-gap-priority.md` §3 Business-Logic-P0 tier. Sister cluster của Wave 15. **3rd consecutive wave reusing `_TEMPLATE.md`** (validates pattern reuse exit criterion from `docs-only-skeleton-agent.md` template variant codified Wave 14).

## Why this wave

- GAP-152 §Dependencies: "Blocked by GAP-153 — secondary persona AC must exist for P5 review to be meaningful"
- Without GAP-153 Phase 1, GAP-152 P5 K-12 review would miss Student/Parent/GVCN UX (the most painful K-12 user touches)
- Per `gap-done-discipline.md` §3 PARTIAL exit-ramp would force Round 1.5 refresh after GAP-153 lands — closure-loop debt
- Path B = ship GAP-153 first (this wave) → ship GAP-152 fully Wave 17 (clean DONE)
- **Pattern reuse milestone:** 3rd consecutive wave using `_TEMPLATE.md` from Wave 15 + `docs-only-skeleton-agent.md` template variant from Wave 14

## Scope

| # | Track | Deliverable | Agent | Disjoint files |
|:-:|-----|-------------|:-----:|----------------|
| 1 | Foundation | secondary/ subdir + README + parent README extension + personas-catalog.md update + ROADMAP | coordinator | `documents/00-brd/persona-criteria/secondary/README.md` (NEW) + `documents/00-brd/persona-criteria/README.md` + `documents/04-quality/gaps/ROADMAP.md` + this wave plan |
| 2 | **GAP-153 Phase 1** | student-in-P2 + student-in-P3 (student at small + medium center) | A | `documents/00-brd/persona-criteria/secondary/student-in-P2.md` (NEW) + `student-in-P3.md` (NEW) |
| 3 | **GAP-153 Phase 1** | student-in-P5 + parent-in-P5 (USER critical K-12 pair) | B | `documents/00-brd/persona-criteria/secondary/student-in-P5.md` (NEW) + `parent-in-P5.md` (NEW) |
| 4 | **GAP-153 Phase 1** | teacher-employee-in-P3 + teacher-employee-in-P5 | C | `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P3.md` (NEW) + `teacher-employee-in-P5.md` (NEW) |
| 5 | **GAP-153 Phase 1** | admin-in-P3 + admin-in-P5 | D | `documents/00-brd/persona-criteria/secondary/admin-in-P3.md` (NEW) + `admin-in-P5.md` (NEW) |

**Total Phase 1:** 8 secondary persona AC docs across 4 agents × 2 docs each.

## Deferred (separate track — file at closure)

- **GAP-281** (NEW) — Phase 2 P1 cells (4 cells): student-in-P1 + parent-in-P2 + parent-in-P3 + teacher-employee-in-P2
- **GAP-282** (NEW) — Phase 3 P2 cells (8 cells): accountant + receptionist + IT staff + parent rep × P3/P5

## File overlap analysis

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| 8 NEW persona AC files in `secondary/` | A,B,C,D each their pair | None |
| `documents/00-brd/persona-criteria/secondary/README.md` | foundation only | None |
| `documents/00-brd/persona-criteria/README.md` | foundation only | None |
| `documents/00-brd/persona-criteria/_TEMPLATE.md` | A,B,C,D (read-only) | **SOFT** — read-only |
| `documents/00-brd/personas-catalog.md` | A,B,C,D (read-only) | **SOFT** — read-only |
| Sibling tenant AC docs `P2/P3/P5-*.md` | A,B,C,D (read-only context) | **SOFT** — read-only |

Net: **0 HARD, 3 SOFT** (all read-only citations). Each agent owns 2 unique files with role+tenant suffix — guaranteed disjoint.

## Agent workflow

Per `feedback_parallel_agent_strategy.md` + `feedback_worktree_absolute_path_contamination.md` + Wave 13/14/15 lessons + `agent-background-spawn-default.md` rule #705:

1. Each agent: `isolation: "worktree"` + `run_in_background: true`
2. Branches: `feat/wave-secondary-persona-ac-gap-153-{topic}` (e.g. `-student-p2-p3`, `-student-parent-p5`, `-teacher-p3-p5`, `-admin-p3-p5`)
3. Branches off main (after this foundation PR merges)
4. Use `docs-only-skeleton-agent.md` template variant (Wave 14 codification)
5. **Prune worktrees BEFORE final merge** (Wave 13/14/15 lesson — applied successfully 3 times)
6. Coordinator merges sequentially: A → B → C → D
7. Status flip GAP-153 🔵 OPEN → 🟢 DONE on closure PR (all 8 ACs met — file follow-up gaps GAP-281/282 per gap §AC inline)

## Acceptance criteria (wave-level, mirrors GAP-153 ACs)

- [ ] `00-brd/persona-criteria/secondary/` directory + README created (foundation)
- [ ] 8 P0 secondary persona AC docs populated (4 agents × 2 docs each)
- [ ] Each doc follows GAP-151 `_TEMPLATE.md` structure (6 categories)
- [ ] Gap linkage populated (cross-reference GAP-051..064 + GAP-180..186)
- [ ] `personas-catalog.md` Secondary Personas section updated (foundation OR closure)
- [ ] `00-brd/persona-criteria/README.md` extended với secondary/ navigation (foundation)
- [ ] GAP-152 dependency unblocked (closure log entry)
- [ ] ROADMAP entries (kickoff + closure)
- [ ] Follow-up gaps filed: GAP-281 (P1 cells) + GAP-282 (P2 cells)
- [ ] Worktrees + branches cleaned post-merge

## Wall-clock target

Per Wave 15 calibration: docs-only-skeleton ~5-6 min/agent for 1 doc + AC derivation. Each agent ships 2 docs → ~8-10 min/agent.

- Foundation PR: ~15 min
- 4 parallel agents (each 2 docs): ~10 min wall (each 8-10 min agent-time, parallel)
- Sequential merge: ~10 min
- Closure (GAP-153 → DONE + GAP-281/282 file + ROADMAP): ~15 min
- **Total wave: ~50 min** (slight increase over Wave 15 30 min because each agent has 2x deliverables)

## Per-agent specifications

### Agent A — student-in-P2 + student-in-P3

**student-in-P2 (Small Tutoring Center context):**
- Scale: 60 students × 2 teachers, mixed văn-toán-anh-lý-hóa
- Student profile: tiểu học/THCS receiving extra-class tutoring, parent-mediated payment
- AC count: 10-15 per doc
- Critical: Zalo/SMS notification, simple class schedule, basic homework, attendance from teacher

**student-in-P3 (Medium Center context):**
- Scale: 250 students × 12 teachers, multi-subject organized
- Student profile: tiểu học → THCS → THPT depending on center, multi-class enrollment
- AC count: 10-15 per doc
- Critical: multi-class schedule, multi-teacher gradebook, parent communication coordination

**Cross-link gaps:** GAP-051 (xlsx import — student receives credentials), GAP-052 (parent portal interaction), GAP-058 (role hierarchy), GAP-063 (Zalo/SMS), GAP-064 (SCORM if relevant)

### Agent B — student-in-P5 + parent-in-P5 (USER CRITICAL PAIR)

**student-in-P5 (K-12 School context — USER PRIORITY):**
- Scale: 800 students × 50 teachers × 30 classes, MOET-regulated
- Student profile: tiểu học/THCS/THPT, parental consent <16, period-based attendance
- AC count: 15-20 per doc (largest secondary)
- Critical: bulk import account receipt, period schedule (5-10 periods/day), formal report card view (học bạ), conduct grade view (hạnh kiểm), GVCN communication, parent-mediated payment, child protection compliance (no off-platform DMs với teachers)

**parent-in-P5 (K-12 School context — LEGAL MANDATE):**
- Scale: 800 students × ~1.5 parents/student = ~1200 parent accounts
- Parent profile: legal right to monitor child's data per Luật Giáo dục Đ.83
- AC count: 15-20 per doc
- Critical: parental consent flow PDPL Art 16, child academic monitoring, child attendance + conduct view, fee payment, GVCN communication, child safety reporting (mandatory reporting per Law on Children 2016)

**Cross-link gaps:** GAP-051 (bulk import — student/parent accounts), GAP-052 (parent portal — CRITICAL), GAP-055 (report card MOET format), GAP-056 (homeroom GVCN), GAP-058 (role hierarchy), GAP-060/061 (period attendance + promotion logic), GAP-063 (Zalo/SMS), GAP-180 (TOS school-parent), GAP-184 (data retention 5y educational + 6mo sensitive minor), GAP-186 (child protection — CRITICAL)

### Agent C — teacher-employee-in-P3 + teacher-employee-in-P5

**teacher-employee-in-P3 (Medium Center context):**
- Scale: 12 teachers, varied commission % (50-80%), multi-subject
- Teacher profile: hired employee, paid via commission, tracks own classes + students
- AC count: 12-18 per doc
- Critical: own class schedule view, gradebook for own classes, commission earning visibility, payroll receipt, BHXH/BHYT contribution tracking, peer collaboration (subject lead approvals)

**teacher-employee-in-P5 (K-12 School context — GVCN + bộ môn split):**
- Scale: 50 teachers (5 GVCN + 45 bộ môn), MOET-regulated, fixed salary + allowances
- Teacher profile: GVCN (homeroom — daily attendance + weekly conduct + monthly parent contact) vs bộ môn (subject teacher — multi-class gradebook), TT 22/2021 evaluation
- AC count: 15-20 per doc (split scope cho GVCN + bộ môn responsibilities)
- Critical: GVCN daily roll-call workflow, conduct grade entry (hạnh kiểm), parent communication scheduling, multi-period schedule, exam invigilation roster, sổ đầu bài digital (TT 32/2020), MOET compliance reporting

**Cross-link gaps:** GAP-053 (academic year), GAP-054 (multi-subject), GAP-056 (GVCN — CRITICAL for P5), GAP-057 (commission/payroll), GAP-058 (conduct), GAP-060 (period attendance), GAP-062 (payroll bank), GAP-064 (SCORM/xAPI optional)

### Agent D — admin-in-P3 + admin-in-P5

**admin-in-P3 (Medium Center context — multi-role RBAC):**
- Scale: 3 admin staff (giám đốc / lễ tân / kế toán) + 12 teachers + 250 students
- Admin profile: tenant-level operations, NOT same as P3 owner-as-tenant (separate role within tenant)
- AC count: 12-18 per doc
- Critical: RBAC role assignment, bulk user import oversight, financial reporting per teacher, MoET licensing renewal, multi-class scheduling conflict resolution, parent complaint handling

**admin-in-P5 (K-12 School context — văn phòng/giáo vụ):**
- Scale: 15 admin/staff (văn phòng + giáo vụ + thư viện + y tế + bảo vệ + IT) + 50 teachers + 800 students
- Admin profile: K-12 organized hierarchy (Hiệu trưởng → Phó hiệu trưởng → Tổ trưởng → Giáo vụ), MOET-mandated workflows
- AC count: 15-20 per doc
- Critical: MOET reporting workflow (TT 22/2021 + TT 32/2020), staff vetting (background check), teacher payroll batch, bulk parent communication coordination, school-year calendar management, budget per department, audit log review

**Cross-link gaps:** GAP-051 (bulk import oversight), GAP-053 (academic year), GAP-058 (role hierarchy — CRITICAL), GAP-061 (promotion logic admin role), GAP-062 (payroll), GAP-180 (TOS), GAP-184 (data retention compliance), GAP-185 (billing VAT), GAP-186 (child protection compliance — vetting + reporting)

## Per-agent constraints (enforced)

All 4 agents MUST:
- Path constraint: only 2 assigned `<role>-in-P<N>.md` files in `secondary/` — KHÔNG touch README, _TEMPLATE.md, sibling files, or any other path
- Use `_TEMPLATE.md` structure (6 categories) — adapt for role-perspective (NOT tenant perspective)
- Frontmatter: copy template format — Persona ID = secondary role; Tier = same as tenant context
- Cross-link verification: every `[GAP-XXX](path)` must resolve
- AC format: ID (AC-CAT-NNN), Statement, Test, Fail signal, Status BLANK, Linked gap
- 6 AC categories per doc: Onboarding / Daily Operations / Financial-Admin / Communication / Edge Cases / Exit-Termination
- AC count target: 10-20 per doc (secondary smaller than tenant; B+C+D may have 15-20 for K-12 scope)
- Vietnamese prose default; English for technical/legal terms
- KHÔNG flip GAP-153 Status — coordinator handles per `gap-done-discipline.md`
- Worktree verify: `pwd | grep -q "\.claude/worktrees/" && git branch --show-current | grep -q "^feat/wave-secondary-persona-"`
- RELATIVE paths only

## Lessons-learned (filled AFTER wave merges)

(Filled per `reference/retrospective-checklist.md`)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold for all 4 agents?

### File overlap accuracy
- [ ] Did predicted overlap (0 HARD, 3 SOFT) match actual?

### Wall-clock variance
- [ ] Estimate ~50 min vs actual?

### 2-doc-per-agent pattern
- [ ] First wave với 2 deliverables per agent — does pattern hold? Ratio agent-time / agents = ~2x single-doc waves?

### Pattern reuse milestone
- [ ] 3rd consecutive wave using `_TEMPLATE.md` (Wave 14 BRD + Wave 15 tenant + Wave 16 secondary) — template stable?

### 4+-agent hazards (Wave 13/14/15 lessons applied)
- [ ] Did coordinator prune worktrees before final merge?
- [ ] Any coordinator cd contamination?

### Closure-loop avoided
- [ ] Path B execution (GAP-153 first) — successfully avoided GAP-152 PARTIAL closure debt?
