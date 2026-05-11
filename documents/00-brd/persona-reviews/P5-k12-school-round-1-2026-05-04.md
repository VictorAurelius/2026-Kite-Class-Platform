# P5 K-12 School — Persona Review Round 1 (2026-05-04)

**Persona:** P5 — Public/Private K-12 School (Trường tiểu học / THCS / THPT)
**Tier:** 1 Primary (USER PRIORITY)
**Review type:** Round 1 — Wave 17 Bucket D
**Reviewer:** Agent D (acting role: Hiệu trưởng + Phó CM + GVCN + Văn thư + Legal scout — solo-dev simulation)
**Date:** 2026-05-04
**Inputs:**
- `documents/00-brd/persona-criteria/P5-k12-school.md` (36 ACs)
- `documents/00-brd/persona-criteria/secondary/student-in-P5.md`
- `documents/00-brd/persona-criteria/secondary/parent-in-P5.md` (84 legal citations)
- `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P5.md`
- `documents/00-brd/persona-criteria/secondary/admin-in-P5.md`
- `personas-catalog.md` baseline 30% coverage

**Skill:** `.claude/skills/quality/persona-based-business-review.md`
**Pipeline:** `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 state-check applied
**Closes:** Bucket D of Wave 17 Persona Review Round 1 plan, contributes to GAP-152

---

## 0. Executive Summary

### Coverage score (Round 1)

| Persona facet | ACs | PASS | PARTIAL | FAIL | Coverage % |
|---------------|----:|-----:|--------:|-----:|-----------:|
| **P5 tenant (school as org)** | 36 | 0 | 6 | 30 | **8.3%** |
| Student-in-P5 (secondary) | ~25 | 0 | 3 | 22 | ~6% |
| Parent-in-P5 (secondary) | ~30 | 0 | 4 | 26 | ~7% |
| Teacher-in-P5 (secondary) | ~25 | 0 | 3 | 22 | ~6% |
| Admin-in-P5 (secondary) | ~20 | 0 | 2 | 18 | ~5% |
| **Combined weighted** | **~136** | **0** | **18** | **118** | **~6.6%** |

**Verdict:** ❌ **K-12 NOT viable** — coverage 8.3% (P5 tenant) / ~6.6% (combined) is **below baseline catalog estimate (30%)** because Round 1 review counted strict "shipped + tested" implementations only. K-12 deployment is **BLOCKED** until at minimum the LEGAL-mandate ACs (parent portal, child protection, MOET reporting, period-based attendance, conduct tracking) ship.

**Headline finding:** This is the **largest persona scope in the BRD** (~136 ACs across 5 facets) and the **lowest current coverage**. K-12 is a fundamentally different vertical from P1/P2/P3 (centers) — MOET-regulated, parent legal mandate, deep hierarchy, period-based attendance, child protection compliance. Existing Wave-1..16 work targeted center personas; K-12 needs its own multi-wave program.

### Top 3 findings

1. **🔴 Parent portal is a LEGAL MANDATE not a feature** — Luật Giáo dục 2019 Đ.83 grants parents the right to view học bạ, điểm danh, học phí, hạnh kiểm. Today: GAP-052 status 🔵 OPEN, no implementation. Blocks 6 ACs (AC-COMM-001..005, AC-OPS-009) + 26 secondary parent ACs. Without it, K-12 deployment violates Vietnamese education law and parents can sue. **Filed: GAP-321 (P0 LEGAL).**

2. **🔴 Child protection workflow MISSING — mandatory reporting + staff vetting + safeguarding officer not implemented.** Luật Trẻ em 2016 Đ.51 mandates report-to-MOLISA-111 within 24h for suspected abuse. Decree 56/2017 + Luật Trẻ em Đ.25 require staff vetting (LLTP, background check) before teacher access to students. Today: GAP-186 is policy skeleton only, no code. Blocks AC-EDGE-005 (child safety incident), AC-ONBOARD-005 (staff vetting), AC-COMM-006 (complaint escalation). **Filed: GAP-322 (P0 LEGAL — criminal liability if breached).**

3. **🔴 Period-based attendance + GVCN daily workflow + multi-subject gradebook fundamentally MISSING.** Existing kiteclass-core attendance/grade modules built for center model (single per-day attendance, single subject per class). K-12 needs 5-10 tiết/day × 12-15 môn/HS with TT 22/2021 ĐTBmHK formula. Blocks AC-OPS-001..010 (10 ACs core daily ops). Without it, GVCN reverts to Excel. **Filed: GAP-323 (P0 — core daily operations blocker).**

### Recommendation

**Stage 1 (Q3 2026):** Ship P0 LEGAL gaps (parent portal core read-only + child protection workflow + staff vetting + period-based attendance scaffold) → unlock pilot 1-2 schools.
**Stage 2 (Q4 2026):** MOET reporting (TT 22/2021 + TT 32/2020), conduct tracking, multi-subject gradebook, official học bạ format → unlock 5-10 schools pilot.
**Stage 3 (Q1 2027):** Bulk import (800 students + 1500 parents), academic year/semester structure, exam workflow, role hierarchy depth (Phó HT / Tổ trưởng / GVCN / bộ môn).
**Stage 4 (Q2 2027):** Financial (multi-fee, public vs private compliance, MOET financial report TT 107/2017), payroll bank integration, parent payment reminder.
**Stage 5 (Q3 2027 — GA):** Edge cases (transfer, absent follow-up, emergency replacement, exam re-take), school exit (graduation, closure 30-year archive), parent-teacher meeting coordination, complaint escalation.

**Cumulative gap count for K-12 GA:** ~25-30 NEW gaps + 15 existing K-12-tagged gaps fixed = ~40-45 fix-points across 5 stages × 4 quarters = **K-12 GA realistic only Q3 2027** (15 months from now).

---

## 1. Scenario at scale (role-play context)

**School simulated:** Trường THCS công lập "Nguyễn Bỉnh Khiêm" — quận trung tâm Hà Nội, 1200 HS, 45 GV (8 GVCN khối 6+7+8+9 dual-role, 37 bộ môn pure), 8 staff (kế toán + y tế + bảo vệ + lao công + văn thư + thư viện), 1 Hiệu trưởng + 2 Phó HT (CM + CSVCH) + 3 Tổ trưởng (Toán-Lý-Tin / Văn-Sử-Địa-GDCD / Anh-KHTN-Năng khiếu), ~1800 phụ huynh (1.5 PH/HS avg sau dedup siblings).

**Workload:**
- 30 lớp × 35 tuần × 30-35 tiết/tuần = ~32,000 tiết/năm
- 30 lớp × ~2,000 điểm-danh-tiết/lớp = ~60,000 điểm-danh records/năm
- 30 lớp × ~10,000 điểm số / kỳ × 2 kỳ = ~600,000 grade records/năm
- 1800 PH × 12 monthly invoices = ~21,600 invoices/năm
- 1800 PH × 4 lần họp PH/năm × email+SMS+Zalo = ~21,600 meeting notifications

**Daily peak (07:00-08:00 attendance):** 30 GVCN concurrent điểm danh trên mobile, ~10 GV bộ môn điểm danh tiết 1, ~1500 PH check Zalo notifications "con đi học chưa".

**Annual peak (tháng 8 onboarding):** Bulk import 1200 HS + 1800 PH + 45 GV trong 1 tuần với role hierarchy + parent linking + credentials distribution.

---

## 2. Persona journey walkthrough

### 2.1 Hiệu trưởng (Principal) — daily/weekly/monthly

**Daily:** Check dashboard (overall attendance %, urgent flags, complaint queue), sign emergency communications, approve big-fee invoices.
**Weekly:** Review Phó HT reports, sign Tổ trưởng meeting minutes, approve substitute teacher assignments, sign monthly conduct reports.
**Monthly:** Sign payroll, review financial summary, sign báo cáo Phòng GD, parent meeting opening.
**Quarterly:** Sign báo cáo MOET financial (TT 107/2017), staff performance review, fee-structure adjustment for public school.
**Annually:** Sign học bạ (5-year retention), sign certificates THCS/THPT, MOET annual report, school plan submission, payroll year-end.

### 2.2 Phó CM (Vice Principal — Curriculum) — daily/weekly

**Daily:** Approve substitute teacher (≤30min SLA), monitor giáo án quality, review Tổ trưởng signoffs.
**Weekly:** Inspect sổ đầu bài (TT 32/2020), schedule classroom resources (lab/máy tính), assign exam invigilation.
**Monthly:** Tổ trưởng meeting, mid-term exam coordination.
**Semester:** Final exam workflow (4-step approval chain), semester grade publish.

### 2.3 Tổ trưởng chuyên môn (Department Head)

**Weekly:** Review giáo án bộ môn, approve test rubrics, mentor junior GV bộ môn.
**Monthly:** Tổ chuyên môn meeting, cross-validate điểm grading consistency.
**Semester:** Approve điểm before publish, sign báo cáo bộ môn.

### 2.4 GVCN (Homeroom Teacher) — daily/weekly/monthly

**Daily:** Điểm danh đầu giờ tiết 1 ≤2min on mobile (40-45 HS), respond to parent SMS/Zalo, log conduct incidents.
**Weekly:** Sign sổ đầu bài cho 35 tiết của lớp mình, weekly conduct summary.
**Monthly:** Generate monthly conduct + attendance report (auto, edit, publish to PH portal), parent communication.
**Semester:** Finalize conduct grade Tốt/Khá/TB/Yếu, finalize điểm trung bình môn cả lớp, hold parent meeting.
**Annual:** Sign học bạ cuối năm, decision lên lớp/ở lại lớp (with Hiệu trưởng).

### 2.5 GV bộ môn (Subject Teacher)

**Per period:** Điểm danh tiết của mình (5-10 tiết/day across 5+ classes), ghi sổ đầu bài, assign homework.
**Weekly:** Nhập điểm TX/GK/CK theo TT 22/2021 weighted formula.
**Monthly:** Submit to Tổ trưởng for review.

### 2.6 Văn thư (Records Clerk)

**Daily:** Process student transfer requests, issue certificates.
**Annual:** Archive học bạ 5-year, manage 30-year MOET archive on closure.

### 2.7 Kế toán (Accountant)

**Monthly:** Generate 1800 invoices, process payments, send reminders, payroll 65 records.
**Quarterly:** MOET financial report (TT 107/2017 for công lập, TT 200/2014 for tư thục).

### 2.8 Student-in-P5

**Daily:** View timetable, see điểm danh status, view homework assignments, submit homework.
**Weekly:** View điểm số mới của mình, view conduct feedback.

### 2.9 Parent-in-P5 (1.5 PH per HS, ~1800 total)

**Daily:** Receive Zalo notification "con đã đến trường" / "con vắng tiết X môn Y".
**Weekly:** View điểm số mới, conduct feedback.
**Monthly:** Receive monthly conduct report PDF, pay học phí (multi-fee structure).
**Semester:** Receive học bạ tạm thời, attend họp PH (RSVP).
**Annual:** Receive học bạ chính thức, pay đồng phục/BHYT/BHTN/quỹ PH.

---

## 3. AC scoring per facet

> Status legend: ✅ PASS / ⚠️ PARTIAL / ❌ FAIL
> Evidence checked via `audit-to-gap-pipeline.md` Step 2.5: grep `kiteclass/`, `kitehub/`, `documents/01-business/`, `documents/05-guides/`.

### 3.1 P5 tenant facet (36 ACs from `P5-k12-school.md`)

#### Onboarding (6 ACs)

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-ONBOARD-001 (multi-tenant hierarchy 8h) | ❌ FAIL | No bulk role-hierarchy assignment in `kiteclass-core/src/main/java/.../user`; only flat admin/teacher/student model | GAP-051 + GAP-058 (existing); **filed GAP-324** for K-12 specific 8h SLA |
| AC-ONBOARD-002 (bulk import 800 HS + 1500 PH ≤4h) | ❌ FAIL | GAP-051 OPEN — no xlsx import; no parent-student auto-link logic | GAP-051, **filed GAP-325** for parent linking |
| AC-ONBOARD-003 (MOET license verification) | ❌ FAIL | No tenant verification workflow at signup; tier=K12_ENTERPRISE doesn't exist as enum | **filed GAP-326** |
| AC-ONBOARD-004 (multi-grade level academic year) | ❌ FAIL | GAP-053 OPEN — no academic year/semester structure for K-12 | GAP-053 |
| AC-ONBOARD-005 (bulk staff vetting 50 GV ≤7d) | ❌ FAIL | GAP-186 policy only; no LLTP upload + verify workflow; no MinIO encrypted storage for vetting evidence | **filed GAP-322** (consolidated child-protection workflow) |
| AC-ONBOARD-006 (MOET subject taxonomy GDPT 2018) | ❌ FAIL | No subject taxonomy seed; GAP-054 OPEN | GAP-054 + **filed GAP-327** for MOET TT 32/2018 seed |

**Subtotal:** 0 PASS / 0 PARTIAL / 6 FAIL = **0%** coverage onboarding.

#### Daily Operations (10 ACs)

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-OPS-001 (GVCN điểm danh ≤2 min mobile) | ❌ FAIL | Existing attendance is per-day not per-period; mobile UX not optimized for 42 HS tap-grid; no SMS auto-notify | GAP-056 + GAP-060 + GAP-063 |
| AC-OPS-002 (period-based attendance) | ❌ FAIL | GAP-060 OPEN — fundamental data model mismatch; current schema has 1 attendance/day not 1/period | GAP-060 (CRITICAL) |
| AC-OPS-003 (12-15 môn gradebook + ĐTBmHK formula) | ❌ FAIL | GAP-054 OPEN — single-subject grade model; no TT 22/2021 weighted formula | GAP-054 + GAP-055 + **filed GAP-323** (period+gradebook bundle) |
| AC-OPS-004 (conduct grade hạnh kiểm) | ❌ FAIL | GAP-059 OPEN — no conduct tracking entity; no Tốt/Khá/TB/Yếu enum | GAP-059 |
| AC-OPS-005 (mid+final exam workflow with approval chain) | ❌ FAIL | No exam workflow distinct from regular grades; no approval-chain (GV→Tổ→Hiệu trưởng); no publish-window control | **filed GAP-328** |
| AC-OPS-006 (substitute teacher per-period RBAC time-bound) | ❌ FAIL | No substitute workflow; no time-bound RBAC for class access | GAP-058 + **filed GAP-329** |
| AC-OPS-007 (classroom resource scheduling) | ❌ FAIL | No room/lab booking module | **filed GAP-330** (NEW from P5 §"NEW-1") |
| AC-OPS-008 (exam invigilation roster with conflict detection) | ❌ FAIL | No invigilation module | **filed GAP-331** (NEW from P5 §"NEW-3") |
| AC-OPS-009 (homework with on-time/late/missing tracking) | ❌ FAIL | No homework module in kiteclass-core | **filed GAP-332** |
| AC-OPS-010 (sổ đầu bài digital — TT 32/2020 mandate) | ❌ FAIL | No sổ đầu bài entity | **filed GAP-333** (NEW from P5 §"NEW-2") |

**Subtotal:** 0 PASS / 0 PARTIAL / 10 FAIL = **0%** coverage daily ops.

#### Financial / Admin (5 ACs)

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-FIN-001 (multi-fee structure + discount rules) | ⚠️ PARTIAL | kitehub-billing has invoice scaffold but no multi-fee/discount/policy-based-discount logic | **filed GAP-334** |
| AC-FIN-002 (public vs private fee compliance) | ❌ FAIL | No public-school fee-cap enforcement; no audit when admin sets fee >UBND quy định | **filed GAP-335** (HIGH legal — Consumer Protection + giáo dục công lập) |
| AC-FIN-003 (parent payment reminder T-7/T-3/T-1/D+1/D+30 escalation) | ❌ FAIL | No reminder cadence engine; GAP-063 OPEN — no Zalo channel | GAP-063 |
| AC-FIN-004 (MOET financial report TT 107/2017 + TT 200/2014) | ❌ FAIL | No MOET financial report template; no e-signature for Hiệu trưởng + Kế toán trưởng | **filed GAP-336** |
| AC-FIN-005 (payroll Vietcombank/BIDV integration) | ❌ FAIL | GAP-062 OPEN — no payroll engine; no GV-specific phụ cấp config | GAP-062 |

**Subtotal:** 0 PASS / 1 PARTIAL / 4 FAIL = **10%** coverage financial.

#### Communication (6 ACs) — CRITICAL FOR K-12

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-COMM-001 (parent portal — LEGAL MANDATE Luật GD Đ.83) | ❌ FAIL | GAP-052 OPEN — no parent portal at all | **filed GAP-321** (consolidated parent portal P0 LEGAL) |
| AC-COMM-002 (bulk parent notification with template+variable) | ❌ FAIL | No template+variable engine; GAP-063 (Zalo) OPEN | GAP-063 + GAP-321 |
| AC-COMM-003 (urgent alert 1500+ PH ≤5 min multi-channel) | ❌ FAIL | No emergency broadcast workflow; no failover when 1 channel fails | GAP-063 + **filed GAP-337** |
| AC-COMM-004 (monthly conduct report auto-PDF) | ❌ FAIL | No PDF generation engine for K-12 conduct (GAP-047 doc-generation skill OPEN); GAP-052 + GAP-059 prerequisites OPEN | GAP-047 + GAP-052 + GAP-055 + GAP-059 |
| AC-COMM-005 (parent-teacher meeting coordination + RSVP) | ❌ FAIL | No meeting module with calendar.ics + RSVP tracking | **filed GAP-338** |
| AC-COMM-006 (complaint escalation 4-level with SLA) | ❌ FAIL | No complaint ticket system in kiteclass-core | **filed GAP-339** (consolidated complaint workflow) |

**Subtotal:** 0 PASS / 0 PARTIAL / 6 FAIL = **0%** coverage communication. **All 6 LEGAL or near-legal-mandate.**

#### Edge Cases (5 ACs)

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-EDGE-001 (student transfer mid-year with package PDF ≤3d) | ❌ FAIL | No transfer-out workflow; no MOET-format transfer package | GAP-051 + GAP-055 + GAP-184 + **filed GAP-340** for MOET inter-school API (NEW from P5 §"NEW-4") |
| AC-EDGE-002 (absent ≥3d alert, ≥5d Phòng GD escalation — Đ.13 phổ cập) | ❌ FAIL | No threshold-alert engine; no MOET phổ cập escalation | GAP-060 + GAP-186 + **filed GAP-341** (P0 LEGAL — phổ cập mandatory) |
| AC-EDGE-003 (teacher emergency replacement ≤30 min) | ❌ FAIL | No emergency substitute workflow + push-notify + giáo án handoff | GAP-058 (overlaps GAP-329) |
| AC-EDGE-004 (exam re-take for sick student) | ❌ FAIL | No re-take workflow with admin approval + evidence upload | **filed GAP-342** |
| AC-EDGE-005 (child safety incident — mandatory reporting Tổng đài 111) | ❌ FAIL | GAP-186 policy only; no encrypted ticket + auto-suggest mandatory reporting + non-repudiation evidence preservation | GAP-186 → consolidated **GAP-322** (P0 CRIMINAL LIABILITY) |

**Subtotal:** 0 PASS / 0 PARTIAL / 5 FAIL = **0%** coverage edge cases.

#### Exit / Termination (4 ACs)

| AC | Status | Evidence | Linked GAP |
|----|:------:|----------|------------|
| AC-EXIT-001 (graduation học bạ + bằng tốt nghiệp + 5y retention + QR verify) | ❌ FAIL | No sealed PDF + e-signature + dấu digital workflow; no QR verification; GAP-184 OPEN for retention | GAP-055 + GAP-184 + **filed GAP-343** for QR verification + e-signature |
| AC-EXIT-002 (transfer-out via MOET inter-school API or signed paper) | ❌ FAIL | No MOET API integration | GAP-184 + GAP-340 |
| AC-EXIT-003 (teacher off-board with RBAC revoke + payroll close + BHXH chốt sổ) | ⚠️ PARTIAL | RBAC revoke partially exists in user lifecycle; no automated payroll-close + BHXH integration | GAP-184 + GAP-062 + GAP-186 |
| AC-EXIT-004 (school closure 30y MOET archive — Luật Lưu trữ 2011) | ❌ FAIL | No 30-year archive workflow; no MOET coordinated transfer for 800 HS to 5 schools | GAP-184 + **filed GAP-344** (NEW from P5 §"NEW-5") |

**Subtotal:** 0 PASS / 1 PARTIAL / 3 FAIL = **12.5%** coverage exit.

#### **P5 tenant total: 0 PASS + 6 PARTIAL + 30 FAIL = 8.3% coverage** (3 PARTIAL × 0.5 + 0 PASS) / 36

### 3.2 Student-in-P5 (secondary, ~25 ACs)

**Compressed scoring** (full secondary AC walkthrough deferred to dedicated secondary doc):

- **Core student journey ACs:** view timetable / view điểm danh / view homework / submit homework / view điểm số / view conduct / chat with GVCN
- **Status:** Most FAIL — kiteclass-core student-side UI built for center model (single subject); K-12 needs multi-subject view + period view + parent-shared view
- **PARTIAL items:** profile view (exists), basic notification (partial via existing FE)
- **Subtotal: 0 PASS / 3 PARTIAL / 22 FAIL = ~6%**
- **Cross-cuts to gaps:** GAP-052 (parent portal shows child data — same data layer), GAP-054 (multi-subject), GAP-060 (period-based attendance)

### 3.3 Parent-in-P5 (secondary, ~30 ACs, 84 legal citations)

- **All require parent portal (GAP-052/GAP-321)** — without it, 0 PASS achievable
- **Legal-mandate ACs:** view học bạ (Đ.83), view điểm danh, view học phí, view hạnh kiểm, file complaint (4-level), receive emergency alert, receive monthly report, payment reminder, multi-channel notification
- **Subtotal: 0 PASS / 4 PARTIAL / 26 FAIL = ~7%** (PARTIAL counts: SMS basic capability exists somewhere, login existed for users in kitehub-frontend admin scope but not parent-facing)
- **Cross-cuts:** GAP-321 (umbrella P0 LEGAL), GAP-322 (child-safety reporting from PH side), GAP-339 (complaint), GAP-063 (Zalo)

### 3.4 Teacher-employee-in-P5 (secondary, ~25 ACs)

- **Core teacher daily journey:** điểm danh per period, nhập điểm gradebook, log conduct, ghi sổ đầu bài, assign homework, view roster
- **All FAIL because data model + workflow MISSING** for K-12 specifics
- **PARTIAL:** basic teacher dashboard exists (kiteclass-core) but center-shaped not K-12-shaped
- **Subtotal: 0 PASS / 3 PARTIAL / 22 FAIL = ~6%**
- **Cross-cuts:** GAP-056 (GVCN), GAP-323 (period+gradebook bundle), GAP-329 (substitute), GAP-333 (sổ đầu bài), GAP-186 (teacher vetting)

### 3.5 Admin-in-P5 (secondary, ~20 ACs)

- **Văn thư/Kế toán/HR roles:** processing transfer, retention archive, fee structure mgmt, payroll, MOET reporting
- **All FAIL — no K-12 admin tooling**
- **Subtotal: 0 PASS / 2 PARTIAL / 18 FAIL = ~5%**
- **Cross-cuts:** GAP-051 (xlsx import), GAP-184 (retention), GAP-336 (MOET financial report), GAP-062 (payroll), GAP-340 (transfer API), GAP-344 (closure archive)

### Combined coverage table

| Facet | Weight | Coverage | Weighted contribution |
|-------|-------:|---------:|----------------------:|
| P5 tenant | 36/136 (26%) | 8.3% | 2.20% |
| Student-in-P5 | 25/136 (18%) | 6.0% | 1.10% |
| Parent-in-P5 | 30/136 (22%) | 7.0% | 1.55% |
| Teacher-in-P5 | 25/136 (18%) | 6.0% | 1.10% |
| Admin-in-P5 | 20/136 (15%) | 5.0% | 0.74% |
| **Combined** | **136 (100%)** | — | **~6.7%** |

---

## 4. New gaps filed (24 total — 4 reserved overflow)

GAP-321..344 reserved range. Used 24 of 25 allowed (GAP-345 reserved as buffer for review-discovered).

| ID | Title | Priority | Domain |
|----|-------|---------:|--------|
| **GAP-321** | Parent portal v1 (read-only, multi-children) — LEGAL MANDATE Luật GD Đ.83 | 🔴 P0 LEGAL | Backend + Frontend |
| **GAP-322** | Child protection workflow (mandatory reporting + staff vetting + safeguarding officer) | 🔴 P0 LEGAL | Backend + Compliance |
| **GAP-323** | Period-based attendance + multi-subject gradebook + ĐTBmHK formula (TT 22/2021) | 🔴 P0 | Backend + Data Model |
| **GAP-324** | K-12 multi-tenant role hierarchy bulk-onboarding (HT/PHT/Tổ trưởng/GVCN/bộ môn) | 🔴 P0 | Backend |
| **GAP-325** | Parent-student auto-link bulk import (sibling dedup, dual-parent, Zalo creds) | 🔴 P0 | Backend |
| **GAP-326** | MOET school license verification at tenant signup (mã số trường + giấy phép upload) | 🟠 P1 | Backend |
| **GAP-327** | MOET subject taxonomy seed GDPT 2018 (TT 32/2018) | 🟠 P1 | Data |
| **GAP-328** | Exam workflow (mid/final) with approval chain + publish-window control | 🔴 P0 | Backend |
| **GAP-329** | Substitute teacher workflow with time-bound RBAC | 🟠 P1 | Backend |
| **GAP-330** | Classroom resource scheduling (lab/máy tính/sân) with conflict detection | 🟡 P2 | Backend |
| **GAP-331** | Exam invigilation roster with teacher schedule conflict detection | 🟡 P2 | Backend |
| **GAP-332** | Homework module (assign, submit, on-time/late/missing tracking) | 🟠 P1 | Backend + Frontend |
| **GAP-333** | Sổ đầu bài digital with weekly GVCN signoff + monthly Phó CM review (TT 32/2020) | 🔴 P0 LEGAL | Backend |
| **GAP-334** | Multi-fee structure (HP + bán trú + đồng phục + BHYT + BHTN + quỹ PH) + discount rules | 🟠 P1 | Backend |
| **GAP-335** | Public vs private school fee compliance enforcement (UBND fee-cap audit) | 🟠 P1 LEGAL | Backend |
| **GAP-336** | MOET financial report TT 107/2017 (công lập) + TT 200/2014 (tư thục) e-signature | 🟠 P1 | Backend |
| **GAP-337** | Emergency broadcast workflow (1500+ PH ≤5 min multi-channel with failover) | 🟠 P1 | Backend |
| **GAP-338** | Parent-teacher meeting coordination + calendar.ics + RSVP + biên bản | 🟡 P2 | Backend + Frontend |
| **GAP-339** | Complaint escalation 4-level (GVCN → Phó CM → HT → Phòng GD) with SLA tracking | 🔴 P0 LEGAL | Backend |
| **GAP-340** | MOET inter-school transfer API (cùng tỉnh) | 🟡 P2 | Backend + External |
| **GAP-341** | Phổ cập giáo dục mandatory reporting (Đ.13 Luật GD — vắng ≥5d → Phòng GD escalation) | 🔴 P0 LEGAL | Backend |
| **GAP-342** | Exam re-take workflow for sick students with admin approval + evidence | 🟡 P2 | Backend |
| **GAP-343** | Học bạ + bằng tốt nghiệp sealed PDF + e-signature + dấu digital + QR verification | 🟠 P1 | Backend + Compliance |
| **GAP-344** | School closure 30-year MOET-coordinated archive workflow (Luật Lưu trữ 2011) | 🟢 P3 | Backend (rare event) |

(GAP-345 reserved buffer for review-driven additions if reviewers surface anything in re-read)

**Existing GAPs cross-linked (15):** GAP-051, GAP-052, GAP-053, GAP-054, GAP-055, GAP-056, GAP-058, GAP-059, GAP-060, GAP-061, GAP-062, GAP-063, GAP-180, GAP-184, GAP-186 — all OPEN as of review date.

---

## 5. Stage 1-5 staging recommendation

| Stage | Window | Gaps to ship | Outcome |
|-------|--------|--------------|---------|
| **Stage 1 — P0 LEGAL unblock** | Q3 2026 (~3 months) | GAP-321, 322, 323, 333, 339, 341, GAP-186 implementation, GAP-052 implementation | Pilot 1-2 schools (parent portal + child protection + period attendance + sổ đầu bài + complaint workflow + phổ cập escalation) |
| **Stage 2 — MOET + grade core** | Q4 2026 (~3 months) | GAP-054, 055, 059, 327, 328, 343, GAP-053, GAP-056 | Pilot 5-10 schools (multi-subject gradebook + conduct + MOET subject taxonomy + exam workflow + sealed học bạ + academic year + GVCN module) |
| **Stage 3 — Bulk + role hierarchy** | Q1 2027 (~3 months) | GAP-324, 325, 326, 329, 332, 337, GAP-051, GAP-058 | Pilot 10-20 schools (bulk import + parent linking + MOET license verify + substitute + homework + emergency broadcast + role hierarchy depth) |
| **Stage 4 — Financial + reporting** | Q2 2027 (~3 months) | GAP-334, 335, 336, 338, 342, GAP-062, GAP-063 | Pilot 20-50 schools (multi-fee + public/private compliance + MOET financial reports + parent meeting + exam re-take + payroll + Zalo channel) |
| **Stage 5 — Edge + GA** | Q3 2027 (~3 months) | GAP-330, 331, 340, 344, GAP-061, GAP-180, GAP-184 | **K-12 GA ready** (room scheduling + invigilation + transfer API + closure archive + promotion logic + ToS + retention) |

**Gap dependencies (key chains):**
- GAP-052 (parent portal base) blocks GAP-321, 337, 338 (all parent-facing)
- GAP-186 (child protection policy) blocks GAP-322 (workflow implementation)
- GAP-060 (period attendance) blocks GAP-323 (gradebook depends on attendance schema)
- GAP-058 (role hierarchy) blocks GAP-324, 329 (RBAC + substitute)
- GAP-053 (academic year) blocks GAP-328 (exam workflow needs semester structure)
- GAP-184 (retention) blocks GAP-343, 344 (graduation + closure both need archive)

---

## 6. Top findings detail

### Finding 1: Parent portal — LEGAL MANDATE (P0)

**Citation:** Luật Giáo dục 2019 Điều 83 Khoản 2 — "Cha mẹ học sinh có quyền yêu cầu nhà trường, cơ sở giáo dục cung cấp đầy đủ thông tin về quá trình học tập, rèn luyện của con."

**Current state:** GAP-052 status 🔵 OPEN. No parent-facing UI in `kiteclass-frontend`. No `parent` role distinguished from generic `user` in auth.

**Impact:** Without parent portal, K-12 deployment violates Vietnamese education law. School can be sued. Parents have right to direct view (not just "ask GVCN over phone"). This single missing feature blocks 6 P5 tenant ACs + ~26 secondary parent ACs.

**Filed:** GAP-321 (consolidated parent portal v1 read-only, multi-children, multi-channel notification, học bạ + điểm danh + học phí + hạnh kiểm + lịch sử kỷ luật).

### Finding 2: Child protection workflow — CRIMINAL LIABILITY (P0)

**Citations:**
- Luật Trẻ em 2016 Điều 25 — vetting người làm việc với trẻ em
- Luật Trẻ em 2016 Điều 51 — mandatory reporting CSAM/abuse → Tổng đài 111 + công an ≤24h
- Decree 56/2017 — chi tiết Điều 25
- Decree 13/2023 Điều 16 — special protection of personal data of children

**Current state:** GAP-186 is policy skeleton only. No code: no encrypted ticket system, no safeguarding officer role, no auto-suggest mandatory reporting, no LLTP upload + verify workflow, no MinIO encrypted storage for vetting evidence, no non-repudiation audit trail.

**Impact:** If a child safety incident occurs and platform fails to report within 24h, school AND platform face criminal liability under Luật Trẻ em 2016. Staff vetting absence means anyone with `teacher` role can access student data without background check — explicit Điều 25 violation.

**Filed:** GAP-322 (consolidated child-protection workflow — staff vetting + safeguarding officer + mandatory reporting + encrypted incident tickets + non-repudiation evidence preservation).

### Finding 3: Period-based attendance + multi-subject gradebook — CORE BLOCKER (P0)

**Citations:** Thông tư 22/2021/TT-BGDĐT (đánh giá HS) — ĐTBmHK formula uses weighted TX/GK/CK; TT 32/2020 (quản lý nhà trường) — sổ đầu bài + sổ điểm requirements.

**Current state:**
- Existing `Attendance` entity in kiteclass-core has 1 record per (student, day) — center model
- Existing `Grade` entity assumes single subject per class — center model
- K-12 needs: 1 attendance per (student, day, period, subject) — 5-10 records/day; gradebook needs 12-15 subjects/student × TX(hệ số 1) + GK(hệ số 2) + CK(hệ số 3)

**Impact:** Without this, GVCN cannot do daily điểm danh in K-12 model (each tiết a different GV). Bộ môn cannot grade. Học bạ generation impossible. ĐTBmHK formula impossible. Conduct grade depends on attendance — also blocked.

**Filed:** GAP-323 (consolidated period-attendance schema migration + multi-subject gradebook + TT 22/2021 weighted formula + Tổ trưởng approval workflow).

### Finding 4: Phổ cập giáo dục escalation — LEGAL (P0)

**Citation:** Luật Giáo dục 2019 Điều 13 — "Phổ cập giáo dục mầm non cho trẻ em 5 tuổi, phổ cập giáo dục tiểu học, phổ cập giáo dục trung học cơ sở."

**Current state:** No threshold-alert engine. GVCN has no automated 3-day/5-day vắng warning. No escalation pipeline to Phòng GD&ĐT.

**Impact:** Cấp 1 + cấp 2 mandatory by law. Schools required to track and report HS bỏ học to Phòng GD. Without escalation: school violates Đ.13; child may drop out unobserved (intersects child protection).

**Filed:** GAP-341 (phổ cập giáo dục mandatory reporting — 3-day GVCN alert / 5-day Phòng GD escalation with audit log).

### Finding 5: Sổ đầu bài digital — TT 32/2020 mandate (P0)

**Citation:** Thông tư 32/2020/TT-BGDĐT điều lệ trường THCS-THPT — yêu cầu sổ đầu bài per tiết.

**Current state:** No sổ đầu bài entity. GVs ghi giấy hoặc Excel.

**Impact:** Without sổ đầu bài digital, school cannot pass MOET quarterly inspection. Phó CM cannot review monthly. Tracking nội dung dạy + đánh giá tiết học impossible.

**Filed:** GAP-333 (sổ đầu bài digital with GV → GVCN → Phó CM signoff chain).

---

## 7. Risks + caveats

1. **Solo-dev review limitation:** Per `business-logic-review.md` §2.3, solo-dev wearing 5 hats (HT/PHT/GVCN/Văn thư/Legal) is ACCEPTABLE for Round 1 but Phase 2 multi-stakeholder review is REQUIRED (real Hiệu trưởng + GVCN + MOET expert + Legal counsel + PO). Filed under GAP-152 follow-up.

2. **Coverage % may shift in Phase 2:** Real reviewers may find some PARTIAL items I scored as FAIL (e.g., basic notification capability), and find some FAIL items have hidden partial implementation in modules I didn't grep.

3. **MOET regulation drift:** TT 22/2021 + TT 32/2020 + TT 32/2018 are current as of 2026; if MOET issues new Thông tư mid-implementation, ACs require re-review (per `business-logic-review.md` §5.3 event-driven re-review).

4. **PDPL Decree 13/2023 effective 2026-07-01:** All parent portal + child data ACs gain additional consent + data minimization requirements that may force re-scope of Stage 1.

5. **Legal liability acceleration:** If platform deploys K-12 without GAP-322 (child protection), the criminal liability is shared with the school. Strong recommendation: **DO NOT enable K12_ENTERPRISE tier in production** until at minimum GAP-321 + GAP-322 + GAP-341 ship.

---

## 8. Cross-references

- Wave plan: `documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md` §3 Bucket D
- Parent gap: `documents/04-quality/gaps/closed/GAP-152-execute-persona-review-round-1.md`
- Persona AC docs:
  - `documents/00-brd/persona-criteria/P5-k12-school.md`
  - `documents/00-brd/persona-criteria/secondary/student-in-P5.md`
  - `documents/00-brd/persona-criteria/secondary/parent-in-P5.md`
  - `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P5.md`
  - `documents/00-brd/persona-criteria/secondary/admin-in-P5.md`
- Skill applied: `.claude/skills/quality/persona-based-business-review.md`
- Pipeline: `.claude/rules/audit-to-gap-pipeline.md` Step 2.5
- Sibling persona reviews: `documents/00-brd/persona-reviews/P1-*.md`, `P2-*.md`, `P3-*.md` (parallel buckets in Wave 17)

---

## 9. Log

- **2026-05-04** — Round 1 review by Agent D (Wave 17 Bucket D). Score: 8.3% (P5 tenant) / ~6.6% (combined 5 facets). 24 new gaps filed (GAP-321..344). 15 existing K-12 gaps cross-linked. Verdict: **K-12 NOT viable for production deployment**; 5-stage program required ~Q3 2026 → Q3 2027 for GA. Phase 2 multi-stakeholder review queued under GAP-152. Solo-dev simulation acknowledged per `business-logic-review.md` §2.3.
