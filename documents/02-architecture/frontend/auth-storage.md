---
title: FE Auth Storage — JWT per-tab isolation via sessionStorage
audience: mixed
status: stable
last_updated: 2026-06-01
related_gaps: [GAP-599]
related_rules: [design-patterns]
---

# Frontend Auth Storage — JWT per-tab isolation

Tài liệu kiến trúc cho cơ chế lưu JWT tokens trong KiteHub frontend. Phục vụ GAP-599 closure (Wave 92 Bucket B PR #1515).

## Vấn đề lịch sử

Trước Wave 92, JWT (access + refresh tokens) lưu trong `localStorage` với key `accessToken` / `refreshToken`. `localStorage` được **share giữa các tab cùng origin** — đây là invariant trình duyệt không thể bypass.

Hậu quả: nếu user mở 2 tab cùng `kitehub.me` (vd tab admin + tab tenant owner), 2 tab dùng cùng JWT → request không bao giờ "thuộc về" đúng actor.

## Giải pháp — sessionStorage facade

Wave 92 Bucket B chuyển JWT sang `sessionStorage`:

- **Per-tab isolation native** — `sessionStorage` mỗi tab có instance riêng (trình duyệt invariant)
- **Trade-off:** đóng tab mất login (re-login khi mở tab mới). Phase 1 BETA acceptable per GAP-599 Proposed Fix Option A
- **Facade pattern** — duy nhất API surface trong `kitehub-frontend/src/lib/auth/jwt-storage.ts` (`setTokens` / `getAccessToken` / `getRefreshToken` / `clearTokens`); caller KHÔNG được truy cập `sessionStorage`/`localStorage` trực tiếp cho JWT
- **SSR safety:** mọi method `guard typeof window` cho Next.js render server-side

## API surface

```typescript
// kitehub/kitehub-frontend/src/lib/auth/jwt-storage.ts

export const ACCESS_TOKEN_KEY = 'accessToken'
export const REFRESH_TOKEN_KEY = 'refreshToken'

setTokens(accessToken: string, refreshToken?: string): void
getAccessToken(): string | null
getRefreshToken(): string | null
clearTokens(): void
hasTokens(): boolean
```

## Banned patterns (per `design-patterns.md`)

| ❌ Don't | ✅ Do |
|---|---|
| `localStorage.setItem('accessToken', token)` direct | `setTokens(token, refreshToken)` qua facade |
| `localStorage.getItem('accessToken')` direct | `getAccessToken()` qua facade |
| Tạo storage key JWT mới ngoài `ACCESS_TOKEN_KEY` / `REFRESH_TOKEN_KEY` | Mở rộng facade nếu cần thêm scope |
| Lưu JWT trong cookie HttpOnly | (Out of scope Phase 1 BETA — `GAP-643` future scope) |

## Test evidence (Wave 92 Bucket B PR #1515)

- `jwt-storage.test.ts` — 17 unit tests cover set/get/clear, SSR guard, edge cases (null/empty/expired) — PASS local + CI
- `jwt-storage.two-tab-simulation.test.ts` — 3 simulation tests verify per-tab isolation in jsdom (mocked sessionStorage scope per test context) — PASS

## Production sites consuming facade (Wave 92 Bucket B)

7 sites migrated từ localStorage trực tiếp sang facade:

1. `lib/auth/api.ts` — login response handler
2. `lib/auth/auth.ts` — token refresh flow
3. `components/auth/LoginForm.tsx` — post-login set tokens
4. `app/login/page.tsx` — server action callback
5. `middleware.ts` — request token read (server-side safe via SSR guard)
6. `lib/api-client.ts` — Authorization header injection
7. `hooks/use-auth.ts` — logout flow

## Live verify mitigation (Wave email-finalize cluster scope)

Tham chiếu `documents/05-guides/operations/acceptance-tests/README.md` § "Concurrent browser session" cho hướng dẫn manual verify post-deploy (mở admin tab A + owner tab B → kiểm DevTools Network tab `Authorization` header → confirm khác nhau).

Live verify mandatory per `pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch flow checklist. Pending AWS-up session per GAP-599 Status.

## Future scope (out of Phase 1 BETA)

- **HttpOnly cookie alternative** — đẩy JWT vào HttpOnly cookie để eliminate XSS exfiltration risk. Tracking GAP-643 (Phase 1.5 paid scope).
- **Refresh token rotation** — current refresh token blacklist + reuse-detection design (separate gap).
- **JWT expiry sync với SSO** — nếu Phase 3 K-12 cohort integrate IdP (Phase 3 scope).

## Related

- Rule: `.claude/rules/design-patterns.md` (Facade pattern + JWT storage banned direct)
- Gap: GAP-599 (this doc closes 1 AC of 6)
- Gap: GAP-643 (future scope HttpOnly cookie)
- Code: `kitehub/kitehub-frontend/src/lib/auth/jwt-storage.ts` (facade implementation)
- Tests: `kitehub/kitehub-frontend/src/lib/auth/__tests__/jwt-storage.{test,two-tab-simulation.test}.ts`
- Wave 92 Bucket B PR #1515 (production sites migration)
