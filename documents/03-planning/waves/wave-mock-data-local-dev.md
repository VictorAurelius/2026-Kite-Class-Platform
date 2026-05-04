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
- `documents/05-guides/local-dev/local-dev-mock-data.md` — hướng dẫn setup FE + BE
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

## 7. AI Branding Workflow Mock (per GAP-014, **v2-aligned 2026-04-26**)

> **Module-location note (verified 2026-04-26):** AI Branding v2 implementation shipped to **`kiteclass/kiteclass-core/`** — NOT `kitehub-branding/` as the original architecture doc specified. Class renames: `BrandingAnalyzer → AnalyzerService`, `BrandingPlanner → PlannerService`, `BrandingExecutor → PlanExecutor` (internal services, NOT REST endpoints). See GAP-016 + GAP-234 for architecture doc drift.

### 7.1 v2 AI Branding Endpoints — ACTUAL controllers in kiteclass-core (10 endpoints, verified 2026-04-26)

Inventoried directly from real source controllers — `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/`:

| Category | Endpoint | Method | Controller | Purpose |
|----------|----------|:------:|------------|---------|
| Instance create | `/api/v1/instances` | POST | `InstanceController` | Create FrontendInstance (status=NOT_STARTED) |
| Instance get | `/api/v1/instances/{id}` | GET | `InstanceController` | Single instance + lifecycle status |
| Instance list | `/api/v1/instances` | GET | `InstanceController` | List instances |
| Lifecycle: init done | `/api/v1/instances/{id}/infrastructure-ready` | POST | `InstanceController` | NOT_STARTED → INITIALIZING → GENERATING |
| Lifecycle: branding done | `/api/v1/instances/{id}/branding-completed` | POST | `InstanceController` | GENERATING/REGENERATING → DEPLOYED (score gate ≥70) |
| Lifecycle: rebrand | `/api/v1/instances/{id}/rebrand` | POST | `InstanceController` | DEPLOYED → REGENERATING |
| Lifecycle: fail | `/api/v1/instances/{id}/failed` | POST | `InstanceController` | Any → FAILED |
| Lifecycle: retry | `/api/v1/instances/{id}/retry` | POST | `InstanceController` | FAILED → INITIALIZING |
| Branding package | `/api/v1/branding/{instanceId}/package` | GET | `BrandingPackageController` | Composite theme + assets (cached, ETag) |
| Branding public | `/api/v1/branding/public` | GET | `PublicBrandingController` | Public-facing branding fetch |
| Internal webhook | `/internal/notify/instance-deployed` | POST | `InternalWebhookController` | Cross-service notify on deploy |

**Internal services (NOT REST — invoked from Saga / Step pipeline):**
- `AnalyzerService.analyze()` — produces `AnalysisResult` from BrandingContext
- `PlannerService.generatePlan()` — produces `BrandingPlan`
- `PlanExecutor.execute()` — runs Steps with fallback
- `InstanceQualityReviewer.review()` — runs 5 quality checks → score `/100`
- `ContentModerationService.moderate()` — 3-stage pipeline → ModerationStatus
- `TenantProvisioningSaga` — orchestrates tenant.created → instance create → branding generate

These are NOT mocked at HTTP layer (no controller). FE doesn't call them directly. Mock at the lifecycle endpoint level (POST infrastructure-ready / branding-completed) which simulates the saga effect.

**Endpoints NOT in v2 backend (FE-only / TBD):**
- Wizard draft autosave (`/api/v1/branding/wizard/draft`) — FE state machine handles this client-side via `wizard-machine.ts`; no BE endpoint
- Templates gallery (`/api/v1/templates`) — Sprint 0 GAP-011 deliverable, no controller yet
- Quality reports (`/api/v1/branding/quality-reports/{id}`) — TBD GAP-012 follow-up; currently surfaced via instance status payload
- Approval per resource — TBD via `/api/v1/branding/rebrand-approvals` (GAP-070 placeholder)

### 7.2 Lifecycle Transitions (mocked state machine, v2-aligned)

Real flow per `FrontendInstanceStatus` (6 states):

```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
                  ↓              ↓          ↑
                FAILED ←────── FAILED ──────┘ (retry)
```

Mock simulation (FE MSW):

```
1. POST /api/v1/instances              → status=NOT_STARTED, return id
2. POST /api/v1/instances/{id}/infrastructure-ready
                                       → wait 1s → status=INITIALIZING → 2s → status=GENERATING
3. POST /api/v1/instances/{id}/branding-completed (with mock QualityReport.score=85)
                                       → wait 2s → status=DEPLOYED (gate passes if score ≥ 70)
4. GET /api/v1/branding/{id}/package   → returns mock theme + 6 asset URLs
5. POST /api/v1/instances/{id}/rebrand → status=REGENERATING; loop step 3 again
6. POST /api/v1/instances/{id}/failed  → terminal FAILED state
7. POST /api/v1/instances/{id}/retry   → FAILED → INITIALIZING (loop step 2)
```

### 7.3 Sub-PR E: FE MSW — v2 AI Branding Lifecycle (~10 mocks)

**Branch:** `feat/fe-mock-ai-branding-v2`
**Dependencies:** PR A (OpenAPI v2 export from kiteclass-core), PR B (FE mock base)
**Scope:**
- Mock 10 v2 endpoints listed in §7.1 (all from real controllers)
- In-memory state machine: each mock instance keeps current `status`, transitions persisted in mock DB
- Simulated delays per §7.2 (1-2s)
- Mock quality score: 85 (deterministic) → DEPLOYED passes; scenario toggle for <70 → FAILED
- Mock branding package returns theme JSON + 6 placeholder asset URLs
- FE wizard (`BrandingWizard.tsx`) uses mocks end-to-end, no real BE

### 7.4 Sub-PR F: BE DataSeeder — kiteclass-core branding tables

**Branch:** `feat/be-dataseeder-branding-v2`
**Dependencies:** PR C (KiteClass seeder foundation)
**Scope:**
- Seed 1 sample `FrontendInstance` (status=DEPLOYED, brandingVersion=1)
- Seed 1 sample `BrandingResource` per category (STATIC/TEMPLATE/FULL_AI = 3 rows)
- Seed 1 sample `QualityReport` with score=85 + 5 mock issues
- Seed 1 sample `OutboxEvent` (`branding.updated`) — not yet dispatched
- Skip `branding_templates` seed until GAP-011 lands template entity (currently no `ImageTemplate` table)
- Respect FK order: Instance → BrandingResource → QualityReport → OutboxEvent
- `@Profile("dev")` only — guarded by `DATABASE_LIFECYCLE_ENABLED=true` per `instance-provisioning/rules.md` INS-14

### 7.5 Demo Flow (v2-aligned)

Local dev full flow (no AI model needed):

```
1. Login tenant admin (mock auth via existing handlers.ts)
2. Onboarding wizard → "Tạo thương hiệu AI" link
3. BrandingWizard.tsx 6-step flow (real component, mock backend)
4. Step 4: pick audience/tone (constrained presets per ai-branding-guidelines.md §2.1)
5. Step 5: pick template từ 6 mock options (templates seeded via GAP-011 stub data)
6. Step 6: preview + approve per-resource
7. Click Deploy → POST /api/v1/instances/{id}/infrastructure-ready
   → Lifecycle animation: INITIALIZING → GENERATING (2s) → DEPLOYED
8. Redirect to tenant instance với mock branding applied (theme CSS vars)
9. Click Regenerate banner → POST /api/v1/instances/{id}/rebrand
   → Lifecycle: REGENERATING → DEPLOYED (loop)
10. View QualityReport panel: score=85, 5 sample issues (mock)
```

### 7.6 Acceptance (extension của §3)

- [ ] 10 v2 endpoints mocked (per real §7.1 inventory) — NOT 12 aspirational paths
- [ ] Lifecycle 6-state transitions simulated với realistic delays
- [ ] DataSeeder seeds 1 sample DEPLOYED instance + 3 BrandingResources + 1 QualityReport
- [ ] Demo flow runs end-to-end locally without `OllamaClient` invocation (verify via log assertion: 0 Ollama calls)
- [ ] Screenshots captured all wizard steps + lifecycle states
- [ ] No real AI model calls required (`MockAIClient` profile active)
- [ ] DataSeeder respects FK order: instance → resource → report
- [ ] OpenAPI spec exported from kiteclass-core covers v2 endpoints

### 7.7 Out-of-scope (track separately)

| Item | Why deferred | Tracking |
|------|--------------|----------|
| `ImageTemplate` entity + 30 template seeds | Sprint 0 designer work | GAP-011 |
| Regenerate counter UI + tier limits enforcement | Per-tier rate limit logic | GAP-005 Phase 2 |
| Real wizard draft persistence (server-side) | Currently FE-only via wizard-machine.ts | GAP-020 |
| `quality-reports/{id}` REST endpoint | Currently surfaced via instance status payload only | GAP-012 follow-up |
| Approval per resource REST | TBD endpoints | GAP-070 placeholder |
| MixSura / Gemma 4 9B real model swap | Infra-blocked | GAP-006 |

---

## 8. Future Wave

**Wave: KiteHub Mock Data** (tách riêng sau wave này)
- Scope: 6 KiteHub services + gateway + frontend
- Ước tính: tương đương wave này (71+ endpoints, 80+ entities)
- Tạo plan doc riêng: `documents/03-planning/wave-mock-data-kitehub.md`

---

## 9. Log

- **2026-04-26 (GAP-014 v2 alignment):** §7 rewritten end-to-end against shipped v2 controllers verified live in `kiteclass-core`. Replaced 12 aspirational endpoints with 10 real ones from `InstanceController` (8) + `BrandingPackageController` (1) + `PublicBrandingController` (1) + `InternalWebhookController` (1). Removed wizard/templates/quality-reports endpoints (don't exist in BE — see §7.7 deferred list). Internal services (Analyzer/Planner/Executor/QualityReviewer/ContentModeration/TenantProvisioningSaga) called out as non-REST. Sub-PR E + F rewritten to target `kiteclass-core` (NOT `kitehub-branding`) per architecture doc drift. Added §7.7 Out-of-scope with 6 deferred items linked to existing gaps. GAP-014 status PARTIAL (planning v2-aligned DONE) — implementation portion split to GAP-235.
- **2026-04-14 T1** — User xác định phương châm: (1) toàn bộ phạm vi, (2) best practice. Plan updated với OpenAPI-first approach, full 36 entities seed, 4 PRs thay vì 3. Waiting user approve trước khi tạo wave branch.
- **2026-04-14 T0** — Wave plan created. Investigation complete (71 FE endpoints, 36 BE entities audited).
