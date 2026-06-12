# GAP-1213: Wizard deploy = MOCK — không propagate theme/assets sang KC-core → landing thật không bao giờ đổi

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Mixed
**Found:** 2026-06-11 (branding-100 failure-mode audit — finding #1)
**Affects:** `kitehub-branding` MockProvisioningService + chuỗi KH-branding → kiteclass-core

## Problem

`MockProvisioningService` ghi `BrandingAsset[]` lên `BrandingJob` entity + lifecycle marker rồi PROCESSING→COMPLETED — KHÔNG có propagation cross-service (event/API) sang kiteclass-core (`BrandingResourceRepository`/`FrontendInstance`/landing_pages). Branding mới chỉ tồn tại trong preview iframe client-side. User "Deploy thành công" nhưng landing per-tenant thật giữ nguyên — toàn chuỗi giá trị của AI branding đứt ở mắt cuối. GAP-1021 chạm phần persist active theme; mảnh cross-service propagation này chưa có gap.

## Proposed Fix

Outbox event `branding.deployed` (per design-patterns §3.5) từ kitehub-branding → consumer kiteclass-core áp theme/assets vào Branding/LandingPage (+ evict cache per GAP-1203) — HOẶC nếu giữ mock Phase 1: relabel UI "mô phỏng" + disclaimer (chống mislead). Quyết định trong wave branding-100 bucket C.

## Acceptance Criteria

- [ ] Wizard deploy xong → landing per-tenant thật đổi theme/banner (browser verify)
- [ ] Hoặc (interim) UI ghi rõ mô phỏng, không toast "deploy thành công"

## Related

- Audit: `2026-06-11-branding-100-failure-mode-matrix.md` #1; sister GAP-1021/1108; wave branding-100 bucket C

## Log

- **2026-06-12** (Wave branding-100 Bucket C — code-level DONE, runtime-walk pending): Chuỗi propagation thật đã ship (Option (a) outbox event, KHÔNG dùng interim relabel). Producer (kitehub-branding): `MockProvisioningService` sau khi DEPLOYED emit `branding.deployed` qua `BrandingDeployedPublisher` (REQUIRES_NEW txn + outbox-first per design-patterns §3.5.1 Exception A) → exchange `branding.events` topic (re-declared cả 2 service idempotent) routing key `branding.deployed`; payload = tenantId(instanceId) + slug + frontendUrl + primary/secondary/accent colours + logoUrl + brandingVersion. Consumer (kiteclass-core): `BrandingDeployedEventConsumer` (raw Message decode UTF-8 per GAP-1045 precedent + TenantContext set/clear + swallow+ACK) → `LandingPageService.applyDeployedBranding` áp primaryColor/secondaryColor/logoUrl vào `landing_pages` + `@CacheEvict("landingPages", key=instanceId)` (cùng key public read) → landing per-tenant đổi theme. Idempotency: `landing_pages.branding_version` (V98) — skip event version ≤ stored. Tests PASS: `BrandingDeployedPublisherTest` (3) + `BrandingDeployedEventConsumerTest` (4) + `LandingPageApplyDeployedBrandingTest` (3). AC #1 còn `[ ]` chờ G2/G1 browser-walk: wizard deploy → mở landing tenant (`{slug}.kiteclass.vn` / nip.io local) xác minh theme đổi thật. Status PARTIAL per gap-done-discipline §3 (runtime walk = coordinator/G2).

## Log — 2026-06-12 G1 walk: wire-format drop (Bug #4) + fix

G1 browser walk TEMPLATE e2e: SSE complete + DoneStep OK nhưng `landing_pages` KHÔNG đổi
(branding_version=0). Root cause: publisher KH gửi `__TypeId__=com.kitehub.branding.outbox.BrandingDeployedEvent`
(FQN class KH) → KC consumer `ClassNotFoundException` → container REJECT + DROP message
TRƯỚC KHI listener raw-Message chạy (Spring AMQP 3.x `extractPayload` vẫn chạy converter —
precedent GAP-925 raw-Message KHÔNG đủ). Fix (wave/branding-100-g1-fixes): KC `RabbitConfig`
idClassMapping FQN KH → record local + regression test `RabbitConfigWireFormatTest` tái lập
đúng message incident (PASS). Bucket C tests cũ pass vì 2 chiều dùng cùng class trong test —
lesson: cross-service AMQP cần wire-format test với header producer thật. Chờ re-walk G1 verify
landing đổi thật.
