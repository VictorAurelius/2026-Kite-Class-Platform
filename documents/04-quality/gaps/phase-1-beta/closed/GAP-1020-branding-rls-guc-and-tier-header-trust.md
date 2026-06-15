# GAP-1020: Branding RLS GUC không set + X-Subscription-Tier client-controlled (quota/rate bypass)

**Status:** 🟢 DONE
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

- [x] RLS active: non-superuser role → cross-tenant branding_jobs query trả rỗng
- [x] Tier resolve server-side; client gửi ENTERPRISE không nâng quota
- [x] IT verify RLS isolation + tier-from-subscription trên Testcontainers

## Related

- Discovered in: KH-6 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md` (FM-3 + FM-6)
- Depends: GAP-1019 (trusted tenant header); Related: `ai-branding-guidelines.md` §2.5 + §4.3 quota

## Log

- **2026-06-07** (Wave g2-blockers-1 Bucket B — investigation, NOT fixed): Để OPEN — 2 phần đều cần infrastructure mới, KHÔNG phải quick edit (không rush security vào high-context). Findings cho next session:
  - **Part 1 (RLS GUC `app.current_tenant_id`):** `grep -rln "current_tenant_id\|set_config\|app.current_tenant"` trên `kitehub-subscription` + `kitehub-branding` Java = **0 hit** → KHÔNG có pattern kitehub nào để mirror (kiteclass-core có RLS+GUC pattern nhưng kitehub-branding chưa). Cần thiết lập cơ chế set GUC per-connection/per-tx TRƯỚC query. Sai cơ chế = silent cross-tenant leak → design cẩn thận + IT Testcontainers verify isolation thực.
  - **Part 2 (tier server-side resolve):** `X-Subscription-Tier` đọc ở 6 site: `BrandingWizardController:89,104` + `AIBrandingController:79,114,152,191` (đều `@RequestHeader defaultValue="FREE"`). KHÔNG có subscription-tier lookup client trong branding (`grep SubscriptionClient` = 0) → cần build cross-service call branding→subscription (Feign/REST) resolve tier từ tenant thay vì trust header. Infra mới, không phải edit.
  - **Status 🔵 OPEN** — deferred Bucket B; cả 2 phần = infra-design tasks security-sensitive.

- **2026-06-15** (Wave security-fix — FIXED, branch `fix/gap-1020-branding-rls-tier-2026-06-15`): cả 2 phần shipped + verified trên Testcontainers Postgres. Branding tests 369/369 PASS; subscription RLS IT PASS (V75 Flyway "now at version v75").

  **Part 1 — RLS GUC active (mirror kiteclass-core `TenantAwareDataSourceInterceptor`):**
  - `tenant/TenantContext.java` — ThreadLocal tenant + platform-admin flag + `runAs` cho background.
  - `tenant/TenantAwareDataSourceInterceptor.java` — AOP `@Around` mọi `@Transactional` → `SELECT set_config('app.current_tenant_id', :id, true)` (SET LOCAL) từ gateway-trusted tenant; thêm `app.is_platform_admin=true` cho admin; idempotent per-tx marker + reset afterCompletion (connection-pool safe).
  - `tenant/TenantContextFilter.java` — `OncePerRequestFilter` bind tenant từ **gateway-trusted `X-Tenant-Id`** (KHÔNG tin client `X-Instance-Id`); clear in finally; async/error re-dispatch covered. Registered trong `config/SecurityConfig.java` (`!test` chain).
  - `queue/BrandingJobConsumer.java` — background consumer wrap `TenantContext.setCurrentTenant(message.instanceId)` + clear finally (consumer không có HTTP request).
  - `V75__rls_admin_bypass_branding_tables.sql` (kitehub-subscription) — thêm `app.is_platform_admin` bypass vào 5 V34 branding-table policies (ai_usage_log / branding_instance_state / branding_jobs / branding_lifecycle_events / branding_regenerate_usage) khớp V58 pattern, NON-forced posture giữ nguyên.

  **Part 2 — tier resolved server-side:**
  - Gateway strip + JWT-tier reinject ĐÃ committed trên main (commit `e7444b455`, default-filter `RemoveRequestHeader=X-Subscription-Tier` + `JwtAuthenticationGatewayFilter` inject từ verified `tier` claim). Phần này = AC "client ENTERPRISE không nâng quota".
  - `tenant/SubscriptionTierResolver.java` — resolve authoritative tier từ `instances.tier` (canonical current-effective tier, khớp `TokenService`); fail-safe fallback header→FREE (không bao giờ escalate). Wired vào 6 site: AIBrandingController (4) + BrandingWizardController (2) — entitlement dùng tier server-resolved, không tin header.

  **Verify evidence (production-equivalent Testcontainers — Part 1 + tier-from-subscription per AC3):**
  - `BrandingRlsIsolationTest` (5 tests, postgres:16-alpine non-superuser role) — cross-tenant SELECT empty, WITH CHECK reject cross-tenant insert, unset-GUC default-deny, platform-admin bypass, tier-read `instances.tier` (PREMIUM) từ real Postgres.
  - `TenantAwareDataSourceInterceptorTest` (3) — set_config issued khi tenant bound / admin / no-op khi unset.
  - `SubscriptionTierResolverTest` (6) — DB tier wins over spoofed header; fail-safe fallback.
  - `AIBrandingControllerTierResolutionTest` (1) — client `X-Subscription-Tier: ENTERPRISE` → FREE limits applied (resolver wins).

  Cross-flow sweep (per `cross-flow-bug-class-sweep.md`): 2 sister sites `BrandingJobController:95` + `BrandingJobV1Controller:186` cùng class (tier-header trust cho FULL_AI gating) → DEFER GAP-1406 (gateway-strip đã đóng spoof; staleness hardening). RLS-without-GUC sweep: kitehub-subscription EXEMPT (documented intentional control-plane non-forced posture V34 header); kiteclass-core OK (đã có GUC interceptor). Sweep details trong PR body.

  Note: deployment hiện dùng DB role `kitehub` (owner) → RLS non-forced bypass → Part 1 là defense-in-depth kích hoạt dưới non-superuser role tương lai; behavior owner-role hôm nay không đổi (zero-regression). Verification dùng Testcontainers non-superuser role làm production-equivalent (live AWS walk dưới non-superuser role = trajectory tương lai, owner-role hiện tại không có gì khác để walk).
