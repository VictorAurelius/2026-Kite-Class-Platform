# Wave: Mock Data for Local Development

**Trạng thái:** 🟡 PLANNING — chờ user review
**Ngày tạo:** 2026-04-14
**Branch:** `wave/mock-data-local-dev`
**Mục tiêu:** FE và BE đều có mock/seed data sẵn ở local dev để làm việc không cần phụ thuộc vào nhau.

---

## 1. Scope Investigation

### 1.1 FE API Coverage (71 endpoints across 14 domains)

| Domain | Endpoints | Currently Mocked | Gap |
|--------|-----------|-----------------|-----|
| Authentication | 6 | 1 (refresh) | 5 |
| Students | 5 | 2 (GET list, GET id) | 3 |
| Teachers | 5 | 2 | 3 |
| Courses | 7 | 2 | 5 |
| Classes | 11 | 3 | 8 |
| Enrollments | 2 | 1 | 1 |
| Attendance | 10 | 3 (stats) | 7 |
| Invoices | 7 | 2 | 5 |
| Payments | 6 | 1 | 5 |
| Branding | 3 | 1 (GET) | 2 |
| Preferences | 2 | 1 (GET) | 1 |
| Public/Guest | 4 | 0 | 4 |
| Marketing | 1 | 0 | 1 |
| Tenant/Landing | 2 | 0 | 2 |
| **TOTAL** | **71** | **19 (27%)** | **52** |

**Kết luận:** Mock hiện tại chỉ cover 27% endpoints. Còn 52 endpoints chưa mock → **risk mock thiếu**.

### 1.2 BE Entity Coverage (36 entities, 21 controllers)

**Đầy đủ:** Student, Teacher, Course, Class, ClassSession, Enrollment, Invoice, InvoiceItem, InvoiceAdjustment, Installment, Payment, Attendance, StudentPoint, Assignment, Submission, Grade, GradeComponent, CourseModule, Lesson, LearningResource, LessonProgress, Branding, LandingPage, Lead, ContactMessage, UploadedFile, StorageQuota, UserPreferences, TeacherCourse, TeacherClass...

**Seed mechanism hiện tại:**
- ❌ Không có `DataSeeder`, `CommandLineRunner`, hay `@PostConstruct` loader
- ✅ Flyway migrations (V1-V27) — chỉ DDL, không có data seed thực sự
- V16 `seed_test_data.sql` → **rỗng**
- V19, V20 → chỉ seed landing page

**Kết luận:** BE **không có** seed data nào. Cần tạo từ đầu.

### 1.3 Foreign Key Dependency Chain

Seeding phải theo thứ tự vì foreign keys:

```
Tenant (instance_id UUID)
  ↓
Students + Teachers (độc lập)
  ↓
Courses (teacherId FK) → TeacherCourse
  ↓
Classes (courseId FK) → ClassSessions
  ↓
Enrollments (studentId + classId FK)
  ↓
Invoices (enrollmentId FK) → InvoiceItems, Payments
  ↓
Attendance (enrollmentId + sessionId FK)
  ↓
Assignments → Submissions → Grades → GradeComponents
  ↓
LMS: CourseModules → Lessons → Resources → LessonProgress
  ↓
StudentPoints (reference các entity trên)
```

---

## 2. Risks & Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Mock thiếu** — FE gọi endpoint chưa mock → fail silently | 🔴 HIGH | Fallback handler trả error rõ ràng thay vì empty |
| **Mock sai** — data shape không match BE → runtime errors khi connect backend | 🔴 HIGH | Generate mock types từ BE DTOs; validate shapes |
| **Multi-tenant complexity** — mọi entity có `instance_id` | 🟡 MEDIUM | Seed 1 tenant mặc định + hard-code UUID trong dev |
| **Auto-calc fields** — Invoice totals, Grade weights | 🟡 MEDIUM | Respect @PrePersist logic hoặc pre-compute trong seed |
| **State transitions** — Class lifecycle, Invoice status | 🟡 MEDIUM | Seed variety of states (SCHEDULED, IN_PROGRESS, COMPLETED) |
| **Payment gateway** — VNPay/MoMo credentials | 🟢 LOW | Mock webhook responses; không call real gateway |
| **FE mock vs BE seed divergence** | 🟡 MEDIUM | Shared fixture file generating both; OR auto-gen từ BE |

---

## 3. Acceptance Criteria

### 3.1 FE Local Dev với Mock API
- [ ] `npm run dev` với `NEXT_PUBLIC_MOCK_API=true` → tất cả pages render với data
- [ ] Toggle off (`NEXT_PUBLIC_MOCK_API=false`) → gọi backend thật
- [ ] 100% endpoints FE gọi đều có mock handler (không còn 52 gap)
- [ ] Mock data consistent giữa các pages (student ID 1 ở list = ID 1 ở detail)
- [ ] No console errors, no "N errors" dev overlay badge

### 3.2 BE Local Dev với Seed Data
- [ ] Spring profile `dev` → tự động seed data khi startup
- [ ] Spring profile `prod` → không seed
- [ ] Seed data cover: 1 tenant, 3 teachers, 10 students, 3 courses, 5 classes, 20 sessions, 15 enrollments, 10 invoices, 5 payments, attendance records
- [ ] Foreign keys + unique constraints không violation
- [ ] Rerunnable: Drop DB → migrate → seed → working state

### 3.3 Integration
- [ ] FE với `NEXT_PUBLIC_MOCK_API=false` + BE `SPRING_PROFILES_ACTIVE=dev` → UI hiện data thật từ BE
- [ ] Mock data shapes match BE response shapes (manual verification)

---

## 4. Sub-PR Breakdown

Wave này chia làm **4 PRs** tuần tự (mỗi PR là dependency của PR sau):

### PR A: BE OpenAPI Spec + Shared Fixtures
**Branch:** `feat/be-openapi-spec`
**Scope:**
- Thêm `springdoc-openapi-starter-webmvc-ui` vào `kiteclass-core`
- Expose OpenAPI spec tại `/v3/api-docs`
- Verify tất cả 21 controllers có schema đầy đủ
- Tạo shared fixtures file `shared/fixtures/mock-data.json` (tiếng Việt realistic)
- CI step: dump OpenAPI spec vào `kiteclass/shared/openapi.json`

**Estimate:** ~100 LOC Maven + config, shared JSON ~500 lines

### PR B: FE MSW Browser Mode — Full 71 Endpoints
**Branch:** `feat/fe-mock-browser-full-coverage`
**Dependencies:** PR A (shared fixtures)
**Scope:**
- Setup MSW browser worker (`public/mockServiceWorker.js`)
- Generate TypeScript types từ OpenAPI (`openapi-typescript`)
- Mock handlers cho **toàn bộ 71 endpoints**:
  - Auth (6): login, logout, refresh, forgot-password, reset-password, verify-email
  - Students (5), Teachers (5), Courses (7), Classes (11)
  - Enrollments (2), Attendance (10), Invoices (7), Payments (6)
  - Branding (3), Preferences (2), Public (4), Marketing (1), Tenant (2)
- Toggle: `NEXT_PUBLIC_MOCK_API=true` → MSW active, `false` → real backend
- Dynamic import MSW → không ảnh hưởng production bundle
- Consistent state: mock "database" in-memory, CRUD operations persist trong session
- Test: `npm run dev` → browse tất cả 36 pages với data

**Estimate:** ~800 LOC mock handlers (auto-gen phần lớn), ~150 LOC setup

### PR C: BE DataSeeder — Full 36 Entities với Spring Profile `dev`
**Branch:** `feat/be-dataseeder-full`
**Dependencies:** PR A (shared fixtures)
**Scope:**
- Tạo `application-dev.yml` profile
- `DataSeeder` component (@Profile("dev") @Component CommandLineRunner)
- Seed theo FK dependency order (xem §1.3):
  1. Tenant (1 UUID cố định)
  2. Teachers (3), Students (10)
  3. Courses (3), TeacherCourse assignments
  4. CourseModule (6), Lesson (18), LearningResource (12) — LMS structure
  5. Classes (5), TeacherClass, ClassSessions (30+)
  6. Enrollments (15)
  7. Invoices (10), InvoiceItems, InvoiceAdjustments
  8. InstallmentPlans, Installments, Payments (5), RefundRequests
  9. Attendance records (60+)
  10. Assignments (5), Submissions, Grades, GradeComponents
  11. LessonProgress, StudentPoints
  12. Branding, UserPreferences, LandingPage, Leads, ContactMessages
- Idempotent: check `count()` trước khi seed (skip nếu có data)
- Load data từ shared fixtures file (PR A)
- Respect business rules: @PrePersist calc totals, weight sum=100%, FK constraints

**Estimate:** ~600-800 LOC Java seeder, ~80 LOC config

### PR D: Docs + Integration Smoke Test
**Branch:** `docs/local-dev-mock-data`
**Dependencies:** PR B + PR C
**Scope:**
- `documents/05-guides/local-dev-mock-data.md` — hướng dẫn setup FE + BE
- Integration smoke test: start BE `--spring.profiles.active=dev`, FE `NEXT_PUBLIC_MOCK_API=false` → verify data flows end-to-end
- Update `CLAUDE.md`: document mock/seed system
- Update `documents/01-business/README.md`: link sang guide
- Verification script: check OpenAPI spec match FE mock types (`npm run verify:openapi`)

**Estimate:** ~300 LOC docs, 1 smoke test script, 1 verification script

---

## 5. Decisions (theo phương châm: toàn bộ + best practice)

**Phương châm user:** (1) làm toàn bộ phạm vi, không bỏ qua; (2) làm theo best practice.

### 5.1 Shared fixtures: **OpenAPI-first (best practice)**
- BE expose OpenAPI spec (springdoc-openapi) → single source of truth cho schemas
- FE mock handlers generate từ OpenAPI (openapi-typescript hoặc orval)
- Mock data fixtures dùng chung JSON file, load cả FE mock + BE seed
- **Ưu điểm:** zero divergence, refactor BE → FE mock tự update
- **Effort:** +1 PR cho OpenAPI setup nhưng long-term value cao

### 5.2 Scope BE seed: **FULL 36 entities**
- Seed đầy đủ: Students, Teachers, Courses, Classes, Sessions, Enrollments, Invoices, Payments, Attendance, Assignments, Submissions, Grades, GradeComponents, LMS Modules, Lessons, Resources, LessonProgress, StudentPoints, TeacherCourse, TeacherClass, Branding, Preferences, LandingPage, Lead, ContactMessage, InstallmentPlan, Installment, PaymentWebhookLog, RefundRequest, UploadedFile, StorageQuota...
- Realistic data (tiếng Việt): 3 teachers, 10 students, 3 courses với modules/lessons, 5 classes với 20+ sessions, 15 enrollments với invoices + payments + attendance records, 5 assignments với submissions + grades

### 5.3 Mock toggle: **Env var `NEXT_PUBLIC_MOCK_API` (best practice)**
- Toàn app level — đơn giản, rõ ràng
- `.env.local` cho dev, `.env.production` không có → prod không bao giờ dùng mock
- Bundle size impact: MSW chỉ load khi mock=true (dynamic import)

### 5.4 Phạm vi KiteHub: **TÁCH WAVE RIÊNG**
- Wave này: **KiteClass FE + BE** (đã đủ lớn: 71 endpoints + 36 entities)
- Wave sau: **KiteHub** (6 services + gateway + frontend) — scope tương đương
- Lý do: gộp chung quá lớn (>100 endpoints, 80+ entities), khó review, khó rollback
- Note trong doc: reference sang wave KiteHub khi tạo

---

---

## 6. Execution Plan

1. ⏳ User approve plan này (decisions §5 đã theo phương châm toàn bộ + best practice)
2. 🆕 Tạo branch `wave/mock-data-local-dev`
3. 🆕 PR A: BE OpenAPI spec + shared fixtures
4. 🆕 PR B: FE MSW browser mode — full 71 endpoints
5. 🆕 PR C: BE DataSeeder — full 36 entities
6. 🆕 PR D: Docs + integration smoke test
7. ✅ Quality check toàn wave trước khi merge
8. ✅ Squash merge wave → main

**Ước tính:** 3-5 ngày công. Tuân thủ Superpowers methodology (brainstorm → TDD → review) cho mỗi PR.

**Acceptance cuối wave:**
- [ ] FE `npm run dev` với mock → 36 pages render đầy đủ, 0 errors
- [ ] BE `--spring.profiles.active=dev` → DB có realistic data cho 36 entities
- [ ] FE + BE connect thật → data shape match, no runtime errors
- [ ] OpenAPI spec lint pass, FE types generated đồng bộ

---

## 7. Future Wave

**Wave: KiteHub Mock Data** (tách riêng sau wave này)
- Scope: 6 KiteHub services + gateway + frontend
- Ước tính: tương đương wave này (71+ endpoints, 80+ entities)
- Tạo plan doc riêng: `documents/03-planning/wave-mock-data-kitehub.md`

---

## 8. Log

- **2026-04-14 T0** — Wave plan created. Investigation complete (71 FE endpoints, 36 BE entities audited).
- **2026-04-14 T1** — User xác định phương châm: (1) toàn bộ phạm vi, (2) best practice. Plan updated với OpenAPI-first approach, full 36 entities seed, 4 PRs thay vì 3. Waiting user approve trước khi tạo wave branch.
