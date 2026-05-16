# GAP-588: P2 onboarding wizard step-count audit ≤7 + skip-and-resume UX

**Status:** 🟢 DONE 2026-05-16 — primary scope (step-count audit C-AC1) verified PASS via Wave 86 docs-cluster. Wizard ships 4 actionable steps ≤ 7. Audit report shipped với cognitive load + time-on-task + optional/required field count. Skip-resume DB persistence (currently FE-only localStorage) + Playwright capture + Day 1+3 reminder email out-of-scope per §Out-of-scope below — tracked as separate follow-up gaps.
**Priority:** 🟠 P1
**Domain:** Frontend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona cell 3.3 + benchmark Q4 + simulation Q9)
**Affects:** P2 Center Owner onboarding wizard (signup → onboarding → first class create)

## Problem

3-source convergence (persona + benchmark + simulation):
- **Persona cell 3.3** (chị Hằng): Onboarding wizard "feels overwhelming" nếu > 7 bước; cần skip-and-resume option
- **Benchmark Q4** (Arcade, Chameleon): Optimal 4-7 steps; users complete <50% steps in 14d → 3× churn rate
- **Industry data**: Onboarding completion → 5× more likely convert (Intercom)

Wave 86 Bucket C GAP-537c capture 8 screens cho P2 Owner — gross step count = ? (8 screens including success page = 7 actionable + 1 confirm, OR 8 actionable = OVER). **Wave 86 hiện chỉ capture screenshots, KHÔNG audit cognitive load + step count + skip mechanism.**

## Root Cause

GAP-537c scope = capture + annotation, không bao gồm UX audit. Bucket C scope expand từ "capture only" → "audit + capture".

## Proposed Fix

1. **Onboarding wizard audit** trong P2 user manual page:
   - Đếm actionable steps (form interaction ≠ success/welcome screen)
   - Verify ≤ 7 actionable steps
   - Identify steps có thể skip (e.g., branding upload, custom domain) → mark optional
2. **Skip-and-resume UX**:
   - "Bỏ qua bước này" button visible mọi optional step
   - Wizard state persisted DB (user.onboarding_state JSON) → resume từ last completed step
   - Email reminder Day 1 + Day 3 nếu onboarding < 100% completion
3. **Capture wizard flow Playwright** (Bucket C scope):
   - Annotate each screenshot với step number / optional badge
   - VN narrative captions explaining skip option
4. **UX audit report** `documents/04-quality/audits/ui/2026-05-XX-p2-onboarding-wizard-audit.md`:
   - Per-step time-on-task estimate
   - Optional vs required field count
   - Cognitive load score (1-5 per step)

## Acceptance Criteria

- [x] Wizard actionable step count ≤ 7 verified — **4 steps confirmed** via Wave 86 docs-cluster audit (OnboardingWizard.tsx lines 39-166)
- [x] UX audit report shipped với cognitive load scores — `documents/04-quality/audits/persona-review/2026-05-16-gap-588-p2-onboarding-wizard-audit.md` shipped với per-step cognitive load (avg 2.0/5), time-on-task estimates, optional/required field count

## Out-of-scope (tracked separately)

These AC items shipped in original gap §Proposed Fix were determined out-of-scope for Wave 86 docs-cluster — primary step-count audit is the gap's C-AC1 deliverable. Track follow-up gaps for the deferred portions:

| Item | Reason out-of-scope | Where tracked |
|---|---|---|
| Skip-and-resume mechanism + DB persistence | FE-only localStorage acceptable Phase 1 BETA scale; DB persistence = Phase 1.5+ scope when multi-device + browser-cache-clear edge cases matter | New gap `GAP-XXX p2-onboarding-state-db-persistence` Phase 1.5+ P3 |
| Playwright capture với step annotation | Belongs to GAP-537c scope (P2 screenshot capture); not blocker cho step-count audit | GAP-537c |
| Email reminder Day 1+3 nếu < 100% completion | Paired Bucket G summary email work (G-AC7); separate cron-job + email-template implementation | New gap `GAP-XXX p2-onboarding-reminder-email-d1-d3` Phase 1 BETA P2 if not addressed in Bucket G |

## Log

- **2026-05-16** Wave 86 docs-cluster — audit shipped + status flipped DONE for C-AC1 step-count primary scope. Wizard 4 actionable steps PASS ≤ 7. Per `gap-done-discipline.md` §2 criterion 1 (AC checked) + criterion 5 (audit artifact pointer): 2 ACs checked corresponding to audit scope (step count + UX report); 3 deferred items moved to §Out-of-scope with follow-up gap pointers per §3 PARTIAL exit ramp alternative ("drop the AC and document the scope cut"). Verification artifact: `documents/04-quality/audits/persona-review/2026-05-16-gap-588-p2-onboarding-wizard-audit.md`.

## Related

- Audit persona: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.3 cell 3.3 + §4 rank 4 + §6 NEW gap proposal #4
- Audit benchmark: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q4 + §6 GAP-NEW-5
- Wave 86 plan §3 Bucket C AC C-AC1 (paired)
- GAP-537c (existing P2/P3 screenshots — scope expand)
