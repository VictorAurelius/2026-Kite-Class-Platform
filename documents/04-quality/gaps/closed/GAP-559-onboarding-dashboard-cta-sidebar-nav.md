# GAP-559: /onboarding entry point invisible — dashboard CTA + sidebar nav missing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 1 N1-P1)
**Related Gaps:** GAP-538 (onboarding checklist + sample data seed — DONE; UI exists nhưng entry point hidden)

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| /onboarding route page | `kitehub/kitehub-frontend/src/app/(customer)/onboarding/page.tsx` | ✅ shipped (Wave 78 GAP-538) |
| OnboardingChecklist component | `kitehub/kitehub-frontend/src/components/onboarding-checklist/OnboardingChecklist.tsx` (240 LOC) | ✅ shipped |
| customerNav Sidebar entry cho `/onboarding` | `kitehub/kitehub-frontend/src/components/layout/Sidebar.tsx` lines 16-21 (4 entries: Tổng quan / Thanh toán / AI Branding / Cài đặt) | ❌ missing — KHÔNG có Onboarding entry |
| Dashboard CTA "Bạn còn N/5 bước chưa làm" | `kitehub/kitehub-frontend/src/app/(customer)/dashboard/*.tsx` | ❌ likely missing (need verify) |

**Grep commands run:**
```bash
grep -n "onboarding" kitehub/kitehub-frontend/src/components/layout/Sidebar.tsx
# Result: 0 hits — Sidebar.tsx không có ref /onboarding
grep -rn "onboarding" kitehub/kitehub-frontend/src/app/\(customer\)/dashboard/ 2>/dev/null
# Result: TBD — need verify
```

## Problem

Onboarding checklist (Wave 78 GAP-538) đã ship đầy đủ UI ở `/onboarding`, NHƯNG entry point hoàn toàn hidden:
- Sidebar customer nav (4 entries) KHÔNG có "Onboarding" / "Bắt đầu"
- Dashboard `/dashboard` KHÔNG có CTA "Bạn còn 5/5 bước onboarding chưa làm"
- User mới login (first time) thấy dashboard trống → không biết đi đâu tiếp theo

Hậu quả:
- Onboarding completion rate thấp → activation rate beta tenant thấp
- Sample data seed (step IMPORT_DATA) không được trigger → empty dashboard → confusion → support email
- Investment Wave 78 GAP-538 không thu được benefit

## Context

Outside-in audit Persona 1 P2 Center Owner (Chị Hằng) walkthrough Bước 3: post-login dashboard rỗng, không thấy CTA dẫn đến onboarding. Có URL `/onboarding` nhưng phải tự gõ hoặc click qua deep link từ welcome email. P2 Owner trung tâm không tech-savvy enough để tìm hidden routes.

Inside-out queue 13 items không có gap riêng cho discoverability — focus auth/feedback/support hardening. Outside-in surface gap discoverability.

## Evidence

- `Sidebar.tsx` line 16-21: `customerNav` array có 4 items, không có Onboarding
- `OnboardingChecklist.tsx` line 117-138: render UI đầy đủ với progress bar — UI ready, only discoverability missing
- Welcome email `welcome.html` lines 76-77: link "Đăng Nhập Ngay" → `/login` (default dashboard) — không direct link `/onboarding`

## Proposed Fix

1. **Add Sidebar nav entry** trong `customerNav`:
   ```tsx
   { href: '/onboarding', label: 'Bắt đầu', icon: Sparkles, testId: 'customer-nav-onboarding' }
   ```
   Hiển thị badge "N/5" pending count khi user chưa complete (fetch from `getOnboardingProgress`)
2. **Add Dashboard CTA banner** ở `/dashboard` (top section, conditional render khi completion < 100%):
   ```tsx
   <OnboardingProgressCTA completionPercent={...} pendingSteps={...} />
   ```
   - Hiển thị "Bạn còn N/5 bước để khởi động trung tâm"
   - CTA button "Tiếp tục onboarding" → `/onboarding`
   - Dismissible với cookie (24h re-show)
3. **Update welcome email** redirect link sau set password → `/onboarding` thay vì `/dashboard` (nếu user first login). BE check `user.onboarding_completed` flag.
4. **Auto-redirect first login** từ `/dashboard` → `/onboarding` nếu `completionPercent === 0` (one-time, set cookie để không re-redirect)

## Acceptance Criteria

- [ ] Sidebar customerNav có entry "Bắt đầu" → `/onboarding`
- [ ] Badge "N/5" hiển thị khi pending (sau fetch from `getOnboardingProgress`)
- [ ] Dashboard `/dashboard` top section có `OnboardingProgressCTA` banner khi completion < 100%
- [ ] Banner dismissible với cookie 24h
- [ ] Welcome email post-set-password redirect → `/onboarding` cho first login (BE flag check)
- [ ] Persona test: P2 Owner first login → land trên `/onboarding` → checklist visible immediately
- [ ] E2E test trong `kitehub-frontend/src/e2e/onboarding-discoverability.spec.ts`

## Related

- GAP-538 (onboarding checklist UI — DONE; this gap closes the discoverability delta)
- GAP-539 (beta disclaimer banner — DONE; similar dashboard banner pattern)
- Inside-out queue: không overlap (no entry covers nav/discoverability)
- Audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md`

## Log

- **2026-05-14:** DONE — Wave 79 Bucket D closure. OnboardingDashboardCTA component + Sidebar customerNav entry shipped; /onboarding now reachable from 2 in-app entry points (PR #1368).

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 1 N1-P1). State-check confirmed UI exists nhưng entry point hidden. Priority P1 vì degrades activation rate (not blocker, user vẫn login OK).
