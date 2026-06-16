# GAP-1451: KH-7 G2 recipe refresh — claim no-FE/curl-only sai (FE đầy đủ tồn tại) + sad-path 403-not-404 + GAP-1023 đã FIXED

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-7)
**Affects:** KH-7 — `documents/05-guides/operations/2026-06-06-g2-recipe-kh7-domain-management.md` §1/§3/§4/§6

## Problem
Discovered Phase-2 browser walk KH-7. Recipe lỗi thời nặng:
- §1 (dòng 16+26) claim no-FE + curl-only, nhưng FE `CustomDomainTab.tsx` + `hooks/use-domain.ts` đầy đủ tồn tại → thiếu browser-walk steps (vi phạm g2-handoff-md-mandate §3.4: affordance FE-wired phải có browser-walk).
- §3/§4/§6 nói cross-tenant domain IDOR (GAP-1023) còn mở "(SAI) 200"; thực tế `DomainController` + `TenantOwnershipGuard.requireOwnership` enforce 403. GAP-1023 hiện DONE.
- §4 sad-path "Instance không tồn tại → 404"; thực tế ownership guard chạy trước existence-check → trả 403 (hành vi no-enumeration, security-positive).

## Proposed Fix
Thêm section setup browser (`:3001` login → /settings → tab "Tên miền") + stepped browser-walk cho form/pending/verify/remove, giữ curl làm bổ trợ. Sửa §4 sad-path cross-tenant + instance-không-tồn-tại kỳ vọng 403 (không 404/200). Xóa phần "cross-tenant còn mở". Cập nhật credentials/instance khớp tenant thật.

## Acceptance Criteria
- [ ] Recipe có browser-walk steps cho `CustomDomainTab` (form/pending/verify/remove)
- [ ] §4 sad-path cross-tenant + instance-không-tồn-tại kỳ vọng 403
- [ ] Bỏ claim GAP-1023 còn mở (đã FIXED)

## Related
- Discovered in: Phase-2 browser walk (flow KH-7), 2026-06-16
- Cross-tenant IDOR đã FIXED: GAP-1023 (DONE)
