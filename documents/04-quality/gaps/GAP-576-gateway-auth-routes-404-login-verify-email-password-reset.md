# GAP-576 — Gateway auth routes trả 404: /api/v1/auth/login, /verify-email, /password-reset

**Status:** 🟠 OPEN
**Priority:** P0 (chặn ADM-LOGIN-002..005 + EMAIL-VERIFY-002 + EMAIL-RESET-001 trong Phase 1 BETA acceptance walk-through)
**Type:** Feature / Infra — gateway route config
**Wave:** Wave 85 candidate
**Created:** 2026-05-15
**Discovered-by:** Phase 1 BETA Acceptance Walk-through ([phase-1-beta-walkthrough-2026-05-15.md](../../05-guides/operations/acceptance-tests/phase-1-beta-walkthrough-2026-05-15.md))
**Related:** GAP-518 (FE role-guard), GAP-524 (email verify), GAP-525 (invite flow), GAP-577 (branding route)

---

## Problem

Phase 1 BETA acceptance walk-through 2026-05-15 phát hiện 3 auth endpoints chính trên gateway `api.kitehub.me` trả HTTP 404 thay vì 200/400:

```
GET https://api.kitehub.me/api/v1/auth/login           → 404
GET https://api.kitehub.me/api/v1/auth/verify-email    → 404
GET https://api.kitehub.me/api/v1/auth/password-reset  → 404
```

Trong khi đó:
- `POST /api/v1/auth/request-beta-access` trả HTTP 405 trên GET (POST handler live) ✅
- `GET /api/v1/admin/beta-requests` trả HTTP 401 (route exists, auth-guard) ✅

→ Pattern rõ ràng: 3 endpoint trên KHÔNG được expose qua gateway prefix `/api/v1/auth/*`, nhưng `request-beta-access` + `admin/beta-requests` ĐƯỢC expose.

## Chặn flow nào

- **ADM-LOGIN-002..005** (Platform admin login) — full BLOCKED
- **EMAIL-VERIFY-002** (click verify link → BE validate token) — full BLOCKED
- **EMAIL-RESET-001..002** (forgot password flow) — full BLOCKED
- **OWNER-SIGNUP-003** (tenant signup auto-login sau provision) — partial BLOCKED
- **TEACH-LOGIN-001..003, PARENT-LOGIN-001..003** — derivative blocked

## Root cause hypothesis

Khả năng cao 1 trong 3:

1. **Gateway route map chưa bao gồm `/api/v1/auth/{login,verify-email,password-reset}`** — config `application-prod.yml` của `kite-gateway` chỉ wire `/auth/request-beta-access`
2. **Service prefix mismatch** — kitehub-platform expose `/login` thay vì `/auth/login`, gateway map fail
3. **Probe artifact** — endpoints chỉ accept POST, GET trả 404 thay vì 405 (less likely vì `request-beta-access` trả 405 đúng pattern)

## Proposed Fix

Cần verify 3 bước:

1. **Read gateway config** (`kitehub/kite-gateway/src/main/resources/application*.yml`) — list route rules cho `/api/v1/auth/*`
2. **Read kitehub-platform / kitehub-subscription controllers** — confirm `@RequestMapping` prefixes cho login/verify-email/password-reset
3. **Test POST với curl** trên 3 endpoint để loại trừ probe artifact

Nếu là gateway route gap → add 3 route rules cùng PR.
Nếu là controller missing → file thêm GAP cho từng controller.

## Acceptance Criteria

- [ ] `POST /api/v1/auth/login` returns 200 (valid creds) hoặc 401 (invalid) — KHÔNG 404
- [ ] `POST /api/v1/auth/verify-email` returns 200/400 — KHÔNG 404
- [ ] `POST /api/v1/auth/password-reset` returns 200/400 — KHÔNG 404
- [ ] ADM-LOGIN-002 acceptance row unblocked (admin có thể login UI)
- [ ] EMAIL-VERIFY-002 acceptance row unblocked
- [ ] EMAIL-RESET-001 acceptance row unblocked
- [ ] Phase 1 BETA walk-through re-run cho 3 flows → PASS

## Notes

Verify-via: `curl -X POST -H "Content-Type: application/json" -d '{}' https://api.kitehub.me/api/v1/auth/login` — kỳ vọng HTTP 400 validation error (không phải 404).

## Log

- **2026-05-15:** Gap filed từ Phase 1 BETA acceptance walk-through 2026-05-15. Discovered probe `curl https://api.kitehub.me/api/v1/auth/login` returns 404. Sister gap GAP-577 cho `/api/v1/branding` 404. 2 gaps có khả năng chung root cause (gateway route config chưa sync với Wave 71b+ controller additions).
