# GAP-965: 5-state lifecycle enum + VN i18n labels (Stripe pattern + MISA labels)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant lifecycle naming) — VN persona role labels + industry pattern
**Defer-to:** After Wave flow-kh3 finish

## Problem

Hiện trạng: tenant lifecycle enum chưa lock + VN labels chưa định. Per benchmark §A row 5: Stripe = trialing/active/past_due/canceled/unpaid, Slack = active/archived/deleted, MISA = (đăng ký + năm học active/closed). Plus VN role names: spec dùng English (Owner/Teacher/Student/Parent) — non-tech Owner persona (40-55 tuổi) confuse với English. Surfaced: benchmark C1 + B2.

## Proposed Fix

Lock lifecycle enum `{TRIAL, ACTIVE, SUSPENDED, EXPIRED, ARCHIVED}` cho Phase 1 (5-state Stripe-pattern). VN i18n labels `{"Dùng thử / Hoạt động / Tạm ngưng / Hết hạn / Lưu trữ"}` in `kiteclass-frontend/src/lib/i18n/vi.json`. Plus role i18n: backend enum English (PLATFORM_ADMIN/TENANT_OWNER/TEACHER/STUDENT/PARENT), FE label always VN (Quản lý / Giáo viên / Học sinh / Phụ huynh).

## Acceptance Criteria

- [ ] `InstanceStatus` enum updated với 5 states
- [ ] FE i18n keys `tenant.status.trial / active / suspended / expired / archived` render VN
- [ ] FE role labels render VN trong admin tables + dropdowns
- [ ] Walk admin + owner view → see VN labels (no raw English enum visible)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-external-benchmark.md C1 + B2
- Sister rule: `.claude/rules/vn-localization-audit-checklist.md` v1.0.0
- Flow Verification Campaign §4 row KC-1
