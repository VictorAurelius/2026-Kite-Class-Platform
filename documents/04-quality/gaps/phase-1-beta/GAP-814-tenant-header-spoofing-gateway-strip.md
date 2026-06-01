---
id: GAP-814
title: Host-spoofing X-Tenant-Id — gateway chưa strip client-sent tenant header (cross-tenant IDOR)
status: PARTIAL
priority: P0
phase: phase-1-beta
domain: Mixed
created: 2026-05-29
last_updated: 2026-06-01
---

# GAP-814 — Host-spoofing X-Tenant-Id (cross-tenant IDOR risk)

> Surfaced bởi outside-in failure-mode matrix audit 2026-05-29 (initiative tenant→domain→landing, GAP-810/811/812/813). Đây là vuln tenant-isolation **rộng hơn scope landing** — ảnh hưởng MỌI request tenant-scoped.

## Problem

Core (`kiteclass-core`) tin tưởng header `X-Tenant-Id` (+ `X-User-Id`) **thẳng** từ request (`TenantFilterInterceptor`) để xác định tenant context. Gateway `kite-gateway` được kỳ vọng là nguồn DUY NHẤT set header này (sau khi resolve Host→tenant). NHƯNG:

1. Gateway `default-filters` chỉ có `DedupeResponseHeader` — **KHÔNG có `RemoveRequestHeader=X-Tenant-Id, X-User-Id`** → client gửi header giả KHÔNG bị strip.
2. `TenantResolverGatewayFilterFactory` chỉ áp trên một số route (`instance-apis`, `staff-invitations`, `onboarding-progress`). Route **public / by-token** KHÔNG có filter `TenantResolver` → header client gửi **passthrough thẳng** xuống core.
3. JWT-claim fallback (`extractJwtTenantClaim`, GAP-711) đọc claim `tenantId` từ Bearer token **KHÔNG verify chữ ký** (best-effort base64 decode).
4. Nếu core expose trực tiếp (không qua gateway, cùng docker network) → client tự gọi core với header giả.

→ **Cross-tenant IDOR**: client gửi `X-Tenant-Id: <uuid tenant khác>` qua route không-có-TenantResolver → core trust → đọc/ghi data tenant khác.

## Root Cause

- Gateway thiếu global `RemoveRequestHeader` strip-then-set pattern cho identity headers.
- Core trust header inbound mà không phân biệt "từ gateway" vs "từ client".
- JWT-claim fallback không verify signature.

## Proposed Fix

1. **Gateway global strip (P0):** thêm `default-filters: - RemoveRequestHeader=X-Tenant-Id` + `- RemoveRequestHeader=X-User-Id` (chạy TRƯỚC `TenantResolver` set lại). Đảm bảo MỌI route — kể cả public/by-token không có TenantResolver — đều strip header client gửi.
2. **Strip-then-set pattern:** `routeToInstance` strip rồi mới `.mutate().header(...)` set giá trị resolved.
3. **Core trust boundary:** core chỉ nhận identity header từ gateway — network-isolate core (không expose port core ra ngoài gateway; chỉ gateway reach core qua docker network), HOẶC shared-secret header gateway↔core (`X-Gateway-Auth`).
4. **JWT-claim fallback verify:** `extractJwtTenantClaim` PHẢI verify signature (HMAC/JWKS) trước khi trust claim, HOẶC chỉ dùng cho path-UUID-whitelisted route.
5. **Audit route coverage:** liệt kê mọi route gateway → xác nhận route nào có TenantResolver vs path-UUID vs public; route tenant-scoped phải có 1 trong 2.

## Acceptance Criteria

- [x] Gateway strip `X-Tenant-Id`/`X-User-Id` client-sent trên MỌI route (Wave tenant-domain-1 Bucket A: `application.yml` `default-filters` extended với `RemoveRequestHeader=X-Tenant-Id` + `RemoveRequestHeader=X-User-Id`)
- [x] JWT-claim fallback verify signature qua `TenantHeaderGuardFilter` (order -99) — re-parses JWT với HS512 access key only (NOT HS256 challenge key per defense-in-depth), inject `X-Tenant-Id` từ verified `tenantId` claim
- [x] Security test: cross-tenant IDOR attempt (spoofed header) → JWT-derived value wins (11 unit tests pass — `TenantHeaderGuardFilterTest`, 5 mandatory scenarios + 6 defense-in-depth: client-spoofed strip + JWT valid inject + spoof-defeat + public bypass + no-JWT + admin no-claim + malformed + expired + public-paths-list + constructor fail-fast + filter order)
- [x] Audit route coverage (Proposed Fix #5): liệt kê mọi route gateway → xác nhận route nào có TenantResolver vs path-UUID vs public (Wave beta-readiness-9 Bucket A: `documents/04-quality/audits/security/2026-06-01-gap-814-gateway-route-tenant-coverage-audit.md` — 27 routes audited, 100% covered: 3 TenantResolver + 1 path-UUID + rest whitelist-public/platform-admin/per-user; global strip covers catch-all `/api/v1/**`)
- [ ] Core không reachable trực tiếp từ ngoài (chỉ qua gateway) — network/firewall verify → **out of Bucket A scope** (network/infrastructure layer; tracked GAP-825 — AWS-gated GAP-612)
- [ ] OWASP A01 (Broken Access Control) regression test thêm vào security audit suite → **out of Bucket A scope** (audit suite update tracked GAP-825)
- [ ] JWT-claim fallback trong `TenantResolverGatewayFilterFactory.extractJwtTenantClaim` verify signature (defense-in-depth, surfaced during route audit) → tracked GAP-825

## Related

- `GAP-810/811/812/813` — initiative tenant→domain→landing (gap này surfaced từ failure-mode audit của initiative)
- `GAP-711` — JWT tenantId claim fallback (nguồn của #3)
- ADR-023 — shared-DB + RLS canonical (RLS là defense-in-depth NHƯNG TenantContext sai = RLS scope sai)
- `audit-skill-rubric-security-audit.md` — A01 Cross-Tenant Isolation

## Log

- **2026-06-01 (Wave beta-readiness-9 Bucket A — PARTIAL, route-audit AC ticked):** State-check (per `audit-to-gap-pipeline.md` §2.8) confirmed GAP-814 P0 code-layer fix already shipped main via PR #1991 (default-filters strip lines 696-698 + `TenantHeaderGuardFilter` order -99 + 11 unit tests). Bucket A scope (header-strip + route-audit, deferring JWT-sig-verify + network-isolation) overlapped already-shipped strip+verify. Net deliverable this bucket = **route coverage audit (Proposed Fix #5)** + AC reconciliation + follow-up gap. Verified gateway module test green: `./mvnw -pl kitehub-gateway test -P strict-warnings` → BUILD SUCCESS, 72/72 (incl. 11/11 `TenantHeaderGuardFilterTest`). Route audit `documents/04-quality/audits/security/2026-06-01-gap-814-gateway-route-tenant-coverage-audit.md`: 27 routes, 100% tenant-coverage (3 TenantResolver `staff-invitations`/`onboarding-progress`/`instance-apis` catch-all + 1 path-UUID `public-tenant-landing` + rest whitelist-public/platform-admin/per-user); global `RemoveRequestHeader` strip covers `/api/v1/**` catch-all — primary defense vs IDOR. Filed GAP-825 (P1) for 3 deferred defense-in-depth layers: (1) JWT-sig-verify in TenantResolver `extractJwtTenantClaim` fallback (surfaced during audit — currently unverified base64 read relying on filter order), (2) core network-isolation (AWS-gated GAP-612), (3) OWASP A01 regression test. Status stays PARTIAL — 3 AC remain unchecked (all → GAP-825), no live verify (AWS GAP-612). `FEATURE_SHIP_WALK_DEFER: GAP-814 — live IDOR verify gated GAP-612 (AWS account suspended); 11 unit tests + route audit cover code-layer; live walk in follow-up post-AWS-restore`.

- **2026-06-01 (Wave tenant-domain-1 Bucket A — PARTIAL):** Core P0 security fix shipped. Gateway-side strip + JWT-verified re-inject pipeline now blocks cross-tenant IDOR via client-spoofed `X-Tenant-Id` header on all routes (including public/by-token paths không có TenantResolver). Changes:
  - `kitehub/kitehub-gateway/src/main/resources/application.yml` — `spring.cloud.gateway.default-filters` extended với `RemoveRequestHeader=X-Tenant-Id` + `RemoveRequestHeader=X-User-Id` (strip client-sent value BEFORE route filters)
  - `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/TenantHeaderGuardFilter.java` — new `GlobalFilter` order=-99 (runs after `JwtAuthenticationGatewayFilter` -100), re-parses JWT với HS512 access key (NOT HS256 challenge key — defense-in-depth), inject `X-Tenant-Id` từ verified `tenantId` claim. Public routes (`/api/auth/**`, `/api/v1/auth/**`, `/api/v1/staff-invitations/by-token/**`, `/api/v1/staff-invitations/*/accept`, `/api/v1/tenants/{id}/landing`, `/actuator/health`, `/docs/**`, `/fallback/**`) bypass — tenant derives từ path/server-side evidence, không từ header.
  - `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/filter/TenantHeaderGuardFilterTest.java` — 11 unit tests (5 mandatory scenarios + 6 defense-in-depth)

  Local verify: `cd kitehub && ./mvnw -pl kitehub-gateway clean verify -P strict-warnings` → BUILD SUCCESS. 72/72 tests pass (gateway module total); 11/11 new tests pass cho TenantHeaderGuardFilter.

  **PARTIAL rationale** per `gap-done-discipline.md` §2-3: Bucket A scope = gateway code-layer fix. 2 AC items remain unchecked vì out-of-Bucket-A-scope:
  - Network-isolate core (firewall/security group ngăn direct access bypass gateway) — infrastructure-layer concern (terraform-aws / security groups), tracked separately
  - OWASP A01 regression test thêm vào security audit suite — audit-skill update, tracked separately

  Follow-up gaps để file separately cho 2 AC remaining khi pick up at infrastructure / audit-skill scopes.

- **2026-05-29:** Gap created từ outside-in failure-mode matrix audit (3-agent, initiative tenant→domain→landing). P0 security — gateway `default-filters` thiếu `RemoveRequestHeader` identity headers → client-spoofed `X-Tenant-Id` passthrough trên route không-TenantResolver → cross-tenant IDOR. Rộng hơn landing scope; ưu tiên cao nhất trong initiative.
