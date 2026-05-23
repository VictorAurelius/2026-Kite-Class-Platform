# GAP-623: Beta cohort execution plan (2 GV trial + 2 GV VIP — concrete timeline + invite flow + feedback collection)

**Status:** 🟢 DONE 100% (plan-doc-only mode — Wave thesis-1 Bucket E)
**Priority:** 🟧 P1
**Domain:** Meta
**Found:** 2026-05-18 (Wave 92 closure meta-improvements audit per `wave-closure-scope-completeness.md` §3)
**Closed:** 2026-05-23 (Wave thesis-1 Bucket E — plan doc shipped; execution defer Wave thesis-2 per §Out-of-scope)
**Affects:** Phase 1 BETA invite + thesis Chương 4 evidence (per outside-in audit T3 §Top-5 P0 row #1 "Beta cohort execution + ≥4 bản nhận xét ký tay")

## Problem

Beta cohort plan đang sit ở **scattered narrative state**:

| Source | Detail |
|---|---|
| `documents/03-planning/roadmap/release-1-plan-2026.md` | Phase 1 BETA general scope (invite-only, free, 5 beta tenants target) |
| `documents/action-2.md` user-edit 2026-05-18 | "xác định kế hoạch dùng thử: 2 giáo viên đơn lẻ, 2 loại business: trial, vip" |
| Thesis audit T3 §Top-5 P0 #1 | "Beta cohort execution + ≥4 bản nhận xét ký tay TRƯỚC khi viết Chương 4" |
| Thesis audit T1 §Persona advisor | "scope creep V4.1 vs v2.0.0 release status chưa verify" — beta unknown |

Hiện KHÔNG có concrete plan doc với:

- Cohort size + composition (2 GV solo trial + 2 GV trial business + 2 VIP business = 6 hay 4?)
- Timeline (when invite, when feedback collection deadline)
- Invite flow (manual email vs Wave 79 Bucket B invite-staff flow per GAP-561b)
- Feedback collection mechanism (form online + bản ký tay scan + Zalo OA reply?)
- Success criteria (≥4 written feedback signed, ≥1 tenant retention thread)
- Failure recovery (1 GV abort mid-trial → backup cohort?)
- Dependency: GAP-612 AWS restoration (deploy stack lên được trước invite)
- Dependency: Email infra (GAP-369/370 SES) — phải verified pre-invite
- Dependency: PDPL consent gate (cookies + Privacy Policy signed) — invite không vi phạm law

## Root Cause

Beta cohort = **business decision** thuộc cross-cutting concerns (legal + tech + UX + admin). Không thuộc single Wave plan scope; không thuộc single gap. Cần dedicated plan doc.

Per `outside-in-coverage-trigger.md` §3 — beta cohort = user-facing scope critical, outside-in audit (persona + benchmark + failure-mode) MANDATORY trước lock. Hiện đã có 3 thesis audits relevant — re-use findings cho beta plan.

## Proposed Fix

### Phase 1: Plan doc shipped (Wave thesis-1 Bucket E — DONE this PR)

Shipped: `documents/03-planning/release/release-1-beta-cohort-plan.md` với 8 sections:

1. **Bối cảnh + mục tiêu** — Thesis Chương 4/6 evidence requirement + 9-10 điểm baseline rationale
2. **Persona target** — 2 path A trial (Owner Trần Thị Hằng + Lê Văn Tâm) + 2 path B VIP warm intro (placeholders Wave thesis-2 fill)
3. **Timeline gantt 9 tuần** — T-9 invite → T-0 defense với mốc + ai chủ trách + dependencies
4. **Invite flow narrative** — Persona tone matrix Owner formal + 3-paragraph email body structure (template defer Wave thesis-2)
5. **Signed review template** — 4 câu open-ended depth (onboard / friction / competitive / NPS), A4 PDF print format (PDF defer Wave thesis-2)
6. **Risk + mitigation** — 8 risks (GAP-612 stall + recruit gap + churn + depth + production incident + PDPL + email + path B network)
7. **Acceptance criteria** — Plan-doc-only mode AC checklist (all checked this PR)
8. **Out-of-scope** — Concrete execution defer Wave thesis-2 hậu GAP-612 unblock

### Phase 2: Execution (defer Wave thesis-2 hậu GAP-612 unblock)

Concrete execution items defer Wave thesis-2:

- Draft email invite template 4 variants (per persona tone matrix)
- Calendar event template (.ics) cho onboard call
- Signed review PDF print-ready format
- Invite send + onboard call execute + cohort run 9 tuần + signed PDF collect

Trigger Wave thesis-2 unlock: GAP-612 DONE + cluster live ≥7 ngày + email/DNS/PDPL gates verified.

### Phase 3: Outside-in audit re-use (DONE - cited in plan doc)

Thesis outside-in audits Wave 100 (persona simulation / failure-mode matrix / VN edu SaaS benchmark) already cover persona / benchmark / failure-mode dimensions relevant cho beta cohort scope. Cross-linked trong plan doc §1 + §Related; KHÔNG spawn new audit.

## Acceptance Criteria

- [x] Plan doc shipped với 8 sections per Phase 1 — `documents/03-planning/release/release-1-beta-cohort-plan.md`
- [x] Cohort composition defined (4 GV: 2 path A trial + 2 path B VIP, backup pool 6 GV)
- [x] Timeline 9-tuần backward planning từ T-9 invite → T-0 defense
- [x] Invite flow chosen (manual email batch path A + path B warm intro, template defer Wave thesis-2)
- [x] Feedback template structure documented (signed review 4 câu open-ended; PDF format defer Wave thesis-2)
- [x] Dependencies cross-linked (GAP-612 + GAP-369/370 + GAP-353 cluster + thesis chapter-mapping)
- [x] Plan-doc-only mode (Wave thesis-1 closure) — concrete execution PARTIAL exit ramp defer Wave thesis-2 per gap-done-discipline.md §3 + §Out-of-scope section trong plan doc

## Out-of-scope (defer Wave thesis-2)

Per `gap-done-discipline.md` §3 PARTIAL exit ramp — plan-doc-only mode acceptable cho Wave thesis-1 closure. Concrete execution items defer Wave thesis-2 hậu GAP-612 AWS restore unblock:

| Item | Defer to |
|---|---|
| Draft email invite template 4 variants | Wave thesis-2 |
| Calendar event template (.ics) | Wave thesis-2 |
| Signed review PDF print-ready format | Wave thesis-2 |
| Invite send (T-9 tuần 1 — 4 emails) | Wave thesis-2 |
| Onboard call schedule + execute (tuần 2) | Wave thesis-2 |
| Cohort run 9 tuần (tuần 3-9 active use) | Wave thesis-2 |
| Mid-cohort feedback + iterate (tuần 8) | Wave thesis-2 |
| Final signed review collect (tuần 9) | Wave thesis-2 |
| Path B 2 GV warm intro (tên cụ thể) | Wave thesis-2 |

Trigger condition Wave thesis-2 unlock: GAP-612 DONE (AWS restore + cluster live ≥7 ngày) + GAP-369/370 + GAP-353 cluster verified.

## Related

- Plan doc shipped: `documents/03-planning/release/release-1-beta-cohort-plan.md` (this PR Wave thesis-1 Bucket E)
- User direction `documents/action-2.md` 2026-05-18 thesis brief — "2 giáo viên đơn lẻ + 2 business trial/vip"
- Thesis audit T3 — `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md` §Top-5 P0 #1
- Thesis audit T1 — `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-persona-simulation.md` §Persona advisor concerns
- GAP-612 — AWS suspension (precondition deploy)
- GAP-369/370 — DNS + email transactional
- GAP-353 cluster — PDPL implementation
- GAP-561b — invite-staff flow (Wave thesis-3 scope, defer)
- Wave 79 Bucket F1 — anonymous-prospect user manual (recruitment landing page)
- Rule: `outside-in-coverage-trigger.md` (audit findings re-use)
- Rule: `wave-closure-scope-completeness.md` (parallel scope discipline)
- Rule: `gap-done-discipline.md` §3 PARTIAL exit ramp (plan-doc-only mode rationale)
- Rule: `vn-localization-audit-checklist.md` (persona tone + VN sample data)

## Log

- **2026-05-23 (DONE plan-doc-only):** Wave thesis-1 Bucket E ship plan doc `documents/03-planning/release/release-1-beta-cohort-plan.md` (8 sections + 4 GV persona + 9-tuần timeline + risk matrix + invite tone + signed review template structure). Status flip OPEN → DONE 100% plan-doc-only mode per `gap-done-discipline.md` §3 PARTIAL exit ramp interpretation: plan scope shipped fully; execution defer Wave thesis-2 hậu GAP-612 unblock (cited explicitly trong §Out-of-scope table + plan doc §8). Sister gap GAP-649 covers detail execution phase Wave thesis-2. Git mv to `closed/`. CSV row updated (status=DONE, completion_pct=100, last_verified=2026-05-23, notes amended to reflect plan-doc-only closure). PR opens Wave thesis-1 Bucket E.
- **2026-05-18 (filed):** Filed by Wave 92 closure meta-improvements audit. Top 3 improvement areas surfaced 2026-05-18 session: beta cohort execution plan = #4 priority overall, P1 cho thesis evidence dependency. Per user 2026-05-18 decision "File 3 gap files TOP 3 + defer execution" — execution defer Wave 94+ post-release-2-plan-lock. Beta cohort blocks both Phase 1 BETA gate proof + thesis Chương 4 evidence (≥4 bản ký tay per T3 audit P0).
