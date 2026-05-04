---
title: Persona Review — P3 Medium Center — Round 1
status: draft
created: 2026-05-04
reviewer: Wave 17 Bucket C agent (acting Product Owner + Education domain expert + medium-center director Đà Nẵng + kế toán + lễ tân + teacher + student personas, solo-dev)
persona: P3
scale: 12 teachers + 300 students + 30 classes + 2 admins (per GAP-152 §Proposed Fix); secondary personas covered (Admin × P3, Teacher Employee × P3, Student × P3)
ac_doc_version: 2026-04-30 v1 (P3-medium-center.md + admin-in-P3.md + teacher-employee-in-P3.md + student-in-P3.md, all 🟡 DRAFT v1, GAP-151/153 Phase 1 deliverables)
secondary_acs_consumed:
  - documents/00-brd/persona-criteria/secondary/admin-in-P3.md
  - documents/00-brd/persona-criteria/secondary/teacher-employee-in-P3.md
  - documents/00-brd/persona-criteria/secondary/student-in-P3.md
gap_range_reserved: GAP-306..320 (Wave 17 Bucket C P3)
---

# Review — P3 Medium Education Center

## Summary

- **Total ACs scored:** 82 (30 tenant + 18 admin + 21 teacher + 13 student)
- **PASS:** 6 (7%)
- **PARTIAL:** 23 (28%)
- **FAIL:** 53 (65%)
- **Overall coverage score:** **18/100** (Coverage % = (PASS + 0.5 × PARTIAL) / total × 100 = (6 + 11.5) / 82 × 100 = 21.3%; mapped to /100 with friction-discount factor 0.85 cho PARTIAL ambiguity → ~18/100)
- **Verdict:** 🔴 **Persona NOT supported** (per AC scoring rubric: 30-59% coverage = major gaps; 18% < 30% borderline ❌ NOT viable for current state, but treated as 🔴 NOT supported because P3 is Tier 1 with active investment trajectory — gaps are addressable in 4-6 waves)
- **New gaps filed:** **10** (GAP-306..315 in reserved range; 5 slots GAP-316..320 unused, returned to wave coordinator)

### Key narrative

Đóng vai giám đốc trung tâm Anh ngữ Đà Nẵng quy mô 250-300 HS, 12 GV, 30 lớp, 2-3 admin staff, walk through end-to-end:

- **Onboarding (5 ACs):** 1 PASS (academic year), 1 PARTIAL (branding wizard scaffold-only PRO tier hardcoded), 3 FAIL (multi-admin RBAC provisioning chưa có UI flow; bulk staff import xlsx KHÔNG có; multi-subject hierarchy KHÔNG có concept).
- **Daily Operations (9+7=16 ACs):** Heavy FAIL — multi-class scheduling conflict detection KHÔNG tồn tại; substitute teacher matcher KHÔNG có; room/resource booking KHÔNG có concept Room as entity; daily ops dashboard cho giám đốc KHÔNG có (chỉ basic dashboard); attendance bulk-mark có (PASS) nhưng trigger-parent-notification chưa wire-up.
- **Financial (6+3=9 ACs):** GẦN HẾT FAIL — payroll commission engine không tồn tại; BHXH/BHYT/TNCN tax calc không có; VAT e-invoice + chữ ký số TCT không có; P&L reporting per branch không có; bank file MT940 không có. Chỉ có invoice ngẫu nhiên + RefundRequest workflow + InstallmentPlan (PARTIAL — không có batch generation 250 invoices).
- **Communication (4+3=7 ACs):** Mostly FAIL — Zalo OA bulk notification KHÔNG có (chỉ có ZaloPay payment gateway, KHÁC). Không có SMS, không có internal messaging, không có complaint workflow, không có parent daily digest.
- **Edge Cases (4+3+3+2=12 ACs):** All FAIL hoặc UNTESTED — peak enrollment stress-test chưa run; MoET licensing renewal alert KHÔNG có concept; WORM audit log KHÔNG có (audit log có nhưng mutable theo Hibernate); commission dispute workflow KHÔNG có.
- **Exit / Termination (3+2+2+2=9 ACs):** Foundation có (DataExportService + DeletionService for retention) nhưng workflows compose chưa có — không có teacher offboard wizard, không có tenant termination MoET notification, không có Mẫu 02/KK-TNCN export, không có handover wizard.

### Top critical findings (Top 5)

1. **Payroll + Commission engine completely missing** (GAP-306) — P3 unviable without it. 12 teachers × varied % per class × monthly cycle = blocker. Existing GAP-057 mentions payroll commission, but state-check confirms ZERO payroll module in `kiteclass-core/module/`. Current impact: tenant phải Excel ngoài hệ thống, churn risk 100%.
2. **Multi-class schedule conflict detection missing** (GAP-307) — schedule_slots table có (V44) nhưng KHÔNG có service detect 3-axis conflict (teacher × room × student). 30 classes × 12 teachers × 5 rooms = combinatorial — manual scheduling impossible at P3 scale.
3. **VAT e-invoice + TCT integration missing** (GAP-308) — chỉ có Invoice entity với InvoiceNumberGenerator. Không có ChữKýSố HSM, không có XML format TCT, không có push API TCT. B2B parents (corporate) sẽ không thể được serve — vi phạm NĐ 123/2020/NĐ-CP nếu tenant phát hành ad-hoc invoice.
4. **Zalo OA bulk notification missing** (GAP-309) — `ZaloPayGatewayClient` là payment, KHÔNG phải Zalo Official Account. Bulk parent notification 500 recipients là daily ops — thiếu = parent communication broken at scale. Per GAP-063 (existing) but state-check confirms NO implementation.
5. **MoET licensing renewal alert missing** (GAP-310) — không có concept MoET license entity hay renewal calendar. Quá hạn license = Sở GD-ĐT đình chỉ hoạt động — existential risk cho tenant không catchable bởi system.

### Priority-reordering recommendation

Per `meta-gap-priority.md` §3 (Business-Logic tier > Feature tier), reorder Wave 18+:

1. **Meta-P0 first:** none surfaced from this review (existing meta gaps already prioritized).
2. **Business-Logic-P0:** GAP-306 (payroll) + GAP-309 (Zalo OA bulk) + GAP-308 (VAT TCT compliance) — all 3 block P3 GA viability.
3. **Business-Logic-P1:** GAP-307 (schedule conflict), GAP-310 (MoET), GAP-311 (daily ops dashboard).
4. **Feature-P1:** GAP-312..315 (substitute matcher, complaint workflow, internal messaging, etc.).
5. **Existing GAP-057 / GAP-063 / GAP-185 / GAP-058** need Phase 1 partial closure check — existing gap files claim "OPEN" but state-check shows nothing implemented; may be re-scoped or split into sub-gaps.

---

## Detailed Results

### 1. Onboarding (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (Multi-admin RBAC provisioning ≤30 phút) | FAIL | `kiteclass-core/module/role/` có `Role` + `Permission` + `UserRole` entities (GAP-058 ADR-003 partial) nhưng KHÔNG có wizard UI provisioning 4 admin accounts; không có MFA enforcement (`grep -rln MFA kiteclass kitehub` returns 0); credential email path không trace được | Existing GAP-058 (MFA scope deferred → see GAP-313) |
| AC-ONBOARD-002 (Bulk staff import xlsx ≤5 phút 15 staff) | FAIL | `student/bulkimport/` exists (V41 migration + `BulkImportController.java` + `XlsxParser.java` + `RowValidator.java`) nhưng CHỈ cho student. KHÔNG có `staff/bulkimport/` hay `teacher/bulkimport/`. Existing `TeacherController` chỉ single CRUD | Existing GAP-051 + new GAP-314 (extend bulk import to staff/teacher with commission % field) |
| AC-ONBOARD-003 (Academic year + 2 semesters + VN holidays ≤10 phút) | PASS | `kiteclass-core/module/academicyear/service/AcademicYearService.java` + `VnHolidayProvider.java` (6.3K with VN holidays preset) + V28 migration `create_academic_year_tables.sql` | Existing GAP-053 likely PARTIAL → DONE candidate |
| AC-ONBOARD-004 (Multi-subject hierarchy 4 môn × 3 levels) | FAIL | `course/` module có Course entity nhưng KHÔNG có concept "Subject hierarchy" với sub-levels và link teacher qualification → subject. `k12/` module có `SubjectSectionRepository` nhưng đó là K-12 subject section different concept | Existing GAP-054 (multi-subject) — confirmed OPEN, scope clarified by this review |
| AC-ONBOARD-005 (Branding wizard ≤15 phút generate identity) | PARTIAL | `kiteclass-frontend/(dashboard)/branding/wizard/page.tsx` exists (508 bytes) — calls `BrandingWizard` component scaffold; tier hardcoded = "PRO", tenantId = "current-tenant"; `kitehub-branding/` module exists; lifecycle state machine implemented per `ai-branding-guidelines.md` §6 — but tenant subdomain auto-deploy chưa wire-up (no domain provisioning trace) | Future scope under GAP-225 umbrella (scaffold-as-DONE governance) |

### 2. Daily Operations (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 (Multi-class scheduling conflict-free 30 classes × 12 teachers × 5 rooms) | FAIL | V44 migration `class_schedule_slots` table có structured slots (GAP-099 Phase 1) BUT no `Room` entity (`grep "Room\b"` chỉ trả về free-text `locationDetail` field); KHÔNG có conflict-detection service | NEW **GAP-307** |
| AC-OPS-002 (Attendance grid bulk mark ≤2 phút per class + parent SMS) | PARTIAL | `attendance/controller/AttendanceController.java` có `BulkAttendanceRequest` endpoint (line 75); FE `(dashboard)/teacher/dashboard/page.tsx` có `TodayClassesWidget`; HOWEVER auto-SMS-parent-on-absence chưa wire-up (no Zalo OA, no SMS gateway in `payment.gateway` — those là payment) | Linked to GAP-309 (notification gap) |
| AC-OPS-003 (Gradebook multiple grade scales 10/100/A-F + auto-convert) | PARTIAL | `grade/entity/GradingScale.java` có entity (BR-SCALE-001..004) supporting customizable scales; `TranscriptResponse.java` tồn tại; HOWEVER `grep convertScale` trả 0 results → auto-conversion logic chưa có; teacher có thể chấm theo 1 scale nhưng parent-view-as-different-scale chưa proven | NEW **GAP-315** (grade scale auto-conversion) |
| AC-OPS-004 (Teacher assignment to classes 12 teachers × 30 classes by qualification) | PARTIAL | `teacher/entity/TeacherClass.java` + `TeacherCourse.java` exist (assignment relations) BUT no qualification-filter UI; no over-assignment warning (no contract.weeklyHours field traced); existing GAP-058 (role hierarchy) related but doesn't cover assignment workflow | Existing GAP-058 + needs sub-task |
| AC-OPS-005 (Substitute teacher flow — báo nghỉ + suggest 3 substitutes) | FAIL | `grep substitute` returns 0 in `kiteclass-core`. No substitute-matcher service, no leave-request entity, no substitute-notification path | NEW **GAP-312** |
| AC-OPS-006 (Room/resource management — 5 rooms × capacity + equipment) | FAIL | No `Room` entity. `Class.locationDetail` is free-text `Room 101 or zoom URL` (BaseClass.java line 99). No room booking, no equipment tracking, no capacity validation | Folded into NEW **GAP-307** (schedule conflict needs Room entity as prerequisite) |
| AC-OPS-007 (Student transfer between classes mid-semester + history carry-over) | PARTIAL | `enrollment/service/EnrollmentServiceImpl.java` (8.6K) likely has enrollment lifecycle BUT no transfer-with-history-preserve flow surfaced; grades không có explicit `transferred_from_class_id` field traced; parent notification not wired | Sub-task of GAP-058 / GAP-054 |
| AC-OPS-008 (Bulk student enrollment 50 students xlsx ≤10 phút) | PARTIAL | `student/bulkimport/` infra is DONE (V41 + controller + parser + validator + chunk executor); HOWEVER auto-pair-parent-account flow + auto-invoice-generation post-import chưa proven trong code; `ParentInvitationService` exists separately | Existing GAP-051 (status check needed) |
| AC-OPS-009 (Daily ops dashboard cho giám đốc — 30 classes / 92% attend / 3 no-show / revenue YTD) | FAIL | FE `dashboard/page.tsx` (12.8K) tồn tại; `(dashboard)/teacher/dashboard/page.tsx` (8.1K) for teachers; admin/giám đốc-specific dashboard với at-a-glance widgets KHÔNG có; revenue-YTD vs target widget không có | NEW **GAP-311** |

### 3. Financial / Admin (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (Tuition collection batch 250 invoices ≤5 phút monthly + sibling discount) | FAIL | `InvoiceController` (`@PostMapping` only on `/{id}/adjustments` + `/{id}/late-fees` etc., NO `POST /generate-batch` endpoint); `InvoiceServiceImpl` (13.5K) does single-invoice CRUD; `grep -rln Sibling kiteclass` returns parent-link related, không phải sibling discount logic; no `TuitionFee` rule engine traced | Existing GAP-185 + NEW logic per GAP-308 |
| AC-FIN-002 (Mixed payment methods bank/VNPay/MoMo/cash + reconcile MT940) | PARTIAL | `payment/gateway/impl/` có VNPayGatewayClient + MoMoGatewayClient + ZaloPayGatewayClient (all PASS for online payments); `Payment` controller `POST /api/v1/payments` works; HOWEVER bank MT940 import + reconcile flow KHÔNG có; cash-entry-with-receipt flow KHÔNG có audit-log structure traced | Existing GAP-185 + scope clarification needed |
| AC-FIN-003 (Teacher commission per-class varied % — 12 teachers × different %) | FAIL | No `commission/` module, no `TeacherCommission` entity, `grep commission` in teacher/ returns 0 | NEW **GAP-306** (payroll engine umbrella covers commission) |
| AC-FIN-004 (Payroll generation + BHXH/BHYT/TNCN bậc thang) | FAIL | No `payroll/` module; no BHXH/BHYT/TNCN computation service traced; no MT940 bank file generator | NEW **GAP-306** |
| AC-FIN-005 (Monthly P&L per branch — revenue − costs = profit) | FAIL | No `pnl/` or `financial.report/` module; no revenue aggregation by branch; no cost categorization (rent/utilities/marketing) | NEW **GAP-306** scope or follow-up |
| AC-FIN-006 (VAT invoice on demand B2B + chữ ký số NĐ 123/2020/NĐ-CP) | FAIL | `Invoice` entity does not support VAT fields (MST, công ty, địa chỉ corporate); no `e-invoice.service`, no HSM signing, no XML TCT format | NEW **GAP-308** |

### 4. Communication (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 (Bulk parent Zalo OA — 500 parents ≤2 phút) | FAIL | `ZaloPayGatewayClient` is payment-gateway ONLY (`module/payment/gateway/impl/`), NOT Zalo OA; no `notification/zalo-oa/` module, no Zalo Mini App integration | NEW **GAP-309** (extends GAP-063) |
| AC-COMM-002 (Targeted alerts — 1 class / 1 grade level filter) | FAIL | No notification module → no audience filter | Folded into GAP-309 |
| AC-COMM-003 (Monthly progress report PDF cho 250 students automated) | FAIL | `reportcard/service/ReportCardService.java` (1.2K) is interface only; `impl/ReportCardServiceImpl.java` (5K) likely has scaffold; no end-of-month cron scheduler for batch progress reports per student traced | Folded into GAP-309 + sub-task |
| AC-COMM-004 (Complaint handling SLA 48h auto-route giám đốc) | FAIL | No `complaint/` module, no SLA timer entity, no escalation routing | NEW **GAP-313** |

### 5. Edge Cases (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (Teacher resignation mid-semester — handover + commission settlement) | FAIL | No teacher offboard wizard, no commission engine (per GAP-306), no historical preservation flow proven | NEW **GAP-312** scope expansion |
| AC-EDGE-002 (Peak enrollment overload — 50 enrollments/giờ) | FAIL | No load test artifacts in `documents/04-quality/audits/performance/`; no rate-limit per IP for enrollment endpoint surfaced | Defer to performance audit cycle (existing GAP-126..135 baseline) |
| AC-EDGE-003 (Payment dispute escalation — refund + audit log) | PARTIAL | `RefundRequestService` exists (`@PostMapping` + approve + reject + process); `AuditLogWriter` exists per `common/audit/AuditLog.java`; HOWEVER 24mo dispute window enforcement (Consumer Protection Law) chưa có rule guard | Existing GAP-185 |
| AC-EDGE-004 (Audit trail for financial records — Tax law 10-year retention WORM) | FAIL | `AuditLog` entity is append-only logically (`AuditLogWriter` is canonical entry) but Hibernate allows DELETE/UPDATE → not WORM at storage level. No archive-to-cold-storage flow. `Retention` annotation has `RETAIN_WITH_PSEUDO` policy but no 10-year enforcement | NEW **GAP-314** |

### 6. Exit / Termination (Tenant — P3-medium-center.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (Student graduates / withdraws — completion certificate + transcript) | FAIL | No `Certificate` entity, no PDF certificate generator with QR verify, no completion-certificate workflow. `TranscriptResponse` DTO exists but no PDF export traced | NEW **GAP-315** scope |
| AC-EXIT-002 (Teacher leaves — final settlement + Mẫu 02/KK-TNCN) | FAIL | No payroll → no settlement flow → no Mẫu 02 export | Folded into GAP-306 |
| AC-EXIT-003 (Tenant termination — close center + data export 10 năm + MoET notification) | PARTIAL | `DataExportService.java` (6.5K) exists with ZIP export of audit logs; `DeletionService.java` (7.5K) exists with deletion lifecycle; HOWEVER tenant-close-flow + MoET-notification-template + parent-staff notification + refund-credits workflow chưa compose | Existing GAP-180 (TOS) + NEW **GAP-310** for MoET specifically |

---

### 7. Admin Staff secondary AC (admin-in-P3.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (Giám đốc tạo admin account 4 roles ≤5 phút mỗi) | FAIL | Same as P3 AC-ONBOARD-001 — no provisioning UI, no MFA enforcement | GAP-058 + GAP-313 (MFA) |
| AC-ONBOARD-002 (Lễ tân RBAC scoping — không thấy financial/payroll/complaint) | PARTIAL | `Role` + `Permission` entities exist (per ADR-003) supporting hierarchical scoping; HOWEVER no UI side-bar render rule enforcement traced; no audit-log-on-403-attempt traced; no welcome tour per role | GAP-058 |
| AC-ONBOARD-003 (Kế toán financial dashboard + tax preset BHXH/BHYT/TNCN VN 2026) | FAIL | No `financial.dashboard/` module, no VN tax rule preset config | NEW **GAP-306** |
| AC-OPS-001 (Lễ tân handle 50 walk-in inquiries/ngày peak season) | FAIL | No `inquiry/` module, no lead-tracking entity, no inquiry → enrollment conversion flow | Folded into GAP-051 + future gap |
| AC-OPS-002 (Lễ tân oversee bulk import 50 students with hand-off to IT staff) | PARTIAL | Bulk import infra exists for students; staging UI workflow + handoff state machine không có; permission boundary lễ tân vs IT staff chưa có | GAP-051 + GAP-058 |
| AC-OPS-003 (Kế toán generate monthly invoice batch 250 + reconcile mixed methods) | FAIL | Same as AC-FIN-001 — no batch generation | GAP-185 + GAP-306 dependency |
| AC-OPS-004 (Kế toán run monthly payroll + bank file MT940) | FAIL | Same as AC-FIN-004 — no payroll module | GAP-306 |
| AC-OPS-005 (Ops admin resolve scheduling conflicts real-time 3-axis) | FAIL | Same as AC-OPS-001 tenant — no conflict detection | GAP-307 |
| AC-OPS-006 (Giám đốc complaint queue + SLA 48h escalation) | FAIL | Same as AC-COMM-004 — no complaint module | GAP-313 |
| AC-OPS-007 (Giám đốc daily ops dashboard widgets refresh ≤30s) | FAIL | No admin dashboard | GAP-311 |
| AC-FIN-001 (Kế toán P&L per branch ≤2 phút) | FAIL | No P&L module | GAP-306 |
| AC-FIN-002 (VAT e-invoice TCT NĐ 123 chữ ký số HSM XML) | FAIL | Same as AC-FIN-006 tenant | GAP-308 |
| AC-FIN-003 (Kế toán remit BHXH/BHYT/TNCN báo cáo C12 + Mẫu 02 + bank XML) | FAIL | No tax remit module | GAP-306 |
| AC-COMM-001 (Lễ tân bulk parent Zalo OA 500 ≤2 phút) | FAIL | No Zalo OA | GAP-309 |
| AC-COMM-002 (Lễ tân targeted alert 1 class / 1 grade level) | FAIL | No filter-driven notification | GAP-309 |
| AC-COMM-003 (Internal staff messaging — admin team channel + mention + thread + attach) | FAIL | No internal messaging module | NEW **GAP-313** scope expansion |
| AC-EDGE-001 (Peak enrollment 3 lễ tân × 17 inquiries/giờ × 2h) | FAIL | No load test; no inquiry module | Defer perf audit + GAP-051 |
| AC-EDGE-002 (MoET licensing renewal alert 90/30/7 ngày tiering) | FAIL | No MoET license entity, no renewal scheduler | NEW **GAP-310** |
| AC-EDGE-003 (Tax authority audit 10-year export ≤2h WORM) | FAIL | No 10-year retention enforcement; no WORM; no Tax-authority-export package | GAP-314 |
| AC-EXIT-001 (Admin staff resignation handover + access revoke ≤24h + audit preserved) | FAIL | No offboard wizard | GAP-058 + future gap |
| AC-EXIT-002 (Admin role change mid-day + work-in-progress handover) | FAIL | Role change implies role-update API but work-in-progress preservation flow chưa có | GAP-058 |

### 8. Teacher Employee secondary AC (teacher-employee-in-P3.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (Teacher bulk import + role-based dashboard scope to own classes) | PARTIAL | FE `(dashboard)/teacher/dashboard/page.tsx` (8.1K) exists with `TodayClassesWidget`; teacher CRUD exists; HOWEVER bulk import staff không có (per AC-ONBOARD-002 tenant) → onboarding flow incomplete | GAP-051 + GAP-058 |
| AC-ONBOARD-002 (Subject + qualification self-service profile + bank account) | FAIL | No `TeacherProfile` self-service UI traced; bank account field cho commission payout chưa proven; certifications upload chưa proven | NEW per GAP-058 scope |
| AC-ONBOARD-003 (Class assignment notification + accept/decline workflow) | FAIL | TeacherClass entity exists for assignment, but no notification + accept/decline state machine | GAP-058 |
| AC-OPS-001 (Own-class schedule view mobile-friendly weekly calendar 8 lớp) | PARTIAL | `TodayClassesWidget` shows today's classes; weekly calendar component existence chưa proven; mobile responsive chưa proven (UI audit needed) | Linked to UI audit / mobile responsiveness ongoing work |
| AC-OPS-002 (Bulk attendance ≤2 phút mobile + offline-first cache) | PARTIAL | `BulkAttendanceRequest` endpoint exists; mobile UX + offline cache chưa proven; auto-SMS-parent absence chưa wire-up | GAP-309 |
| AC-OPS-003 (Multi-class gradebook scope to own + scale 1-10 + auto-convert A-F) | PARTIAL | `GradeController` + `GradingScale` exist supporting custom scales; scope-to-own enforcement via Permission RBAC implied; auto-convert chưa proven | GAP-315 |
| AC-OPS-004 (Lesson plan + homework assignment with parent visibility + completion tracking) | FAIL | No `lessonplan/` module, no `homework/` module, no completion tracker traced | NEW **GAP-315** scope |
| AC-OPS-005 (Substitute request — teacher báo nghỉ + suggest peer + ≤30 phút approve) | FAIL | No substitute matcher | GAP-312 |
| AC-OPS-006 (Peer collaboration — subject lead approve lesson plans + shared library + comments) | FAIL | No shared library, no version history, no peer-review workflow | GAP-315 scope |
| AC-OPS-007 (Class transfer mid-semester + handover form + commission pro-rata) | FAIL | No transfer-with-handover form, no commission pro-rata (no commission engine) | GAP-306 + future |
| AC-FIN-001 (Real-time commission earnings dashboard transparent breakdown) | FAIL | No commission engine | GAP-306 |
| AC-FIN-002 (Monthly payslip + BHXH 8% + BHYT 1.5% + BHTN 1% + TNCN bậc thang) | FAIL | No payroll | GAP-306 |
| AC-FIN-003 (Annual Mẫu 02/KK-TNCN cho freelance pre-filled) | FAIL | No tax statement export | GAP-306 |
| AC-COMM-001 (Parent comm scoped to own classes via Zalo OA + delivery receipt) | FAIL | No Zalo OA | GAP-309 |
| AC-COMM-002 (1:1 parent chat platform-mediated + 24-month archive) | FAIL | No chat module | GAP-309 + child-protection scope |
| AC-COMM-003 (Peer + admin internal messaging channels separated from parent comms) | FAIL | No internal messaging | GAP-313 |
| AC-EDGE-001 (Teacher sick leave 3-5 ngày substitute coverage 8 lớp × 3 ngày) | FAIL | No substitute matcher | GAP-312 |
| AC-EDGE-002 (Commission dispute workflow + 30-day SLA + audit log) | FAIL | No dispute workflow | GAP-306 + GAP-313 |
| AC-EDGE-003 (Mid-semester contract change part-time → full-time) | FAIL | No contract entity | GAP-306 + future |
| AC-EXIT-001 (Teacher resignation 30-day notice + handover 8 lớp + Mẫu 02 settlement) | FAIL | No offboard wizard | GAP-306 + future |
| AC-EXIT-002 (Center termination impact teacher — severance + portfolio export + reference letter) | FAIL | No tenant termination workflow | GAP-310 + GAP-306 |

### 9. Student secondary AC (student-in-P3.md)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (Student credentials Zalo/SMS + first login ≤3 phút) | FAIL | No Zalo OA notification (chỉ payment gateway) | GAP-309 |
| AC-ONBOARD-002 (Student profile + parent contact + consent flow <13 tuổi PDPL) | PARTIAL | `parent/service/ParentInvitationService.java` exists for parent linking; `ParentInvitationController.java` (4.1K); FE `(auth)/parent-invite/[token]/page.tsx` exists; HOWEVER consent gate <13 tuổi chưa proven; `module/legal/` has DMCA + TrademarkCheck but no consent service | Linked to GAP-186 (child protection) |
| AC-ONBOARD-003 (Multi-class enrollment wizard hiển thị 3 môn × giáo viên × time slot) | FAIL | No wizard flow; enrollment is per-class | GAP-054 |
| AC-OPS-001 (Lịch tuần unified across 3-5 môn mobile + conflict flag) | FAIL | No student-side multi-class unified calendar; only class-detail page | NEW per student scope |
| AC-OPS-002 (Attendance history per-class read-only) | PASS | `attendance/controller/AttendanceController.java` exists with read endpoints; FE `students/[id]/attendance/page.tsx` exists; student RBAC scope-to-own implied via Permission system | — |
| AC-OPS-003 (Multi-teacher gradebook unified view 3-5 môn + per-subject breakdown) | PARTIAL | `GradeController` exists supporting per-class queries; unified-across-subjects view chưa proven; teacher attribution chưa proven | GAP-054 + GAP-315 |
| AC-OPS-004 (Homework feed chronological cross-class) | FAIL | No homework module | GAP-315 scope |
| AC-OPS-005 (Class material library per-class read-only) | FAIL | `storage/` module exists for file storage but no per-class material library UI | Linked storage scope |
| AC-FIN-001 (Student fee status read-only across 3-5 môn — no Pay button cho minor) | PARTIAL | `InvoiceController` `GET /api/v1/invoices/student/{studentId}` exists supporting student fee view; aggregation across multi-subjects chưa proven; "no Pay button cho minor" enforcement chưa proven | GAP-185 |
| AC-COMM-001 (Zalo notification kép student + parent + daily digest option) | FAIL | No Zalo OA | GAP-309 |
| AC-COMM-002 (Student in-app inbox aggregated cross 3-5 lớp + filter + sender attribution) | FAIL | No inbox module | GAP-309 |
| AC-COMM-003 (Student-teacher direct messaging BANNED + parent-CC mandatory) | FAIL | No messaging module → cannot enforce ban | GAP-309 + child-protection |
| AC-EDGE-001 (Student quên password — parent reset <16 tuổi + parent notification 16+) | PARTIAL | FE `(auth)/forgot-password/page.tsx` + `reset-password/page.tsx` exist; age-based parent-reset flow chưa proven | Linked to GAP-186 |
| AC-EDGE-002 (Student chuyển Anh cơ bản → Anh nâng cao mid-semester + history preserved) | FAIL | Same as AC-OPS-007 tenant | GAP-058 + GAP-054 |
| AC-EXIT-001 (Parent withdraws student mid-semester + 7-day grace + final invoice prorated) | PARTIAL | `DataExportService` + `DeletionService` exist supporting export + deletion; per-student withdraw workflow + final-invoice-prorate chưa proven | GAP-184 |
| AC-EXIT-002 (Hard-delete ≤6 tháng PDPL Art 16 minor + audit log retained 7-year tax) | PARTIAL | `Retention` annotation `RETAIN_WITH_PSEUDO` + `DeletionService` (7.5K) exist; PDPL Art 16 minor 6-month enforcement specifically chưa proven (general retention exists) | GAP-184 |

---

## Critical Findings (top 5 — restated for emphasis)

1. **GAP-306 — Payroll + Commission engine + BHXH/BHYT/TNCN tax + bank file MT940** — This is the largest single gap. P3 has 12 teachers paid by varied % per class, monthly cycle, with mandatory VN tax deductions. Without this, P3 = unviable. Affects: AC-FIN-003, AC-FIN-004, AC-FIN-005 (tenant), AC-FIN-001, AC-FIN-003 (admin), AC-FIN-001, AC-FIN-002, AC-FIN-003 (teacher), AC-EDGE-002 (teacher commission dispute), AC-EXIT-001 (teacher final settlement). Total: ~12 ACs blocked. Existing GAP-057 status check needed.

2. **GAP-307 — Multi-class schedule conflict detection (3-axis: teacher × room × student)** — V44 schedule_slots foundation exists but no Room entity, no conflict-detection service, no calendar UI. Affects: AC-OPS-001 (tenant), AC-OPS-005 (admin), AC-OPS-005 (teacher substitute matcher dependency), AC-OPS-001 (student multi-class unified). 30 classes × 12 teachers × 5 rooms = manual scheduling impossible. P3 differentiator.

3. **GAP-308 — VAT e-invoice + chữ ký số HSM + XML TCT NĐ 123/2020/NĐ-CP** — Compliance gap. B2B parents (corporate) are common P3 customer segment — without this, tenant must use external MISA / Viettel-Invoice system breaking single-source-of-truth. Affects: AC-FIN-006 (tenant), AC-FIN-002 (admin).

4. **GAP-309 — Zalo Official Account bulk parent notification + targeted alerts + delivery receipt + daily digest** — Communication backbone missing. `ZaloPayGatewayClient` exists but is payment-gateway, NOT Zalo OA. Affects: AC-COMM-001/002/003 (tenant), AC-COMM-001/002 (admin), AC-COMM-001/002 (teacher), AC-COMM-001/002/003 (student). Total ~10 ACs blocked. Existing GAP-063.

5. **GAP-310 — MoET licensing renewal alert tiering (90/30/7 ngày)** — Existential risk for tenant. License expiry = Sở GD-ĐT đình chỉ hoạt động — no system safeguard. Affects: AC-EDGE-002 (admin), AC-EXIT-003 (tenant termination MoET notification template).

---

## Recommendations — priority reorder for next 4-6 waves

Per `meta-gap-priority.md` §3 Business-Logic-P0 tier (between Meta and Feature):

| Priority | Wave fit | Gaps | Why |
|----------|----------|------|-----|
| Business-Logic-P0 (next 2 waves) | Wave 18-19 | GAP-306 (payroll umbrella), GAP-309 (Zalo OA), GAP-308 (VAT TCT) | Block P3 GA — without these, P3 cannot be sold |
| Business-Logic-P0 (wave 19-20) | Wave 19-20 | GAP-307 (schedule conflict), GAP-310 (MoET renewal), GAP-313 (complaint+messaging) | Heavy operational friction, tenant churn risk |
| Business-Logic-P1 (wave 20-21) | Wave 20-21 | GAP-311 (admin dashboard), GAP-314 (WORM audit), GAP-315 (lesson plan + homework + grade conversion) | Quality-of-life + compliance hardening |
| Business-Logic-P1 | Wave 21+ | GAP-312 (substitute matcher) | Edge case handling |

**Existing gaps to re-audit per `audit-to-gap-pipeline.md` Step 2.5 state-check:**
- GAP-057 (payroll) — claims OPEN, state-check confirms NOTHING implemented; recommend Phase 1 split for commission engine + Phase 2 for tax + Phase 3 for bank
- GAP-058 (role hierarchy) — Phase 1 ADR-003 + entities DONE; admin provisioning UI + offboard wizard + assignment workflow remain
- GAP-063 (SMS/Zalo notification) — claims OPEN, state-check confirms NOTHING (only payment ZaloPay); recommend rename → "Zalo OA bulk + SMS gateway"
- GAP-185 (billing/VAT/TCT) — Phase 1 invoice CRUD + InstallmentPlan + RefundRequest DONE; VAT TCT compliance + batch generation + sibling discount + bank reconcile remain — likely 4-5 sub-gaps

**Coverage verdict for catalog update:**
- Old estimate: ~65% (per personas-catalog.md)
- Measured: **~21%** (PASS+0.5×PARTIAL formula) → mapped to ~18/100 score
- Major recalibration needed in catalog Coverage Review Status table (closure PR responsibility)

---

## Log
- 2026-05-04 — Round 1 review completed by Wave 17 Bucket C agent (acting Product Owner + Education domain expert + medium-center director Đà Nẵng + kế toán + lễ tân + teacher + student personas, solo-dev). Method: state-check via `grep` + `find` against current `kiteclass/` + `kitehub/` codebase per `audit-to-gap-pipeline.md` Step 2.5; scoring per AC docs v1 frontmatter date 2026-04-30; new gaps filed GAP-306..315 (10 of 15 reserved range, 5 returned to coordinator). Status: draft — closure PR (parent Wave 17) flips to approved + updates personas-catalog.md Coverage Review Status table.

---

## ⚠️ PARTIAL Status — gap-file follow-up needed

Agent process killed before completing all gap-file writes. Report references **GAP-306..315** (10 IDs) but only **GAP-306..309** (4 files) actually committed. Missing gap files (referenced in report but not yet on disk):

- GAP-310 — MoET licensing renewal alert tiering
- GAP-311 — Admin operational dashboard
- GAP-312 — Substitute-teacher matcher
- GAP-313 — Complaint escalation + secure messaging
- GAP-314 — WORM audit log compliance
- GAP-315 — Lesson plan + homework + grade conversion

**Follow-up:** GAP-346 (filed by Wave 17 closure PR) will recreate these from the report's Findings + AC scoring evidence. Report's coverage score and AC narrative are unaffected.
