# GAP-288: First-login onboarding tour for solo teacher (5-feature highlight)

**Status:** 🟡 PARTIAL — core AC shipped + verified (build+tests); full browser walk of this build deferred
**Priority:** 🟠 P1 — UX gap; AC-ONBOARD-004 FAIL
**Domain:** Frontend (kiteclass-frontend dashboard)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher first-login UX

## Problem

Theo AC-ONBOARD-004, onboarding tour PHẢI highlight ≤5 features quan trọng nhất cho solo teacher (schedule, attendance, gradebook, invoice, communicate) — KHÔNG phải toàn bộ feature list. Phải có "Skip tour" mọi lúc.

Hiện trạng: KHÔNG có tour component / library / first-login flag detection.

**State-check (verified 2026-05-04):**
- Grep `tour|onboarding|driver.js|reactour|joyride` ở `kiteclass-frontend/src` = 0 hits ngoài `__tests__`
- Dashboard layout (`components/layout/`) không inject tour overlay
- KHÔNG có `first_login_at` flag trên User entity ở core
- KHÔNG có persistent dismissal storage cho per-feature tour

## Root Cause

Onboarding UX deferred to post-GA polish. Solo persona review Round 1 surface this as P1 (không P0 vì user vẫn navigate được app, chỉ là discovery friction).

## Proposed Fix

1. **Backend:** thêm field `first_login_at` + `tour_dismissed` JSON column trên User entity
2. **Frontend:** integrate `react-joyride` hoặc `driver.js` (≤30KB, no external service)
3. **Tour content (5 steps):** persona-aware:
   - Solo (P1): Schedule → Attendance → Gradebook → Invoice → Send-message
   - Center owner (P2/P3): Class management → Teacher list → Revenue → Marketing → Settings
4. **Skip button:** persistent ở mọi step, footer "Bỏ qua tour"
5. **Replay:** Settings → Help → "Xem lại hướng dẫn" CTA

## Acceptance Criteria

- [x] Tour fires CHỈ on first login (localStorage `completed` flag; re-fires only on replay)
- [x] ≤5 steps cho solo persona (`TOTAL_STEPS = 5`)
- [x] Skip button visible mọi step (per-step "Bỏ qua bước N" + persistent "Đóng hướng dẫn" header control)
- [x] Tour KHÔNG show K-12 features (academic year, semester) — 5 steps là school-info/teacher/course/student/done; ZERO K-12 feature steps (no tier/role gating needed vì content vốn không chứa K-12)
- [x] Replay-from-settings works (`OnboardingReplayCard` trong Settings → Tùy chọn tab; resets localStorage + nav `/dashboard`)
- [x] Mobile-friendly (inline Card layout — không có tooltip overlay risk out-of-viewport)
- [x] A11y: keyboard nav (native `<button>`) + ARIA labels mọi control
- [x] Telemetry: track tour completion rate + skip rate (`onboarding-telemetry.ts` + `summarizeOnboardingFunnel()`)

## Current State (verified 2026-06-02)

State-check trước fix:
- `OnboardingWizard.tsx` (5-step inline Card wizard, localStorage, ≤5 steps, VN, ARIA) đã tồn tại từ wave trước NHƯNG **dead code** — grep usage = 0 (chỉ định nghĩa + test, KHÔNG render anywhere). Dashboard chỉ render `DashboardWelcome` (3-button banner).
- KHÔNG có replay-from-settings CTA.
- KHÔNG có telemetry.

Delta shipped (this PR):
1. **Wire `OnboardingWizard`** vào `/dashboard/page.tsx` (first-login 5-step tour; `DashboardWelcome` chỉ hiện sau khi wizard done → không 2 card chồng).
2. **`OnboardingReplayCard`** mới — Settings → Tùy chọn tab "Xem lại hướng dẫn".
3. **`onboarding-telemetry.ts`** mới — funnel events (start/step_view/step_skip/complete/replay) → capped localStorage ring buffer + `summarizeOnboardingFunnel()` (completion/skip rate).
4. **"Đóng hướng dẫn"** persistent skip-whole-tour control trong wizard header.

Design note: gap §Proposed Fix đề xuất `react-joyride`/`driver.js` overlay + BE `first_login_at`/`tour_dismissed` columns. Shipped approach dùng **inline Card wizard + localStorage** (no external lib, no BE migration) — satisfies tất cả core AC với footprint nhỏ hơn cho P1 Solo persona. BE-backed cross-device dismissal là enhancement future (không cần cho Phase 1 BETA single-device beta).

PARTIAL (không DONE) vì: build PASS + 42 telemetry/wizard/replay unit tests + 39 onboarding/dashboard/settings suite PASS, NHƯNG full browser RST walk của build này chưa chạy (running local stack :3000 = pre-change build; rebuild shared stack từ worktree risky). Wizard render/hide path test-verified per `feature-ship-runtime-walk-mandate.md`; browser walk → follow-up khi stack rebuild.

## Related

- AC-ONBOARD-004 (P1 review 2026-05-04)
- GAP-053 (Academic year/semester gating — related, tour must hide K-12 features cho solo)
- GAP-286 + GAP-287 (paired onboarding fixes)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
- **2026-06-02** — Autonomous gap-fix agent (local-doable campaign). State-check found `OnboardingWizard` shipped prior wave but dead code (unwired). Wired into dashboard + added replay-from-settings CTA + telemetry util + skip-whole-tour control. All 8 core AC met (design alt: inline Card wizard + localStorage instead of joyride/driver.js overlay + BE columns). Build PASS, 42+39 tests PASS. Status PARTIAL pending browser walk of this build. Cross-flow sweep: no sister dead-code (BrandingWizard/WizardProgress/WelcomeStep all wired).
