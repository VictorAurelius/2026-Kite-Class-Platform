---
audience: dev
---

# GAP-781 — KC settings narrow to branding only (B12 partial coverage)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B12 probe)
**Affects:** B12 Cài đặt trung tâm — Đổi tên / đổi logo / đổi mật khẩu
**Phase:** phase-1-beta

## Problem

Plan §3 B12 expects "Đổi tên / đổi logo / đổi mật khẩu" (3 actions). Catalog probe:

```
KC controllers:
@RequestMapping("/api/v1/settings/branding")     ← logo only (branding scope)
# KHÔNG có /api/v1/settings/center (tên trung tâm)
# KHÔNG có /api/v1/settings/password (đổi mật khẩu)
```

Đổi mật khẩu = KH side (`POST /api/auth/change-password` exists trong AuthController). Nhưng:
- Tên trung tâm: schema `instances.subdomain` + likely `instances.center_name` field — KH endpoint `PUT /api/platform/instances/{id}` exists ⚠️ but Owner phải biết own instance UUID
- Đổi logo: `/api/v1/settings/branding` ✅

So B12 split:
- ✅ Đổi logo: covered via `/api/v1/settings/branding` (KC)
- ⚠️ Đổi mật khẩu: covered via `POST /api/auth/change-password` (KH) — Owner phải hop sang KH context
- ❌ Đổi tên trung tâm: covered via `PUT /api/platform/instances/{id}` (KH) NHƯNG cần UUID — không có FE settings page wired

FE catalog `(dashboard)/settings/page.tsx` — cần inspect xem có integrate 3 actions không.

## Root Cause

Phase 1 BETA scope settings page chưa được build full — branding-only currently exposed.

## Proposed Fix

1. FE `(dashboard)/settings/page.tsx`: 3 tabs/sections (Trung tâm / Thương hiệu / Tài khoản):
   - Trung tâm: PUT tên trung tâm via KH `/api/platform/instances/{id}` — FE resolves UUID from user-context (per GAP-780)
   - Thương hiệu: existing branding flow
   - Tài khoản: link to `change-password` flow (or inline form calling KH endpoint)
2. Optional: thêm KC façade `PUT /api/v1/settings/center` proxy to KH để FE chỉ gọi 1 origin

## Acceptance Criteria

- [ ] FE settings page render 3 sections (tên / logo / mật khẩu)
- [ ] Tên + mật khẩu update work end-to-end với owner.test
- [ ] VN label per `vn-localization-audit-checklist.md` §2

## Related

- Wave 106 plan §3 B12
- Sister: GAP-780 (`/instances/mine` cần để FE resolve own UUID for tên update)
- Existing endpoints: `/api/v1/settings/branding` (KC) + `PUT /api/platform/instances/{id}` (KH) + `POST /api/auth/change-password` (KH)
