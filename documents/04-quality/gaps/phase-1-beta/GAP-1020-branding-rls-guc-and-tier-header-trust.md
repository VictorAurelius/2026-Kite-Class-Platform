# GAP-1020: Branding RLS GUC không set + X-Subscription-Tier client-controlled (quota/rate bypass)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-6 AI Branding wizard G1 walk)
**Affects:** kitehub-branding (RLS tenant isolation + AI quota/rate-limit)

## Problem

KH-6 G1 walk catalog 2 "trust-client-header" issue:

1. **RLS GUC không set (FM-3):** `branding_jobs` + `branding_outbox` có RLS enabled (V34 + V58 policy trên `app.current_tenant_id`) nhưng branding service **không bao giờ SET GUC** (0 grep hit cho `set_config`/`current_tenant_id`). Local OK vì DB role `kitehub` bypass RLS (owner/superuser), nhưng prod với non-superuser role → RLS policy đánh giá `app.current_tenant_id` = NULL → list rỗng + insert WITH CHECK fail. RLS hiện chỉ là defense-in-depth giả (không active).

2. **X-Subscription-Tier client-controlled (FM-6):** `AIBrandingController.generate-*` đọc `@RequestHeader("X-Subscription-Tier", defaultValue="FREE")` — header này client tự gửi, gateway KHÔNG inject/strip. Owner gửi `X-Subscription-Tier: ENTERPRISE` → bypass regenerate quota (FREE 3/session) + AI rate-limit + input cap tier. Cost-control bypass.

## Root Cause

(1) Branding chưa wire một `OncePerRequestFilter`/interceptor set Postgres GUC `app.current_tenant_id` từ trusted tenant. (2) Tier phải resolve server-side từ subscription (instance → active subscription tier), không tin client header.

## Proposed Fix

1. Set `app.current_tenant_id` GUC per-request từ gateway-trusted tenant (depends GAP-1019 trusted header). Verify RLS active với non-superuser DB role.
2. Resolve subscription tier server-side (gọi subscription service / shared DB lookup theo instanceId) thay vì tin `X-Subscription-Tier`; gateway strip client-sent tier header.

## Acceptance Criteria

- [ ] RLS active: non-superuser role → cross-tenant branding_jobs query trả rỗng
- [ ] Tier resolve server-side; client gửi ENTERPRISE không nâng quota
- [ ] IT verify RLS isolation + tier-from-subscription trên Testcontainers

## Related

- Discovered in: KH-6 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md` (FM-3 + FM-6)
- Depends: GAP-1019 (trusted tenant header); Related: `ai-branding-guidelines.md` §2.5 + §4.3 quota
