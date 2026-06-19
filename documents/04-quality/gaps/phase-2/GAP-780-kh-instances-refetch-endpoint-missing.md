---
audience: dev
---

# GAP-780 — KH owner instances refetch endpoint missing (login returns but no GET)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B3 chọn trung tâm probe)
**Affects:** Owner với N tenants: refresh tab → mất instances list trừ khi re-login
**Phase:** phase-1-beta

## Problem

`POST /api/auth/login` response includes `"instances":[]` array. Verified InstanceController có:
```
@RequestMapping("/api/platform/instances")
GET /{id}
GET /subdomain/{subdomain}
GET /owner/{ownerId}
```

NHƯNG probe:
```
GET /api/instances                       → 404
GET /api/subscriptions/instances         → 404
GET /api/platform/instances/owner/{me}   → would require client to know own UUID
```

Wave 106 plan §3 B3 expects "Đăng nhập lại + chọn trung tâm (nếu có N trung tâm)" — Owner với 2+ instances cần endpoint để refetch list mà không cần know own UUID hardcoded.

## Root Cause

`InstanceController` exposes `/owner/{ownerId}` requiring client to pass UUID. FE typically wants `GET /api/platform/instances/mine` (uses JWT to resolve owner_id server-side).

## Proposed Fix

Add `GET /api/platform/instances/mine` trong `InstanceController`:
- Resolve owner_id from JWT `sub` claim
- Return list of instances cho owner (same shape as login response `instances:[]`)
- Role-guard: OWNER + PLATFORM_ADMIN

Effort: ~10 LOC + 1 integration test.

## Acceptance Criteria

- [ ] `GET /api/platform/instances/mine` 200 cho OWNER role, returns array
- [ ] 401 cho missing JWT, 403 cho non-OWNER non-ADMIN
- [ ] B3 luồng walk: Owner login → refresh tab → instances list available without re-login

## Related

- Wave 106 RST B3 probe evidence (login response `instances:[]` field)
- Pair với GAP-779 (`/api/auth/me`) — both serve user-context rehydrate flow
- Existing: `InstanceController.getByOwner(ownerId)` template
