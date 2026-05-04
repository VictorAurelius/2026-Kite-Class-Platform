---
title: Persona Review — P5 K-12 School — Round 1
status: draft
created: 2026-05-04
reviewer: Wave 17 Bucket D agent (acting K-12 Hiệu trưởng + GVCN + Parent + Student + Admin composite)
persona: P5
scale: 45 teachers · 1200 students · 40 classes · 1 principal + 2 vice-principals + 5 dept heads + 5 staff + ~1500 parents
ac_doc_version: P5-k12-school.md v1 (2026-04-30) + 4 secondary AC docs (student/parent/teacher/admin) v1 (2026-04-30)
secondary_acs_consumed:
  - persona-criteria/secondary/student-in-P5.md (23 ACs)
  - persona-criteria/secondary/parent-in-P5.md (28 ACs)
  - persona-criteria/secondary/teacher-employee-in-P5.md (26 ACs)
  - persona-criteria/secondary/admin-in-P5.md (21 ACs)
gap_range_reserved: GAP-321..345 (25 reserved for Bucket D — biggest bucket reflecting K-12 hierarchical complexity + MOET regulatory burden + child-protection legal mandates)
---

# Review — P5 K-12 School (Public/Private)

## Summary

- **Total ACs scored:** 134 (P5 tenant 36 + Student 23 + Parent 28 + Teacher 26 + Admin 21)
- **PASS:** 7 (5%)
- **PARTIAL:** 27 (20%)
- **FAIL:** 100 (75%)
- **Overall coverage:** 19/100 — 🔴 NOT supported (below 30% threshold; **K-12 GA blocked**, confirms pre-review baseline 30% estimate was actually optimistic)
- **New gaps filed:** 15 (GAP-321..335) — 10 P0 LEGAL/MOET-mandate + 5 P1; remaining FAIL items already covered by existing GAP-051..063, GAP-184..186, GAP-200, GAP-237 et al.
- **Critical legal-mandate failures:** 22/134 ACs touch Luật Trẻ em 2016 / Luật Giáo dục 2019 Đ.83 / PDPL Art 16 / TT 22-32/2020-2021 — only **2** of these are PASS. Cannot ship K-12 without closing these.

### Coverage at a glance

| Bucket | ACs | PASS | PARTIAL | FAIL | Coverage |
|--------|:---:|:----:|:-------:|:----:|:--------:|
| P5 tenant (Onboarding/Ops/Fin/Comm/Edge/Exit) | 36 | 1 | 8 | 27 | 14% |
| Student-in-P5 | 23 | 1 | 5 | 17 | 15% |
| Parent-in-P5 | 28 | 1 | 5 | 22 | 13% |
| Teacher-employee-in-P5 | 26 | 3 | 6 | 17 | 23% |
| Admin-in-P5 | 21 | 1 | 3 | 17 | 12% |
| **Total** | **134** | **7** | **27** | **100** | **19%** |

---

## State-check methodology (per `audit-to-gap-pipeline.md` Step 2.5)

For every AC, evidence was gathered by direct grep / file inspection in the worktree. Concrete pointers below cite:

- **Backend:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/<module>/...`
- **Migrations:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V##__*.sql`
- **Frontend:** `kiteclass/kiteclass-frontend/src/app/(dashboard)/...`
- **Business docs:** `documents/01-business/kiteclass/<domain>/...`
- **Existing gaps:** `documents/04-quality/gaps/GAP-XXX-*.md`

If no concrete file path is cited, status = FAIL (nothing exists). If pointer cites scaffold/MVP/Wave-N-deferred, status = PARTIAL (something exists but missing critical functionality).

---

## Detailed Results

### Section A — P5 Tenant ACs (36)

#### 1. Onboarding (6)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (multi-tenant 65 staff hierarchy ≤8h) | FAIL | `role/entity/Role.java`, `Permission.java`, `UserRole.java` exist (3 entities, 1 service), but no Hiệu trưởng/Phó CM/Tổ trưởng named roles, no XLSX bulk import for staff (only students via `student/bulkimport/`), no role hierarchy with scope-bound (Phó CM scoped to "lịch dạy") | GAP-058 + GAP-051 (extend) |
| AC-ONBOARD-002 (bulk 800 HS + ~1500 PH ≤4h, auto-link, Zalo credential) | PARTIAL | `BulkImportController.java` 4 endpoints (preview/commit/jobs status/errors). `parent/service/ParentInvitationService.java` exists. BUT: invitation flow is per-parent invite link (not bulk parent provisioning at student-import time), no Zalo channel (`Zalo` not found in code), no auto-link parent↔student during XLSX import | GAP-051 + GAP-052 + GAP-063 |
| AC-ONBOARD-003 (MOET license verification) | FAIL | No `mã số trường`/`giấy phép thành lập` upload + admin verify workflow anywhere in codebase | **GAP-321 NEW** |
| AC-ONBOARD-004 (multi-grade 1-12 academic year, VN holidays, 35 weeks) | PARTIAL | `module/academicyear/` (entity/repository/service) + `V28__create_academic_year_tables.sql` exist, `module/k12/entity/Curriculum.java` exists. BUT: no auto-create 4 khối × N classes from level choice, no VN public holiday calendar (Tết, 30/4, 1/5, 2/9, 20/11), no 35-week template, no liên cấp (1+2 hoặc 2+3) | GAP-053 (extend) |
| AC-ONBOARD-005 (LLTP staff vetting ≤7d) | FAIL | No staff vetting workflow; no encrypted CCCD/LLTP/diploma upload; no admin Kite verify step. `module/storage/` exists but generic | **GAP-322 NEW** (sub-feature of GAP-186) |
| AC-ONBOARD-006 (MOET subject taxonomy auto-link 13 môn THCS) | FAIL | `module/k12/entity/Curriculum.java` has JSONB subjects field, no MOET TT 32/2018 GDPT seed data, admin must enter manually | **GAP-323 NEW** |

#### 2. Daily Operations (10)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 (GVCN morning roll-call ≤2min mobile, 4 status, SMS auto ≤30s) | PARTIAL | `attendance/entity/Attendance.java` supports 5 statuses incl. PRESENT/ABSENT/LATE/EXCUSED/MAKEUP (covers 4 MOET states). `attendance/controller/AttendanceController.java` POST endpoint exists. BUT: no mobile UI optimized for ≤2 min (current `(dashboard)/classes/[id]/attendance/page.tsx` is generic table), no auto SMS-to-parent integration (Zalo/SMS gateway absent), no audit log with GVCN ID + IP per save | GAP-056 + GAP-063 |
| AC-OPS-002 (period-based 5-10 tiết/day, GVCN aggregate) | FAIL | `Attendance` entity is keyed by `session_id` (per-class-session), not per-period. `module/k12/entity/ClassScheduleSlot.java` exists for period definitions but attendance NOT linked to slot. Daily attendance ≠ aggregate of period attendance | GAP-060 (CRITICAL — fundamental K-12 model mismatch) |
| AC-OPS-003 (12-15 môn TX/GK/CK formula MOET) | PARTIAL | `module/k12/entity/SubjectGrade.java` has regular_score/midterm_score/final_score columns (V29 migration). `grade/service/GradeServiceImpl.java` exists. BUT: TT 22/2021 Đ.7 weighted formula `(TB.TX + GK×2 + CK×3) / 6` not codified — `ReportCardData.SubjectRow.average` is computed but documented as "simple arithmetic mean", not weighted formula. Curriculum-weighted = Phase 2 follow-up per `ReportCardData.java` javadoc | GAP-054 + GAP-055 |
| AC-OPS-004 (conduct grade Tốt/Khá/TB/Yếu auto-suggest weekly) | FAIL | No conduct entity/service/controller anywhere. `ReportCardData.conduct` is "intentionally nullable in Phase 1 — GAP-059 deferred" per javadoc | GAP-059 (no impl) |
| AC-OPS-005 (mid-term + final exam workflow, approval chain Phó CM → Hiệu trưởng → publish) | FAIL | `assignment/controller/AssignmentController.java` has publish/close endpoints but no exam-vs-regular distinction, no approval chain (Tổ trưởng → Hiệu trưởng), no publish window control | **GAP-324 NEW** |
| AC-OPS-006 (substitute teacher per-period RBAC time-bound ≤30min) | FAIL | No substitute workflow; `role/service/RoleService.java` not time-bound; no temporary access grant | **GAP-325 NEW** |
| AC-OPS-007 (classroom resource scheduling — phòng máy/lab/sân) | FAIL | No resource entity/scheduling/conflict-detection. `class_schedule_slots` table covers period times only | **GAP-326 NEW** (matches NEW-1 in P5 AC doc) |
| AC-OPS-008 (exam invigilation roster, conflict detect) | FAIL | No invigilation roster / coi thi assignment / conflict detection | **GAP-327 NEW** (matches NEW-3 in P5 AC doc) |
| AC-OPS-009 (homework assign + tracking, parent visibility) | PARTIAL | `assignment/AssignmentController.java` has POST/PUT/publish/submit endpoints, `Submission.java` entity. BUT: no late penalty config per school, no parent visibility (parent dashboard MVP per `parent/page.tsx` doesn't surface assignments), no on-time/late status flag visible in DTO | GAP-052 + GAP-054 |
| AC-OPS-010 (sổ đầu bài digital — TT 32/2020 mandate) | FAIL | No sổ đầu bài entity/service. Vi phạm TT 32/2020 quản lý nhà trường | **GAP-328 NEW** (matches NEW-2 in P5 AC doc) |

#### 3. Financial / Admin (5)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (multi-fee structure, discount per-policy HS) | PARTIAL | `module/invoice/entity/Invoice.java` (10K) + `InvoiceItem.java` + `InstallmentPlan.java` + `RefundRequest.java` cover invoice modeling. BUT: no fee-type taxonomy (HP/bán trú/đồng phục/BHYT/BHTN/quỹ PH), no discount rule engine for "con thương binh / hộ nghèo", no actual-bán-trú-day calc | GAP-051 (extend) |
| AC-FIN-002 (public school fee cap UBND vs private flexible) | FAIL | No public/private mode distinction in invoice/pricing. No HĐND fee cap import, no audit when public school exceeds cap | **GAP-329 NEW** |
| AC-FIN-003 (multi-channel reminder Zalo+SMS+email+push, escalation Hiệu trưởng) | FAIL | No reminder scheduler; `core/common/service/email/` exists for email only; no Zalo/SMS gateway integration; no escalation chain | GAP-063 |
| AC-FIN-004 (MOET financial report TT 107/2017 / TT 200/2014 export) | FAIL | No financial report module; `module/document/` has generic xlsx/pdf generators but no MOET-format templates | **GAP-330 NEW** |
| AC-FIN-005 (payroll 50 GV + 15 staff, bank integration Vietcombank/BIDV) | FAIL | No payroll module entirely. GAP-062 OPEN | GAP-062 |

#### 4. Communication (6)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 (parent portal — 6 legal-mandate views) | FAIL | `parent/controller/ParentController.java` has only 2 endpoints (`GET /me`, `GET /me/children` returning `ChildSummaryResponse` with className/grade nullable per Wave 5 scope). NO endpoints for: học bạ, điểm danh, học phí, hạnh kiểm, GVCN messages, kỷ luật history. Vi phạm Luật Giáo dục 2019 Đ.83 | GAP-052 (CRITICAL/LEGAL — Phase 2 needed) |
| AC-COMM-002 (bulk parent notif, GVCN→42, Phó HT→320, Hiệu trưởng→1500) | FAIL | No bulk notif service. Generic email service in `core/common/service/email/`. No template + variable substitution for parent context. No Zalo OA integration | GAP-063 |
| AC-COMM-003 (urgent alert ≤5min → 1500 PH multi-channel) | FAIL | No emergency broadcast workflow. No multi-channel failover | GAP-063 |
| AC-COMM-004 (monthly conduct report auto PDF per HS) | FAIL | No conduct (GAP-059 OPEN) → cannot generate | GAP-059 + GAP-052 |
| AC-COMM-005 (parent-teacher meeting RSVP + biên bản) | FAIL | No meeting coordination entity / calendar.ics generator / biên bản module | **GAP-331 NEW** |
| AC-COMM-006 (complaint escalation L1 GVCN → L4 Phòng GD) | FAIL | No complaint ticket / escalation engine / SLA tracking | **GAP-332 NEW** |

#### 5. Edge Cases (5)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (transfer mid-year, transfer package PDF ≤3d) | PARTIAL | `module/document/` generators exist; `module/retention/DataExportService.java` exists. BUT: no "transfer-out" workflow chaining kế toán close + GVCN finalize + văn thư export; no MOET-format learning record bundle | GAP-051 + GAP-184 |
| AC-EDGE-002 (vắng 3d auto-alert, vắng 5d escalate Phòng GD per Luật Giáo dục Đ.13) | FAIL | No threshold-based alert; no Phổ cập escalation. Period attendance gap (GAP-060) blocks anyway | GAP-060 + GAP-186 |
| AC-EDGE-003 (teacher emergency replacement same-day ≤30min) | FAIL | No emergency substitution workflow (covered with AC-OPS-006) | **GAP-325 NEW (combined)** |
| AC-EDGE-004 (exam re-take cho HS ốm, evidence upload, Tổ trưởng approve) | FAIL | No re-take workflow; AC-OPS-005 exam workflow doesn't exist either | **GAP-324 NEW (combined)** |
| AC-EDGE-005 (child safety incident — encrypted, mandatory report MOLISA + công an Tổng đài 111) | FAIL | No safeguarding officer role; no CRITICAL-priority encrypted ticket; no mandatory reporting suggestion. GAP-186 is policy-only, no impl | GAP-186 (CRITICAL/LEGAL) |

#### 6. Exit / Termination (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (graduation học bạ + bằng tốt nghiệp sealed PDF + 5y retention) | PARTIAL | `module/reportcard/` Phase 1 ships report card generation (Wave 5 GAP-055 PR series). BUT: no sealed PDF signature, no QR verification code, no diploma generation (TT 22/2021 Phụ lục II), no dấu trường digital. `RetentionBucket.java` + `Retention.java` cover retention concept but no 5y-educational policy codified | GAP-055 + GAP-184 |
| AC-EXIT-002 (transfer-out signed package, MOET inter-school API) | FAIL | No MOET inter-school API integration | **GAP-333 NEW** (matches NEW-4 in P5 AC doc) |
| AC-EXIT-003 (teacher retirement off-board, BHXH chốt sổ) | FAIL | No off-boarding workflow; `RoleService` revokes via direct repository calls but no scheduled trigger; no BHXH integration | **GAP-334 NEW** |
| AC-EXIT-004 (school closure, 30y archive Luật Lưu trữ) | FAIL | `RetentionBucket.java` has buckets but no 30y closure-state, no MOET storage handover | **GAP-335 NEW** (matches NEW-5 in P5 AC doc) |

---

### Section B — Student-in-P5 ACs (23)

#### 1. Onboarding (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (HS first login ≤5min, simplified TOS-minor) | PARTIAL | `(auth)/register/student/` route + `(auth)/login/` exist. BUT: no simplified TOS-for-minor variant in `legal/entity/`; no parent-distributes-credential UX path; no symbol-free password policy for primary school | GAP-186 + **GAP-336 NEW** (TOS-minor variant) |
| AC-ONBOARD-002 (profile setup, HS<16 cannot edit DOB/CCCD) | FAIL | `student/controller/StudentController.java` has CRUD endpoints but no minor-data edit lock policy enforced | GAP-186 |
| AC-ONBOARD-003 (parent link visible, masked PII, cannot unlink) | PARTIAL | `parent/entity/ParentStudentLink.java` exists. Read endpoint via parent side. BUT: no student-side `GET /me/parents` with masked PII; no unlink restriction policy | GAP-052 + GAP-058 |

#### 2. Daily Operations (7)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 (today's period schedule ≤2 tap mobile) | FAIL | `ClassScheduleSlot.java` entity exists; no student-facing schedule view in frontend (`(dashboard)/students/[id]/` lacks schedule subpage) | GAP-060 + GAP-053 |
| AC-OPS-002 (submit BTVN 12+ môn, late/on-time status) | PARTIAL | `assignment/Submission.java` entity + `AssignmentController` POST /submit endpoint. BUT: no per-subject organization, no late penalty visibility, no submit-history view | GAP-054 |
| AC-OPS-003 (12+ môn grades publish-window controlled) | PARTIAL | `grade/controller/GradeController.java` has student/class endpoints. BUT: no publish workflow; no Tổ trưởng approval gate; HS sees grades immediately upon GV insert | GAP-054 + GAP-055 |
| AC-OPS-004 (view conduct grade with reasoning, appeal channel) | FAIL | No conduct module | GAP-059 |
| AC-OPS-005 (exam results in publish window, view chấm bài) | FAIL | No exam workflow | **GAP-324 NEW (combined)** |
| AC-OPS-006 (formal report card PDF + watermark + e-signature + QR) | FAIL | `reportcard/service/impl/` Phase 1 generates PDF but no e-signature, no QR, no watermark per HS | GAP-055 |
| AC-OPS-007 (period-granular attendance view per HS) | FAIL | `(dashboard)/students/[id]/attendance/page.tsx` exists but uses session-based attendance not period | GAP-060 |

#### 3. Financial (2)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (HS view-only fee status, no Pay button, no PII PH) | FAIL | No student-side fee-status endpoint; current invoice controller `/student/{studentId}` returns full invoice (would expose PH PII) | GAP-052 + GAP-186 |
| AC-FIN-002 (HS download invoice/receipt PDF) | PARTIAL | `module/document/pdf/` generic PDF generator exists. BUT: no per-student receipt query | GAP-051 |

#### 4. Communication (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 (HS receive GVCN announcements, template-only reply) | FAIL | No notification/messaging module for HS-GVCN | GAP-052 + GAP-186 |
| AC-COMM-002 (HS request 1-to-1 GVCN call with consent recording) | FAIL | No call scheduling; no recording with consent prompt | GAP-186 + **GAP-337 NEW** (1-to-1 recording) |
| AC-COMM-003 (HS receive GV bộ môn class-level only, no DM) | FAIL | No class-level announcement engine | GAP-052 + GAP-186 |
| AC-COMM-004 (parent-mediated sensitive notif, HS cannot hide from PH) | FAIL | No incident-notif sync engine | GAP-052 + GAP-186 |

#### 5. Edge Cases (5)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (forgot password — parent or GVCN mediated, no email reset for minor) | FAIL | `(auth)/forgot-password/` + `(auth)/reset-password/` exist as standard email-reset flow → vi phạm PDPL Art 16 minor data | GAP-186 |
| AC-EDGE-002 (transfer mid-year, account archive 5y read-only) | PARTIAL | `RetentionBucket.java` exists but no archive-mode for student account | GAP-184 |
| AC-EDGE-003 (graduation, alumni 5y read-only) | FAIL | No alumni status / lifecycle | GAP-184 + GAP-055 |
| AC-EDGE-004 (child safety report — CRITICAL encrypted, optional bypass GVCN) | FAIL | No safety-report channel | GAP-186 (CRITICAL) |
| AC-EDGE-005 (account compromise, parent-assist lockout, 1y audit log) | FAIL | No account-activity view, no lockout-with-PH-confirm | GAP-186 |

#### 6. Exit (2)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (5y archive + 6mo soft-delete grace + hard delete sensitive-minor) | FAIL | `Retention.java` + `DeletionService.java` exist but no per-bucket policy with grace warnings | GAP-184 |
| AC-EXIT-002 (school closure, MOET archive 30y) | FAIL | Same as P5 AC-EXIT-004 | **GAP-335 NEW (combined)** |

---

### Section C — Parent-in-P5 ACs (28)

#### 1. Onboarding (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (PH invite via email + Zalo, mobile activate ≤10min) | PARTIAL | `parent/controller/ParentInvitationController.java` (4K, 4 endpoints) + `(auth)/parent-invite/[token]/` route. BUT: email-only (no Zalo channel) | GAP-052 + GAP-063 |
| AC-ONBOARD-002 (granular PDPL Art 16 parental consent, 7y audit) | FAIL | No consent module / consent-version tracking / 7y audit retention | GAP-186 + GAP-184 |
| AC-ONBOARD-003 (multi-child link, 1+ children per PH) | PARTIAL | `ParentStudentLink.java` supports many-to-many but no admin-approve workflow for manual link request | GAP-052 |
| AC-ONBOARD-004 (notification preferences per category × channel) | FAIL | No notification preference table / per-category mapping | GAP-063 |

#### 2. Daily Ops (7)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 (real-time daily attendance push ≤30s) | FAIL | No push notification pipeline; no Zalo/SMS gateway | GAP-063 |
| AC-OPS-002 (period-granular attendance view) | FAIL | Session-based attendance only | GAP-060 + GAP-052 |
| AC-OPS-003 (real-time grade monitoring ≤24h, trend graph) | FAIL | No parent grade endpoint; `ParentController` only 2 endpoints | GAP-052 + GAP-054 |
| AC-OPS-004 (homework progress, alert miss >3 BTVN/week) | FAIL | No parent assignment-status view, no alert engine | GAP-052 |
| AC-OPS-005 (conduct monitoring + serious-violation alert) | FAIL | Conduct doesn't exist | GAP-059 |
| AC-OPS-006 (formal học bạ download with QR verify) | FAIL | Same as P5 AC-EXIT-001 | GAP-055 + GAP-184 |
| AC-OPS-007 (multi-child unified dashboard, bulk acknowledge) | FAIL | Parent dashboard MVP shows children list only | GAP-052 |

#### 3. Financial (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (multi-fee payment one-click + multi-channel VNPay/MoMo/ZaloPay) | PARTIAL | `payment/controller/PaymentController.java` POST endpoints. BUT: no PH-side payment UI; no VNPay/MoMo/ZaloPay gateway integration (only payment_method enum in `PaymentMethod.java`) | GAP-052 + **GAP-338 NEW** (VN payment gateways) |
| AC-FIN-002 (e-invoice TT 78/2021 ≤5min after payment) | PARTIAL | `Invoice.java` exists, `document/` PDF generator exists. BUT: no e-invoice TT 78/2021 format (`HoaDonDienTu`); no auto-fire post-payment | GAP-051 + **GAP-339 NEW** (TT 78/2021 e-invoice) |
| AC-FIN-003 (escalation Hiệu trưởng + lock con features) | FAIL | No escalation workflow | GAP-063 + GAP-058 |
| AC-FIN-004 (scholarship + discount auto-apply with chứng nhận) | FAIL | No discount rule engine | **GAP-329 NEW (combined)** |

#### 4. Communication (5)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 (bulk class messages from GVCN, read receipt) | FAIL | No messaging engine | GAP-052 + GAP-063 |
| AC-COMM-002 (1-to-1 GVCN with recording option, in-app only) | FAIL | Same as student AC-COMM-002 | **GAP-337 NEW (combined)** |
| AC-COMM-003 (parent-teacher meeting RSVP + biên bản) | FAIL | Same as P5 AC-COMM-005 | **GAP-331 NEW (combined)** |
| AC-COMM-004 (mandatory child safety notif unless PH is perpetrator) | FAIL | No safety workflow | GAP-186 |
| AC-COMM-005 (complaint escalation L1→L4 SLA) | FAIL | Same as P5 AC-COMM-006 | **GAP-332 NEW (combined)** |

#### 5. Edge (5)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (PH forgot password multi-channel SMS OTP) | PARTIAL | Email-based reset exists; no SMS OTP backup | GAP-063 |
| AC-EDGE-002 (divorced parents joint custody) | FAIL | No custody-type field; no per-parent permission scope | **GAP-340 NEW** |
| AC-EDGE-003 (PH 2 con ở 2 trường — cross-tenant aggregate or SSO) | FAIL | No multi-tenant parent identity (each tenant owns its own users) | **GAP-341 NEW** |
| AC-EDGE-004 (PH report safety as victim — CRITICAL encrypted) | FAIL | No safeguarding ticket | GAP-186 |
| AC-EDGE-005 (PH suspected perpetrator — suppress notif to PH, MOLISA route) | FAIL | No tip-off-prevention logic | GAP-186 |

#### 6. Exit (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (5y archive + 6mo grace + hard delete) | FAIL | Same as student AC-EXIT-001 | GAP-184 |
| AC-EXIT-002 (PH right to be forgotten, export-then-delete) | PARTIAL | `DataExportService.java` exists; no right-to-be-forgotten workflow with 5y educational legal-minimum override | GAP-184 |
| AC-EXIT-003 (school closure 6mo notice + 30y MOET archive) | FAIL | Same as P5 AC-EXIT-004 | **GAP-335 NEW (combined)** |
| AC-EXIT-004 (PH death/incapacitation, surviving PH takeover) | FAIL | No legal-guardian transition workflow | **GAP-342 NEW** |

---

### Section D — Teacher-employee-in-P5 ACs (26)

#### 1. Onboarding (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (teacher onboarding with role variant GVCN/Bộ môn + qualification metadata) | PARTIAL | `teacher/entity/Teacher.java` + `TeacherClass.java` + `TeacherCourse.java` exist; `module/k12/entity/HomeroomClass.java` has `homeroom_teacher_id`. BUT: no qualification metadata (TT 22/2021 evaluator role); no role-variant flow at onboarding | GAP-058 |
| AC-ONBOARD-002 (LLTP background upload, 3y recheck) | FAIL | No staff vetting (covered AC-ONBOARD-005 P5 tenant) | **GAP-322 NEW (combined)** |
| AC-ONBOARD-003 (class assignment per academic year, 7d review window) | PARTIAL | `HomeroomClassService.java` has assign endpoints. BUT: no review window enforcement | GAP-053 |

#### 2. Daily Operations (10)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 [GVCN] (morning roll-call mobile ≤2min, SMS auto) | PARTIAL | Same as P5 AC-OPS-001 | GAP-056 + GAP-063 |
| AC-OPS-002 [Bộ môn] (period-based per tiết, GVCN aggregate) | FAIL | Session-based only | GAP-060 |
| AC-OPS-003 [Bộ môn] (multi-class gradebook 5 lớp × 210 HS, TT 22/2021 formula) | PARTIAL | Grade module exists; multi-class scope works via teacher_id. BUT: weighted formula not implemented (per `ReportCardData.java`) | GAP-054 + GAP-055 |
| AC-OPS-004 [GVCN] (conduct weekly tracking + auto-suggest end-of-term) | FAIL | No conduct | GAP-059 |
| AC-OPS-005 [Bộ môn] (sổ đầu bài digital — TT 32/2020) | FAIL | Same as P5 AC-OPS-010 | **GAP-328 NEW (combined)** |
| AC-OPS-006 (multi-period schedule view, mobile responsive) | PARTIAL | `(dashboard)/teacher/dashboard/page.tsx` exists; period schedule TBD | GAP-060 |
| AC-OPS-007 (lesson plan + BTVN assign with parent visibility) | PARTIAL | `assignment/AssignmentController` exists; no lesson plan field; no parent visibility | GAP-052 + GAP-054 |
| AC-OPS-008 (exam preparation + version control) | FAIL | No exam-vs-regular distinction | **GAP-324 NEW (combined)** |
| AC-OPS-009 (exam invigilation roster fair assignment) | FAIL | No invigilation roster | **GAP-327 NEW (combined)** |
| AC-OPS-010 [GVCN] (centralized lớp chủ nhiệm workspace) | PARTIAL | `HomeroomClass.java` entity exists; no aggregated workspace UI (no `(dashboard)/teacher/homeroom/[classId]/` page) | GAP-056 |

#### 3. Financial (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (salary scale + allowances dashboard) | FAIL | No payroll module | GAP-062 + GAP-057 |
| AC-FIN-002 (monthly payslip mẫu C2-04/NS với BHXH/BHYT/BHTN/Công đoàn/TNCN) | FAIL | No payslip generator | GAP-062 |
| AC-FIN-003 (Mẫu 05/QTT-TNCN annual tax statement) | FAIL | No annual tax statement | GAP-062 |

#### 4. Communication (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 [GVCN] (bulk parent comms scoped to homeroom 42 PH) | FAIL | No bulk-comms engine | GAP-063 |
| AC-COMM-002 [GVCN] (parent meeting RSVP + biên bản signed Đ.83) | FAIL | Same as P5 AC-COMM-005 | **GAP-331 NEW (combined)** |
| AC-COMM-003 (student-teacher messaging platform-mediated, banned-words blocked) | FAIL | No messaging + content moderation for HS-teacher chat | GAP-186 |
| AC-COMM-004 (peer + tổ chuyên môn internal messaging, scope-separated) | FAIL | No internal teacher messaging | **GAP-343 NEW** |

#### 5. Edge (4)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (emergency replacement, sub access time-bound RBAC) | FAIL | Same as P5 AC-EDGE-003 | **GAP-325 NEW (combined)** |
| AC-EDGE-002 (annual leave 12d/yr workflow, sub coverage) | FAIL | No leave request workflow | **GAP-344 NEW** |
| AC-EDGE-003 (salary dispute audit log + SLA) | FAIL | No payroll → no dispute | GAP-062 |
| AC-EDGE-004 [GVCN] (child safety reporting confidential audit trail) | FAIL | No safeguarding | GAP-186 |

#### 6. Exit (2)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (resignation 30/45d notice, handover, severance) | FAIL | No resignation workflow | **GAP-345 NEW** |
| AC-EXIT-002 (retirement 60/55, pension calc, MOET notify) | FAIL | Same as P5 AC-EXIT-003 | **GAP-334 NEW (combined)** |

---

### Section E — Admin-in-P5 ACs (21)

#### 1. Onboarding (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (Hiệu trưởng tạo 15 admin/staff với 6 roles distinct ≤2h) | FAIL | `RoleService.java` exists but no 6-role taxonomy (văn phòng/giáo vụ/thư viện/y tế/bảo vệ/IT) seeded | GAP-058 |
| AC-ONBOARD-002 (bulk staff vetting per Luật Trẻ em) | FAIL | Same as P5 AC-ONBOARD-005 | **GAP-322 NEW (combined)** |
| AC-ONBOARD-003 (văn phòng+giáo vụ first login MoET dashboard with deadlines) | FAIL | No MoET deadlines dashboard | **GAP-330 NEW (combined)** |

#### 2. Daily Operations (7)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 (PCGD báo cáo monthly ≤30min) | FAIL | No PCGD report generator | **GAP-330 NEW (combined)** |
| AC-OPS-002 (giáo vụ oversee teacher điểm input dashboard with alert) | PARTIAL | Grade module exists; no overseer dashboard | GAP-055 |
| AC-OPS-003 (finalize học bạ HK1 800 HS, Hiệu trưởng ký số ≤2h batch) | PARTIAL | `reportcard/service/impl/` Phase 1 generates PDFs but no batch + e-signature pipeline | GAP-055 |
| AC-OPS-004 (school-year calendar with VN holidays + 35 weeks) | PARTIAL | `academicyear/` exists; no VN holidays seed | GAP-053 |
| AC-OPS-005 (IT staff bulk import 800 HS + 1200 PH ≤4h) | PARTIAL | `BulkImportController` exists; no parent bulk import | GAP-051 + GAP-052 |
| AC-OPS-006 (bảo vệ check-in/out card scan + Zalo notify PH) | FAIL | No bảo vệ scanner integration | **GAP-326 NEW (combined)** |
| AC-OPS-007 (y tế sổ y tế + vaccination + injury log) | FAIL | No medical records module | **GAP-326 NEW (combined)** |

#### 3. Financial (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 (payroll batch 50 teachers + MoET payscale) | FAIL | No payroll | GAP-062 |
| AC-FIN-002 (fee collection oversight, public/private mode) | FAIL | Same as P5 AC-FIN-002 | **GAP-329 NEW (combined)** |
| AC-FIN-003 (báo cáo tài chính TT 107/2017 quarterly) | FAIL | Same as P5 AC-FIN-004 | **GAP-330 NEW (combined)** |

#### 4. Communication (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 (bulk school-year notif 1200 PH ≤5min Zalo OA + SMS fallback) | FAIL | No Zalo OA + SMS gateway | GAP-063 |
| AC-COMM-002 (giáo vụ targeted alert per khối/lớp) | FAIL | No targeted alert | GAP-063 |
| AC-COMM-003 (parent complaint escalation queue 48h SLA) | FAIL | Same as P5 AC-COMM-006 | **GAP-332 NEW (combined)** |

#### 5. Edge (3)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 (emergency school closure broadcast 1200 PH ≤5min) | FAIL | Same as P5 AC-COMM-003 | GAP-063 |
| AC-EDGE-002 (MoET audit Sở GD-ĐT, full data export 5y ≤4h) | PARTIAL | `DataExportService.java` exists; no 5y MoET-format export | GAP-184 |
| AC-EDGE-003 (child safety incident triage y tế/bảo vệ/GVCN/văn phòng) | FAIL | No multi-stakeholder safety triage | GAP-186 |

#### 6. Exit (2)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 (admin staff resignation, revoke ≤24h, audit log) | FAIL | No off-boarding scheduled trigger | **GAP-345 NEW (combined)** |
| AC-EXIT-002 (transcript request handling ≤5d, TT 32/2020) | PARTIAL | `reportcard/` exists; no SLA-tracked workflow | GAP-055 |

---

## Critical Findings (Top 5)

### 1. Parent portal is a 2-endpoint MVP — vi phạm Luật Giáo dục 2019 Đ.83

**Evidence:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/controller/ParentController.java` exposes only `GET /me` + `GET /me/children`. `ChildSummaryResponse` has nullable className/grade per Wave 5 deferral. NO endpoints for học bạ, điểm danh hàng ngày, học phí, hạnh kiểm, GVCN messages, kỷ luật history.

**Impact:** This is the SINGLE legal-mandate AC under Luật Giáo dục 2019 Điều 83 (parent monitoring rights). Cannot deploy K-12 to a single tenant without violating law. 9 of 28 Parent ACs reference this gap directly.

**Fix path:** GAP-052 needs Phase 2 with full grade/attendance/conduct/payment/messaging endpoints + corresponding parent dashboard pages. Current parent dashboard `(dashboard)/parent/page.tsx` is explicitly marked "Wave 2 MVP" and "Wave 5 will layer ... on top of this shell" — Wave 5 hasn't shipped that layer.

### 2. Period-based attendance is the wrong abstraction (GAP-060) — fundamental K-12 model mismatch

**Evidence:** `Attendance.java` keyed by `session_id` (per-class-session). `module/k12/entity/ClassScheduleSlot.java` exists for period definition but attendance NOT linked. K-12 model is 5-10 tiết/day with different GV bộ môn per tiết, GVCN aggregates daily — current model assumes 1 lớp = 1 buổi (center-style).

**Impact:** Affects 8 ACs across P5 + Student + Parent + Teacher. Cannot derive vắng-cả-ngày from vắng-tiết. GVCN cannot aggregate. Parents see wrong attendance picture. Per Luật Giáo dục Đ.13 phổ cập attendance threshold (3d/5d), the entire alert engine cannot be built on session model.

**Fix path:** GAP-060 must redesign Attendance to be `period_attendance` (slot_id + student_id + status + recorded_by) with daily aggregation view materializing GVCN's responsibility. Current `Attendance` becomes legacy or migrates.

### 3. Child safety + child protection have policy doc but ZERO implementation — vi phạm Luật Trẻ em 2016 Đ.51

**Evidence:** `documents/00-brd/child-protection-policy.md` (referenced repeatedly in ACs) exists. GAP-186 OPEN (Phase 1 skeleton). NO code: no safeguarding officer role, no CRITICAL-priority encrypted ticket, no mandatory reporting suggestion endpoint, no MOLISA / Tổng đài 111 / công an integration, no recording-with-consent for 1-to-1 GVCN call, no template-only HS↔GVCN message constraint, no minor-data-edit lock.

**Impact:** Affects 22 of 134 ACs (16%). All are P0 LEGAL — cannot ship K-12 without these or risk Luật Trẻ em 2016 Đ.51 (mandatory reporting) violations + grooming-prevention failures (child-protection-policy.md §4.2).

**Fix path:** GAP-186 needs full implementation phase: safeguarding officer role + safety-report channel + mandatory-reporting suggestion + recording option + template-only message + minor-data-edit lock. New gaps GAP-322 (LLTP vetting), GAP-336 (TOS-minor), GAP-337 (recording 1-to-1) split out scope.

### 4. MOET compliance surface is empty — học bạ TT 22/2021, sổ điểm, báo cáo TT 107/2017 missing

**Evidence:** ReportCard Phase 1 generates a PDF but no e-signature (Hiệu trưởng), no QR verification, no watermark per HS, no diploma generation (TT 22/2021 Phụ lục II), no MoET-format financial reports (TT 107/2017 / TT 200/2014), no PCGD báo cáo, no sổ đầu bài (TT 32/2020 mandate).

**Impact:** Public schools cannot submit báo cáo to Phòng GD&ĐT. Học bạ cannot be verified by receiving school during transfer/cấp 3 admission. Vi phạm 5+ MoET regulations means trường KHÔNG được dùng platform legally.

**Fix path:** GAP-055 needs Phase 2 (signature + QR + watermark + Phụ lục II diploma); GAP-330 NEW (PCGD + TT 107/2017 financial reports); GAP-328 NEW (sổ đầu bài).

### 5. Multi-channel notification (Zalo/SMS) absent — affects 17 ACs across all 5 personas

**Evidence:** `core/common/service/email/` exists. NO Zalo OA / SMS gateway integration in any service. Search for "Zalo" returns only `BrandingResponse.java` configuration field, no actual gateway. Search for "SMS" returns only `PaymentMethod` enum and webhook controllers (no SMS sender).

**Impact:** GVCN cannot notify 42 PH about attendance in ≤30s (AC-OPS-001 P5+teacher). Hiệu trưởng cannot broadcast emergency to 1500 PH in ≤5min (AC-COMM-003 P5 + AC-EDGE-001 admin). Payment reminders cannot escalate (AC-FIN-003). PH miss daily attendance updates. Zalo is THE dominant K-12 channel in VN; building K-12 without it is infeasible.

**Fix path:** GAP-063 must ship full Zalo OA + SMS gateway integration with multi-channel routing, escalation, read-receipts. Plus GAP-200 (school MIS integration) for upstream phone book sync.

---

## Recommendations

### Priority reordering (apply `meta-gap-priority.md` §3 + Business-Logic tier)

K-12 is **Tier 1 USER PRIORITY** persona but currently 19% covered. To reach 60% threshold (allow pilot with single tenant) we need to close ~55 ACs. To reach 85% (production) we need ~93. Recommended sequencing:

**Stage 1 — LEGAL + foundation (blocks K-12 GA, ~6 weeks at current velocity):**
1. **GAP-186 implementation** (child protection — safeguarding officer + CRITICAL-encrypted ticket + mandatory reporting + recording option + template-only message + minor-data-edit lock) — closes 22 ACs
2. **GAP-052 Phase 2** (parent portal full endpoints + dashboard pages) — closes 12 ACs
3. **GAP-060 redesign** (period-based attendance) — closes 8 ACs
4. **GAP-063** (Zalo OA + SMS gateway) — closes 17 ACs

**Stage 2 — MOET compliance (blocks public school deployment, ~4 weeks):**
5. **GAP-055 Phase 2** (e-signature + QR + watermark + diploma TT 22/2021 Phụ lục II)
6. **GAP-059** (conduct grade — auto-suggest hạnh kiểm) — closes 6 ACs
7. **GAP-330 NEW** (MOET financial reports TT 107/2017 + PCGD)
8. **GAP-322 NEW** (LLTP staff vetting per Luật Trẻ em 2016 Đ.25)
9. **GAP-323 NEW** (TT 32/2018 GDPT subject taxonomy seed)
10. **GAP-328 NEW** (sổ đầu bài digital — TT 32/2020 mandate)

**Stage 3 — exam + workflow (blocks daily ops, ~3 weeks):**
11. **GAP-324 NEW** (exam workflow + re-take)
12. **GAP-325 NEW** (substitute teacher RBAC time-bound)
13. **GAP-326 NEW** (classroom resource + bảo vệ scanner + y tế sổ y tế — combined infra ops)
14. **GAP-327 NEW** (exam invigilation roster)

**Stage 4 — financial + escalation (~3 weeks):**
15. **GAP-062 + GAP-057** (payroll bank + teacher commission)
16. **GAP-329 NEW** (public/private fee mode distinction)
17. **GAP-338/339 NEW** (VN payment gateways + e-invoice TT 78/2021)
18. **GAP-331 NEW** (parent-teacher meeting RSVP)
19. **GAP-332 NEW** (complaint escalation L1→L4)

**Stage 5 — edge + exit (~2 weeks):**
20. **GAP-321 NEW** (MOET license verification at provisioning)
21. **GAP-333 NEW** (MOET inter-school transfer API)
22. **GAP-334 NEW** (teacher retirement + BHXH chốt sổ)
23. **GAP-335 NEW** (school closure 30y archive)
24. **GAP-340/341/342/343/344/345 NEW** (parent custody + cross-tenant + guardian + teacher internal msg + leave + resignation)

### Wave-pack opportunity (per `meta-gap-priority.md` + `wave-pack-planner` skill)

Stage 1 has 4 disjoint gaps that can run in parallel by 4 worktree-isolated agents:
- Bucket A: GAP-186 (child protection)
- Bucket B: GAP-052 Phase 2 (parent portal)
- Bucket C: GAP-060 (period attendance redesign)
- Bucket D: GAP-063 (Zalo + SMS gateway)

Stage 2 has 6 gaps → 4-bucket wave possible (combine GAP-055 + GAP-059 reportcard adjacent; GAP-322 + GAP-323 + GAP-328 admin/MOET).

### Don't ship K-12 GA until at least Stage 1 + Stage 2 close. Pilot with single supportive trường XYZ at end of Stage 1 (~6 weeks).

---

## Out-of-scope (this review)

- Detailed mockup/UI design for parent portal Phase 2 (deferred to design-system kit + UI review wave)
- Performance / load testing for 1500 concurrent PH push notifications (defer to performance audit post-Stage 1 ship)
- Real legal sign-off on TOS-minor + child-protection-policy by lawyer (defer to GAP-156 quarterly business-logic review)
- Real MoET pilot review of học bạ format compatibility (defer to first-tenant pilot post-Stage 2)
- Cross-comparison delta vs P3 Medium Center (different vertical — different review)

---

## Methodology notes

- **Role-played** end-to-end journey: Hiệu trưởng provisioning trường → admin bulk import 1200 HS + ~1500 PH → GVCN morning roll-call → bộ môn nhập điểm → PH check con → HS submit BTVN → exam workflow → conduct/hạnh kiểm cuối kỳ → học bạ ban hành → transfer-out HS → school closure.
- **Evidence-first scoring**: every PASS/PARTIAL has a concrete file:line or controller endpoint pointer. FAIL cases either cite "no impl" with grep-evidence or cite explicit "Phase X deferred" javadoc in existing impl.
- **State-check applied** per `audit-to-gap-pipeline.md` Step 2.5 BEFORE filing every NEW gap (grep code + check existing GAP files first).
- **Combined gaps** noted where 1 NEW gap covers multiple ACs across personas (e.g., GAP-322 LLTP vetting → 1 P5 tenant + 1 admin + 1 teacher AC).

---

## Log

- **2026-05-04** — Round 1 review completed by Wave 17 Bucket D agent. 134 ACs scored: 7 PASS / 27 PARTIAL / 100 FAIL → 19/100 coverage (🔴 below 30% threshold; K-12 GA blocked). 15 NEW gaps filed (GAP-321..335 + GAP-336/337/338/339/340/341/342/343/344/345 — 25 used, range exhausted). Recommendation: Stage 1 LEGAL + foundation wave (GAP-186/052/060/063 parallel) before any K-12 deployment. Existing GAP-051..063 + GAP-184..186 cover most remaining FAIL items but need Phase 2/redesign per Findings §1-5.

---

## ⚠️ PARTIAL Status — gap-file follow-up needed

Agent process killed before completing all gap-file writes. Report references **GAP-321..345** (25 IDs, full reserved range) but only **GAP-321..324** (4 files) actually committed. Missing gap files (referenced in report but not yet on disk):

- GAP-325..335 (11 files) — most Stage-2 NEW gaps (BIS bell schedule, học bạ format, conduct hạnh kiểm, parent meeting RSVP, complaint escalation L1-L4, MoET inter-school transfer, teacher BHXH retirement, school closure 30y archive, etc.)
- GAP-336..345 (10 files) — supplementary gaps (parent custody/cross-tenant/guardian + teacher internal msg + leave + resignation + payment gateways + e-invoice TT 78/2021)

**Follow-up:** GAP-346 (filed by Wave 17 closure PR) will recreate these from the report's Findings + AC scoring evidence + Recommendations §Wave-pack opportunity. Report's coverage score (19/100), AC narrative, and Stage 1-5 staging unaffected.
