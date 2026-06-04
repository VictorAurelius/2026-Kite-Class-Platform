---
description: "Dùng khi review business logic correctness, user nói 'persona review', 'end-user review', 'test từng đối tượng', 'nghiệp vụ đủ chưa', 'feature gap', 'role-play review', HOẶC 'pre-walk persona simulation' / 'pre-walk failure modes' / 'simulate walk' (Pre-Walk Mode, added v1.3). Nhập vai từng persona → walk through nghiệp vụ → phát hiện missing features."
---

# Skill: Persona-Based Business Review

**Version:** 1.3
**Created:** 2026-04-14
**Last Updated:** 2026-06-04
**Purpose:** Review business coverage bằng cách nhập vai (role-play) từng persona sử dụng platform → phát hiện gaps về core features thiếu. Reviewer dùng formal AC docs per persona (`documents/00-brd/persona-criteria/P<N>-*.md`) để mark PASS/PARTIAL/FAIL với evidence — replaces ad-hoc "Key needs" walkthrough trước GAP-151. **v1.3 (2026-06-04):** thêm Pre-Walk Mode per `pre-walk-persona-simulation-mandate.md` v1.0.0 — invoke skill BEFORE flow walk to surface ≥5 failure modes prospectively.

**Project principle:** "SAAS này phải tạo sân chơi chung cho TẤT CẢ đối tượng thỏa mãn nhu cầu core của quản lý và học trực tuyến."

---

## Pre-Walk Mode (added v1.3, per `pre-walk-persona-simulation-mandate.md` v1.0.0)

**When to invoke Pre-Walk Mode** — trước khi user / coordinator chạy manual walk end-to-end trên local Docker stack cho user-facing flow (signup / auth / invite / payment / tenant-switch / upload / email-driven / async). Per the mandate, this is REQUIRED, not optional.

**Difference vs full persona audit:**

| Aspect | Full persona audit (default) | Pre-Walk Mode (this section) |
|---|---|---|
| Scope | Mọi persona × tất cả features (broad) | 1 persona × 1 flow (narrow + deep) |
| Output | Coverage Analysis table /128 PASS/PARTIAL/FAIL | Numbered list 5-10 failure modes per pre-walk-persona-simulation-mandate.md §3 |
| Timing | Quarterly / pre-launch / milestone | Per Wave / PR ship user-facing flow, BEFORE walk |
| Duration | 1-3h | 5-10 min Opus agent spawn |
| Artifact location | `documents/04-quality/audits/persona-review/YYYY-MM-DD-<scope>.md` | `documents/04-quality/audits/persona-review/YYYY-MM-DD-pre-walk-<flow>.md` |

### Pre-Walk invocation pattern (agent prompt)

Spawn Opus 4.7 background agent per `agent-model-opus-default.md` + `agent-background-spawn-default.md`:

```
Wave <X> sắp ship flow <Y> (vd "Owner mời staff", "Invitee accept beta invite",
"Parent claim student"). User sẽ walk end-to-end local Docker stack persona <Z>.

Trước walk, simulate persona psychology + return ≥5 failure modes per
.claude/rules/pre-walk-persona-simulation-mandate.md §3 format:

1. Đọc persona doc `documents/00-brd/persona-criteria/<P-N>-*.md` (nếu có)
2. Step into persona mindset:
   - Tôi là <persona>. Tôi vừa mới <trigger action — click email link / submit form>.
   - Kỳ vọng tôi: <list 3-5 expectations>.
   - Lỗi tôi sợ gặp: <subdomain trùng / email format sai / token expired / network slow / 2FA wrong / payment declined / file format unsupported>.
   - Retry behavior: F5 refresh / click lại / mở tab mới / contact support — what does each path expose?
3. Cross-reference simulation-gap-finder.md failure-mode matrix (3 axis):
   - Auth state (logged-in / logged-out / token-expired / role-mismatch)
   - Sad path (input invalid / network drop / server 5xx / vendor down)
   - Locale (vi labels / VN sample data / Zalo notifications / Mon-Sat week)
4. Optional external benchmark — vendor sister product (vd Notion invite, Linear
   onboarding, Stripe checkout, Slack signup) — surface industry pattern.

Return 5-10 failure modes per §3.1 format:

  N. <1-line title>
     - (a) Where: <FE file:line / BE endpoint / gateway route / consumer queue / side-effect>
     - (b) Symptom: <browser behavior / email observed-or-not / DB state>
     - (c) Pre-walk check: <grep / Read / curl / psql command — concrete + executable>

Append "Recommended pre-walk batch fix" section sorting by confidence × impact:
- HIGH: fix trước walk
- MEDIUM: spot-check Read + grep trước walk
- LOW: defer to walk catch

Save artifact `documents/04-quality/audits/persona-review/YYYY-MM-DD-pre-walk-<flow>.md`.
```

### Pre-Walk Mode quality criteria

Output PASS khi:
- ≥5 failure modes returned (≤10 cap to avoid noise)
- Mỗi failure mode có 3 fields (a) where + (b) symptom + (c) pre-walk check
- Mỗi (c) check is concrete + executable (grep command / file:line / curl probe / DB query)
- Cross-reference 3+ axes (auth state / sad path / locale OR network / retry / device)
- Recommended batch fix section sorts findings by confidence × impact
- Artifact saved (not chat-only)

Output FAIL khi:
- <5 failure modes (insufficient persona sweep)
- Failure modes generic / non-actionable ("user might be confused")
- No file:line / endpoint citations
- Skipped artifact save

---

## When to Use

- Review feature completeness sau mỗi milestone
- Trước launch GA
- Khi user raise "thiếu feature X"
- Quarterly business coverage check
- Onboarding specific persona type mới

## Process

### Step 0 — Canonical-status lookup TRƯỚC khi emit candidates (BẮT BUỘC)

Persona role-play là speculative — generate candidates based on "what a persona would want", KHÔNG phải "what's missing from code". Step 3.5 (state-check before file gap) chặn ở filing time NHƯNG candidate vẫn đã liệt kê trong persona report → user/Claude vẫn phải triage thủ công. Step 0 chặn ngay tại emission time.

**Mandatory commands (read full output, KHÔNG `| head` truncate per `audit-to-gap-pipeline.md` §2.5 hardened protocol):**

```bash
# 1. List items đã shipped (skip nếu candidate match)
bash scripts/query-gaps.sh "" DONE ""

# 2. List items active scope (cross-ref candidate)
bash scripts/query-gaps.sh "" PARTIAL ""
bash scripts/query-gaps.sh "" OPEN ""
bash scripts/query-gaps.sh "" IN_PROGRESS ""

# 3. List items user-rejected
grep -E "Dropped:.*GAP-" documents/04-quality/gaps/ROADMAP.md
```

**Filter rule:** Emit candidate (trong persona report Coverage Analysis table) CHỈ khi:
- Title/scope KHÔNG match existing CSV row (any status)
- ID KHÔNG appear trong ROADMAP §Dropped section
- Đã shipped → note "coverage confirmed" thay vì list as gap
- Attach evidence inline: "Verified against gap-status.csv YYYY-MM-DD: GAP-XXX absent → genuinely new"

**Why mandatory:** 2026-04-20 audit `simulation-gap-finder` (sister skill) emit 12 candidates với 66% noise (7 shipped same week + 1 user-rejected). Pattern lặp lại bất cứ khi nào persona/simulation skill scan codebase mà skip canonical CSV lookup. Memory `feedback_audit_candidate_pre_filing_state_check.md` documents incident. Reference: `audit-to-gap-pipeline.md` §2.5 + `gap-architecture-v2.md` §1 + `pre-mutation-state-check.md` §1.

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
- **Quarterly:** refresh review per persona (xem §Quarterly Review Cadence dưới)
- **Per major feature:** check impact cross-persona
- **On user complaint:** deep-dive specific persona

---

## Quarterly Review Cadence

**Cadence:** End-of-quarter review at last business week of Q1 (March 26-31), Q2 (June 25-30), Q3 (Sept 26-30), Q4 (Dec 22-31). Calendar-anchored, không phải floating "every 3 months". 4 reviews/year.

**Trigger conditions** (any one fires an off-cycle review on top of the quarterly cadence):
- New regulation/law published affecting education sector (MOE circular, Decree on private education, data-protection update)
- New persona added to `documents/00-brd/personas-catalog.md`
- ≥3 user complaints / support tickets clustering on the same persona's workflow within 30 days
- Pricing model change affecting tier coverage
- Major feature launched with cross-persona impact (e.g., parent portal, payroll engine)

**Output (mandatory artifacts):**
- Review report saved to `documents/00-brd/persona-reviews/YYYY-QN-persona-review.md` (one file per quarterly cycle, follow-up off-cycle reviews use `YYYY-MM-DD-trigger-<reason>.md`)
- Gap files filed via `.claude/rules/audit-to-gap-pipeline.md` Step 3 for any new findings (NOT inline fixes)
- ROADMAP entry per `audit-to-gap-pipeline.md` Step 5

**Reviewer:**
- **Primary:** Product Manager (drives walkthrough, owns report)
- **Co-reviewer:** Business Lead (validates persona scale + market assumptions)
- **Optional:** Domain expert (e.g., school admin for K-12 persona, accountant for payroll persona)
- Solo-dev mode (current 2026-04-29): solo-dev role-plays both, but MUST capture sign-off in report frontmatter (`reviewer: solo-dev` + rationale)

**Tracking (next-review date):**
- `documents/00-brd/personas-catalog.md` frontmatter MUST contain `next_review: YYYY-MM-DD` field — set to next quarter-end at completion of each review cycle
- `/repo-status` skill should flag if `next_review` is past today's date (overdue)
- Quarterly cron sanity-check: `find documents/00-brd/persona-reviews -name "*.md" -newer ...` to detect skipped quarters

**Output → gap pipeline (mandatory):**
Per `.claude/rules/audit-to-gap-pipeline.md` — DO NOT fix issues inline during review. Each finding becomes:
1. Gap file via Step 3 template (with state-check per Step 2.5)
2. Memory entry only if pattern repeats (per Step 4)
3. ROADMAP entry per Step 5

**First review:** GAP-152 ships first 4 Tier 1 reports (Solo Teacher, Tutoring Center, Medium Center, K-12 School). After GAP-152 closes, the cadence above becomes the standing process.

## Skill Contents

- This SKILL.md — methodology
- Reference: `documents/00-brd/personas-catalog.md` (canonical list — 10 personas, Tier 1/2/3 classification)
- Reference: `documents/00-brd/persona-criteria/_TEMPLATE.md` (reusable AC template — 6 categories: onboarding/ops/fin/comm/edge/exit)
- Reference: `documents/00-brd/persona-criteria/P<N>-*.md` (per-persona AC docs — 15-30 measurable ACs each, PASS/PARTIAL/FAIL gradable)
- Reference: `documents/00-brd/persona-criteria/README.md` (index + last-reviewed tracking)
- Reference: `documents/00-brd/persona-reviews/` (scored output reports populated by GAP-152 onwards)
- Integration: create gap files via `audit-to-gap-pipeline.md` (NOT inline fixes during review)

## AC docs integration (added v1.2 — GAP-151)

Before GAP-151 (this skill v1.0/1.1): reviewer derived AC ad-hoc from "Key needs" → non-reproducible, score drift, can't compare quarter-over-quarter.

After GAP-151 (this skill v1.2+): reviewer loads formal AC doc → marks each AC PASS/PARTIAL/FAIL with evidence → outputs scored report.

### Updated review flow (replaces Step 2-3 walkthrough for Tier-1 personas)

1. **Load AC doc** for target persona — `documents/00-brd/persona-criteria/P<N>-<slug>.md`
2. **Read §0 Context** — calibrate scale assumption + organization archetype
3. **Role-play with that scale** — walk through each AC's Test scenario
4. **Mark each AC** PASS / PARTIAL / FAIL with concrete evidence:
   - **PASS** = system handles scenario without manual workaround
   - **PARTIAL** = works but with friction, edge case missing, manual step required
   - **FAIL** = missing entirely, no system support, blocks persona
5. **Calculate Coverage %** = (PASS + 0.5 × PARTIAL) / total × 100
6. **For each FAIL AC** — check §Gap Linkage Summary for existing GAP-XXX; if not present, file new gap via `audit-to-gap-pipeline.md` Step 2.5 state-check first
7. **Output scored report** to `documents/00-brd/persona-reviews/YYYY-QN-persona-review.md`

### Coverage verdict thresholds (from `_TEMPLATE.md` §Scoring)

| Coverage % | Verdict |
|------------|---------|
| ≥85% | ✅ Persona fully supported (production-ready) |
| 60-84% | ⚠️ Persona partially supported (defer GA for this persona) |
| 30-59% | 🔴 Persona NOT supported (major gaps) |
| <30% | ❌ Persona NOT viable (consider deferring to Tier 2/3) |

### When to use ad-hoc walkthrough (Step 2-3) vs AC docs

- **AC docs available (Tier 1: P1/P2/P3/P5):** load AC doc, follow flow above
- **AC doc NOT available (Tier 2/3, secondary personas):** fall back to Step 2-3 ad-hoc walkthrough; if pattern recurs, file follow-up gap to add AC doc per `_TEMPLATE.md`
- **Off-cycle deep-dive triggered by user complaint:** AC doc as starting point, extend with complaint-specific scenarios in review report (don't modify AC doc mid-review)

---

## Gotchas

- **Realistic scale matters** — K-12 School persona = 500 students × 30 teachers × 10 staff, not 10×2×1. Small numbers hide bottlenecks (bulk import, attendance grid rendering, report card batch). Use the canonical scales in `documents/00-brd/personas-catalog.md`
- **Don't review against all 10 personas in one sitting** — context fatigue → shallow walkthrough → missed gaps. Tier 1 personas first (Solo Teacher, Tutoring Center, K-12 School), defer Tier 2 to next session
- **Secondary personas often reveal more gaps than primary** — Admin/Director walks through onboarding once; Teacher does daily attendance → finds keyboard shortcut gaps, mobile gaps, search gaps that primary misses
- **Compliance gaps ≠ feature gaps** — VN K-12 needs MOE bảng điểm format (GAP-055), legal-required parent contact for minors, hạnh kiểm grade — these block GA, not "nice-to-have" missing features
- **Output → file gaps via `audit-to-gap-pipeline.md`** — do NOT fix issues mid-review; persona walkthrough produces a list, then convert to gaps in a separate step. Fixing inline corrupts the persona's perspective
- **Quarterly cadence is calendar-anchored, NOT floating** — "every 3 months from last review" drifts; end-of-quarter dates (Q1=Mar 26-31, Q2=Jun 25-30, Q3=Sep 26-30, Q4=Dec 22-31) keep cycles aligned with regulation/budget rhythms (see §Quarterly Review Cadence)
- **`next_review` field in personas-catalog.md frontmatter is the source of truth** — `/repo-status` flags overdue. Don't track quarterly review in scattered docs/calendars

---

## Log

- **2026-04-30** (v1.2): Integrated formal AC framework per GAP-151. Added §"AC docs integration" — replaces ad-hoc "Key needs" walkthrough với load-from-`documents/00-brd/persona-criteria/P<N>-*.md` flow for Tier-1 personas. Bumped purpose statement + Skill Contents section. Coverage verdict thresholds standardized (85/60/30 cutoffs). Closes GAP-151 framework AC #5 ("persona-based-business-review.md skill updated to consume AC docs"). Reviewer: @nguyenvankiet (solo-dev — paired với `documents/00-brd/persona-criteria/_TEMPLATE.md` + 4 Tier-1 AC docs (P1/P2/P3/P5) in same wave per `rule-change-process.md` §6.5 Enforcement Parity Mandate).
- **2026-04-29** (v1.1): Added §Quarterly Review Cadence (calendar-anchored EOQ dates, off-cycle triggers, reviewer roles, output artifacts, `next_review` tracking field). Closes GAP-050 framework AC #4 ("Quarterly review cadence documented"). Reviewer: @nguyenvankiet (solo-dev — paired with `pre-flight-check.md` Layer 4 + `quality-audit/SKILL.md` Cat 11 in same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate).
- **2026-04-14** (v1.0): Skill created. Closes GAP-050 framework AC #2.
