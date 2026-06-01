---
id: GAP-825
title: Tenant-isolation hardening — JWT-sig-verify in TenantResolver fallback + core network-isolation + OWASP A01 regression
status: OPEN
priority: P1
phase: phase-1-beta
domain: Mixed
created: 2026-06-01
last_updated: 2026-06-01
---

# GAP-825 — Tenant-isolation hardening (follow-up of GAP-814)

> Follow-up gap cho 2 AC items của GAP-814 nằm ngoài scope Bucket A code-layer fix (gateway header-strip + JWT-verified re-inject đã shipped PR #1991). Đây là 3 lớp phòng vệ bổ sung (defense-in-depth) cho cross-tenant isolation.

## Problem

GAP-814 đóng P0 cross-tenant IDOR qua gateway strip-then-set pipeline (`RemoveRequestHeader=X-Tenant-Id/X-User-Id` default-filter + `TenantHeaderGuardFilter` re-inject từ verified JWT). Route coverage audit (`documents/04-quality/audits/security/2026-06-01-gap-814-gateway-route-tenant-coverage-audit.md`) xác nhận mọi route tenant-scoped có TenantResolver / path-UUID / whitelist-public.

CÒN 3 lớp phòng vệ chưa hoàn thiện (defense-in-depth, không phải P0 vì primary strip+verify pipeline đã chặn IDOR):

1. **JWT-claim fallback signature verify trong `TenantResolverGatewayFilterFactory.extractJwtTenantClaim`** (GAP-711). Fallback này đọc claim `tenantId` qua base64 decode **KHÔNG verify chữ ký**. Phòng vệ hiện tại dựa vào filter order: `JwtAuthenticationGatewayFilter` (-100) verify sig trước → short-circuit token sai. NHƯNG nếu filter order drift hoặc route bypass JwtAuthenticationGatewayFilter → fallback có thể trust claim chưa verify. Nên harden TenantResolver tự verify sig (HS512 access key) thay vì rely vào filter order.

2. **Core network-isolation** — `kiteclass-core` + `kitehub-subscription` KHÔNG được reachable trực tiếp từ ngoài gateway. Nếu container expose port ra ngoài docker network → client tự gọi core với header `X-Tenant-Id` giả (bypass gateway strip hoàn toàn). Cần firewall / AWS security group ngăn direct access (chỉ gateway reach core qua internal network) HOẶC shared-secret header gateway↔core (`X-Gateway-Auth`).

3. **OWASP A01 regression test** — thêm cross-tenant IDOR attempt (spoofed header trên route không-TenantResolver) vào security audit suite (`pre-launch-owasp-rest-hardening-checklist.md` §2.1 A01).

## Root Cause

- TenantResolver JWT fallback (GAP-711) prioritized convenience (best-effort claim read) over independent sig verify — relies on upstream filter ordering.
- Network-isolation = infrastructure-layer concern, không có trong gateway code scope; AWS-gated (GAP-612 account suspension blocks live verify).
- A01 regression test absent — security audit suite chưa có cross-tenant spoofing scenario.

## Proposed Fix

1. **TenantResolver fallback sig verify:** thay `extractJwtTenantClaim` base64-decode bằng `Jwts.parser().verifyWith(accessSigningKey)...parseSignedClaims` (mirror `TenantHeaderGuardFilter`). Inject `JWT_SECRET` vào `TenantResolverGatewayFilterFactory`. On verify fail → return null (no tenant resolve) thay vì trust unverified claim.
2. **Core network-isolation (AWS-gated):** terraform-aws security group cho EC2 chỉ allow gateway → core trên internal port; deny 0.0.0.0/0 trực tiếp core. Verify qua `aws ec2 describe-security-groups` post-restore. Defer cho đến khi GAP-612 (AWS account) restore.
3. **OWASP A01 regression:** thêm IT test trong gateway `@SpringBootTest` — POST tới route không-TenantResolver với spoofed `X-Tenant-Id: <other-tenant>` → assert forwarded request KHÔNG có spoofed value (stripped) OR có JWT-derived value. Wire vào security audit suite checklist.

## Acceptance Criteria

- [ ] `TenantResolverGatewayFilterFactory.extractJwtTenantClaim` verify JWT signature (HS512 access key) trước khi trust `tenantId` claim — independent của filter order
- [ ] Core network-isolation: terraform-aws security group ngăn direct access bypass gateway (chỉ gateway reach core internal); live verify post-AWS-restore (GAP-612)
- [ ] OWASP A01 cross-tenant IDOR regression test thêm vào gateway IT suite + security audit checklist
- [ ] Gateway module test PASS (`./mvnw -pl kitehub-gateway test -P strict-warnings`)

## Related

- `GAP-814` — parent P0 (gateway strip + JWT-verified re-inject; PARTIAL → satisfied AC ticked Wave beta-readiness-9 Bucket A)
- `GAP-711` — JWT tenantId claim fallback (source of #1 unverified-read concern)
- `GAP-612` — AWS account suspension (blocks #2 live network-isolation verify)
- `documents/04-quality/audits/security/2026-06-01-gap-814-gateway-route-tenant-coverage-audit.md` — route coverage audit
- `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 A01 Cross-Tenant Isolation
- ADR-023 — shared-DB + RLS canonical (defense-in-depth layer)

## Log

- **2026-06-01 (Wave beta-readiness-9 Bucket A — OPEN):** Filed as follow-up of GAP-814 for 2 deferred AC items (network-isolation + OWASP A01 regression) + 1 hardening item surfaced during route-coverage audit (TenantResolver JWT-claim fallback unverified-read). Bucket A scope was gateway header-strip + route-audit; these 3 defense-in-depth layers tracked separately. #1 + #3 AWS-free (can ship next wave); #2 AWS-gated (GAP-612 unblock required for live verify).
