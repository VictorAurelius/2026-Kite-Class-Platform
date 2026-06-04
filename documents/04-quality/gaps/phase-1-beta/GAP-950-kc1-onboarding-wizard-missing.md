# GAP-950: Onboarding wizard không tồn tại — non-tech Owner persona blocker

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Onboarding wizard) — persona P3 (Owner non-tech 50+)
**Defer-to:** After Wave flow-kh3 finish

## Problem

Flow Verification Campaign KC-1 expected step 5 "Onboarding wizard (chọn school type / academic year / etc.)" — KHÔNG có evidence trong codebase. `find kiteclass/kiteclass-frontend/src -name '*onboarding*' -o -name '*wizard*' -o -name '*setup*'` likely empty. Bác Hùng (non-tech Owner) login `kc-trung-tam-anh-ngu-be-yeu.kitehub.me/admin` → thấy empty dashboard "0 students, 0 classes, 0 teachers" → không có guided path → đóng tab → email support. Per benchmark C3 (Shopify sample-product) + C4 (Slack progressive checklist) — VN edu non-tech persona benefit lớn. Surfaced: persona Finding 3.1 + benchmark C3+C4.

## Proposed Fix

Tạo onboarding wizard FE `/admin/onboarding`: 5-step checklist {Năm học verified / first class / first teacher invited / first student / settings reviewed}. Progress bar + dismissible. Plus "Tạo dữ liệu mẫu" (sample tenant fixture) option per benchmark C3. Related GAP-280 (Track 2 onboarding wizard kit) + GAP-288 (first-login tour).

## Acceptance Criteria

- [ ] FE route `/admin/onboarding` renders với 5-step checklist
- [ ] Progress persistence (DB row `onboarding_progress.steps`)
- [ ] Sample-data import option works (1 lớp + 3 học sinh + 1 GV fixture)
- [ ] Mobile-friendly breakpoint <768px tested

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,external-benchmark}.md
- Sister gaps: GAP-280, GAP-288, GAP-531 (init handoff)
- Flow Verification Campaign §4 row KC-1
