# GAP-980: Thiếu tenant-config settings surface (locale/currency/tuần làm việc/academic-defaults) cho Owner

**Status:** 🔵 OPEN
**Priority:** P3
**Domain:** Mixed
**Found:** 2026-06-05 (Wave flow-kc1 KC-1 G1 walk — re-scope decision, user-approved)
**Affects:** Owner muốn cấu hình tenant-level settings ngoài branding (locale, currency, tuần làm việc Mon-Sat, niên khóa mặc định)

## Problem

Plan KC-1 giả định Owner mở `/settings` thấy "tenant settings thống nhất" gồm org-name + niên khóa + tuần Mon-Sat + locale vi-VN + currency VND + branding. Walk + persona sim 2026-06-05 xác nhận **surface này KHÔNG tồn tại**:
- Settings thực tế chỉ có: `branding` (display name + tagline + 3 màu + contact) + user-preferences (parent/teacher/student) + landing.
- `Branding` entity (`Branding.java:43-64`) không có locale/currency/week/academic field.
- `organization_name` nằm ở `kitehub.instances` (set lúc onboarding, read-only từ KiteClass).
- Niên khóa là module riêng (`module/academicyear`, thuộc scope KC-3), không phải "settings".
- locale/currency/tuần làm việc: **không có nơi lưu** trong settings.

Business docs (`tenant-settings/rules.md`) cũng chỉ scope branding + preferences + landing — KHÔNG promise tenant-config thống nhất. Plan over-specified.

User quyết định 2026-06-05: re-scope KC-1 G1 về branding + preferences (có thật); file gap riêng cho tenant-config thiếu (KHÔNG block KC-1 thông).

## Proposed Fix

Phase 1.5+ (defer, không block Phase 1 BETA):
- Thiết kế tenant-config settings surface: `TenantSettings` entity (locale, currency, working_days, default_academic_year ref) + endpoint `/api/v1/settings/tenant` (GET/PATCH) + FE tab.
- Hoặc xác nhận đây là out-of-scope vĩnh viễn nếu locale/currency cố định vi-VN/VND cho Phase 1 BETA (VN-only market).

## Acceptance Criteria

- [ ] Quyết định: tenant-config là feature Phase 1.5 hay out-of-scope (vi-VN/VND hardcoded acceptable)?
- [ ] Nếu feature → `TenantSettings` entity + endpoint + FE tab + walk
- [ ] Nếu out-of-scope → document rõ trong `tenant-settings/rules.md` rằng locale/currency cố định Phase 1 BETA

## Related

- Discovered in: Wave flow-kc1 KC-1 G1 walk 2026-06-05 (re-scope, user-approved AskUserQuestion)
- `thesis-as-future-state-mandate.md` (nếu thesis claim tenant-config → Phase 1.5 delivery)
- Business docs: `documents/01-business/kiteclass/tenant-settings/`

## Log

- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes 'Defer Phase 1.5 feature').
