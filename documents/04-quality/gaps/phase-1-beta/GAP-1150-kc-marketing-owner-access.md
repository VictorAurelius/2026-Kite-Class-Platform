# GAP-1150: KiteClass marketing controllers gate `ADMIN,TEACHER` — OWNER có nên truy cập?

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-10 (cross-flow sweep từ GAP-1139 owner-authz fix)
**Affects:** kiteclass-core — `marketing/{LandingPageController, ContactMessageController, LeadController}` (10 @PreAuthorize sites)

## Problem

Trong khi fix GAP-1139 (OWNER bị 403 trên reports/enrollments/payroll), cross-flow sweep phát hiện 10 endpoint marketing/CRM dùng `@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")` — cũng KHÔNG có OWNER. Cùng bug-class signature (gate loại OWNER) nhưng khác scope (CRM/landing/lead/contact), nên DEFER khỏi GAP-1139 fix.

Câu hỏi design: OWNER (chủ trường) có nên truy cập landing-page config + contact messages + leads không? Khả năng cao CÓ (OWNER ≥ TEACHER), nhưng `ADMIN,TEACHER` là allowlist curated — cần xác nhận intent trước khi thêm OWNER (vs đây là deliberate teacher-marketing scope).

## Proposed Fix

Design-first: check rules.md/use-cases marketing domain → xác định OWNER trong allowlist. Nếu yes → 10 sites `hasAnyRole('ADMIN','TEACHER')` → `hasAnyRole('ADMIN','OWNER','TEACHER')`.

## Acceptance Criteria

- [ ] Design decision: OWNER trong marketing allowlist (cite rules.md)
- [ ] Nếu yes — 10 sites thêm OWNER + walk owner truy cập landing/lead/contact 200

## Related

- Parent: GAP-1139 (owner tenant-admin authz fix)
- Discovered in: cross-flow sweep GAP-1139 PR
