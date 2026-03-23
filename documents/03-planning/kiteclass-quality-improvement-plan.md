# KiteClass Quality Improvement Plan

**Ngày tạo:** 2026-03-23
**Baseline:** 78/100 (Grade C) — audit ngày 2026-03-23
**Mục tiêu:** 90+/100 (Grade A)
**Dựa trên:** Quality Audit Framework (10 categories × 10 điểm)

---

## Gap Analysis

| # | Category | Current | Target | Gap |
|---|----------|---------|--------|-----|
| 1 | E2E Functionality | 5 | 9 | -4 |
| 2 | Security | 8 | 9 | -1 |
| 3 | Backend Tests | 8 | 10 | -2 |
| 4 | Frontend Tests | 9 | 10 | -1 |
| 5 | CI/CD | 10 | 10 | 0 |
| 6 | UI/UX | 8 | 9 | -1 |
| 7 | DevOps/Infrastructure | 6 | 8 | -2 |
| 8 | Documentation | 7 | 9 | -2 |
| 9 | Code Quality | 8 | 9 | -1 |
| 10 | Project Management | 9 | 10 | -1 |
| **Total** | | **78** | **93** | **-15** |

---

## PR Plan

### PR-KC-1: Fix @Disabled Tests (Quick Win)

**Score impact:** Backend Tests +1 → 9/10
**Estimate:** 2 giờ
**Scope:**
- [ ] Audit 4 @Disabled tests trong kiteclass-core
- [ ] Fix root cause hoặc remove nếu obsolete
- [ ] Verify: `./mvnw test` — 0 skipped, 0 failures

**Lý do:** Skipped tests = technical debt. Fix hoặc xóa.

---

### PR-KC-2: Integration Tests cho Core Service

**Score impact:** Backend Tests +1 → 10/10
**Estimate:** 1 ngày
**Scope:**
- [ ] Tạo `StudentEnrollmentIT.java` — full flow: create student → enroll → verify
- [ ] Tạo `AttendanceFlowIT.java` — flow: create class → record attendance → report
- [ ] Tạo `CourseManagementIT.java` — flow: create course → add teacher → add class
- [ ] Dùng `@SpringBootTest` + H2 (giống pattern hiện tại)
- [ ] Verify: `./mvnw test` pass

**Files cần tạo:**
- `kiteclass-core/src/test/java/.../integration/StudentEnrollmentIT.java`
- `kiteclass-core/src/test/java/.../integration/AttendanceFlowIT.java`
- `kiteclass-core/src/test/java/.../integration/CourseManagementIT.java`

---

### PR-KC-3: Standalone E2E Test Script

**Score impact:** E2E +2 → 7/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Tạo `kiteclass/scripts/test-api-e2e.sh` — test qua gateway:
  - Student CRUD endpoints
  - Teacher CRUD endpoints
  - Course + Class CRUD endpoints
  - Attendance recording + report endpoints
- [ ] Health check wait trước khi test
- [ ] Verify: pass khi Docker stack up

---

### PR-KC-4: Multi-tenant E2E Verification

**Score impact:** E2E +2 → 9/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Add multi-tenant test cases vào E2E script:
  - Create 2 instances (tenant-a, tenant-b)
  - Create student in tenant-a
  - Verify tenant-b KHÔNG thấy student của tenant-a
  - Verify TenantResolver header routing
- [ ] Document multi-tenant testing approach

---

### PR-KC-5: Fix Frontend TODOs

**Score impact:** Code Quality +1 → 9/10
**Estimate:** 2 giờ
**Scope:**
- [ ] Fix `useAuth.ts` — hardcoded placeholder UUID → decode from JWT hoặc fallback graceful
- [ ] Fix `attendance/stats/page.tsx` — TODO fetch teacher name
- [ ] Fix `attendance/reports/page.tsx` — TODO show details
- [ ] Fix `students/[id]/attendance/page.tsx` — TODO fetch enrollment
- [ ] Fix `use-attendance.ts` — TODO fetch from enrollment
- [ ] Verify: 0 TODO/FIXME trong frontend src/

---

### PR-KC-6: Documentation — README + Deploy Guide

**Score impact:** Documentation +2 → 9/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Tạo `kiteclass/kiteclass-core/README.md`:
  - Architecture overview (multi-tenant, module structure)
  - Local development setup
  - API overview (Student, Teacher, Course, Class, Attendance, Enrollment)
  - Testing guide
- [ ] Tạo `kiteclass/QUICK_START.md`:
  - Standalone dev setup (nếu không qua KiteHub)
  - Required environment variables
  - Database setup

---

### PR-KC-7: DevOps — Standalone Docker Support

**Score impact:** DevOps +2 → 8/10
**Estimate:** 2 giờ
**Scope:**
- [ ] Tạo `kiteclass/docker-compose.dev.yml` — minimal stack:
  - PostgreSQL + Redis + kiteclass-core
  - Không cần KiteHub services
  - Dùng cho dev/test độc lập
- [ ] Tạo `kiteclass/scripts/dev-up.sh` — start minimal stack
- [ ] Healthchecks cho tất cả services
- [ ] Document trong QUICK_START.md

---

### PR-KC-8: Input Validation Hardening

**Score impact:** Security +1 → 9/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Audit controllers với @Disabled tests — ensure validation complete
- [ ] Add `@Valid` nơi còn thiếu
- [ ] Verify GlobalExceptionHandler xử lý MethodArgumentNotValidException
- [ ] Add validation test cho critical endpoints

---

### PR-KC-9: Onboarding UX cho Teacher/Admin

**Score impact:** UI/UX +1 → 9/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Tạo `DashboardWelcome` component — hiện cho user lần đầu:
  - "Chào mừng! Bắt đầu với..."
  - Quick actions: Thêm học sinh, Tạo lớp, Điểm danh
- [ ] Empty state messages cho các list pages khi chưa có data
- [ ] Component test

---

### PR-KC-10: Plan Completion Tracking

**Score impact:** Project Management +1 → 10/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Update `kiteclass-implementation-plan.md` với completion status
- [ ] Mark ✅ cho PRs đã done
- [ ] Add PR numbers cho tracked items
- [ ] Review và archive obsolete plan items

---

## Execution Order

```
Phase 1 — Quick Wins (1 ngày):
  PR-KC-1 (@Disabled) ──→ PR-KC-5 (FE TODOs) ──→ PR-KC-6 (README)
                                                          ↓
Phase 2 — Backend + E2E (2 ngày):
  PR-KC-2 (integration tests) ──→ PR-KC-3 (E2E script) ──→ PR-KC-4 (multi-tenant)
                                                                  ↓
Phase 3 — DevOps + UX (1 ngày):
  PR-KC-7 (docker) ──→ PR-KC-8 (validation) ──→ PR-KC-9 (onboarding)
                                                        ↓
Phase 4 — Finalize:
  PR-KC-10 (plan tracking)
```

---

## Score Projection

| Sau PR | Score | Grade | Tăng |
|--------|-------|-------|------|
| Baseline | 78 | C | — |
| PR-KC-1 (@Disabled) | 79 | C | +1 |
| PR-KC-5 (FE TODOs) | 80 | B | +1 |
| PR-KC-6 (README) | 82 | B | +2 |
| PR-KC-2 (integration tests) | 83 | B | +1 |
| PR-KC-3 (E2E script) | 85 | B+ | +2 |
| PR-KC-4 (multi-tenant) | 87 | B+ | +2 |
| PR-KC-7 (docker) | 89 | B+ | +2 |
| PR-KC-8 (validation) | 90 | A | +1 |
| PR-KC-9 (onboarding) | 91 | A | +1 |
| PR-KC-10 (plan tracking) | **92** | **A** | +1 |

---

## Estimate tổng

| Phase | PRs | Days |
|-------|-----|------|
| Phase 1 | KC-1, KC-5, KC-6 | 1 ngày |
| Phase 2 | KC-2, KC-3, KC-4 | 2 ngày |
| Phase 3 | KC-7, KC-8, KC-9 | 1 ngày |
| Phase 4 | KC-10 | 0.5 ngày |
| **Total** | **10 PRs** | **~4.5 ngày** |

---

## Completion Status

| PR | Status | GitHub | Score |
|----|--------|--------|-------|
| PR-KC-1 Fix @Disabled tests | ✅ DONE | #199→#202 (Wave 2) | +1 |
| PR-KC-2 Integration tests | ✅ DONE | #199→#202 (Wave 2) | +1 |
| PR-KC-3 E2E test script | ⬜ TODO | — | +2 |
| PR-KC-4 Multi-tenant E2E | ⬜ TODO | — | +2 |
| PR-KC-5 Fix FE TODOs | ✅ DONE | #198→#202 (Wave 2) | +1 |
| PR-KC-6 README + docs | ⬜ TODO | — | +2 |
| PR-KC-7 Standalone Docker | ⬜ TODO | — | +2 |
| PR-KC-8 Validation hardening | ⬜ TODO | — | +1 |
| PR-KC-9 Onboarding UX | ⬜ TODO | — | +1 |
| PR-KC-10 Plan tracking | ⬜ TODO | — | +1 |
| **Total** | **3/10** | | **3/14** |
