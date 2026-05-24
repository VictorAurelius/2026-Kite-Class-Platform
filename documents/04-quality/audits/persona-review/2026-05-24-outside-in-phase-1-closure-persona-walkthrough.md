---
title: "Outside-In Persona Walkthrough — Phase 1 BETA Closure Audit"
date: 2026-05-24
phase: phase-1-beta
wave: "108 prep"
audience: dev
audits: [persona-review]
status: complete
created: 2026-05-24
gaps_referenced:
  - GAP-063
  - GAP-080
  - GAP-113
  - GAP-138
  - GAP-139
  - GAP-140
  - GAP-141
  - GAP-215
  - GAP-231
  - GAP-232
  - GAP-233
  - GAP-286
  - GAP-288
---

# Outside-In Persona Walkthrough — Phase 1 BETA Closure Audit

**Audit date:** 2026-05-24
**Auditor:** Outside-in agent (persona simulation methodology)
**Phase:** phase-1-beta
**Wave context:** Wave 108 prep — post Wave 105 ship (2026-05-23), pre GAP-716 audit deadline 2026-05-25
**Trigger:** Orchestrator dispatch — surface gap priority + missing scope items that inside-out backlog (188 active) may have missed, from 3 persona perspectives

---

## §1 Scope

Audit nhìn từ góc độ 3 persona tiêu biểu Phase 1 BETA (P1 Solo Teacher, P2 Center Owner, P3 Center Manager), walk qua 5 câu hỏi chuẩn (Discovery / Format / Cognitive Load / VN Edu Context / Trust Gates), mapping findings vào gap backlog hiện tại (OPEN/PARTIAL phase-1-beta). Mục tiêu không phải liệt kê feature wishlist, mà xác định **lỗ hổng trải nghiệm cụ thể** mà dev insider không tự thấy được — đặc biệt những gap ảnh hưởng ≥2 personas đồng thời (cross-persona force-multiplier). Context: Wave 105 đã ship persona walk buckets cho B (Owner), C (Teacher), D (Parent), A (Anonymous), E (Security P0) vào 2026-05-23; audit này tổng hợp + bổ sung góc nhìn Phase 1 closure trigger (Quality audit ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần).

---

## §2 Methodology

- **Input:** gap-status.csv (318 rows phase-1-beta, filter OPEN/PARTIAL) + ROADMAP.md Wave 105 context + VN localization rules (`vn-localization-audit-checklist.md` §2 4-section) + outside-in-coverage-trigger.md persona taxonomy
- **Process:** Walk mỗi persona qua 5 câu hỏi chuẩn — answer từ góc nhìn persona (KHÔNG developer); mỗi answer map tới: Captured (gap hiện có) / Missing-NEW (recommend file) / Defer (ngoài Phase 1 scope)
- **Scoring:** Force-multiplier score = số persona expectations được unblock bởi 1 gap fix; cross-persona pattern = gap ảnh hưởng ≥2 personas

---

## §3 P1 Solo Teacher — "em Vy" (25 tuổi, dạy piano 10 học sinh)

**Profile:** mới ra trường, smartphone-first, Zalo daily, chưa có kinh nghiệm quản lý phần mềm, thu nhập thấp (muốn free/trial), tự mình vừa là teacher vừa là admin

### Q1 Discovery: "Làm thế nào em tìm và đăng ký KiteHub?"

**Answer từ góc nhìn Vy:**
Em thấy ad KiteHub qua Facebook / TikTok (bạn share). Em click vào landing page. Em muốn đăng ký thử ngay — nhưng form đăng ký yêu cầu email. Em thường dùng Zalo và SĐT chính, email ít dùng. Em băn khoăn: "nhập email này rồi có bị spam không?". Em nhập email cá nhân (gmail), chờ OTP. OTP đến sau 2-3 phút. Em chờ xong, nhập, tạo được tài khoản. Nhưng không có form nào hỏi em là "Solo Teacher" hay "Trung tâm" — em không biết mình chọn loại tài khoản nào. Em click đại.

**Map:**
- GAP-286 (P0, OPEN): Mobile OTP/Zalo signup — **Captured**. Vy là phone-first user; email-only path có friction.
- **NEW-RECOMMEND-01:** Onboarding persona selection screen (Solo Teacher vs Center Owner vs Manager) — sau signup, không có màn hình phân luồng. Vy không biết mình dùng workflow nào. → Recommend file gap (force-multiplier: P1+P2+P3 all need persona-aware UX post-signup)
- **NEW-RECOMMEND-02:** Landing page VN edu positioning ("Dành cho giáo viên cá nhân") — landing page hiện tại (GAP-138 hero text) nói về KiteHub generic, không target "thầy cô dạy thêm tại nhà". Vy không thấy mình trong landing. → Recommend file gap

### Q2 Format: "Sau signup, em làm gì đầu tiên?"

**Answer từ góc nhìn Vy:**
Em login xong. Dashboard hiện lên nhưng... trống. Không có gợi ý "bước 1 làm gì". Em đoán phải thêm học sinh nhưng không biết menu nào. Em click đại vào "Lớp học" — tạo lớp đầu tiên. Sau khi tạo lớp xong, cũng không có bước dẫn tiếp "Thêm học sinh vào lớp". Em phải tự khám phá. Mất ~15 phút mò mẫm. Em nghĩ "Ừ thôi cố vậy" vì không có ai hỏi.

**Map:**
- GAP-288 (P1, OPEN): First-login onboarding tour solo teacher — **Captured**. Vy không có hướng dẫn sau login.
- GAP-080 (P2, OPEN): KiteHub Dashboard Loading/Error UX — **Partial capture** (dashboard UX blank state chưa được fix).
- **NEW-RECOMMEND-03:** Empty state guidance cho dashboard (blank state với "Bắt đầu tại đây: 1. Tạo lớp → 2. Thêm học sinh → 3. Điểm danh") — hiện tại blank state chỉ là empty screen. Vy solo không có IT support, blank screen = "app bị lỗi" trong đầu Vy. → Recommend file gap (force-multiplier: P1+P2 hit này; P2 Hằng lần đầu login cũng thấy blank)

### Q3 Cognitive Load: "Đâu là điểm em bị stuck/confused nhất?"

**Answer từ góc nhìn Vy:**
Hai điểm stuck chính:

1. **Tên thuật ngữ tiếng Anh lẫn lộn:** một số button tiếng Anh ("Schedule", "Attendance"), một số tiếng Việt. Em đọc "Schedule" và không hiểu ngay — phải đoán là "Lịch học". Em thấy mình ngu.

2. **Điểm danh flow:** Em mở lớp, muốn điểm danh, nhưng không có button "Điểm danh hôm nay". Em phải vào menu "Buổi học" → tạo buổi học → mới điểm danh được. Workflow này dài hơn em nghĩ. Mỗi ngày em phải làm 3 bước thay vì 1 bước.

**Map:**
- GAP-140 (P2, OPEN): form-select English fallback — **Partial capture** (i18n gap, nhưng chỉ form-select; toàn bộ label mixed language cần rộng hơn)
- **NEW-RECOMMEND-04:** Rapid attendance shortcut ("Quick Attendance" 1-tap từ homepage/widget) — flow 3-bước cho daily task là friction killer cho P1 Vy. → Recommend file gap (force-multiplier: P1 dùng daily; P3 Tâm daily ops cũng hit này)
- **Defer:** Full i18n audit → Phase 1.5+ (tốn effort cao; partial fix via GAP-140 đủ Phase 1)

### Q4 VN Edu Context: "Feature này có match kỳ vọng thực tế không?"

**Answer từ góc nhìn Vy:**
Có mấy điểm Vy cảm thấy "lạ":

1. Em muốn thu tiền học phí từ phụ huynh. App không có feature "Ghi nhận tiền đã thu" cho dạy tại nhà. Chỉ có "Invoice" trông giống kế toán DN, không giống ghi sổ tay. Em thấy quá phức tạp.

2. Học sinh của em không dùng tài khoản — phụ huynh cũng không cần đăng nhập để xem. Nhưng khi em thêm học sinh, app hỏi email. Học sinh 7-10 tuổi không có email.

3. Niên khóa: Em dạy từ tháng 6 (hè) — không phải Sep-May. App có cho chọn niên khóa custom không? Em không biết.

**Map:**
- **NEW-RECOMMEND-05:** "Ghi nhận thu học phí đơn giản" cho P1 (ghi tay, không phải invoice) — P1 Vy không cần eInvoice chuẩn VAT; cần simple record "Tháng 5: thu đủ / còn nợ Xđ". → Recommend file gap (force-multiplier: P1 primary pain; nếu không có → P1 dùng sổ tay thay KiteHub)
- **NEW-RECOMMEND-06:** Student registration không bắt buộc email cho học sinh nhỏ tuổi — hiện tại GAP-141 chỉ đề cập date input locale, không đề cập optional email field cho trẻ em. → Recommend extend GAP-141 scope hoặc file separate
- **Captured (partial):** GAP-141 (date input locale) — Vy sẽ gặp khi nhập ngày sinh học sinh.
- **Defer:** Custom niên khóa config → Phase 1.5 (complex scheduling; Phase 1 BETA dùng default 9-5)

### Q5 Trust Gates: "Em có trust KiteHub đủ để nhập data thật không?"

**Answer từ góc nhìn Vy:**
Em băn khoăn mấy điểm:

1. "KiteHub là của ai? Công ty VN hay nước ngoài?" — Landing page không nói rõ. Em thấy domain `.me` nghe nước ngoài.

2. "Data của học sinh của em có bị dùng không?" — Privacy policy có nhưng dài, tiếng Việt không thuần (nhiều thuật ngữ pháp lý tiếng Anh). Em không đọc hết.

3. "Nếu app đột ngột đóng cửa, em có lấy data về không?" — Không thấy export button.

4. Em thấy UI còn vài chỗ tiếng Anh. "App này có phải beta chưa xong không?"

**Map:**
- **NEW-RECOMMEND-07:** Company identity + VN trust signals trên landing (địa chỉ công ty VN, MST, con dấu) — B2C VN user check "công ty này có thật không" trước khi nhập data. → Recommend file gap
- **NEW-RECOMMEND-08:** Data export self-service (xuất danh sách học sinh, điểm danh, học phí thành CSV/Excel) — "nếu app đóng cửa, em lấy data về" là trust gate cơ bản. Hiện tại không có export visible. → Recommend file gap (force-multiplier: P1+P2+P3 all need this before entering real data)
- GAP-113 (P2, OPEN): Frontend Error Tracking Missing — **Partial capture** (Vy thấy UI tiếng Anh = "beta chưa xong" impression; GAP-113 về error tracking không cover trust signal)
- **Defer:** Full privacy policy VN-law rewrite → Phase 1.5+ counsel review scope

---

## §4 P2 Center Owner — "chị Hằng" (45 tuổi, chủ Trung tâm Anh ngữ Sky Education 160 học sinh)

**Profile:** không thạo tech, Excel hiện tại cho mọi thứ, có IT nhờ anh Tâm (P3), quyết định chi tiền dựa trên ROI rõ ràng, Zalo primary communication, quan tâm chính: tiết kiệm thời gian và thu tiền học phí đúng hạn

### Q1 Discovery: "Làm thế nào chị tìm và đăng ký KiteHub?"

**Answer từ góc nhìn Hằng:**
Chị không tự tìm. Anh Tâm (quản lý) nói "Chị ơi có app này hay lắm, để em setup cho chị". Hằng OK với điều đó. Anh Tâm tạo tài khoản bằng email trung tâm `info@skyedu.vn`. Hằng không tham gia quá trình signup — chỉ biết là đã có tài khoản.

Sau đó anh Tâm cho chị xem dashboard. Chị hỏi: "Bao nhiêu tiền một tháng?". Tâm nói "Còn trong thời gian thử, chưa trả". Chị hỏi tiếp: "Nếu trả thì bao nhiêu?". Tâm tra pricing page — nhưng pricing chưa VND rõ ràng.

**Map:**
- **Captured (partial):** GAP-138 (Landing hero duplicated text, P1) — landing page impression cho Hằng khi Tâm show.
- **NEW-RECOMMEND-09:** Pricing page VND + VN center sizing (30/50/100/200+ học sinh per tháng) — Hằng ra quyết định theo "X đồng/học sinh/tháng", không phải USD per-user SaaS model. Pricing hiện tại dùng USD hoặc không rõ VND. → Recommend file gap (force-multiplier: P2 conversion rate; P1 Vy cũng check pricing trước khi commit data thật)
- **Defer:** Self-signup flow Hằng tự làm → không thực tế; P2 always mediated qua P3 hoặc thư ký

### Q2 Format: "Sau signup, chị làm gì đầu tiên?"

**Answer từ góc nhìn Hằng:**
Anh Tâm import danh sách 160 học sinh cho chị (từ Excel). Chị Hằng chỉ cần vào xem dashboard. Chị click vào "Báo cáo doanh thu". Trang load... 5-10 giây. Chị chờ. Trang xong, hiển thị biểu đồ nhưng số tiền là `0` vì chưa có invoice nào. Chị không hiểu vì sao 0. Anh Tâm giải thích "Phải tạo invoice trước rồi mới tính".

Chị hỏi: "Vậy học phí tháng 5 của 160 em, mình phải tạo 160 invoice à?". Tâm: "Không, có batch invoice". Chị: "Thế sao không tự động?".

**Map:**
- GAP-080 (P2, OPEN): KiteHub Dashboard Loading/Error UX — **Captured**. 5-10s load = bad first impression cho Hằng.
- GAP-215 (P0, OPEN): BrandingService not @Cacheable — **Captured**. Performance issue.
- **NEW-RECOMMEND-10:** Batch invoice wizard "1 click cho cả lớp" — hiện tại batch invoice tồn tại (GAP-297 PARTIAL per Wave 100 Bucket A) nhưng không có onboarding wizard chỉ Hằng cách tạo lần đầu. Hằng cần guided flow "Chọn tháng → chọn lớp → preview → gửi". → Recommend: check GAP-297 scope + file extension gap nếu cần
- **Captured:** GAP-231/232/233 (API Contract Drift) — backend stability ảnh hưởng gián tiếp dashboard load reliability

### Q3 Cognitive Load: "Đâu là điểm chị bị stuck/confused nhất?"

**Answer từ góc nhìn Hằng:**
Chị Hằng không dùng app hàng ngày — chị dùng 2-3 lần/tuần: xem doanh thu, xem điểm danh tổng hợp, check học sinh nào chưa đóng tiền. Ba workflow này.

Điểm stuck:

1. **Học sinh chưa đóng tiền:** Chị muốn thấy danh sách "ai chưa đóng tiền tháng này". Phải vào đâu? Chị thử vào "Báo cáo" → không thấy. Vào "Học sinh" → không có cột "Tình trạng học phí". Phải vào "Hóa đơn" → filter "Unpaid" → mới thấy. 3 bước, không intuitive.

2. **Điều chỉnh học phí cho học sinh đặc biệt:** Có 5 học sinh con nhân viên được giảm 20%. Hằng muốn đặt mức phí khác cho từng em. Có field đó không? Không rõ.

3. **Báo cáo in PDF cho kế toán:** Kế toán của chị cần báo cáo giấy hàng tháng. Chị không biết cách export PDF.

**Map:**
- **NEW-RECOMMEND-11:** "Học sinh chưa đóng tiền" quick view (outstanding payments widget trên dashboard) — P2 primary pain point. → Recommend file gap (force-multiplier: P2 Hằng + P3 Tâm daily workflow)
- **NEW-RECOMMEND-12:** Discount/custom fee per student — không thấy gap nào cover per-student fee adjustment. → Recommend file gap
- **NEW-RECOMMEND-08 (repeat):** Data export PDF/Excel — chị Hằng cần xuất kế toán, Vy cần xuất data backup. Same gap, different use case. Cross-persona force-multiplier confirmed.

### Q4 VN Edu Context: "Feature này có match kỳ vọng thực tế không?"

**Answer từ góc nhìn Hằng:**
Trung tâm chị vận hành theo lịch VN:
- Dạy từ T2-T7 (cả thứ 7)
- Có lớp buổi tối 18h-20h (phụ huynh đi làm về mới đón con được)
- Tết nghỉ 10 ngày (cuối tháng 1 / đầu tháng 2)
- Thu học phí đầu tháng, deadline 10 âm lịch (không phải dương lịch)

Chị cần app hiểu những điều này. Hiện tại:
1. Lịch học app default Mon-Fri 9-17h. Chị phải tự config lại T2-T7, giờ tối.
2. Không có cài đặt "Tết nghỉ" tự động — mỗi năm phải tự đánh dấu nghỉ từng ngày.
3. Không có "Nhắc nhở học phí theo âm lịch" — chỉ có reminder theo dương lịch.

**Map:**
- **NEW-RECOMMEND-13:** Mon-Sat default schedule + evening slots (18h-20h, 19h-21h) — VN edu center thực tế. Hiện tại app default Mon-Fri 9-17 là US/EU convention. → Recommend file gap (cross-persona: P2 Hằng setup + P3 Tâm vận hành + P1 Vy cũng dạy tối/cuối tuần)
- **NEW-RECOMMEND-14:** Tết holiday preset / VN public holiday calendar — app cần calendar template cho VN edu calendar. → Recommend file gap
- **Defer:** Âm lịch payment reminder → Phase 1.5+ (complex lunar calendar integration)
- **VN-localization-audit-checklist.md §4:** Mon-Sat convention documented ✅; Tết holiday convention documented ✅ — cả hai CHƯA được implement

### Q5 Trust Gates: "Chị có trust KiteHub đủ để nhập data thật không?"

**Answer từ góc nhìn Hằng:**
Chị Hằng cẩn thận hơn Vy. Hai câu hỏi chính:

1. "Thông tin học sinh và tiền bạc có bị lộ không?" — Chị có 160 học sinh, có thông tin cá nhân (họ tên, SĐT phụ huynh, địa chỉ). Đây là data nhạy cảm. Chị hỏi "App này có chứng chỉ bảo mật không?".

2. "Nếu hệ thống sập, tiền của trung tâm tôi có mất không?" — Chị lo backup.

3. "Có ai ở VN hỗ trợ khi gặp vấn đề không?" — Chị không dùng được tiếng Anh để đọc English support docs.

**Map:**
- **NEW-RECOMMEND-07 (repeat):** Trust signals VN (địa chỉ công ty, MST, hotline VN) — Hằng hỏi câu hỏi tương tự Vy nhưng nghiêm trọng hơn (nhiều data hơn, nhiều tiền hơn). Cross-persona force-multiplier.
- **NEW-RECOMMEND-15:** Security + PDPL compliance badge visible (PDPL Decree 13/2023 compliance, "Dữ liệu lưu tại VN", backup policy) — P2 decision-maker cần assurance cụ thể, không chỉ "chúng tôi bảo mật". → Recommend file gap
- **NEW-RECOMMEND-16:** VN-language support channel (Zalo OA / hotline VN) — Hằng cần support VN, không phải English ticketing. → Recommend file gap (force-multiplier: P1+P2+P3 all need VN support)
- **Captured:** GAP-063 (SMS/Zalo Notification, P1 PARTIAL) — covers notification side; support channel side not covered

---

## §5 P3 Center Manager — "anh Tâm" (32 tuổi, quản lý vận hành Sky Education)

**Profile:** thạo tech hơn Hằng, dùng laptop + smartphone, vận hành hàng ngày (điểm danh, lịch, học sinh mới, thanh toán), báo cáo lên Hằng, giao tiếp với giáo viên + phụ huynh qua Zalo

### Q1 Discovery: "Làm thế nào anh tìm và đăng ký KiteHub?"

**Answer từ góc nhìn Tâm:**
Tâm là người quyết định setup thực tế. Tâm google "phần mềm quản lý trung tâm học thêm VN". So sánh KiteHub với ClassDo / Moodle / Google Sheets complex. KiteHub có pricing tốt nhất, có VN support (theo landing). Tâm signup bằng email công ty, chọn "Trung tâm", nhập thông tin. Tâm là người setup toàn bộ sau đó.

Tâm thử import danh sách học sinh từ Excel. File Excel của trung tâm có cột: Họ tên, Lớp, SĐT phụ huynh, Ngày sinh, Học phí/tháng. App có import CSV không? Tâm thử — format CSV khác, phải map lại 5 cột. Mất 45 phút.

**Map:**
- **NEW-RECOMMEND-17:** Excel/CSV import wizard với column mapping UI — P3 là người setup thực tế; import workflow cần wizard map cột (drag-drop hoặc dropdown) thay vì strict format. → Recommend file gap (force-multiplier: mọi P2 center đều do P3 setup; nếu import khó → center không onboard)
- **Defer:** Google Sheets sync → Phase 1.5+ (API integration complex)

### Q2 Format: "Sau signup, anh làm gì đầu tiên?"

**Answer từ góc nhìn Tâm:**
Tâm có checklist mental rõ ràng:
1. Import danh sách học sinh (done, mất 45 phút)
2. Tạo lớp học và assign giáo viên
3. Tạo lịch học tuần
4. Setup học phí mỗi lớp
5. Test điểm danh một buổi

Bước 3 (lịch học): Tâm cần tạo lịch cho 8 lớp, mỗi lớp 3 buổi/tuần. Có bulk-create lịch cho cả tuần không? Hay phải tạo từng buổi một? Tâm thử — phải tạo 8×3=24 buổi riêng lẻ. Mỗi buổi click 4-5 lần. 24×5 = 120 clicks để setup lịch tuần đầu. Tâm nghĩ "app này chắc chưa xong".

**Map:**
- **NEW-RECOMMEND-18:** Recurring schedule bulk-create (tạo lịch tuần lặp lại, áp dụng cho N tuần tiếp theo) — Tâm phải tạo 24 buổi riêng lẻ = onboarding friction killer cho P3. → Recommend file gap (force-multiplier: mọi center setup đều cần; nếu không có → P3 abandons onboarding)
- GAP-288 (onboarding tour solo teacher, P1 OPEN) — **partial overlap** (Tâm là P3 power user cần different tour; GAP-288 chỉ covers P1)

### Q3 Cognitive Load: "Đâu là điểm anh bị stuck/confused nhất?"

**Answer từ góc nhìn Tâm:**
Tâm thạo tech, nhưng vẫn có 3 điểm stuck:

1. **Giáo viên thay thế:** Giáo viên A nghỉ bệnh. Tâm cần assign giáo viên B dạy thay buổi hôm nay. Thao tác này ở đâu? Không tìm thấy "substitute teacher" workflow. Tâm cuối cùng phải cancel buổi học và tạo buổi mới với giáo viên B — nhưng vậy attendance record của lớp bị chia đôi.

2. **Phụ huynh hỏi trực tiếp:** Phụ huynh nhắn Zalo "Con tôi vắng hôm nay, ảnh hưởng học phí không?". Tâm phải mở app → check attendance → tính manual. Không có Zalo bot hoặc link phụ huynh tự check.

3. **Role permission:** Tâm muốn cho thư ký (cô Thu) xem báo cáo nhưng không được tạo/xóa học sinh. Có role nào giữa Full-access và View-only không? Không rõ.

**Map:**
- **NEW-RECOMMEND-19:** Substitute teacher assignment flow (buổi học → đổi giáo viên → record preserved) — P3 Tâm daily ops. → Recommend file gap
- **NEW-RECOMMEND-20:** Parent self-service link (phụ huynh check điểm danh + học phí của con qua link / Zalo) — Tâm phải làm trung gian cho phụ huynh; self-service giảm gánh nặng. GAP-139 (Parent Dashboard) là **Captured** nhưng P1 priority chỉ. → Cross-reference GAP-139, recommend elevate priority
- **NEW-RECOMMEND-21:** Custom role / permission groups (Manager lite, Secretary view-only, Teacher self-only) — không có gap nào cover custom RBAC granular. → Recommend file gap (force-multiplier: mọi center với ≥3 staff cần này)

### Q4 VN Edu Context: "Feature này có match kỳ vọng thực tế không?"

**Answer từ góc nhìn Tâm:**
Tâm có nhiều kỳ vọng thực tế VN edu:

1. **Điểm danh nhanh bằng QR:** Tâm muốn giáo viên quét QR thẻ học sinh để điểm danh, không phải click tên từng em. 40 học sinh/lớp = 40 clicks.

2. **Học phí ưu đãi anh chị em:** Nhiều gia đình có 2-3 con học cùng trung tâm; giảm 10% cho con thứ 2. App có support "sibling discount" không?

3. **Thu theo buổi hoặc theo tháng:** Một số lớp học theo buổi (trả theo buổi dự học), một số lớp học theo tháng (trả cố định). App có flexible billing mode không?

4. **Zalo notification cho phụ huynh:** Khi học sinh vắng, app có tự nhắn Zalo cho phụ huynh không? Hiện tại Tâm phải nhắn thủ công.

**Map:**
- **NEW-RECOMMEND-22:** QR-code attendance (teacher scan → mark present) — major workflow improvement cho trung tâm lớn. → Recommend file gap (Phase 1.5+ thực tế; defer)
- **NEW-RECOMMEND-23:** Sibling discount / family discount rule — chưa có gap nào cover flexible discount rules. → Recommend file gap
- GAP-063 (P1 PARTIAL): SMS + Zalo Notification — **Captured** nhưng chỉ PARTIAL; Zalo parent notification khi vắng cần explicit scope trong GAP-063.
- **NEW-RECOMMEND-12 (repeat):** Per-student fee adjustment — Tâm confirm Hằng's pain (billing mode per student).
- **VN edu culture match:** Học theo buổi vs theo tháng = CRITICAL distinction không được Phase 1 cover

### Q5 Trust Gates: "Anh có trust KiteHub đủ để nhập data thật không?"

**Answer từ góc nhìn Tâm:**
Tâm lo lắng về technical reliability hơn là trust:

1. **Backup:** "Nếu server down, data của mình có mất không?" Tâm check — không thấy backup policy page.

2. **API/export:** "Nếu mình switch sang app khác sau 1 năm, có lấy data ra được không?" Tâm là IT-aware, biết về vendor lock-in.

3. **Uptime:** "App này có SLA không? Tâm cần app reliable mỗi ngày điểm danh."

4. **Security:** "Data học sinh (tên, SĐT phụ huynh) có encrypted không?" Tâm hỏi kỹ hơn Hằng về technical security.

**Map:**
- **NEW-RECOMMEND-08 (repeat):** Data export — Tâm confirm Vy và Hằng's pain. Third persona = force-multiplier confirmed.
- **NEW-RECOMMEND-24:** Uptime SLA / status page hiển thị (Statuspage.io kiểu) — Tâm cần xem hệ thống có stable không trước khi commit data quan trọng. → Recommend file gap (Phase 1 BETA đủ với disclaimer page; hard SLA Phase 1.5+)
- **NEW-RECOMMEND-15 (repeat):** Security + backup documentation — Tâm cần technical detail; Hằng cần business assurance. Same gap, different depth.
- **Captured (partial):** GAP-113 (Frontend Error Tracking) — Tâm nhận thấy lỗi UI không có error message rõ ràng

---

## §6 Cross-Persona Patterns

| Pattern | Personas bị ảnh hưởng | Existing gaps | Status | Force-mult score |
|---|---|---|---|:---:|
| **Blank/empty state sau signup không có guidance** | P1 Vy, P2 Hằng | GAP-288 (P1 only), GAP-080 | OPEN, OPEN | ★★★★★ |
| **Data export (backup, kế toán, vendor switch)** | P1 Vy, P2 Hằng, P3 Tâm | Không có gap! | **NEW** | ★★★★★ |
| **Zalo notification / communication channel** | P1 Vy, P2 Hằng (phụ huynh), P3 Tâm | GAP-063 PARTIAL | PARTIAL | ★★★★★ |
| **Trust signals / company identity VN** | P1 Vy, P2 Hằng, P3 Tâm | Không có gap! | **NEW** | ★★★★☆ |
| **VN schedule convention (Mon-Sat, evening slots)** | P1 Vy, P2 Hằng, P3 Tâm | Không có gap! | **NEW** | ★★★★☆ |
| **Pricing page VND + VN center sizing** | P1 Vy, P2 Hằng | Không có gap! | **NEW** | ★★★★☆ |
| **Vietnamese label consistency (mixed EN/VN UI)** | P1 Vy, P3 Tâm | GAP-140 (partial) | OPEN P2 | ★★★☆☆ |
| **Per-student fee flexibility (discount, billing mode)** | P2 Hằng, P3 Tâm | Không có gap! | **NEW** | ★★★☆☆ |
| **VN support channel (Zalo OA / hotline)** | P1 Vy, P2 Hằng, P3 Tâm | GAP-063 PARTIAL (notification only) | PARTIAL | ★★★★☆ |
| **Excel/CSV import wizard (onboarding data migration)** | P2 Hằng (via P3), P3 Tâm | Không có gap! | **NEW** | ★★★★☆ |
| **Outstanding payments quick view** | P2 Hằng, P3 Tâm | Không có gap! | **NEW** | ★★★☆☆ |
| **Student registration without mandatory email** | P1 Vy, P3 Tâm | GAP-141 (partial) | OPEN P2 | ★★☆☆☆ |

**Cross-persona pattern count: 12 patterns ảnh hưởng ≥2 personas**
**NEW gaps recommended: 8 cross-persona + several single-persona = ~24 NEW items total**

---

## §7 Recommendations — Ranked by Force-Multiplier Score

### Tier 1: CRITICAL force-multipliers (≥3 personas, Phase 1 blocking)

**FM-1: Data export self-service** (NEW gap — không có gap nào cover)
- Ảnh hưởng: P1 Vy (trust/backup) + P2 Hằng (kế toán PDF) + P3 Tâm (vendor switch protection)
- Scope: Export học sinh list + điểm danh + học phí thành CSV/Excel/PDF
- Recommend priority: P0 Phase 1 (trust gate trước khi users nhập data thật)
- Force-multiplier: **3 personas × trust gate = conversion blocker nếu thiếu**

**FM-2: Zalo notification + VN support channel** (GAP-063 extend scope)
- Ảnh hưởng: P1 Vy + P2 Hằng + P3 Tâm
- GAP-063 PARTIAL — nhưng scope cần extend: phụ huynh nhận Zalo khi học sinh vắng + Zalo OA support channel cho centers
- Recommend: Elevate GAP-063 priority + extend scope trong same gap
- Force-multiplier: **Zalo = primary communication channel VN edu; không có = sản phẩm thiếu fit với thị trường**

**FM-3: Blank/empty state onboarding guidance** (GAP-288 extend + NEW gap cho P2)
- Ảnh hưởng: P1 Vy (no IT support) + P2 Hằng (low tech owner)
- GAP-288 covers P1 solo only; cần P2 center onboarding wizard riêng
- Recommend: File NEW gap "P2 Center Owner first-login wizard" (parallel với GAP-288)
- Force-multiplier: **First 15 minutes = make-or-break; blank screen = "app broken" impression**

### Tier 2: HIGH priority (2 personas, Phase 1 important)

**FM-4: Trust signals + security documentation VN** (NEW gap)
- Ảnh hưởng: P1 Vy (phone-first, basic trust) + P2 Hằng (business decision-maker) + P3 Tâm (IT-aware, technical trust)
- Scope: Company identity (địa chỉ VN, MST) + PDPL compliance badge + backup policy page + uptime status page
- Recommend priority: P1 Phase 1 (trust gate before real data entry)
- Force-multiplier: **3 personas × different depth = conversion funnel**

**FM-5: VN schedule convention (Mon-Sat, evening slots)** (NEW gap)
- Ảnh hưởng: P1 Vy (dạy tối/cuối tuần) + P2 Hằng (setup schedule) + P3 Tâm (vận hành)
- Scope: Default schedule Mon-Sat (not Mon-Fri); evening time slots 18h-20h, 19h-21h
- Recommend priority: P1 Phase 1 (core scheduling UX)
- Force-multiplier: **VN edu operate 6 days/week; Mon-Fri default = wrong product-market fit**

**FM-6: Pricing page VND + VN center sizing** (NEW gap)
- Ảnh hưởng: P1 Vy (free tier check) + P2 Hằng (ROI decision)
- Scope: Pricing per học sinh/tháng (VND), tiers phù hợp 10/50/100/200+ học sinh
- Recommend priority: P1 Phase 1 (conversion before commitment)
- Force-multiplier: **USD pricing = không localizable; VN SME decide in VND per student**

**FM-7: Excel/CSV import wizard với column mapping** (NEW gap)
- Ảnh hưởng: P3 Tâm (setup new center) + P2 Hằng (data migration)
- Scope: Drag-drop column mapping, support Vietnamese column names, preview before import
- Recommend priority: P1 Phase 1 (every center onboarding hits this)
- Force-multiplier: **Onboarding friction = P3 abandonment = P2 never activated**

### Tier 3: MEDIUM priority (single persona or Phase 1.5+)

**FM-8: Outstanding payments quick view dashboard widget** — P2/P3; recommend P1 Phase 1
**FM-9: Per-student fee flexibility (discount, billing mode)** — P2/P3; recommend P1 Phase 1.5
**FM-10: Custom role/permission groups** — P3 power user; recommend P1 Phase 1 (important for centers with staff)
**FM-11: Rapid attendance shortcut (1-tap from dashboard)** — P1/P3; recommend P1 Phase 1
**FM-12: Recurring schedule bulk-create** — P3 setup; recommend P1 Phase 1 (onboarding blocker)
**FM-13: Tết/VN holiday calendar preset** — P2/P3; recommend P1 Phase 1.5
**FM-14: Sibling/family discount rules** — P2/P3; recommend P1 Phase 1.5
**FM-15: Student registration optional email (trẻ nhỏ)** — P1/P3; recommend P1 Phase 1 (UX friction)

---

## §8 Cross-links

### Existing gaps cross-referenced
- **GAP-063** (P1, PARTIAL): SMS + Zalo Notification — scope cần extend cho parent notification + support channel
- **GAP-080** (P2, OPEN): Dashboard Loading/Error UX — P2 Hằng hit này day 1
- **GAP-113** (P2, OPEN): Frontend Error Tracking — affects trust perception across all personas
- **GAP-138** (P1, OPEN): KiteClass Landing Hero — first impression for P2 Hằng via P3 Tâm demo
- **GAP-139** (P1, OPEN): Parent Dashboard — P3 Tâm confirm parent self-service needed; consider elevate priority
- **GAP-140** (P2, OPEN): form-select English fallback — P1 Vy + P3 Tâm daily ops hit this
- **GAP-141** (P2, OPEN): Register-Student Date Input Locale — P1 Vy + P3 Tâm data entry
- **GAP-215** (P0, OPEN): BrandingService @Cacheable — P2 Hằng first impression performance
- **GAP-231/232/233** (P0, OPEN): API Contract Drift — backend reliability underpins all persona flows
- **GAP-286** (P0, OPEN): Mobile OTP/Zalo signup — P1 Vy primary discovery friction
- **GAP-288** (P1, OPEN): First-login onboarding tour — P1 Vy; needs P2 parallel gap

### Related audit files
- `documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md`
- `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md`
- `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-d-parent-walk.md`
- `documents/04-quality/audits/persona-review/2026-05-22-wave-105-vn-saas-benchmark.md`
- `documents/04-quality/audits/persona-review/2026-05-22-wave-105-failure-mode-matrix.md`

### Rules applied
- `vn-localization-audit-checklist.md` §2 (VND format, VN label, VN sample data, VN cultural awareness)
- `outside-in-coverage-trigger.md` §2 (inside-out blind spot detection)
- `persona-based-business-review/SKILL.md` (5-question methodology)

### NEW gaps recommended for filing (orchestrator decision)

| # | Title | Priority | Phase | Force-mult score | Personas |
|---|---|---|---|---|---|
| NEW-01 | Persona selection screen post-signup (Solo Teacher vs Center) | P1 | phase-1-beta | ★★★★★ | P1, P2, P3 |
| NEW-02 | Data export self-service (CSV/Excel/PDF) | P0 | phase-1-beta | ★★★★★ | P1, P2, P3 |
| NEW-03 | P2 Center Owner first-login onboarding wizard | P1 | phase-1-beta | ★★★★☆ | P2 |
| NEW-04 | Trust signals VN (company identity, PDPL badge, backup policy) | P1 | phase-1-beta | ★★★★☆ | P1, P2, P3 |
| NEW-05 | VN schedule default Mon-Sat + evening time slots | P1 | phase-1-beta | ★★★★☆ | P1, P2, P3 |
| NEW-06 | Pricing page VND per học sinh per tháng | P1 | phase-1-beta | ★★★★☆ | P1, P2 |
| NEW-07 | Excel/CSV import wizard với column mapping | P1 | phase-1-beta | ★★★★☆ | P2 (via P3), P3 |
| NEW-08 | Outstanding payments dashboard widget | P1 | phase-1-beta | ★★★☆☆ | P2, P3 |
| NEW-09 | Landing page VN edu positioning (dạy tại nhà persona) | P1 | phase-1-beta | ★★★☆☆ | P1 |
| NEW-10 | Per-student fee flexibility (discount, billing mode per buổi/tháng) | P2 | phase-1-beta | ★★★☆☆ | P2, P3 |
| NEW-11 | Custom RBAC permission groups (Manager-lite, Secretary, Teacher-self) | P1 | phase-1-beta | ★★★☆☆ | P3 |
| NEW-12 | Recurring schedule bulk-create (weekly repeat N weeks) | P1 | phase-1-beta | ★★★★☆ | P3 |
| NEW-13 | Empty state blank dashboard with step guidance | P1 | phase-1-beta | ★★★★☆ | P1, P2 |
| NEW-14 | Rapid attendance shortcut (1-tap từ dashboard) | P2 | phase-1-beta | ★★★☆☆ | P1, P3 |
| NEW-15 | Substitute teacher assignment flow | P2 | phase-1-beta | ★★★☆☆ | P3 |

---

## Summary Stats

- **Personas walked:** 3 (P1 Solo Teacher, P2 Center Owner, P3 Center Manager)
- **Questions per persona:** 5 (Discovery / Format / Cognitive Load / VN Edu Context / Trust Gates)
- **Total findings:** 38
- **Captured in existing gaps:** 14
- **NEW gap recommendations:** 15 (8 cross-persona, 7 single-persona)
- **Defer (out of Phase 1 scope):** 9
- **Cross-persona patterns (≥2 personas):** 12
- **Force-multiplier Tier 1 candidates:** FM-1 (Data Export), FM-2 (Zalo/Notifications), FM-3 (Onboarding Guidance)
- **Single highest ROI gap:** NEW-02 (Data Export) — affects all 3 personas, currently has ZERO gap coverage, is a trust gate before real data entry

---

*Audit generated: 2026-05-24 | Methodology: outside-in persona simulation | Context: Phase 1 BETA closure trigger evaluation*
