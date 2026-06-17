# GAP-1394: 6 FE page stub TODO chưa track — chờ BE endpoint chưa ship

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-15 (GAP-1345 FE TODO triage — nhóm (c) gap-worthy chưa track)
**Affects:** `kitehub/kitehub-frontend/src/**` + `kiteclass/kiteclass-frontend/src/**`

## Problem

Triage 72 marker FE (GAP-1345) tách ra **6 marker gap-worthy CHƯA được track** — đều là FE page render stub/placeholder chờ một BE endpoint chưa ship. Không gắn gap → silent-decay risk (stub "quên" qua nhiều wave, user thấy dữ liệu giả/placeholder không có lý do hiển thị).

| # | App | File:line | Stub chờ gì |
|---|---|---|---|
| 1 | KH | `src/app/(customer)/settings/components/AccountTab.tsx:72` | `/api/users/{id}/preferences` chưa ship |
| 2 | KH | `src/components/support/SupportMenu.tsx:79` | role-based help routing (JWT role claim → /help/p1\|p2-owner\|p3-manager) |
| 3 | KH | `src/app/(customer)/dashboard/page.tsx:79` (+ prose :42) | `/api/subscription/health` chưa ship (stub series) |
| 4 | KC | `src/app/(teacher)/teacher/grades/[classId]/page.tsx:114` | kc-core gradebook API (đã ghi "sub-gap to be filed") |
| 5 | KC | `src/app/(teacher)/teacher/attendance/[classId]/page.tsx:6` | `attendance-period` API |
| 6 | KC | `src/app/(teacher)/teacher/attendance/[classId]/page.tsx:79` | kc-core attendance-period API (Wave 49+ follow-up) |

## Root Cause

FE ship nhanh hơn BE trong wave RBAC/LMS — FE page dựng trước, BE endpoint chưa có → stub TODO tạm, không gắn gap tracking.

## Proposed Fix

Mỗi stub gắn 1 gap BE (hoặc gộp theo domain): preferences endpoint, subscription-health endpoint, gradebook API, attendance-period API, role-based help routing. Khi BE endpoint ship → FE thay stub bằng call thật + retire TODO. Cân nhắc CI WARN-mode đếm FE stub-TODO drift.

## Acceptance Criteria

- [x] Mỗi stub có gap BE tương ứng (hoặc 1 gap gộp per-domain) để khi endpoint ship thì FE wire + retire TODO — 5 stub pending-BE giữ TODO truy vết được `// TODO(GAP-1394)`; 1 stub (SupportMenu) không cần BE → đã wire luôn
- [~] 6 marker được replace bằng call thật khi endpoint sẵn sàng (hoặc downgrade thành empty-state UI có chủ ý) — 1/6 wired (SupportMenu role-based routing, FE-only); 5/6 pending-BE vì endpoint chưa ship (xem Resolution)

## Resolution (2026-06-15 — PARTIAL: 1/6 wired)

Đánh giá từng stub: stub nào BE đã có/không cần BE thì wire ngay; stub nào genuinely cần BE chưa ship thì giữ TODO truy vết được. Không stub dữ liệu giả để "đóng".

| # | Stub | Disposition | Lý do |
|---|---|---|---|
| 1 | KH `AccountTab.tsx:72` preferences | **PENDING-BE** | Không có endpoint user-prefs tổng hợp `/api/users/{id}/preferences`. API notification-preferences hiện có (`/api/v1/notification-preferences`) không map: `trialReminders`→`TRIAL_ENDING` là mandatory (không tắt được), `productUpdates`+`locale` không có chỗ ở BE. Giữ client-side + TODO truy vết. |
| 2 | KH `SupportMenu.tsx:79` role-based help routing | **WIRED** ✅ | Pure FE — đọc role từ `auth-store`, map sang help page đã tồn tại (`/help/{p2-owner,p3-manager,platform-admin,anonymous}`). Không cần BE. Cũng sửa bug dead-link: stub cũ trả `/help` (404, không có route). +6 unit test. |
| 3 | KH `dashboard/page.tsx:79`(+:42) subscription/health | **PENDING-BE (FE de-fabricated 2026-06-17)** | `/api/subscription/health` (telemetry tổng hợp tier + usage% + 7-day series) chưa có backend telemetry. tier/trialDaysLeft/instances đã là dữ liệu thật (từ instances API). **2026-06-17 (PR #2471):** bỏ số liệu giả `Lớp đang vận hành 62 (+8.2%)` + `Lượt gọi API 1620 (+24.4%)` + `Quota brand 7/10` (stub array + hardcoded delta) → render "Sắp có" honest (anti-fabrication cf. GAP-1205) thay vì show số/trend giả cho tenant thật. Giữ `// TODO(GAP-1394)` truy vết để wire khi telemetry endpoint ship. |
| 4 | KC `teacher/grades/[classId]/page.tsx:114` gradebook API | **PENDING-BE** | kc-core gradebook API (LMS domain) chưa ship — thuộc phạm vi parallel GAP-1307/1393. Giữ TODO truy vết. |
| 5 | KC `teacher/attendance/[classId]/page.tsx:6` attendance-period | **PENDING-BE** | Cần shape overview-by-class của kc-core attendance-period (per-period route đã wire `attendancePeriodApi.upsertBatch`, overview-by-class chưa). LMS domain, parallel GAP-1307/1393. |
| 6 | KC `teacher/attendance/[classId]/page.tsx:79` attendance-period | **PENDING-BE** | Cùng class với #5 (overview-by-class shape chưa ship). |

**Verify:** `pnpm --filter kitehub-frontend test --run` (917 PASS) + `build` (PASS); `pnpm --filter kiteclass-frontend test --run` (924 PASS / 206 skip) + `build` (PASS). KC chỉ sửa comment (không đụng kc-core storage/LMS/migrations).

**Khi BE ship:** mỗi TODO truy vết được bằng `// TODO(GAP-1394)` → grep ra để wire + retire.

## Related

- Parent triage: `documents/04-quality/audits/quality-audit/2026-06-14-fe-todo-triage.md` (GAP-1345 nhóm (c))
- Rule: `.claude/rules/discovery-to-gap-inline-filing.md`
- Filed by: audit-fixG-quality wave (cùng PR Jacoco/bundle/triage)
