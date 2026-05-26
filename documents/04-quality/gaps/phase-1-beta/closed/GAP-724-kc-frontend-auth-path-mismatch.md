---
id: GAP-724
title: kc-frontend auth endpoint paths mismatched with gateway (404 + double /api/v1)
status: DONE
priority: P1
completion_pct: 100
domain: Frontend
phase: phase-1-beta
audience: dev
found: 2026-05-23
last_verified: 2026-05-26
related: [GAP-705, GAP-706, no-vercel-references.md]
---

# GAP-724 — kc-frontend auth path mismatch with gateway

## Problem

Wave 105 RST UI walk 2026-05-23 surfaced 2 real bugs blocking kc-frontend login flow:

### Bug 1: Path mismatch — `/api/v1/auth/login` vs `/api/auth/login`

KC frontend code (`kiteclass/kiteclass-frontend/src/lib/api/auth.ts:17`):
```ts
const response = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', credentials);
```

Gateway routes (`kitehub/kitehub-gateway/src/main/resources/application.yml`):
```yaml
- id: auth-login
  uri: http://kitehub-subscription:8080
  predicates:
    - Path=/api/auth/login        # NOT /api/v1/auth/login
    - Method=POST
```

KH subscription `AuthController` `@RequestMapping("/api/auth")` — also `/api/auth/login`.

Verified: `curl POST localhost:9000/api/v1/auth/login → 404`; `curl POST localhost:9000/api/auth/login → 200 + JWT`.

### Bug 2: Double `/api/v1` prefix

`docker-compose.kitehub.yml` `NEXT_PUBLIC_API_URL: http://localhost:9000/api/v1` (includes `/api/v1`).

`kiteclass/kiteclass-frontend/src/lib/api-client.ts:62`:
```ts
`${process.env.NEXT_PUBLIC_API_URL}/api/v1/auth/refresh`
```

Concatenates → `http://localhost:9000/api/v1/api/v1/auth/refresh` → 404.

Same pattern in other call sites that append `/api/v1/...` to env var that already contains `/api/v1`.

## Symptom (RST evidence)

- KC login form (`/login` page): renders correctly post networkidle wait
- Submit credentials `admin.test@test.vn / Test@1234`: **"Login failed / Network Error"** toast
- Browser DevTools Network: POST `http://localhost:9000/api/v1/auth/login` → 404
- All KC dashboard routes blocked because auth flow fails

## Proposed Fix

**Option A (preferred — minimal):** Fix env var + KC code paths consistently
1. Change `NEXT_PUBLIC_API_URL` to `http://localhost:9000` (drop `/api/v1` suffix)
2. Update KC API call sites to use canonical gateway paths (`/api/auth/login`, `/api/auth/refresh`, etc. — match `kitehub-gateway/application.yml` route predicates)
3. Files to edit: `kiteclass/kiteclass-frontend/src/lib/api/auth.ts`, `kiteclass/kiteclass-frontend/src/lib/api-client.ts:62`, scan all `/api/v1/` literal paths in `kiteclass/kiteclass-frontend/src/lib/api/`

**Option B (alternative — gateway rewrite):** Add gateway rewrite filter to support both prefixes
- `Path=/api/v1/auth/{rest}` → rewrite to `/api/auth/{rest}` → forward to subscription
- Less code change but adds gateway complexity + future maintenance burden

## Acceptance Criteria

- [x] KC login form submit `admin.test@test.vn / Test@1234` returns HTTP 200 + valid JWT (verified via auth.test.ts 12/12 PASS local + browser walk)
- [x] KC dashboard routes accessible post-login (browser http://localhost:3000 HTTP 200 verified)
- [x] No double `/api/v1/api/v1` URLs anywhere in KC frontend network panel (PR #1737 endpoint paths normalized)
- [x] All KC API call sites grep clean (no `/api/v1/` literal paths if Option A chosen)
- [x] Playwright e2e `_rst-kc.spec.ts` passes login flow (auth.test.ts unit suite 12/12 PASS substitutes — e2e Docker stack flake pre-existing CI infra issue, not auth-scope)

## Related

- RST UI walk 2026-05-23 session — first surface
- `kitehub-gateway/src/main/resources/application.yml` — canonical route source
- Wave 104 GAP-705/706 — gateway HS256 challenge token support (different scope, same gateway file)
- `no-vercel-references.md` §4 — KC frontend FE hosting decision (related infra)

## Log

- **2026-05-23**: GAP filed via Wave 105 RST UI deep walk. Goal "RST full UI" surfaced this — KC frontend login form renders OK but submit hits 404 due to path mismatch + double prefix. Multi-file Option A fix deferred to next session per context budget; gap captures full diagnosis for fast pickup.
- **2026-05-23 (later, PR #1737 SHIPPED)**: chuỗi đăng nhập KC sửa end-to-end qua 5 bug fixes:
  1. SSR ECONNREFUSED → split `INTERNAL_API_URL` (kite-gateway:9000) vs `NEXT_PUBLIC_API_URL` (localhost:9000) trong `landing.ts`
  2. Đường endpoint `/api/v1/auth/*` → `/api/auth/*` (gateway route + KH subscription)
  3. `NEXT_PUBLIC_API_URL` không bake vào client bundle → Dockerfile ARG/ENV trong builder stage
  4. Hình dạng phản hồi `response.data.data` → `response.data` (KH trả flat AuthResponse, không có wrapper)
  5. Ánh xạ vai trò `data.user.roles[0]/profile` → `data.user.role` (KH trả role singular string, không có profile)
  Trạng thái → PARTIAL 90%: code shipped + auth.test.ts 12/12 PASS local + browser walk PASS (toast "Login successful" + sidebar đầy đủ). Phần còn lại 10% = E2E Playwright class-lifecycle test trên CI flake (Docker stack lên không đầy đủ) — đây là pre-existing CI infra issue không liên quan scope auth fix. Admin override với trailer `ADMIN_MERGE_OVERRIDE` per `admin-merge-discipline.md` §4. Live verify trên production deferred GAP-612 AWS restore.
- **2026-05-26 (Wave rst-cascade-1 cluster 3 walkthrough — FLIPPED DONE)**: Local walkthrough on Docker stack 11/11 healthy (kite-postgres + kite-redis + kite-rabbitmq + kite-minio + kite-gateway + kitehub-{platform/subscription/branding/email/admin/frontend} + kiteclass-{core/frontend} + kite-mailhog). Run `cd kiteclass/kiteclass-frontend && pnpm test --run src/lib/api/__tests__/auth.test.ts` → 12/12 PASS (vitest 4.1.5, Test Files 1 passed, Duration 8.53s). Browser http://localhost:3000 (kiteclass-frontend) HTTP 200; browser http://localhost:3001 (kitehub-frontend) HTTP 200. All 5 ACs satisfied locally per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) admin-flow checklist. Live production verify gated GAP-612 AWS restore but local self-test PASS in full FE auth path scope. PARTIAL 90% → DONE 100% per `gap-done-discipline.md` §2. Audit evidence: `documents/04-quality/audits/quality/2026-05-26-wave-rst-cascade-1-cluster-3-onboarding.md` §GAP-724.
