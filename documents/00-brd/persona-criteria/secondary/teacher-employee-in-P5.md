# Acceptance Criteria — Teacher Employee in P5 K-12 School (GVCN + Bộ môn)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Teacher Employee × P5
**Persona name (VN):** Giáo viên trường K-12 (GVCN + Bộ môn)
**Persona name (EN):** Teacher Employee in K-12 School (Homeroom + Subject)
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent C (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real GVCN + Bộ môn teacher + Hiệu phó + MOET education expert + Product Owner (deferred to GAP-152)
**Tier:** 1 Primary (USER PRIORITY tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152
**Tenant context:** Public/Private K-12 School (P5)
**Role:** Teacher Employee (secondary persona, GVCN homeroom OR Bộ môn subject teacher)
**Compensation model:** Fixed salary per MOET payscale + allowances
**Legal compliance:** TT 22/2021 evaluation, TT 32/2020 management (sổ đầu bài), Bộ luật Lao động 2019 (employment), Luật Trẻ em 2016 (child safety vetting)

---

## 0. Context

### Scale assumption (from `personas-catalog.md` §"Secondary Personas — Teacher" + sibling `../P5-k12-school.md`)
- **Tenant scale:** 800 students, 50 teachers (5 GVCN homeroom + 45 bộ môn subject teachers — typical THCS), 30 classes (vd 7A→7F, 8A→8F, 9A→9F + 6 lớp khối 6), 15 admin/staff
- **This persona's scope (TWO ROLE VARIANTS in single AC doc):**
  - **GVCN (Giáo viên chủ nhiệm / homeroom):** owns 1 lớp full year (vd 7A 42 HS), daily roll-call + weekly conduct + monthly parent contact + report card preparation, primary parent contact
  - **Bộ môn (subject teacher):** dạy 1 môn (vd Toán) across 5-8 classes (200-300 HS), multi-class gradebook, 18-22 periods/week, less parent contact (GVCN handles)
- **Role overlap:** 1 teacher CAN be both GVCN of 7A + bộ môn Toán dạy 7B/7C/8A/8B; AC mixed apply. ACs marked `[GVCN]` apply to homeroom variant, `[BỘ MÔN]` apply to subject variant, unmarked apply to both.
- **Usage pattern:** Daily Mon-Sat 07:00–17:00 (school hours); peak 07:00–08:00 (GVCN morning roll-call) + each period transition (bộ môn attendance + sổ đầu bài); weekly conduct (GVCN); monthly parent reports (GVCN)

### Organization archetype
- **Type:** Trường THCS công lập / Trường THPT tư thục — MOET-regulated
- **Hierarchy (this persona's place):** Hiệu trưởng → Phó hiệu trưởng (Phó CMUE chuyên môn / Phó CSVCH cơ sở vật chất) → Tổ trưởng chuyên môn (vd tổ Toán) → **Teacher (this persona — GVCN OR Bộ môn)** ⇄ peer teachers
- **Decision-making:**
  - Teacher OWNS: own attendance entry per period, own grade entry per assessment, own lesson plans, sổ đầu bài per period
  - GVCN ADDITIONALLY OWNS: conduct grade (hạnh kiểm) for class, parent communication for homeroom, daily roll-call aggregate, weekly summary signing
  - Teacher does NOT own: salary scale (MOET-mandated), class assignment (Hiệu phó assigns), MOET reporting (admin); subject curriculum (TT 32/2018 mandate)

### Revenue tier mapping (passive — teacher does not own subscription)
- **Inherited tenant tier:** ENTERPRISE (P5 default — 800 students × MOET regulations)
- **Teacher-relevant features at ENTERPRISE:** mobile app for period attendance, multi-class gradebook with TT 22/2021 formula, sổ đầu bài digital, conduct grade tracker, parent meeting coordination, exam invigilation roster

### Real-world reviewer profile
- **Acting role (mixed scenario):** "Cô Hương, 38 tuổi, GVCN lớp 7A (42 HS) + Bộ môn Toán dạy 7B, 7C, 8A, 8B (~165 HS thêm). Tổng 207 HS. 22 periods/week. Trường THCS công lập 800 HS quận trung tâm Hà Nội. Lương cơ bản theo bậc 3 nghề giáo + phụ cấp đứng lớp 30% + phụ cấp GVCN + thâm niên. Đã dạy 12 năm."
- **Critical concerns:**
  1. **GVCN morning roll-call speed** — 7:00-7:30 cô Hương đến lớp 7A → roll-call 42 HS phải ≤2 phút trên mobile, phân biệt vắng có phép / không phép / muộn (TT 22/2021); phụ huynh nhận SMS trong ≤30s
  2. **Multi-class gradebook (bộ môn)** — Toán 5 lớp × 42 HS = 210 HS → cấu trúc điểm TX (hệ số 1) + GK (hệ số 2) + CK (hệ số 3) per HK theo TT 22/2021 Đ.7; auto ĐTBmHK formula
  3. **Conduct grade tracking [GVCN]** — track hành vi 42 HS hàng tuần (vi phạm/khen thưởng) → cuối kỳ auto-suggest hạnh kiểm Tốt/Khá/TB/Yếu theo TT 22/2021 → finalize → vào học bạ
  4. **Sổ đầu bài [BỘ MÔN]** — sau mỗi tiết phải ghi sổ đầu bài digital (TT 32/2020) — nội dung dạy, HS vắng, đánh giá tiết; GVCN ký tuần, Phó CM kiểm tháng; nếu paper-only = vi phạm
  5. **Parent communication scope [GVCN]** — chỉ liên lạc 42 phụ huynh lớp chủ nhiệm (privacy + đúng vai trò); họp PH 4 lần/năm với RSVP + biên bản digital
  6. **Background check + vetting** — Lý lịch tư pháp số 2 (Luật Trẻ em 2016 Đ.55) phải upload + verified trước khi dạy; periodic re-check 3 năm
  7. **Exam invigilation roster** — kỳ thi giữa kỳ + cuối kỳ, mỗi GV invigilate 2-4 ca; roster công bằng + không bias; per Quy chế thi MOET
  8. **Salary transparency** — thấy được payslip với bậc lương + phụ cấp + thâm niên + GVCN allowance + (-) BHXH 8% + BHYT 1.5% + BHTN 1% + Công đoàn 1% + thuế TNCN; mẫu C2-04/NS theo Thông tư 79/2022/TT-BTC

---

## 1. Onboarding AC

Initial signup → role assignment → vetting → first usable state.

- [ ] **AC-ONBOARD-001:** Teacher onboarding với MOET role assignment — Hiệu phó/HR cấp account với role variant (GVCN cho 1 lớp / Bộ môn cho 1 môn) + qualification metadata (TT 22/2021 evaluator role)
  - **Test:** Hiệu phó mở "New teacher" wizard → input: tên, email, MST cá nhân, môn dạy (Toán), bằng cấp (Cử nhân Sư phạm Toán), bậc lương (3.00), GVCN assignment (lớp 7A nếu có), quyền invigilate exam → save; cô Hương nhận credential email + first login forces password reset + MFA; dashboard hiển thị 2 modes: "GVCN view (lớp 7A)" + "Bộ môn view (5 lớp Toán)" — toggle giữa 2 mode
  - **Fail signal:** Không có dual-role support (chỉ 1 role); không có MFA; password gửi plaintext; bậc lương không link với MOET payscale; không có GVCN role distinct
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy), GAP-056 (homeroom GVCN)

- [ ] **AC-ONBOARD-002:** Background check upload (Lý lịch tư pháp số 2) — teacher upload + admin verify trước khi go-live; 3-year recheck
  - **Test:** Cô Hương first login → "Compliance documents" prompt: upload (1) Lý lịch tư pháp số 2 (PDF có dấu xác nhận); (2) Giấy khám sức khỏe ≤6 tháng; (3) Bằng cấp + chứng chỉ sư phạm; HR verify trong 5 ngày làm việc → status "Active"; system auto-reminder 3 năm sau cho recheck (per Luật Trẻ em 2016 Đ.55 + TT 32/2020); chưa verified → KHÔNG được dạy lớp / điểm danh
  - **Fail signal:** Không có upload portal cho compliance docs (vi phạm Luật Trẻ em 2016 Đ.55 child safeguarding); không có verification workflow; không có 3-year recheck reminder; teacher chưa vetted vẫn dạy được
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — staff vetting + safeguarding training)

- [ ] **AC-ONBOARD-003:** Class assignment per academic year — Hiệu phó assign GVCN cho 1 lớp + bộ môn cho 5 lớp khi tạo năm học mới; teacher có review window 7 ngày
  - **Test:** Đầu năm học 2026-2027, Hiệu phó assign cô Hương: GVCN 7A (42 HS) + Bộ môn Toán 7B (40 HS) + 7C (40 HS) + 8A (42 HS) + 8B (43 HS) → cô Hương nhận notification + danh sách roster + estimated load (22 periods/week + GVCN duties); review window 7 ngày để raise concerns (vd "Em đang nuôi con nhỏ, giảm load được không?"); sau 7 ngày tự confirm
  - **Fail signal:** Class assignment im lặng (teacher chỉ phát hiện khi xem TKB); không có review window; load estimate sai (vượt 25h/tuần limit per Bộ luật LĐ Đ.105); không có concern-raising channel
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy), GAP-053 (academic year structure)

---

## 2. Daily Operations AC

Recurring workflows — period attendance, multi-class gradebook, sổ đầu bài, conduct, lesson plans (largest category).

- [ ] **AC-OPS-001:** **[GVCN]** Morning roll-call mobile-first ≤2 phút cho 42 HS; SMS auto fires đến phụ huynh vắng/muộn ≤30s
  - **Test:** 7:25 sáng cô Hương đến lớp 7A → mobile app "Today's classes" prominent → tap "Lớp chủ nhiệm 7A — tiết 1 (07:30)" → grid 42 HS với photo thumbnail → tap status (có mặt / vắng có phép / vắng không phép / muộn) → swipe 42 HS trong 90s → submit → SMS auto fires cho parents 3 HS vắng + 1 muộn trong ≤30s; audit log: GVCN ID + timestamp + IP + GPS (verify on-site, optional)
  - **Fail signal:** Phải mark từng row (slow); không phân biệt vắng có phép vs không phép (vi phạm TT 22/2021); SMS chậm >5 phút; không audit log
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-056 (GVCN), GAP-060 (period-based attendance), GAP-063 (SMS/Zalo notification)

- [ ] **AC-OPS-002:** **[BỘ MÔN]** Period-based attendance — sau mỗi tiết Toán dạy, GV bộ môn điểm danh độc lập, hệ thống aggregate cho GVCN cuối ngày
  - **Test:** Cô Hương dạy Toán 7B tiết 2 (08:25) → "Điểm danh tiết 2 — 7B" → hiển thị status tiết 1 (từ GVCN 7B) → bổ sung HS vắng tiết 2 (vd HS đi y tế) → submit; cuối ngày GVCN 7B xem aggregate: HS X vắng tiết 4-5 (Toán + Lý), HS Y muộn tiết 1, HS Z nghỉ cả ngày (≥7 tiết); daily attendance auto-derive
  - **Fail signal:** Single per-day attendance only (như trung tâm); GV bộ môn không điểm danh được; không phân biệt vắng tiết vs vắng cả ngày; không aggregate cho GVCN
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance), Related: `P5-k12-school.md` AC-OPS-002

- [ ] **AC-OPS-003:** **[BỘ MÔN]** Multi-class gradebook với TT 22/2021 formula — Toán 5 lớp (210 HS), cấu trúc điểm TX (hệ số 1) + GK (hệ số 2) + CK (hệ số 3); auto ĐTBmHK + ĐTBmCN
  - **Test:** Cô Hương vào "Gradebook Toán" → 5 tabs (7B, 7C, 8A, 8B, 9A nếu thêm) → vào tab 7B → nhập 4 cột TX + 1 GK + 1 CK cho HK1 cho 40 HS → hệ thống auto-compute ĐTBmHK1 = (TB.TX × 1 + GK × 2 + CK × 3) / 6 theo TT 22/2021 Đ.7 → cuối năm ĐTBmCN = (ĐTBmHK1 + 2 × ĐTBmHK2) / 3 → kết quả hiển thị trên học bạ MOET-format + sổ điểm; cô Hương KHÔNG xem được gradebook môn khác (Văn, Anh, ...)
  - **Fail signal:** Không có TT 22/2021 weighted formula; chỉ trung bình cộng (vi phạm TT 22); teacher xem được gradebook môn khác (privacy + compliance fail); không export học bạ MOET-format
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report card MOET), Related: `P5-k12-school.md` AC-OPS-003

- [ ] **AC-OPS-004:** **[GVCN]** Conduct grade tracking weekly + auto-suggest hạnh kiểm cuối kỳ theo TT 22/2021
  - **Test:** Cô Hương GVCN 7A log incident hàng tuần: "HS Nguyễn Văn A: nghỉ học không phép 3 buổi tuần 5", "HS Trần Thị B: tích cực phát biểu + giúp bạn"; hệ thống tích lũy → cuối HK1 dashboard GVCN: HS A có 5 vi phạm + 0 khen thưởng → suggest "Khá" với lý do; HS B có 0 vi phạm + 8 khen thưởng → suggest "Tốt"; cô Hương review + override (nếu cần) + finalize → đẩy vào học bạ + monthly parent report
  - **Fail signal:** Không có conduct tracker (GVCN tự ghi sổ tay); không có auto-suggest theo TT 22/2021 criteria; finalize manual + không sync học bạ; vi phạm Đ.83 Luật Giáo dục (PH có quyền xem hạnh kiểm)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-056 (GVCN), GAP-055 (official report card MOET)

- [ ] **AC-OPS-005:** **[BỘ MÔN]** Sổ đầu bài (class log book) digital — sau mỗi tiết ghi: nội dung dạy, HS vắng (auto-populate), đánh giá tiết (Tốt/Khá/TB/Yếu); GVCN ký tuần, Phó CM kiểm tháng (TT 32/2020)
  - **Test:** Sau tiết 3 ngày 15/10 cô Hương dạy Toán 7B → "Sổ đầu bài 7B tiết 3 15/10" → form: nội dung "Bài 5: PT bậc nhất 1 ẩn", HS vắng auto-populate từ AC-OPS-002 attendance, đánh giá "Khá" + ghi chú "Lớp tham gia tốt nhưng còn 5 HS chưa hiểu" → submit; cuối tuần GVCN 7B xem 35 tiết của 7B trong tuần (ngẫu nhiên 3-5 môn) → ký digital; Phó CM dashboard tháng 10 thấy báo cáo tổng kết 7B + alert nếu có tiết "Yếu" >2 lần
  - **Fail signal:** Không có sổ đầu bài (vi phạm TT 32/2020 Đ.21 quản lý nhà trường); paper-only (không digital); không có chain ký GV → GVCN → Phó CM; HS vắng không auto-populate
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: TT 32/2020/TT-BGDĐT điều lệ trường THCS-THPT, `P5-k12-school.md` AC-OPS-010

- [ ] **AC-OPS-006:** Schedule view multi-period — teacher xem TKB tuần với 5-10 periods/day across multiple classes; mobile responsive; trip-coverage indicator (dạy lớp nào, phòng nào)
  - **Test:** Cô Hương mở "My schedule week 41" → hiển thị 22 periods: thứ 2 [tiết 1 GVCN 7A, tiết 3 Toán 7B, tiết 5 Toán 8A], thứ 3 [tiết 2 Toán 7C, tiết 4 Toán 8B], ...; mỗi period click → roster + lesson plan + room + sổ đầu bài shortcut; mobile widget "Today: 4 periods, next at 09:25 — Toán 7C phòng B201"
  - **Fail signal:** Schedule rời rạc per-class (phải vào 5 nơi); không mobile responsive; không có "next period" prompt; không indicator GVCN vs bộ môn
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: `P5-k12-school.md` AC-OPS-006 (substitute coverage)

- [ ] **AC-OPS-007:** Lesson plan + homework assignment — teacher tạo lesson plan + assign BTVN cho 1 lớp với deadline; HS thấy task + parent thấy assignment qua portal
  - **Test:** Sau tiết Toán 7B, cô Hương mở "Lesson plans 7B" → ghi nội dung tuần này "Unit 5: PT bậc nhất 1 ẩn" + assign BTVN "Bài 5 trang 23 + bài 7 trang 25, hạn thứ 5 23:59" → 40 HS + parents nhận notification; cô Hương xem dashboard "Homework completion 7B: 35/40 nộp đúng hạn, 5 chưa nộp" + can thiệp bằng SMS GVCN nếu cần
  - **Fail signal:** Không có lesson plan module (Word ngoài); homework assignment không sync với student/parent portal; không track completion %; GVCN không see-through bộ môn assignments
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal), Related: `P5-k12-school.md` AC-OPS-007

- [ ] **AC-OPS-008:** Exam preparation — teacher tạo đề kiểm tra (giữa kỳ + cuối kỳ) per môn; chấm điểm + import vào gradebook; exam version control
  - **Test:** Cô Hương soạn đề Toán giữa kỳ HK1 lớp 7 → upload Word/PDF + key đáp án → Phó CM review + approve → in 200 bản (5 lớp × 40 HS); sau thi → cô Hương chấm 200 bài → input điểm vào gradebook 5 lớp → auto vào ĐTBmHK1 GK column; 3 năm sau audit MOET truy cập đề + key vẫn còn (TT 32/2020 retention)
  - **Fail signal:** Không có exam module (Word + email); chấm điểm không import gradebook (manual entry); đề + key không retain 5 năm (vi phạm); không version control
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: `P5-k12-school.md` AC-OPS-008

- [ ] **AC-OPS-009:** Exam invigilation roster — teacher được assign coi thi (giữa kỳ + cuối kỳ) công bằng + không bias; mobile reminder + sign-in tại điểm coi thi
  - **Test:** Kỳ thi giữa kỳ HK1 (15-19/10) → Phó CM tạo roster cho 50 GV: 4 ca / GV bình quân (200 ca total cho 50 lớp × 4 ca); cô Hương nhận lịch coi thi: 16/10 ca 1 (07:00-09:00) phòng A101 môn Văn lớp 8C, 17/10 ca 2 (09:30-11:30) phòng B201 môn Anh lớp 9A; mobile reminder 15 phút trước; tại phòng coi thi tap "Sign in" + GPS verify; audit log
  - **Fail signal:** Roster manual (Excel + dán); bias (GVCN luôn coi lớp mình → conflict); không reminder; không sign-in verify (latent risk); audit log thiếu
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-010:** **[GVCN]** Class hierarchy view — GVCN see-through 7A: 42 HS, 12 môn, 11 GV bộ môn, attendance week, conduct issues, parent contacts; centralized "lớp chủ nhiệm" workspace
  - **Test:** Cô Hương GVCN 7A → "Lớp chủ nhiệm" workspace → tabs: [Roster 42 HS với photo + parent contact], [Attendance week — heatmap absent], [Grades — multi-subject 12 môn aggregate ĐTBmHK], [Conduct issues — list vi phạm + khen thưởng], [Sổ đầu bài — 35 tiết tuần], [Parent meetings + RSVP], [Documents — biên bản họp PH, decision/discipline]; click HS A drill-down personal page; KHÔNG see-through các lớp khác
  - **Fail signal:** Không có centralized GVCN workspace; phải vào 6 modules riêng; không drill-down HS; GVCN xem được lớp khác (privacy)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-056 (homeroom GVCN)

---

## 3. Financial AC

Fixed salary view, payslip with comprehensive deductions, allowances, annual tax statement.

- [ ] **AC-FIN-001:** Salary scale + allowances dashboard — teacher xem bậc lương MOET + phụ cấp đứng lớp + phụ cấp GVCN (nếu có) + thâm niên; transparent breakdown
  - **Test:** Cô Hương mở "My Salary" → hiển thị: bậc lương 3.00 (theo Nghị định 204/2004/NĐ-CP áp dụng nghề giáo) = 4.475.000 VND + phụ cấp đứng lớp 30% = 1.342.500 VND + phụ cấp GVCN 0.3 hệ số = 447.500 VND + phụ cấp thâm niên 12% (12 năm) = 537.000 VND → tổng gross 6.802.000 VND; next bậc dự kiến 2027-04 (3.33)
  - **Fail signal:** Salary opaque (chỉ thấy net cuối tháng); không hiển thị bậc lương MOET (vi phạm transparency); không track phụ cấp; không tính thâm niên; vi phạm Bộ luật LĐ 2019 Đ.95 (transparency mức lương)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission — extends for K-12 fixed salary)

- [ ] **AC-FIN-002:** Monthly payslip với BHXH 8% + BHYT 1.5% + BHTN 1% + Công đoàn 1% + thuế TNCN bậc thang (mẫu C2-04/NS theo Thông tư 79/2022/TT-BTC)
  - **Test:** Ngày 5/6/2026 cô Hương nhận payslip 5/2026: lương cơ bản 4.475k + đứng lớp 1.342k + GVCN 447k + thâm niên 537k = gross 6.802k; (-) BHXH 8% × mức đóng = 544k; (-) BHYT 1.5% × mức đóng = 102k; (-) BHTN 1% × mức đóng = 68k; (-) Công đoàn 1% = 68k; (-) thuế TNCN bậc thang per Luật Thuế TNCN = 50k; net = 5.970k; bank transfer ref VCB-2026-05-Huong-001; PDF format theo mẫu C2-04/NS có ký số Hiệu trưởng
  - **Fail signal:** Payslip thiếu Công đoàn 1% (K-12 mandate khác trung tâm); gộp deductions; không format mẫu C2-04/NS; không ký số Hiệu trưởng (vi phạm Luật Kế toán 2015)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank integration)

- [ ] **AC-FIN-003:** Annual tax statement (Mẫu 05/QTT-TNCN cho lao động) — cuối năm thuế trường khấu trừ cấp Mẫu 05; teacher có thể download cho tax filing cá nhân
  - **Test:** Tháng 1/2027 cô Hương mở "Tax statements 2026" → Mẫu 05/QTT-TNCN PDF pre-filled với: tên, MST cá nhân, tổng thu nhập 2026 (12 × payslip + bonus Tết + bonus lễ), thuế TNCN đã khấu trừ, BHXH/BHYT/BHTN đã đóng, organization MST trường; download → submit qua portal Tổng cục Thuế (etax.gdt.gov.vn) hoặc kèm hồ sơ thuế cá nhân
  - **Fail signal:** Không có Mẫu 05 export (vi phạm Luật Quản lý Thuế 2019 Đ.42); pre-fill thiếu MST/BHXH; không match format Tổng cục Thuế
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

---

## 4. Communication AC

Stakeholder messaging — student, parent (GVCN-led), peer/dept, admin.

- [ ] **AC-COMM-001:** **[GVCN]** Bulk parent communication scoped to homeroom class — GVCN gửi 42 phụ huynh 7A (không phải 800 toàn trường); via Zalo OA + i18n VN
  - **Test:** Cô Hương GVCN 7A viết "Kính gửi PH HS [TÊN_HS] 7A. Kết quả thi giữa kỳ Toán: [ĐIỂM]. Kính mời PH dự họp 18/10. GVCN cô Hương" → audience auto-scope: parents của 42 HS 7A; render 42 messages cá nhân hóa với template variables (tên, điểm); preview → send qua Zalo OA → 42 deliveries trong ≤2 phút; analytics: 38 read, 4 unread → cô Hương follow-up 4 cases manual; KHÔNG gửi được parents lớp khác
  - **Fail signal:** Audience scope không tự giới hạn (privacy fail — gửi nhầm 800 phụ huynh); không có Zalo integration; không variable substitution; không delivery analytics
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification), Related: `P5-k12-school.md` AC-COMM-002

- [ ] **AC-COMM-002:** **[GVCN]** Parent meeting coordination — họp PH định kỳ 4 lần/năm (đầu năm, giữa kỳ, cuối kỳ, cuối năm); RSVP + biên bản digital + signed (Đ.83 Luật Giáo dục 2019)
  - **Test:** Cô Hương GVCN 7A tạo "Họp PH 7A cuối HK1" 25/12 18:00-20:00 phòng 7A → hệ thống gửi mời 42 PH qua SMS+Zalo+email với calendar.ics attachment → PH RSVP (Tham gia / Không tham gia / Cử người khác) → 35 confirm, 7 absent với lý do; ngày họp cô Hương ghi biên bản digital trên mobile + attendance + photo → upload portal → PH absent vẫn xem được biên bản; biên bản giữ 5 năm (TT 32/2020 retention)
  - **Fail signal:** Không có meeting coordination; GVCN tự gửi giấy mời + collect RSVP bằng tay; biên bản chỉ Word file → khó tìm; không retention 5 năm; vi phạm TT 32/2020 quản lý nhà trường
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal), Related: `P5-k12-school.md` AC-COMM-005

- [ ] **AC-COMM-003:** Student-teacher messaging (platform-mediated, child protection compliant per Luật Trẻ em 2016) — teacher chat với HS qua portal được; conversation log preserved 5 năm; banned từ ngữ blocked
  - **Test:** HS A 7A hỏi cô Hương qua portal "Em chưa hiểu bài hôm nay, cô có thể giải thích lại không?" → cô Hương trả lời + attach ảnh ghi chú; conversation log preserved 5 năm; system auto-flag nếu có từ ngữ inappropriate (banned word filter); teacher KHÔNG share số điện thoại cá nhân hoặc Zalo cá nhân (platform-only); parent có quyền xem (Đ.83 Luật GD)
  - **Fail signal:** Không có 1:1 chat trong platform (teacher dùng Zalo cá nhân — child protection risk); conversation không archive; không banned word filter (latent harassment risk); parent không see-through (vi phạm Đ.83)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal), GAP-186 (child protection)

- [ ] **AC-COMM-004:** Peer + dept (tổ chuyên môn) internal messaging — teacher chat với peer cùng tổ Toán + escalate to Phó CM; channels separated khỏi parent comms
  - **Test:** Cô Hương tạo thread "Tổ Toán" với 7 đồng nghiệp Toán → discuss curriculum + share đề kiểm tra; cô Hương DM Phó CM "Em xin nghỉ ngày 20/5 vì việc gia đình + xin GV thay" → Phó CM respond + assign GV thay; channels rõ ràng tách biệt khỏi parent chat + GVCN chat
  - **Fail signal:** Internal chat trộn với parent chat (privacy); không có DM admin escalation flow; phải dùng Zalo cá nhân (latent risk khi có incident)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 5. Edge Cases AC

Failure scenarios, special situations.

- [ ] **AC-EDGE-001:** Teacher emergency replacement (sick, family) — báo nghỉ same-day; Phó CM phân công GV thay trong ≤30 phút; substitute có quyền access lớp đó CHỈ tiết đó (RBAC time-bound)
  - **Test:** Cô Hương báo nghỉ ốm sáng thứ 3 (8:00) → wizard "Báo nghỉ khẩn" → upload giấy bệnh viện (optional); Phó CM nhận alert → assign cô D (Toán cùng tổ) thay tiết 3 + 5; cô D login thấy được lớp 7B tiết 3 + 8A tiết 5 ngày đó (chỉ tiết đó), điểm danh được, ghi sổ đầu bài được; sau tiết access tự revoke; HS/parent nhận thông báo; cô Hương payroll giảm đúng số tiết nghỉ (per Bộ luật LĐ Đ.115 ốm đau)
  - **Fail signal:** Không có same-day báo nghỉ; substitute access không time-bound (latent security risk); HS/parent không thông báo; payroll không pro-rata; vi phạm Bộ luật LĐ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: `P5-k12-school.md` AC-OPS-006

- [ ] **AC-EDGE-002:** Annual leave (nghỉ phép năm) — 12 ngày phép năm theo Bộ luật LĐ Đ.113; teacher request + approval flow; substitute coverage cho periods affected
  - **Test:** Cô Hương request nghỉ phép 5 ngày (15-19/10) → "Leave request" wizard → hiển thị balance: đã nghỉ 4/12 ngày → còn 8 ngày → request 5 ngày → submit; Phó CM approve trong 3 ngày làm việc → system find substitutes cho tất cả periods 5 ngày × 22 periods/week = ~22 periods total; HS/parent nhận thông báo; payroll tính đầy đủ vì là phép năm có lương
  - **Fail signal:** Không track balance (teacher tự nhớ); không substitute coverage automatic; payroll bị giảm sai (phép năm vẫn pay full); vi phạm Bộ luật LĐ Đ.113
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: AC-EDGE-001

- [ ] **AC-EDGE-003:** Salary dispute — teacher khiếu nại payslip (vd kê thiếu phụ cấp GVCN); workflow + audit log + SLA per Bộ luật LĐ Đ.96
  - **Test:** Cô Hương nhận payslip 5/2026 thấy thiếu phụ cấp GVCN 447k → mở "Dispute payslip" → ghi rõ + attach bằng chứng (giấy phân công GVCN năm 2026-2027); ticket route đến Kế toán + Hiệu phó + Công đoàn CC; SLA 30 ngày làm việc per Bộ luật LĐ Đ.96; Kế toán review → confirm miss → adjustment trong payslip 6/2026 với note "Adjustment for May 2026 — phụ cấp GVCN 7A"; full audit log
  - **Fail signal:** Không có dispute workflow; salary đã pay không adjust được; không SLA → vô tận; không Công đoàn CC (vi phạm Luật Công đoàn 2012)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission)

- [ ] **AC-EDGE-004:** **[GVCN]** Child safety incident reporting — HS có dấu hiệu bị bạo hành/bỏ rơi (Luật Trẻ em 2016 Đ.51 — báo cáo bắt buộc) → GVCN escalate bí mật + audit trail
  - **Test:** Cô Hương GVCN 7A nghi HS Nguyễn Văn A bị bạo hành ở nhà (nhiều bầm tím, hay sợ hãi, không liên lạc được phụ huynh 1 tuần) → mở "Child safety report" (CONFIDENTIAL channel) → ghi observations + timestamp + (optional photos under chain of custody) → escalate đến Hiệu trưởng + counselor + Tổng đài 111 hotline (Cục Trẻ em); phụ huynh KHÔNG được notified về report (per Luật Trẻ em 2016 Đ.51 protection); audit trail immutable; GVCN protected from retaliation per Đ.52
  - **Fail signal:** Không có CONFIDENTIAL reporting channel (GVCN sợ retaliation → không báo); phụ huynh được notified (vi phạm Đ.51 — alert kẻ bạo hành); không escalate Tổng đài 111; audit trail editable; vi phạm Luật Trẻ em 2016 Đ.51-52 + GAP-186
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — staff vetting + safeguarding training)

---

## 6. Exit / Termination AC

Resignation, retirement, settlement, MOET notification.

- [ ] **AC-EXIT-001:** Teacher resignation — 45-day notice cho viên chức (Luật Viên chức 2010) hoặc 30-day cho hợp đồng LĐ (Bộ luật LĐ Đ.35); handover all classes; final salary + severance
  - **Test:** Cô Hương submit resignation ngày 1/6/2026 effective 31/7 (45-day notice cho viên chức biên chế / hoặc 30 ngày nếu hợp đồng) → wizard: (1) handover GVCN 7A — Hiệu phó assign GVCN mới, cô Hương export conduct grades + parent contacts + biên bản họp PH 4 lần đã có; (2) handover bộ môn Toán 5 lớp — assign GV mới với deadline 25/7, cô Hương export lesson plans + grade history per class; (3) final salary tính đến 31/7 + bonus (nếu eligible) + severance theo Bộ luật LĐ Đ.46-48 + (-) final deductions; (4) Mẫu 05/QTT-TNCN export period 1/1-31/7; (5) Lý lịch tư pháp returned (per data retention); (6) account access revoked 31/7 23:59 nhưng historical records preserved 5 năm
  - **Fail signal:** Không có handover wizard (lesson plans + conduct + biên bản mất); severance không tính đúng (vi phạm Bộ luật LĐ); không export final tax statement; account access không revoke đúng cách
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-058 (role hierarchy), Related: `P5-k12-school.md` AC-EXIT-002

- [ ] **AC-EXIT-002:** Teacher retirement (về hưu nghề giáo) — 60 nam / 55 nữ (Luật Lao động 2019 Đ.169 + Luật BHXH 2014); pension calculation + emeritus access + reference letter; MOET notification
  - **Test:** Cô Hương đến tuổi nghỉ hưu (55 nữ) ngày 30/6/2030 → wizard "Retirement" 6 tháng trước (1/1/2030) → (1) calculate pension theo Luật BHXH 2014: BHXH đóng 30 năm × tỷ lệ 75% × mức lương bình quân 5 năm cuối; (2) handover all classes (GVCN + bộ môn) sang đồng nghiệp với 6-month overlap cho mentoring; (3) reference letter từ Hiệu trưởng PDF + ký số; (4) MOET Sở GD-ĐT notification cho danh sách hưu trí; (5) emeritus access (read-only) cho 1 năm để truy lục historical records (học bạ HS đã dạy, etc.); (6) BHXH transfer to pension provider; (7) records preserved 5 năm + archive lifetime cho tham khảo
  - **Fail signal:** Không có retirement workflow; pension calculation manual (vi phạm Luật BHXH); không MOET notification; không emeritus access (teacher cần truy cập records để verify cho HS cũ xin học bạ sau này)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), Related: `P5-k12-school.md` AC-EXIT-002

---

## Scoring

**Total ACs:** 26 (3 Onboarding + 10 Daily Ops + 3 Financial + 4 Communication + 4 Edge Cases + 2 Exit) — markers `[GVCN]` apply to homeroom variant only, `[BỘ MÔN]` apply to subject variant only, unmarked apply to both

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
| AC-ONBOARD-001 | TBD | GAP-058, GAP-056 | 🔵 OPEN | P1 |
| AC-ONBOARD-002 | TBD | GAP-186 | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | GAP-058, GAP-053 | 🔵 OPEN | P1 |
| AC-OPS-001 | TBD | GAP-056, GAP-060, GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-002 | TBD | GAP-060 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-056, GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-007 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-OPS-010 | TBD | GAP-056 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-FIN-002 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1, P2 |
| AC-FIN-003 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-COMM-003 | TBD | GAP-052, GAP-186 | 🔵 OPEN | P0 |
| AC-EDGE-003 | TBD | GAP-057 | 🔵 OPEN | P1 |
| AC-EDGE-004 | TBD | GAP-186 | 🔵 OPEN | P0 |
| AC-EXIT-001 | TBD | GAP-057, GAP-058 | 🔵 OPEN | P1 |
| AC-EXIT-002 | TBD | GAP-057 | 🔵 OPEN | P1 |

**Candidate NEW gaps to file at review time** (state-check qua `audit-to-gap-pipeline.md` Step 2.5 trước khi filing):
- Sổ đầu bài digital (AC-OPS-005) — likely no current gap, mandate per TT 32/2020
- Conduct grade auto-suggest engine (AC-OPS-004) — extends GAP-056
- Multi-class gradebook với TT 22/2021 weighted formula (AC-OPS-003) — extends GAP-055
- Teacher dual-role support (GVCN + Bộ môn) (AC-ONBOARD-001) — extends GAP-058
- Background check / Lý lịch tư pháp upload + verify + 3-year recheck (AC-ONBOARD-002) — extends GAP-186
- Class assignment review window (AC-ONBOARD-003) — extends GAP-058
- Exam preparation module + version control + 5-year retention (AC-OPS-008) — likely no current gap
- Exam invigilation roster + bias prevention + GPS sign-in (AC-OPS-009) — likely no current gap
- Salary scale (Nghị định 204/2004) + allowances dashboard (AC-FIN-001) — extends GAP-057 cho K-12 fixed salary
- Mẫu 05/QTT-TNCN annual export (AC-FIN-003) — extends GAP-057
- Annual leave balance + automatic substitute (AC-EDGE-002) — likely no current gap
- Salary dispute với Công đoàn CC + 30-day SLA (AC-EDGE-003) — extends GAP-057
- Child safety CONFIDENTIAL reporting channel + Tổng đài 111 (AC-EDGE-004) — extends GAP-186 (CRITICAL — Luật Trẻ em 2016)
- Teacher retirement workflow + pension calculation + MOET notification (AC-EXIT-002) — likely no current gap

---

## Cross-References

- **Sibling tenant AC:** [`../P5-k12-school.md`](../P5-k12-school.md) — admin-side workflows + tenant compliance (MOET reporting, exam scheduling)
- **Sibling secondary AC:** `student-in-P5.md`, `parent-in-P5.md`, `admin-in-P5.md` (Wave Secondary-Persona-AC parallel agents)
- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas — Teacher"
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md) v1.2+
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (tenant AC + template)
- **Secondary AC execution gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (this Phase 1)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check

### Linked feature gaps (cross-link for review traceability)
- [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — Parent portal (CRITICAL: GVCN parent communication + meeting + biên bản)
- [GAP-053](../../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) — Academic year (RELEVANT: HK1/HK2 boundary cho conduct + grades)
- [GAP-054](../../../04-quality/gaps/GAP-054-multi-subject-per-student.md) — Multi-subject (RELEVANT: bộ môn dạy multiple subjects across classes)
- [GAP-055](../../../04-quality/gaps/GAP-055-official-report-card-moet.md) — Official report card MOET (CRITICAL: TT 22/2021 weighted formula + GVCN finalize hạnh kiểm)
- [GAP-056](../../../04-quality/gaps/GAP-056-homeroom-gvcn.md) — Homeroom GVCN (CRITICAL: dedicated GVCN workflows + conduct + parent contact)
- [GAP-057](../../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) — Payroll (CRITICAL: extends to fixed-salary K-12 model + Mẫu 05)
- [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md) — Role hierarchy (CRITICAL: dual-role GVCN + bộ môn)
- [GAP-060](../../../04-quality/gaps/GAP-060-period-attendance.md) — Period attendance (CRITICAL: per-period roll-call + aggregate)
- [GAP-062](../../../04-quality/gaps/GAP-062-teacher-payroll-bank-integration.md) — Payroll bank integration (RELEVANT: Vietcombank/BIDV monthly batch)
- [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) — SMS/Zalo notification (CRITICAL: GVCN parent comms scoped + delivery analytics)
- [GAP-064](../../../04-quality/gaps/GAP-064-scorm-xapi-compliance.md) — SCORM/xAPI (PARTIAL: depends on subject; basic curriculum tracking enough)
- [GAP-180](../../../04-quality/gaps/GAP-180-terms-of-service.md) — TOS (RELEVANT: K-12 contract + child protection clauses)
- [GAP-186](../../../04-quality/gaps/GAP-186-child-protection-luat-tre-em.md) — Child protection (CRITICAL: Lý lịch tư pháp + safeguarding training + CONFIDENTIAL reporting per Luật Trẻ em 2016 Đ.51-55)

---

## Log

- **2026-04-30** — Initial AC set v1 (author: Agent C, Wave Secondary-Persona-AC, GAP-153 Phase 1). 26 ACs across 6 categories with explicit GVCN/Bộ môn split markers. Highlights: dual-role support (1 teacher = GVCN của 1 lớp + Bộ môn của 5 lớp), GVCN morning roll-call ≤2 min mobile, multi-class gradebook với TT 22/2021 weighted formula, sổ đầu bài digital per TT 32/2020, conduct grade auto-suggest theo TT 22/2021, child safety CONFIDENTIAL reporting per Luật Trẻ em 2016 Đ.51-52, full payslip với Công đoàn 1% + Mẫu 05/QTT-TNCN annual, retirement workflow với pension calculation per Luật BHXH 2014. 13 cross-links to existing feature gaps + 14 candidate NEW gaps surfaced. Legal/MOET citations: TT 22/2021 evaluation (Đ.7 weighted formula), TT 32/2020 management (Đ.21 sổ đầu bài, Đ.40 retention), Luật Giáo dục 2019 (Đ.83 parent rights), Luật Trẻ em 2016 (Đ.51-55 child protection + reporting), Bộ luật Lao động 2019 (Đ.35 notice + Đ.46-48 severance + Đ.95 transparency + Đ.105 working hours + Đ.113 annual leave + Đ.115 sick leave + Đ.169 retirement age), Luật BHXH 2014 (pension), Luật Viên chức 2010 (45-day notice), Nghị định 204/2004/NĐ-CP (salary scale nghề giáo), Luật Công đoàn 2012, Thông tư 79/2022/TT-BTC (mẫu C2-04/NS payslip).
- **TBD** — GAP-152 Round 1 review by domain expert (real GVCN + Bộ môn teacher Hà Nội) + Hiệu phó + MOET education expert + Product Owner sign-off; status updates fill PASS/PARTIAL/FAIL.
