# GAP-885: RLS coverage gap — bảng tạo sau V58/V59 không có policy

**Status:** 🟡 PARTIAL (45%)
**Priority:** 🔴 P0
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance/branding + KH auth/email)
**Affects:** Mọi bảng có `instance_id`/`tenant_id` tạo SAU V58/V59 (KC) hoặc V34/V50 (KH)

## Problem

V58/V59 (KC) + V34/V50 (KH) enable RLS với danh sách bảng tĩnh chạy 1 lần. Bảng tạo SAU không được enable:

**KC:**
- `landing_pages` (V75) — có `instance_id` NOT NULL nhưng không policy (cluster 08 §A2)
- `idempotency_keys` (V66) — có `tenant_id` NOT NULL nhưng không policy
- `payment_records` (V69) — không RLS (cluster 04 §A1)

**KH:**
- `oauth_attempts` (V51), `onboarding_progress` (V43), `staff_invitations` (V45) — có `tenant_id` nhưng sau V34/V50 (cluster 01 §A6)
- `staff_invitations`, `staff_invitation_audit_log` (V49), `impersonation_audit_log` (V48) — có `tenant_id` sau V34/V50 (cluster 04 §A3)

→ Cross-tenant leak risk medium-high (compliance: PDPL Art 5/11).

## Proposed Fix

Migration RLS bổ sung (DO-block re-run với danh sách mở rộng) — per cluster batched. `landing_pages` đặc biệt: public homepage endpoint cần admin-bypass thiết kế.

## Acceptance Criteria

- [ ] KC migration V## extend V58/V59 cho `landing_pages`/`idempotency_keys`/`payment_records`
- [ ] KH migration V## extend V34/V50 cho `oauth_attempts`/`onboarding_progress`/`staff_invitations`/`staff_invitation_audit_log`/`impersonation_audit_log`
- [ ] `landing_pages` admin-bypass design documented
- [ ] Reference cluster docs 04 §A1 + 08 §A2 + KH 01 §A6 + KH 04 §A3

## Discovered in

4 cluster docs Wave 13.
