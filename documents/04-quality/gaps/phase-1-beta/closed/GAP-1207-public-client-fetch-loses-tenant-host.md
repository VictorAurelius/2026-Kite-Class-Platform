# GAP-1207: Catalog 400 — public client-side fetch mất tenant Host + gateway local không resolve nip.io

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-11 (user G2★ walk — "không tải được khóa học", GET /api/v1/courses 400)
**Affects:** `kiteclass-frontend/src/lib/api/public.ts` (browser baseURL) + `kitehub/docker-compose.kitehub.yml` (gateway BASE_DOMAIN local)

## Problem

Trang "Khóa học" (catalog) trên landing tenant fetch client-side `GET localhost:9000/api/v1/courses` → 400. Chuỗi: (1) browser dùng `NEXT_PUBLIC_API_URL=localhost:9000` tĩnh → Host=localhost, mất tenant; (2) gateway strip mọi `X-Tenant-Id` client (GAP-814 anti-spoofing, đúng design) nên Host là tín hiệu duy nhất; (3) gateway local `BASE_DOMAIN` mặc định `.kiteclass.com` → không resolve được cả Host nip.io ("Could not resolve tenant" log) → forward không header → core 400 (per GAP-1117 missing-header=400). Verify: curl core trực tiếp + header → 200.

## Fix (shipped PR #2326)

1. `public.ts` browser baseURL: khi hostname có subdomain (production *.kiteclass.com / local nip.io) → gọi gateway trên CÙNG hostname (chỉ lấy port từ NEXT_PUBLIC_API_URL) → Host giữ slug. localhost/IP giữ behavior cũ.
2. Compose gateway env `BASE_DOMAIN: ${BASE_DOMAIN:-.127.0.0.1.nip.io}` — local access-mode parity (production giữ default `.kiteclass.com` qua env riêng, không đổi prod surface per local-fix-production-parity-check: prod value từ application.yml default/terraform env, compose chỉ local).

## Acceptance Criteria

- [x] Catalog `co-ha-toan.127.0.0.1.nip.io:3000/catalog` load 200 qua browser (re-walk evidence PR)
- [x] Gateway log resolve được tenant từ Host nip.io
- [x] localhost dev fallback không regression

## Related

- Sister: GAP-814 (gateway strip — nguyên nhân header path bị chặn, đúng design), GAP-1077/1199 (FE middleware Host), GAP-1117 (missing-header 400)
- Discovered in: user G2★ walk 2026-06-11
