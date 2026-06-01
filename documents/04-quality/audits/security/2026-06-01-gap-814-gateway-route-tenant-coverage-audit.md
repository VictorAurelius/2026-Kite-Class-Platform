---
audience: mixed
audit_id: AUDIT-2026-06-01-gap-814-gateway-route-tenant-coverage
category: security
phase: phase-1-beta
wave: beta-readiness-9-bucket-a
date: 2026-06-01
status: complete
---

# GAP-814 — Gateway route tenant-coverage audit (Wave beta-readiness-9 Bucket A)

> Audit kiểm tra mọi route gateway → xác nhận route tenant-scoped đều có 1 trong 3 cơ chế bảo vệ tenant: (A) `TenantResolver` filter, (B) path-UUID server-side resolve, hoặc (C) whitelisted-public (không cần tenant). Đây là AC #5 của GAP-814 (route coverage audit).

## Bối cảnh

GAP-814 P0 fix đã shipped main qua PR #1991 (Wave tenant-domain-1 Bucket A):
- `application.yml` `default-filters` strip `X-Tenant-Id` + `X-User-Id` client-sent (lines 696-698) — áp dụng MỌI route.
- `TenantHeaderGuardFilter` (GlobalFilter order -99) re-inject `X-Tenant-Id` từ verified JWT `tenantId` claim (HS512 access key, defense-in-depth no challenge key).
- 11 unit tests pass (`TenantHeaderGuardFilterTest`).

Strip-then-set pipeline đảm bảo client KHÔNG thể spoof `X-Tenant-Id`/`X-User-Id` trên BẤT KỲ route nào — kể cả route không có `TenantResolver` (public/by-token). Audit này verify route coverage để chắc không còn route tenant-scoped nào trust header client-sent.

## Strip-then-set pipeline (global)

```
Request → default-filter RemoveRequestHeader=X-Tenant-Id (strip client value)
        → default-filter RemoveRequestHeader=X-User-Id     (strip client value)
        → JwtAuthenticationGatewayFilter (order -100) verify JWT sig + set X-User-Id/X-User-Roles/X-User-Email
        → TenantHeaderGuardFilter        (order -99)  re-inject X-Tenant-Id từ verified JWT claim
        → Route filters (TenantResolver per-route có thể override X-Tenant-Id từ Host subdomain)
        → Downstream service trust X-Tenant-Id là gateway-resolved
```

Precedence: TenantResolver per-route override → guard JWT-claim fallback → stripped null trên public routes.

## Route coverage table

Phân loại mọi route trong `kitehub/kitehub-gateway/src/main/resources/application.yml`:

| Route id | Path | Đích | Phân loại | Cơ chế tenant | Verdict |
|---|---|---|---|---|---|
| auth-register/login/refresh/verify-email/resend-verification/password-reset-request/auth | `/api/auth/**` | subscription | Public auth | C — whitelist (`isPublicPath`) | ✅ |
| platform-instances | `/api/platform/instances/**` | subscription | Platform-admin scope | Header stripped; admin JWT no tenant claim | ✅ — platform-scope không tenant-scoped |
| kitehub-instance-domain-verify | `/api/instances/**` | subscription | Platform-admin scope | Header stripped; admin auth | ✅ |
| platform-config | `/api/platform/config/**` | subscription | Public config | C — no tenant needed | ✅ |
| platform-subscription/payment | `/api/platform/{subscriptions,payments}/**` | subscription | Platform-admin scope | Header stripped; admin auth | ✅ |
| platform-branding | `/api/platform/branding/**` | branding | Platform-scope (AI) | Header stripped; tenant-keyed rate-limit only | ✅ — platform admin path |
| platform-admin-emails/instances-force-convert/rollback/admin | `/api/platform/admin/**` | subscription/admin | Platform-admin scope | Header stripped; PLATFORM_ADMIN JWT no tenant claim | ✅ |
| platform-email | `/api/platform/emails/**` | email | Platform-scope | Header stripped | ✅ |
| {subscription,branding,admin,email}-docs | `/docs/**` | per-service | Public docs | C — whitelist (`isPublicPath`) | ✅ |
| kitehub-auth-v1-request-beta-access | `/api/v1/auth/request-beta-access` | subscription | Public beta signup | C — whitelist | ✅ |
| kitehub-auth-v1-2fa-* + auth-2fa-*-alias | `/api/v1/auth/2fa/**`, `/api/auth/2fa/**` | subscription | Public auth (2FA) | C — whitelist (`/api/v1/auth/`, `/api/auth/`) | ✅ |
| kitehub-feedback-v1 | `/api/v1/feedback` | subscription | Public feedback | C — IP-keyed rate-limit; no tenant | ✅ |
| kitehub-auth-v1 | `/api/v1/auth/**` | subscription | Public auth | C — whitelist | ✅ |
| kitehub-admin-beta-requests-v1 | `/api/v1/admin/beta-requests/**` | subscription | Platform-admin | Header stripped; PLATFORM_ADMIN no tenant claim | ✅ |
| kitehub-admin-impersonate | `/api/v1/admin/impersonate/**` | subscription | Platform-admin | Header stripped; admin auth | ✅ |
| kitehub-admin-v1 | `/api/v1/admin/**` | admin | Platform-admin | Header stripped; admin auth | ✅ |
| kitehub-consent-v1 | `/api/v1/consent/**` | subscription | Per-user (DSAR/consent) | Header stripped; guard JWT-claim inject if present | ✅ — per-user scope, not tenant-data |
| kitehub-dsar-v1 | `/api/v1/dsar/**` | subscription | Per-user (DSAR) | Header stripped; per-user scope | ✅ |
| kitehub-notification-preferences-v1 | `/api/v1/notification-preferences/**` | subscription | Per-user | Header stripped; guard inject | ✅ |
| kitehub-branding-v1 | `/api/v1/branding/**` | branding | Tenant-scoped | Header stripped; guard JWT-claim inject | ✅ — guard re-inject from verified JWT |
| kitehub-beta-status | `/api/v1/beta-status` | subscription | Public | C — no tenant | ✅ |
| staff-invitations-public-token | `/api/v1/staff-invitations/by-token/**`, `/api/v1/staff-invitations/*/accept` | subscription | Public by-token | C — whitelist (`isPublicPath`); tenant derived server-side from token row | ✅ |
| staff-invitations | `/api/v1/staff-invitations/**` | subscription | Tenant-scoped (Owner) | A — `TenantResolver` filter | ✅ |
| kitehub-onboarding-progress | `/api/v1/onboarding-progress/**`, `/api/v1/onboarding-progress` | subscription | Tenant-scoped (Owner) | A — `TenantResolver` filter | ✅ |
| public-tenant-landing | `/api/v1/tenants/*/landing` | kiteclass-core | Public landing | B — path-UUID; whitelist (`isPublicTenantLandingPath`) | ✅ — tenant from path UUID |
| public-tenant-resolve | `/api/v1/public/tenants/**` | subscription | Public resolve | C — IP-keyed rate-limit; subdomain in path | ✅ |
| instance-apis | `/api/v1/**` (catch-all) | kiteclass-core | Tenant-scoped | A — `TenantResolver` filter | ✅ |

## Findings

**Coverage verdict: ✅ PASS** — mọi route đều có 1 trong 3 cơ chế bảo vệ tenant:

- **Cơ chế A (TenantResolver):** `staff-invitations`, `kitehub-onboarding-progress`, `instance-apis` (catch-all) — 3 route tenant-scoped resolve tenant từ Host subdomain HOẶC JWT tenantId claim fallback (GAP-711).
- **Cơ chế B (path-UUID):** `public-tenant-landing` — tenant resolve từ UUID trong path (`/api/v1/tenants/{id}/landing`), `LandingPageController` đọc path UUID.
- **Cơ chế C (whitelisted-public):** mọi `/api/auth/**`, `/api/v1/auth/**`, by-token paths, docs, beta-status, feedback, public-tenant-resolve — không cần tenant context; header stripped global + không inject vì path public.
- **Platform-admin routes:** `/api/platform/**`, `/api/v1/admin/**` — không tenant-scoped (PLATFORM_ADMIN JWT không có tenantId claim → guard không inject; xác nhận bởi test `jwtWithoutTenantClaim_noInject`).
- **Per-user routes:** `/api/v1/consent/**`, `/api/v1/dsar/**`, `/api/v1/notification-preferences/**` — per-user scope (DSAR/consent của chính user), guard inject tenant nếu JWT có claim; downstream scope theo user.

**Defense-in-depth chính:** global `RemoveRequestHeader` strip đảm bảo NGAY CẢ route catch-all `/api/v1/**` HOẶC route public không-có-TenantResolver đều KHÔNG passthrough header client-sent. Đây là điểm khác biệt cốt lõi so với pre-GAP-814 (strip chỉ áp per-route trong TenantResolver).

## Out-of-Bucket-A scope (deferred → GAP-825)

2 AC items remain unchecked, out-of-scope cho code-layer fix Bucket A:

1. **Core network-isolation** — kiteclass-core + kitehub-subscription KHÔNG reachable trực tiếp từ ngoài gateway (firewall / security group). Đây là infrastructure-layer concern (terraform-aws security groups), không phải gateway code. AWS-gated (GAP-612 account suspension blocks live verify).
2. **JWT-claim fallback signature verify trong `TenantResolverGatewayFilterFactory.extractJwtTenantClaim`** — fallback này (GAP-711) đọc claim `tenantId` qua base64 decode KHÔNG verify chữ ký (best-effort). Phòng vệ hiện tại: `JwtAuthenticationGatewayFilter` (order -100) là canonical signature gate đã short-circuit token sai TRƯỚC khi reach fallback; `TenantHeaderGuardFilter` (order -99) verify sig độc lập cho header inject. NHƯNG fallback trong TenantResolver vẫn read-unverified — defense-in-depth nên harden để TenantResolver tự verify thay vì rely vào filter order. Track GAP-825.
3. **OWASP A01 regression test** thêm vào security audit suite — track GAP-825.

## References

- `kitehub/kitehub-gateway/src/main/resources/application.yml` — routes + default-filters
- `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/TenantHeaderGuardFilter.java` — GAP-814 guard
- `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/TenantResolverGatewayFilterFactory.java` — per-route resolver + JWT-claim fallback
- `documents/04-quality/gaps/phase-1-beta/GAP-814-tenant-header-spoofing-gateway-strip.md`
- GAP-825 (follow-up — JWT-sig-verify in TenantResolver fallback + core network-isolation + OWASP A01 regression)
- `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 A01 Cross-Tenant Isolation
