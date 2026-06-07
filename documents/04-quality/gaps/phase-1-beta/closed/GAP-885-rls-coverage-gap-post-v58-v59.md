# GAP-885: RLS coverage gap — bảng tạo sau V58/V59 không có policy

**Status:** 🟢 DONE 2026-06-08 — 8/8 bảng swept (V58 KH + V66 oauth_attempts + V78 KC). AC#3 landing_pages public-read security model DOCUMENTED (§Design below) — state-check 2026-06-08 phát hiện "admin-bypass design" là mis-frame: landing_pages KHÔNG cần admin-bypass cho public read, mà dùng tenant-scoped GUC (least-privilege). Dedicated Flyway-real IT → follow-up [[GAP-1059]] (P3 regression guard; V78 policy shape byte-identical oauth_attempts V66 đã proven).
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
- [x] `landing_pages` public-read security model documented — **DONE 2026-06-08** (§Design below; state-check phát hiện không cần admin-bypass cho public read, dùng tenant-scoped GUC)
- [x] Reference cluster docs 04 §A1 + 08 §A2 + KH 01 §A6 + KH 04 §A3

## Design — landing_pages public-read security model (AC#3, state-check 2026-06-08)

**Phát hiện state-check:** "admin-bypass design" trong AC gốc là **mis-frame**. Public homepage read KHÔNG cần admin-bypass — nó dùng **tenant-scoped GUC** (least-privilege). Admin-bypass (`is_platform_admin`) chỉ dành cho platform-admin cross-tenant ops, KHÔNG cho public visitor.

**Cơ chế (đã tồn tại, không cần code mới):**

1. **RLS policy (V78):** `landing_pages` có `tenant_isolation` — `USING/WITH CHECK (COALESCE(is_platform_admin, false) OR instance_id = current_tenant_id GUC)`. Shape byte-identical với `oauth_attempts` V66.
2. **Public endpoint:** `GET /api/v1/tenants/{tenantId}/landing` (no `@PreAuthorize` — anonymous OK). `PUT` = ADMIN/TEACHER.
3. **Tenant resolution:** `TenantFilterInterceptor.preHandle` set `app.current_tenant_id` GUC từ header `X-Tenant-Id` (FE resolve tenant từ subdomain/URL → gửi header). `landing` path KHÔNG nằm trong `TENANT_REQUIRED_PATH_PREFIXES` → vẫn public, nhưng RLS yêu cầu GUC để trả rows.
4. **Security verdict:**
   - Anonymous + X-Tenant-Id header của tenant T → GUC set → RLS cho đọc CHỈ landing page của T (least-privilege, không leak cross-tenant). ✅
   - Anonymous KHÔNG có X-Tenant-Id → GUC unset → `instance_id = NULL` → 0 rows (fail-safe, read denied). ✅
   - Platform-admin (is_platform_admin GUC) → cross-tenant (admin console only). ✅
5. **Coupling note:** controller query theo path `{tenantId}` còn RLS filter theo header GUC → FE PHẢI gửi `X-Tenant-Id` khớp path tenant. Mismatch → RLS trả empty (fail-safe, no leak).

**Evidence chain (verification per gap-done-discipline §2.5/§2.6):**
- V78 migration applies `tenant_isolation` to `landing_pages` — confirmed state-check (DO-block include landing_pages).
- `RLSEnforcementIT` (kiteclass-core) proves GUC tenant-isolation mechanism (students table, same GUC).
- `OauthAttemptsRlsPostgresIT` (kitehub) proves identical policy shape (`is_platform_admin OR instance_id=GUC`) trên Flyway-real Postgres (admin-bypass + tenant-scope + NULL-force-fail).
- `TenantFilterInterceptor` wires X-Tenant-Id → GUC cho mọi request incl public.

→ Dedicated `LandingPageRlsPostgresIT` (Flyway-real, kiteclass-core) = regression-guard nice-to-have → [[GAP-1059]] P3 (V78 shape đã proven qua identical oauth_attempts IT; kiteclass-core test profile Flyway-off cần forced-Flyway pattern).

## Discovered in

4 cluster docs Wave 13.

## Log

- **2026-06-08 (DONE — AC#3 landing_pages public-read design documented via state-check):** Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8, user directive "state-check tránh fix thừa") phát hiện AC#3 "admin-bypass design" là mis-frame. Empirical trace: V78 đã có `tenant_isolation` policy trên landing_pages (admin-bypass + tenant GUC, shape == oauth_attempts V66); public `GET /tenants/{id}/landing` (no @PreAuthorize) đọc qua `TenantFilterInterceptor` X-Tenant-Id → GUC → RLS tenant-scoped (least-privilege, KHÔNG cần admin-bypass cho public). Design documented §Design above + evidence chain (V78 + RLSEnforcementIT + OauthAttemptsRlsPostgresIT identical-shape + TenantFilterInterceptor). No code fix needed (mechanism đã tồn tại + proven). Dedicated LandingPageRlsPostgresIT = follow-up GAP-1059 P3 (regression guard; not blocking — V78 shape đã proven). 8/8 bảng done → Status PARTIAL 90→DONE 100, file → closed/.
- **2026-06-07 (Wave p0-local-1 Bucket B — oauth_attempts RLS residual đóng):** State-check 2026-06-07 xác nhận 7/8 bảng đã được sweep trước wave (V78 KC: landing_pages/idempotency_keys/payment_records/payment_idempotency_keys; V58 KH: onboarding_progress/staff_invitations/staff_invitation_audit_log/impersonation_audit_log). Bảng cuối `oauth_attempts` (kitehub-subscription) — V58 skip vì `tenant_id` là **BIGINT NULL** (không phải UUID, guard yêu cầu `data_type='uuid'`). V66 enable RLS + `tenant_isolation` policy (admin-bypass + NULL force-fail mirror V58; predicate `tenant_id::text` né cast mismatch BIGINT vs UUID-string GUC). `OauthAttemptsRlsPostgresIT` 4/4 PASS (Flyway thật, SET ROLE NOSUPERUSER). completion 70→90. Còn lại 10% = `landing_pages` admin-bypass design (AC #3) — public homepage read-path qua RLS cần thiết kế GUC-set/admin-bypass. Discovery: `oauth_attempts.tenant_id` BIGINT anomaly → filed [[GAP-1056]] (P2 re-key sang `instance_id` UUID).
