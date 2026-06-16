# GAP-1447: Wizard redirect dead-end — owner KC click "Mở wizard" bị bounce tới KH :3001 login (chưa có session/SSO)

**Status:** 🟡 PARTIAL
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-10)
**Affects:** KC-10 — `kiteclass-frontend/src/app/(dashboard)/branding/wizard/page.tsx` (redirect `NEXT_PUBLIC_KITEHUB_URL/branding/wizard`)

## Problem
Discovered Phase-2 browser walk KC-10. Owner KC click "Mở wizard" → redirect tới `NEXT_PUBLIC_KITEHUB_URL/branding/wizard` nhưng landing thực tế = `http://localhost:3001/login` (KiteHub login form) vì chưa có KH session/SSO → dead-end.

## Proposed Fix
Xác nhận cross-product SSO ở production (apex `kitehub.me` share session). Phương án: (a) verify shared-cookie SSO prod trước khi expose, (b) deep-link mang token/return-url để KH auto-auth, hoặc (c) hiển thị thông báo "cần đăng nhập KiteHub" rõ ràng thay vì bounce thẳng. G3 production-parity phải confirm owner KC reach được KH wizard.

## Acceptance Criteria
- [x] Owner KC mở wizard không rơi vào KH login dead-end (chọn phương án (c) thông báo rõ + mở tab mới — code implemented, pending re-walk)
- [ ] G3 confirm production-parity cross-product (DEFER — cần shared-session SSO KC↔KH, chưa có)

## Fix (PARTIAL — phương án (c) khả thi; SSO đầy đủ defer)
Branch `fix/phase3-bucketD2-kc-branding`. File `kiteclass-frontend/src/app/(dashboard)/branding/wizard/page.tsx`:
- BỎ auto-bounce `window.location.assign(target)` trong `useEffect` — đây chính là nguyên nhân dead-end (đá owner sang KH login form không session).
- Thay bằng hand-off card tường minh: tiêu đề + mô tả + **cảnh báo amber** "Hiện chưa có đăng nhập dùng chung KiteClass↔KiteHub, có thể cần đăng nhập lại KiteHub ở tab mới; phiên KiteClass hiện tại được giữ nguyên".
- Link mở `target="_blank" rel="noopener noreferrer"` → owner click chủ động, KHÔNG mất session KC (không bị bounce ra khỏi app).
- Giữ `data-testid="kc-wizard-redirect"` + `kc-wizard-redirect-link`; loading state (no-tenant) giữ nguyên.
- Test `wizard-page.test.tsx` cập nhật: assert KHÔNG còn auto `window.location.assign`, link `target="_blank"`, có notice "chưa có đăng nhập dùng chung" (3 PASS).

DEFER (cần design lớn, ngoài scope Bucket D2): cross-product shared-session SSO (apex `kitehub.me` share cookie HOẶC deep-link token để KH auto-auth) — khi SSO land, card có thể resume auto-redirect. G3 production-parity confirm phụ thuộc SSO + AWS stack → defer.

## Related
- Discovered in: Phase-2 browser walk (flow KC-10), 2026-06-16
- KH/KC boundary: `.claude/rules/kitehub-kiteclass-boundary.md` §2 (KC `:3000` ↔ KH `:3001`)
- Fixed in: `fix/phase3-bucketD2-kc-branding` (Phase-3 Bucket D2)
