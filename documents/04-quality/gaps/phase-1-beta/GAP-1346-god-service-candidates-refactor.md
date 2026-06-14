# GAP-1346: 11 backend service class >20KB — God Service refactor candidate

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Affects:** kiteclass-core (5) + kitehub-subscription (6) service classes

## Problem

Quality audit Cat 9 sub-check "No God Services (>500 lines / >15 methods)" flag **11 service class >20KB** — vượt ngưỡng God Service candidate per `.claude/rules/design-patterns.md` §3:

- kitehub-subscription (6): `SubscriptionService`, `AuthService`, `InstanceService`, `PaymentService`, `BetaAccessService`, `EmailServiceClient`
- kiteclass-core (5): `LmsServiceImpl`, `GradeServiceImpl`, `CourseServiceImpl`, `ClassServiceImpl`, `AttendanceServiceImpl`

Class càng lớn → càng nhiều trách nhiệm trộn (SRP vi phạm), test khó cô lập, merge-conflict cao, đọc-hiểu chậm. `LmsServiceImpl` + `SubscriptionService` + `AttendanceServiceImpl` là ứng viên rõ nhất (domain phình theo wave LMS + biz-100).

## Root Cause

Service layer tích lũy logic qua nhiều wave mà không tách collaborator (query helper, state-machine handler, mapper, policy object). >20KB là proxy theo byte; cần đo dòng + method count để xác nhận.

## Proposed Fix

Audit từng class (line + method count); với class thật sự vi phạm SRP, tách theo pattern (vd State handler cho transition, dedicated query service, policy object). Ưu tiên 3 lớn nhất. KHÔNG cần tách hết — chỉ class có >15 method / >500 dòng / multi-concern rõ.

## Acceptance Criteria

- [ ] Đo line + method count cho 11 class; xác nhận class nào thật sự God Service (>500 dòng OR >15 method)
- [ ] ≥3 class lớn nhất có kế hoạch tách (hoặc đã tách) collaborator theo design-patterns.md §3
- [ ] Quality audit Cat 9 sub-check "No God Services" verify lại sau refactor

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 9)
- Rule: `.claude/rules/design-patterns.md` §3 (anti-pattern God Service)
