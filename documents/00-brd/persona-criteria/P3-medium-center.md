# Acceptance Criteria — P3 Medium Education Center (Trung tâm quy mô vừa)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** P3
**Persona name (VN):** Trung tâm quy mô vừa
**Persona name (EN):** Medium Education Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent C (Wave Persona-AC-Template, GAP-151 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real medium-center director + Product Owner + Finance lead sign-off (deferred to GAP-152)
**Tier:** 1 Primary
**Tracking:** GAP-151 Phase 1 → GAP-152 → GAP-153

---

## 0. Context

### Scale assumption (from `personas-catalog.md` §P3)
- **Users:** 5-20 teachers, 100-500 students, 10-50 classes, 3-5 admin staff (giám đốc + lễ tân + kế toán + ops)
- **Data volume:** 250 students × ~4 monthly tuitions = ~1000 invoices/year; 12 teachers × payroll cycles; ~500 attendance records/week per class × 30 active classes = ~15k attendance rows/week
- **Usage pattern:** Daily ops Mon-Sat; peak enrollment beginning of semester (Tháng 8 / Tháng 1); monthly billing cycle; weekly parent communication; semester-end report cards

### Organization archetype
- **Type:** Trung tâm dạy thêm / Trung tâm Anh ngữ / STEM club — multi-subject, organized but not K-12
- **Hierarchy:** Giám đốc / Owner → Quản lý học vụ (Academic manager) → Giáo viên (Teachers, full-time + part-time) → Lễ tân (Receptionist) → Kế toán (Accountant) → Học sinh (Students) ↔ Phụ huynh (Parents)
- **Decision-making:** Giám đốc owns subscription + pricing; Kế toán handles invoicing + payroll; Quản lý học vụ handles scheduling + teacher assignment; Lễ tân handles enrollment + parent comms

### Revenue tier mapping
- **Expected tier:** PREMIUM → ENTERPRISE (~3-10M VND/month tolerance)
- **Reason:** Multi-role admin needs RBAC; payroll commission engine = PREMIUM feature; bulk import + advanced reports = PREMIUM; multi-branch / B2B invoicing nudges toward ENTERPRISE

### Real-world reviewer profile
- **Acting role:** Giám đốc trung tâm Anh ngữ tại Đà Nẵng, 250 học sinh, 12 giáo viên + 3 admin staff, multi-subject (Anh-Toán-Sciences-Tin học), licensed center under MoET Sở GD-ĐT TP Đà Nẵng
- **Critical concerns:**
  1. Payroll/commission cho 12 teachers (varied % per teacher per class) — must not require manual Excel
  2. Multi-class scheduling conflict-free (30 classes × 12 teachers × 5 rooms × 7 time slots)
  3. Parent communication scale (250 students × 2 parents = ~500 contacts), without spamming
  4. Financial reporting per teacher + per branch (monthly P&L) for tax + management
  5. MoET licensing compliance — keep records 10 năm per Tax law for organized centers
  6. Multi-admin role-based access (giám đốc xem mọi thứ; lễ tân chỉ enrollment; kế toán chỉ finance)

---

## 1. Onboarding AC

Initial signup → tenant provisioning → first usable state.

- [ ] **AC-ONBOARD-001:** Giám đốc complete tenant signup + multi-admin role provisioning trong ≤30 phút
  - **Test:** Đăng ký tenant PREMIUM → wizard cấp 4 admin accounts (giám đốc, lễ tân, kế toán, ops) với role-based permissions distinct → mỗi admin nhận credential email + first login forces MFA setup
  - **Fail signal:** Wizard không support multi-admin; chỉ tạo được 1 owner; roles không có RBAC distinction; credentials gửi plaintext
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + org chart)

- [ ] **AC-ONBOARD-002:** Bulk staff onboarding — import 15 staff (12 teachers + 3 admin) qua xlsx ≤5 phút
  - **Test:** Upload xlsx 15 rows với cột [họ tên, email, phone, role, môn dạy (cho teacher), commission % default] → assert tất cả accounts created + email mời + tag môn cho teacher
  - **Fail signal:** Không có bulk staff import UI; phải tạo từng người manually; không hỗ trợ commission % field
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import xlsx)

- [ ] **AC-ONBOARD-003:** Academic year setup — tạo năm học 2026-2027 với 2 học kỳ + holidays VN ≤10 phút
  - **Test:** Wizard "New Academic Year" → chọn start/end dates → 2 semester segments → import holidays VN (Tết, 30/4, 2/9, etc.) → teaching weeks calculated tự động
  - **Fail signal:** Không có concept academic year; chỉ class-level dates; không có VN holidays preset
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-053 (academic year/semester structure)

- [ ] **AC-ONBOARD-004:** Multi-subject hierarchy setup — định nghĩa 4 môn (Anh, Toán, Sciences, Tin) với sub-levels (beginner/intermediate/advanced)
  - **Test:** Subject manager UI → tạo 4 môn → mỗi môn có 3 levels → mỗi level link với teachers qualified → curriculum templates per subject available
  - **Fail signal:** Chỉ có flat course list; không có subject hierarchy; không link teacher qualifications với subjects
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject per student)

- [ ] **AC-ONBOARD-005:** Branding wizard generates center identity (logo, theme, parent portal hostname) ≤15 phút
  - **Test:** Upload logo → AI Branding wizard chọn audience "Trung tâm Anh ngữ" + tone "Professional" → preview deployed tại `<center-slug>.kitehub.me` → giám đốc approve per resource (logo/banner/hero)
  - **Fail signal:** Không có branding wizard; subdomain không deploy; preview không show actual content
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 2. Daily Operations AC

Recurring workflows after onboarding.

- [ ] **AC-OPS-001:** Multi-class scheduling conflict-free — schedule 30 classes × 12 teachers × 5 rooms × 7 time slots without double-booking
  - **Test:** Quản lý học vụ vào "Schedule Builder" → drag-drop class to time slot → system blocks if (teacher đã có lớp), (phòng đã occupied), hoặc (student trùng giờ với lớp khác đã enrolled); calendar view hiển thị conflict warnings
  - **Fail signal:** Không có conflict detection; cho phép double-book teacher/room; calendar UI rời rạc per-class
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Attendance grid for 10-50 active classes — bulk mark attendance ≤2 phút per class
  - **Test:** Teacher login → "Today's classes" widget → mỗi lớp 1 click vào grid 25 students → mark present/absent/late với 1 tap → save → notification gửi cho parents của students vắng
  - **Fail signal:** Phải mark từng student per row; không có bulk action; không tự gửi parent notification cho absences
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-003:** Gradebook with multiple grade scales (10, 100, A-F) — teacher chấm 1 bài kiểm tra cho 25 students với scale tự chọn
  - **Test:** Teacher tạo assessment "Mid-term Speaking" → chọn scale (1-10 cho VN, A-F cho theo curriculum quốc tế) → chấm 25 students → grade auto-converts cho parent report card view (parent thấy 10-scale)
  - **Fail signal:** Chỉ 1 grade scale; conversion không auto; teacher phải duplicate work cho 2 scales
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-004:** Teacher assignment to classes — assign 12 teachers cho 30 classes dựa trên subject + qualification
  - **Test:** "Assign teachers" wizard → filter teachers by subject (Anh) → drag-drop to 8 English classes → system warns nếu teacher hours/week > contract limit (40h)
  - **Fail signal:** Không link qualification với assignment; không cảnh báo over-assignment; phải assign từng class một
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

- [ ] **AC-OPS-005:** Substitute teacher flow — teacher A báo nghỉ → system suggest 3 substitutes available + qualified
  - **Test:** Teacher A click "Báo nghỉ" cho lớp 14:00 ngày mai → system filter teachers (qualified subject + free slot + same level) → suggest 3 → quản lý học vụ chọn 1 → notification gửi substitute + students/parents
  - **Fail signal:** Không có substitute matcher; phải tìm manual; students/parents không được thông báo
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-006:** Room/resource management — track 5 rooms × capacity + equipment (projector, máy lạnh, etc.)
  - **Test:** Resource manager UI → 5 rooms với capacity 20-30 + equipment tags → assign class chỉ chấp nhận room có đủ capacity; book equipment riêng (e.g. projector cho lớp Tin)
  - **Fail signal:** Không track capacity; book class vượt capacity room; không có equipment booking
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-007:** Student transfer between classes giữa kỳ — transfer history + grade carry-over
  - **Test:** Student X chuyển từ Anh-Beg-A sang Anh-Beg-B giữa semester → system update enrollment + carry attendance history + grades; old class roster removes; parent notification fires
  - **Fail signal:** Phải withdraw + re-enroll (mất history); grades không carry; parent không biết
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-008:** Bulk student enrollment — import 50 students mới đầu kỳ qua xlsx ≤10 phút
  - **Test:** Upload xlsx 50 rows [họ tên, DOB, phụ huynh contact, lớp đăng ký] → validate → assign vào classes → tạo parent accounts paired → notification email/SMS với credentials
  - **Fail signal:** Không có bulk enrollment; phải nhập từng student; không tự pair parent account
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import xlsx)

- [ ] **AC-OPS-009:** Daily ops dashboard cho giám đốc — at-a-glance: today's classes, attendance %, no-show alerts, revenue YTD
  - **Test:** Giám đốc login → dashboard widgets: "30 classes hôm nay, 92% attendance, 3 no-show alerts, doanh thu tháng này 245M VND vs target 250M"; click widget → drill-down detail
  - **Fail signal:** Không có dashboard; phải vào từng module; widgets stale (>24h); không drill-down
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial / Admin AC

Billing, invoicing, payroll, financial reporting.

- [ ] **AC-FIN-001:** Tuition collection at scale — generate 250 monthly invoices in 1 batch ≤5 phút
  - **Test:** Kế toán click "Generate monthly invoices" cho tháng 5/2026 → system tính tuition per student dựa trên enrolled classes × pricing + sibling discount + late-fee carryover → 250 invoices PDF + email + Zalo notification
  - **Fail signal:** Generate từng invoice một; missing pricing rules; sibling discount manual; không gửi parent
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT)

- [ ] **AC-FIN-002:** Mixed payment methods — bank transfer + VNPay + MoMo + cash, all reconcile cùng 1 invoice ledger
  - **Test:** 3 invoices same student → 1 paid via VNPay (auto-reconcile), 1 paid bank transfer (reconcile từ MT940/CSV import), 1 paid cash (lễ tân nhập manual với receipt #) → ledger shows all 3 methods + status = paid
  - **Fail signal:** Chỉ 1 method per tenant; không reconcile bank statements; cash entry không có audit log
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT)

- [ ] **AC-FIN-003:** Teacher commission per-class with varied % — 12 teachers, mỗi teacher có commission % khác nhau per class
  - **Test:** Teacher Anh A: 40% commission cho lớp Anh-Adv (vì high tier), 30% cho Anh-Beg; Teacher Toán B: 35% flat → kế toán mở "Commission rules" → set % per teacher × per class → monthly run produces statement per teacher
  - **Fail signal:** Chỉ 1 % flat per teacher; không varied per class; phải tính Excel ngoài system
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

- [ ] **AC-FIN-004:** Payroll generation — 12 teachers monthly payroll = base salary + commission + deductions (BHXH/BHYT/thuế TNCN)
  - **Test:** Kế toán click "Run payroll" tháng 5 → system tính per teacher: base + Σ(commission per class) + (-) BHXH 8% + (-) BHYT 1.5% + (-) thuế TNCN bậc thang → payslip PDF + bank transfer file (MT940 format) ready
  - **Fail signal:** Không tính BHXH/BHYT/TNCN; phải xuất ra Excel; bank transfer file không match MT940
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank integration)

- [ ] **AC-FIN-005:** Monthly P&L per branch — revenue (tuition) − cost (payroll + rent + utilities + marketing) = profit; teacher-level breakdown
  - **Test:** Giám đốc mở "Financial Reports" → tháng 5/2026 → P&L hiển thị: Revenue 245M, Costs 180M (payroll 130M + rent 30M + utilities 10M + marketing 10M), Profit 65M; drill-down per teacher: doanh thu tạo ra vs commission paid
  - **Fail signal:** P&L không có; chỉ có revenue chart; không drill-down per teacher; không break down cost categories
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-FIN-006:** VAT invoice on demand for B2B parents — corporate parent yêu cầu hóa đơn VAT
  - **Test:** Phụ huynh A (B2B, công ty thanh toán học phí cho con) yêu cầu hóa đơn VAT → kế toán mở invoice → click "Convert to VAT" → nhập MST + tên công ty + địa chỉ → e-invoice generated theo Nghị định 123/2020/NĐ-CP + ký số → email PDF + XML cho công ty
  - **Fail signal:** Không có VAT invoice option; phải xuất manual qua hệ thống ngoài (như Misa); không ký số; không export XML cho TCT
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT)

---

## 4. Communication AC

Notifications, parent engagement, internal staff messaging.

- [ ] **AC-COMM-001:** Bulk parent notification via Zalo OA — gửi 500 parents (250 students × 2) thông báo lịch nghỉ Tết ≤2 phút
  - **Test:** Lễ tân tạo notification "Trung tâm nghỉ Tết 28/1 - 5/2/2026" → chọn audience "Tất cả phụ huynh" → preview → send → tất cả 500 parents nhận Zalo OA message + delivery receipt tracked
  - **Fail signal:** Không có Zalo OA integration; phải gửi từng người; không có delivery receipt
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-002:** Targeted alerts — gửi notification cho 1 specific class hoặc grade level
  - **Test:** Tạo alert "Buổi học hôm nay đổi giờ" → audience filter "Class = Anh-Beg-A" → 25 parents nhận; filter "Subject = Toán + Level = Advanced" → 60 parents nhận; system loga audience size trước khi send
  - **Fail signal:** Chỉ all-or-nothing; không filter granular; không preview audience size
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-003:** Monthly progress report to parents — automated PDF mỗi cuối tháng cho 250 students
  - **Test:** End-of-month cron → mỗi student generate progress report PDF (attendance %, grades, teacher comments, curriculum progress, upcoming tuition) → email + Zalo OA cho parent → giám đốc xem dashboard "report sent: 248/250, 2 failed"
  - **Fail signal:** Không có automated report; phải teacher viết manual mỗi tháng; failure không tracked
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-004:** Complaint handling SLA — parent gửi complaint qua portal; auto-route to giám đốc; SLA 48h
  - **Test:** Parent X gửi complaint "Con tôi vắng học mà tôi không nhận thông báo" → auto-route inbox giám đốc + lễ tân; SLA timer 48h start; auto-escalate nếu vượt; resolution log captured
  - **Fail signal:** Không có complaint workflow; email rời rạc; không SLA tracking
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal)

---

## 5. Edge Cases AC

Failure scenarios, peak loads, data corruption recovery.

- [ ] **AC-EDGE-001:** Teacher resignation mid-semester — handover students/classes/grades + commission settlement không lost data
  - **Test:** Teacher Anh A nghỉ ngày 15/5 → quản lý học vụ "Offboard teacher" wizard → reassign 3 lớp cho Teacher B + carry attendance/grades history → final commission tính đến 15/5 + payout → access revoked nhưng historical records preserved (per Tax law 10 năm)
  - **Fail signal:** Phải manual reassign; grades mất link; commission không pro-rata; access không revoke đúng cách
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

- [ ] **AC-EDGE-002:** Peak enrollment overload — 50 enrollments/giờ vào ngày đầu kỳ không break system
  - **Test:** Stress test simulate 50 concurrent parent enrollments × 3 children mỗi enrollment → trong 1 giờ; assert: no 5xx errors, payment processing không mất giao dịch, capacity warning UI hiển thị nếu lớp full
  - **Fail signal:** 5xx errors >1%; payment timeouts; over-enrollment vượt capacity; UI freeze
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EDGE-003:** Payment dispute escalation — parent contest invoice; refund flow + audit log
  - **Test:** Parent X dispute invoice INV-001 (claim đã đóng cash 2 tuần trước nhưng chưa reconcile) → kế toán mở "Dispute" → gắn evidence (cash receipt) → refund/credit applied → audit log captures: who created, evidence attached, decision rationale, timestamp; per Consumer Protection Law dispute window 24mo
  - **Fail signal:** Không có dispute workflow; refund không trace được; không audit log; vượt 24mo dispute window không block
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT)

- [ ] **AC-EDGE-004:** Audit trail for financial records — Tax law 10 năm retention enforced; immutable log
  - **Test:** Generate invoice/payment/payroll record → audit log row WORM (write-once read-many) cho immutability; query: "show all financial txns 2017-2026" returns 10 năm history; old records archived to cold storage but retrievable trong ≤24h
  - **Fail signal:** Records deletable/editable post-creation; không có 10-year retention enforced; archive irretrievable
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 6. Exit / Termination AC

Tenant offboarding, contract termination, MoET reporting.

- [ ] **AC-EXIT-001:** Student graduates / withdraws — final report card + grade transcript exportable
  - **Test:** Student Y hoàn thành Anh-Adv với certificate → giám đốc click "Issue completion certificate" → PDF với template trung tâm + ký giám đốc + QR verify → export student transcript (all grades, attendance) PDF cho parent
  - **Fail signal:** Không có completion certificate generator; transcript không exportable; QR verify không work
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EXIT-002:** Teacher leaves with commission settlement — final payslip + tax form (Mẫu 02/KK-TNCN cho freelance)
  - **Test:** Teacher rời 30/6 → kế toán click "Final settlement" → tính commission đến 30/6 + base salary pro-rata + bonus + (-) deductions; xuất payslip + Mẫu 02 cho thuế TNCN; bank transfer file ready; teacher account access lifted nhưng historical records preserved 10 năm
  - **Fail signal:** Không có Mẫu 02 export; không pro-rata commission; access không revoke đúng cách
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

- [ ] **AC-EXIT-003:** Tenant termination — center đóng cửa, data export + 10-năm retention transition + MoET reporting
  - **Test:** Giám đốc trigger "Close center" workflow → wizard: (1) export ALL data (students, payroll, financial 10 năm) cho local archive; (2) MoET notification template (báo cáo đóng trung tâm cho Sở GD-ĐT); (3) parent + staff notification "Trung tâm đóng từ ngày X"; (4) tenant chuyển sang "Archived" mode (read-only, retain 10 năm); (5) refund credits cho parents có balance
  - **Fail signal:** Bulk delete sau đóng (vi phạm Tax law); không có MoET template; parents/staff không thông báo; refund process manual + lossy
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-180 (TOS — termination clause)

---

## Scoring

**Total ACs:** 30 (5 Onboarding + 9 Daily Ops + 6 Financial + 4 Communication + 4 Edge Cases + 3 Exit)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported |
| 60-84% | ⚠️ Persona partially supported (defer GA cho persona này) |
| 30-59% | 🔴 Persona NOT supported (major gaps) |
| <30% | ❌ Persona NOT viable |

---

## Gap Linkage Summary

ACs filled at GAP-152 review time. Pre-filled with linked existing gaps:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-ONBOARD-002 | TBD | GAP-051 | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | GAP-053 | 🔵 OPEN | P1 |
| AC-ONBOARD-004 | TBD | GAP-054 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-OPS-008 | TBD | GAP-051 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-FIN-002 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-FIN-003 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-FIN-004 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1, P2 |
| AC-FIN-006 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-003 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-004 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-EDGE-001 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-EDGE-003 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-EXIT-002 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-EXIT-003 | TBD | GAP-180 | 🔵 OPEN | P1 |

**Candidate NEW gaps to file at review time** (state-check qua `audit-to-gap-pipeline.md` Step 2.5 trước khi filing):
- Multi-class scheduling conflict detection (AC-OPS-001) — likely no current gap, P3-specific
- Substitute teacher matcher (AC-OPS-005) — likely no current gap
- Room/resource booking (AC-OPS-006) — likely no current gap, niche cho P3
- Daily ops dashboard (AC-OPS-009) — likely overlap với GAP-052 parent portal but admin-side
- Complaint handling workflow (AC-COMM-004) — extends GAP-052
- Stress test enrollment peak (AC-EDGE-002) — performance gap
- WORM audit log (AC-EDGE-004) — compliance gap
- Completion certificate generator (AC-EXIT-001) — likely no current gap
- Mẫu 02/KK-TNCN export (AC-EXIT-002) — extends GAP-057

---

## Cross-References

- **Persona source:** [`../personas-catalog.md`](../personas-catalog.md) §P3 Medium Education Center
- **Sibling persona ACs:** P1 Solo Teacher / P2 Small Tutoring / P5 K-12 School (Wave Persona-AC-Template parallel agents)
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Review reports:** [`../persona-reviews/`](../persona-reviews/) (output of GAP-152 quarterly reviews)
- **AC framework gap:** [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md)
- **Review execution gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Secondary persona AC gap:** [GAP-153](../../04-quality/gaps/GAP-153-secondary-persona-ac-execute.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check

### Linked feature gaps (cross-link for review traceability)
- [GAP-051](../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) — Bulk import xlsx (CRITICAL: 250 students + 15 staff onboarding)
- [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) — Parent portal (CRITICAL: 250 × 2 parents engagement)
- [GAP-053](../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) — Academic year/semester (RELEVANT: organized scheduling)
- [GAP-054](../../04-quality/gaps/GAP-054-multi-subject-per-student.md) — Multi-subject (CRITICAL: multi-subject is core for P3)
- [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) — Payroll commission (CRITICAL: 12 teachers varied %)
- [GAP-058](../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md) — Role hierarchy (CRITICAL: multi-admin RBAC)
- [GAP-062](../../04-quality/gaps/GAP-062-teacher-payroll-bank-integration.md) — Payroll bank integration (RELEVANT: 12 teacher payroll bank file)
- [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) — SMS/Zalo notification (CRITICAL: 250 parents bulk + targeted)
- [GAP-064](../../04-quality/gaps/GAP-064-scorm-xapi-compliance.md) — SCORM/xAPI (PARTIAL: depends on subject mix; basic curriculum tracking enough)
- [GAP-180](../../04-quality/gaps/GAP-180-terms-of-service.md) — Terms of Service (RELEVANT: formal contract with parents)
- [GAP-185](../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) — Billing/VAT/TCT (RELEVANT: mixed B2C + B2B invoicing)

---

## Log

- **2026-04-30** — Initial AC set v1 (author: Agent C, Wave Persona-AC-Template, GAP-151 Phase 1). 30 ACs across 6 categories. Highlights: multi-role admin RBAC, teacher commission engine at 12-teacher × varied-% scale, MoET licensing compliance + Tax law 10-year retention, B2B+B2C invoicing với VAT e-invoice (NĐ 123/2020), bulk parent communication 500-recipient via Zalo OA. 11 cross-links to existing feature gaps + 9 candidate NEW gaps surfaced (queue qua `audit-to-gap-pipeline.md` Step 2.5 state-check at GAP-152 review time).
- **TBD** — GAP-152 Round 1 review by domain expert (Real medium-center director Đà Nẵng) + Product Owner + Finance lead sign-off; status updates fill PASS/PARTIAL/FAIL.
