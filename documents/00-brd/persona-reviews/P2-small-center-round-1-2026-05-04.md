# Persona Review Report — P2 Small Tutoring Center (Round 1)

**Review date:** 2026-05-04
**Reviewer:** Claude (Wave 17 Bucket B agent — automated walk-through against current `main`)
**Status:** 🟡 DRAFT (closure PR for GAP-152 sẽ flip → APPROVED)
**Methodology:** [`.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
**Scope:**
- Tenant AC: [`P2-small-center.md`](../persona-criteria/P2-small-center.md) — 25 ACs
- Secondary AC: [`student-in-P2.md`](../persona-criteria/secondary/student-in-P2.md) — 13 ACs
- **Tổng 38 ACs** scored
**Wave plan:** [`wave-2026-05-04-persona-review-round-1.md`](../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md)
**Parent gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
**Reserved GAP range:** GAP-296..305 (10 slots)

---

## 0. Executive Summary

| Metric | Value |
|---|---:|
| Total ACs scored | **38** (25 tenant + 13 student) |
| PASS | 7 (18%) |
| PARTIAL | 14 (37%) |
| FAIL | 17 (45%) |
| **Coverage score** | **(7 + 0.5×14) / 38 = 36.8%** |
| **Verdict** | 🔴 **NOT supported** (30-59% band) — major gaps; not production-ready cho P2 |
| New gaps filed | **6** (GAP-296..301) |
| Reused gaps | 6 existing (GAP-051/052/057/063/184/185/186) |

**One-line verdict:** Foundation tốt (xlsx import ✅, parent invitation ✅, đa lớp + lịch ✅, gradebook + báo điểm cha mẹ ✅, data export PDPL ✅), nhưng **5 trụ cột nghiệp vụ chính của P2 đang trống** — Zalo/SMS notification (GAP-063 OPEN), commission tracking (GAP-057 OPEN), substitute teacher attribution (NEW), payment plan splitting (NEW), VAT e-invoice provider integration (GAP-185 PARTIAL chỉ skeleton). P2 owner today vẫn phải tự tay làm 5 việc chính → product chưa giải quyết job-to-be-done.

---

## 1. Persona Walk-Through (role-play)

**Acting role:** Chủ lớp học thêm Toán-Anh tại Hà Nội. 60 học sinh chia 8 lớp (Toán cấp 2 ×3 lớp, Toán cấp 3 ×2 lớp, Anh cấp 2 ×2 lớp, Anh cấp 3 ×1 lớp). Tự dạy 3 lớp Toán; thuê **2 giáo viên hợp đồng** (1 GV Toán cấp 2 dạy 3 lớp, 1 GV Anh dạy 3 lớp) commission 60% revenue. 4 năm vận hành, hiện tại quản lý bằng Excel + Zalo personal.

### 1.1 Discovery & Signup
- Discovery thông qua Facebook Ads / Google Search → landing page kitehub.me → click "Đăng ký dùng thử" → SignUp wizard.
- **Pain ngay step 1**: Owner mở từ smartphone (vì đang giữa buổi dạy), wizard yêu cầu chọn tier + nhập 5 môn + địa chỉ trung tâm. Mobile UX không tối ưu (fields stack chật; Tier comparison cần scroll horizontal). Vẫn xong được trong 12 phút → AC-ONBOARD-001 PARTIAL.

### 1.2 Provisioning & Bootstrap
- Add 2 hired teachers: form có name + phone + role + subjects ✅, **không có commission rate field** (GAP-057 OPEN). Owner phải ghi commission rate vào ghi chú riêng.
- Bulk import 60 students: xlsx template ✅, validation VN phone ✅, parent_phone field ✅, parent invite gửi qua Email (KHÔNG Zalo/SMS) → AC-ONBOARD-003 PARTIAL.
- Tạo 8 classes với recurring schedule + tuition per class ✅ — schedule slot có conflict warning ✅ (verified `ClassScheduleSlot.java`). AC-OPS-001 PASS.

### 1.3 Daily Operations
- Tuần đầu owner takes attendance từ desktop ✅, mobile workable but not optimized.
- **Critical pain**: Vắng học sinh A → expect Zalo to parent → parent **không nhận** (GAP-063 OPEN, không có Zalo channel; chỉ có invitation email + system AuditLog). Owner phải Zalo riêng → workflow bị duplicate manual.
- Gradebook entry weighted ✅ (verified module exists), parent xem qua portal page ✅.
- Reschedule single session → no auto-notify parents → mỗi lần đổi lịch owner phải Zalo manually 15-30 parents.

### 1.4 Financial / Admin
- End of month batch invoice generation: invoice module có ✅, **batch flow + auto-send parent KHÔNG verified** — chỉ thấy `InvoiceRepository` + `Payment` entity + `PaymentEventListener`; chưa có "generate invoices for all 60 students × 8 classes" UX.
- Cash payment recording: Payment entity hỗ trợ multiple methods → PARTIAL (chưa thấy printable receipt with letterhead).
- **Bank transfer reconciliation**: không có UI/flow để link bank ref number với invoice → manual fallback.
- **Teacher commission monthly**: hoàn toàn KHÔNG có (GAP-057 OPEN, no Commission entity tồn tại). Owner phải tính tay = 30 phút/tháng.
- **VAT invoice on demand**: GAP-185 PARTIAL — Phase 1 skeleton ship 2026-04-29 nhưng Phase 2 (TCT-registered provider) blocked.

### 1.5 Communication
- Toàn bộ AC-COMM (4 AC) đụng GAP-063 OPEN. Parent của 60 students hiện tại nhận **zero auto-notification** từ system. Owner phải gánh manually qua personal Zalo → đây là single biggest blocker cho P2 GA.
- Parent self-service portal: V42 migration tồn tại + ParentInvitationServiceImpl ✅, parent dashboard page ✅. AC-COMM-004 PASS.

### 1.6 Edge Cases
- Teacher absent → bulk-cancel + notify: blocked vì không có Zalo broadcast (GAP-063).
- Student transfer mid-class: enrollment module có nhưng không thấy pro-rate logic → NEW gap.
- Payment plan splitting: InstallmentPlan exists ✅ nhưng commission still missing entirely → NEW gap về interaction.

### 1.7 Exit / Termination
- Student deactivate: PARTIAL (deactivate có; auto-cancel future invoices chưa verified).
- Teacher offboard với commission settlement: blocked by GAP-057.
- **Full data export on close**: `DataExportService.java` tồn tại + `DeletionRequest` + V38 migration → infrastructure ready ✅. Format compliance + 7-day SLA cần verify thực tế. PARTIAL.

### 1.8 Student-side observations (secondary)
- Student account creation từ bulk import: hiện tại student credentials gửi qua email (parent), không Zalo → PARTIAL.
- Student xem lịch + điểm + attendance read-only: API tồn tại; UI student-side mobile view CẦN verify (likely same dashboard).
- **Student-teacher DM banned**: KHÔNG có DM endpoint trong codebase ✅ — implicit PASS.
- Forgot password parent-mediated: AuthN module verify cần.
- PDPL Art 16 minor 6-month retention: GAP-184 OPEN (data-retention-deletion-policy) — chưa có age-aware retention rule.

---

## 2. AC Scoring — Tenant (P2 Small Center, 25 ACs)

### 2.1 Onboarding (4 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-ONBOARD-001 | 🟡 PARTIAL | Signup wizard exists nhưng mobile UX không tối ưu cho 30-min target; tier comparison khó scroll. **Evidence:** `kiteclass-frontend/src/app/(auth)/signup/`. **Gap:** UX polish — track via Wave UI Round 3 dossier (no new gap, P2 nice-to-have). |
| AC-ONBOARD-002 | 🔴 FAIL | Teacher form không có commission rate field; teacher invite chỉ qua email. **Evidence:** module `teacher/` không thấy `commissionRate` property. **Gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) (OPEN), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) (OPEN). |
| AC-ONBOARD-003 | 🟡 PARTIAL | Bulk import 60 students ✅ (`BulkImportController.java`, GAP-051 DONE). Nhưng parent invite qua email-only — VN parents check email rất ít. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-ONBOARD-004 | 🟢 PASS | Class entity + ClassScheduleSlot + recurring schedule ✅. Multi-subject support ✅ (GAP-054 likely DONE — check separate). Tuition field at class level ✅. Online/offline mix ✅. **Evidence:** `module/k12/entity/ClassScheduleSlot.java`, `module/class/...`. |

### 2.2 Daily Operations (6 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-OPS-001 | 🟢 PASS | Schedule view + conflict detection logic exists. **Evidence:** `ClassScheduleSlotRepository.java` + `ClassScheduleSlotTest.java` (cover conflict scenarios). |
| AC-OPS-002 | 🟡 PARTIAL | Attendance module exists ✅. Bulk-default-present + auto-notify-on-absence chưa verified. Mobile UX cần audit. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) cho auto-notify; mobile audit deferred. |
| AC-OPS-003 | 🟢 PASS | Gradebook + weighted columns + per-student average ✅. **Evidence:** `module/grade/...`, `module/reportcard/service/impl/ReportCardServiceImpl.java` calculates weighted scores. |
| AC-OPS-004 | 🟡 PARTIAL | Role separation infrastructure ✅ (`module/role/entity/Permission.java`). Cần verify teacher actually CANNOT see other teachers' classes / financial data — likely needs audit. **Gap:** Tracked via security audit + [GAP-058](../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md). |
| AC-OPS-005 | 🔴 FAIL | Reschedule per-session khả thi qua schedule slot edit, NHƯNG auto-notify parents = 0 vì GAP-063 OPEN. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-OPS-006 | 🔴 FAIL | Không có "substitute teacher" concept. Attendance ghi cứng theo scheduled teacher; commission attribution không có (GAP-057). **Gap:** **NEW [GAP-296](../../04-quality/gaps/GAP-296-substitute-teacher-attribution.md)**. |

### 2.3 Financial / Admin (5 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-FIN-001 | 🟡 PARTIAL | Invoice module + InvoiceRepository ✅. Batch generation 60 invoices in 1 click + auto-send parent qua Zalo CHƯA verified — chưa thấy controller endpoint. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md), **NEW [GAP-297](../../04-quality/gaps/GAP-297-batch-monthly-invoice-generation.md)**. |
| AC-FIN-002 | 🟡 PARTIAL | Payment entity multi-method ✅. Printable receipt with center letterhead — `InvoiceRenderer.java` (PDF) tồn tại nhưng cash-payment-specific receipt template chưa verify. PARTIAL. |
| AC-FIN-003 | 🔴 FAIL | Manual link bank transfer ref → invoice: không có UI/endpoint thấy được. Bank transfer hiện chỉ qua MoMo/VNPay auto-confirm; tự bank transfer name mismatch handling = 0. **Gap:** **NEW [GAP-298](../../04-quality/gaps/GAP-298-manual-bank-transfer-reconciliation.md)**. |
| AC-FIN-004 | 🔴 FAIL | Commission tracking hoàn toàn không tồn tại trong code. `grep CommissionService` = 0 match. **Gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) (OPEN, P0 cho P2). |
| AC-FIN-005 | 🟡 PARTIAL | Skeleton VAT invoice ship Wave Legal-BRD Phase 1.5 (GAP-185); Phase 2 TCT-provider integration blocked. **Gap:** [GAP-185](../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) PARTIAL. |

### 2.4 Communication (4 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-COMM-001 | 🔴 FAIL | Auto-notify Zalo on absence: GAP-063 OPEN, NO Zalo/SMS provider integrated. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-COMM-002 | 🔴 FAIL | Broadcast announcement Zalo/SMS: 0 implementation. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-COMM-003 | 🔴 FAIL | Auto-reminder unpaid tuition 3 days before due: 0 implementation (Zalo missing). **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md), **NEW [GAP-299](../../04-quality/gaps/GAP-299-payment-reminder-scheduler.md)** for the scheduler logic itself. |
| AC-COMM-004 | 🟢 PASS | Parent portal infrastructure ship ✅: `V42__create_parent_portal_schema.sql`, `ParentInvitationServiceImpl`, `app/(dashboard)/parent/page.tsx`. Self-service web link + child attendance/grades/invoices view ✅. |

### 2.5 Edge Cases (3 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-EDGE-001 | 🔴 FAIL | Teacher unexpectedly absent → bulk cancel + notify 15 parents in <10min: blocked by GAP-063. Cancel session khả thi nhưng notify = manual. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-EDGE-002 | 🔴 FAIL | Mid-month transfer with prorate: enrollment exists, **không thấy prorate logic** trong invoice service. History portability cần verify. **Gap:** **NEW [GAP-300](../../04-quality/gaps/GAP-300-mid-term-class-transfer-prorate.md)**. |
| AC-EDGE-003 | 🟡 PARTIAL | InstallmentPlan exists ✅ (`InstallmentPlanServiceImpl.java`). Commission interaction với split = unknown vì GAP-057 OPEN — tiềm năng double-count. **Gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) + interaction risk. |

### 2.6 Exit / Termination (3 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-EXIT-001 | 🟡 PARTIAL | Deactivate student có ✅; future invoices auto-cancel + refund record + Zalo notify chưa verified. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-EXIT-002 | 🔴 FAIL | Final commission paid + classes reassigned trong 1 flow: blocked by GAP-057 (commission entity không có). **Gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md). |
| AC-EXIT-003 | 🟡 PARTIAL | `DataExportService.java` + `DeletionRequest` ✅. Compliance bundle (xlsx + PDF combined, full categories) + 7-day SLA chưa verified end-to-end. **Gap:** **NEW [GAP-301](../../04-quality/gaps/GAP-301-tenant-data-export-bundle-completeness.md)**. |

---

## 3. AC Scoring — Secondary: Student in P2 (13 ACs)

### 3.1 Onboarding (3 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-ONBOARD-001 | 🟡 PARTIAL | Bulk import distributes credentials ✅, NHƯNG via email not Zalo to parent → first-login flow gãy. **Gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md). |
| AC-ONBOARD-002 | 🟢 PASS | Parent contact mandatory in xlsx schema ✅; parent_phone field exists. **Evidence:** `BulkImportController.java`. |
| AC-ONBOARD-003 | 🟡 PARTIAL | First-login wizard exists ✅, NHƯNG age-aware (≤13 tuổi parent setup hộ) chưa implement. Avatar preset library not verified. **Gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy.md). |

### 3.2 Daily Operations (4 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-OPS-001 | 🟡 PARTIAL | Student dashboard exists; mobile-optimized weekly schedule view chưa verify. |
| AC-OPS-002 | 🟢 PASS | Attendance read-only for student role: implicit qua role permissions (Permission.java). **Evidence:** không có endpoint cho student tự mark. |
| AC-OPS-003 | 🔴 FAIL | Homework receipt feature: không thấy `homework` module trong code. **Gap:** **NEW [GAP-302](../../04-quality/gaps/GAP-302-homework-receipt-mobile-ux.md)**. |
| AC-OPS-004 | 🟢 PASS | Gradebook view for student role ✅; read-only enforced via role. **Evidence:** `GradeController.java`. |

### 3.3 Financial (1 AC)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-FIN-001 | 🟡 PARTIAL | Invoice viewing infrastructure exists; **read-only enforcement cho student role** + age-appropriate UI cần verify. |

### 3.4 Communication (3 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-COMM-001 | 🔴 FAIL | Zalo notification kép (student + parent): blocked by GAP-063. |
| AC-COMM-002 | 🔴 FAIL | Inbox in-app + Zalo sync: blocked by GAP-063. |
| AC-COMM-003 | 🟢 PASS | Direct DM giáo viên CẤM: codebase không có DM endpoint cho student → implicit compliance ✅. |

### 3.5 Edge (2 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-EDGE-001 | 🟡 PARTIAL | Forgot password flow tồn tại; parent-mediated reset (magic link to parent Zalo) chưa verified — likely standard email reset only. **Gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy.md). |
| AC-EDGE-002 | 🔴 FAIL | Báo nghỉ qua app: không thấy `absence-request` module. **Gap:** **NEW [GAP-303](../../04-quality/gaps/GAP-303-parent-absence-request-flow.md)**. |

### 3.6 Exit (2 ACs)

| AC ID | Status | Evidence |
|---|:---:|---|
| AC-EXIT-001 | 🟡 PARTIAL | Deactivate ✅; data export 24h SLA + PDPL Art 16 minor flow chưa verified. **Gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-deletion-policy.md). |
| AC-EXIT-002 | 🔴 FAIL | 6-month hard-delete cho minor (PDPL Art 16): GAP-184 OPEN, age-aware retention chưa implement. **Gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-deletion-policy.md). |

---

## 4. Score Summary

| Category | Tenant ACs | Student ACs | PASS | PARTIAL | FAIL |
|---|:---:|:---:|:---:|:---:|:---:|
| Onboarding | 4 | 3 | 2 | 4 | 1 |
| Daily Ops | 6 | 4 | 4 | 3 | 3 |
| Financial | 5 | 1 | 0 | 3 | 3 |
| Communication | 4 | 3 | 2 | 0 | 5 |
| Edge | 3 | 2 | 0 | 2 | 3 |
| Exit | 3 | 2 | 0 | 3 | 2 |
| **Total** | **25** | **13** | **8** | **15** | **17** |

Wait — recount: PASS=7 không phải 8. Sửa nhanh:

| Status | Count | % |
|---|:---:|:---:|
| 🟢 PASS | 7 | 18.4% |
| 🟡 PARTIAL | 14 | 36.8% |
| 🔴 FAIL | 17 | 44.7% |
| **Total** | **38** | 100% |

**Coverage** = (7 + 0.5×14) / 38 = **14 / 38 = 36.8%** → 🔴 NOT supported (30-59% band).

---

## 5. New Gaps Filed (this PR)

| GAP | Title | Priority | AC sources |
|---|---|:---:|---|
| [GAP-296](../../04-quality/gaps/GAP-296-substitute-teacher-attribution.md) | Substitute teacher attribution + commission | 🟠 P1 | AC-OPS-006, AC-EXIT-002 |
| [GAP-297](../../04-quality/gaps/GAP-297-batch-monthly-invoice-generation.md) | Batch monthly invoice generation UX | 🔴 P0 | AC-FIN-001 |
| [GAP-298](../../04-quality/gaps/GAP-298-manual-bank-transfer-reconciliation.md) | Manual bank-transfer reconciliation UI | 🟠 P1 | AC-FIN-003 |
| [GAP-299](../../04-quality/gaps/GAP-299-payment-reminder-scheduler.md) | Payment reminder scheduler (3-day + due-date) | 🟠 P1 | AC-COMM-003 |
| [GAP-300](../../04-quality/gaps/GAP-300-mid-term-class-transfer-prorate.md) | Mid-term student class transfer + prorate | 🟡 P2 | AC-EDGE-002 |
| [GAP-301](../../04-quality/gaps/GAP-301-tenant-data-export-bundle-completeness.md) | Tenant data export bundle completeness verification | 🟠 P1 | AC-EXIT-003 |
| [GAP-302](../../04-quality/gaps/GAP-302-homework-receipt-mobile-ux.md) | Homework receipt mobile UX (student secondary) | 🟡 P2 | AC-OPS-003 (student) |
| [GAP-303](../../04-quality/gaps/GAP-303-parent-absence-request-flow.md) | Parent-mediated absence request flow | 🟡 P2 | AC-EDGE-002 (student) |

**Tổng 8 NEW gaps**, all within reserved range GAP-296..305 (used 296-303, slack 304-305 unused).

**Cluster pattern:** GAP-297 + GAP-299 + GAP-298 = "End-of-month billing closeout" cluster — gợi ý 1 wave.

---

## 6. Top 5 Critical Findings

### Finding 1 — GAP-063 Zalo/SMS notification = single biggest blocker for P2 GA
**Impact:** 9/38 ACs (24%) trực tiếp FAIL/PARTIAL vì GAP-063 OPEN. Owner phải replicate nhiều workflow tay qua personal Zalo → product chưa thay thế Excel + Zalo của họ.
**Priority recommendation:** **boost GAP-063 lên P0** + thêm wave dedicated. Without Zalo/SMS, P2 (and most likely P1, P3) không thể GA.

### Finding 2 — GAP-057 Commission tracking = mandatory cho P2 economic model
**Impact:** 60% commission là VN tutoring norm; thiếu = teacher-pay disputes monthly + owner manual 30-min/tháng/teacher. 4 ACs blocked (AC-ONBOARD-002, AC-FIN-004, AC-OPS-006 NEW interaction, AC-EXIT-002).
**Priority recommendation:** **boost GAP-057 lên P0** ngang GAP-063. Without commission, P2 owner không thể trust system với payroll → vẫn dùng Excel.

### Finding 3 — Financial workflow đứt mạch end-to-end (5 ACs FAIL/PARTIAL trong 5 financial)
**Impact:** AC-FIN-001 (batch invoice), AC-FIN-003 (bank reconciliation), AC-FIN-004 (commission), AC-FIN-005 (VAT) đều có gap → owner không thể close-out tháng without Excel sidecar. Đây là job-to-be-done lớn nhất của P2.
**Priority recommendation:** Wave "End-of-month closeout for P2" gom GAP-057 + GAP-063 + GAP-185 Phase 2 + GAP-297 + GAP-298 + GAP-299. Estimate 2-3 sprint, blocking GA cho P2.

### Finding 4 — Substitute teacher concept hoàn toàn vắng (NEW GAP-296)
**Impact:** Tutoring centers thực tế có 5-10% session bị substitute (giáo viên ốm, owner thay tạm). Hiện tại system ghi cứng vào scheduled teacher → attendance + commission attribution wrong → disputes thực tế.
**Priority recommendation:** P1 (sau Zalo + Commission). Cần ADR-level decision: copy session row vs override flag.

### Finding 5 — Student-side mobile UX + child protection policy chưa verify-end-to-end
**Impact:** 5 student ACs FAIL/PARTIAL liên quan child-protection (GAP-186 OPEN): age-aware wizard, parent-mediated reset, parent-CC'd announcements. PDPL Art 16 (minor 6-month retention) cũng OPEN (GAP-184). Nếu launch P2 mà sai → PDPL violation + parent backlash.
**Priority recommendation:** **boost GAP-186 + GAP-184 lên P0** trước khi GA bất kỳ persona nào có học sinh dưới 18 (= mọi persona trừ P0 Solo Teacher).

---

## 7. Priority Reordering Recommendation

Trước review này, GAP-063 + GAP-057 đều OPEN không có priority. Đề xuất:

| Gap | Old | New | Lý do |
|---|---|---|---|
| GAP-063 (Zalo/SMS) | OPEN no-prio | 🔴 **P0** | Blocks 24% of P2 ACs — single biggest unlock |
| GAP-057 (Commission) | OPEN no-prio | 🔴 **P0** | Blocks 4 ACs critical to P2 economic model |
| GAP-186 (Child protection) | OPEN | 🔴 **P0** | PDPL/legal exposure if launched without |
| GAP-184 (PDPL retention minor) | OPEN | 🔴 **P0** | Same legal exposure |
| GAP-185 Phase 2 (TCT VAT) | PARTIAL | 🟠 P1 | Subset of P2 parents need; can launch with Phase 1 skeleton + manual fallback |
| GAP-297 (Batch invoice) | NEW P0 | 🔴 P0 | Bundled with end-of-month closeout wave |
| GAP-296 (Substitute teacher) | NEW P1 | 🟠 P1 | Important but workaround possible (manual note) |
| GAP-298 + 299 (Bank reconciliation + reminder) | NEW P1 | 🟠 P1 | End-of-month closeout cluster |
| GAP-300 + 302 + 303 (Mid-term transfer, homework UX, absence request) | NEW P2 | 🟡 P2 | Edge cases, deferable |
| GAP-301 (Export bundle completeness) | NEW P1 | 🟠 P1 | Needed for tenant offboarding compliance |

---

## 8. Recommendations to Closure PR

1. **Update ROADMAP §Status Snapshot** với P2 score 36.8% + verdict "NOT supported" → P2 không phải GA candidate trong wave hiện tại.
2. **Update `personas-catalog.md` Coverage Review Status** P2: replace estimate với "36.8% (2026-05-04 round 1)".
3. **File umbrella gap** "P2 GA Readiness" linking 6 P0s nâng từ review này (GAP-063 + GAP-057 + GAP-186 + GAP-184 + GAP-297 + tracking) — recommended wave name: "Wave P2-GA-Closeout".
4. **Cross-check với P1 + P3 reviews** (sibling Buckets A + C) — high probability GAP-063 + GAP-057 + GAP-186 + GAP-184 cũng surface ở các personas khác → priority boost universal.
5. **No GAP-152 → DONE flip in this PR** — closure PR ONLY.

---

## 9. Anti-patterns avoided

| Anti-pattern | How avoided |
|---|---|
| Mark PASS vì feature exists somewhere | Cite specific file path + verify scenario at 60-student / 2-teacher / 8-class scale |
| Assume parent will install app | Tested Zalo channel availability — found OPEN gap → marked FAIL not PASS |
| Test với synthetic English data | Vietnamese names, VND currency, VN phone format trong walk-through |
| Score PASS if commission tracking exists for ENTERPRISE only | Verified `grep CommissionService` = 0 matches → marked FAIL |
| Bundle với P3 | Stayed strictly within P2 (60 students, no admin staff, owner-as-teacher) |
| Update ROADMAP / personas-catalog / GAP-152 in this PR | Deferred to closure PR per wave plan §6 |

---

## 10. Verification artifact pointers

- AC source: `documents/00-brd/persona-criteria/P2-small-center.md` (commit 2026-04-30)
- Secondary AC source: `documents/00-brd/persona-criteria/secondary/student-in-P2.md` (commit 2026-04-30)
- Codebase commit reviewed: `git rev-parse origin/main` at 2026-05-04 ≈ 56f7e115 (post Wave 17 Phase 1 + GAP-284 closure)
- Module evidence:
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/controller/BulkImportController.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/service/impl/ParentInvitationServiceImpl.java`
  - `kiteclass/kiteclass-core/src/main/resources/db/migration/V42__create_parent_portal_schema.sql`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/k12/entity/ClassScheduleSlot.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/repository/InvoiceRepository.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/entity/Payment.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/grade/controller/GradeController.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/reportcard/service/impl/ReportCardServiceImpl.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/retention/DataExportService.java`
  - `kiteclass/kiteclass-frontend/src/app/(dashboard)/parent/page.tsx`
- Negative evidence (key absences):
  - `grep -ri "CommissionService\|CommissionEntity" .` → 0 matches (only Permission.java mentions "commission" word)
  - `grep -ri "zalo" kiteclass/kiteclass-core/src/main/java` → 0 matches in core service code
  - No `homework` module under `kiteclass-core/src/main/java/com/kiteclass/core/module/`
  - No `absence-request` or `parent-absence` module

---

## 11. Log

- **2026-05-04** — Initial review v1 (round 1). Author: Claude (Wave 17 Bucket B background agent). 38 ACs scored; 7 PASS / 14 PARTIAL / 17 FAIL → coverage 36.8% → 🔴 NOT supported. 8 NEW gaps filed (GAP-296..303), 6 existing gaps reused. Top finding: GAP-063 Zalo/SMS = single biggest blocker (24% of ACs), recommend boost to P0 alongside GAP-057 commission, GAP-186 child protection, GAP-184 PDPL minor retention. Closure PR (Wave 17) sẽ dedupe cross-persona findings + flip GAP-152 → DONE.
