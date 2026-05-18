# GAP-287: Skip / Use Default option in branding wizard

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks P1 Solo Teacher onboarding (AC-ONBOARD-002 FAIL)
**Domain:** Frontend (branding wizard) + Backend (theme defaults)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (FREE tier, không cần custom branding); P2 Small Center initial signup

## Problem

Theo AC-ONBOARD-002, solo teacher PHẢI có khả năng skip phần "Upload logo" và "Choose template" trong wizard branding (vì solo teacher không cần custom branding) — system tự dùng theme mặc định và vào dashboard ngay.

Hiện trạng: branding wizard force flow hết 6 steps trước khi user vào được dashboard. AI branding pipeline (10+ phút) là blocker cho solo teacher persona muốn có thể dùng app trong ≤10 phút.

**State-check (verified 2026-05-04):**
- `kiteclass-frontend/src/components/branding/wizard/BrandingWizard.tsx` exists
- `kiteclass-frontend/src/components/branding/wizard/wizard-machine.ts` (XState) chứa state machine flow
- Grep `Skip|skip` ở wizard files = 0 hits — KHÔNG có skip button/transition
- `kiteclass-frontend/src/app/(dashboard)/branding/wizard/page.tsx` hard-code `tier="PRO"` — chưa có tier-aware skip logic

## Root Cause

Wizard thiết kế theo `ai-branding-guidelines.md` §4.1 6-step (welcome → logo → audience → tone → template → preview) cho enterprise/center personas. Solo teacher không cần custom branding nhưng wizard chưa có branch "skip + use default theme" path.

## Proposed Fix

1. **wizard-machine.ts:** thêm state transition `skip` từ mỗi step → `done` state với theme = system default
2. **BrandingWizard.tsx:** thêm "Skip — dùng giao diện mặc định" button ở mỗi step (after step 1 welcome)
3. **Backend:** ensure default theme exists trong `branding` table cho new tenant (V40 migration đã có `if_not_exists` clause)
4. **Tier-aware default:** FREE tier auto-skip wizard và route thẳng vào dashboard (banner CTA "Tùy chỉnh giao diện sau" trong dashboard)
5. **Telemetry:** track skip rate per tier để inform AI branding ROI

## Acceptance Criteria

- [ ] "Skip" button visible ở step 2-5 của wizard
- [ ] FREE tier auto-skip → vào dashboard ngay (KHÔNG hiện wizard)
- [ ] PRO tier hiển thị wizard nhưng có skip option
- [ ] Skip → dashboard với theme mặc định (KHÔNG broken layout)
- [ ] User có thể quay lại `/branding/wizard` từ Settings → re-run wizard
- [ ] E2E test: skip flow on FREE tier ≤2 clicks từ login → dashboard
- [ ] No regression cho ENTERPRISE Advanced Mode flow

## Related

- AC-ONBOARD-002 (P1 review 2026-05-04)
- GAP-013 (Guided branding wizard UX) — orthogonal, this gap fixes solo persona escape ramp
- `.claude/rules/ai-branding-guidelines.md` §4.1 (wizard 6-step)
- GAP-286 (Mobile OTP signup) — paired onboarding flow fix

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1. State-check confirmed no skip option in wizard.
