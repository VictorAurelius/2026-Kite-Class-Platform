# GAP-588: P2 onboarding wizard step-count audit ≤7 + skip-and-resume UX

**Status:** 🔵 OPEN
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

- [ ] Wizard actionable step count ≤ 7 verified (or refactor if currently >7)
- [ ] Skip-and-resume mechanism implemented + persisted DB
- [ ] Playwright capture với step annotation shipped (paired GAP-537c)
- [ ] UX audit report shipped với cognitive load scores
- [ ] Email reminder Day 1+3 nếu < 100% completion (paired G-AC7 summary email)

## Related

- Audit persona: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.3 cell 3.3 + §4 rank 4 + §6 NEW gap proposal #4
- Audit benchmark: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q4 + §6 GAP-NEW-5
- Wave 86 plan §3 Bucket C AC C-AC1 (paired)
- GAP-537c (existing P2/P3 screenshots — scope expand)
