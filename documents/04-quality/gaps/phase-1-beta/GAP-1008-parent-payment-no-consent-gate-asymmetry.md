# GAP-1008: Parent payment endpoint thiếu consent gate (asymmetry)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-05 (Wave flow-kc8 KC-8 pre-walk persona sim FM#4)
**Affects:** `kiteclass-core` `ParentPaymentController` + `ParentPaymentService`

## Problem

`POST /api/v1/parent/children/{childId}/payments` enforce link (403 PARENT_NOT_LINKED) + idempotency + (Wave flow-kc8) `@PreAuthorize hasAccessToChild`, NHƯNG **không gọi `consentService.checkConsent`**. Cả 5 read facet (attendance/fees/conduct/transcript/notifications) đều gate consent.

Asymmetry: parent đã link nhưng chưa cấp consent `fees` → KHÔNG đọc được fees (403 PARENT_CONSENT_REQUIRED) nhưng VẪN POST payment được (pay-but-can't-see). Có thể intentional (payment ≠ personal-data read, PDPL angle khác) — cần confirm vs `documents/01-business/.../parent/rules.md` trước khi quyết định fix hay accept.

## Proposed Fix

Confirm vs rules.md: nếu payment cần consent field (vd `fees` hoặc `payment`) → thêm `consentService.checkConsent(parentId, childId, CONSENT_FIELD_FEES)` trong ParentPaymentService. Nếu intentional (payment exempt) → document rationale trong rules.md + javadoc.

## Acceptance Criteria

- [ ] Quyết định documented: payment requires consent OR exempt (với rationale).
- [ ] Nếu requires → checkConsent wired + walk verify 403 khi no consent.

## Related

- Discovered in: Wave flow-kc8 KC-8 pre-walk persona sim FM#4
- Payment VietQR là stub Wave 106 (GAP-286) — fix có thể fold vào Wave 106
