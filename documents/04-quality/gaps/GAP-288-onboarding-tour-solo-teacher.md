# GAP-288: First-login onboarding tour for solo teacher (5-feature highlight)

**Status:** 🔵 OPEN
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

- [ ] Tour fires CHỈ on first login per persona type
- [ ] ≤5 steps cho solo persona
- [ ] Skip button visible mọi step
- [ ] Tour KHÔNG show K-12 features (academic year, semester) cho solo persona — gated by tier/role
- [ ] Replay-from-settings works
- [ ] Mobile-friendly (tooltip không out-of-viewport)
- [ ] A11y: keyboard nav + ARIA tour role
- [ ] Telemetry: track tour completion rate + skip rate

## Related

- AC-ONBOARD-004 (P1 review 2026-05-04)
- GAP-053 (Academic year/semester gating — related, tour must hide K-12 features cho solo)
- GAP-286 + GAP-287 (paired onboarding fixes)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
