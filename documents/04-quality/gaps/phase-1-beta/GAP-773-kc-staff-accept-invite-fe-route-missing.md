---
audience: dev
---

# GAP-773 — KC `/staff/accept-invite` FE route 404 (Mảng C1 blocker)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng C catalog probe)
**Affects:** C1 Nhân viên nhận thư mời → đăng ký
**Phase:** phase-1-beta

## Problem

`curl -sI http://localhost:3000/staff/accept-invite` → HTTP 404.

FE route catalog:
```
kiteclass-frontend/src/app/(auth)/parent-invite          ← PARENT only
kiteclass-frontend/src/app/(auth)/parent-invite/[token]
# KHÔNG có (auth)/staff/* hoặc staff/accept-invite
```

Wave 106 plan §3 C1 expects staff click invite link → land claim page. Page không tồn tại — invite email sẽ link tới dead URL.

## Root Cause

Pair với GAP-772 (BE controller missing) — FE chưa được build vì BE chưa có endpoint.

## Proposed Fix

Pair fix với GAP-772 cùng PR khi quyết định Option A vs B:
- Option A: thêm `(auth)/staff/accept-invite/[token]/page.tsx` (mirror parent-invite pattern); + role-guard render Vietnamese form per `vn-localization-audit-checklist.md` §2
- Option B: defer — không tạo route, plan §3 C đánh dấu out-of-scope Phase 1 BETA

## Acceptance Criteria

- [ ] Decision sync với GAP-772 (đồng bộ option)
- [ ] Nếu A: route exists, render form, integration test pass
- [ ] Nếu B: route không cần tạo, plan §3 C explicit out-of-scope

## Related

- Sister BE: GAP-772
- Wave 106 plan §3 C1
- FE template: `kiteclass-frontend/src/app/(auth)/parent-invite/[token]/page.tsx`
