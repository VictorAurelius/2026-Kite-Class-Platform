---
id: GAP-814
title: Host-spoofing X-Tenant-Id — gateway chưa strip client-sent tenant header (cross-tenant IDOR)
status: OPEN
priority: P0
phase: phase-1-beta
domain: Mixed
created: 2026-05-29
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

- [ ] Gateway strip `X-Tenant-Id`/`X-User-Id` client-sent trên MỌI route (test: gửi header giả → core nhận giá trị gateway-resolved, không phải client)
- [ ] Core không reachable trực tiếp từ ngoài (chỉ qua gateway) — network/firewall verify
- [ ] JWT-claim fallback verify signature (hoặc loại bỏ, dùng path-UUID)
- [ ] Security test: cross-tenant IDOR attempt (spoofed header) → 403/resolved-correct, KHÔNG leak tenant khác
- [ ] OWASP A01 (Broken Access Control) regression test thêm vào security audit suite

## Related

- `GAP-810/811/812/813` — initiative tenant→domain→landing (gap này surfaced từ failure-mode audit của initiative)
- `GAP-711` — JWT tenantId claim fallback (nguồn của #3)
- ADR-023 — shared-DB + RLS canonical (RLS là defense-in-depth NHƯNG TenantContext sai = RLS scope sai)
- `audit-skill-rubric-security-audit.md` — A01 Cross-Tenant Isolation

## Log

- **2026-05-29:** Gap created từ outside-in failure-mode matrix audit (3-agent, initiative tenant→domain→landing). P0 security — gateway `default-filters` thiếu `RemoveRequestHeader` identity headers → client-spoofed `X-Tenant-Id` passthrough trên route không-TenantResolver → cross-tenant IDOR. Rộng hơn landing scope; ưu tiên cao nhất trong initiative.
