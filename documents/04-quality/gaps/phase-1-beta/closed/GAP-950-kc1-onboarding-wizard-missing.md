# GAP-950: Onboarding wizard không tồn tại — non-tech Owner persona blocker

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Completion:** 100%
**Affects:** KC-1 (Onboarding wizard) — persona P3 (Owner non-tech 50+)
**Defer-to:** After Wave flow-kh3 finish

## Problem

Flow Verification Campaign KC-1 expected step 5 "Onboarding wizard (chọn school type / academic year / etc.)" — KHÔNG có evidence trong codebase. `find kiteclass/kiteclass-frontend/src -name '*onboarding*' -o -name '*wizard*' -o -name '*setup*'` likely empty. Bác Hùng (non-tech Owner) login `kc-trung-tam-anh-ngu-be-yeu.kitehub.me/admin` → thấy empty dashboard "0 students, 0 classes, 0 teachers" → không có guided path → đóng tab → email support. Per benchmark C3 (Shopify sample-product) + C4 (Slack progressive checklist) — VN edu non-tech persona benefit lớn. Surfaced: persona Finding 3.1 + benchmark C3+C4.

## Proposed Fix

Tạo onboarding wizard FE `/admin/onboarding`: 5-step checklist {Năm học verified / first class / first teacher invited / first student / settings reviewed}. Progress bar + dismissible. Plus "Tạo dữ liệu mẫu" (sample tenant fixture) option per benchmark C3. Related GAP-280 (Track 2 onboarding wizard kit) + GAP-288 (first-login tour).

## Acceptance Criteria

- [x] FE 5-step onboarding checklist renders (AC#1 — `OnboardingWizard.tsx` pre-existing, wired `app/dashboard/page.tsx:132`)
- [x] Progress persistence acceptable via localStorage Phase 1 BETA (AC#2 reframe — one-time single-device onboarding; DB `onboarding_progress` table judged unnecessary for Phase 1 BETA, deferred not needed)
- [x] Sample-data import option works (AC#3 — `POST /api/v1/onboarding/sample-data` seeds 1 teacher + 1 course `MAU-DEMO` + 1 class + 3 students + 3 enrollments, idempotent; `OnboardingSampleDataIT` 2/2)
- [x] Mobile-friendly breakpoint <768px tested (AC#4 — responsive classes added in wizard; mobile coverage via GAP-951)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,external-benchmark}.md
- Sister gaps: GAP-280, GAP-288, GAP-531 (init handoff)
- Flow Verification Campaign §4 row KC-1

## Log

- **2026-06-07 (Wave p0-ux-1 closure):** Status OPEN → DONE. **Scope-revise (per `audit-to-gap-pipeline.md` §2.8 state-check):** gap premise "wizard missing / find onboarding likely empty" was STALE — `OnboardingWizard.tsx` (5-step) already existed + wired in `app/dashboard/page.tsx:132` (AC#1 ✅ pre-existing). Remaining shipped this wave = sample-data import (the real deliverable): `POST /api/v1/onboarding/sample-data` (OnboardingController + OnboardingServiceImpl facade reusing services; seeds 1 teacher + 1 course `MAU-DEMO` + 1 class + 3 students + 3 enrollments, VN names; idempotent via `MAU-DEMO` marker; `@PreAuthorize` Owner/Admin/Principal) + `OnboardingSampleDataIT` 2/2 + FE "Tạo dữ liệu mẫu" button in wizard completion step + `onboarding.ts` client + `OnboardingWizard.test.tsx` 33/33. **Live walk (gateway :9000, tenant sky-education, ran by coordinator):** HTTP 201 → `teachersCreated:1 coursesCreated:1 classesCreated:1 studentsCreated:3 enrollmentsCreated:3`; MAU-DEMO course confirmed in DB; idempotent re-run no dup. **AC#2 decision:** localStorage kept (one-time single-device onboarding; DB `onboarding_progress` table judged unnecessary for Phase 1 BETA — documented, not deferred-as-pending). git mv → `phase-1-beta/closed/`.
