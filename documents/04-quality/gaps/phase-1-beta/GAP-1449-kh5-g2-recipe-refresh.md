# GAP-1449: KH-5 G2 recipe refresh — seed/credential không tồn tại + renew semantics khác claim

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-5)
**Affects:** KH-5 — `documents/05-guides/operations/2026-06-06-g2-recipe-kh5-subscription-lifecycle.md` §1/§2.2/§4a/§5

## Problem
Discovered Phase-2 browser walk KH-5. Recipe G2 lỗi thời, không reproduce được:
- Recipe hardcode SUB `81cf38cd-...` (absent) và `owner.test@test.vn` (instance đã DELETED). Walk phải tự seed subscription trên instance `271dc912` rồi cleanup để chạy downgrade/renew/cancel.
- Renew (§4a/§5, cite GAP-1016): recipe claim renew không tạo payment; thực tế renew trả 204 + extend `expires_at` +1 tháng NGAY trước khi VietQR payment confirm (status PENDING). GAP-1016 hiện DONE — cần xác nhận semantics (defer grant tới payment COMPLETED, hay immediate-extend-then-reconcile by design) rồi update §4a/§5.

## Proposed Fix
Re-seed BASIC/ACTIVE subscription cho live owner instance + refresh SUB id + credentials (hoặc thêm seed script). Xác nhận + document renew semantics đúng trong §4a/§5.

## Acceptance Criteria
- [ ] Recipe §1/§2.2 trỏ SUB id + credentials tồn tại, reproduce được downgrade/renew/cancel
- [ ] §4a/§5 mô tả đúng renew semantics (immediate-extend + PENDING payment hoặc deferred grant)

## Related
- Discovered in: Phase-2 browser walk (flow KH-5), 2026-06-16
- Renew semantics: GAP-1016 (DONE)
