# GAP-1345: 72 TODO/FIXME tích lũy trên frontend (KC 29 + KH 43)

**Status:** 🟡 PARTIAL
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

- [x] 72 marker được phân loại (actionable / gap-worthy / acceptable) — có bảng triage
- [x] Marker gap-worthy đã file gap riêng (GAP-1394)
- [ ] FE TODO count ≤30 sau triage (DEFER — xoá vật lý chờ UI agent + CI WARN-mode; lý do giữ tài liệu hoá)

## Resolution (2026-06-15 — audit-fixG-quality wave)

**PARTIAL — inventory DONE, xoá vật lý DEFER.** Triage đầy đủ 72 marker tại `documents/04-quality/audits/quality-audit/2026-06-14-fe-todo-triage.md`.

Phân loại 4 nhóm:
- (a) **Acceptable Phase-2 legal placeholder: 52** — nội dung legal page (privacy/terms/cookies) chờ đăng ký pháp nhân (tên công ty, MST, DPO) — Phase 2 có chủ ý, KEEP.
- (b) **Đã gap-tracked: 12** — branding wizard `TODO(GAP-272x/226/227/228)` + schemas GAP-174, đã theo dõi.
- (c) **Gap-worthy chưa track: 6** → **file GAP-1394** (FE stub chờ BE endpoint chưa ship: preferences / subscription-health / gradebook / attendance-period / role-help-routing).
- (d) **False-positive prose: 2** — chữ "TODO" trong văn xuôi comment.

**Untracked actionable debt sau triage = 0.** Raw grep vẫn 72 vì marker nằm trong component/page file (UI agent sở hữu — PR này KHÔNG sửa, theo ownership boundary). Mục tiêu "≤30" diễn giải là "untracked actionable → 0", đạt qua triage + GAP-1394. Xoá marker vật lý + CI WARN-mode count DEFER. Giữ PARTIAL.

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 4/9)
- Rule: `.claude/rules/discovery-to-gap-inline-filing.md` (triage marker → gap)
