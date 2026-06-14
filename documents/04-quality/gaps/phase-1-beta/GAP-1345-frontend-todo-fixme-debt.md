# GAP-1345: 72 TODO/FIXME tích lũy trên frontend (KC 29 + KH 43)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Affects:** `kiteclass/kiteclass-frontend/src/**` + `kitehub/kitehub-frontend/src/**`

## Problem

Grep `TODO|FIXME|HACK` trên 2 frontend src đếm được **72 marker** (kiteclass-frontend 29 + kitehub-frontend 43). Trong khi backend Java main giữ ổn định 12 marker (KC 7 + KH 5), FE đã tích lũy debt cao gấp 6 lần mà chưa được triage. Các marker này không gắn gap tracking → silent-decay risk: feature scaffolding tạm, edge-case chưa xử lý, hoặc workaround có thể "quên" qua nhiều wave.

## Root Cause

Tốc độ ship FE cao (RBAC role-shell + LMS + ui-kits/landing/branding) trong 26 ngày sinh scaffolding TODO không được retire; không có CI gate cảnh báo FE TODO count drift.

## Proposed Fix

Triage 72 marker thành 3 nhóm: (a) actionable-now (fix inline), (b) gap-worthy (file gap riêng per `discovery-to-gap-inline-filing`), (c) acceptable-permanent (gắn comment lý do). Xem xét CI WARN-mode đếm FE TODO drift (threshold mềm). Mục tiêu giảm về ≤30 sau triage.

## Acceptance Criteria

- [ ] 72 marker được phân loại (actionable / gap-worthy / acceptable) — có bảng triage
- [ ] Marker gap-worthy đã file gap riêng
- [ ] FE TODO count ≤30 sau triage (hoặc tài liệu hóa lý do giữ marker còn lại)

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 4/9)
- Rule: `.claude/rules/discovery-to-gap-inline-filing.md` (triage marker → gap)
