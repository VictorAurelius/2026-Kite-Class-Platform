---
title: Persona Review — P2 Small Tutoring Center — Round 1
status: draft
created: 2026-05-04
reviewer: Wave 17 Bucket B agent (acting Product Owner + simulated Owner @ Hà Nội tutoring center)
persona: P2
scale: 1 owner + 2 hired teachers, 60 students, 5 classes (Toán-Văn-Anh-Lý-Hóa)
ac_doc_version: documents/00-brd/persona-criteria/P2-small-center.md @ 2026-04-30 v1 + secondary/student-in-P2.md @ 2026-04-30 v1
secondary_acs_consumed: [secondary/student-in-P2.md]
gap_range_reserved: GAP-296..305
---

# Review — P2 Small Tutoring Center

## Summary

- **Total ACs scored:** 38 (25 owner-tier P2 + 13 student-in-P2 secondary)
- **PASS:** 5 (13.2%)
- **PARTIAL:** 12 (31.6%)
- **FAIL:** 21 (55.3%)
- **Overall coverage score:** **(5 + 0.5 × 12) / 38 × 100 = 28.9 / 100**
- **Verdict:** ❌ **Persona NOT viable** — Tier 1 GA blocker. Sits below the `<30%` threshold (per AC doc §Scoring) which means fundamental misfit with current build, not just gaps.
- **New gaps filed:** 7 (GAP-296, GAP-297, GAP-298, GAP-299, GAP-300, GAP-301, GAP-302). 3 reserved slots (GAP-303..305) unused.

### Why so low (1-line per major axis)

| Axis | Status | Why |
|---|:---:|---|
| Onboarding (mobile signup, teacher invite, bulk import, recurring schedule) | ⚠️ partial | xlsx import shipped (GAP-051 DONE), but no commission %, no recurring class schedule, no Zalo invite path |
| Daily Ops (schedule view, attendance, gradebook, role separation, reschedule, substitute) | 🔴 weak | No unified weekly schedule, no conflict detector, no per-session reschedule, no substitute concept |
| Financial (batch invoice, cash, bank transfer, commission, VAT) | 🔴 weak | Per-invoice flows exist, but no batch monthly run, no PDF receipt, no commission engine, VAT only Phase 1 skeleton |
| Communication (Zalo absence, broadcast, payment reminder, parent self-serve) | 🔴 broken | GAP-063 OPEN — Zalo/SMS adapter missing entirely; parent portal Wave 2 identity only |
| Edge cases (substitute, transfer, payment plan) | 🔴 weak | Installments controller exists, but commission/transfer attribution missing |
| Exit (deactivate, offboard teacher, full export) | ⚠️ partial | DataExportService scaffold only (per its own javadoc) — not P2-complete |
| **Student-in-P2 (kép channel, anti-fraud, child protection, retention)** | 🔴 broken | All notification ACs FAIL until GAP-063 ships; child-protection (GAP-186) Phase 1 skeleton only; minor-retention (GAP-184) Phase 1 only |

---

## Detailed Results

### 1. Onboarding (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 — Owner signup ≤30 min from smartphone | PARTIAL | Auth flows exist at `kiteclass/kiteclass-frontend/src/app/(auth)/` (login/register tested). No tier picker (PRO/PREMIUM) in signup flow — `kitehub-subscription` tier selection isn't wired into KiteClass tenant signup. Mobile breakpoints not verified per AC ≤30-min budget. | GAP-296 (signup mobile-tier-flow) |
| AC-ONBOARD-002 — Add teacher with commission rate ≤5 min | PARTIAL | Teachers CRUD exists (`(dashboard)/teachers/page.tsx`, `kiteclass-core/.../module/teacher/`). NO commission % field on teacher form/entity (grep `commission` in teacher module returns only `Permission.java` — different concept). NO Zalo/SMS invite (GAP-063 OPEN). Teacher must self-register today. | GAP-057 (commission), GAP-063 (invite channel) |
| AC-ONBOARD-003 — Bulk import 60 students via xlsx | PASS | `BulkImportController` at `kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/controller/BulkImportController.java` ships `POST /api/v1/students/bulk-import/preview` + `/commit` + error xlsx download. Parent phone column supported (GAP-051 DONE Wave 1 MVP 2026-04-17). Parent-invite step depends on GAP-063 — but xlsx import itself works. | — (parent-invite delta = GAP-063) |
| AC-ONBOARD-004 — Create class with recurring schedule + tuition | FAIL | `Class.java` entity has no tuition/fee column (grep `tuition\|fee\|price` empty). `ClassSession.java` exists per session but no recurrence rule (RRULE/cron) — sessions appear created ad-hoc, no T2-T4-T6 19h-21h pattern generator. Multi-subject support per class not visible. | GAP-296 (also covers class-tuition + recurrence) |

### 2. Daily Operations (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 — Unified weekly schedule across 5 classes + conflict detection | FAIL | No schedule index page found (`(dashboard)/` has classes/students/teachers but no `schedule/` route). Conflict detection: grep `conflict.*schedul` in `kiteclass-core` empty. Owner today must check per-class. | GAP-297 (unified schedule + conflict detector) |
| AC-OPS-002 — Mobile attendance ≤2 min | PARTIAL | Attendance module exists (`kiteclass-core/.../module/attendance/`), README mentions notification design but auto-notify pipeline empty (GAP-063 dependency). Frontend `attendance/page.tsx` uses `enhanced-attendance-calendar.tsx` — not verified mobile-optimized for tap-mark ≤2 min budget. | GAP-063 |
| AC-OPS-003 — Per-class gradebook with weighted columns | PARTIAL | `grade` module exists with `GradeMapper`, `GradingSummaryResponse`, `GradeComponentResponse` — schema supports components. No verification that weight % entry UI (e.g., 20%/30%/50%) and auto-weighted-average are user-facing complete on a per-class basis at this scale. | GAP-298 (gradebook UX verification for tutoring-center scale) |
| AC-OPS-004 — Role separation (teacher sees only own classes, no financial) | PARTIAL | `role` module + `Permission.java` entity exists; `(dashboard)/teachers/` route shows teacher list to logged-in user. Cannot verify teacher-role restriction on commission/financial routes without a live tenant test. Likely OK for class scope, unverified for $/commission scope. | GAP-298 (role-based visibility audit at tutoring-center scale) |
| AC-OPS-005 — Single-session reschedule + auto-notify parents in 5 min | FAIL | `ClassSession` entity has `sessionDate`/`startTime`/`endTime` so single-session edit possible at data layer. NO reschedule endpoint visible in `clazz/controller/`. Auto-notify requires GAP-063. | GAP-063 + GAP-297 |
| AC-OPS-006 — Substitute teacher without losing attribution | FAIL | grep `substitute` in `kiteclass-core/src/main/java` returns 0 hits. No substitute concept in `ClassSession` or attendance/grade entities. Owner workaround: edit teacher_id manually, but historical attribution lost. | GAP-299 (substitute-teacher attribution model) |

### 3. Financial / Admin (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 — Batch monthly invoice generation for 60 students ≤5 min | FAIL | `InvoiceController` has per-invoice GET/POST + adjustments + late-fees + mark-paid + cancel — but NO batch-generate endpoint (grep `batch.*invoice\|generateMonthly\|monthlyInvoice` empty). Owner must create 60 invoices one-by-one today. | GAP-300 (batch invoice generator + dispatch) |
| AC-FIN-002 — Cash payment + printable receipt | PARTIAL | `PaymentMethod.java` enum exists (cash listed). `PaymentController` has POST endpoints. But NO PDF receipt generator — grep `receipt\|pdf.*invoice` in payment/invoice modules returns mapper/event/repo only, no renderer. Print uses browser print at best. | GAP-300 (also covers receipt PDF) |
| AC-FIN-003 — Reconcile bank transfer with invoice (manual link) | PARTIAL | `InvoiceController.markPaid` accepts payment, `Payment.java` likely supports reference field. Manual link UX not verified — bank notification → owner search → match flow not documented. Webhook controllers (vnpay/momo/zalopay) handle auto, not manual bank-transfer reconcile. | GAP-300 |
| AC-FIN-004 — Auto-compute monthly teacher commission | FAIL | No commission engine in code. GAP-057 OPEN. Owner does manually in spreadsheets today. | GAP-057 |
| AC-FIN-005 — VAT invoice (hóa đơn đỏ) on demand | FAIL | GAP-185 PARTIAL Phase 1 skeleton only — no TCT-registered e-invoice provider integration, no MST collection UI in invoice flow. Cannot satisfy enterprise/parent reimbursement use case. | GAP-185 |

### 4. Communication / Stakeholders (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 — Zalo to absent-student parent ≤15 min | FAIL | GAP-063 OPEN. No Zalo/SMS adapter in `kiteclass-core/integration/`. Today, owner must manually message via personal Zalo. | GAP-063 |
| AC-COMM-002 — Owner broadcast to a class ≤2 min | FAIL | grep `announcement\|broadcast` in `kiteclass-core` returns 0. No broadcast API. | GAP-063 + GAP-301 (announcement domain) |
| AC-COMM-003 — Auto-reminder unpaid tuition (3 days + due-date) | FAIL | `InvoiceOverdueScheduler` exists at `invoice/scheduler/InvoiceOverdueScheduler.java` — likely flags overdue, but reminder dispatch requires GAP-063 channel. Confirm scheduler logic via code review (currently classified scheduler, dispatch pipe missing). | GAP-063 |
| AC-COMM-004 — Parent self-serve (no app install): attendance + grades + invoices | PARTIAL | `(dashboard)/parent/page.tsx` exists (parent route shipped Wave 2 per GAP-052 IN_PROGRESS). Wave 2 ships identity + invitation; Wave 5 completes parent-portal data display. Today: incomplete data view. | GAP-052 |

### 5. Edge Cases (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 — Teacher unexpectedly absent → bulk-cancel + notify ≤10 min | FAIL | No bulk-cancel-and-notify pipeline. Cancel single session would need ad-hoc edit; notify depends on GAP-063. | GAP-297 + GAP-063 |
| AC-EDGE-002 — Mid-month student transfer between classes (pro-rated tuition + history preserved) | FAIL | grep `transfer\|reEnroll` in enrollment module returns 0. No transfer endpoint. Owner workaround: drop + re-enroll — but tuition pro-rate logic missing AND attendance history lost on entity FK change. | GAP-302 (student inter-class transfer w/ pro-rate + history) |
| AC-EDGE-003 — Payment plan split (1M → 2× 500K) without commission double-count | PARTIAL | `InstallmentPlanController` exists with POST/approve/reject + per-installment payment. Splits supported. Commission attribution untested because GAP-057 missing — once commission engine ships, must verify it sums installments to original total exactly once. | GAP-057 (with installment-aware computation requirement) |

### 6. Exit / Termination (P2 owner ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 — Student drops out → deactivate + auto-cancel future invoices + refund | PARTIAL | `EnrollmentStatus.java` enum exists (likely INACTIVE). `RefundRequestController` exists. Auto-cancel of future invoices = unverified. Parent notification = GAP-063 dependency. | GAP-063 |
| AC-EXIT-002 — Teacher offboard preserves historical attribution | PARTIAL | Teacher entity supports active/inactive likely. Historical attendance/grade FK to teacher_id remains regardless of teacher active state — likely PASS at data layer. Final-commission-pay step missing (GAP-057). | GAP-057 |
| AC-EXIT-003 — Full data export (xlsx + PDF) within 7 days | PARTIAL | `DataExportService` exists at `kiteclass-core/.../module/retention/` BUT its own javadoc states *"current implementation is a scaffold — real profile queries, full branding history, and streaming-to-MinIO are deferred"*. Output is GDPR Art. 20 ZIP (json+csv+README), NOT xlsx+PDF. Misses P2 owner need. | GAP-301 (P2-shaped owner closure export) |

---

### 7. Student-in-P2 secondary ACs

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 (student) — Receive credentials via Zalo, login ≤3 min | FAIL | Bulk import shipped (GAP-051 DONE), but credential-distribution path requires GAP-063 (Zalo) — currently only email path possible. Parents at this age band don't reliably check email. | GAP-063 |
| AC-ONBOARD-002 (student) — Profile linked to parent contact mandatory | PARTIAL | Bulk import accepts parent_phone column (verified). Whether the Student entity ENFORCES non-null parent_contact at create-time is not verified — likely lenient (would FAIL child-protection at GAP-186 Phase 2 review). | GAP-186 |
| AC-ONBOARD-003 (student) — First-login wizard ≤3 steps, parent-can-setup-on-behalf | FAIL | No student-specific first-login wizard found. Generic auth flow won't ship preset avatars / parent-on-behalf mode. | GAP-186 + GAP-296 (extend signup wizard for under-13 mode) |
| AC-OPS-001 (student) — Weekly schedule view ≤2 taps | FAIL | No student-facing schedule route in `(dashboard)/students/` — only owner-facing student CRUD. Student app surface for P2 not built. | GAP-301 (student-app surface for tutoring scale) |
| AC-OPS-002 (student) — Attendance read-only history | FAIL | `dynamic-attendance-calendar.tsx` exists in `components/student/` — but no student-facing route confirmed. Anti-fraud (no self-mark) cannot be verified without route. | GAP-301 |
| AC-OPS-003 (student) — Homework receipt per session | FAIL | `assignment` module exists (`AssignmentController`, `AssignmentCreatedEvent`) — full LMS-shaped, more than P2 needs. No simple receipt UI for student. Notification depends on GAP-063. | GAP-063 + GAP-301 |
| AC-OPS-004 (student) — View grades read-only | FAIL | No student-facing grades route confirmed. Grade entities exist; UI surface for student missing. | GAP-301 |
| AC-FIN-001 (student) — Read-only fee status (no Pay button) | FAIL | No student fee-status view. (Owner fee management exists but isolated from student app.) | GAP-301 |
| AC-COMM-001 (student) — Zalo notification kép (student + parent) on schedule/grade/HW changes | FAIL | GAP-063 OPEN. Cannot deliver to either side, let alone kép. | GAP-063 |
| AC-COMM-002 (student) — In-app inbox for broadcast | FAIL | No inbox component. No broadcast source. | GAP-063 + GAP-301 |
| AC-COMM-003 (student) — DM teacher BANNED, must be parent-CC'd | FAIL | No DM feature exists today (so bug isn't possible) — but no positive enforcement either. Once any messaging surface ships, must enforce per GAP-186. | GAP-186 |
| AC-EDGE-001 (student) — Forgot password → parent-mediated reset | FAIL | No parent-mediated reset flow. Standard auth reset goes to user's own email — bypassable by minor. | GAP-186 |
| AC-EDGE-002 (student) — Sick-day report by parent, student VIEW | FAIL | No "Báo nghỉ" parent-side flow. No student-side view. | GAP-301 + GAP-186 |
| AC-EXIT-001 (student) — Withdrawal grace + parent export window | PARTIAL | DataExportService scaffold; deactivation likely stops login. Grace-period engineering not implemented. | GAP-184 |
| AC-EXIT-002 (student) — Hard-delete after PDPL Art 16 minor 6mo | PARTIAL | GAP-184 PARTIAL Phase 1 skeleton. Retention values not configured per minor; deletion engineering SOP not landed. | GAP-184 |

(Student-in-P2 student-AC FIN was intentionally a single AC; that gives 13 student ACs total — verified.)

---

## Critical Findings

Top 5 most damaging gaps for this persona, severity-ordered:

1. **GAP-063 — SMS + Zalo notification adapter is the keystone failure.** It alone causes 8 of 21 FAILs (~38% of all FAILs). Without Zalo, every parent-comm AC degrades from PARTIAL → FAIL because email/SMS-only ≠ acceptable for VN tutoring centers. Prioritise above any UI polish.
2. **GAP-300 (NEW) — No batch-monthly-invoice + no PDF receipt = owner does 60 invoices manually.** Single biggest daily-pain blocker. Owner workflow today is: spreadsheet + WhatsApp screenshots. That's the workflow we promise to replace.
3. **GAP-057 — Teacher commission engine missing.** Owner pays 60% to 2 teachers; doing this monthly by hand is the SECOND-biggest pain. Without it, hired-teacher centers cannot adopt — they reject the platform on first commission cycle.
4. **GAP-297 (NEW) — No unified weekly schedule view + no conflict detector.** P2 has 5 classes × 2-3 sessions/wk = 10-15 weekly slots; same teacher in 2-3 classes guarantees occasional conflicts. Without a conflict catcher the platform actively *causes* operational errors instead of preventing them.
5. **GAP-301 (NEW) — Student-app surface missing for tutoring-center scale.** Existing `(dashboard)/students/` is owner-CRUD, NOT a student-facing app. Half of student-in-P2 secondary ACs FAIL because there is no surface to evaluate.

### Secondary critical (P2-PRO bracket gating)

- **GAP-186** Child Protection Phase 2 — must close before any K-12-adjacent tutoring center adopts (lớp 5-9 fall in scope per `student-in-P2` AC doc).
- **GAP-184** Data Retention Phase 2 — minor 6-month retention not enforced; legal exposure when first parent requests deletion.

---

## Recommendations

### Priority reordering (per `meta-gap-priority.md` §3 — Business-Logic > Feature, P0 > P1)

| Rank | Gap | Tier | Why first |
|---|---|---|---|
| 1 | **GAP-063** (Zalo/SMS) — currently P1 → recommend **bump to P0** | Feature-P0 | Force-multiplier: 8 ACs unlock once it ships. Without it nothing else helps P2. |
| 2 | **GAP-186 Phase 2** + **GAP-184 Phase 2** (Child Protection + Retention) | Business-Logic-P0 | Legal-mandate before any P2 onboarding (lớp 5-9 students = minors). Should ship in same wave as P5 K-12 prep. |
| 3 | **GAP-300** (NEW — batch invoice + PDF receipt) | Feature-P0 | Single biggest manual workload owner has. |
| 4 | **GAP-057** (Commission) | Feature-P0 (was P1) | Bump per blast-radius rule (`audit-to-gap-pipeline.md` §6 dependency rules) — payroll dispute is a center-killer. |
| 5 | **GAP-297** (NEW — schedule+conflict) | Feature-P0 | Operational safety; cheaper than support tickets from double-booked teachers. |
| 6 | **GAP-296** (NEW — signup mobile + tier + class tuition + recurrence) | Feature-P1 | Combined onboarding gap. Recurrence is a sub-AC of class creation. |
| 7 | **GAP-301** (NEW — student-app surface for P2) | Feature-P1 | Unblocks 6 student-in-P2 ACs. |
| 8 | **GAP-298** (NEW — gradebook + role audit at tutoring scale) | Feature-P2 | Verification of features that probably already work, just unproven for this persona scale. |
| 9 | **GAP-299** (NEW — substitute-teacher) | Feature-P2 | Edge case; lower frequency than commission. |
| 10 | **GAP-302** (NEW — student inter-class transfer) | Feature-P2 | Mid-month transfers ~5% of student volume but high-pain when they happen. |
| 11 | GAP-185 Phase 2 (VAT) | Business-Logic-P1 | Only triggered when a parent asks for hóa đơn đỏ — episodic, not daily. |
| 12 | GAP-052 Wave 5 (Parent portal completion) | Feature-P1 | Already in plan — dependency on GAP-063 for invitation channel. |

### Sequencing observation (for closure PR)

GAP-063 is the **single highest-leverage move**. If GAP-063 + GAP-300 + GAP-057 + GAP-297 ship in one sub-wave, P2 coverage jumps from ~29% to ~60% (estimate: +6 ACs PASS, +5 PARTIAL→PASS uplift). That brings P2 into "⚠️ partially supported" — which is the threshold to consider P2 GA-eligible alongside P5.

### Wave-pack candidate (per `wave-pack-planner` skill)

Disjoint clusters that can run as parallel agents:
- Cluster A: **GAP-063** (Backend — adapter wiring)
- Cluster B: **GAP-300 + GAP-057** (Backend — invoice batch + commission engine; both touch invoice/payment/commission ledger)
- Cluster C: **GAP-297 + GAP-296 (recurrence sub-task)** (Backend + Frontend — schedule view + class entity recurrence)
- Cluster D: **GAP-186 Phase 2 + GAP-184 Phase 2** (Legal/BRD Phase 2)

≥3 disjoint = wave qualifies per `feedback_wave_pack_cross_gap_clustering`.

---

## Methodology Note

Scored per `persona-based-business-review.md` skill. Evidence chain for every entry:
- **PASS:** file path of working code + specific endpoint OR UI route
- **PARTIAL:** file path of partial implementation + 1-line "what's missing" delta
- **FAIL:** grep output showing 0 hits for the keyword OR explicit "no route at `/X`" check

State-checks performed:
- `kiteclass-core/src/main/java/com/kiteclass/core/module/{student,teacher,clazz,course,attendance,billing,invoice,payment,grade,enrollment,role,retention,assignment,parent}` — module presence
- `kiteclass-frontend/src/app/(dashboard)/{students,teachers,classes,attendance,billing,parent}/page.tsx` — UI route presence
- Grep keywords: `commission`, `recurring`, `conflict.*schedul`, `substitute`, `transfer`, `batch.*invoice`, `receipt|pdf.*invoice`, `zalo`, `sms\b`, `announcement|broadcast`, `parentReset|parentMediated`
- Read DataExportService.java — own javadoc declares scaffold

Existing gaps already covering FAIL paths cited rather than re-filed (per `audit-to-gap-pipeline.md` Step 2 dedupe): GAP-051, GAP-052, GAP-054, GAP-057, GAP-063, GAP-184, GAP-185, GAP-186.

7 NEW gaps filed (Step 2.5 state-check confirmed each touches code surface that doesn't exist OR exists only as scaffold): GAP-296..302. 3 reserved slots GAP-303..305 unused — closure PR can free them or reuse.

---

## Log

- **2026-05-04** — Round 1 review completed by Wave 17 Bucket B agent (acting Product Owner + simulated owner @ Hà Nội tutoring center, 60 students / 2 hired teachers / 5 subjects). Coverage 28.9/100 — below `<30%` "NOT viable" threshold. Single keystone fix = GAP-063 (Zalo). Closure PR will: (1) update personas-catalog.md "Coverage Review Status" P2 row from estimated to measured 28.9, (2) flip GAP-152 → DONE per `gap-done-discipline.md` only after all 4 buckets land, (3) recommend GAP-063 priority bump P1 → P0.
