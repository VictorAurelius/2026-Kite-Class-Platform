---
audience: dev
---

# GAP-779 — `/api/auth/me` endpoint missing (FE convention)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B3 probe)
**Affects:** FE refresh-user-context flow + token validation check
**Phase:** phase-1-beta

## Problem

Probe `GET /api/auth/me` → HTTP 404 "Endpoint not found".

KH `AuthController` endpoints:
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/verify-email
POST /api/auth/resend-verification
PUT  /api/auth/profile
POST /api/auth/change-password
# Không có GET /api/auth/me
```

FE convention common pattern: sau khi refresh page, FE store token → call `GET /api/auth/me` để rehydrate user object (id/email/role) thay vì decode JWT client-side.

## Root Cause

Current FE pattern: JWT decode client-side để extract role + email. Works nhưng:
- JWT có thể stale nếu user role thay đổi server-side (vd PLATFORM_ADMIN demoted)
- Client-side JWT decode không catch token revocation
- Convention violation: most SaaS có `/me` endpoint

## Proposed Fix

Add `GET /api/auth/me` trong `AuthController`:
- Reads JWT from Authorization header (filter chain already validates)
- Query DB by JWT `sub` claim → return current user state (id, email, name, role, tenant_id, email_verified, instances list)
- Response shape match `/api/auth/login` user object (consistency)

Effort: ~15 LOC + 1 integration test.

## Acceptance Criteria

- [ ] `GET /api/auth/me` returns 200 + user object cho valid JWT
- [ ] 401 cho expired/invalid JWT
- [ ] Integration test verify role drift catches (admin role demoted → /me reflects new role)
- [ ] FE migrate from client-side JWT decode to `/api/auth/me` rehydrate (optional follow-up)

## Related

- Wave 106 RST B3 probe evidence
- Sister: GAP-780 (`/api/instances` refetch endpoint also missing — pair `/me` + `/instances` for full user-context rehydrate)
