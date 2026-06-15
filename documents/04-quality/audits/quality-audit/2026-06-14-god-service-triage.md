# God-Service Triage — GAP-1346 (Quality full audit 2026-06-14, Cat 9)

**Ngày:** 2026-06-15
**Scope:** 11 backend service class > 20KB bị audit flag God-Service candidate.
**Trạng thái GAP-1346:** PARTIAL — triage (đo line + method count + đề xuất split) DONE; refactor thực sự DEFER sang wave riêng (không tách trong PR này, theo chỉ đạo — refactor 11 class là việc lớn, rủi ro cao).
**Ngưỡng God-Service per `.claude/rules/design-patterns.md` §3:** > 500 dòng **HOẶC** > 15 method.

---

## 1. Đo lường 11 class (line + method count)

Method count đo bằng grep signature `(public|protected|private) ... (...) {` — xấp xỉ; cột "public" là API bề mặt.

| # | Class | Module | Dòng | KB | ~Method (public) | Vi phạm ngưỡng? |
|---|---|---|---:|---:|---|---|
| 1 | `EmailServiceClient` | kitehub-subscription | **1105** | 47 | ~14 (9) | ✅ dòng (>500) |
| 2 | `AuthService` | kitehub-subscription | **907** | 43 | ~18 (11) | ✅ dòng + method |
| 3 | `LmsServiceImpl` | kiteclass-core | **796** | 35 | ~24 (17) | ✅ dòng + method |
| 4 | `SubscriptionService` | kitehub-subscription | **700** | 34 | ~14 (11) | ✅ dòng |
| 5 | `InstanceService` | kitehub-subscription | **659** | 27 | ~17 (14) | ✅ dòng + method |
| 6 | `BetaAccessService` | kitehub-subscription | **643** | 31 | ~21 (12) | ✅ dòng + method |
| 7 | `GradeServiceImpl` | kiteclass-core | **633** | 26 | ~21 (17) | ✅ dòng + method |
| 8 | `ClassServiceImpl` | kiteclass-core | **625** | 24 | ~18 (14) | ✅ dòng + method |
| 9 | `CourseServiceImpl` | kiteclass-core | **590** | 25 | ~14 (10) | ✅ dòng |
| 10 | `PaymentService` | kitehub-subscription | **560** | 24 | ~13 (11) | ✅ dòng |
| 11 | `AttendanceServiceImpl` | kiteclass-core | **500** | 22 | ~8 (7) | ⚠️ biên (đúng 500 dòng, 8 method) |

**Kết luận đo:** 10/11 vi phạm rõ (> 500 dòng); `AttendanceServiceImpl` biên (500 dòng / 8 method — ít trách nhiệm trộn nhất, **ưu tiên thấp nhất**, có thể chấp nhận tạm).

---

## 2. Kế hoạch tách — Top 3 ưu tiên (AC ≥3)

### 2.1 `EmailServiceClient` (1105 dòng) — fan-out client

24 method `send*` (sendTrialExpirationWarning, sendTrialExpired, sendRenewalReminder, sendSuspensionNotification, sendWelcomeEmail, sendTenantReadyEmail, sendDsar*, sendBetaInviteEmail, sendInviteStaffEmail, sendTemplatedEmail, …).

- **Bản chất:** mỗi method là wrapper mỏng dựng payload + gọi `sendTemplatedEmail`. "Lớn" do FAN-OUT số loại email, không phải logic trộn → SRP vi phạm NHẸ hơn các service khác.
- **Đề xuất split:** tách theo domain email → `SubscriptionLifecycleEmailClient` (trial/renewal/suspension/retention) + `DsarEmailClient` (DSAR/data-rights) + `OnboardingEmailClient` (welcome/tenant-ready/beta-invite/staff-invite), tất cả delegate 1 `TemplatedEmailSender` core (transport + template resolve).
- **Rủi ro:** THẤP — chỉ di chuyển method, không đổi logic; caller inject client con tương ứng. Test theo nhóm dễ cô lập hơn.

### 2.2 `AuthService` (907 dòng) — multi-concern auth

Method: validateConfig, register, registerFromBetaInvite, verifyEmail, resendVerification, login (×2 overload), refresh, logout, updateProfile, changePassword + private helper.

- **Bản chất:** trộn 4 concern: (a) đăng ký/xác thực email, (b) đăng nhập/refresh/logout (session), (c) profile update, (d) đổi mật khẩu. SRP vi phạm RÕ.
- **Đề xuất split:** `RegistrationService` (register + registerFromBetaInvite + verifyEmail + resendVerification) + `AuthenticationService` (login + refresh + logout — giữ tên cốt lõi) + `AccountProfileService` (updateProfile + changePassword). Token-mint helper tách `JwtTokenIssuer` nếu chưa có.
- **Rủi ro:** TRUNG BÌNH — auth là path nhạy cảm; cần re-walk login/register/refresh sau tách (per `pre-handoff-self-test-completeness.md` §2.1). Tách từng concern 1 PR.

### 2.3 `LmsServiceImpl` (796 dòng) — module/lesson/resource/completion + 3 read-persona

Method: getCourseStructurePublic/ForStudent, getLesson{Public,ForStudent,ForTeacher}, createModule/updateModule/deleteModule/getModule, createLesson/updateLesson/deleteLesson, addResource/deleteResource, reorderModules/reorderLessons, generateResourceUploadUrl, getCompletionRoster.

- **Bản chất:** trộn 4 concern: (a) Module CRUD + reorder, (b) Lesson CRUD + reorder, (c) LearningResource (add/delete/presigned-upload), (d) Completion roster + 3 read-path theo persona (public/student/teacher). SRP vi phạm RÕ + 24 method.
- **Đề xuất split:** `LmsModuleService` (module CRUD + reorder) + `LmsLessonService` (lesson CRUD + reorder + read-by-persona) + `LearningResourceService` (resource + presigned upload) + `LmsCompletionService` (completion roster). Read-by-persona gom 1 `LmsReadFacade` hoặc giữ trong từng service con với guard riêng.
- **Rủi ro:** TRUNG BÌNH — nhiều endpoint LMS phụ thuộc; cần re-walk teacher LMS flow sau tách.

---

## 3. Còn lại (ưu tiên sau Top 3)

| Class | Hướng tách gợi ý |
|---|---|
| `SubscriptionService` (700) | tách `SubscriptionLifecycleService` (state-machine trial→active→suspended→expired) khỏi `SubscriptionQueryService` (read/list/health) |
| `InstanceService` (659) | tách provisioning saga handler khỏi instance CRUD/query |
| `BetaAccessService` (643) | tách `BetaSignupService` (exchange/complete/rollback) khỏi `BetaModerationService` (approve/reject/list) + honeypot guard policy object |
| `GradeServiceImpl` (633) | tách gradebook compute/aggregate khỏi grade-entry CRUD |
| `ClassServiceImpl` (625) | tách scheduling/reschedule khỏi class CRUD |
| `CourseServiceImpl` (590) | tách enrollment-related khỏi course CRUD |
| `PaymentService` (560) | tách payment state-machine + reconciliation khỏi payment CRUD/query |
| `AttendanceServiceImpl` (500) | **ưu tiên thấp nhất** — biên ngưỡng; chấp nhận tạm, theo dõi |

---

## 4. Deferral + AC

- **Refactor DEFER** sang các wave chuyên biệt (mỗi service-cluster 1 wave, ưu tiên Top 3). Lý do: tách 11 class là MAJOR refactor đa-module, rủi ro regression cao trên auth/payment/LMS path; vượt scope PR build-config/triage này. Per `meta-gap-priority.md` các refactor này là Feature-tier tech-debt, không chặn Phase 1 BETA.
- AC GAP-1346:
  - [x] Đo line + method count cho 11 class; xác nhận 10/11 vi phạm rõ, 1 biên.
  - [x] ≥3 class lớn nhất có kế hoạch tách collaborator theo design-patterns.md §3 (EmailServiceClient / AuthService / LmsServiceImpl ở §2).
  - [ ] (DEFER) Quality audit Cat 9 verify lại sau refactor — chờ refactor wave.
- **GAP-1346 giữ PARTIAL.** Refactor tracking ở chính GAP-1346 (Resolution ghi "triage done, refactor deferred").
