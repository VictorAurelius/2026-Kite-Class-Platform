# Acceptance Criteria — P5 Public/Private K-12 School (Trường tiểu học / THCS / THPT)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** P5
**Persona name (VN):** Trường tiểu học / THCS / THPT công lập + dân lập
**Persona name (EN):** Public/Private K-12 School
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent D (Wave Persona-AC-Template, GAP-151 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real school principal/vice-principal + GVCN representative + MOET education domain expert + Legal counsel (child protection) + Product Owner (deferred to GAP-152 Round 1 — multi-stakeholder review)
**Tier:** 1 Primary (USER PRIORITY)
**Tracking:** GAP-151 Phase 1 → GAP-152 → GAP-153 (secondary personas: Student/Parent/GVCN/Bộ môn teacher/Admin within K-12 tenant context)
**Strategic priority:** USER's example persona — blocks K-12 deployment without comprehensive coverage
**Legal compliance:** MOET (Thông tư 22/2021, TT 32/2020), Luật Giáo dục 2019, Luật Trẻ em 2016, PDPL Decree 13/2023 Art 16

---

## 0. Context

### Scale assumption (from `personas-catalog.md`)
- **Users:** 50+ giáo viên (5 GVCN + 45 bộ môn typical THCS), 500–3000 học sinh (cấp 1: ~500 / cấp 2: ~800 / cấp 3: ~1500), 10–30 staff, **PARENTS: 1–2 phụ huynh / học sinh** → 800–6000 phụ huynh total
- **Data volume:** 30+ classes (lớp), 12–15 môn học per cấp 2, 5–10 tiết / ngày × 5 ngày × 35 tuần = **~1,250–1,750 tiết-điểm-danh / năm / lớp**, ~10,000+ điểm số / kỳ, 2 kỳ / năm
- **Usage pattern:**
  - **Daily peak:** GVCN điểm danh đầu giờ (07:00–08:00) — concurrent ~30 GVCN cho 30 lớp
  - **Weekly peak:** Thứ Sáu — bộ môn nhập điểm cuối tuần
  - **Monthly peak:** Cuối tháng — báo cáo hạnh kiểm + thông báo phụ huynh
  - **Semester peak:** Cuối học kỳ I (cuối tháng 12) + cuối học kỳ II (cuối tháng 5) — chốt điểm + in học bạ + báo cáo MOET
  - **Annual peak:** Tháng 8 (chuẩn bị năm học mới) — bulk import students + thay đổi lớp + provision parent accounts

### Organization archetype
- **Type:** Public K-12 (công lập) — quản lý theo phòng/sở GD&ĐT — OR Private K-12 (dân lập, tư thục) — đăng ký kinh doanh + giấy phép MOET
- **Hierarchy:** Hiệu trưởng (Principal) → Phó hiệu trưởng (Vice Principal — typically 2: Phó CMUE phụ trách chuyên môn + Phó CSVCH phụ trách cơ sở vật chất) → Tổ trưởng chuyên môn (Department Head — Toán, Ngữ Văn, Anh, …) → GVCN (Homeroom teacher) + Giáo viên bộ môn (Subject teacher) → Học sinh + **Phụ huynh** + Staff (kế toán, y tế học đường, bảo vệ, lao công, văn thư)
- **Decision-making:**
  - **Hiệu trưởng** — sign báo cáo MOET, học bạ chính thức, hợp đồng giáo viên, quyết định lên lớp / ở lại lớp
  - **Phó CM** — duyệt lịch dạy, phân công lớp/môn, tổ chức thi
  - **Tổ trưởng** — duyệt giáo án, thống nhất tiêu chí chấm điểm theo bộ môn
  - **GVCN** — chịu trách nhiệm hạnh kiểm + liên lạc phụ huynh + điểm danh hằng ngày
  - **Kế toán** — thu học phí, bán trú, đồng phục, BHYT, lập báo cáo tài chính
  - **Văn thư** — lưu trữ học bạ, phát giấy chứng nhận, xử lý hồ sơ chuyển trường

### Revenue tier mapping
- **Expected tier:** ENTERPRISE only
- **Reason:**
  - Quy mô (500+ HS, 50+ GV) vượt PREMIUM
  - Yêu cầu **MOET compliance** (báo cáo định kỳ, format chuẩn) — không có ở tier dưới
  - Yêu cầu **parent portal LEGAL MANDATE** (Luật Giáo dục 2019 Đ.83 — quyền phụ huynh giám sát) — không thể optional
  - **Background check + staff vetting** cho K-12 (Luật Trẻ em 2016) — chỉ ENTERPRISE có
  - SLA cao (99.9% uptime kỳ thi) + **5-year data retention** (Quy chế quản lý hồ sơ HS — TT 32/2020)
  - Multi-tenant hierarchy nâng cao (departments, vice-principal scopes)
  - **AC-FIN-002** dưới đây phân biệt rõ public school (low fee + scholarship-heavy) vs private school (flexible pricing) — cùng tier ENTERPRISE nhưng khác config

### Real-world reviewer profile
- **Acting role:** "Hiệu trưởng trường THCS công lập 800 học sinh tại quận trung tâm Hà Nội, 50 giáo viên (5 GVCN + 45 bộ môn), 15 admin/staff, theo MOET regulations Thông tư 22/2021"
- **Critical concerns (top 7):**
  1. **MOET reporting compliance** — báo cáo PCGD + học bạ + sổ điểm phải đúng format Thông tư 22/2021/TT-BGDĐT (đánh giá HS) + TT 32/2020 (quản lý nhà trường); sai 1 trường = bị phòng GD trả về
  2. **GVCN workflow daily** — điểm danh đầu giờ + theo dõi hạnh kiểm hàng tuần + liên lạc phụ huynh hàng tháng — nếu UX kém GVCN sẽ chuyển về Excel
  3. **Parent portal** — phụ huynh có **quyền pháp lý** xem học bạ, điểm danh, học phí; thiếu = vi phạm Luật Giáo dục 2019
  4. **Period-based attendance** — 5–10 tiết / ngày, mỗi tiết 1 GV bộ môn khác → KHÔNG phải single per-day attendance như trung tâm
  5. **Conduct grade (hạnh kiểm)** — Tốt / Khá / TB / Yếu mỗi học kỳ, ảnh hưởng đến lên lớp; quy trình MOET-mandated
  6. **Child protection** — parental consent <16, mandatory reporting (GAP-186), staff vetting (Luật Trẻ em 2016 Đ.25, Decree 56/2017)
  7. **Backup data ownership + 5-year retention** — học bạ phải lưu 5 năm sau khi HS tốt nghiệp (TT 32/2020) + exportable on-demand

---

## 1. Onboarding AC

Initial provisioning, MOET registration verification, multi-grade tenant setup, bulk staff vetting, parent account bulk creation tied to student accounts.

- [ ] **AC-ONBOARD-001:** Trường có thể hoàn tất tenant provisioning với multi-tenant hierarchy (Hiệu trưởng + 2 Phó HT + 5 Tổ trưởng + 50 GV + 15 staff) trong ≤8 giờ làm việc (1 ngày)
  - **Test:** Hiệu trưởng đăng ký tenant ENTERPRISE → upload danh sách 65 staff (xlsx mẫu MOET) → hệ thống tạo accounts + role hierarchy → gửi email/SMS credentials → role permissions hoạt động đúng (Phó CM thấy được lịch dạy, Tổ trưởng thấy giáo án bộ môn)
  - **Fail signal:** Không có bulk-import staff với role assignment, hoặc role hierarchy phẳng (chỉ admin/teacher/student không có GVCN/Phó HT/Tổ trưởng), hoặc credentials không tự động phân phối, hoặc setup mất >2 ngày
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (xlsx import), GAP-058 (role hierarchy)

- [ ] **AC-ONBOARD-002:** Trường có thể bulk import 800 học sinh + 1200–1600 phụ huynh (1–2 / HS) trong ≤4 giờ với link parent ↔ student tự động tạo
  - **Test:** Upload xlsx mẫu (cột: Mã HS, Tên, DOB, Lớp, Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ, Địa chỉ) → hệ thống tạo 800 student accounts + ~1500 parent accounts + auto-link relationships → gửi credentials qua SMS/Zalo (cho parents) + email/in giấy (cho students <16) → kiểm tra 1 phụ huynh login thấy đủ child(ren) của mình
  - **Fail signal:** Phải tạo parent thủ công, hoặc không có cột parent trong xlsx, hoặc duplicate parent khi sibling cùng trường, hoặc credentials không gửi được qua Zalo (kênh phổ biến nhất ở VN)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (xlsx import), GAP-052 (parent portal), GAP-063 (SMS/Zalo)

- [ ] **AC-ONBOARD-003:** Hệ thống verify giấy phép MOET (mã số trường + giấy phép thành lập) trước khi enable tenant K-12
  - **Test:** Khi đăng ký tenant tier=K12_ENTERPRISE → form bắt buộc upload: (1) mã số trường MOET, (2) giấy phép thành lập / quyết định công nhận, (3) thông tin Hiệu trưởng (CCCD + chứng chỉ quản lý) → hệ thống lưu evidence → admin Kite verify → tenant chuyển từ TRIAL → ACTIVE
  - **Fail signal:** Không có verification step, ai cũng đăng ký được dưới danh nghĩa "trường" mà không cần giấy phép, hoặc evidence không được lưu để audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy — principal verification), Related: child-protection-policy.md §6 staff vetting

- [ ] **AC-ONBOARD-004:** Multi-grade level setup hỗ trợ cả cấp 1 (1–5), cấp 2 (6–9), cấp 3 (10–12), hoặc liên cấp (1–12) với academic year + semester structure đúng MOET
  - **Test:** Trong wizard onboarding chọn cấp học (THCS) → hệ thống auto-tạo: 4 khối (6, 7, 8, 9) × N lớp / khối, 2 học kỳ / năm (HK1: 8/9 → 31/12, HK2: 6/1 → 31/5), VN public holidays (Tết, 30/4, 1/5, 2/9, 20/11), 35 tuần học chuẩn → admin có thể adjust lịch khối/lớp
  - **Fail signal:** Phải tự cấu hình từng lớp / từng tuần, hoặc không có VN public holiday calendar, hoặc không phân biệt khối / lớp / tổ chuyên môn, hoặc không support liên cấp (1+2 hoặc 2+3)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-053 (academic year/semester structure)

- [ ] **AC-ONBOARD-005:** Bulk staff vetting workflow hoàn tất ≤7 ngày cho 50 giáo viên — upload CCCD + bằng cấp + lý lịch tư pháp (LLTP) + ảnh chân dung; hệ thống track audit log
  - **Test:** HR/admin upload bulk staff records (xlsx + zip files cho mỗi GV gồm: CCCD scan, bằng tốt nghiệp sư phạm, LLTP số 2 — không quá 6 tháng, ảnh 3×4) → hệ thống lưu vào MinIO encrypted → tạo task queue cho admin Kite verify từng GV → khi đủ verify-pass GV mới có quyền access học sinh → audit log ghi lại từng bước
  - **Fail signal:** Không có vetting workflow (vi phạm Luật Trẻ em 2016 Đ.25 + Decree 56/2017), hoặc evidence lưu plaintext, hoặc admin không thể track GV nào chưa verify, hoặc verify time >2 tuần
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — staff vetting), Related: child-protection-policy.md §6

- [ ] **AC-ONBOARD-006:** Tenant inherit MOET-compliant subject taxonomy + subject-class-grade mapping đúng chương trình giáo dục
  - **Test:** Sau setup khối 6 → hệ thống auto-link 13 môn THCS (Toán, Ngữ Văn, Tiếng Anh, KHTN, KHXH, Lịch Sử, Địa Lý, GDCD, Tin học, Công Nghệ, Thể Dục, Âm Nhạc, Mỹ Thuật) với số tiết / tuần / môn theo Thông tư 32/2018 (chương trình GDPT 2018) → admin có thể edit nếu trường có môn tự chọn / CLB
  - **Fail signal:** Không có subject taxonomy MOET, phải tự nhập 13 môn / khối, hoặc số tiết / môn / tuần không phù hợp chương trình GDPT 2018
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject per student), Related: TT 32/2018 (chương trình GDPT)

---

## 2. Daily Operations AC

Period-based attendance, GVCN daily workflow, multi-subject gradebook, homework, exam workflow, conduct grade weekly, substitute teacher, classroom resource, exam invigilation roster.

- [ ] **AC-OPS-001:** GVCN có thể điểm danh đầu giờ (tiết 1) cho 1 lớp 40–45 HS trong ≤2 phút trên mobile, phân biệt: có mặt / vắng có phép / vắng không phép / muộn
  - **Test:** GVCN mở app → chọn lớp 7A → tiết 1 (07:30) → list 42 HS với photo thumbnail → bấm 1 tap / HS để toggle status → submit → SMS auto gửi phụ huynh các HS vắng / muộn trong ≤30s → audit log ghi GVCN ID + timestamp + IP
  - **Fail signal:** UI trên mobile không tap-friendly, mất >5 phút / lớp, hoặc không có 4 trạng thái phân biệt (chỉ có/vắng), hoặc không tự động notify phụ huynh, hoặc audit log thiếu thông tin
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-056 (GVCN), GAP-060 (period-based attendance), GAP-063 (SMS/Zalo notify)

- [ ] **AC-OPS-002:** Period-based attendance — mỗi tiết / ngày (5–10 tiết) GV bộ môn có thể điểm danh độc lập, hệ thống tổng hợp daily attendance auto cho GVCN
  - **Test:** GV Toán dạy 7A tiết 2 (08:25) → điểm danh thấy tiết 1 đã có sẵn (status từ GVCN), bổ sung HS vắng tiết 2 (vd HS đi y tế) → submit → cuối ngày GVCN xem report tổng hợp 7A: HS A vắng tiết 4–5, HS B muộn tiết 1, HS C nghỉ cả ngày → daily attendance auto-derive (vắng cả ngày = vắng 7+ tiết)
  - **Fail signal:** Single per-day attendance only (như trung tâm), GV bộ môn không điểm danh được, hoặc không phân biệt vắng tiết vs vắng cả ngày, hoặc không có aggregation cho GVCN
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance — CRITICAL)

- [ ] **AC-OPS-003:** Gradebook hỗ trợ 12–15 môn / HS với cấu trúc điểm theo TT 22/2021 — điểm thường xuyên (TX), điểm giữa kỳ (GK), điểm cuối kỳ (CK) per kỳ — auto-compute điểm trung bình môn (ĐTBmôn) theo công thức MOET
  - **Test:** GV Toán nhập 4 cột điểm TX (hệ số 1) + 1 GK (hệ số 2) + 1 CK (hệ số 3) cho 7A HK1 → hệ thống auto-compute ĐTBmHK = (TB.TX + GK×2 + CK×3) / 6 (theo TT 22/2021 Đ.7) → cuối năm ĐTBmCN = (ĐTBmHK1 + 2×ĐTBmHK2) / 3 → kết quả hiển thị trên học bạ + sổ điểm
  - **Fail signal:** Công thức ĐTBm sai, không có điểm hệ số, không phân biệt TX/GK/CK, hoặc không support 12+ môn / HS, hoặc không export ra format MOET-compliant
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject), GAP-055 (báo cáo học bạ MOET)

- [ ] **AC-OPS-004:** Conduct grade (hạnh kiểm) — GVCN có thể track hành vi HS hàng tuần (vi phạm nội quy / khen thưởng / hoạt động tích cực) → cuối kỳ auto-suggest hạnh kiểm Tốt/Khá/TB/Yếu theo TT 22/2021
  - **Test:** GVCN log incident "HS Nguyễn Văn A: nghỉ học không phép 3 buổi tuần 5" → hệ thống tích lũy → cuối HK1 dashboard cho GVCN: HS A có 5 vi phạm + 2 khen thưởng → suggest hạnh kiểm "Khá" với lý do; GVCN review + override nếu cần → finalize → đẩy vào học bạ + báo cáo phụ huynh
  - **Fail signal:** Không có conduct tracking, hoặc chỉ là free-text comment, hoặc không có 4 mức Tốt/Khá/TB/Yếu, hoặc không track lịch sử để audit khi có khiếu nại từ phụ huynh
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-059 (student conduct tracking)

- [ ] **AC-OPS-005:** Mid-term + final exam workflow — Phó CM tạo lịch thi → admin in giấy mời phụ huynh + danh sách phòng thi → GV chấm điểm + nhập điểm trong window 7 ngày → Tổ trưởng duyệt → Hiệu trưởng ký thông báo → kết quả gửi phụ huynh + lưu sổ điểm
  - **Test:** Phó CM tạo lịch thi cuối HK1 (15/12 → 25/12) cho cả khối 7 → hệ thống tạo phòng thi (3 HS / phòng phân theo SBD MOET-style), generate giấy mời phụ huynh PDF → GV chấm xong nhập điểm → workflow approval Tổ trưởng → Hiệu trưởng → publish → phụ huynh nhận notification + xem điểm con qua portal trong ≤24h sau publish
  - **Fail signal:** Không có exam workflow tách biệt khỏi điểm thường, hoặc không có approval chain, hoặc không có publish window control (điểm rò rỉ trước duyệt), hoặc không generate được giấy mời phụ huynh format MOET
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report), GAP-058 (role hierarchy)

- [ ] **AC-OPS-006:** Substitute teacher per-period — khi GV bộ môn báo nghỉ, Phó CM phân công GV thay thế trong ≤30 phút, GV thay có quyền access lớp đó CHỈ trong tiết đó (RBAC time-bound)
  - **Test:** GV Toán 7A báo nghỉ tiết 3 ngày 15/10 (1 ngày trước) → Phó CM thấy alert → assign GV thay (cô B dạy Toán lớp khác) → cô B login thấy được lớp 7A tiết 3 ngày 15/10 (chỉ tiết đó), điểm danh được, ghi sổ đầu bài được; sau tiết 3 access tự revoke; audit log ghi rõ
  - **Fail signal:** Không có substitute workflow, hoặc cô B nhận quyền vĩnh viễn lớp 7A, hoặc không có audit log substitution, hoặc Phó CM phải tự gọi điện không có hệ thống hỗ trợ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + RBAC time-bound)

- [ ] **AC-OPS-007:** Classroom resource scheduling — phòng máy tính / phòng thí nghiệm / phòng năng khiếu / sân thể dục có thể đặt lịch theo tiết, conflict detection auto, ai sử dụng được audit
  - **Test:** GV Tin báo phòng máy tính tiết 4 ngày 20/10 cho lớp 8A → hệ thống check conflict (có lớp 7B đăng ký rồi) → từ chối với gợi ý slot trống; GV chuyển sang tiết 5 → confirm; ngày 20/10 báo cáo "phòng máy 1 — sử dụng: 7B tiết 1, 8A tiết 5, 9C tiết 7"
  - **Fail signal:** Không có resource scheduling (GV phải tranh nhau qua group chat), hoặc không có conflict detection, hoặc không có audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: GAP-053 (calendar) extension; new sub-feature

- [ ] **AC-OPS-008:** Exam invigilation roster — Phó CM tạo bảng phân công coi thi cho mỗi đợt thi, mỗi GV coi thi 2–3 buổi / kỳ, không trùng lịch dạy của chính GV đó
  - **Test:** Đợt thi cuối HK1 (15→25/12, 8 buổi) × 12 phòng = 96 slot coi thi; Phó CM upload roster → hệ thống detect conflict (cô C có lịch dạy 9A tiết 2 ngày 18/12 nhưng được phân coi thi tiết 2 ngày 18/12) → auto-flag conflict + gợi ý GV thay; GV nhận thông báo lịch coi thi qua app
  - **Fail signal:** Không có invigilation roster, Phó CM phải làm thủ công Excel + dán bản tin, hoặc không detect lịch trùng → GV bỏ thi đột xuất
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: GAP-058 (role hierarchy)

- [ ] **AC-OPS-009:** Homework assignment + tracking — GV bộ môn assign bài tập cho lớp 7A môn Toán hạn nộp 3 ngày, system track HS đã nộp / chưa nộp, late penalty config theo trường, parent thấy được con đã/chưa làm bài
  - **Test:** GV Toán assign "BTVN bài 5 trang 23" cho 7A, hạn 17/10 23:59 → 42 HS thấy task; HS A nộp lúc 16/10 → status "On time"; HS B nộp 18/10 (trễ 1 ngày) → status "Late" auto-flag; GVCN thấy 5 HS chưa nộp → can thiệp; phụ huynh thấy con mình "BTVN Toán: nộp đúng hạn" trên portal
  - **Fail signal:** Không có homework module, hoặc không phân biệt on-time / late / missing, hoặc parent không visibility, hoặc không tích hợp file upload (HS phải email cho GV)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: GAP-052 (parent portal), GAP-054 (multi-subject)

- [ ] **AC-OPS-010:** Sổ đầu bài (class log book) digital — mỗi tiết GV bộ môn phải ghi: nội dung dạy, HS vắng, đánh giá tiết học (Tốt/Khá/TB/Yếu); GVCN ký xác nhận hàng tuần; Phó CM kiểm tra hàng tháng
  - **Test:** Sau tiết 3 ngày 15/10, GV Toán click "Sổ đầu bài 7A tiết 3" → form: nội dung "Bài 5: PT bậc nhất 1 ẩn", HS vắng auto-populate từ điểm danh, đánh giá "Khá" + ghi chú "Lớp tham gia tốt nhưng còn 5 HS chưa hiểu" → submit; cuối tuần GVCN xem 35 tiết của 7A trong tuần → ký digital → Phó CM dashboard tháng 10 thấy báo cáo tổng kết
  - **Fail signal:** Không có sổ đầu bài (vi phạm TT 32/2020 quản lý nhà trường), hoặc paper-only, hoặc không có chain ký GV → GVCN → Phó CM
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: TT 32/2020/TT-BGDĐT điều lệ trường THCS-THPT

---

## 3. Financial / Admin AC

Multi-fee structure, public school fee compliance, private school flexible pricing, MOET financial reporting, parent payment reminder.

- [ ] **AC-FIN-001:** Multi-fee structure — học phí chính + bán trú + đồng phục + bảo hiểm Y tế + bảo hiểm Tai nạn + quỹ phụ huynh, mỗi fee có scheduling riêng (monthly / semester / annual / one-time), discount rules theo HS chính sách
  - **Test:** Admin tạo fee structure năm 2026–2027 cho 7A: HP 300k/tháng × 9 tháng, bán trú 25k/buổi × actual days, đồng phục 800k/năm one-time, BHYT 950k/năm, BHTN 100k/năm, quỹ PH 500k/năm; HS A là con thương binh → discount HP 50% + miễn quỹ PH; system generate invoice từng HS từng tháng đúng schedule
  - **Fail signal:** Single-fee model, hoặc không support discount rules theo policy HS, hoặc không phân biệt one-time / recurring, hoặc không calculate đúng bán trú theo actual attendance
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md, billing-terms.md

- [ ] **AC-FIN-002:** Public school fee compliance vs Private school flexible pricing — system phân biệt 2 mode trong cùng tier ENTERPRISE
  - **Test:** Public school: fee structure phải import được từ quyết định UBND tỉnh/thành (vd HP THCS công lập Hà Nội: 0–155k / tháng theo Nghị quyết HĐND), không cho admin tự đặt cao hơn quy định, scholarship-heavy (>30% HS được giảm/miễn); Private school: admin tự đặt fee, support tiered pricing (gói cao cấp / chuẩn / cơ bản), early-bird discount, sibling discount
  - **Fail signal:** Cùng UI cho cả 2 loại, hoặc public school không có cap, hoặc private school không có flexible pricing, hoặc không có audit khi public school đặt fee >quy định
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md, child-protection-policy.md (scholarship cho HS khó khăn)

- [ ] **AC-FIN-003:** Parent payment reminder — phụ huynh nhận reminder qua SMS + Zalo + email + push notification 7 ngày + 3 ngày + 1 ngày trước due date; sau due date tăng tần suất; quá 30 ngày escalate Hiệu trưởng
  - **Test:** HP tháng 10 due 5/10 cho HS B → phụ huynh nhận: 28/9 (T-7) reminder gentle, 2/10 (T-3) reminder polite, 4/10 (T-1) reminder urgent, 6/10 (D+1) overdue, 12/10 (D+7) escalate to GVCN, 5/11 (D+30) escalate Hiệu trưởng + alert legal action; tất cả channels (SMS+Zalo+email+app) đều gửi với i18n VN tiếng Việt
  - **Fail signal:** Email-only reminder (phụ huynh không check email), hoặc không có escalation chain, hoặc không có grace period config theo trường, hoặc không tích hợp Zalo (kênh phổ biến nhất ở VN)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-FIN-004:** MOET financial reporting — quý / năm export báo cáo tài chính theo format Bộ Tài Chính + Bộ GD&ĐT (số HS, tổng thu, tổng chi, ngân sách nhà nước cho công lập, học phí thu, miễn giảm, scholarship)
  - **Test:** Admin chạy report Q4/2026 → export Excel theo template Thông tư 107/2017/TT-BTC (kế toán hành chính sự nghiệp với công lập) hoặc TT 200/2014 (DN với private) → đầy đủ fields + đúng công thức + Hiệu trưởng + Kế toán trưởng ký electronic signature → submit qua hệ thống MOET online (hoặc print PDF gửi phòng GD)
  - **Fail signal:** Không export được format MOET, hoặc thiếu fields bắt buộc, hoặc số liệu sai (vd quên miễn giảm), hoặc không support electronic signature → vi phạm hệ thống chính phủ điện tử
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: TT 107/2017, TT 200/2014; GAP-051 (export)

- [ ] **AC-FIN-005:** Payroll cho 50 GV + 15 staff — bank integration với Vietcombank/BIDV/Techcombank, tự tính lương (lương cơ bản + phụ cấp đứng lớp + phụ cấp GVCN + thâm niên + ngoài giờ), trừ thuế TNCN + BHXH + công đoàn, ngày 5 hàng tháng auto-transfer
  - **Test:** Admin chạy payroll tháng 10 → system aggregate: cô A GV Toán: lương cơ bản 7.2tr + thâm niên 30% + đứng lớp 30% + ngoài giờ 5 tiết × 60k = 9.86tr → trừ BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc 1 = 8.95tr net → upload file MT940/CSV vào Vietcombank cổng doanh nghiệp; ngày 5/11 auto-transfer 65 records; audit log lưu evidence
  - **Fail signal:** Tính lương thủ công Excel, hoặc không support phụ cấp đặc thù GV, hoặc không integrate với bank (admin phải upload từng tháng), hoặc không có file format chuẩn ngân hàng
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-062 (payroll bank integration)

---

## 4. Communication AC (CRITICAL for K-12)

Parent portal access, bulk parent notification, urgent alert, monthly conduct report, parent-teacher meeting, complaint escalation hierarchy.

- [ ] **AC-COMM-001:** Parent portal — phụ huynh có quyền pháp lý xem (Luật Giáo dục 2019 Đ.83): học bạ con, điểm danh hàng ngày, học phí + thanh toán, hạnh kiểm, thông báo từ GVCN, lịch sử kỷ luật
  - **Test:** Phụ huynh login (qua Zalo OTP hoặc email password) → dashboard hiển thị dashboard thẻ children: HS Nguyễn Văn A 7A — điểm số 12 môn HK1 hiện tại, điểm danh tháng (32/35 buổi, vắng 3 buổi có phép), học phí tháng 10 paid, hạnh kiểm "Tốt" (HK1 sơ kết); click vào A → xem chi tiết: timeline GVCN comment "tích cực phát biểu trong giờ Văn", upload Avatar, view sổ liên lạc digital
  - **Fail signal:** Không có parent portal (vi phạm Đ.83 Luật Giáo dục), hoặc parent phải gọi điện hỏi GVCN, hoặc thiếu 1 trong 6 quyền pháp lý trên, hoặc không support multi-children (1 phụ huynh có 2 con cùng trường)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — **CRITICAL/LEGAL MANDATE**)

- [ ] **AC-COMM-002:** Bulk parent notification per class / grade / school — GVCN gửi 42 phụ huynh 7A, Phó HT gửi 320 PH khối 7, Hiệu trưởng gửi 1500 PH toàn trường, chọn channel: SMS / Zalo / email / push, có template + variable substitution
  - **Test:** GVCN viết "Kính gửi PH HS [TÊN_HS] 7A. Kết quả thi giữa kỳ Toán: [ĐIỂM]. Kính mời PH dự họp 18/10. GVCN [TÊN_GVCN]" → hệ thống render 42 messages cá nhân hóa → preview → send qua Zalo (default cho K-12 vì miễn phí) → 42 deliveries trong ≤2 phút → analytics: 38 read, 4 unread → GVCN follow-up 4 cases manual
  - **Fail signal:** Không có template + variable, hoặc bulk send mất >10 phút (gọi API rate-limited), hoặc không có read-receipt analytics, hoặc không support Zalo (kênh chính ở VN)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-003:** Urgent alert — Hiệu trưởng có thể broadcast emergency (school closure do bão, dịch, ngập lụt; lockdown; weather warning) cho 1500+ PH trong ≤5 phút, multi-channel cùng lúc
  - **Test:** Hiệu trưởng nhận tin bão số 5 → dashboard emergency broadcast → gõ "THÔNG BÁO KHẨN: Trường nghỉ học ngày 16/10 do bão. Thầy cô tự bảo trọng. HS làm BTVN tại nhà." → chọn "All parents + all staff" → send SMS + Zalo + push + email cùng lúc → 1500 PH × 4 channels = 6000 deliveries trong ≤5 phút → admin dashboard real-time: SMS 1490/1500 success, Zalo 1480/1500
  - **Fail signal:** Phát thanh viên chỉ + group chat (không reach hết), hoặc emergency mất >15 phút, hoặc không có failover khi 1 channel fail
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification — emergency channel)

- [ ] **AC-COMM-004:** Monthly conduct report — đầu tháng GVCN tự động generate báo cáo hạnh kiểm + điểm danh tháng trước cho từng HS, gửi PH dạng PDF qua email + viewable trên portal
  - **Test:** 1/11 system auto-generate cho 42 HS 7A: PDF 1 trang / HS gồm: ảnh HS, điểm danh tháng 10 (32/35), điểm số 12 môn (cập nhật đến 31/10), nhận xét GVCN (free-text 200 từ), khen thưởng (nếu có), vi phạm (nếu có), suggested action; GVCN review 42 reports trong dashboard, edit nhận xét → publish → PH nhận email + portal
  - **Fail signal:** Không có monthly report tự động (GVCN làm thủ công Word + email), hoặc PDF format không chuyên nghiệp, hoặc không có khả năng GVCN edit nhận xét trước publish
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal), GAP-055 (báo cáo PDF format), GAP-059 (conduct)

- [ ] **AC-COMM-005:** Parent-teacher meeting coordination — họp PH định kỳ (đầu năm, giữa kỳ, cuối kỳ, cuối năm = 4 lần/năm), GVCN tạo lịch + mời PH RSVP + ghi biên bản
  - **Test:** GVCN tạo cuộc họp "Họp PH 7A cuối HK1" 25/12 18:00–20:00 phòng 7A → hệ thống gửi mời 42 PH qua SMS+Zalo+email với calendar.ics attachment → PH RSVP (Tham gia / Không tham gia / Cử người khác) → 35 confirm, 7 absent với lý do; ngày họp GVCN ghi biên bản digital + attendance + photo → upload portal → PH absent vẫn xem được biên bản
  - **Fail signal:** Không có meeting coordination, GVCN tự gửi giấy mời + collect bằng tay, hoặc không có RSVP tracking, hoặc biên bản chỉ Word file → khó tìm
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal), Related: TT 32/2020 quy định họp PH

- [ ] **AC-COMM-006:** Complaint escalation hierarchy — phụ huynh khiếu nại theo tier: GVCN (level 1) → Phó CM (level 2) → Hiệu trưởng (level 3) → Phòng GD&ĐT (level 4 — external); SLA mỗi level + auto-escalate
  - **Test:** PH gửi complaint "GVCN cô D đối xử bất công với HS A" qua portal → hệ thống tạo ticket level 1 assigned cô D + Phó CM CC; SLA 5 ngày làm việc cô D phải reply; nếu PH unsatisfied → escalate level 2 (Phó CM) SLA 5 ngày; tiếp escalate level 3 (Hiệu trưởng) SLA 7 ngày; tiếp escalate level 4 (Phòng GD) — system export gói tài liệu chuẩn để PH gửi cơ quan ngoài; full audit trail
  - **Fail signal:** Không có escalation, PH phải đến trực tiếp / gọi điện, hoặc không có SLA, hoặc không có audit trail khi vụ việc kéo dài → khi tranh chấp pháp lý không có evidence
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy), Related: child-protection-policy.md §4.4 mandatory reporting

---

## 5. Edge Cases AC

Student transfer between schools, absent student follow-up legal mandate, teacher emergency replacement, exam re-take, child safety incident.

- [ ] **AC-EDGE-001:** Student transfer mid-year — HS chuyển trường giữa năm, hệ thống xuất gói chuyển trường (học bạ + điểm danh + hạnh kiểm + học phí pending) cho PH cầm sang trường mới trong ≤3 ngày
  - **Test:** PH HS Nguyễn Văn A 7A báo chuyển trường ngày 15/12 (giữa HK1) → admin tạo "Transfer-out" workflow → sequence: kế toán close học phí (refund pro-rated tháng 12), GVCN finalize điểm + hạnh kiểm tạm thời HK1, văn thư export "Transfer package" PDF gồm: học bạ MOET-format, biên bản điểm danh chi tiết, conduct history, lịch sử kỷ luật nếu có; A nhận PDF + bản cứng có dấu trường; trường mới import được
  - **Fail signal:** Không có transfer workflow, văn thư phải copy thủ công từ Excel + đóng dấu, hoặc thiếu fields trong transfer package, hoặc không có pro-rated refund
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (export), GAP-055 (học bạ), GAP-184 (data retention)

- [ ] **AC-EDGE-002:** Absent student follow-up legal mandate — HS vắng không phép 3 ngày liên tiếp → auto-alert GVCN + Phó CM + Phụ huynh; vắng 5 ngày → escalate Phòng GD (Luật Giáo dục 2019 Đ.13 phổ cập giáo dục)
  - **Test:** HS B 7A vắng không phép 3 ngày 12, 13, 14/10 → ngày 15/10 sáng system auto-alert GVCN + Phó CM + 2 phụ huynh; GVCN gọi PH nhưng không liên lạc được → ngày 16/10 vẫn vắng (4 ngày) → tăng alert Hiệu trưởng; ngày 17/10 vắng 5 ngày → system tạo "Phổ cập alert" + suggest contact Phòng GD công an khu vực; full audit log
  - **Fail signal:** Không có 3-day / 5-day threshold alert, GVCN phải tự nhớ, hoặc không escalate → vi phạm Luật Giáo dục Đ.13 phổ cập (cấp 1 + cấp 2 bắt buộc); cũng có thể là child safety risk theo child-protection-policy
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period attendance), Related: GAP-186 child protection §4.4 mandatory reporting

- [ ] **AC-EDGE-003:** Teacher emergency replacement — GV bộ môn nghỉ ốm đột xuất (cùng ngày), Phó CM phân công GV thay trong ≤30 phút trên mobile, GV thay nhận giáo án + roster lớp
  - **Test:** 06:30 cô C GV Lý nghỉ ốm đột xuất, báo qua app → Phó CM nhận alert; mở dashboard "Emergency substitution" → list GV Lý có sẵn (off-period hoặc free) → assign cô D; cô D nhận push notification "Bạn vừa được phân công dạy 8A tiết 2 hôm nay 15/10, link giáo án [URL]"; cô D mở app thấy: roster 8A 38 HS với photo, giáo án tiết 2 (cô C đã upload), nội dung tuần này, audit log; sau tiết 2 access auto-revoke
  - **Fail signal:** Không có emergency workflow, Phó CM gọi điện thủ công, hoặc cô D không thấy giáo án, hoặc không có time-bound RBAC → cô D có quyền vĩnh viễn 8A
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy + RBAC time-bound)

- [ ] **AC-EDGE-004:** Exam re-take cho HS ốm — HS nghỉ ốm có giấy bệnh viện trong kỳ thi → admin tạo re-take session trong ≤7 ngày sau khỏi, điểm tính như thi bình thường, không penalty
  - **Test:** HS C ốm nặng nghỉ thi cuối HK1 ngày 20/12 → có giấy nhập viện → PH submit qua portal kèm scan; admin Phó CM duyệt → tạo re-take session 28/12 cùng đề thi (sealed) hoặc đề tương đương (do Tổ trưởng quyết định) → C thi → điểm tính bình thường vào ĐTBmHK1 → không bị mark "absent"
  - **Fail signal:** Không có re-take workflow, HS C bị mark vắng + 0 điểm → ảnh hưởng lên lớp; hoặc không có evidence upload từ PH; hoặc không có audit ai duyệt re-take
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: GAP-061 (promotion logic), GAP-055 (báo cáo)

- [ ] **AC-EDGE-005:** Child safety incident — PH/HS/GV report sự việc nghi ngờ ngược đãi/grooming/bullying → hệ thống capture confidential, alert designated safeguarding officer + Hiệu trưởng + (nếu CSAM) MOLISA + công an theo Luật Trẻ em 2016 Đ.51
  - **Test:** PH HS D 7A report qua portal "nghi ngờ con bị bullying online từ HS E cùng lớp, có ảnh nhạy cảm" → ticket priority CRITICAL, encrypted, only safeguarding officer + Hiệu trưởng + designated counselor see; hệ thống auto-suggest mandatory reporting (Luật Trẻ em Đ.51) — alert Tổng đài 111 + công an địa phương; preserve evidence (chat logs, attendance, parent contact); full audit log + non-repudiation
  - **Fail signal:** Không có safeguarding workflow, report đi qua bình thường ticket → leak; hoặc không có mandatory reporting suggestion → vi phạm pháp luật; hoặc evidence bị xóa khi gap closure → mất chứng cứ pháp lý
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**), Related: child-protection-policy.md §4.4

---

## 6. Exit / Termination AC

Student graduation, student transfer-out, teacher retirement / resignation, school closure MOET coordination.

- [ ] **AC-EXIT-001:** Student graduation — cuối lớp 9 / lớp 12, hệ thống generate học bạ chính thức (sealed PDF với e-signature Hiệu trưởng + dấu trường), bằng tốt nghiệp THCS/THPT theo format MOET, lưu 5 năm (TT 32/2020 Đ.40)
  - **Test:** 31/5/2027 lớp 9C tốt nghiệp 38 HS → sequence: GVCN finalize điểm cả năm + hạnh kiểm cả năm, Tổ trưởng duyệt, Hiệu trưởng ký electronic + dấu digital → system generate 38 học bạ PDF + 38 bằng tốt nghiệp THCS PDF (format Phụ lục I TT 22/2021); lưu vào archive 5-year retention; HS + PH download được; trường mới (cấp 3) verify bằng QR code
  - **Fail signal:** Học bạ không sealed, có thể edit sau publish; hoặc không e-signature; hoặc không lưu 5 năm (vi phạm TT 32/2020); hoặc QR verification không hoạt động → trường mới phải gọi điện confirm
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report MOET), GAP-184 (5-year retention), Related: TT 22/2021 Phụ lục I, TT 32/2020 Đ.40

- [ ] **AC-EXIT-002:** Student transfer-out (out-of-cycle) — HS chuyển trường, gói transfer được generate đầy đủ + records gửi receiving school qua kênh chính thức MOET hoặc bản cứng có dấu
  - **Test:** Sau AC-EDGE-001 transfer-out workflow hoàn tất → file gói transfer signed + sealed → admin văn thư export 2 versions: (1) PDF qua MOET inter-school API (nếu trường mới ở cùng tỉnh), (2) bản cứng có dấu trường + ký Hiệu trưởng cho PH cầm tay; receiving school confirm receipt → system close transfer ticket; data của HS A vẫn lưu trong tenant cũ 5 năm theo retention
  - **Fail signal:** Không có channel chính thức (chỉ PH cầm), hoặc receiving school không confirm được, hoặc data của HS bị xóa khi transfer-out (vi phạm 5-year retention)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (5-year retention), Related: GAP-055, GAP-051

- [ ] **AC-EXIT-003:** Teacher retirement / resignation — GV nghỉ hưu / nghỉ việc, system off-board: hệ thống RBAC revoke tất cả quyền access HS/lớp, lưu archive teaching history + giáo án + điểm đã chấm; HR generate quyết định nghỉ + thanh lý hợp đồng + thanh toán lương cuối + chốt sổ BHXH
  - **Test:** Cô A GV Toán nghỉ hưu 31/10/2026 (60 tuổi) → 30/10 admin trigger off-board: 1/11 sáng access auto-revoke (không vào được hệ thống), điểm đã chấm + giáo án archive, hồ sơ chuyển sang archive 7 năm (BHXH retention), payroll tính lương cuối + thưởng nghỉ hưu, BHXH chốt sổ qua VBA hoặc TT 56/2017; cô A nhận PDF quyết định + thanh lý + bản tổng kết teaching history
  - **Fail signal:** Access không revoke ngay (cô A vẫn xem được điểm sau nghỉ — privacy violation), hoặc không archive teaching history → trường mất evidence; hoặc payroll không close → cô A bị quên trả lương
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (retention), GAP-062 (payroll), Related: child-protection-policy.md §6 staff off-board

- [ ] **AC-EXIT-004:** School closure — trường giải thể (private school phá sản, công lập sáp nhập), MOET coordination: HS chuyển sang trường khác, dữ liệu archive 30 năm theo Luật Lưu trữ 2011 + TT 32/2020 Đ.40, all parents notify
  - **Test:** Hiệu trưởng + chủ tịch HĐQT (private school) ra quyết định giải thể 30/6/2027 → workflow: 1/4 thông báo MOET + Phòng GD + 1500 PH 6 tháng trước, 1/5 sequence transfer 800 HS sang 5 trường khác (MOET phân công), 30/6 close active operations, all parent invoices closed/refunded; sau 30/6: data bulk-archive 30 năm vào MOET storage hoặc third-party archive (TT 32/2020 Đ.40), tenant ENTERPRISE giải thể nhưng archive read-only available 30 năm cho cựu HS query học bạ
  - **Fail signal:** Không có closure workflow MOET-coordinated, hoặc data bị xóa thay vì archive 30 năm (vi phạm Luật Lưu trữ), hoặc cựu HS không thể query học bạ sau 5 năm
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (retention extended 30y), Related: TT 32/2020 Đ.40, Luật Lưu trữ 2011

---

## Scoring

**Total ACs:** 36 (sum across 6 categories: 6 + 10 + 5 + 6 + 5 + 4 = LARGEST persona doc trong Wave Persona-AC-Template — comprehensive coverage of MOET hierarchical complexity)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / 36 × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ K-12 fully supported (production-ready, MOET-compliant deployment ready) |
| 60–84% | ⚠️ K-12 partially supported (usable for pilot trường nhỏ, defer GA Tier-1 deployment) |
| 30–59% | 🔴 K-12 NOT supported (major gaps; not production-ready — block K-12 GA) |
| <30% | ❌ K-12 NOT viable (current persona-catalog estimate 30% — confirms blocker status) |

**Pre-review baseline (from `personas-catalog.md` 2026-04-14):** 🔴 30% coverage — **CRITICAL gaps:** bulk import, parent portal, academic year, report cards, homeroom, conduct. This AC doc enumerates 36 ACs to make the gap concrete + measurable.

---

## Gap Linkage Summary

(Pre-filled with linked gaps; Status filled at GAP-152 review time)

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-051, GAP-058 | 🔵 OPEN | P0 |
| AC-ONBOARD-002 | TBD | GAP-051, GAP-052, GAP-063 | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | GAP-058, GAP-186 | 🔵 OPEN | P0 |
| AC-ONBOARD-004 | TBD | GAP-053 | 🔵 OPEN | P0 |
| AC-ONBOARD-005 | TBD | GAP-186 | 🔵 OPEN | P0 |
| AC-ONBOARD-006 | TBD | GAP-054 | 🔵 OPEN | P0 |
| AC-OPS-001 | TBD | GAP-056, GAP-060, GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-002 | TBD | GAP-060 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-054, GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-059 | 🔵 OPEN | P0 |
| AC-OPS-005 | TBD | GAP-055, GAP-058 | 🔵 OPEN | P0 |
| AC-OPS-006 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-OPS-007 | TBD | (new sub-feature) | — | P1 |
| AC-OPS-008 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-OPS-009 | TBD | GAP-052, GAP-054 | 🔵 OPEN | P1 |
| AC-OPS-010 | TBD | (TT 32/2020 — new) | — | P1 |
| AC-FIN-001 | TBD | (pricing-model.md) | — | P0 |
| AC-FIN-002 | TBD | (pricing-model.md) | — | P0 |
| AC-FIN-003 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-FIN-004 | TBD | GAP-051, (TT 107/2017) | — | P0 |
| AC-FIN-005 | TBD | GAP-062 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-052 | 🔵 OPEN | **P0 LEGAL** |
| AC-COMM-002 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-003 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-004 | TBD | GAP-052, GAP-055, GAP-059 | 🔵 OPEN | P0 |
| AC-COMM-005 | TBD | GAP-052 | 🔵 OPEN | P1 |
| AC-COMM-006 | TBD | GAP-058, GAP-186 | 🔵 OPEN | P0 |
| AC-EDGE-001 | TBD | GAP-051, GAP-055, GAP-184 | 🔵 OPEN | P0 |
| AC-EDGE-002 | TBD | GAP-060, GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-003 | TBD | GAP-058 | 🔵 OPEN | P1 |
| AC-EDGE-004 | TBD | GAP-061, GAP-055 | 🔵 OPEN | P1 |
| AC-EDGE-005 | TBD | GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-EXIT-001 | TBD | GAP-055, GAP-184 | 🔵 OPEN | P0 |
| AC-EXIT-002 | TBD | GAP-184, GAP-055, GAP-051 | 🔵 OPEN | P0 |
| AC-EXIT-003 | TBD | GAP-184, GAP-062, GAP-186 | 🔵 OPEN | P1 |
| AC-EXIT-004 | TBD | GAP-184 | 🔵 OPEN | P1 |

**New gaps surfaced by this persona review** (file via `audit-to-gap-pipeline.md` Step 2.5 state-check after review):
- **NEW-1:** Classroom resource scheduling (phòng máy, lab, sân) with conflict detection — needs new gap
- **NEW-2:** Sổ đầu bài digital (TT 32/2020 mandate) — needs new gap
- **NEW-3:** Exam invigilation roster — sub-feature of GAP-058
- **NEW-4:** MOET inter-school transfer API (cùng tỉnh) — needs new gap, blocks AC-EDGE-001 + AC-EXIT-002
- **NEW-5:** School closure 30-year archive workflow — sub-feature of GAP-184

---

## Cross-References

- **Persona source:** [`../personas-catalog.md`](../personas-catalog.md) §P5 Public/Private K-12 School (USER's example)
- **Sibling AC docs (Wave Persona-AC-Template):**
  - [`P1-solo-teacher.md`](P1-solo-teacher.md) — different scale (1 teacher vs 50)
  - [`P2-small-center.md`](P2-small-center.md) — different vertical (private center vs school)
  - [`P3-medium-center.md`](P3-medium-center.md) — closest scale match but different regulator (no MOET reporting)
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **AC framework gap:** [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (this template — Phase 1)
- **Review execution gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md) (Round 1 review — multi-stakeholder for K-12)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **K-12 specific gaps cross-linked above:** GAP-051, GAP-052, GAP-053, GAP-054, GAP-055, GAP-056, GAP-058, GAP-059, GAP-060, GAP-061, GAP-062, GAP-063, GAP-180, GAP-184, GAP-186 (15 gaps total)
- **Legal docs cross-linked:**
  - [`../child-protection-policy.md`](../child-protection-policy.md) — child protection (GAP-186 Phase 1 skeleton, parental consent, mandatory reporting, staff vetting)
  - [`../data-retention-deletion-policy.md`](../data-retention-deletion-policy.md) — 5y educational records, 30y archived school data
  - [`../terms-of-service.md`](../terms-of-service.md) — formal school-parent contract terms
  - [`../pricing-model.md`](../pricing-model.md) — public school fee compliance + private school flexible pricing
  - [`../billing-terms.md`](../billing-terms.md) — payment terms, late fees, refunds
  - [`../compliance-scope.md`](../compliance-scope.md) — Vietnam-only PDPL + MOET + Luật Trẻ em scope

---

## Reviewer Hat (Phase 2 — for GAP-152 multi-stakeholder review)

**K-12 đặc biệt cần multi-stakeholder review** (không thể chỉ 1 PO sign):

| Reviewer role | Critical responsibility | Sample stakeholder |
|---------------|------------------------|--------------------|
| **Real school principal** (Hiệu trưởng) | Validate hierarchy + workflow + GVCN UX + financial structure | Hiệu trưởng THCS công lập 800 HS Hà Nội, 10+ năm kinh nghiệm |
| **GVCN representative** | Validate AC-OPS-001..010, AC-COMM-004..005 (daily reality of GVCN) | Tổ trưởng GVCN khối 7, 5+ năm GVCN |
| **MOET education domain expert** | Validate compliance với TT 22/2021 + TT 32/2020 + Luật Giáo dục 2019 | Cán bộ Phòng GD&ĐT Quận hoặc lecturer trường ĐH Sư Phạm chuyên về quản lý giáo dục |
| **Legal counsel (child protection)** | Validate GAP-186 cross-references + Luật Trẻ em 2016 + Decree 13/2023 Art 16 + mandatory reporting | Luật sư chuyên về quyền trẻ em, có kinh nghiệm với MOLISA |
| **Product Owner (KiteClass)** | Cross-cut với P3 medium center, identify shared logic vs K-12-specific | Internal — @nguyenvankiet acting PO |

**Review process estimate:** 4–6 ngày (review 36 ACs × 5 stakeholders = significant effort) — defer to GAP-152.

---

## How to Use This Doc

1. **Phase 1 (now — 2026-04-30):** AC framework drafted (this file v1)
2. **Phase 2 (GAP-152 Round 1 review):** 5 stakeholders fill Status (PASS/PARTIAL/FAIL) + add evidence; new gaps filed for FAIL items không match existing GAP
3. **Phase 3 (post-review):** ROADMAP updated với new gaps; coverage % computed; if <60% → block K-12 GA + plan Wave K-12 Coverage; if 60–84% → defer K-12 GA, allow pilot
4. **Phase 4 (quarterly re-review):** Re-score sau mỗi wave fix related gaps; track delta

**This doc itself is reviewed every quarter** (per `business-logic-review.md` Quarterly cadence) — at minimum re-check MOET regulation updates (Thông tư mới có thể change AC-OPS-003 grade formulas, AC-FIN-002 fee caps).

---

## Anti-Patterns (specific to K-12 ACs)

| ❌ Don't | ✅ Do |
|---------|------|
| Treat K-12 like a "big medium center" | Recognize K-12 = different vertical (MOET regulated, parent legal mandate, hierarchy depth, period attendance) |
| Single ENTERPRISE config cho cả public + private | AC-FIN-002 phân biệt rõ — public school có fee cap + scholarship, private có flexible |
| Skip parent portal as "nice-to-have" | AC-COMM-001 = LEGAL MANDATE per Luật Giáo dục Đ.83 — non-optional |
| Adapt center's per-day attendance for K-12 | Period-based (5–10 tiết/ngày) — fundamentally different model |
| Vendor-lock với 1 SMS provider | Multi-channel (SMS + Zalo + email + push) — Zalo dominant in VN K-12 |
| Sai format học bạ chỉ vì "look similar" | TT 22/2021 Phụ lục I cụ thể — sai 1 trường = phòng GD trả về |

---

## Log

- **2026-04-30** — Initial AC set v1 created by Agent D (Wave Persona-AC-Template, GAP-151 Phase 1). 36 ACs across 6 categories (LARGEST persona doc in wave — reflects K-12 hierarchical complexity, MOET regulatory burden, parent legal mandate, child protection compliance). Cross-referenced 18 GAP refs + 6 legal/BRD docs + 73 MOET/Luật citations. Multi-stakeholder Phase 2 review requirement documented (5 reviewer roles).
- **TBD** (2026-Q3 target) — Phase 2 GAP-152 Round 1 review with multi-stakeholder sign-off (principal + GVCN + MOET expert + legal counsel + PO).
