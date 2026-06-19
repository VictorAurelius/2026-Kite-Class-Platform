# GAP-1170: PreferencesController banner-dismiss state in-memory — mất khi restart, cần user_preferences table

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-11 (TODO classification — IDE diagnostics review)
**Affects:** `kitehub-subscription` `PreferencesController` (banner-dismiss state)

## Problem

`PreferencesController` lưu trạng thái "đã ẩn banner" trong **in-memory `ConcurrentHashMap`** (per-server, mất khi restart):

```java
// In-memory dismissed state (Phase 1 — per GAP-656). Lost on restart.
private final ConcurrentHashMap<String, Boolean> dismissedState = new ConcurrentHashMap<>();
...
String mapKey = "anonymous:" + sanitizedKey; // TODO Wave 99: derive userId từ SecurityContextHolder
```

Hệ quả:
1. **Mất khi restart** — server restart → mọi banner-dismiss server-side reset (client-side cookie 30 ngày vẫn giữ, nên impact UX nhỏ).
2. **Không multi-server** — `ConcurrentHashMap` per-instance, không share giữa các pod.
3. **userId hardcode `anonymous`** — chưa derive từ `SecurityContextHolder` cho user đã đăng nhập.

Author đánh dấu Phase 2 (Wave 99+): "will persist into `user_preferences` table when user authenticated". Đây là **deferred-có-chủ-đích** (GAP-656 Phase 1 ship cookie + in-memory), nhưng chưa có gap track Phase 2 persistence → file để không sót.

## Root Cause

GAP-656 Phase 1 cố ý ship lightweight (cookie client-side + in-memory server-side) để launch nhanh; persistence DB hoãn Phase 2.

## Proposed Fix

1. Tạo bảng `user_preferences` (Flyway migration) — key (user_id, preference_key) → value.
2. Thay `ConcurrentHashMap` bằng repository persistence cho user đã đăng nhập.
3. Derive `userId` từ `SecurityContextHolder` thay vì hardcode `anonymous`.
4. Giữ cookie fallback cho anonymous user.

## Acceptance Criteria

- [ ] `user_preferences` table + migration
- [ ] Banner-dismiss state survive server restart (verify: dismiss → restart → vẫn dismissed)
- [ ] userId derive từ `SecurityContextHolder` cho authenticated user
- [ ] Anonymous user vẫn dùng cookie (no regression)

## Related

- Discovered in: TODO classification 2026-06-11 (IDE diagnostic `PreferencesController.java:60,70`)
- Origin: GAP-656 (Wave 98 — banner-dismiss Phase 1 cookie + in-memory, DONE)
- Author intent: "Phase 2 (Wave 99+)"
