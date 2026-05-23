---
id: GAP-724
title: kc-frontend auth endpoint paths mismatched with gateway (404 + double /api/v1)
status: OPEN
priority: P1
domain: Frontend
phase: phase-1-beta
audience: dev
found: 2026-05-23
last_verified: 2026-05-23
completion_pct: 0
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

- [ ] KC login form submit `admin.test@test.vn / Test@1234` returns HTTP 200 + valid JWT
- [ ] KC dashboard routes accessible post-login
- [ ] No double `/api/v1/api/v1` URLs anywhere in KC frontend network panel
- [ ] All KC API call sites grep clean (no `/api/v1/` literal paths if Option A chosen)
- [ ] Playwright e2e `_rst-kc.spec.ts` passes login flow

## Related

- RST UI walk 2026-05-23 session — first surface
- `kitehub-gateway/src/main/resources/application.yml` — canonical route source
- Wave 104 GAP-705/706 — gateway HS256 challenge token support (different scope, same gateway file)
- `no-vercel-references.md` §4 — KC frontend FE hosting decision (related infra)

## Log

- **2026-05-23**: GAP filed via Wave 105 RST UI deep walk. Goal "RST full UI" surfaced this — KC frontend login form renders OK but submit hits 404 due to path mismatch + double prefix. Multi-file Option A fix deferred to next session per context budget; gap captures full diagnosis for fast pickup.
