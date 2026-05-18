# GAP-623: Beta cohort execution plan (2 GV trial + 2 GV VIP — concrete timeline + invite flow + feedback collection)

**Status:** 🔵 OPEN
**Priority:** 🟧 P1
**Domain:** Meta
**Found:** 2026-05-18 (Wave 92 closure meta-improvements audit per `wave-closure-scope-completeness.md` §3)
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

### Phase 1: Draft `documents/03-planning/plans/beta-cohort-execution-plan-2026.md`

Plan doc với 8 sections:

1. **Cohort composition** — 2 GV solo trial + 2 GV trial business + 2 VIP business (user-confirmed count needed)
2. **Recruitment channels** — Zalo OA / personal network / KitClass beta-access-request flow
3. **Timeline** — D0 invite, D+7 first feedback, D+14 mid-trial check, D+30 final feedback + signed
4. **Invite flow** — automated via existing invite-staff (Wave 79 GAP-561b) OR manual email batch
5. **Feedback collection** — Google Forms + bản nhận xét template + Zalo OA reply intake
6. **Success criteria** — ≥4 written signed feedback + ≥1 tenant 30-day retention + 0 P0 incident report
7. **Failure recovery** — 1 abort → backup cohort list pre-prepared; complete abandonment → file gap + retro
8. **Dependencies + blockers** — cross-link GAP-612 (AWS) + GAP-369/370 (email infra) + GAP-353 PDPL gates

### Phase 2: Outside-in audit (re-use thesis T1/T2/T3 findings)

Thesis outside-in audits already cover persona / benchmark / failure-mode dimensions relevant cho beta cohort scope. Reference cross-link, không spawn new audit.

### Phase 3: Pre-invite checklist runbook

Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow + §2.5 file-upload (bản ký tay scan) + §2.6 payment (nếu VIP có trial→paid flow). Pre-invite runbook ensure deploy + emails + UX verified.

## Acceptance Criteria

- [ ] Plan doc shipped với 8 sections per Phase 1
- [ ] Cohort composition user-confirmed (4 hay 6 personas, trial vs VIP split)
- [ ] Timeline D-day backward planning từ thesis defense deadline (target?)
- [ ] Invite flow chosen (auto via GAP-561b vs manual batch)
- [ ] Feedback template ship (`documents/05-guides/user-manual/feedback-template/bản-nhận-xét-template.docx`)
- [ ] Dependencies cross-linked (GAP-612 + GAP-369/370 + GAP-353 cluster + thesis Chương 4 plan)
- [ ] Status flip DONE only sau cohort recruited + ≥1 trial active

## Related

- User direction `documents/action-2.md` 2026-05-18 thesis brief — "2 giáo viên đơn lẻ + 2 business trial/vip"
- Thesis audit T3 — `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md` §Top-5 P0 #1
- Thesis audit T1 — `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-persona-simulation.md` §Persona advisor concerns
- GAP-612 — AWS suspension (precondition deploy)
- GAP-369/370 — DNS + email transactional
- GAP-353 cluster — PDPL implementation
- GAP-561b — invite-staff flow (potential auto invite mechanism)
- Wave 79 Bucket F1 — anonymous-prospect user manual (recruitment landing page)
- Rule: `outside-in-coverage-trigger.md` (audit findings re-use)
- Rule: `wave-closure-scope-completeness.md` (parallel scope discipline)

## Log

- **2026-05-18 (filed):** Filed by Wave 92 closure meta-improvements audit. Top 3 improvement areas surfaced 2026-05-18 session: beta cohort execution plan = #4 priority overall, P1 cho thesis evidence dependency. Per user 2026-05-18 decision "File 3 gap files TOP 3 + defer execution" — execution defer Wave 94+ post-release-2-plan-lock. Beta cohort blocks both Phase 1 BETA gate proof + thesis Chương 4 evidence (≥4 bản ký tay per T3 audit P0).
