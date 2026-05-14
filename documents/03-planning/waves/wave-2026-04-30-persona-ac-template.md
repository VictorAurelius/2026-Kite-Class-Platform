---
title: Wave Persona-AC-Template — Full ship GAP-151 (template + 4 Tier-1 AC docs + skill update)
status: active
created: 2026-04-30
updated: 2026-04-30
gaps: [GAP-151]
deferred_to_next_wave: []
deferred_separate_track: [GAP-152, GAP-153]
sister_waves: [wave-2026-04-29-business-correctness, wave-2026-04-29-legal-brd-phase1, wave-2026-04-29-legal-brd-phase1-5]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave Persona-AC-Template — Cluster Pack 15 (11th wave-pack)

**Wave date:** 2026-04-30 (resumed 2026-04-30 after foundation paused 2026-04-29 evening due to transient skill-Edit errors; rebased onto current main b81e67d9 after 14 unrelated commits merged during pause)
**Cluster theme:** Full ship GAP-151 — Persona-Specific Acceptance Criteria framework (template + 4 Tier-1 per-persona AC docs + skill integration). First real test of new `docs-only-skeleton-agent.md` template variant codified Wave 14 Agent D.
**Strategy reference:** `meta-gap-priority.md` §3 — Business-Logic-P0 tier (sister cluster của Wave Business Correctness 2026-04-29 + Wave Legal-BRD Phase 1+1.5). Pattern reuse: Wave 13/14 cadence (~30-35 min) extends naturally to AC docs scope.

## Why this wave

- GAP-151 has 4 deliverables that fit wave-pack pattern:
  - Deliverable 1 (template) → foundation
  - Deliverable 2 (4 Tier-1 AC docs) → 4 parallel agents
  - Deliverable 3 (skill update) → foundation
  - Deliverable 4 (README index) → foundation
- 4 disjoint persona AC docs perfect parallel-agent fit (one per persona)
- Closes GAP-151 fully (🟢 DONE — all 8 ACs met) → unblocks GAP-152 (Round 1 review execution, next wave-pack candidate)
- **First real test** of `docs-only-skeleton-agent.md` template variant (codified Wave 14, 2nd recurrence threshold) — validates pattern-reuse exit criterion
- Resume from paused state — foundation work (template + README) untracked-but-written; skill edits failed transient → retry succeeded

## Scope

| # | Track | Deliverable | Agent | Disjoint files |
|:-:|-----|-------------|:-----:|----------------|
| 1 | Foundation | Template + skill update + READMEs + ROADMAP | coordinator | `documents/00-brd/persona-criteria/_TEMPLATE.md` + `documents/00-brd/persona-criteria/README.md` + `documents/00-brd/README.md` + `.claude/skills/quality/persona-based-business-review.md` + `documents/04-quality/gaps/ROADMAP.md` + this wave plan |
| 2 | **GAP-151 Tier-1 AC** | P1 Solo Teacher AC (15-25 ACs) | A | `documents/00-brd/persona-criteria/P1-solo-teacher.md` (NEW) |
| 3 | **GAP-151 Tier-1 AC** | P2 Small Tutoring Center AC (15-25 ACs) | B | `documents/00-brd/persona-criteria/P2-small-center.md` (NEW) |
| 4 | **GAP-151 Tier-1 AC** | P3 Medium Education Center AC (20-30 ACs) | C | `documents/00-brd/persona-criteria/P3-medium-center.md` (NEW) |
| 5 | **GAP-151 Tier-1 AC** | P5 Public/Private K-12 School AC (25-30 ACs, **largest, USER PRIORITY**) | D | `documents/00-brd/persona-criteria/P5-k12-school.md` (NEW) |

## Deferred (separate track)

- **GAP-152** — Execute Persona Review Round 1 (consumes this wave's AC docs). Next wave-pack after GAP-151 lands.
- **GAP-153** — Secondary persona AC (Student/Parent/Teacher/Admin within tenant — extends template to user-within-tenant journeys). Phase 2 of GAP-151 expansion.
- **Tier 2/3 AC docs** (P4/P6/P7/P8/P9/P10) — defer until Tier-1 reviews stable per GAP-151 §Out-of-Scope.

## File overlap analysis

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/00-brd/persona-criteria/_TEMPLATE.md` | foundation only | None |
| `documents/00-brd/persona-criteria/README.md` | foundation only | None |
| `documents/00-brd/persona-criteria/P1-solo-teacher.md` (NEW) | A only | None |
| `documents/00-brd/persona-criteria/P2-small-center.md` (NEW) | B only | None |
| `documents/00-brd/persona-criteria/P3-medium-center.md` (NEW) | C only | None |
| `documents/00-brd/persona-criteria/P5-k12-school.md` (NEW) | D only | None |
| `documents/00-brd/README.md` | foundation only | None |
| `.claude/skills/quality/persona-based-business-review.md` | foundation only | None |
| `documents/00-brd/personas-catalog.md` | A,B,C,D (read-only) | **SOFT** — read-only |
| `documents/00-brd/persona-criteria/_TEMPLATE.md` | A,B,C,D (read-only after foundation merge) | **SOFT** — read-only |

Net: **0 HARD, 1 SOFT** (read-only `personas-catalog.md` citation by all 4 agents). Template is read-only by agents (created in foundation, all agents copy structure).

## Agent workflow

Per `feedback_parallel_agent_strategy.md` + `feedback_worktree_absolute_path_contamination.md` + Wave 13/14 lessons:

1. Each agent gets `isolation: "worktree"` + `run_in_background: true` per `agent-background-spawn-default.md` (rule #705 codifies pattern)
2. Branches off main (after this foundation PR merges)
3. Agent verify cwd: `pwd | grep -q "\.claude/worktrees/" || abort`
4. Branches: `feat/wave-persona-ac-gap-151-{persona-slug}-skeleton`
5. **NEW: use `docs-only-skeleton-agent.md` template** (variant codified Wave 14) — first real test
6. Coordinator merges sequentially: A → B → C → D
7. **Prune worktrees BEFORE final merge** (Wave 13 lesson; Wave 14 confirmed prevents glitch)
8. Status flip GAP-151 🔵 OPEN → 🟢 DONE on closure PR (all 8 ACs met — NOT PARTIAL because Tier 2/3 + secondary personas already documented as out-of-scope in gap §Out-of-Scope)

## Acceptance criteria (wave-level, mirrors GAP-151 ACs)

- [ ] `_TEMPLATE.md` exists với 6 AC categories (foundation)
- [ ] 4 Tier-1 AC docs populated (15-30 ACs each, total 60-120 ACs)
- [ ] Each AC has `AC-<CAT>-<NUM>` ID + Test + Fail signal (per-agent constraint)
- [ ] Gap linkage section populated (cross-reference GAP-051..064 + recent legal-BRD GAP-180..186)
- [ ] `persona-based-business-review.md` skill updated v1.1 → v1.2 (foundation)
- [ ] `persona-criteria/README.md` index created (foundation)
- [ ] `00-brd/README.md` directory map references persona-criteria/ (foundation)
- [ ] ROADMAP entry (kickoff + closure)
- [ ] Tier 2/3 personas explicitly out-of-scope (already in gap §Out-of-Scope)
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended với wall-clock + lessons

## Wall-clock target

Per Wave 13/14 calibration: docs-only-skeleton ~5-7 min/agent + sequential merge ~3 min/PR + closure ~10 min.

- Foundation PR (template + 2 READMEs + skill + ROADMAP): ~25 min (foundation work in 2 parts — pre-pause + post-resume)
- 4 parallel agents: ~7-10 min wall (each 6-8 min agent-time, parallel; AC derivation slightly longer than pure skeleton)
- Sequential merge: ~12 min
- Closure (GAP-151 → DONE + ROADMAP + retrospective): ~12 min
- **Total wave: ~60-70 min** (slight over Wave 14's 30 min because AC derivation requires real persona-specific reasoning, not pure skeleton)

## Per-agent skeleton requirements

### Agent A — P1 Solo Teacher AC

- **Persona:** Solo Teacher (gia sư tự do)
- **Scale:** 1 teacher, 5-50 students, 1-5 courses
- **Profile:** Tự do/part-time, có thể là giáo viên chính thức ngoài giờ
- **Tier:** FREE → PRO (low budget tolerance)
- **Reviewer profile:** "Gia sư tiếng Anh part-time, 30 học sinh tại TPHCM, dạy ngoài giờ + cuối tuần"
- **AC count target:** 15-25 (smallest scope — solo, no admin staff, no parent portal)
- **Critical concerns:** ease of setup, mobile-friendly, low monthly fee, simple invoicing
- **Cross-link gaps:** GAP-051 (xlsx — relevant if 50 students), GAP-052 (parent portal — N/A solo), GAP-055 (report card — N/A informal)

### Agent B — P2 Small Tutoring Center AC

- **Persona:** Small Tutoring Center (trung tâm nhỏ / lớp học thêm)
- **Scale:** 1-3 teachers, 20-100 students, 3-10 classes
- **Profile:** Chủ trung tâm tự dạy + thuê 1-2 giáo viên, dạy thêm văn-toán-anh-lý-hóa
- **Tier:** PRO → PREMIUM
- **Reviewer profile:** "Chủ lớp học thêm Toán-Anh tại Hà Nội, 60 học sinh, 2 giáo viên thuê + tự dạy"
- **AC count target:** 15-25 (small ops + light financial admin)
- **Critical concerns:** simple billing, parent communication via Zalo, attendance tracking, basic gradebook
- **Cross-link gaps:** GAP-051 (xlsx for 60 students), GAP-052 (parent portal — relevant for 60 students × 2 parents), GAP-063 (Zalo notification), GAP-185 (billing/VAT — VND invoice)

### Agent C — P3 Medium Education Center AC

- **Persona:** Medium Education Center (trung tâm quy mô vừa)
- **Scale:** 5-20 teachers, 100-500 students, 10-50 classes
- **Profile:** Organized với dedicated admin, multiple subjects
- **Tier:** PREMIUM → ENTERPRISE
- **Reviewer profile:** "Giám đốc trung tâm Anh ngữ tại Đà Nẵng, 250 học sinh, 12 giáo viên + 3 admin staff, multi-subject"
- **AC count target:** 20-30 (full ops + financial + multi-role admin)
- **Critical concerns:** payroll/commission cho 12 teachers, multi-class scheduling, parent communication scale, financial reporting per teacher, MoET licensing compliance
- **Cross-link gaps:** GAP-051, GAP-052, GAP-053 (academic year structure), GAP-054 (multi-subject), GAP-057 (payroll teacher commission), GAP-064 (SCORM/xAPI), GAP-185 (billing/VAT)

### Agent D — P5 Public/Private K-12 School AC (USER PRIORITY)

- **Persona:** Public/Private K-12 School (trường tiểu học/THCS/THPT)
- **Scale:** 50+ teachers, 500-3000 students, 30+ classes, 10-30 staff
- **Profile:** Hierarchical (principal, VPs, department heads, teachers, students, **PARENTS** strict, staff)
- **Tier:** ENTERPRISE only (regulated)
- **Reviewer profile:** "Hiệu trưởng trường THCS công lập 800 học sinh tại quận trung tâm Hà Nội, 50 giáo viên, 15 admin/staff, theo MOET regulations"
- **AC count target:** 25-30 (largest scope — hierarchy + compliance + scale + parent portal)
- **Critical concerns:** MOET reporting compliance, GVCN (homeroom teacher) workflow, official report card format (báo cáo học bạ + điểm), conduct grade (hạnh kiểm), parent portal critical, attendance per period, child protection (GAP-186), backup data ownership
- **Cross-link gaps:** GAP-051 (xlsx 500-3000 rows), GAP-052 (parent portal **CRITICAL**), GAP-055 (official report card VN format), GAP-056 (homeroom teacher GVCN), GAP-058/059 (conduct + period attendance), GAP-060/061 (period attendance + promotion logic), GAP-186 (child protection — Phase 1 skeleton from Wave 14), GAP-180 (TOS for school), GAP-184 (data retention 5y educational records)

## Per-agent constraints (enforced)

All 4 agents MUST:
- Path constraint: only their assigned `P<N>-*.md` file — KHÔNG touch README, _TEMPLATE.md, sibling files, or any other path
- Frontmatter standard: copy from `_TEMPLATE.md` (already in foundation merge) — markdown-header style
- Each AC MUST have: ID (AC-CAT-NNN), Statement, Test, Fail signal, Status (blank — filled at GAP-152 review time), Linked gap (existing GAP-XXX or "—" if NEW)
- 6 AC categories: Onboarding / Daily Operations / Financial-Admin / Communication / Edge Cases / Exit-Termination
- Cross-link verification: every `[GAP-XXX](path)` must resolve — test với existing gap files
- Vietnamese prose default; English for technical/legal terms (MOET, VAT, SCORM, etc.)
- KHÔNG flip GAP-151 Status — coordinator handles per `gap-done-discipline.md`
- Worktree verify: `pwd | grep -q "\.claude/worktrees/" && git branch --show-current | grep -q "^feat/wave-persona-ac-"`
- RELATIVE paths only

## Lessons-learned (filled AFTER wave merges)

(Filled per `reference/retrospective-checklist.md`)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold for all 4 agents?

### File overlap accuracy
- [ ] Did predicted overlap (0 HARD, 1 SOFT) match actual?

### Wall-clock variance
- [ ] Estimate ~60-70 min vs actual?

### Agent template effectiveness
- [ ] **First real test of `docs-only-skeleton-agent.md`** — held without adjustment? AC docs are skeleton-with-content (15-30 derived ACs vs pure structural skeleton from Wave 13/14) — does template scale?

### Pattern reuse
- [ ] AC derivation pattern reusable for GAP-153 (secondary personas)?
- [ ] Coverage % calculation thresholds (85/60/30) align with reviewer expectations?

### 4+-agent hazards (Wave 13/14 lessons applied)
- [ ] Did coordinator prune worktrees before final merge?
- [ ] Any coordinator cd contamination?
- [ ] Local main glitch recurrence?

### Session-resume hazards (NEW Wave 15)
- [ ] How did 14-commit-ahead main rebase work?
- [ ] Untracked files preserved through branch recreation?
- [ ] Skill edit transient errors recoverable?
