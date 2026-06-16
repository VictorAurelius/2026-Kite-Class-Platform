# GAP-1447: Wizard redirect dead-end — owner KC click "Mở wizard" bị bounce tới KH :3001 login (chưa có session/SSO)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-10)
**Affects:** KC-10 — `kiteclass-frontend/src/app/(dashboard)/branding/wizard/page.tsx` (redirect `NEXT_PUBLIC_KITEHUB_URL/branding/wizard`)

## Problem
Discovered Phase-2 browser walk KC-10. Owner KC click "Mở wizard" → redirect tới `NEXT_PUBLIC_KITEHUB_URL/branding/wizard` nhưng landing thực tế = `http://localhost:3001/login` (KiteHub login form) vì chưa có KH session/SSO → dead-end.

## Proposed Fix
Xác nhận cross-product SSO ở production (apex `kitehub.me` share session). Phương án: (a) verify shared-cookie SSO prod trước khi expose, (b) deep-link mang token/return-url để KH auto-auth, hoặc (c) hiển thị thông báo "cần đăng nhập KiteHub" rõ ràng thay vì bounce thẳng. G3 production-parity phải confirm owner KC reach được KH wizard.

## Acceptance Criteria
- [ ] Owner KC mở wizard không rơi vào KH login dead-end (SSO reach, deep-link auth, hoặc thông báo rõ)
- [ ] G3 confirm production-parity cross-product

## Related
- Discovered in: Phase-2 browser walk (flow KC-10), 2026-06-16
