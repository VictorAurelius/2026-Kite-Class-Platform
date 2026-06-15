# GAP-1394: 6 FE page stub TODO chưa track — chờ BE endpoint chưa ship

**Status:** 🔵 OPEN
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

- [ ] Mỗi stub có gap BE tương ứng (hoặc 1 gap gộp per-domain) để khi endpoint ship thì FE wire + retire TODO
- [ ] 6 marker được replace bằng call thật khi endpoint sẵn sàng (hoặc downgrade thành empty-state UI có chủ ý)

## Related

- Parent triage: `documents/04-quality/audits/quality-audit/2026-06-14-fe-todo-triage.md` (GAP-1345 nhóm (c))
- Rule: `.claude/rules/discovery-to-gap-inline-filing.md`
- Filed by: audit-fixG-quality wave (cùng PR Jacoco/bundle/triage)
