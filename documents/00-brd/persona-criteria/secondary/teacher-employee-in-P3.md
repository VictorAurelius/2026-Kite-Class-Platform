# Acceptance Criteria — Teacher Employee in P3 Medium Education Center

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Teacher Employee × P3
**Persona name (VN):** Giáo viên thuê tại trung tâm vừa
**Persona name (EN):** Teacher Employee in Medium Education Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent C (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real medium-center teacher + Finance lead + Product Owner (deferred to GAP-152)
**Tier:** 1 Primary (Tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152
**Tenant context:** Medium Education Center (P3)
**Role:** Teacher Employee (secondary persona, hired NOT owner)
**Compensation model:** Per-class commission 50–80% of tuition collected

---

## 0. Context

### Scale assumption (from `personas-catalog.md` §"Secondary Personas — Teacher" + sibling `../P3-medium-center.md`)
- **Tenant scale:** 250 students, 12 teachers (typical mix: 5 full-time + 7 part-time), 30 active classes, 3-5 admin staff
- **This persona's scope:** ONE teacher inside the tenant — owns 4-8 active classes (not all 30); owns ~80-150 students (not all 250); peer to 11 other teachers
- **Usage pattern:** Daily Mon-Sat 14:00–21:00 (after-school center); peak before/after each class (attendance + lesson notes); weekly grade entry; monthly payslip review

### Organization archetype
- **Type:** Trung tâm Anh ngữ / Trung tâm STEM / Trung tâm dạy thêm — multi-subject specialist
- **Hierarchy (this persona's place):** Giám đốc → Quản lý học vụ (line manager) → **Teacher Employee (this persona)** ⇄ peer teachers
- **Decision-making:**
  - Teacher OWNS: own attendance entry, own grade entry, own lesson notes, own substitute requests
  - Teacher does NOT own: tuition pricing, class assignment (Quản lý học vụ assigns), commission % rules (Giám đốc + Kế toán set), tenant settings
  - Teacher CONSULTS on: lesson plan templates (peer subject lead), class transfer requests (Quản lý học vụ approves)

### Revenue tier mapping (passive — teacher does not own subscription)
- **Inherited tenant tier:** PREMIUM (P3 default)
- **Teacher-relevant features at PREMIUM:** mobile app for attendance, gradebook UI, commission statement view, peer messaging, lesson plan library

### Real-world reviewer profile
- **Acting role:** "Cô Anh, 32 tuổi, giáo viên Tiếng Anh tại Trung tâm Anh ngữ Đà Nẵng (P3 archetype). Hợp đồng part-time với 8 lớp/tuần (3 lớp Beginner + 3 lớp Intermediate + 2 lớp Advanced), commission 50% Beg / 60% Inter / 70% Adv. Có 1 con nhỏ (cần linh hoạt giờ)."
- **Critical concerns:**
  1. **Earnings transparency** — phải xem được commission earnings real-time, không đợi cuối tháng mới biết; mỗi class × tuition collected × % của mình = bao nhiêu
  2. **Schedule clarity** — chỉ xem lớp của mình, không bị nhiễu lịch toàn trung tâm; mobile-friendly cho check trước giờ dạy
  3. **Bulk attendance speed** — 25-30 HS / lớp × 8 lớp/tuần — phải ≤2 phút / lớp, không thì sẽ về Excel
  4. **Payslip clarity** — base + commission + BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc thang, mỗi khoản breakdown rõ ràng; Mẫu 02/KK-TNCN cuối năm
  5. **Substitute coverage flexibility** — báo nghỉ + tự gợi ý người thay (peer) trong cùng môn, không phụ thuộc 100% vào Quản lý học vụ
  6. **Privacy boundary** — không xem được dữ liệu lớp/HS không phải của mình; không xem lương đồng nghiệp

---

## 1. Onboarding AC

Initial signup → role assignment → first usable state.

- [ ] **AC-ONBOARD-001:** Teacher nhận credentials qua bulk staff import (xlsx) + first login forces password reset + role-based dashboard hiển thị đúng "Teacher Employee" view (không phải admin/owner view)
  - **Test:** Giám đốc bulk-import 12 teachers qua xlsx (per `P3-medium-center.md` AC-ONBOARD-002) → cô Anh nhận email với credential + temp password; first login → forced password reset + MFA setup; dashboard mặc định: "My Classes (8 lớp)", "My Schedule today", "Pending Tasks (attendance/grade)", "This month earnings: ##" — KHÔNG hiển thị tenant-level dashboard widgets (revenue, all teachers, etc.)
  - **Fail signal:** Teacher login thấy admin dashboard (RBAC fail) hoặc thấy tenant-wide finance; password gửi plaintext; không có MFA; dashboard hiển thị tất cả 30 lớp thay vì 8 lớp của mình
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

- [ ] **AC-ONBOARD-002:** Subject + qualification self-service profile — teacher xem & hoàn thiện profile (môn dạy, levels qualified, certifications, ngân hàng cho commission payout) ≤15 phút
  - **Test:** Cô Anh mở "My Profile" → fields: subjects taught (Anh), levels qualified (Beg/Inter/Adv), certifications (TESOL #), bank account (Vietcombank STK) → upload chứng chỉ PDF → save; HR + Quản lý học vụ nhận review notification; sau approve, teacher có thể được assign vào lớp matching qualification
  - **Fail signal:** Không có self-service profile, phải gửi giấy tờ qua email; bank account hardcoded ở payroll module riêng (không sync); upload certification format kỳ lạ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

- [ ] **AC-ONBOARD-003:** Class assignment notification + acceptance flow — Quản lý học vụ assign teacher vào 8 lớp; teacher nhận notification + có thể accept/decline (với lý do) trong ≤24h
  - **Test:** Quản lý học vụ assign cô Anh vào 8 lớp HK2 (3 Beg + 3 Inter + 2 Adv) với commission % per class → teacher nhận notification (push + email + Zalo) liệt kê 8 lớp với time slots + commission % + estimated weekly hours (24h) → cô Anh "Accept all" hoặc "Decline" 1-2 lớp với lý do (vd "Trùng giờ con đón học") → Quản lý học vụ nhận decline + reassign
  - **Fail signal:** Class assignment im lặng (teacher chỉ phát hiện khi xem lịch); không có decline option (top-down only); không có lý do → conflict không resolve được
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy)

---

## 2. Daily Operations AC

Recurring workflows after onboarding (largest category).

- [ ] **AC-OPS-001:** Own-class schedule view — mobile-friendly weekly calendar hiển thị 8 lớp của riêng cô Anh (không phải toàn trung tâm); click lớp → roster + lesson plan + room
  - **Test:** Cô Anh mở mobile app sáng thứ 2 → "My week" calendar hiển thị 8 lớp tuần này với time + room + class name; tap lớp Anh-Inter-A 16:00 → roster 25 HS + lesson plan tuần trước + phòng A201; "Today's classes" widget hiển thị 2 lớp hôm nay với "Take attendance" CTA prominent
  - **Fail signal:** Calendar hiển thị toàn 30 lớp của trung tâm (privacy + UX fail); mobile responsive kém; không có "Today's classes" shortcut; không thấy room
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Bulk attendance entry per class — mark 25-30 HS in ≤2 phút trên mobile, 1 tap / HS để toggle present/absent/late, auto-save không lost data nếu mạng yếu
  - **Test:** Cô Anh vào lớp Anh-Inter-A 16:00 → "Take attendance" → grid 25 HS với photo + name → 1 tap each: present (default green) / late (yellow) / absent (red) → swipe tổng 25 HS trong 90s → submit; với 2 vắng → SMS auto fires đến parent của 2 HS đó (per `P3-medium-center.md` AC-OPS-002); mạng mất giữa chừng → local cache + auto-resume khi reconnect
  - **Fail signal:** 1 row / HS (slow); không bulk action; không SMS parent auto; mạng yếu mất data; không offline-first cache
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-OPS-003:** Multi-class gradebook — chỉ xem & nhập điểm cho 8 lớp của mình (không thấy lớp đồng nghiệp); per-assessment scoring với scale 1-10 (VN default) + auto-convert sang A-F nếu lớp Adv theo curriculum quốc tế
  - **Test:** Cô Anh tạo assessment "Mid-term Speaking" cho lớp Anh-Adv-A → scale chọn 1-10 (cho VN report) HOẶC A-F (vì lớp Adv theo CEFR) → grade 25 HS → save; gradebook view chỉ hiển thị 8 lớp; thử mở lớp Toán-Beg-X (đồng nghiệp) → 403 Forbidden hoặc "Not your class"
  - **Fail signal:** Có thể truy cập gradebook lớp khác (privacy); chỉ 1 scale; conversion manual; không track per-assessment (chỉ tổng kết cuối kỳ)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject per student) — RELEVANT cho specialist teacher

- [ ] **AC-OPS-004:** Lesson plan + homework assignment — teacher tạo lesson plan template + assign BTVN cho 1 lớp với deadline; HS thấy task + parent thấy "BTVN: assigned"
  - **Test:** Sau lớp Anh-Inter-A, cô Anh mở "Lesson plans" → ghi nội dung tuần này "Unit 5: Past Perfect" + assign homework "Workbook p.42-44, hạn thứ 5" → 25 HS + parents nhận notification; cô Anh xem dashboard "Homework completion: 18/25 nộp đúng hạn"
  - **Fail signal:** Không có lesson plan module (phải Word ngoài); homework assignment không sync với student/parent portal; không track completion %
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — homework visibility)

- [ ] **AC-OPS-005:** Substitute request — teacher báo nghỉ 1 lớp + tự gợi ý peer thay thế (cùng môn + qualified) → Quản lý học vụ approve trong ≤30 phút
  - **Test:** Cô Anh báo nghỉ lớp Anh-Beg-B thứ 5 14:00 (con ốm) → wizard "Báo nghỉ" → chọn lớp + lý do; system suggest 3 peer teachers (Anh-qualified + free slot + same level) → cô Anh propose teacher B; Quản lý học vụ nhận notification, approve trong 30 phút; teacher B nhận assignment + roster + lesson plan; HS/parent nhận thông báo "Lớp Anh-Beg-B thứ 5 do cô B đứng lớp"
  - **Fail signal:** Không có substitute matcher (phải gọi điện peer); teacher không tự suggest được; HS/parent không được thông báo; substitute không có quyền access lớp
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: `P3-medium-center.md` AC-OPS-005 (admin-side substitute flow)

- [ ] **AC-OPS-006:** Peer collaboration — subject lead (vd Tổ trưởng tổ Anh) approve lesson plans + share library với peers; teacher view shared library + comment
  - **Test:** Cô Anh upload lesson plan "Unit 5 Past Perfect" → request review từ subject lead; subject lead approve + tag "Recommended for Inter level" → 6 Anh-teachers thấy lesson plan trong shared library; cô Anh download/clone cho lớp khác; comment "Hoạt động group work cuối hoạt động OK với HS yếu"
  - **Fail signal:** Không có shared library (mỗi teacher Excel riêng); không peer review workflow; không tagging; không version history
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-007:** Class transfer between teachers (mid-semester) — student chuyển từ lớp cô Anh sang đồng nghiệp; cô Anh handover grades/attendance/notes mà không lost data; commission pro-rata
  - **Test:** HS A muốn chuyển từ Anh-Inter-A (cô Anh) sang Anh-Inter-B (cô C) giữa kỳ → Quản lý học vụ trigger transfer → cô Anh notification + handover form: student attendance history (auto), grades to-date (auto), free-text notes (cô Anh viết "HS A tiến bộ nhanh, recommend lên Adv"); cô C accept; commission cô Anh tính đến ngày transfer (pro-rata)
  - **Fail signal:** Phải withdraw + re-enroll (mất history); grades không carry; no handover notes; commission không pro-rata gây dispute
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: `P3-medium-center.md` AC-OPS-007

---

## 3. Financial AC

Commission earning visibility, payslip, tax statements.

- [ ] **AC-FIN-001:** Real-time commission earnings dashboard — teacher xem earnings to-date trong tháng (per class × tuition collected × my %); transparent breakdown
  - **Test:** Cô Anh mở "My Earnings" giữa tháng → dashboard hiển thị: "Tháng 5/2026 (đến nay): Anh-Beg-A 3.5M (50% × 7M tuition collected), Anh-Inter-A 4.8M (60% × 8M), Anh-Adv-A 2.1M (70% × 3M, chỉ 12/25 HS đã đóng) → Total to-date 10.4M; Projected end-of-month nếu tất cả thu đủ: 14.2M"; click drill-down per class → list HS + paid status
  - **Fail signal:** Earnings chỉ thấy cuối tháng (không real-time); breakdown không transparent (chỉ tổng); teacher phải hỏi kế toán; tuition collection % không phản ánh
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

- [ ] **AC-FIN-002:** Monthly payslip với deductions breakdown — cuối tháng nhận payslip PDF với base + commission + BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc thang, từng dòng rõ ràng + bank transfer reference
  - **Test:** Ngày 5/6/2026 cô Anh nhận payslip PDF cho tháng 5: lương cơ bản 5M (full-time) hoặc 0 (part-time), commission Σ = 14.2M, total gross 19.2M; (-) BHXH 8% × (mức đóng) = 1.04M; (-) BHYT 1.5% × (mức đóng) = 195k; (-) BHTN 1% × (mức đóng) = 130k; (-) thuế TNCN bậc thang per Luật Thuế TNCN 2007/2012 = ~1.2M; net = 16.6M; bank transfer ref VCB-2026-05-Anh-001
  - **Fail signal:** Payslip thiếu BHXH/BHYT/BHTN/TNCN breakdown; gộp vào "Deductions" mơ hồ; không có bank transfer ref; PDF không có header trung tâm hoặc digital signature
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank integration)

- [ ] **AC-FIN-003:** Annual tax statement (Mẫu 02/KK-TNCN cho freelance / part-time) — cuối năm teacher download Mẫu 02 đã pre-fill cho self-declaration thuế TNCN
  - **Test:** Tháng 1/2027 cô Anh mở "Tax statements 2026" → Mẫu 02/KK-TNCN PDF pre-filled với: tên, MST cá nhân, tổng thu nhập 2026, thuế TNCN đã khấu trừ, người chi trả (trung tâm có MST), period; cô Anh download → bổ sung thu nhập khác (nếu có) → submit qua portal Tổng cục Thuế hoặc trực tiếp
  - **Fail signal:** Không có Mẫu 02 export (phải tự ghi tay từ payslip); pre-fill thiếu MST/tổng thu nhập; không match format Tổng cục Thuế; vi phạm Luật Quản lý Thuế 2019
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

---

## 4. Communication AC

Stakeholder messaging — student, parent, peer, admin.

- [ ] **AC-COMM-001:** Parent communication scoped to own classes — teacher gửi message cho parents của HS trong 8 lớp của mình (không phải toàn trung tâm); via Zalo OA + delivery receipt
  - **Test:** Cô Anh tạo notification "Tuần sau lớp Anh-Inter-A có bài kiểm tra 15 phút Past Perfect" → audience auto-scope: parents của 25 HS trong Anh-Inter-A (= ~50 parents); preview → send via Zalo OA → 50 deliveries trong ≤30s + delivery receipt; cô Anh KHÔNG gửi được cho parents lớp khác
  - **Fail signal:** Audience scope không tự giới hạn (privacy fail — teacher gửi nhầm parents lớp khác); không có Zalo integration; không delivery receipt
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-002:** 1:1 parent chat (platform-mediated) — phụ huynh chat với teacher trong portal; conversations archived 24 tháng cho compliance
  - **Test:** Phụ huynh HS A mở conversation với cô Anh qua portal → "Em A có vẻ stress vì BTVN nhiều, cô có thể giảm cho em không?" → cô Anh trả lời "Tôi sẽ điều chỉnh tuần này, cảm ơn anh chị thông báo"; conversation log preserved 24 tháng (Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 dispute window); teacher KHÔNG share số điện thoại cá nhân (platform-only)
  - **Fail signal:** Không có 1:1 chat trong platform (parent gọi trực tiếp, không trace); conversation không archive; teacher phải share số cá nhân
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal)

- [ ] **AC-COMM-003:** Peer + admin internal messaging — teacher chat với peer (cùng môn) + escalate to Quản lý học vụ; channels separated từ parent comms
  - **Test:** Cô Anh tạo thread "Anh-team" với 5 đồng nghiệp Anh → discuss curriculum; cô Anh DM Quản lý học vụ "Em xin nghỉ ngày 20/5 vì việc gia đình" → manager respond → leave request approved trong app; channels rõ ràng tách biệt khỏi parent chat
  - **Fail signal:** Internal chat trộn với parent chat (privacy); không có DM admin escalation flow; phải dùng Zalo cá nhân
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 5. Edge Cases AC

Failure scenarios, partial failures, special situations.

- [ ] **AC-EDGE-001:** Teacher sick leave / personal leave — báo nghỉ 3-5 ngày; substitute coverage cho tất cả 8 lớp; commission pro-rata
  - **Test:** Cô Anh báo nghỉ ốm 3 ngày (15-17/5) → wizard "Leave request" → upload giấy bệnh viện (optional); system find substitutes cho 8 lớp × 3 ngày = ~24 lớp slots; Quản lý học vụ phối hợp + approve; commission cô Anh tính đến 14/5; substitutes nhận commission cho ngày 15-17/5; HS/parent nhận thông báo
  - **Fail signal:** Phải tự gọi điện đồng nghiệp; commission không pro-rata; HS/parent không thông báo; medical certificate không attach được
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: AC-OPS-005 substitute

- [ ] **AC-EDGE-002:** Commission dispute resolution — teacher khiếu nại commission tháng (kê thiếu 1 lớp); workflow + audit log + 30-day SLA
  - **Test:** Cô Anh nhận payslip 5/2026 thấy chỉ 7 lớp tính commission (thay vì 8) → mở "Dispute payslip" → ghi rõ lớp Anh-Adv-A bị miss; ticket route đến Kế toán + Quản lý học vụ CC; Kế toán review tuition collection record + class assignment → confirm miss → adjustment trong payslip 6/2026 với note "Adjustment for May 2026 — Anh-Adv-A commission"; full audit log: who claimed, evidence, decision, timestamp; SLA 30 ngày
  - **Fail signal:** Không có dispute workflow (phải email Kế toán + lost in inbox); không audit log; không SLA → vô tận; commission đã pay không adjust được
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

- [ ] **AC-EDGE-003:** Mid-semester contract change — teacher upgrade từ part-time sang full-time (hoặc ngược lại); base salary kích hoạt; commission % adjust
  - **Test:** Cô Anh chuyển từ part-time (commission only) sang full-time (5M base + commission giảm 10%) ngày 1/6/2026 → Quản lý học vụ + HR mở "Contract change" wizard → effective date 1/6 → payslip 6/2026 reflect: 5M base (pro-rata if mid-month) + commission tính theo % mới; cô Anh nhận confirmation email + new contract PDF; old contract archived 5 năm
  - **Fail signal:** Contract change manual (Excel ngoài), payslip không tự update, old contract không archive (vi phạm Bộ luật Lao động 2019 retention)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

---

## 6. Exit / Termination AC

Resignation, settlement, data handover.

- [ ] **AC-EXIT-001:** Teacher resignation — 30-day notice + handover 8 lớp + final commission settlement
  - **Test:** Cô Anh submit resignation ngày 1/6/2026 effective 30/6 (30-day notice per Bộ luật LĐ 2019 Đ.35) → wizard: (1) handover 8 lớp — Quản lý học vụ assign new teachers per class với deadline 25/6; (2) cô Anh export lesson plans + grade history + free-text notes per lớp cho successor; (3) final commission tính đến 30/6 + bonus (nếu eligible) + (-) final deductions → final payslip 5/7; (4) Mẫu 02/KK-TNCN cho period 1/1-30/6/2026 export; (5) account access revoked 30/6 23:59 nhưng historical records preserved 5 năm (per Tax law)
  - **Fail signal:** Không có handover wizard (lesson plans + grades mất); commission không pro-rata; final tax statement không export; account access không revoke đúng cách (latent risk)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-058 (role hierarchy)

- [ ] **AC-EXIT-002:** Center termination (tenant closes) impact on teacher — center đóng cửa giữa kỳ; teacher final settlement + data export + reference letter
  - **Test:** Trung tâm thông báo đóng cửa từ 31/7/2026 → tất cả 12 teachers nhận notification ngày 1/7 (30-day notice); cô Anh: (1) final commission Σ đến 31/7 + severance pay theo Bộ luật LĐ Đ.46-48; (2) export personal teaching portfolio (lesson plans, grades taught, certifications uploaded) trước cutoff; (3) reference letter từ Giám đốc PDF có ký số; (4) account access transitions to read-only 90 ngày (cho download), sau đó archive theo tenant retention policy
  - **Fail signal:** Teacher không 30-day notice (vi phạm LĐ); severance không tính đúng; portfolio không exportable (mất công sức 5 năm); không reference letter
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-180 (TOS — termination clause for teachers), Related: `P3-medium-center.md` AC-EXIT-003

---

## Scoring

**Total ACs:** 21 (3 Onboarding + 7 Daily Ops + 3 Financial + 3 Communication + 3 Edge Cases + 2 Exit)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported |
| 60–84% | ⚠️ Persona partially supported |
| 30–59% | 🔴 Persona NOT supported |
| <30% | ❌ Persona NOT viable |

---

## Gap Linkage Summary

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-ONBOARD-002 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-ONBOARD-003 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-OPS-002 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-054 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-FIN-002 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1, P2 |
| AC-FIN-003 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-EDGE-002 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-EDGE-003 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-EXIT-001 | TBD | GAP-057, GAP-058 | 🔵 OPEN | P1 |
| AC-EXIT-002 | TBD | GAP-180 | 🔵 OPEN | P1 |

**Candidate NEW gaps to file at review time** (state-check qua `audit-to-gap-pipeline.md` Step 2.5 trước khi filing):
- Real-time teacher earnings dashboard (AC-FIN-001) — likely no current gap, extends GAP-057
- Mẫu 02/KK-TNCN annual export (AC-FIN-003) — extends GAP-057
- Teacher self-service profile + qualifications upload (AC-ONBOARD-002) — likely no current gap
- Class assignment accept/decline workflow (AC-ONBOARD-003) — extends GAP-058
- Lesson plan shared library + peer review (AC-OPS-006) — likely no current gap
- Substitute teacher self-suggest peer (AC-OPS-005) — extends `P3-medium-center.md` AC-OPS-005 admin flow
- Teacher 1:1 parent chat platform-mediated (AC-COMM-002) — extends GAP-052
- Internal teacher↔admin messaging (AC-COMM-003) — likely no current gap
- Commission dispute workflow + 30-day SLA (AC-EDGE-002) — likely no current gap
- Mid-semester contract change wizard (AC-EDGE-003) — extends GAP-057
- Teacher resignation handover wizard (AC-EXIT-001) — likely no current gap

---

## Cross-References

- **Sibling tenant AC:** [`../P3-medium-center.md`](../P3-medium-center.md) — admin-side workflows (assign teachers, run payroll, manage substitutes)
- **Sibling secondary AC:** `student-in-P3.md`, `admin-in-P3.md` (Wave Secondary-Persona-AC parallel agents)
- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas — Teacher"
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md) v1.2+
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (tenant AC + template)
- **Secondary AC execution gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (this Phase 1)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check

### Linked feature gaps (cross-link for review traceability)
- [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — Parent portal (RELEVANT: 1:1 chat + homework visibility)
- [GAP-053](../../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) — Academic year (RELEVANT: semester boundaries cho commission cycle)
- [GAP-054](../../../04-quality/gaps/GAP-054-multi-subject-per-student.md) — Multi-subject (RELEVANT: specialist teacher dạy multi-level cùng môn)
- [GAP-057](../../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) — Payroll commission (CRITICAL: per-class varied %, 50-80%)
- [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md) — Role hierarchy (CRITICAL: teacher RBAC, scope to own classes)
- [GAP-062](../../../04-quality/gaps/GAP-062-teacher-payroll-bank-integration.md) — Payroll bank integration (RELEVANT: bank transfer monthly)
- [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) — SMS/Zalo notification (CRITICAL: parent comms scoped)
- [GAP-064](../../../04-quality/gaps/GAP-064-scorm-xapi-compliance.md) — SCORM/xAPI (PARTIAL: depends on subject; basic curriculum tracking enough)
- [GAP-180](../../../04-quality/gaps/GAP-180-terms-of-service.md) — TOS (RELEVANT: termination clause for teacher contracts)

---

## Log

- **2026-04-30** — Initial AC set v1 (author: Agent C, Wave Secondary-Persona-AC, GAP-153 Phase 1). 21 ACs across 6 categories. Highlights: per-class commission earning real-time visibility (50-80% varied %), self-scoped privacy boundary (own 8 lớp only), peer collaboration via shared lesson plan library, full payslip breakdown (BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc thang), commission dispute workflow, Mẫu 02/KK-TNCN annual export. 11 cross-links to existing feature gaps + 11 candidate NEW gaps surfaced (queue qua `audit-to-gap-pipeline.md` Step 2.5 state-check at GAP-152 review time).
- **TBD** — GAP-152 Round 1 review by domain expert (real medium-center teacher Đà Nẵng) + Finance lead + Product Owner sign-off; status updates fill PASS/PARTIAL/FAIL.
