# GAP-1452: KH-8 G2 recipe refresh — consent/DSAR shape sai, Bearer che bug, GAP-1025 đã FIXED

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-8)
**Affects:** KH-8 — `documents/05-guides/operations/2026-06-06-g2-recipe-kh8-offboarding-pdpl-consent.md` Bước 1/4/6 + §5

## Problem
Discovered Phase-2 browser walk KH-8. Recipe drift nhiều bước:
- Bước 1: consent shape recipe dùng `{consents:{analytics,marketing}}` → 400; BE `ConsentRequest` muốn flat `analyticsConsented`/`marketingConsented`.
- Bước 4: DSAR shape recipe dùng `{requestType,email}` + Bearer → 400; BE `DsarRequest` muốn `{rightType,requesterName,requesterEmail,nationalIdLast4}`. Bearer che cả 404 FE lẫn 401 anonymous (curl-with-header anti-pattern, vi phạm g1-browser-walk).
- Bước 6 + §5: recipe nói owner thấy TẤT CẢ instances (GAP-1025 P0 KNOWN-ISSUE expect 200); thực tế `owner.test` → 403 (GAP-1025 đã FIX `@PreAuthorize` admin-only). Owner-self path = `/api/platform/instances/owner/{ownerId}` → 200.

## Proposed Fix
- Bước 1 → `{"visitorId":"$VID","analyticsConsented":true,"marketingConsented":false}`.
- Bước 4 → browser-walk qua FE `/legal/data-rights` (public, no auth) đúng shape `rightType/requesterName/requesterEmail/nationalIdLast4` thay vì curl gắn Bearer.
- Bước 6 + §5: bỏ GAP-1025 khỏi KNOWN-ISSUE; owner GET `/api/platform/instances` → 403 (đúng), off-boarding path = `/owner/{ownerId}` → 200. (Khuyến nghị browser-walk `(customer)/instances` FE page.)

## Acceptance Criteria
- [ ] Bước 1 consent body khớp `ConsentRequest` flat shape → 201
- [ ] Bước 4 dùng browser-walk FE DSAR đúng shape (không curl+Bearer)
- [ ] §5 bỏ GAP-1025 khỏi KNOWN-ISSUE; Bước 6 kỳ vọng owner-self path đúng

## Related
- Discovered in: Phase-2 browser walk (flow KH-8), 2026-06-16
- GAP-1025 đã FIXED (DONE); FE DSAR bug GAP-1438; anonymous DSAR GAP-1439
