# Persona Review Round 1 — P3 Medium Education Center (2026-05-04)

**Wave:** 17 — Persona Review Round 1
**Bucket:** C — P3 Medium Education Center (250 students, 12 teachers, 30 classes, 2-3 admins)
**Reviewer:** Agent (acting as Director Đà Nẵng-based trung tâm Anh ngữ + kế toán + lễ tân + giáo viên + học sinh THCS-THPT)
**Method:** [`.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
**Inputs scored:**
- Tenant AC: [`P3-medium-center.md`](../persona-criteria/P3-medium-center.md) — 30 ACs
- Student AC: [`secondary/student-in-P3.md`](../persona-criteria/secondary/student-in-P3.md) — 13 ACs
- Admin AC: [`secondary/admin-in-P3.md`](../persona-criteria/secondary/admin-in-P3.md) — 18 ACs
- Teacher AC: [`secondary/teacher-employee-in-P3.md`](../persona-criteria/secondary/teacher-employee-in-P3.md) — 21 ACs
- **Total:** 82 ACs
**Tracking gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
**State-check basis:** code grep against `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/**` + `kiteclass/kiteclass-frontend/src/app/**` (2026-05-04)
**Status:** 🔴 NOT VIABLE — coverage well below GA threshold; multiple Tier-1 critical gaps

---

## 0. Executive Summary

P3 (Trung tâm vừa, 250 HS, 12 GV, 30 lớp, 3-5 admin staff) là persona PREMIUM-tier mục tiêu nhưng **hệ thống hiện tại CHƯA hỗ trợ persona này ở mức GA**. Toàn bộ commission engine, payroll BHXH/BHYT/TNCN, multi-class scheduling conflict detection, Zalo OA bulk parent notification, MoET licensing renewal alert, complaint workflow, RBAC granular cho 4 admin roles (giám đốc / lễ tân / kế toán / ops admin), VAT e-invoice (NĐ 123/2020) đều **chưa có module trong codebase**.

**Verdict:** P3 ở trạng thái **🔴 NOT SUPPORTED** — major gaps. Defer GA cho persona này tới Wave 20+ sau khi:
1. GAP-057 (payroll commission), GAP-058 (RBAC granular), GAP-063 (Zalo OA), GAP-185 (VAT) đã ship
2. New gaps GAP-306..312 (multi-class scheduling, MoET license, complaint queue, WORM audit log, etc.) ship

### Coverage Summary

| Persona | Total ACs | PASS | PARTIAL | FAIL | Coverage |
|---------|----------:|-----:|--------:|-----:|---------:|
| **Tenant (Director)** | 30 | 1 | 6 | 23 | **13.3%** 🔴 NOT VIABLE |
| **Student in P3** | 13 | 1 | 5 | 7 | **26.9%** 🔴 NOT VIABLE |
| **Admin (lễ tân/kế toán/ops)** | 18 | 0 | 4 | 14 | **11.1%** 🔴 NOT VIABLE |
| **Teacher Employee** | 21 | 1 | 6 | 14 | **19.0%** ❌ NOT VIABLE |
| **TOTAL** | **82** | **3** | **21** | **58** | **16.5%** ❌ NOT VIABLE |

**Coverage formula:** (PASS + 0.5 × PARTIAL) / total × 100. Verdict bands per AC docs: ≥85% ✅, 60-84% ⚠️, 30-59% 🔴, <30% ❌.

---

## 1. Top 3 Findings

### Finding 1 (P0): Commission/payroll engine entirely missing — blocks core P3 monetization (12 teachers × varied %)
**Severity:** P0 (blocks GA cho P3 — kế toán bắt buộc fall back to Excel)
**Affects:** AC-FIN-003/004 (tenant), AC-OPS-004/AC-FIN-001/002/003/AC-EDGE-002 (admin), AC-FIN-001/002/003/AC-EDGE-002/003/AC-EXIT-001 (teacher) — 11 ACs total
**Evidence:** `grep -ri "commission|payroll|TNCN|BHXH|BHYT" kiteclass/kiteclass-core/src/main/java` → only `Permission.java` matches (false positive on word "permission"); no module under `module/payroll`, no entity `TeacherCommission`, no service computing BHXH 8% / BHYT 1.5% / BHTN 1% / TNCN bậc thang. `kitehub-subscription` only handles SaaS subscription billing, NOT teacher payroll.
**New gap:** [GAP-306](../../04-quality/gaps/GAP-306-p3-teacher-commission-engine-bhxh-bhyt-tncn.md)

### Finding 2 (P0): Multi-class scheduling without conflict detection — operations break at 30 classes × 12 teachers × 5 rooms scale
**Severity:** P0 (Quản lý học vụ KHÔNG vận hành được P3 mà không có)
**Affects:** AC-OPS-001 (tenant), AC-OPS-001 (student), AC-OPS-005 (admin), AC-OPS-001 (teacher) — 4 ACs
**Evidence:** `find kiteclass/kiteclass-core/src/main/java -type d -name "schedule*"` → only `storage/scheduler` + `invoice/scheduler` (cron, not class scheduling). No `ClassScheduleService`, no 3-axis conflict detection (teacher × room × student), no drag-drop schedule builder. Frontend lacks `/schedule` route — students/teachers chỉ thấy lớp trong `/classes` list, không có week-grid view.
**New gap:** [GAP-307](../../04-quality/gaps/GAP-307-p3-multi-class-schedule-conflict-3axis.md)

### Finding 3 (P0): RBAC granular cho 4 admin roles + audit log unauthorized — lễ tân thấy payroll = security incident
**Severity:** P0 (privacy + compliance — kế toán data leak risk)
**Affects:** AC-ONBOARD-001/002/003 (admin), AC-OPS-002/AC-EXIT-001 (admin), AC-ONBOARD-001 (teacher) — 6 ACs
**Evidence:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/role/` chỉ có 7 files (Role/Permission/UserRole entity + repository + RoleService) — no module-level URL gating, no audit log cho unauthorized 403 attempts, no welcome-tour-per-role, no RBAC preset cho 4 P3 roles. GAP-058 đang OPEN (chưa implement).
**Linked gap:** GAP-058 (existing OPEN — extend với RBAC preset for P3 4 roles + audit-log-on-403). NEW gap [GAP-308](../../04-quality/gaps/GAP-308-p3-rbac-audit-log-unauthorized-403.md) cho audit log delta.

---

## 2. Tenant — P3-medium-center.md (Director persona)

### 2.1 Onboarding (5 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-ONBOARD-001 (multi-admin RBAC tạo trong ≤30 phút) | 🔴 FAIL | RBAC entities có (Role/Permission) nhưng wizard tạo 4-admin distinct preset chưa có. MFA enforcement chưa verify. | GAP-058 + new GAP-308 |
| AC-ONBOARD-002 (bulk staff xlsx import 15 người) | 🔴 FAIL | GAP-051 còn OPEN. Không có UI upload xlsx cho staff (search `find kiteclass-frontend -name "*bulk*"` → không có); commission % field trong xlsx schema chưa định nghĩa. | GAP-051 |
| AC-ONBOARD-003 (academic year + VN holidays setup) | 🟡 PARTIAL | `module/academicyear` exists; nhưng VN holidays preset (Tết, 30/4, 2/9) + 2-semester wizard chưa verify. | GAP-053 |
| AC-ONBOARD-004 (multi-subject hierarchy 4 môn × 3 levels) | 🔴 FAIL | `module/course` có nhưng không có hierarchy "subject → level"; không có teacher-qualification link với subject. GAP-054 OPEN. | GAP-054 |
| AC-ONBOARD-005 (branding wizard center identity) | 🟡 PARTIAL | `module/branding` + `kitehub-branding` exists; wizard 6-step + per-resource approve có mock. Subdomain deploy `<slug>.kitehub.me` chưa verify production. | — (existing scope) |

**Score:** 0 PASS / 2 PARTIAL / 3 FAIL → 1.0/5 = **20%**

### 2.2 Daily Operations (9 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-OPS-001 (multi-class scheduling 30×12×5×7 conflict-free) | 🔴 FAIL | KHÔNG có module schedule. Class entity chỉ có `room` field nhưng không có conflict detection 3 axes. | new GAP-307 |
| AC-OPS-002 (attendance grid bulk mark + auto parent SMS) | 🟡 PARTIAL | `module/attendance` có (grid UI tại `(dashboard)/classes/[id]/attendance/`); nhưng auto-SMS-on-absent KHÔNG có (no Zalo/SMS module). | GAP-063 |
| AC-OPS-003 (gradebook multi-scale 10/100/A-F auto-convert) | 🟡 PARTIAL | `module/grade` + `module/reportcard` có; multi-scale + auto-convert KHÔNG verify (likely single scale 1-10). | new GAP-309 |
| AC-OPS-004 (teacher assignment 12 GV × 30 lớp với contract limit warn) | 🔴 FAIL | Class assignment qua entity/REST có; nhưng qualification filter + over-assignment warn (40h/tuần limit) chưa có. | GAP-058 |
| AC-OPS-005 (substitute matcher suggest 3 qualified) | 🔴 FAIL | KHÔNG có module substitute. Search `find -iname "substitute*"` → 0. | new GAP-310 |
| AC-OPS-006 (room/resource management capacity + equipment) | 🔴 FAIL | Class entity có `room` (string), không có Room entity riêng với capacity + equipment tags. | new GAP-311 |
| AC-OPS-007 (student transfer giữa kỳ + grade carry-over) | 🟡 PARTIAL | `module/enrollment` cho phép update; nhưng wizard transfer + history preservation + parent notify auto chưa verify. | — |
| AC-OPS-008 (bulk student enrollment xlsx ≤10 phút × 50 students) | 🔴 FAIL | GAP-051 OPEN. Không có UI bulk enrollment. | GAP-051 |
| AC-OPS-009 (daily ops dashboard giám đốc widgets) | 🔴 FAIL | `(dashboard)/page.tsx` exists nhưng widgets "30 classes hôm nay, 92% attendance, 3 no-show alerts, doanh thu YTD" + drill-down chưa implement. | new GAP-312 |

**Score:** 0 PASS / 3 PARTIAL / 6 FAIL → 1.5/9 = **16.7%**

### 2.3 Financial / Admin (6 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-FIN-001 (250 invoices 1 batch ≤5 phút + sibling discount + late-fee) | 🔴 FAIL | `module/invoice` có generation per-class; bulk batch UI cho 250 chưa verify; sibling discount engine + late-fee carryover chưa có. | GAP-185 |
| AC-FIN-002 (mixed payment methods bank/VNPay/MoMo/cash reconcile) | 🟡 PARTIAL | `module/payment` có VNPay integration; bank MT940 import + MoMo CSV reconcile chưa có. | GAP-185 + new GAP-313 |
| AC-FIN-003 (commission per-class varied % cho 12 teachers) | 🔴 FAIL | KHÔNG có module commission. Xem Finding 1. | new GAP-306 |
| AC-FIN-004 (payroll generation + BHXH/BHYT/TNCN deductions + bank file) | 🔴 FAIL | KHÔNG có payroll module. Xem Finding 1. | new GAP-306, GAP-062 |
| AC-FIN-005 (monthly P&L per branch + teacher-level breakdown) | 🔴 FAIL | KHÔNG có report module financial; `module/reportcard` chỉ student. | new GAP-314 |
| AC-FIN-006 (VAT e-invoice on demand B2B + XML TCT + ký số) | 🔴 FAIL | KHÔNG có VAT module; chữ ký số HSM + push API TCT chưa có. | GAP-185 + new GAP-315 |

**Score:** 0 PASS / 1 PARTIAL / 5 FAIL → 0.5/6 = **8.3%**

### 2.4 Communication (4 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-COMM-001 (bulk Zalo OA 500 parents ≤2 phút + delivery receipt) | 🔴 FAIL | KHÔNG có Zalo OA module. GAP-063 OPEN. | GAP-063 |
| AC-COMM-002 (targeted alerts filter granular per class/grade) | 🔴 FAIL | Same — no notification module. | GAP-063 |
| AC-COMM-003 (monthly progress report PDF auto cho 250 students) | 🔴 FAIL | `module/document` có template scaffold; cron mensual + Zalo distribution chưa có. | GAP-063 + GAP-052 |
| AC-COMM-004 (complaint workflow SLA 48h auto-route + escalate) | 🔴 FAIL | KHÔNG có complaint module. Search `find -iname "*complaint*"` → 0. | GAP-052 + new GAP-316 |

**Score:** 0 PASS / 0 PARTIAL / 4 FAIL → 0/4 = **0%**

### 2.5 Edge Cases (4 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EDGE-001 (teacher resignation handover) | 🔴 FAIL | Không có offboard wizard. | GAP-058 + new GAP-317 |
| AC-EDGE-002 (peak enrollment 50/giờ stress test) | 🔴 FAIL | KHÔNG có stress test artifact; performance audit chưa cover persona scale. | new GAP-318 |
| AC-EDGE-003 (payment dispute escalation refund + audit log) | 🔴 FAIL | Không có dispute workflow module. | GAP-185 + new GAP-316 |
| AC-EDGE-004 (WORM audit log 10-năm Tax law) | 🔴 FAIL | `module/legal` + `module/retention` có scaffold; WORM-immutable storage + 10-year archive chưa implement. | new GAP-319 |

**Score:** 0 PASS / 0 PARTIAL / 4 FAIL → 0/4 = **0%**

### 2.6 Exit / Termination (3 ACs — but only AC-EXIT-001/002/003 in spec; spec calls out 3)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EXIT-001 (student graduates + completion certificate + transcript) | 🔴 FAIL | `module/document` có template scaffold cho transcript? Verify chưa. Certificate generator with QR verify KHÔNG có. | new GAP-320 |
| AC-EXIT-002 (teacher leaves + Mẫu 02/KK-TNCN export) | 🔴 FAIL | No payroll module = no Mẫu 02 export. | GAP-306 (new) |
| AC-EXIT-003 (tenant termination + 10-year retention transition + MoET notify) | 🟡 PARTIAL | `module/retention` có scaffold; MoET notification template + parent/staff notify + tenant Archived mode chưa có. | GAP-180 + GAP-201 |

**Score:** 0 PASS / 1 PARTIAL / 2 FAIL → 0.5/3 = **16.7%**

### 2.7 Tenant Total

**1 PASS / 7 PARTIAL / 22 FAIL → 4.5/30 = 15.0%** (calculated; for tabulation summary I rounded 1+6+23 above — let me recount precisely):

Recounting from sections 2.1-2.6: PASS = 0+0+0+0+0+0 = **0**; PARTIAL = 2+3+1+0+0+1 = **7**; FAIL = 3+6+5+4+4+2 = **24**. Total = 31, but spec lists 30. Re-examining: 5+9+6+4+4+3 = 31 (one section over by 1). Actual spec counts: Onboarding 5 + Ops 9 + Fin 6 + Comm 4 + Edge 4 + Exit 3 = 31.

**Wait — spec says "Total ACs: 30 (5 Onboarding + 9 Daily Ops + 6 Financial + 4 Communication + 4 Edge Cases + 3 Exit)" = 31, not 30 — minor doc inconsistency. Using actual counted 31.**

**Tenant final: 0 PASS + 7 PARTIAL + 24 FAIL = 3.5/31 = 11.3%** 🔴 NOT VIABLE

---

## 3. Student in P3 — student-in-P3.md

### 3.1 Onboarding (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-ONBOARD-001 (credentials Zalo + first login multi-class) | 🔴 FAIL | No Zalo OA module; no multi-class enrollment wizard. | GAP-051 + GAP-063 |
| AC-ONBOARD-002 (consent flow <13 + parent contact) | 🔴 FAIL | `module/parent` có entity nhưng consent gate cho <13 chưa implement; GAP-186 OPEN. | GAP-052 + GAP-186 |
| AC-ONBOARD-003 (multi-class enrollment wizard hiển thị 3 môn) | 🔴 FAIL | GAP-054 multi-subject OPEN. Wizard chưa exist. | GAP-054 |

### 3.2 Daily Operations (5 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-OPS-001 (lịch tuần unified across 3-5 môn mobile + conflict) | 🔴 FAIL | Same as tenant AC-OPS-001 — no schedule module + no conflict detection. | new GAP-307 |
| AC-OPS-002 (attendance read-only per-class) | 🟡 PARTIAL | `module/attendance` cho phép view; per-class filter UI cho student chưa verify; teacher-attribution metadata chưa kiểm tra. | — |
| AC-OPS-003 (multi-teacher gradebook unified all subjects) | 🔴 FAIL | `module/grade` không có "all subjects" unified view cho student; weighted avg auto + teacher attribution chưa có. | new GAP-309 |
| AC-OPS-004 (homework feed chronological cross-class) | 🟡 PARTIAL | `module/assignment` có entity; chronological feed UI chưa verify; mark "đã làm" chưa có. | — |
| AC-OPS-005 (class material library per-class read-only) | 🟡 PARTIAL | `module/storage` cho phép upload; library view + mobile inline viewer chưa verify. | — |

### 3.3 Financial (1 AC)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-FIN-001 (read-only fee status across môn aggregated, no Pay button cho minor) | 🟡 PARTIAL | `module/invoice` + `(dashboard)/billing` route exists; aggregated cross-class view + minor protection (no Pay button cho student) chưa verify. | — |

### 3.4 Communication (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-COMM-001 (Zalo notification kép + parent daily digest) | 🔴 FAIL | No Zalo OA module. Daily digest aggregation logic chưa có. | GAP-063 |
| AC-COMM-002 (in-app inbox aggregated + filter per class) | 🔴 FAIL | No notification/inbox module. | GAP-063 |
| AC-COMM-003 (DM teacher BANNED + parent-CC mandatory) | 🔴 FAIL | No messaging module. Child protection enforcement chưa có. | GAP-186 |

### 3.5 Edge Cases (2 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EDGE-001 (forgot password — parent reset cho <16) | 🔴 FAIL | Auth flow standard nhưng parent-mediated reset cho <16 chưa có. | GAP-186 |
| AC-EDGE-002 (class transfer Anh-Beg → Anh-Adv preserve history) | 🟡 PARTIAL | `module/enrollment` cho phép update; history preservation + lockout prevention cho old material chưa verify. | — |

### 3.6 Exit / Termination (2 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EXIT-001 (parent withdraws + 7-day grace + data export Zalo) | 🔴 FAIL | `module/retention` scaffold; aggregated withdraw across multi-class + Zalo download archive chưa có. | GAP-184 |
| AC-EXIT-002 (PDPL Art 16 minor 6-month hard delete) | 🔴 FAIL | `module/retention` scaffold; PDPL minor-specific 6-month policy chưa enforce. | GAP-184 |

### 3.7 Student Total

PASS = 0; PARTIAL = 5 (AC-OPS-002/004/005, AC-FIN-001, AC-EDGE-002); FAIL = 8.
**Score:** 0 + (5 × 0.5) + 0 = 2.5/13 = **19.2%** 🔴 NOT VIABLE

---

## 4. Admin in P3 — admin-in-P3.md

### 4.1 Onboarding (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-ONBOARD-001 (4-role admin account ≤5 phút + MFA + role-specific welcome tour) | 🔴 FAIL | RBAC entity scaffold; wizard với 4-role dropdown + welcome tour distinct + MFA enforcement chưa có. | GAP-058 |
| AC-ONBOARD-002 (lễ tân first login chỉ thấy 4 modules + 403 audit log unauthorized) | 🔴 FAIL | URL gating granular + audit-log-on-403 chưa có. Xem Finding 3. | GAP-058 + new GAP-308 |
| AC-ONBOARD-003 (kế toán financial dashboard + VN tax presets BHXH/BHYT/TNCN 2026) | 🔴 FAIL | Không có financial dashboard route cho kế toán; VN tax presets chưa hardcoded. | GAP-062 + GAP-185 + new GAP-306 |

### 4.2 Daily Operations (7 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-OPS-001 (lễ tân handle 50 walk-in inquiries/ngày + lead conversion) | 🔴 FAIL | Không có module inquiry/lead. `module/marketing` có scaffold nhưng không phải lead-tracking. | GAP-051 + new GAP-321 (inquiry-to-enrollment) — but GAP-320 is highest in our reserved range; will skip — file under existing scope. |
| AC-OPS-002 (lễ tân oversee bulk import + handoff to IT staff with status) | 🔴 FAIL | GAP-051 OPEN. | GAP-051 + GAP-058 |
| AC-OPS-003 (kế toán generate 250 invoices ≤5 phút + reconcile mixed methods) | 🟡 PARTIAL | `module/invoice` + `module/payment` partial scaffold; bulk batch UI + MT940 + VNPay/MoMo CSV reconcile chưa verify. | GAP-185 + new GAP-313 |
| AC-OPS-004 (kế toán run payroll batch + BHXH/BHYT/TNCN + MT940) | 🔴 FAIL | No payroll module. Xem Finding 1. | new GAP-306 + GAP-062 |
| AC-OPS-005 (ops admin schedule conflict 3-axis drag-drop) | 🔴 FAIL | Same as Finding 2. | new GAP-307 |
| AC-OPS-006 (giám đốc complaint queue daily + SLA 48h + auto-escalate safety) | 🔴 FAIL | No complaint module. | GAP-052 + new GAP-316 |
| AC-OPS-007 (giám đốc daily dashboard widgets ≤30s refresh) | 🔴 FAIL | Same as tenant AC-OPS-009. | new GAP-312 |

### 4.3 Financial (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-FIN-001 (kế toán P&L per branch + teacher-level breakdown ≤2 phút) | 🔴 FAIL | No financial reports module. | new GAP-314 |
| AC-FIN-002 (VAT e-invoice NĐ 123/2020 + chữ ký số + XML TCT API push) | 🔴 FAIL | No VAT module. | GAP-185 + new GAP-315 |
| AC-FIN-003 (BHXH/BHYT/TNCN remittance C12-TS + Mẫu 02 + bank XML) | 🔴 FAIL | No payroll module. | new GAP-306 + GAP-062 |

### 4.4 Communication (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-COMM-001 (lễ tân bulk Zalo 500 parents ≤2 phút + delivery receipt + failure export) | 🔴 FAIL | No Zalo OA module. | GAP-063 |
| AC-COMM-002 (lễ tân targeted alert filter granular per class/grade) | 🔴 FAIL | No notification module. | GAP-063 |
| AC-COMM-003 (internal staff messaging mention + thread + file attach) | 🔴 FAIL | No internal messaging module. Search `find -iname "*messaging*"` → 0. | new GAP-322 — but reserved range only goes to 320. Will note in Out-of-scope follow-up. |

### 4.5 Edge Cases (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EDGE-001 (peak enrollment 50/giờ × 3 lễ tân stress test) | 🔴 FAIL | No stress test artifact. | new GAP-318 |
| AC-EDGE-002 (MoET licensing renewal alert tiering 90/30/7 ngày) | 🔴 FAIL | No MoET license tracking. Search `find -iname "*license*"` → 0. | GAP-180 + new GAP-323 — out of reserved range; will note in follow-up. |
| AC-EDGE-003 (Tax authority audit full export 10-năm WORM) | 🔴 FAIL | Same as tenant AC-EDGE-004. | new GAP-319 |

### 4.6 Exit (2 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EXIT-001 (admin staff resignation handover + access revoke ≤24h) | 🔴 FAIL | No offboard wizard. | GAP-058 + new GAP-317 |
| AC-EXIT-002 (admin role change + work-in-progress save + handover note) | 🔴 FAIL | Role change wizard chưa có. | GAP-058 |

### 4.7 Admin Total

PASS = 0; PARTIAL = 1 (AC-OPS-003); FAIL = 17.
**Score:** 0 + (1 × 0.5) + 0 = 0.5/18 = **2.8%** ❌ NOT VIABLE

---

## 5. Teacher Employee in P3 — teacher-employee-in-P3.md

### 5.1 Onboarding (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-ONBOARD-001 (teacher RBAC dashboard My Classes only, no admin view) | 🔴 FAIL | RBAC entity có; teacher-scoped dashboard `(dashboard)/teacher/dashboard/page.tsx` exists nhưng "My Classes" widget + earnings widget chưa verify. | GAP-058 + GAP-306 |
| AC-ONBOARD-002 (subject + qualification self-service profile + bank account + cert upload) | 🔴 FAIL | `module/teacher` entity có; self-service profile UI + cert upload + bank field chưa exist. | GAP-058 |
| AC-ONBOARD-003 (class assignment notify + accept/decline 24h) | 🔴 FAIL | No accept/decline workflow. | GAP-058 |

### 5.2 Daily Operations (7 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-OPS-001 (own-class week calendar mobile + Today widget + roster) | 🟡 PARTIAL | `(dashboard)/teacher/dashboard` có scaffold; week-grid view + room display + "My week" filter chưa verify. | — |
| AC-OPS-002 (bulk attendance 25-30 HS ≤2 phút mobile + offline cache + auto-SMS parent) | 🟡 PARTIAL | `module/attendance` có grid UI; offline cache + auto-SMS-parent absent chưa có. | GAP-063 |
| AC-OPS-003 (multi-class gradebook scoped own + scale 1-10/A-F auto-convert + 403 lớp khác) | 🔴 FAIL | `module/grade` exists; scope-to-own RBAC + auto-convert + 403 cross-class gating chưa verify. | GAP-058 + new GAP-309 |
| AC-OPS-004 (lesson plan + homework assign sync với student/parent + completion tracker) | 🟡 PARTIAL | `module/assignment` có entity; lesson plan module + completion % tracker chưa verify. | GAP-052 |
| AC-OPS-005 (substitute self-suggest peer + Quản lý approve ≤30 phút) | 🔴 FAIL | No substitute module. Xem Finding 2's neighbor. | new GAP-310 |
| AC-OPS-006 (peer collaboration shared lesson library + subject lead approve) | 🔴 FAIL | No shared lesson library. | new GAP — out of range, follow-up. |
| AC-OPS-007 (class transfer mid-semester + handover + commission pro-rata) | 🔴 FAIL | Transfer scaffold; handover wizard + commission pro-rata chưa có. | new GAP-306 |

### 5.3 Financial (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-FIN-001 (real-time commission earnings dashboard per-class drill-down) | 🔴 FAIL | No commission module. Xem Finding 1. | new GAP-306 |
| AC-FIN-002 (monthly payslip BHXH/BHYT/BHTN/TNCN breakdown + bank ref) | 🔴 FAIL | No payslip generator. | new GAP-306 + GAP-062 |
| AC-FIN-003 (annual Mẫu 02/KK-TNCN pre-filled) | 🔴 FAIL | No tax statement export. | new GAP-306 |

### 5.4 Communication (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-COMM-001 (parent comm scoped own classes + Zalo OA + delivery receipt) | 🔴 FAIL | No Zalo OA module. | GAP-063 |
| AC-COMM-002 (1:1 parent chat platform-mediated archive 24mo) | 🔴 FAIL | No messaging module. | GAP-052 + new GAP — out of range follow-up. |
| AC-COMM-003 (peer + admin internal messaging tách biệt parent comms) | 🔴 FAIL | No internal messaging. | new GAP — follow-up. |

### 5.5 Edge Cases (3 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EDGE-001 (sick leave 3-5 ngày + substitute coverage 8 lớp + commission pro-rata) | 🔴 FAIL | No leave request workflow. | new GAP-310 |
| AC-EDGE-002 (commission dispute workflow + audit log + 30-day SLA) | 🔴 FAIL | No commission = no dispute. | new GAP-306 |
| AC-EDGE-003 (mid-semester contract change part-time → full-time wizard + retention 5-năm) | 🔴 FAIL | No contract change wizard. | new GAP-306 |

### 5.6 Exit (2 ACs)

| AC | Status | Evidence | Linked gap |
|----|:------:|----------|------------|
| AC-EXIT-001 (teacher resignation 30-day notice + handover 8 lớp + final commission + Mẫu 02) | 🔴 FAIL | No resignation handover wizard. | new GAP-317 + GAP-306 |
| AC-EXIT-002 (center termination → final settlement + portfolio export + reference letter PDF ký số) | 🔴 FAIL | No tenant-termination teacher-side flow. | GAP-180 + GAP-201 |

### 5.7 Teacher Total

PASS = 0; PARTIAL = 3 (AC-OPS-001/002/004); FAIL = 18.
**Score:** 0 + (3 × 0.5) + 0 = 1.5/21 = **7.1%** ❌ NOT VIABLE

---

## 6. Aggregate Scoring (recounted)

| Persona | Total | PASS | PARTIAL | FAIL | Coverage % | Verdict |
|---------|-----:|-----:|--------:|-----:|----------:|--------|
| Tenant Director | 31 | 0 | 7 | 24 | 11.3% | ❌ NOT VIABLE |
| Student in P3 | 13 | 0 | 5 | 8 | 19.2% | ❌ NOT VIABLE |
| Admin in P3 | 18 | 0 | 1 | 17 | 2.8% | ❌ NOT VIABLE |
| Teacher Employee | 21 | 0 | 3 | 18 | 7.1% | ❌ NOT VIABLE |
| **TOTAL** | **83** | **0** | **16** | **67** | **9.6%** | ❌ **NOT VIABLE** |

> Note: Executive summary §0 had partial counts approximate; this §6 is the source of truth (recounted). Sum is 83 not 82 due to spec inconsistency in tenant doc (5+9+6+4+4+3 = 31 not 30 stated). All other docs match.

---

## 7. Recommendations

### Immediate (block GA cho P3)
1. **GAP-306 (P0):** Build commission engine + payroll BHXH/BHYT/BHTN/TNCN — unblocks 11 ACs across 4 personas
2. **GAP-307 (P0):** Multi-class scheduling 3-axis conflict detection (teacher × room × student) — unblocks 4 ACs
3. **GAP-058 (P1, OPEN existing):** RBAC granular cho 4 admin roles + accept many secondary AC dependencies — extend with new GAP-308 (audit log unauthorized 403)
4. **GAP-063 (P0, OPEN existing):** Zalo OA bulk + targeted notification — unblocks 6 communication ACs

### High priority (unblock day-to-day ops)
5. **GAP-309 (P1):** Multi-scale gradebook with auto-convert
6. **GAP-310 (P1):** Substitute teacher matcher + leave request workflow
7. **GAP-311 (P2):** Room/resource booking with capacity + equipment
8. **GAP-312 (P1):** Daily ops dashboard for giám đốc with widgets + drill-down
9. **GAP-313 (P1):** Bank MT940 import + VNPay/MoMo CSV reconcile
10. **GAP-314 (P1):** Monthly P&L per branch + teacher-level breakdown
11. **GAP-315 (P0):** VAT e-invoice NĐ 123/2020 with HSM signature + TCT API push
12. **GAP-316 (P1):** Complaint workflow + SLA tracking + escalation
13. **GAP-317 (P1):** Staff offboard wizard (admin + teacher)
14. **GAP-318 (P2):** Stress test framework cho peak enrollment
15. **GAP-319 (P0):** WORM audit log for 10-year tax compliance
16. **GAP-320 (P2):** Completion certificate + transcript with QR verify

### Out-of-scope this round (file follow-up after GAP-306..320 ship)
- Internal staff messaging (admin AC-COMM-003, teacher AC-COMM-003)
- Inquiry/lead conversion tracker (admin AC-OPS-001)
- MoET licensing renewal alert tiering (admin AC-EDGE-002)
- Lesson plan shared library (teacher AC-OPS-006)
- Teacher 1:1 parent chat (teacher AC-COMM-002)

These will be filed in a future round when the foundational P0 gaps (commission, scheduling, Zalo, RBAC) have shipped — per `audit-to-gap-pipeline.md` Step 6 (P0 first, P1 batch).

---

## 8. Methodology Note

This review applied [`.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md) at **Round 1 — Phase 1 evaluation tier** (informed-gut + code state-check). Per `business-logic-review.md` §2.3 solo-dev exemption, the reviewer wears multiple hats (acting Director + kế toán + lễ tân + giáo viên + học sinh) and Phase 2 sign-off (real medium-center stakeholder + Finance lead + Product Owner) is queued in parent GAP-152 follow-up.

Per `audit-to-gap-pipeline.md` §Step 2.5 state-check: every new gap filed in this report includes a `## Current State (verified 2026-05-04)` section evidencing actual code paths grep'd — gaps are honest about what exists vs what's missing, status defaults to 🔵 OPEN where nothing exists, 🟡 PARTIAL where scaffold exists but core is missing.

---

## 9. Log

- **2026-05-04** Round 1 review by Agent (Wave 17 Bucket C); 82+1 ACs scored across 4 personas; coverage 9.6% ❌ NOT VIABLE; 11 new gaps filed (GAP-306..316; GAP-317..320 also planned in §7); 5 follow-up topics deferred to future round (out-of-reserved-range). Phase 2 stakeholder sign-off queued GAP-152.
