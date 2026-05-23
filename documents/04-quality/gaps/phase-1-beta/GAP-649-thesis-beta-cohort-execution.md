# GAP-649: Thesis beta cohort execution — ≥4 signed user reviews

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed (Product + Business)
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** All 3 outside-in audits converge

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Beta invite mechanism | ✅ DONE (GAP-372 — Beta Tenant Invite Mechanism) |
| Live beta tenants | ❌ 0 currently (per CSV no beta_tenant_count metric) |
| Signed beta-user feedback form | ❌ no template |
| Beta cohort communication channel | ⚠️ partial (email shipped) |
| Beta walkthrough session script | ❌ missing |
| Beta feedback storage | ❌ no `documents/08-thesis/beta-feedback/` folder |

## Problem

Persona audit + VN benchmark §3 Q4 + Failure-mode aggregate P0 #3 all converge: **5 beta tenants live + ≥4 signed reviews phân biệt thesis 8 điểm vs 9-10 điểm**. Đây là "real users validate market fit" narrative đầu Chapter 4 mà Failure-mode Move 3 highlighted.

Current Phase 1 BETA gate criteria per CLAUDE.md: "5 beta tenants live + 0 P0 incidents 2 tuần". Beta invite shipped (GAP-372 DONE) nhưng EXECUTION (mời + onboard + collect signed feedback) chưa start.

## Proposed Fix

### Step 1: Beta cohort target list

Define 5-7 candidate tenants:
- ≥2 GV freelance (P1 Solo Teacher persona) — IELTS / TOEIC / kid English
- ≥2 GV chủ trung tâm nhỏ (P2 Center Owner persona) — 1 trung tâm 50-100 HS
- 1-2 backup (rejection buffer)

Sources: personal network HUST/UET alumni, contact qua Facebook group "GV ngoại ngữ VN", LinkedIn outreach.

### Step 2: Beta walkthrough script

Create `documents/08-thesis/beta-feedback/walkthrough-script.md`:
- 30-min session script: signup → onboarding → core feature demo → free exploration → feedback
- Screen-share via Zoom/Google Meet
- Record với consent

### Step 3: Signed feedback form

Create `documents/08-thesis/beta-feedback/feedback-form-template.md`:
- Tên đầy đủ + tên trung tâm + ngày ký
- 5-7 rating questions (1-5 scale): ease of use, feature completeness, design polish, would recommend, would pay
- Open-text: top 3 pain points + top 3 nice features + suggestion
- Signature line + date

Convert to PDF cho signing. Store signed PDFs `documents/08-thesis/beta-feedback/signed/`.

### Step 4: Onboarding support runbook

`documents/05-guides/operations/beta-onboarding-runbook.md`:
- Support channel: email + Zalo OA
- SLA: respond within 4h business hours
- Track issues per tenant trong support log

### Step 5: Cohort execution timeline

Week 1: Invite 7 candidates, secure ≥5 commits
Week 2-3: Onboard sequentially (1 tenant/2-day cycle)
Week 4-6: Active usage + bi-weekly check-in
Week 7-8: Collect signed feedback + retrospective interview
Week 9: Aggregate findings → thesis Chapter 4

Total: ~9 weeks. Start now (2026-05-18) → complete ~2026-07-20 → fits defense window 2026-08-15+.

### Step 6: Anonymization + ethics

Per PDPL 2023 + research ethics:
- Beta users sign consent for thesis citation
- Anonymize sensitive data trong thesis (tên trung tâm có thể đề + ngày, không đề doanh thu cụ thể)
- Option opt-out anytime

## Acceptance Criteria

- [ ] 5-7 candidate target list documented
- [ ] Walkthrough script + recording consent template ready
- [ ] Feedback form template + signed PDF version ready
- [ ] Onboarding support runbook published
- [ ] ≥4 signed beta reviews collected by 2026-07-20
- [ ] Aggregate findings doc `documents/08-thesis/beta-feedback/aggregate-findings.md` ready cho thesis Chapter 4 cite
- [ ] PDPL consent + anonymization protocol verified

## Related

- GAP-372 (DONE) — Beta Tenant Invite Mechanism (mechanism shipped, execution this gap)
- GAP-538 (PARTIAL 85%) — Day-1 onboarding checklist + sample data (improves onboarding UX cho cohort)
- GAP-540 (PARTIAL 80%) — Beta support channel discoverability
- GAP-542 (PARTIAL 80%) — Feedback channel widget + email survey
- GAP-541 (PARTIAL 60%) — Vietnamese i18n audit (UX polish trước invite)
- `documents/08-thesis/chapter-mapping.md` — Chapter 4 sources include beta feedback
- Failure-mode audit Move 3 — "Beta validates market fit" narrative

## Log

- **2026-05-18 (created):** Filed per outside-in audit all 3 agents convergence. EXECUTION gap (not infrastructure) — mechanism shipped GAP-372 DONE, this gap = actual cohort execution + signed feedback collection.
- **2026-05-23:** DEFER Wave thesis-2 — ≥4 nhận xét người dùng ký tay phân biệt 8đ vs 9-10đ cần beta tenant thật + 9 tuần timeline. Wave thesis-1 (`documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md`) ship `release-1-beta-cohort-plan.md` doc-only via Bucket E (plan + persona + timeline + invite flow + signed review template); execution defer. Trigger restart: GAP-612 DONE + invite email gửi.
