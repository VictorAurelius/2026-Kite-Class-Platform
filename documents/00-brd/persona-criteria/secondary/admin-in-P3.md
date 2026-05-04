# Acceptance Criteria — Admin Staff in P3 Medium Education Center

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Admin × P3
**Persona name (VN):** Nhân viên hành chính trung tâm vừa (giám đốc / lễ tân / kế toán)
**Persona name (EN):** Admin Staff in Medium Education Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent D (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real medium-center admin (lễ tân + kế toán roles) + Finance lead + Product Owner (deferred to GAP-152)
**Tier:** 1 Primary (Tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152
**Tenant context:** Medium Education Center (P3)
**Role:** Admin Staff (secondary persona, multi-role: giám đốc / lễ tân / kế toán with RBAC differentiation)
**Note:** Distinct from P3 owner-as-tenant (owner usually wears giám đốc hat but center may employ separate giám đốc as admin)

---

## 0. Context

### Scale assumption (from `personas-catalog.md` §Secondary Personas + sibling `../P3-medium-center.md`)
- **Tenant scale:** 100–500 students, 5–20 teachers, **3–5 admin staff** (giám đốc + lễ tân + kế toán + ops admin + optional IT staff)
- **Admin workload pattern:**
  - **Lễ tân daily peak:** 08:00–10:00 + 17:00–19:00 (parent inquiries, walk-in enrollment, student check-in/out)
  - **Kế toán monthly peak:** Last 5 days of month (invoice batch generation), 5th–10th of next month (payment reconciliation), 25th–30th (payroll batch + BHXH/BHYT/TNCN remittance)
  - **Giám đốc weekly peak:** Monday morning (review previous week dashboards), end-of-month (P&L review + complaint escalation queue)
  - **Annual peak:** Tháng 8 / Tháng 1 (semester onboarding — bulk import, schedule resolution); annual MoET licensing renewal cycle
- **Admin profile:** **hired employees** (NOT owner) — receive RBAC-scoped accounts, work shifts, escalate up to giám đốc
- **Data they touch:** student enrollment forms, parent contact list, teacher payroll, financial ledgers, complaint queue, MoET license renewal documents

### Role differentiation (CRITICAL — RBAC-driven)
| Role | Scope | Cannot see | Daily core action |
|------|-------|-----------|-------------------|
| **Giám đốc** (acting Director, hired) | Full ops + financial summary + team mgmt | Tenant subscription/billing (owner only) | Approve payroll, resolve escalated complaints, sign MoET docs |
| **Lễ tân** (Receptionist) | Enrollment + parent contact + attendance attendance lookup + class schedule | Financial ledgers, payroll, teacher commission | Check-in students, answer parent calls, log enrollment inquiries |
| **Kế toán** (Accountant) | Financial ledgers + payroll + BHXH/BHYT/TNCN + VAT invoices | Student conduct grades, complaint queue, MoET ops | Generate monthly invoices, run payroll, reconcile bank statements |
| **Ops admin** (optional) | Schedule conflicts + room/teacher assignment + ops dashboards | Financial detail | Resolve schedule conflicts, manage class moves |

### Revenue tier mapping
- **Expected tier:** PREMIUM (~5M VND/month) — multi-role RBAC + commission engine + bulk operations are core
- **Reason:** Admin roles use 80% of FREE/BASIC features daily; if RBAC granular gating fails, admin staff fall back to Excel + email — center churns

### Real-world reviewer profile
- **Acting role:** "Nguyễn Thị Lan, kế toán trung tâm Anh ngữ Đà Nẵng 250 HS, làm kế toán 5 năm, đã dùng MISA + Excel trước đây — kỳ vọng KiteClass thay thế cả 2"
- **Critical concerns:**
  1. **RBAC scoping** — không được lộ payroll cho lễ tân; không được lộ complaint cho kế toán
  2. **Bulk operations** không hang khi thao tác 50–500 records
  3. **Audit log** mọi thao tác financial (yêu cầu Tax law 10 năm + dispute window)
  4. **Bank file format** chuẩn MT940 / Vietcombank / BIDV (kế toán không thể tự convert)
  5. **MoET licensing renewal** alert trước hạn ≥90 ngày (không thể trễ — Sở GD-ĐT đình chỉ hoạt động)
  6. **Complaint escalation flow** lễ tân → giám đốc trong SLA 48h (parent đã trả tiền — kỳ vọng response)

---

## 1. Onboarding AC

Initial admin account provisioning, RBAC permission verification, first-login system overview.

- [ ] **AC-ONBOARD-001:** Giám đốc tạo admin account cho lễ tân + kế toán + ops admin với role distinct trong ≤5 phút mỗi account
  - **Test:** Giám đốc vào "User Management" → "Add staff" → form: [họ tên, email, phone, role dropdown {giám đốc | lễ tân | kế toán | ops admin}, default permissions auto-applied per role] → submit → invite email + first-login MFA setup → login lần đầu thấy welcome tour cho role đó
  - **Fail signal:** Phải tạo permission từng cái một (không có role preset); không có MFA enforcement; welcome tour chung cho mọi role
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + RBAC)

- [ ] **AC-ONBOARD-002:** Lễ tân first login chỉ thấy modules được phép (enrollment, attendance lookup, parent contact, class schedule); KHÔNG thấy financial / payroll / complaint admin
  - **Test:** Lễ tân login → sidebar chỉ hiển thị 4 modules cho phép; truy cập trực tiếp URL `/admin/payroll` → 403 Forbidden + audit log "unauthorized access attempt"; truy cập URL `/admin/finance/p&l` → 403; UI show error message "Vui lòng liên hệ giám đốc"
  - **Fail signal:** Sidebar leak modules cấm; URL direct access không bị block; không audit log unauthorized attempts; error message generic không gợi escalation path
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + RBAC)

- [ ] **AC-ONBOARD-003:** Kế toán first login thấy financial dashboard + tax rule presets VN (BHXH 8% / BHYT 1.5% / TNCN bậc thang) đã configured
  - **Test:** Kế toán login → dashboard "Financial Center" với widgets: outstanding invoices, overdue >7 days, payroll due this month, VAT invoices to issue; mở "Tax Settings" → BHXH / BHYT / TNCN rates đã preset theo VN regulation 2026 (kế toán có thể override nếu trường có policy khác)
  - **Fail signal:** Dashboard rỗng / phải tự config; tax rates phải nhập tay; không có preset VN; rates outdated (2024 thay vì 2026)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-062 (payroll bank), GAP-185 (billing/VAT)

---

## 2. Daily Operations AC

Role-specific workflows — lễ tân (enrollment + parent contact), kế toán (payroll + invoicing), giám đốc (oversight), ops admin (schedule conflicts).

- [ ] **AC-OPS-001:** Lễ tân handle 50 walk-in enrollment inquiries/ngày peak season — log inquiry, follow-up, convert to enrollment trong ≤3 phút mỗi inquiry
  - **Test:** Lễ tân mở "Inquiry Queue" → quick-form: [parent name, phone, child name + age, môn quan tâm, trial class request] → submit → auto-create lead + assign follow-up reminder 24h; convert lead → enrollment wizard pre-fill data → 1-click bulk-add child as student → invoice + parent account auto-created
  - **Fail signal:** Phải dùng Excel ngoài hệ thống; lead không có follow-up reminder; convert phải nhập lại data; không link lead → enrollment cho conversion-rate analytics
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import — extends to single enrollment), GAP-052 (parent portal)

- [ ] **AC-OPS-002:** Lễ tân oversee bulk student import 50 students mới đầu kỳ — receive xlsx từ marketing/sales, validate, hand off to IT staff cho import; track status
  - **Test:** Lễ tân nhận xlsx 50 students → upload vào "Bulk Import Staging" → validation report (missing fields, duplicates, invalid phone formats) → fix issues / contact parent để xác nhận → handoff to IT staff with 1-click "Send for Import" → import progress visible to lễ tân + giám đốc; failure notifications routed back
  - **Fail signal:** Lễ tân tự import → can permission too broad; staging UI không có validation; hand-off không track status; failure không route lại; lễ tân không biết kết quả
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import), GAP-058 (role hierarchy — IT staff vs lễ tân)

- [ ] **AC-OPS-003:** Kế toán generate monthly invoice batch 250 students trong ≤5 phút + reconcile mixed payment methods (bank/VNPay/MoMo/cash)
  - **Test:** Kế toán click "Generate monthly invoices" → preview 250 invoices với pricing rules + sibling discount + late-fee carryover → confirm → 250 PDFs + email + Zalo notification batch sent; sau 5 ngày click "Reconcile payments" → import bank MT940 file + VNPay CSV + MoMo CSV → auto-match 200/250 invoices; manual cash entries cho 30 còn lại
  - **Fail signal:** Generate từng invoice một (>30s mỗi invoice); reconcile manual; không support MT940 format; cash entries không có audit log + receipt #
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT), GAP-062 (payroll bank — extends to invoice reconcile)

- [ ] **AC-OPS-004:** Kế toán run monthly payroll batch 12 teachers + deduct BHXH/BHYT/TNCN + generate bank transfer file MT940
  - **Test:** Kế toán click "Run payroll" tháng 5 → tính per teacher: base + Σ commission per class + (-) BHXH 8% + (-) BHYT 1.5% + (-) TNCN bậc thang → 12 payslips PDF + bank transfer file MT940 ready để upload Vietcombank/BIDV; audit log mọi thao tác
  - **Fail signal:** Không tính BHXH/BHYT/TNCN auto; export Excel rời rạc; bank file không match MT940 → kế toán phải convert tay
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank integration)

- [ ] **AC-OPS-005:** Ops admin / quản lý học vụ resolve scheduling conflicts — drag-drop class trên Schedule Builder, system flag conflicts (teacher double-book / room occupied / student overlap) trong real-time
  - **Test:** Ops admin mở "Schedule Builder" tuần 18/5–24/5 → drag class Anh-Beg-A từ Thứ 3 sang Thứ 4 → system check 3 axes (teacher / room / student) → flag conflict "Cô A đã có lớp Toán-Beg-B Thứ 4 14:00" → suggest 3 alternative slots → ops admin chọn → confirm → notification gửi teacher + parents affected
  - **Fail signal:** Không có conflict detection real-time; suggest slots manual; notification không tự gửi affected stakeholders; conflict log không lưu cho audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-006:** Giám đốc review parent complaint queue daily — escalation flow lễ tân → giám đốc với SLA 48h
  - **Test:** Lễ tân nhận complaint qua phone/parent portal → log vào "Complaint Queue" với category (academic / financial / safety / behavioral) → auto-route to giám đốc nếu category = safety hoặc nếu lễ tân click "Escalate"; giám đốc inbox alert; SLA timer 48h start; auto-escalate notification nếu vượt; resolution log captured (action taken, parent response, closure date)
  - **Fail signal:** Complaint qua email rời rạc; không có category routing; không có SLA tracker; no audit log of resolution; parent không biết case bị handle hay không
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — complaint submission)

- [ ] **AC-OPS-007:** Giám đốc daily ops dashboard — at-a-glance: today's classes (30), attendance % (92%), no-show alerts (3), complaints in SLA (5/7), revenue YTD (245M / 250M target)
  - **Test:** Giám đốc login → dashboard widgets refresh ≤30s; click widget "no-show alerts" → drill-down list 3 students với phụ huynh contact + last attendance; click "complaints in SLA" → top 5 complaints with countdown; click "revenue YTD" → break-down per branch / per month
  - **Fail signal:** Dashboard tải >5s; widgets stale (>1h); không drill-down; phải vào từng module để get detail
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial AC

Kế toán role — payroll, invoicing, tax reporting, audit trail.

- [ ] **AC-FIN-001:** Kế toán generate monthly P&L per branch trong ≤2 phút — revenue (tuition + extras) − cost (payroll + rent + utilities + marketing) = profit; teacher-level breakdown
  - **Test:** Kế toán mở "Financial Reports" → tháng 5/2026 → P&L hiển thị: Revenue 245M, Costs 180M (payroll 130M + rent 30M + utilities 10M + marketing 10M), Profit 65M; drill-down teacher Anh A: doanh thu tạo ra 32M vs commission paid 12.8M (40%); export PDF/Excel ready cho giám đốc
  - **Fail signal:** P&L chỉ revenue chart; không drill-down teacher; cost categories không break-down; export format không matched với MISA/SAP nếu trung tâm import vào hệ thống kế toán cũ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-FIN-002:** Kế toán issue VAT e-invoice cho B2B parents — corporate parent yêu cầu hóa đơn VAT theo NĐ 123/2020/NĐ-CP với chữ ký số + XML format chuẩn TCT
  - **Test:** Phụ huynh A (B2B, công ty Acme thanh toán học phí) yêu cầu hóa đơn VAT → kế toán mở invoice → click "Convert to VAT" → form: [MST 0312345678, tên cty Acme, địa chỉ HCM] → generate e-invoice với template TCT + ký số HSM → email PDF + XML cho công ty + push lên Tổng cục Thuế qua API
  - **Fail signal:** Không có VAT invoice option; không có chữ ký số HSM; XML không match TCT schema; không push API TCT (kế toán phải upload tay qua MISA / Viettel-Invoice)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT)

- [ ] **AC-FIN-003:** Kế toán remit BHXH/BHYT/TNCN monthly — generate báo cáo C12/13 (BHXH) + Mẫu 02/KK-TNCN (TNCN) + bank transfer files
  - **Test:** Kế toán click "Tax & Insurance Reports" tháng 5 → hệ thống generate: (1) báo cáo C12-TS BHXH với 12 teachers + đóng góp; (2) Mẫu 02/KK-TNCN cho TNCN; (3) bank transfer file riêng cho BHXH (đến cơ quan BHXH) + TNCN (đến KBNN); export XML cho cổng giao dịch BHXH (giaodichdientu.bhxh.gov.vn)
  - **Fail signal:** Không có C12-TS template; không match XML format BHXH; phải xuất Excel rồi nhập tay vào cổng BHXH/TCT
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank)

---

## 4. Communication AC

Bulk parent notification, internal staff messaging, complaint escalation queue.

- [ ] **AC-COMM-001:** Lễ tân send bulk parent notification via Zalo OA — 500 parents (250 students × 2) thông báo lịch nghỉ Tết trong ≤2 phút với delivery receipt tracked
  - **Test:** Lễ tân tạo notification "Trung tâm nghỉ Tết 28/1 - 5/2/2026" → audience "Tất cả phụ huynh" → preview → send → 500 parents nhận Zalo OA + delivery receipt; lễ tân xem dashboard "delivered: 487/500, failed: 13 (with reason: số sai / không có Zalo)"; failure list export để follow-up qua SMS
  - **Fail signal:** Không có Zalo OA integration; gửi từng parent một; không có delivery receipt; failure list không export
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-002:** Lễ tân send targeted alert — chỉ 1 lớp hoặc 1 grade level (audience filter granular)
  - **Test:** Lễ tân tạo alert "Buổi học hôm nay đổi giờ" → audience filter "Class = Anh-Beg-A" → preview hiển thị "25 phụ huynh" → send → 25 parents nhận; thử filter "Subject = Toán + Level = Advanced" → preview "60 phụ huynh"; system loga audience size trước khi send (để confirm không spam toàn trung tâm)
  - **Fail signal:** Chỉ all-or-nothing audience; không filter granular; preview không hiển thị size trước send; lễ tân không biết spam đã đi đâu
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-003:** Internal staff messaging — admin team có channel riêng cho daily ops handoff (lễ tân ↔ giám đốc, kế toán ↔ giám đốc); mention + thread reply + file attach
  - **Test:** Lễ tân tag @giám-đốc trong message "Phụ huynh Nguyễn Văn B yêu cầu refund hôm nay, parent claims đã đóng cash 2 tuần trước" + attach receipt photo → giám đốc nhận notification + reply trong thread; complaint case ID auto-link
  - **Fail signal:** Không có internal messaging; phải dùng Zalo group ngoài hệ thống → mất audit log; mention không trigger notification; file attach không persist trong audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 5. Edge Cases AC

Failure scenarios, peak loads, audit by Tax authority, system overload.

- [ ] **AC-EDGE-001:** Peak enrollment overload — 50 walk-in inquiries/giờ × 3 lễ tân concurrent đầu kỳ không break system
  - **Test:** Stress test simulate 3 lễ tân × 17 enrollment inquiries/giờ trong 2 giờ liên tục → assert: no 5xx errors, response time <2s p95, parent account creation không tạo duplicate khi sibling cùng đăng ký, payment processing không lost transaction
  - **Fail signal:** 5xx errors >1%; duplicate parent accounts; payment timeouts; UI freeze; lễ tân không thể login đồng thời
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EDGE-002:** MoET licensing renewal alert — system warn giám đốc trước hạn renewal ≥90 ngày + 30 ngày + 7 ngày
  - **Test:** Giấy phép MoET hết hạn 31/12/2026 → 1/10/2026 (90 ngày) giám đốc nhận email + dashboard banner "Renewal due in 90 days"; 1/12 (30 ngày) banner đổi màu cam + SMS giám đốc; 24/12 (7 ngày) banner đỏ + auto-escalate to owner; system NOT auto-suspend khi quá hạn (vì có grace period 30 ngày), nhưng banner đỏ + lock một số ops feature
  - **Fail signal:** Không có MoET renewal tracking; không có alert tiering; auto-suspend ngay khi quá hạn (vi phạm grace period thực tế của Sở GD-ĐT); không escalate to owner
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-180 (TOS — termination/renewal terms)

- [ ] **AC-EDGE-003:** Tax authority audit — kế toán generate full financial export 10 năm trong ≤2 giờ
  - **Test:** Cơ quan thuế yêu cầu audit 2017-2026 → giám đốc trigger "Tax Audit Export" → kế toán xác nhận → hệ thống generate package: invoices + receipts + payroll + bank statements + audit logs (WORM-immutable) → encrypted ZIP download + bàn giao cho thuế qua portal TCT; audit log ghi rõ ai trigger, ai download, hash file để verify integrity
  - **Fail signal:** Export không có audit log; records mutable / deletable; không match Tax law 10-year retention; không có WORM immutability; cơ quan thuế reject vì format
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT — audit trail)

---

## 6. Exit AC

Admin role change, departure, role transfer to successor, data handover.

- [ ] **AC-EXIT-001:** Admin staff resignation — handover responsibilities + revoke access ≤24h sau official last day; audit log preserved
  - **Test:** Lễ tân Lan thôi việc 30/6 → giám đốc trigger "Offboard staff" wizard → (1) reassign open inquiries / leads cho lễ tân Hoa; (2) reassign open complaints to giám đốc; (3) export Lan's audit log (PDF + CSV) for HR file; (4) revoke Lan's account access 30/6 23:59; (5) historical records preserved (10 năm Tax law); (6) Lan's name vẫn xuất hiện trong audit log nhưng marked "former staff" để tránh confusion
  - **Fail signal:** Phải reassign manual từng case; access không revoke timely; historical records bị xóa khi delete account; audit log reference Lan biến mất → break compliance trail
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + offboard workflow), GAP-184 (data retention)

- [ ] **AC-EXIT-002:** Admin role change — kế toán Lan promoted thành ops admin → role permissions update mid-day không lose work-in-progress
  - **Test:** Giám đốc click "Change role" cho Lan từ kế toán → ops admin → wizard prompt: "Lan có 3 invoices đang draft. Save + reassign to kế toán Hoa?" → Lan accept → role change at next login → Lan thấy new dashboard ops admin, no longer thấy financial modules; Hoa nhận 3 invoices draft với handover note
  - **Fail signal:** Role change instant không cảnh báo work-in-progress; Lan thấy mixed UI (cũ + mới); work-in-progress lost; không handover note
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

---

## Scoring

**Total ACs:** 18 (3 Onboarding + 7 Daily Ops + 3 Financial + 3 Communication + 3 Edge Cases + 2 Exit)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported |
| 60-84% | ⚠️ Persona partially supported |
| 30-59% | 🔴 Persona NOT supported (major gaps) |
| <30% | ❌ Persona NOT viable |

---

## Gap Linkage Summary

ACs filled at GAP-152 review time. Pre-filled with linked existing gaps:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-ONBOARD-002 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-ONBOARD-003 | TBD | GAP-062, GAP-185 | 🔵 OPEN | P1 |
| AC-OPS-001 | TBD | GAP-051, GAP-052 | 🔵 OPEN | P0 |
| AC-OPS-002 | TBD | GAP-051, GAP-058 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-185, GAP-062 | 🔵 OPEN | P1 |
| AC-OPS-004 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1 |
| AC-OPS-006 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-FIN-002 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-FIN-003 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-EDGE-002 | TBD | GAP-180 | 🔵 OPEN | P1 |
| AC-EDGE-003 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-EXIT-001 | TBD | GAP-058, GAP-184 | 🔵 OPEN | P1 |
| AC-EXIT-002 | TBD | GAP-058 | 🔵 OPEN | P1 |

**Candidate NEW gaps to file at review time** (state-check qua `audit-to-gap-pipeline.md` Step 2.5 trước khi filing):
- RBAC granular gating verification UI (AC-ONBOARD-002) — extends GAP-058
- Inquiry-to-enrollment lead conversion tracker (AC-OPS-001) — likely no current gap
- Schedule conflict 3-axis detection (AC-OPS-005) — likely no current gap, P3-specific
- Internal staff messaging channel (AC-COMM-003) — likely no current gap
- MoET licensing renewal alert tiering (AC-EDGE-002) — likely no current gap, P3+P5 shared
- WORM audit log for tax authority export (AC-EDGE-003) — extends GAP-185
- Admin offboard wizard (AC-EXIT-001) — extends GAP-058
- BHXH/BHYT/TNCN remittance XML export (AC-FIN-003) — likely no current gap, kế toán-specific

---

## Cross-References

- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §Secondary Personas — Admin row
- **Sibling tenant AC:** [`../P3-medium-center.md`](../P3-medium-center.md) — tenant-level (giám đốc-as-tenant); this doc is admin-as-employee
- **Sibling secondary AC:** [`teacher-employee-in-P3.md`](teacher-employee-in-P3.md) (parallel — Teacher in P3); [`student-in-P3.md`](student-in-P3.md), [`admin-in-P5.md`](admin-in-P5.md) (parallel — Admin in P5)
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md)
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (template)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Secondary persona AC gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (this Phase 1)

### Linked feature gaps (cross-link for review traceability)
- [GAP-051](../../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) — Bulk import (lễ tân oversees, IT staff executes)
- [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — Parent portal (complaint submission + lễ tân answers)
- [GAP-057](../../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) — Payroll commission (kế toán-driven)
- [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md) — Role hierarchy + RBAC (CRITICAL — admin-defining)
- [GAP-062](../../../04-quality/gaps/GAP-062-teacher-payroll-bank-integration.md) — Payroll bank MT940
- [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) — SMS/Zalo (lễ tân-driven bulk + targeted)
- [GAP-180](../../../04-quality/gaps/GAP-180-terms-of-service.md) — TOS (MoET renewal terms)
- [GAP-184](../../../04-quality/gaps/GAP-184-data-retention.md) — Data retention (10-year Tax law)
- [GAP-185](../../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) — Billing/VAT/TCT (kế toán e-invoice)

---

## Log

- **2026-04-30** — Initial AC set v1 (author: Agent D, Wave Secondary-Persona-AC, GAP-153 Phase 1). 18 ACs across 6 categories covering admin staff in Medium Center context. Highlights: 4-role RBAC differentiation (giám đốc / lễ tân / kế toán / ops admin), bulk parent communication 500-recipient via Zalo OA, kế toán full lifecycle (invoice batch → reconcile → payroll → BHXH/BHYT/TNCN → VAT e-invoice TCT), MoET licensing renewal alert tiering, WORM audit log for Tax authority audit (10-year retention). 9 cross-links to existing feature gaps + 8 candidate NEW gaps surfaced at review time.
- **TBD** — GAP-152 Round 1 review by domain expert (Real medium-center kế toán + lễ tân + giám đốc) + Finance lead + Product Owner sign-off; status updates fill PASS/PARTIAL/FAIL.
