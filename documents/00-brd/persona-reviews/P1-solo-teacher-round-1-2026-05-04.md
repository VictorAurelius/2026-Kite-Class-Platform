---
title: P1 Solo Teacher — Persona Review Round 1
status: draft
persona_id: P1
persona_name: Solo Teacher (Gia sư tự do)
tier: 1 Primary
review_date: 2026-05-04
reviewer: Wave 17 Bucket A Agent (autonomous)
ac_source: documents/00-brd/persona-criteria/P1-solo-teacher.md
gap_range_reserved: GAP-286..295
---

# P1 Solo Teacher — Persona Review Round 1 (2026-05-04)

**Status:** 🟡 DRAFT — closure PR (Wave 17 closure step) sẽ flip sang `approved`
**Wave:** 17 — Persona Review Round 1 (GAP-152)
**Bucket:** A (P1)
**Methodology:** [`persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
**Source AC:** [`P1-solo-teacher.md`](../persona-criteria/P1-solo-teacher.md) — 29 ACs across 6 categories
**Filing pipeline:** [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md)

---

## §0 Executive Summary

| Metric | Value |
|---|---|
| Total ACs reviewed | 29 |
| PASS | 7 (24.1%) — incl. 2 PASS-by-design (out-of-scope features correctly absent) |
| PARTIAL | 7 (24.1%) |
| FAIL | 15 (51.7%) |
| Coverage score | **36.2/100** = (7 + 0.5×7) / 29 × 100 |
| Verdict | 🔴 **Persona NOT supported (major gaps; not production-ready)** |
| New gaps filed | **10** in reserved range GAP-286..295 |
| Overflow (extension needed) | **3** (mobile offline PWA, student progress PDF, account pause) — closure PR allocate extension range |

**Top 3 critical findings:**
1. **Mobile-first signup blocker (AC-ONBOARD-001/002 FAIL):** KHÔNG có OTP qua Zalo/SMS, KHÔNG có skip option ở branding wizard. Solo persona không thể onboard ≤10 phút trên phone — bottleneck #1 cho FREE tier acquisition.
2. **Notification infrastructure missing (5 ACs FAIL: AC-OPS-006/007, AC-COMM-001/002/003 + AC-FIN-002/004 PARTIAL):** KHÔNG có Zalo/SMS messaging service (chỉ EmailService). Class reschedule/cancel/reminder không gửi tự động → solo teacher phải duplicate work qua Zalo native app. GAP-063 đã OPEN nhưng chưa fix; 5+ ACs depend on it.
3. **Recurring class generator missing (AC-OPS-002 FAIL):** `Class.schedule` plain string, KHÔNG có RRULE generator. Tutoring use case dominant = weekly recurring (Tuesday-Thursday 19:00-20:30 × 12 weeks) nhưng phải tạo manual từng buổi → blocks daily ops cho cả P1, P2, P3, P5.

**Priority-reordering recommendation:**
- Bump GAP-063 (Zalo/SMS notification) từ P1 → **P0** — blast radius >5 ACs across 3 personas (verify trong Bucket B/C/D reports)
- Sequence Wave 18: GAP-286 (mobile OTP) + GAP-287 (skip wizard) + GAP-290 (recurring class) + GAP-063 — these unblock 60% of P1 FAIL ACs in single wave
- Defer GAP-288 (tour) + GAP-289 (quick-add UX) đến Wave 19+ — UX polish, không blocker
- Coordinate với P2/P3 reviews trong closure PR để dedupe gaps (GAP-290 RRULE likely shared finding)

---

## §1 Scenario at scale

Theo `personas-catalog.md` §P1 + AC §0:

- **Actor:** 1 gia sư tiếng Anh part-time tại TPHCM, 30 học sinh, 3 courses (English-Beginner / English-Intermediate / English-IELTS)
- **Hierarchy:** Flat — 1 teacher = owner = operator = billing person. Không có admin staff. Phụ huynh KHÔNG đăng nhập (chỉ là contact qua Zalo/SMS).
- **Device profile:** iPhone (Safari) làm primary; Android (Chrome) làm backup. Không có laptop riêng cho công việc dạy thêm.
- **Usage pattern:** Mobile-first 70%+ thời gian. Peak hours weekday 17:00-21:00 + cuối tuần. Lessons 1-on-1 hoặc nhóm ≤5 students.
- **Tier:** FREE (5-15 students) → PRO (15-50 students). Sub-200K VND/month preferred. Không bao giờ chạm PREMIUM/ENTERPRISE.
- **Communication channel:** Zalo + SMS primary; email secondary; phụ huynh nhận receipt PDF qua Zalo link.
- **Billing:** Cash (60%) + bank transfer (40%). Không cần e-invoice (mã số thuế). Per-session pricing (200K/buổi × 8 buổi = 1.6M/tháng).

**Critical concerns** (từ AC §0):
1. Setup ≤30 phút onboarding
2. Mobile-friendly toàn diện
3. Sub-200K VND/month FREE tier useable
4. Simple invoicing (PDF receipt qua Zalo, không e-invoice)
5. Zalo/SMS-first (không email-first)

---

## §2 Journey walk-through

End-to-end role-play: discovery → signup → provisioning → daily ops → financial → communication → edge case → termination.

### 2.1 Discovery

Solo teacher tìm thấy KiteClass qua:
- Google search "phần mềm quản lý lớp học gia sư"
- Facebook group giáo viên dạy thêm TPHCM
- Word-of-mouth từ giáo viên khác

→ Landing page (KiteHub marketing site) cần (a) explain "miễn phí cho ≤15 students", (b) demo screenshots mobile-first, (c) pricing minh bạch trước khi signup. Nếu landing toàn enterprise/multi-tenant copy → bounce ngay.

### 2.2 Signup + provisioning

(AC-ONBOARD-001..004) — Mở landing trên iPhone Safari → "Sign up" → email + phone + tên → role "Solo Teacher" → OTP qua Zalo/SMS → confirm → wizard branding (skip được) → dashboard ready.

Target: ≤10 phút từ click "Sign up" tới dashboard. KHÔNG force AI branding flow.

### 2.3 Daily ops

(AC-OPS-001..008) — Schedule lesson, recurring class (Tuesday-Thursday 19:00-20:30), mark attendance trên phone (≤2 phút cho 10 students), nhập grades, track student progress, reschedule, cancel, quick-add student mid-course.

Tất cả thao tác phải work hoàn toàn trên mobile. Tap-target ≥44pt. Offline-capable cho mark attendance.

### 2.4 Financial

(AC-FIN-001..005) — Per-session pricing (200K/buổi). PDF receipt gửi qua Zalo link. Monthly income summary (thu / outstanding / chi). Reminder cho học sinh chưa đóng. KHÔNG hiển thị payroll/teacher commission menu (irrelevant).

### 2.5 Communication

(AC-COMM-001..004) — Zalo/SMS template predefined. Auto-reminder 1h trước class. Cancel/reschedule notify ngay. KHÔNG có parent portal (parent là contact, không phải user).

### 2.6 Edge cases

(AC-EDGE-001..005) — No-show vs excused-absent differentiation. Late-cancel <2h policy (charge full/partial/waive). Payment dispute audit log. Offline attendance sync. Account survive mobile-uninstall.

### 2.7 Exit / termination

(AC-EXIT-001..003) — Student progress export PDF. Account pause/resume (3-state lifecycle: Active → Paused 30d free → Archived). Self-service export toàn bộ data (xlsx + PDF zip).

---

## §3 AC Scoring (29 ACs)

Format: AC-ID | Status (PASS/PARTIAL/FAIL) | Evidence | Linked gap

### 3.1 Onboarding (4 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-ONBOARD-001 | 🔴 FAIL | Auth flow ở `kiteclass-frontend/src/app/(auth)/register/` chỉ có email/password — KHÔNG có OTP qua Zalo/SMS. `kiteclass-core` grep `OTP\|sms.?verify` = 0 hits ngoài ParentInvitationController (parent invite, không phải tenant signup). Tenant provisioning trong `kitehub-subscription` async (TRIAL state created → instance provisioning takes minutes, not <10 phút wall to dashboard ready). | NEW: GAP-286 (Mobile OTP signup via Zalo/SMS) |
| AC-ONBOARD-002 | 🔴 FAIL | `BrandingWizard.tsx` + `wizard-machine.ts`: grep `Skip\|skip` = 0 hits. Wizard XState force-flows qua all steps. KHÔNG có "Skip / Use default" button. Solo teacher buộc phải hoàn tất 6-step AI branding hoặc abandon. | NEW: GAP-287 (Skip/default option in branding wizard) |
| AC-ONBOARD-003 | 🟡 PARTIAL | `students/new/page.tsx` + `courses/new/page.tsx` exist, FE forms tồn tại. Nhưng required fields cho Student entity bao gồm parent contact + DOB (kiteclass-core/module/student) — không có "skip optional for adult students" path. Time-to-first-course có thể >15 phút do nhiều fields required. | Related GAP-051 (bulk import — không trực tiếp giải) |
| AC-ONBOARD-004 | 🔴 FAIL | KHÔNG có onboarding tour component. `find -type d -iname "*tour*\|*onboard*"` không trả file nào. Dashboard layout không inject tour. | NEW: GAP-288 (First-login onboarding tour highlighting solo-teacher core features) |

### 3.2 Daily Operations (8 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-OPS-001 | 🟡 PARTIAL | `classes/new` tồn tại nhưng form full-featured (max_students, location_type, dates, code_expires_at...). Mobile click count >5 cho create lesson session vì entity mix Class + ClassSession. Mobile responsive nhưng KHÔNG mobile-optimized. | NEW: GAP-289 (Quick-add lesson UI for mobile <5 clicks) |
| AC-OPS-002 | 🔴 FAIL | `Class.java` chỉ có `schedule` plain string ("Mon-Wed-Fri 18:00-20:00"). KHÔNG có RRULE generator. `ClassSession.java` exists nhưng phải tạo từng buổi manual. Grep `recurring\|RRULE\|recurrence` = 0 hits. | NEW: GAP-290 (Recurring class generator — RRULE/multi-day weekly) |
| AC-OPS-003 | 🟢 PASS | `attendance/page.tsx` + `dynamic-attendance-form-list.tsx` + `Attendance.java` entity exist. AttendanceStatus enum (PRESENT/ABSENT/LATE/EXCUSED). FE form supports per-session bulk mark. Tap target h-12 acceptable. Mobile-friendly. | — |
| AC-OPS-004 | 🟢 PASS | `module/grade/` full module exists (entity, service, controller). Free-form grade input. Không force rubric/weighted scheme. Out-of-scope MOET report card (GAP-055) đúng theo spec. | GAP-055 (out-of-scope, gated correctly) |
| AC-OPS-005 | 🟡 PARTIAL | `students/[id]/page.tsx` exists + `students/[id]/attendance` subpage exists, nhưng grep `attendanceRate\|attendancePct` ở student profile = 0 hits. Profile show student fields, không show attendance % summary + last 5 grades inline. Phải navigate sub-pages. | Related to GAP-289 |
| AC-OPS-006 | 🔴 FAIL | `Class.java` có `canCancel()` + `cancelled_at`, nhưng KHÔNG có reschedule action/endpoint. Grep `reschedule\|RESCHEDULED` = 0 hits trong kiteclass-core. KHÔNG có Zalo/SMS notification (xem AC-COMM-001..003). | GAP-063 (notify) + NEW: GAP-291 (Reschedule lesson endpoint + state) |
| AC-OPS-007 | 🟡 PARTIAL | Cancel logic exists (`canCancel` + `cancelled_at`), nhưng KHÔNG có notification gửi tự động qua Zalo/SMS — chỉ email service exists. | GAP-063 |
| AC-OPS-008 | 🟢 PASS | `enrollment/` module exists, students có thể add mid-course không reset attendance history (`Attendance` entity link FK class_id + student_id, không cascade). | GAP-051 (related, bulk import optional) |

### 3.3 Financial / Admin (5 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-FIN-001 | 🔴 FAIL | `Course.java` chỉ có `price BigDecimal` (single price field). KHÔNG có `pricingModel` enum (per_session vs monthly_subscription vs course_package). Grep `PerSession\|per_session\|pricingModel` = 0 hits. Solo teacher KHÔNG thể setup 200K/buổi billing model. | GAP-185 (related) + NEW: GAP-292 (Per-session pricing model) |
| AC-FIN-002 | 🟡 PARTIAL | `InvoiceRenderer.java` + `PdfGenerator.java` exist — invoice PDF generation works. Receipt # + URL fields trên Payment entity. Nhưng KHÔNG có "Send via Zalo" 1-click button (no Zalo messaging integration). Teacher phải manual download PDF + share Zalo. | GAP-063 (Zalo integration) |
| AC-FIN-003 | 🔴 FAIL | `billing/page.tsx` chỉ là invoice list table với pagination. KHÔNG có monthly summary view (Tháng → Thu/Outstanding/Net). Grep `incomeSummary\|monthlyRevenue\|RevenueSummary` = 0 hits. | NEW: GAP-293 (Monthly income summary dashboard) |
| AC-FIN-004 | 🟡 PARTIAL | `InvoiceStatus` enum exists (PARTIAL/UNPAID likely tracked). Filter `status=UNPAID` trong invoice list works. KHÔNG có dedicated "Outstanding" tab + reminder action button — phải manual filter. KHÔNG có Zalo reminder. | GAP-063 + GAP-293 |
| AC-FIN-005 | 🟢 PASS-by-design | Grep `payroll\|teacher.*employee\|commission` = 0 hits trong toàn codebase. Feature KHÔNG được build → menu không hiển thị. FeatureGate.tsx exists để gate sau này khi build. | GAP-057 (out-of-scope, correctly absent) |

### 3.4 Communication (4 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-COMM-001 | 🔴 FAIL | KHÔNG có Zalo/SMS messaging service. Grep `Zalo.*notif\|sms.*service\|sms.*sender` ở kiteclass-core = 0 hits (ZaloPay khác — chỉ là payment gateway, KHÔNG phải messaging). `EmailService.java` only sends email. KHÔNG có notification template predefined cho teacher → students. | GAP-063 |
| AC-COMM-002 | 🔴 FAIL | KHÔNG có scheduled reminder service cho class sessions. KHÔNG có `auto_reminder_minutes_before` field trên Class entity. Cron/scheduler infrastructure exists (e.g., `OnboardingEmailScheduler`) nhưng chỉ for kitehub trial emails, không cho per-class lesson reminders. | GAP-063 |
| AC-COMM-003 | 🔴 FAIL | Cancel/reschedule logic chỉ update DB state. KHÔNG có domain event publish + notification consumer cho students. Grep `class.cancelled.*notif\|reschedule.*notif` = 0 hits. | GAP-063 |
| AC-COMM-004 | 🟢 PASS-by-design | Parent portal feature is gated — `parent/` module exists ở core (parent entity for K-12 use cases) nhưng FE `app/(dashboard)/parent/` route exists chỉ cho khi feature toggle ON (P3+ tier). Solo persona không thấy parent invite CTA. ParentLinkType enum exists nhưng dùng cho P3+. | GAP-052 (out-of-scope, gated correctly) |

### 3.5 Edge Cases (5 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-EDGE-001 | 🔴 FAIL | `AttendanceStatus.java` có 4 values: PRESENT/ABSENT/LATE/EXCUSED. KHÔNG có NO_SHOW (cố ý vắng không báo) phân biệt với EXCUSED. Solo teacher không track được pattern no-show để decide drop student. | NEW: GAP-294 (Add NO_SHOW attendance status) |
| AC-EDGE-002 | 🔴 FAIL | KHÔNG có late-cancel policy logic. KHÔNG có `late_cancel_charge_policy` field. KHÔNG có UI prompt "Charge: Full / Partial / Waive". Teacher phải manual chargeback ngoài system. | NEW: GAP-295 (Late-cancel policy + charge decision workflow) |
| AC-EDGE-003 | 🟢 PASS | `Payment.java` có paymentNumber + transactionId + paymentMethod + gatewayTransactionId + amount + auditing (CreatedDate). `common/audit/AuditLog.java` infrastructure exists. PaymentMapper exposes receiptNumber + receiptUrl. Dispute resolution workable. | — |
| AC-EDGE-004 | 🔴 FAIL | KHÔNG có PWA / Service Worker / offline storage. Grep `offline\|service.?worker\|sw\.js\|workbox\|PWA` ở kiteclass-frontend = 0 hits. Mark attendance offline = data lost khi mất signal. | OVERFLOW (range exhausted) — closure PR allocate extension: "Mobile offline attendance sync — PWA" |
| AC-EDGE-005 | 🟢 PASS | Auth + data hoàn toàn cloud-side (Postgres + tenant DB). Re-install app + login từ phone khác preserves data theo design. Không có local-only data. | — |

### 3.6 Exit / Termination (3 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-EXIT-001 | 🔴 FAIL | KHÔNG có "Course completed" student progress export PDF endpoint. Grep `studentProgressReport\|courseCompletionRecord` = 0 hits. `InvoiceRenderer` chỉ render invoice. KHÔNG có student transcript/progress PDF generator. | OVERFLOW (range exhausted) — closure PR allocate extension: "Student progress export PDF on course completion" |
| AC-EXIT-002 | 🔴 FAIL | `InstanceStatus` enum (kitehub-platform) có PENDING/TRIAL/ACTIVE/SUSPENDED/DELETED/PURGED. KHÔNG có PAUSED state. SUSPENDED ≠ user-initiated 30-day pause (SUSPENDED là failed-payment state). Teacher KHÔNG thể tạm dừng dạy 30 ngày miễn phí. | OVERFLOW (range exhausted) — closure PR allocate extension: "Account pause/resume lifecycle 30-day free" |
| AC-EXIT-003 | 🟡 PARTIAL | `DataExportService.java` exists (GDPR Art. 20 scaffold) — chỉ profile.json stub + audit-trail.csv + README.txt. THIẾU attendance/grades/payments/courses data trong export. Comment trong code: "real profile queries...deferred". Không phải xlsx + PDF zip. | Related GAP-051 (xlsx export side, scaffold-only) |

---

## §4 Top Critical Findings (detailed)

### Finding #1 — Mobile-first signup completely blocked

**ACs affected:** AC-ONBOARD-001 (FAIL), AC-ONBOARD-002 (FAIL)

**Evidence:**
- `kiteclass-frontend/src/app/(auth)/register/page.tsx` chỉ email/password — KHÔNG OTP
- Grep `OTP|sms.?verify|zalo.*verify` ở `kiteclass-core` = 0 hits ngoài parent invite
- `BrandingWizard.tsx` + `wizard-machine.ts` không có skip option (grep `Skip|skip` = 0 hits)
- Solo persona target ≤10 phút onboarding mobile-only KHÔNG khả thi với current code

**Business impact:** P1 FREE tier acquisition funnel broken. Solo teacher landing → bounce rate likely >80% trên mobile.

**Fix path:** GAP-286 (mobile OTP) + GAP-287 (skip wizard) — both P0, paired same wave.

### Finding #2 — Notification infrastructure missing across multiple ACs

**ACs affected:** AC-OPS-006 (FAIL), AC-OPS-007 (PARTIAL), AC-COMM-001 (FAIL), AC-COMM-002 (FAIL), AC-COMM-003 (FAIL), AC-FIN-002 (PARTIAL), AC-FIN-004 (PARTIAL) — **7 ACs total**

**Evidence:**
- KHÔNG có Zalo/SMS messaging service (ZaloPay là payment gateway khác — không phải messaging)
- `EmailService.java` only sends email; VN context phụ huynh + học sinh KHÔNG dùng email primary
- Class cancel/reschedule logic chỉ update DB state — KHÔNG publish notification event
- GAP-063 (SMS/Zalo notification integration) đã OPEN nhưng status chưa close

**Business impact:** Teacher phải duplicate work qua Zalo native app cho mọi notification → kills "≤30 phút setup" promise + ongoing ops friction. Receipt PDF không gửi được 1-click. Late-payment reminder không automated. Class change notification không gửi được.

**Fix path:** Bump GAP-063 từ P1 → P0; coordinate với GAP-291 (reschedule), GAP-293 (outstanding reminder).

### Finding #3 — Recurring class generator missing (cross-persona blocker)

**ACs affected:** AC-OPS-002 (FAIL); likely shared finding cross-persona

**Evidence:**
- `Class.java` line 84: `schedule String length=200` plain text only
- `ClassSession.java` exists nhưng phải tạo từng buổi manual
- Grep `recurrence|RRULE|recurring` ở `kiteclass-core` = 0 hits

**Business impact:** Tutoring use case dominant = weekly recurring schedule. Solo teacher với 3 courses × 8-12 buổi/tháng × 12 tháng = ~300 sessions/year phải tạo manual → unfeasible. Cross-cuts P1 + P2 + P3 + P5 (K-12 period schedules).

**Fix path:** GAP-290 (RRULE generator) — P0 cross-cutting; coordinate dedupe với P2/P3/P5 reviews.

### Finding #4 (bonus) — Per-session pricing model missing

**ACs affected:** AC-FIN-001 (FAIL)

**Evidence:** `Course.java` chỉ có `price BigDecimal` single field, KHÔNG có PricingModel enum.

**Business impact:** Solo teacher dominant billing = 200K/buổi × N buổi attended. Force monthly subscription model làm system not match real cash-flow → teacher không dùng nổi.

**Fix path:** GAP-292 (per-session pricing model) — P0; coordinate với GAP-185 (VAT scope).

### Finding #5 (bonus) — Mobile offline + account pause + student progress export

**ACs affected:** AC-EDGE-004 (offline FAIL), AC-EXIT-001 (progress PDF FAIL), AC-EXIT-002 (account pause FAIL)

**Evidence:** PWA / Service Worker = 0 hits; InstanceStatus enum không có PAUSED; KHÔNG có student progress PDF generator.

**Business impact:** Edge cases nhưng critical cho solo persona retention (vacation pause, end-of-course closure).

**Fix path:** OVERFLOW gaps — closure PR allocate extension range (GAP-296+ likely).

---

## §5 Priority-Reordering Recommendation

### Recommended Wave 18 sequence

| Order | Gap | Reason |
|:---:|---|---|
| 1 | **GAP-286** Mobile OTP signup | P0 — unblocks acquisition funnel |
| 2 | **GAP-287** Skip wizard | P0 — paired same flow as GAP-286, must ship same wave |
| 3 | **GAP-063 → bump P0** Zalo/SMS notification | Force-multiplier — unblocks 7 ACs across communication + reschedule + reminder |
| 4 | **GAP-290** RRULE recurring class | P0 — cross-persona blocker; tutoring + center + K-12 all need |
| 5 | **GAP-292** Per-session pricing model | P0 — solo billing doesn't work without it |
| 6 | **GAP-291** Reschedule endpoint | P0 — depends on GAP-063 for notification |

### Defer to Wave 19+

- GAP-288 (Onboarding tour) — UX polish, not blocker
- GAP-289 (Quick-add lesson UI) — depends on GAP-290 + better Class entity model
- GAP-293 (Income summary dashboard) — depends on GAP-292 for accurate revenue calc
- GAP-294 (NO_SHOW status) — small enum addition, batch with other attendance fixes
- GAP-295 (Late-cancel policy) — P2 edge case

### Cross-bucket coordination signals

- Watch P2/P3/P5 reviews for **shared findings** on: GAP-290 (RRULE), GAP-063 (Zalo/SMS), GAP-292 (pricing model), Mobile OTP (GAP-286 likely shared).
- Closure PR (Wave 17 step 6) MUST dedupe these — link cross-references in gap files.

### Out-of-scope features correctly absent (don't change)

- Parent portal (GAP-052) — gated correctly
- Payroll (GAP-057) — not built, correctly hidden
- MOET report card (GAP-055) — out-of-scope per AC

---

## §6 New Gaps Filed (range GAP-286..295)

10 NEW gaps filed in reserved range:

| Gap ID | Title | Priority | AC linked |
|---|---|:---:|---|
| [GAP-286](../../04-quality/gaps/GAP-286-mobile-otp-signup-zalo-sms.md) | Mobile OTP signup via Zalo/SMS | P0 | AC-ONBOARD-001 |
| [GAP-287](../../04-quality/gaps/GAP-287-branding-wizard-skip-default.md) | Skip / Use Default in branding wizard | P0 | AC-ONBOARD-002 |
| [GAP-288](../../04-quality/gaps/GAP-288-onboarding-tour-solo-teacher.md) | First-login onboarding tour | P1 | AC-ONBOARD-004 |
| [GAP-289](../../04-quality/gaps/GAP-289-quick-add-lesson-mobile.md) | Quick-add lesson UI for mobile | P1 | AC-OPS-001 |
| [GAP-290](../../04-quality/gaps/GAP-290-recurring-class-generator.md) | Recurring class RRULE generator | P0 | AC-OPS-002 |
| [GAP-291](../../04-quality/gaps/GAP-291-reschedule-lesson-session.md) | Reschedule lesson endpoint + lifecycle | P0 | AC-OPS-006 |
| [GAP-292](../../04-quality/gaps/GAP-292-per-session-pricing-model.md) | Per-session pricing model | P0 | AC-FIN-001 |
| [GAP-293](../../04-quality/gaps/GAP-293-monthly-income-summary-dashboard.md) | Monthly income summary dashboard | P1 | AC-FIN-003, AC-FIN-004 |
| [GAP-294](../../04-quality/gaps/GAP-294-attendance-no-show-status.md) | NO_SHOW attendance status | P1 | AC-EDGE-001 |
| [GAP-295](../../04-quality/gaps/GAP-295-late-cancel-policy-workflow.md) | Late-cancel policy + charge decision | P2 | AC-EDGE-002 |

### Overflow (extension allocation needed in closure PR)

3 additional gaps need numbers beyond reserved range:

| Suggested ID | Title | AC linked | Priority |
|---|---|---|:---:|
| GAP-29X | Mobile offline attendance sync (PWA / Service Worker) | AC-EDGE-004 | P1 |
| GAP-29X | Student progress export PDF on course completion | AC-EXIT-001 | P1 |
| GAP-29X | Account pause/resume lifecycle (PAUSED state, 30-day free) | AC-EXIT-002 | P0 (retention impact) |

Closure PR should allocate next 3 free numbers (likely GAP-346..348 if other buckets fill 296-345 fully, OR within 296-305 if those are unused) and create files. Stub references already in §3 scoring.

### Existing gaps cross-linked (no new file)

| Existing Gap | Linked AC | Note |
|---|---|---|
| [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | AC-OPS-006/007, AC-COMM-001/002/003, AC-FIN-002/004 | **Recommend bump P1 → P0** — affects 7 ACs |
| [GAP-051](../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) | AC-OPS-008 (related), AC-EXIT-003 (export side) | Existing — solo persona can workaround quick-add (PASS) |
| [GAP-185](../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) | AC-FIN-001 (related) | Existing — VAT broader scope; per-session is GAP-292 |
| [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) | AC-COMM-004 | Out-of-scope cho P1 — gated correctly |
| [GAP-053](../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) | AC-ONBOARD-004 | Related — academic year cần hidden cho solo |
| [GAP-055](../../04-quality/gaps/GAP-055-moet-report-card.md) | AC-OPS-004 | Out-of-scope cho P1 |
| [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) | AC-FIN-005 | Out-of-scope cho P1 — correctly absent |

---

## §7 Coverage Calculation

```
Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100
           = (7 + 0.5 × 7) / 29 × 100
           = 10.5 / 29 × 100
           = 36.2%
```

| Tier | Cutoff | Verdict |
|---|---|---|
| ≥85% | ✅ Production-ready for this persona |  |
| 60-84% | ⚠️ Partially supported (defer GA) |  |
| **30-59%** | **🔴 Major gaps (not production-ready)** | **← P1 Solo Teacher** |
| <30% | ❌ Persona NOT viable |  |

**Result: 36.2/100 → 🔴 Persona NOT supported (major gaps; not production-ready).**

Path to 60%+ verdict (defer-GA threshold): close 7 P0 gaps (GAP-286, GAP-287, GAP-290, GAP-291, GAP-292 + bumped GAP-063 + extension account-pause) → would convert ~10 FAIL → PASS, projected score ≈ 60-65%.

Path to 85%+ verdict (production-ready): + close GAP-288/289/293/294/295 + 3 overflow + GAP-051 export → projected ~85-90%.

---

## §8 Methodology Notes

- State-check approach: cho mỗi AC, grep code paths trong `kiteclass-frontend/`, `kiteclass-core/`, `kitehub-*` để xác nhận PASS/PARTIAL/FAIL. KHÔNG self-score from imagination.
- Mobile-first emphasis: AC nói "trên mobile" → check responsive CSS + viewport meta + tap-target sizes.
- Out-of-scope ACs (AC-COMM-004 parent portal, AC-FIN-005 payroll, AC-OPS-004 MOET report card) scored as PASS-by-design IF system correctly hides feature theo tier/role gate; FAIL nếu force surface vào solo UX.
- Banned phrases (per `gap-done-discipline.md`): KHÔNG dùng "deferred", "manual run", "out-of-scope" trong bất kỳ Status DONE flip — review report là draft, không flip GAP-152.

---

## §9 Log

- **2026-05-04** (skeleton): Wave 17 Bucket A Agent created. Filling §3 + §4 + §5 + §6 incrementally with commit-frequently mandate (3/4 prior agents in this session were killed silently mid-flight).
- **2026-05-04** (final): All 29 ACs scored; 10 NEW gaps filed in reserved range GAP-286..295; 3 overflow noted for closure PR extension allocation; coverage = 36.2% → 🔴 verdict. Status remains `draft` — closure PR (Wave 17 step 6) sẽ flip sang `approved` sau khi dedupe cross-bucket findings.
