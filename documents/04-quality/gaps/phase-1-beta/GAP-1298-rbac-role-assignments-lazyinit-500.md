# GAP-1298: RBAC Bucket D assign/list 500 — LazyInitializationException trên Role proxy

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Phase:** phase-1-beta
**Found:** 2026-06-14 (G1 runtime walk RBAC+LMS — Flow 2)
**Affects:** `kiteclass-core` `RoleService.listAssignments()` + `getAssignmentForUser()` (RBAC Bucket D, GAP-1119)

## Problem

G1 gateway-walk phát hiện RBAC Bucket D assign + list trả **HTTP 500 `SYSTEM_INTERNAL_ERROR`** khi tenant có ≥1 role assignment:

```
POST /api/v1/roles/assignments {userId:4,roleName:TEACHER} → 500
GET  /api/v1/roles/assignments                              → 500 (sau khi có assignment)
```

Stacktrace:
```
org.hibernate.LazyInitializationException: Could not initialize proxy [Role#4] - no session
  at RoleService.getAssignmentForUser(RoleService.java:205)
  at RoleController.assignRole(RoleController.java:119)
```

Root cause: `listAssignments()` (line 185) + `getAssignmentForUser()` (line 202) KHÔNG có `@Transactional`. Chúng stream `UserRole` rồi gọi `ur.getRole().getName()` / `Role::getName` trên **lazy-loaded Role proxy** sau khi session đã đóng (OSIV off — `spring.jpa.open-in-view=false`). List rỗng → không truy cập Role → 200 OK; list có row → lazy init fail → 500.

Tác dụng phụ: `assignRole` save user_role THÀNH CÔNG rồi 500 khi build response → row persist nhưng client thấy lỗi (state ambiguity). Blocking RBAC Bucket D owner-shell assign/revoke (GAP-1119).

## Proposed Fix (SHIPPED inline)

Thêm `@Transactional(readOnly = true)` lên `listAssignments()` + `getAssignmentForUser()` → session mở suốt stream mapping → lazy `Role.getName()` resolve OK.

## Acceptance Criteria

- [x] `GET /api/v1/roles/assignments` trả 200 (list rỗng + list có row) — G1 re-walk PASS
- [x] `POST /api/v1/roles/assignments` trả 201 + `{userId, roles:[…]}` — G1 re-walk PASS
- [x] `DELETE …/assignments` revoke 204 idempotent — PASS
- [ ] G2★ human browser-walk owner-shell `/admin/roles` assign/revoke UI (FE `:3000`) PASS
- [ ] (nâng cao) `RoleRepository` JOIN FETCH role để tránh N+1 (perf, không blocking)

## Related

- Discovered in: G1 walk `documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`
- Flow: GAP-1119 (RBAC role-shell Bucket D assign-UI)
- Cùng pattern OSIV-off lazy-init class (chú ý các mapping-outside-tx khác trong kiteclass-core)

## G1-FE browser walk note (2026-06-14)

G1-FE confirm: fix `@Transactional(readOnly=true)` hoạt động — `/admin/roles` render 5 mẫu vai trò (role-template list) qua browser không còn LazyInitializationException 500. — verified qua Playwright headless trên FE thật `skytest.127.0.0.1.nip.io:3000` (rebuild kiteclass-frontend). Evidence: `documents/04-quality/audits/rst-html/2026-06-14-g1-fe-browser-walk.md`. **Giữ PARTIAL** — human G2★ vẫn bắt buộc (mutation deep-interaction chưa walk).
