# GAP-289: Quick-add lesson session UI for mobile (≤5 clicks)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — UX gap; AC-OPS-001 PARTIAL
**Domain:** Frontend (kiteclass-frontend)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher daily ops on mobile; P2 Small Center teacher ops

## Problem

Theo AC-OPS-001, teacher PHẢI có thể schedule 1 lesson session trong ≤5 clicks trên mobile. Hiện tại form `classes/new` full-featured (max_students, location_type, dates, code_expires_at, schedule plain string, description...) khiến mobile click count >5.

**State-check (verified 2026-05-04):**
- `kiteclass-frontend/src/app/(dashboard)/classes/[id]/` exists với attendance/edit
- `kiteclass-core/module/clazz/entity/Class.java` có ~10 fields, nhiều fields required
- `kiteclass-core/module/clazz/entity/ClassSession.java` riêng entity — UI chưa có quick-add session within existing class
- Mobile responsive nhưng form layout = desktop-style (multi-column → vertical stack)

## Root Cause

UX thiết kế cho center admin (multi-class, multi-teacher) — solo teacher 1-2 courses chỉ cần quick-add với defaults. Class entity vs ClassSession concept không clearly separated trong UI.

## Proposed Fix

1. **New FE route:** `app/(dashboard)/classes/[id]/sessions/new` — quick-add 1 session với 3 fields:
   - Date picker (default = today)
   - Time picker (default = 19:00)
   - Duration (default = 90 min)
   - All other fields use sensible defaults
2. **Floating Action Button (FAB) on mobile:** "+ Buổi học" trong dashboard / class detail
3. **Recent-defaults memory:** remember last used time + duration để pre-fill
4. **Tap target ≥44pt** mọi button (already h-12 = 48pt OK)

## Acceptance Criteria

- [ ] ≤5 clicks/taps để create 1 session từ dashboard
- [ ] Mobile viewport (375px) layout không scroll-lock
- [ ] FAB visible trên dashboard mobile
- [ ] Recent defaults persist trong localStorage per-user
- [ ] Playwright mobile E2E: create-session under 60 seconds
- [ ] No regression cho desktop "create class" full form

## Related

- AC-OPS-001 (P1 review 2026-05-04)
- GAP-290 (Recurring class generator) — multi-session pattern
- GAP-291 (Reschedule session) — paired session-management UX

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
