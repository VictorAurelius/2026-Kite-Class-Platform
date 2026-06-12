---
id: GAP-813
title: Base-domain config không nhất quán + thiếu public endpoint resolve slug → tenantId UUID
status: PARTIAL
priority: P1
phase: phase-1-beta
domain: Mixed
created: 2026-05-29
---

# GAP-813 — Base-domain consistency + slug → tenantId UUID resolution

## Problem

Chuỗi **tenant → domain → landing** đang gãy ở 2 điểm độc lập:

### Vấn đề 1 — Base domain không nhất quán (4 nguồn khác nhau)

Cùng một khái niệm "base domain" được hardcode/cấu hình khác nhau across layers:

| Layer | Nguồn | Giá trị base domain |
|---|---|---|
| Gateway tenant resolver | `TenantResolverGatewayFilterFactory:49` + `KeyResolverConfig:44` `@Value("${kitehub.domain.base:.kitehub.me}")` | `.kitehub.me` |
| Gateway runtime config | `kitehub-gateway/src/main/resources/application.yml:700` `base: ${BASE_DOMAIN:.kitehub.me}` | `.kitehub.me` (env `BASE_DOMAIN`) |
| Demo seeder | `BrandingDataSeeder:65` `SKY_FRONTEND_URL = "https://sky-education.kite.local"` + `:53` `DEV_FRONTEND_URL = "https://thanglong.kite.local"` | `.kite.local` |
| Deploy/ADR docs | `documents/02-architecture/domain-management.md:12,24` | `kitehub.me` (tenant) + `kitehub.vn` (SaaS); CLAUDE.md/ADR nhắc `kitehub.me`/`kite.me` |

→ Subdomain seed (`sky-education.kite.local`) KHÔNG khớp suffix gateway strip (`.kitehub.me`). Khi browser truy cập `sky-education.kite.local`, gateway `extractSubdomain` so sánh `host.endsWith(".kitehub.me")` → fail → rơi xuống fallback → resolve sai hoặc 400. Tenant resolution chỉ "may mắn" hoạt động qua header `X-Instance-Subdomain` (local dev) hoặc JWT claim fallback (GAP-711), KHÔNG qua subdomain thật.

### Vấn đề 2 — Thiếu public endpoint map slug → tenantId UUID cho FE

FE `useTenantFromUrl.ts` parse subdomain/query-param trả về **slug string** (vd `sky-education`, `abc-center`). NHƯNG backend landing endpoint `LandingPageController` (kiteclass-core) khai báo:

```java
@RequestMapping("/api/v1/tenants/{tenantId}/landing")
public ... getLandingPage(@PathVariable UUID tenantId) { ... }
```

→ cần **UUID**, không phải slug. Hiện KHÔNG có public endpoint nào map `slug → tenantId UUID` cho FE middleware (GAP-811) gọi. Gateway nội bộ CÓ `InstanceRepository.findBySubdomain(slug)` nhưng chỉ dùng nội bộ để gắn header `X-Tenant-Id` cho request đã route — KHÔNG expose ra public cho FE resolve trước khi gọi landing API.

### Vấn đề 3 (phụ) — Slug-availability proxy chưa cross-check `instances` table thật

`SlugAvailabilityService:94` chỉ check `BrandingJob.organizationName` (lowercased) qua `existsByOrganizationNameLowercased` + reserved-words filter. Javadoc (`:22-26`) tự ghi đây là "best-effort proxy" — CHƯA cross-check `frontend_instances`/`instances` table thật. Một slug đã được provision thành `Instance.subdomain` nhưng chưa có `BrandingJob` row sẽ báo "available" sai → nguy cơ collision unique constraint `idx_instances_subdomain`.

## Root Cause

1. **Base domain**: mỗi layer được viết ở thời điểm khác nhau (gateway Wave sớm dùng placeholder `.kitehub.me`; seeder demo dùng `.kite.local` cho local; ADR/deploy docs evolve sang `kitehub.me`/`kite.me`) — chưa có single env-driven source-of-truth nào được dùng chung.
2. **slug → UUID**: landing API thiết kế UUID-based (đúng cho internal consistency) nhưng FE chỉ biết slug từ URL; bước resolve trung gian chưa được wire thành public endpoint. Gateway có repository sẵn nhưng không expose.
3. **slug-availability**: cross-service check (`kitehub-branding` → `instances` table thuộc kitehub-platform/gateway scope) chưa được implement; tác giả để follow-up.

## Proposed Fix (design)

### (a) Reconcile base domain về 1 nguồn env-driven `KITE_BASE_DOMAIN`

- **Canonical value đề xuất**: `kite.me` (prod) / `kite.local` (dev local).
  - **Lý do**: (1) GitHub Student Pack / AWS Activate dùng `kitehub.me` đã đăng ký (GAP-458/459) — `kite.me` ngắn gọn cho subdomain `{slug}.kite.me`; (2) `.kitehub.me` là placeholder cũ chưa sở hữu; (3) `kite.local` đã là quy ước seeder hiện tại cho local → ít churn nhất khi giữ local suffix.
  - User confirm cuối cùng giữa `kite.me` vs `kitehub.me` vs `kitehub.me` trước khi implement (đây là decision-doc config-shaped value per `audit-to-gap-pipeline.md` §2.7 → cần code-sync sweep).
- **Single env var `KITE_BASE_DOMAIN`** (default `.kite.local` cho dev), inject vào:
  - Gateway: đổi `@Value("${kitehub.domain.base:.kitehub.me}")` → `${kite.base.domain:.kite.local}` (cả `TenantResolverGatewayFilterFactory` + `KeyResolverConfig`) + `application.yml:700` `base: ${KITE_BASE_DOMAIN:.kite.local}`.
  - Seeder: thay hardcode `.kite.local` literal trong `BrandingDataSeeder` (SKY_FRONTEND_URL, DEV_FRONTEND_URL, contact website) bằng `@Value("${kite.base.domain:.kite.local}")` build URL `https://{slug}{baseDomain}`.
  - FE: expose qua `NEXT_PUBLIC_KITE_BASE_DOMAIN` env để `useTenantFromUrl` strip đúng suffix (thay vì hardcode `.kitehub.me` trong `getSubdomain`).
  - Docs: cập nhật `domain-management.md` reflect canonical value (per §2.7 decision-doc code-sync).

### (b) Public endpoint `GET /api/v1/tenants/by-subdomain/{slug}` resolve slug → tenantId

- **Endpoint mới** (đề xuất đặt ở kitehub-platform hoặc gateway-exposed, là nơi sở hữu `instances` table): `GET /api/v1/tenants/by-subdomain/{slug}` → response `{ tenantId: UUID, status: InstanceStatus, subdomain: String }`.
- Optional sister: `GET /api/v1/tenants/by-domain/{domain}` dùng `InstanceRepository.findByCustomDomain` cho custom-domain tenants.
- **Implementation**: reuse `InstanceRepository.findBySubdomain(slug)` (đã có). Service trả DTO chỉ chứa `tenantId + status + subdomain` (KHÔNG leak database_url/password — projection-only).
- **Public/auth**: phải nằm trong gateway whitelist (no JWT) vì FE gọi TRƯỚC khi có session (landing page public). Cross-flow sweep per `cross-flow-bug-class-sweep.md`: verify gateway SecurityConfig whitelist + CORS cho path này.
- **Status gate**: nếu `status ∉ {ACTIVE, TRIAL}` → trả `404 Not Found` (hoặc `403` với body `{status}`) để FE render trang "instance unavailable", KHÔNG leak instance tồn tại. Mirror gateway `ALLOWED_STATUSES` logic.
- **Not-found**: slug không tồn tại → `404` (FE render landing-not-found).
- **Caching**: response cacheable (slug→UUID mapping ít đổi). Đề xuất `Cache-Control: public, max-age=300` + FE middleware cache trong memory/localStorage; invalidate khi tenant suspend (event-driven, defer Phase 2). FE flow (GAP-811): `useTenantFromUrl` → slug → gọi `by-subdomain/{slug}` → nhận UUID → gọi `/api/v1/tenants/{tenantId}/landing`.

### (c) Fix slug-availability cross-check `instances` table thật

- `SlugAvailabilityService.isTaken` bổ sung check thứ 3: query `instances.subdomain` thật (qua endpoint nội bộ tới kitehub-platform HOẶC shared read của `InstanceRepository.findBySubdomain`). Một slug "taken" nếu: reserved-word OR `BrandingJob.organizationName` match OR `instances.subdomain` đã tồn tại.
- Cross-service call cần resilience (Circuit Breaker per `design-patterns.md` §3.6) vì gọi sang service khác.

## Acceptance Criteria

- [ ] 1 env var `KITE_BASE_DOMAIN` (+ `NEXT_PUBLIC_KITE_BASE_DOMAIN`) là single source; gateway + seeder + FE đều đọc từ đó (zero hardcode `.kitehub.me`/`.kite.local` literal còn lại — verify bằng grep).
- [ ] Canonical base-domain value chốt với user; `domain-management.md` cập nhật đồng bộ; code-sync sweep zero stale ref per `audit-to-gap-pipeline.md` §2.7.
- [x] Endpoint `GET /api/v1/public/tenants/by-subdomain/{slug}` trả `{id, subdomain, name, status}` (UUID đúng), projection-only (không leak DB credential). — Wave tenant-domain-1 Bucket B (PR pending) `PublicTenantController` + `TenantLookupService` + `TenantResolveDto`. Path chỉnh `public/tenants/by-subdomain/{slug}` per Bucket 0 api-contract §9.
- [x] Endpoint trong gateway public whitelist + CORS đúng; status ACTIVE/TRIAL → 200 (TRIAL collapsed to ACTIVE); SUSPENDED/DELETED → 410 GONE; slug không tồn tại → 404; format invalid → 400. — `kitehub-gateway` route `public-tenant-resolve` (rate-limit 30/min/IP) + `kitehub-subscription` SecurityConfig `permitAll` `/api/v1/public/tenants/**`.
- [ ] `SlugAvailabilityService` cross-check `instances.subdomain` thật (3 nguồn: reserved + BrandingJob + instances).
- [ ] RST walk per `feature-ship-runtime-walk-mandate`: browser truy cập `{slug}.{KITE_BASE_DOMAIN}` → gateway resolve đúng tenant → FE resolve slug→UUID → landing render. Evidence 3 lớp (gateway log X-Tenant-Id + curl endpoint trả UUID + browser landing) paste vào closure block.
- [x] BE `mvn test` PASS (Bucket B subscription module: compile + test-compile + `PublicTenantControllerTest` 13/13 PASS local). FE `pnpm build` — Bucket C scope.

## Related

- **GAP-811** — FE tenant middleware consume endpoint `by-subdomain/{slug}` này để map slug → UUID trước khi gọi landing API (FE side của bridge).
- **GAP-810** — Demo landing image assets (cùng chuỗi tenant → domain → landing; GAP-810 lo asset/render, GAP-813 lo resolution).
- **GAP-711** — Gateway JWT tenantId claim fallback (đã ship; là workaround khi subdomain resolution fail — GAP-813 fix root cause subdomain).
- Files liên quan: `TenantResolverGatewayFilterFactory.java`, `KeyResolverConfig.java`, `kitehub-gateway/application.yml:700`, `BrandingDataSeeder.java`, `Instance.java` + `InstanceRepository.java`, `SlugAvailabilityService.java`, `LandingPageController.java`, `useTenantFromUrl.ts`, `domain-management.md`.

## Log

- **2026-05-29:** Gap created sau research session — xác nhận 4 nguồn base-domain không nhất quán (gateway `.kitehub.me` / seeder `.kite.local` / docs `kitehub.me`+`kitehub.vn` / ADR `kitehub.me`) + slug→UUID gap (FE trả slug, landing API cần UUID, không có public resolve endpoint). Design 3 phần: (a) reconcile env-driven `KITE_BASE_DOMAIN`, (b) endpoint `GET /api/v1/tenants/by-subdomain/{slug}`, (c) fix slug-availability cross-check `instances` thật. Status OPEN — chờ implement (decision-doc base-domain value cần user confirm trước, per audit-to-gap-pipeline §2.7).
- **2026-06-01 (Wave tenant-domain-1 Bucket B):** Flip OPEN → PARTIAL (completion 55%). Ship BE endpoint per Bucket B scope: `PublicTenantController` (`/api/v1/public/tenants/by-subdomain/{slug}` — slug regex + 200/400/404/410 mapping + TRIAL→ACTIVE collapse for public projection), `TenantLookupService` (read-only Optional<Instance> + DTO projection — zero sensitive field leak), `TenantResolveDto`, `kitehub-gateway` route `public-tenant-resolve` (rate-limit 30/min/IP via `ipKeyResolver` + circuit breaker), `kitehub-subscription` SecurityConfig `permitAll` `/api/v1/public/tenants/**` (in front of authenticated gateway tail). Tests: `PublicTenantControllerTest` (Mockito, 13 cases — 200 ACTIVE/TRIAL, 404 unknown/PENDING, 410 SUSPENDED/DELETED, 400 uppercase/leading-hyphen/trailing-hyphen/length/empty/underscore, boundary 1-char + 50-char). IT: `PublicTenantPostgresIT` (Testcontainers postgres:16-alpine, 7 cases — 200 ACTIVE × 2, 404 unknown, 410 SUSPENDED, 400 uppercase + underscore, 404 soft-deleted). `InstanceRepository.findBySubdomainAndDeletedFalse(String)` đã có sẵn — không cần thêm method. Local verify: `mvnw -pl kitehub-subscription compile + test-compile + test -Dtest=PublicTenantControllerTest` all PASS. Pending Bucket C: FE consumer `resolveTenant.ts` (GAP-811), base-domain env unification, SlugAvailabilityService cross-check, RST walk evidence 3 lớp.

## Outside-in findings (3-agent audit 2026-05-29)

Bổ sung trước khi lock (per `outside-in-coverage-trigger.md`):

- **Failure-mode (P0):** base-domain drift (`.kitehub.me` gateway vs `kite.me`/`kite.local`) → `extractSubdomain` fail im lặng → mọi resolve về 404/JWT-fallback. Reconcile 1 env `KITE_BASE_DOMAIN` + audit-parity script (như `audit-env-coverage`) verify đồng bộ mọi layer.
- **Security (P0) — cross-ref GAP-814:** localhost/apex no-subdomain → JWT-claim fallback NHƯNG signature KHÔNG verify (best-effort) → kết hợp host-spoofing thành lỗ; endpoint by-subdomain + middleware phải dựa nguồn tin cậy.
- **Benchmark:** tách rõ ownership-verify (TXT/DCV) khỏi routing-record (CNAME/A) — endpoint `by-subdomain` trả `{tenantId, status}` chỉ cho routing/landing resolve, không lẫn verify.
- **Finding:** `resolveTenantUuid` pattern đã tồn tại internal → endpoint mới chủ yếu expose, không build mới.
