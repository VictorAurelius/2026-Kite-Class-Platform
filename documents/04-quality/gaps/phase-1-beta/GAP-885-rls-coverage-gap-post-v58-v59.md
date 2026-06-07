# GAP-885: RLS coverage gap — bảng tạo sau V58/V59 không có policy

**Status:** 🟡 PARTIAL (90% — Wave p0-local-1 Bucket B: oauth_attempts RLS residual đóng (V66 + IT); landing_pages admin-bypass design còn lại)
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

- [x] KC migration extend cho `landing_pages`/`idempotency_keys`/`payment_records` — **V78__rls_sweep.sql** (đã ship trước wave này; state-check 2026-06-07 xác nhận DO-block ENABLE RLS + CREATE POLICY tenant_isolation cho cả 4 bảng KC)
- [x] KH migration extend cho `onboarding_progress`/`staff_invitations`/`staff_invitation_audit_log`/`impersonation_audit_log` (V58) + `oauth_attempts` (**V66**, Wave p0-local-1 Bucket B + `OauthAttemptsRlsPostgresIT` 4/4 PASS)
- [ ] `landing_pages` admin-bypass design documented — **CÒN LẠI** (public homepage read-path qua RLS tenant_isolation cần thiết kế admin-bypass / GUC-set; residual cuối)
- [x] Reference cluster docs 04 §A1 + 08 §A2 + KH 01 §A6 + KH 04 §A3

## Discovered in

4 cluster docs Wave 13.

## Log

- **2026-06-07 (Wave p0-local-1 Bucket B — oauth_attempts RLS residual đóng):** State-check 2026-06-07 xác nhận 7/8 bảng đã được sweep trước wave (V78 KC: landing_pages/idempotency_keys/payment_records/payment_idempotency_keys; V58 KH: onboarding_progress/staff_invitations/staff_invitation_audit_log/impersonation_audit_log). Bảng cuối `oauth_attempts` (kitehub-subscription) — V58 skip vì `tenant_id` là **BIGINT NULL** (không phải UUID, guard yêu cầu `data_type='uuid'`). V66 enable RLS + `tenant_isolation` policy (admin-bypass + NULL force-fail mirror V58; predicate `tenant_id::text` né cast mismatch BIGINT vs UUID-string GUC). `OauthAttemptsRlsPostgresIT` 4/4 PASS (Flyway thật, SET ROLE NOSUPERUSER). completion 70→90. Còn lại 10% = `landing_pages` admin-bypass design (AC #3) — public homepage read-path qua RLS cần thiết kế GUC-set/admin-bypass. Discovery: `oauth_attempts.tenant_id` BIGINT anomaly → filed [[GAP-1056]] (P2 re-key sang `instance_id` UUID).
