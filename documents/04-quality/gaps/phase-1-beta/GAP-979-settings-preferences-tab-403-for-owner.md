# GAP-979: Settings "Tùy chọn" (preferences) tab 403 cho Owner — Owner không có numeric ref-id

**Status:** 🟡 PARTIAL
**Priority:** P2
**Domain:** Mixed
**Found:** 2026-06-05 (Wave flow-kc1 KC-1 G1 walk — coordinator walk production-equivalent V86)
**Affects:** Mọi Owner mở `/settings` trên KiteClass FE → click tab "Tùy chọn" → tab hỏng (403)

## Problem

FE `/settings/page.tsx` render 3 tab vô điều kiện: Branding / Theme preview / **Tùy chọn** (`TabsTrigger value="preferences"` line 57, không role-gate). Tab "Tùy chọn" mount `PreferencesSettings` → `usePreferences()` → GET `/api/v1/users/{userId}/preferences`.

Backend `UserPreferencesController.validateUserAccess()` so khớp `UserContext.getCurrentReferenceId()` (kiểu **Long**) = header `X-User-Reference-Id` = `parents.id / teachers.id / students.id` (numeric domain id, per `UserContext.java:48`).

**Owner có identity cấp KiteHub (UUID `sub`, KHÔNG có numeric domain ref-id trong KiteClass).** JWT Owner chỉ chứa `{sub(UUID), email, role:OWNER, tenantId}` — không có reference id.

Empirical (walk 2026-06-05, sky-education tenant):
- Login Owner `owner@skyedu.vn` → JWT HTTP 200 (tenantId=0edaee10, role=OWNER)
- GET `/api/v1/users/1/preferences` + Bearer JWT → **HTTP 403 `USER_NOT_AUTHENTICATED`** (`getCurrentReferenceId()` = null cho Owner)
- → Owner click tab "Tùy chọn" sẽ thấy lỗi/empty-error, không phải settings

Branding tab (cùng trang) hoạt động hoàn hảo (GET/PUT 200 + DB persist) — chỉ preferences tab hỏng cho Owner.

## Root Cause

Preferences được thiết kế cho in-tenant persona (parent/teacher/student có numeric domain row). Owner là KiteHub-level identity không thuộc 3 domain đó → không có ref-id → endpoint từ chối. FE không gate tab theo role nên Owner vẫn thấy + click được.

## Proposed Fix

Quyết định product trước (1 trong 2):
- **A (nhỏ, Phase 1 BETA):** FE ẩn tab "Tùy chọn" khi `role === OWNER` (Owner dùng KiteHub-level settings, không có per-user preferences trong KiteClass). Hoặc render empty-state "Không áp dụng cho Owner".
- **B (lớn hơn):** Cấp Owner một preferences scope riêng (owner-level preferences path hoặc numeric ref-id cho Owner).

Khuyến nghị A cho Phase 1 BETA (2-role MVP OWNER+STAFF).

## Acceptance Criteria

- [ ] Owner mở `/settings` KHÔNG thấy tab hỏng (ẩn tab HOẶC empty-state graceful, không 403 error surface)
- [ ] Parent/Teacher/Student (nếu có trong scope) vẫn truy cập preferences bình thường
- [ ] Walk lại: Owner `/settings` → mọi tab hiển thị đều dùng được

## Log

- **2026-06-05** (Wave flow-kc1, fix shipped — PARTIAL): chọn Option A. `settings/page.tsx` thêm `useAuth()` → `showPreferences = user?.userType !== 'OWNER'` → ẩn cả `TabsTrigger` + `TabsContent` "Tùy chọn" cho Owner. Verified: `pnpm/next build` PASS (exit 0); role login Owner trả `OWNER` → `userType='OWNER'` → tab ẩn (data+build+logic verified). PARTIAL vì AC "Owner mở /settings không thấy tab hỏng" cần G2 browser-visual confirm (human login Owner). Parent/teacher/student giữ preferences (có numeric ref-id) — chưa walk persona đó (out of KC-1 scope).

## Related

- Discovered in: Wave flow-kc1 KC-1 G1 walk 2026-06-05
- Sister finding: pre-walk persona sim FM-7 (`documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc1-tenant-settings.md`)
- `pre-handoff-self-test-completeness.md` §2.4 (admin/privileged nav must render without error)
