# GAP-955: Provisioning UI feedback missing — Owner thấy spinner vô tận

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Onboarding UX) — first impression trust signal
**Defer-to:** After Wave flow-kh3 finish

## Problem

Provisioning saga chạy ~3-30s (DB create + Flyway migrate + branding plan execute). UC-PROV-01 không define UI feedback contract. FE chưa có `/onboarding/provisioning` polling page. Owner click "Vào trung tâm của tôi" trong KH-2c → redirect `kc-<slug>.kitehub.me/admin` → BE saga vẫn GENERATING → admin page 503/redirect-loop → Owner refresh 5 lần → bỏ cuộc. Surfaced: persona Finding 1.3.

## Proposed Fix

Tạo FE polling page `/onboarding/provisioning` show progress steps (DB ready / Migrate ready / Branding ready / Deployed). Poll `Instance.status` every 2s với exponential backoff. Auto-redirect `/admin` khi DEPLOYED. Friendly error message + retry button cho FAILED state.

## Acceptance Criteria

- [ ] FE route `/onboarding/provisioning` exists với polling logic
- [ ] Status updates render trong <2s after BE state change
- [ ] FAILED state shows "Liên hệ hỗ trợ" + admin contact

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 1.3
- Sister: GAP-531 (init handoff)
- Flow Verification Campaign §4 row KC-1
