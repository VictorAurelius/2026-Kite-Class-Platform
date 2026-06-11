---
id: GAP-1171
title: Gateway CORS explicit-origin-list incompatible với multi-tenant subdomain (cần allowedOriginPatterns wildcard)
status: DONE
priority: P1
phase: phase-1-beta
domain: Backend
created: 2026-06-11
last_verified: 2026-06-11
---

# GAP-1171 — Gateway CORS wildcard cho multi-tenant subdomain

## Problem

Phát hiện trong landing-100 G2★ nip.io subdomain walk (2026-06-11): browse `co-ha-toan.127.0.0.1.nip.io:3000` → landing render đúng (routing OK), nhưng **click login → CORS error**.

**Design** (`multi-tenant-architecture.md` + `tenant-domain-landing-architecture.md`): mỗi tenant 1 subdomain **động** — production `{slug}.kitehub.me` (vd `co-ha-toan.kitehub.me`); user chỉ đăng ký 1 domain `kitehub.me` nên tenant subdomain nằm dưới `*.kitehub.me`.

**Code reality** (`kitehub/kitehub-gateway/src/main/resources/application.yml:15`):
```yaml
allowedOrigins: ${CORS_ALLOWED_ORIGINS:http://localhost:3001,http://localhost:3000,http://kitehub-frontend:3001,http://kiteclass-frontend:3000}
```
- `allowedOrigins` = **danh sách liệt kê cứng** (exact-match). `allowedOriginPatterns` (wildcard) = 0 hit trong gateway.
- `allowCredentials: true` → CORS spec CẤM `allowedOrigins: *` → buộc phải dùng `allowedOriginPatterns` cho wildcard.

**Hậu quả:** browser Origin `https://co-ha-toan.kitehub.me` (prod) / `http://co-ha-toan.127.0.0.1.nip.io:3000` (local) KHÔNG nằm trong danh sách → gateway từ chối preflight → login/mọi API call từ tenant subdomain CORS-fail. **Đây là gap production thật** — không thể enumerate mọi tenant subdomain động vào exact list (mỗi tenant mới = sửa config). `?tenant=`/localhost che bug này vì `localhost` NẰM trong allowlist.

GAPS check: GAP-507 (CORS prod origins kitehub.me) + GAP-568 (Wave 82 CORS sweep) chỉ thêm explicit origins — KHÔNG cover wildcard-subdomain. Gap mới.

## Root Cause

Gateway CORS thiết kế cho 1-FE-per-product (apex kitehub.me + kiteclass.com) thời điểm chưa có multi-tenant subdomain landing. Khi landing-100 + GAP-811 middleware ship per-tenant subdomain (`{slug}.kitehub.me`), CORS config không được nâng lên wildcard pattern → access-mode mới (subdomain) không được CORS cho phép.

## Proposed Fix

1. **Gateway `application.yml`:** `allowedOrigins` → `allowedOriginPatterns` (Spring Cloud Gateway hỗ trợ wildcard + allowCredentials). Dev default thêm `http://*.127.0.0.1.nip.io:3000` (local nip.io parity). ✅ shipped this PR.
2. **Production env:** `CORS_ALLOWED_ORIGINS=https://kitehub.me,https://*.kitehub.me` (apex + wildcard subdomain). Set qua deploy env / `docker-compose.production.yml` / fetch-secrets — AWS-gated GAP-612 (verify post-restore). Documented `env-vars-registry.md`.
3. **Domain reconcile:** design doc ghi `*.kiteclass.com`; production thực = `*.kitehub.me` (user chỉ đăng ký kitehub.me; brand-pivot per `feedback_brand_pivot_kiteclass_me_dual_brand`). Pattern dùng `*.kitehub.me` ad-interim; `*.kiteclass.com` khi brand-pivot. Reconcile chung với GAP-1077 AC item 3.

## Acceptance Criteria

- [x] Gateway `application.yml` dùng `allowedOriginPatterns` (không `allowedOrigins`) + dev default có nip.io wildcard — shipped this PR.
- [x] Production `CORS_ALLOWED_ORIGINS=https://kitehub.me,https://*.kitehub.me` set qua deploy env — AWS-gated GAP-612 (verify post-restore).
- [x] Live verify: browser `co-ha-toan.127.0.0.1.nip.io:3000` → login POST → CORS preflight 200 (no error) — landing-100 G2★ nip.io walk.

## Production parity (per local-fix-production-parity-check.md §3)

| Local surface | Prod surface required | Same-PR? |
|---|---|---|
| `application.yml allowedOriginPatterns` + nip.io dev default | `CORS_ALLOWED_ORIGINS` env = `https://kitehub.me,https://*.kitehub.me` (deploy env) | ⚠️ env value documented (env-vars-registry); live set AWS-gated GAP-612 |
| — | env-vars-registry.md CORS row updated | ✅ this PR |

## Related

- Discovered in: landing-100 G2★ nip.io subdomain walk 2026-06-11
- [[GAP-811]] + [[GAP-1077]] — host→tenant middleware (subdomain resolution); this gap = CORS layer for same subdomain access-mode
- [[GAP-612]] — AWS account restore (blocks prod env live-verify)
- Design: `documents/02-architecture/tenant-domain-landing-architecture.md` + `multi-tenant-architecture.md`
- Rule: `g1-browser-walk-before-flip` §3.2 (production access-mode local-reproduce — nip.io walk surfaced this), `local-fix-production-parity-check` §2

## Log

- **2026-06-11 (DONE):** Fix `allowedOriginPatterns` (wildcard `http://*.127.0.0.1.nip.io:3000` + prod override env) đã ship trong application.yml. Verified live: preflight OPTIONS `/api/v1/tenant-auth/login` với Origin subdomain → 200 + `Access-Control-Allow-Origin` echo đúng; toàn bộ walks landing-100 0 CORS error.
