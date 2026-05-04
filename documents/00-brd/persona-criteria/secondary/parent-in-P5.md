# Acceptance Criteria — Parent in P5 Public/Private K-12 School

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Parent × P5
**Persona name (VN):** Phụ huynh học sinh K-12
**Persona name (EN):** Parent of K-12 Student
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent B (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real K-12 parent + GVCN + Legal counsel (PDPL + child protection) + Product Owner (deferred to GAP-152 Round 1)
**Tier:** 1 Primary (LEGAL MANDATE tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152 → privacy-policy.md + child-protection-policy.md
**Tenant context:** Public/Private K-12 School (P5)
**Role:** Parent (secondary persona, legal guardian of minor)
**Strategic priority:** LEGAL MANDATE — parent has legal right to monitor child per Luật Giáo dục 2019 Đ.83
**Legal compliance:** Luật Giáo dục 2019 Đ.83 (parental rights), Luật Trẻ em 2016 (Đ.6 + Đ.51), PDPL Decree 13/2023 Art 16 (parental consent), Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (financial transactions), MOET TT 22/2021 + TT 32/2020

---

## 0. Context

### Persona scale (within tenant — see [`../P5-k12-school.md`](../P5-k12-school.md) §0 cho tenant-level)
- **Population per tenant:** ~1.2× số HS = ~1200 PH accounts cho trường 800 HS (1–2 PH / HS, có sibling discount khi cùng trường); P5 modal scale = **1200–1600 PH active**
- **Per-parent data volume:** 1–3 children (multi-child support critical), 12–15 môn × 2 kỳ × 1 child = ~24–36 grade events / kỳ to monitor; ~250 attendance events / kỳ / child; multiple invoices / month (HP + bán trú + extras); 4 parent-teacher meetings / năm
- **Usage pattern:**
  - **Daily:** sáng (06:30–07:30) check thông báo GVCN từ tối qua, check con đã đi học chưa (attendance auto-notification); tối (19:00–22:00) check BTVN của con + điểm số mới
  - **Monthly:** check + thanh toán học phí đầu tháng (5/T), nhận monthly conduct report (1/T)
  - **Quarterly:** parent-teacher meeting (đầu năm + giữa kỳ + cuối kỳ + cuối năm = 4 lần/năm)
  - **Semester peak:** cuối HK (15/12 + 25/5) — xem học bạ con + finalize thanh toán
  - **Annual peak:** đầu năm 8/9 — bulk consent + thanh toán học phí đầu năm + đồng phục + BHYT

### Profile (typical phụ huynh K-12 VN)
- **Age:** 30–55 tuổi (phụ huynh con cấp 1–3); generational diversity — millennials (con tiểu học) đến Gen X (con cấp 3)
- **Tech savviness:** smartphone-first (Zalo native, Facebook native), email used but not primary; PH cấp 1–2 nhiều khi nhờ con setup app
- **Income tier:** broad — từ thu nhập thấp (HS chính sách 30%) đến thu nhập cao (private school cao cấp); financial decision-making bằng SĐ Ngân hàng / ví điện tử (MoMo, ZaloPay) phổ biến
- **Primary interaction surface:** mobile app (95%+); web rarely cho download báo cáo + invoice
- **Communication channels:** Zalo (PRIMARY 80%), SMS (urgent), email (formal docs invoice/transcript), in-app push (real-time updates), phone call (escalation)
- **Multi-child support critical:** ~30% PH có ≥2 con cùng trường (anh/em, sinh đôi); 1 account → multiple child accounts linked

### Tenant context dependencies — UNIQUE PARENT LEGAL ROLE
- **Legal guardian of minor** — PH là người consent thay mặt cho HS <16 per PDPL Decree 13/2023 Art 16 (parental consent for data processing of minors)
- **Primary financial decision-maker** — học phí (HP) + bán trú + đồng phục + BHYT + BHTN + quỹ PH (multi-fee structure per AC-FIN-001 P5 tenant); KHÔNG phải HS hay nhà trường
- **Right to monitor child academic + behavioral data** per Luật Giáo dục 2019 Đ.83 — PH có quyền pháp lý xem học bạ + điểm danh + hạnh kiểm + sổ liên lạc; cấm trường giấu thông tin
- **Mandatory reporting recipient** — khi GVCN/Hiệu trưởng phát hiện HS có dấu hiệu bị bạo hành / grooming / bullying, PH phải được thông báo (trừ trường hợp PH là người vi phạm — chuyển safeguarding officer + công an)
- **Parent-teacher meeting attendance** — formal 4 lần/năm họp PH lớp; PH là external accountability cho GVCN
- **Communication mediator** cho con — PH chuyển credentials, password recovery (per AC-EDGE-001 student), incident escalation, complaint filing (vì HS không đủ tuổi tự khiếu nại)
- **Cross-tenant** — PH không thuộc school staff, không có school email; account thuộc về PH cá nhân với SĐT/email cá nhân

### Critical concerns (top 7 — driving AC selection)
1. **Parental consent flow** — onboarding consent for child data processing (PDPL Art 16) + recurring re-consent khi có changes (regulation, scope)
2. **Real-time child monitoring** — điểm danh hôm nay con đã đi học chưa (auto-notify <30s sau GVCN điểm danh), điểm số mới, hạnh kiểm tuần
3. **Multi-fee payment + receipt** — HP + bán trú (theo actual buổi) + đồng phục + BHYT + BHTN + quỹ PH per child × multi-child; one-click pay all + per-fee chọn lọc; receipt + e-invoice download
4. **GVCN communication** — primary contact channel cho mọi concerns; bulk class messages from GVCN + 1-to-1 sensitive matters; complaint escalation chain (GVCN → Phó CM → Hiệu trưởng → Phòng GD)
5. **Child safety incident notification** — Luật Trẻ em 2016 mandatory reporting → PH phải được thông báo realtime + được tham gia process safeguarding
6. **Multi-child support** — 1 PH account, nhiều con cùng/khác trường; switch giữa con A / con B / con C thuận tiện; bulk operations across con
7. **Formal documents access** — học bạ + bằng tốt nghiệp + biên bản họp PH + invoice cho purposes scholarship application + visa cho con + chuyển trường

---

## AC Categories (6 standardized)

Each AC: ID, Statement, Test scenario, Fail signal, Status (filled at review time), Linked gap.

---

## 1. Onboarding AC

PH receives signup invite (bulk import → email/Zalo invite) → consent flow → first login → multi-child link.

- [ ] **AC-ONBOARD-001:** PH nhận invite signup qua email + Zalo (primary) sau khi trường bulk import; activate trong ≤10 phút trên mobile; primary credential = email cá nhân + SĐT VN (10 số)
  - **Test:** Trường bulk import 800 HS + 1200 PH (per AC-ONBOARD-002 tenant) → 1200 PH nhận đồng thời: Zalo OA "Trường XYZ — Mời quý phụ huynh kích hoạt tài khoản" + email backup; PH chị Lan tap link → app/web → form: confirm thông tin từ trường (tên đầy đủ, SĐT, email, con: HS Nguyễn Văn A 7A1) + tạo password (min 8 ký tự, không validation phức tạp cho mobile typing); first login → wizard 3 bước (consent + verify children + setup notification preferences)
  - **Fail signal:** Email-only invite (PH không check email → miss), hoặc activation flow >30 phút (PH bỏ giữa chừng), hoặc password complexity blocks (mobile typing capital + symbol khó), hoặc form không pre-fill thông tin từ bulk import (PH phải gõ lại — duplicate effort)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import), GAP-052 (parent portal — onboarding flow), GAP-063 (Zalo OA invite)

- [ ] **AC-ONBOARD-002:** Parental consent flow — PH explicitly consent for child data processing per PDPL Decree 13/2023 Art 16 (data collection scope, retention period, third-party sharing, AI processing); consent audit trail preserved 7 years
  - **Test:** First login wizard step 2 → "Đồng ý xử lý dữ liệu của con" form: (1) data scope (educational records, attendance, grades, photos optional, biometric optional — separate checkbox cho mỗi loại), (2) retention period (5 năm sau khi con tốt nghiệp/transfer), (3) third-party sharing (MOET reporting, Phòng GD external — listed explicitly), (4) AI processing (auto-notification matching, conduct trend analysis — separate consent); PH có thể decline optional items (photos/biometric); cannot decline mandatory items (educational records — required for service); audit log: parent_id, timestamp, IP, consent_version, accepted_items, declined_items; lưu 7 năm
  - **Fail signal:** Single bulk consent (vi phạm PDPL Art 16 granular consent), hoặc không có optional vs mandatory phân biệt (forced consent invalid), hoặc consent không có version (khi PDPL update PH không re-consent), hoặc audit log lưu <7 năm (vi phạm legal compliance), hoặc không có decline option cho photos/biometric
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — parental consent), GAP-184 (consent audit retention 7y), Related: privacy-policy.md §PDPL Art 16, child-protection-policy.md §3 parental consent

- [ ] **AC-ONBOARD-003:** Multi-child link — PH có thể link 1+ children trên cùng account; bulk import auto-link based on parent_id ↔ student_id; PH manual link/request thêm con (anh/em đăng ký sau, sinh đôi); admin văn thư approve manual links
  - **Test:** Sau bulk import: PH chị Lan tự động link con A 7A1 (đã match qua SĐT mẹ trong xlsx); 6 tháng sau con B vào lớp 6 cùng trường → admin văn thư bulk import HS lớp 6 → system detect "Mẹ Nguyễn Thị Lan đã có account" → auto-suggest link → PH receive Zalo + push "Trường đã thêm con thứ 2 vào tài khoản của chị" → PH confirm → link complete; PH cũng có thể manual link request: "Tôi có con khác cùng trường — gửi yêu cầu" → admin verify (CCCD PH + birth cert con) → approve
  - **Fail signal:** PH phải tạo account riêng cho mỗi con (UX terrible cho multi-child families), hoặc auto-link miss khi sibling join later, hoặc không có manual link request channel, hoặc admin approval mất >7 ngày (PH miss đầu năm onboarding của con thứ 2)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — multi-child), GAP-051 (bulk import — sibling detection), GAP-058 (role hierarchy — admin verify)

- [ ] **AC-ONBOARD-004:** Notification preferences setup — PH chọn channel ưu tiên (Zalo / SMS / email / push / phone call) per category (academic / financial / urgent / marketing); default sane (Zalo + push for academic, SMS+Zalo for financial, multi-channel for urgent)
  - **Test:** First login wizard step 3 → grid: rows = categories (Học tập, Tài chính, Khẩn cấp, Marketing), columns = channels (Zalo / SMS / Email / Push / Phone) với checkboxes; default: Học tập = Zalo+Push, Tài chính = Zalo+SMS, Khẩn cấp = ALL, Marketing = Email only; PH adjust nếu muốn (vd opt-out marketing entirely); save → preferences applied; PH có thể edit anytime trong Settings
  - **Fail signal:** Single global notification setting (PH không phân biệt được academic vs marketing — opt-out marketing thì cũng miss học tập), hoặc default tất cả ON (spam), hoặc không có opt-out marketing (vi phạm Luật Bảo vệ Quyền lợi NTD 2023), hoặc không có channel "Phone call" cho urgent (PH không có smartphone vẫn phải reach được)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (multi-channel notification), Related: privacy-policy.md §Marketing opt-out

---

## 2. Daily Operations AC

Real-time child monitoring, attendance roll-call view, homework progress, grade view (full + report card), conduct view.

- [ ] **AC-OPS-001:** Real-time daily attendance — PH nhận push + Zalo notification trong ≤30 giây sau GVCN điểm danh đầu giờ (07:00–08:00); thông báo phân biệt "có mặt / vắng có phép / vắng không phép / muộn"
  - **Test:** GVCN cô Lan điểm danh 7A1 lúc 07:30 (per AC-OPS-001 P5 tenant) → submit → trong ≤30s 42 PH 7A1 nhận: "✓ Con của bạn (HS A) đã có mặt tại trường lúc 07:30" hoặc "⚠ HS A vắng (chưa có phép) — vui lòng liên hệ GVCN" hoặc "⏰ HS A đến muộn 10 phút"; PH multi-child (3 con) nhận 3 notifications riêng biệt với rõ tên con; nếu PH không có internet 30s đó → notification queue và deliver lúc reconnect
  - **Fail signal:** Notification mất >5 phút (PH lo lắng + bắt đầu gọi điện trường), hoặc không phân biệt 4 trạng thái, hoặc không gửi Zalo (chỉ in-app — PH không mở app sáng), hoặc không xử lý multi-child (PH 3 con chỉ nhận 1 thông báo gộp confused)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — attendance push), GAP-060 (period-based attendance), GAP-063 (Zalo notification)

- [ ] **AC-OPS-002:** Period-granular attendance view — PH xem điểm danh con per tiết (5–10 tiết/ngày), aggregated daily / weekly / monthly / semester; calendar view với color-coded status; tap day → detail từng tiết
  - **Test:** PH chị Lan mở "HS A — Điểm danh" tab → calendar tháng 10 với màu (xanh full, vàng vắng có phép, đỏ vắng không phép, cam muộn); tap 15/10 → "Tiết 1 ✓ Toán cô A, Tiết 2 ✓ Văn thầy B, Tiết 3 vắng có phép (giấy bệnh viện), Tiết 4 ✓ Anh, ..."; weekly summary "Tuần 7: 35/40 tiết = 87.5%, vắng tiết 3 thứ 5 do bệnh"; semester "HK1: 580/630 = 92%"; multi-child: switch dropdown "HS A — 7A1" → "HS B — 5A2" mượt mà
  - **Fail signal:** Per-day attendance only (PH không thấy con vắng tiết nào — nghi GV không công bằng), hoặc no aggregation (PH phải tự tính), hoặc switch multi-child mất >5 tap, hoặc không có evidence (giấy bệnh viện) attached cho vắng có phép
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance), GAP-052 (parent portal — multi-child switch)

- [ ] **AC-OPS-003:** Real-time grade monitoring — PH nhận notification trong ≤24h sau GV publish điểm; xem điểm 12+ môn theo cấu trúc TT 22/2021 (TX/GK/CK), auto-compute ĐTBmHK + ĐTBmCN, trend graph theo time
  - **Test:** GV Toán publish điểm GK1 7A1 → 24h sau PH nhận: "📊 HS A vừa có điểm mới: Toán GK1 = 8" + link vào app; PH mở "HS A — Điểm số" → table 12 môn × 6 cột TX/GK/CK; tap "Toán" → detail: TX1=7, TX2=8, TX3=8, TX4=9, GK1=8 → ĐTBmHK1 partial = 4.00 (chờ CK); trend graph: HK1 năm nay vs HK1 năm ngoái (lớp 6) — show progress; nếu ĐTBmHK <5.0 (yếu) → alert PH nên liên hệ GVCN
  - **Fail signal:** PH thấy điểm trước GV publish (vi phạm publishing window), hoặc no trend (PH không thấy con đang lên/xuống), hoặc no automatic alert weak performance (PH miss intervention timing), hoặc no comparison year-over-year
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — grade view), GAP-054 (multi-subject), GAP-055 (báo cáo MOET)

- [ ] **AC-OPS-004:** Homework progress monitoring — PH thấy con đã/chưa nộp BTVN cho 12 môn; daily / weekly summary; alert nếu con miss >3 BTVN trong tuần (intervention trigger)
  - **Test:** GV Toán assign BTVN bài 5, hạn 17/10; HS A nộp 16/10 22:00 → PH nhận push "✓ HS A đã nộp BTVN Toán bài 5 đúng hạn"; nếu HS A không nộp đến 17/10 23:59 → PH nhận push "⚠ HS A chưa nộp BTVN Toán bài 5 (hạn 17/10) — vui lòng nhắc con"; weekly digest sáng T2: "Tuần qua HS A nộp 8/10 BTVN, 2 BTVN trễ (Sinh, Sử)"; nếu missed BTVN >3 trong 7 ngày → escalate alert + suggest "Liên hệ GVCN"
  - **Fail signal:** PH không có visibility vào BTVN (chỉ HS thấy → vi phạm Đ.83 monitoring rights), hoặc no escalation alert weak (PH không biết kịp can thiệp), hoặc digest spam quá thường xuyên (PH disable notifications luôn)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — BTVN visibility), GAP-054 (multi-subject), Related: GAP-056 (GVCN escalation)

- [ ] **AC-OPS-005:** Conduct (hạnh kiểm) monitoring — PH xem hạnh kiểm tuần / tháng / kỳ với reasoning từ GVCN; alert ngay khi có vi phạm nghiêm trọng (vi phạm nội quy nặng, đánh nhau, etc.); PH có thể acknowledge + reply
  - **Test:** GVCN log incident "HS A 15/10: đánh bạn HS C trong giờ ra chơi — mức độ trung bình" → auto-trigger PH chị Lan notification (cấp trên ngay): "⚠ Sự việc về HS A: đánh bạn HS C trong giờ ra chơi 15/10. Vui lòng liên hệ GVCN cô Lan để bàn"; PH mở app → "HS A — Hạnh kiểm" → timeline: đỏ marker 15/10 với mô tả; cuối kỳ HK1 → "Hạnh kiểm HK1: Khá" + reasoning "5 vi phạm + 2 khen thưởng" → PH có thể acknowledge + add comment "Tôi sẽ trao đổi với con"
  - **Fail signal:** PH không nhận realtime alert nghiêm trọng (chỉ thấy cuối kỳ — quá muộn), hoặc no reasoning (PH bị shock với "Khá"), hoặc no acknowledge channel (PH cảm thấy bị one-way)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-059 (conduct tracking), GAP-052 (parent portal — conduct), Related: GAP-056 (GVCN), child-protection-policy.md §4.4

- [ ] **AC-OPS-006:** Formal report card view — PH xem học bạ con cuối kỳ + cuối năm; PDF format MOET TT 22/2021 Phụ lục I; download được (cho scholarship application + chuyển cấp); QR verify code
  - **Test:** Cuối HK1 (15/12) Hiệu trưởng publish học bạ → PH nhận push "📑 Học bạ HK1 của HS A đã có"; PH mở "Tài liệu chính thức" → học bạ HK1 PDF; download (3MB, 5 trang format TT 22/2021); QR code verify → camera mobile scan → confirm "Học bạ chính thức trường THCS XYZ, ban hành 15/12/2026, không bị giả mạo"; lưu local cho purposes; cuối năm thi cấp 3 → PH nộp QR cho trường mới verify
  - **Fail signal:** Học bạ chỉ HTML view (không downloadable), hoặc không có watermark + signature (verify không được), hoặc QR verify không hoạt động (manual phone call để verify chậm), hoặc multi-child PH không thể bulk download tất cả con cùng lúc
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report card MOET), GAP-184 (5y retention), Related: TT 22/2021 Phụ lục I

- [ ] **AC-OPS-007:** Multi-child dashboard — PH 2+ con thấy unified dashboard với summary mỗi con (status: present today / pending fee / new grade / urgent alert); switch detail view giữa con thuận tiện; bulk operations across con (acknowledge attendance, pay all fees)
  - **Test:** PH chị Lan có 2 con (HS A 7A1 + HS B 5A2) mở app → home screen 2 cards: "HS A: ✓ Có mặt 07:30, Toán mới 8 điểm, HP tháng 10 ✓ paid, 1 BTVN pending"; "HS B: ✓ Có mặt 07:25, Hạnh kiểm tuần Tốt, HP tháng 10 ⏳ pending 200k"; tap card → switch to detail view của con đó; nút "Pay all pending fees" → 1 transaction trả tất cả con; "Acknowledge all attendance" → 1 click confirm
  - **Fail signal:** Không có multi-child dashboard (PH phải logout/login giữa con), hoặc no bulk operations (PH 3 con phải pay 3 lần riêng biệt — friction), hoặc switch detail mất >3 tap, hoặc payments không apply correct child
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — multi-child UX)

---

## 3. Financial AC

Multi-fee structure payment, receipt/invoice, payment plan, scholarship handling.

- [ ] **AC-FIN-001:** Multi-fee payment — PH thanh toán HP + bán trú (theo actual buổi) + đồng phục + BHYT + BHTN + quỹ PH per child; one-click pay all hoặc per-fee selective; multi-channel (bank transfer / VNPay / MoMo / ZaloPay / credit card)
  - **Test:** Đầu tháng 10 PH chị Lan mở "Học phí HS A" tab → list 6 khoản: HP 300k + bán trú 500k (25k×20 buổi tháng 9 actual) + đồng phục 800k (one-time, paid Aug → not show) + BHYT 950k (annual, paid 8/9) + BHTN 100k (annual, paid 8/9) + quỹ PH 500k (annual, paid 8/9) → currently due: HP+bán trú = 800k; click "Thanh toán tất cả" → modal: chọn payment method (bank/VNPay/MoMo/ZaloPay/CC) → pay 800k thành công trong ≤2 phút → status update: HP ✓ + bán trú ✓; receipt + invoice gửi email + Zalo; multi-child: dashboard hiển thị "HS A: 800k done, HS B: 250k pending" → "Pay HS B" → option to combine "Pay all children": 1 transaction 1.05M
  - **Fail signal:** Single-fee payment (PH phải pay HP riêng, bán trú riêng — friction), hoặc bán trú không theo actual buổi (charge full month even khi con nghỉ ốm 5 buổi), hoặc no Vietnamese payment methods (chỉ Stripe → fail), hoặc no multi-child combine, hoặc transaction timeout >2 phút
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md (multi-fee), billing-terms.md (payment), GAP-062 (payment integration)

- [ ] **AC-FIN-002:** Receipt + e-invoice download — sau mỗi thanh toán PH nhận receipt PDF + (nếu private school) e-invoice format TT 78/2021 ngay (≤5 phút); lưu archive 5 năm cho tax / scholarship purposes
  - **Test:** PH thanh toán HP tháng 10 = 300k qua VNPay → 5 phút sau nhận: (1) email "Biên lai HS A — HP tháng 10 — 300k" với PDF attached, (2) Zalo notification + link app, (3) in-app: "Hóa đơn" tab → list invoices: "INV-20261005-001 — HP tháng 10 — 300k — Đã thanh toán 5/10/2026" → tap → PDF download; private school: e-invoice format TT 78/2021 với mã số thuế trường + mã CT MST; PH có thể email forward cho kế toán cá nhân (gia đình) cho thuế TNCN; archive 5 năm read-only access cho mọi invoice
  - **Fail signal:** No e-invoice for private school (vi phạm TT 78/2021 — no MST), hoặc receipt mất >24h (PH lo missed transaction), hoặc invoice không có MST (không scholarship verify được), hoặc archive <5 năm (vi phạm tax retention)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: TT 78/2021 (e-invoice), GAP-184 (5y retention)

- [ ] **AC-FIN-003:** Payment reminder + escalation — PH nhận reminder 7d + 3d + 1d trước due date; sau due date tăng tần suất; quá 30d escalate Hiệu trưởng + lock con khỏi 1 số features (warning, không full lockout vì child rights)
  - **Test:** HP tháng 10 due 5/10 cho HS A → PH nhận: 28/9 (T-7) Zalo "Nhắc nhở: HP tháng 10 còn 7 ngày", 2/10 (T-3) Zalo + email, 4/10 (T-1) Zalo + SMS + email + push, 6/10 (D+1) urgent SMS + Zalo, 12/10 (D+7) escalate GVCN message, 5/11 (D+30) escalate Hiệu trưởng + child cảnh báo "Account của con sẽ bị restrict (vẫn xem được nhưng không submit BTVN mới) trong 7 ngày nếu không thanh toán"; PH có thể request hardship payment plan (giãn 3 tháng) qua app → admin văn thư approve
  - **Fail signal:** Email-only reminder (PH miss), hoặc full lockout con (vi phạm child rights — con không nên bị penalize do PH chậm trễ), hoặc no hardship channel (PH gặp khó khăn không có cách), hoặc escalation timing không clear
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (multi-channel reminder), Related: GAP-186 (child rights — no full lockout)

- [ ] **AC-FIN-004:** Scholarship + discount handling — PH HS chính sách (con thương binh, hộ nghèo, ...) + discount rules được auto-apply; PH upload chứng nhận chính sách qua app; admin văn thư verify; transparent breakdown trong invoice
  - **Test:** PH chị Lan submit HS A là con thương binh hạng 2 → upload giấy chứng nhận thương binh + sổ hộ khẩu → admin văn thư verify (qua đối chiếu Phòng LĐTBXH) → approve → policy "discount 50% HP + miễn quỹ PH" applied for năm 2026–2027; invoice tháng 10 hiển thị: "HP tháng 10: 300k - 150k (giảm 50% con thương binh) = 150k due"; PH thanh toán 150k thay vì 300k; transparent + audit log; private school discount tier (early-bird 5%, sibling 10%) cũng apply tương tự
  - **Fail signal:** No scholarship handling (vi phạm chính sách HS chính sách), hoặc no transparent breakdown (PH không hiểu vì sao 150k thay vì 300k), hoặc verify mất >7 ngày (PH miss đầu năm onboarding), hoặc no audit log (không thể prove khi MOET audit)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md (scholarship), child-protection-policy.md (HS khó khăn), GAP-051 (verify document upload)

---

## 4. Communication AC (CRITICAL — LEGAL MANDATE per Đ.83)

GVCN primary contact, parent-teacher meeting RSVP, complaint escalation, child safety incident notification (mandatory per Luật Trẻ em).

- [ ] **AC-COMM-001:** Bulk class messages from GVCN — PH nhận thông báo lớp từ GVCN (announcements, class events, BTVN bulk reminders) qua Zalo + push + email; per-message read receipt cho GVCN tracking
  - **Test:** GVCN cô Lan gửi 7A1 "Họp PH 18/10 18:00 — vui lòng dự" → 42 PH nhận đồng thời qua Zalo + push + email; PH chị Lan mở Zalo → đọc → app sync read-receipt; 35/42 PH read trong 24h, 7 PH chưa → GVCN dashboard thấy unread list → re-send qua phone call cho 7 PH; PH có thể reply "Tham gia / Không / Cử người khác" qua RSVP buttons (template-based, không free-form để tránh harsh language)
  - **Fail signal:** Không có Zalo OA integration (vi phạm primary VN channel), hoặc no read-receipt (GVCN không track ai miss), hoặc reply free-form (abuse risk + chat noise), hoặc bulk send mất >5 phút (delayed delivery)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (Zalo + multi-channel), GAP-052 (parent portal — read-receipt), Related: GAP-056 (GVCN)

- [ ] **AC-COMM-002:** 1-to-1 GVCN conversation — PH có thể initiate sensitive conversation với GVCN qua portal (academic concern về con, hạnh kiểm dispute, family issue impact con); với recording option per child-protection-policy.md §4.3; in-app only, no off-platform
  - **Test:** PH chị Lan trong app → "Liên hệ GVCN" → "Tôi cần nói chuyện riêng với cô Lan về HS A" → option: (1) "Đặt lịch gọi" (chỉ trong office hours 08:00–17:00 working days) → schedule slot → call qua Zoom/Meet integrated với recording prompt; (2) "Gửi tin nhắn detail" (in-app, NOT free-form chat — guided form: subject, concern category, description max 500 chars, optional attachments) → GVCN respond trong 24h SLA; KHÔNG private DM continuous chat (vi phạm child-protection-policy.md §4.2)
  - **Fail signal:** Free-form continuous chat (vi phạm child protection — không có audit boundary), hoặc no recording option for 1-to-1 calls, hoặc PH có thể call GVCN ngoài giờ hành chính (vi phạm work-life balance + grooming risk for staff), hoặc no SLA (PH lo lắng kéo dài)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — recording 1-to-1), GAP-052 (parent portal — message form), Related: child-protection-policy.md §4.2-4.3

- [ ] **AC-COMM-003:** Parent-teacher meeting RSVP — PH nhận giấy mời họp PH định kỳ (4 lần/năm); RSVP qua app (Tham gia / Không tham gia / Cử người khác); calendar.ics auto-add; biên bản post-meeting accessible
  - **Test:** GVCN tạo họp 7A1 cuối HK1 ngày 25/12 18:00–20:00 phòng 7A1 → PH chị Lan nhận push + Zalo + email "Mời họp PH 7A1 cuối HK1" với calendar.ics attached + RSVP buttons; chị Lan tap "Tham gia" → calendar tự thêm; nếu chị Lan bận → "Cử người khác" → form: tên + SĐT người thay (chị/dì/cô) + (optional) gửi credentials tạm thời 1-time access; sau họp 25/12 22:00 → biên bản digital + photo + attendance list available trên portal cho 42 PH (gồm 7 PH absent với lý do); PH có thể reply biên bản nếu không đồng ý
  - **Fail signal:** Phải gọi điện confirm (UX terrible), hoặc no calendar.ics (PH miss meeting do quên), hoặc PH absent không thấy biên bản (vi phạm Đ.83 right to know), hoặc không có "cử người khác" workflow (PH bận buộc miss)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — meeting), GAP-063 (multi-channel invite), Related: TT 32/2020 §parent meeting

- [ ] **AC-COMM-004:** Child safety incident notification — Mandatory reporting per Luật Trẻ em 2016 Đ.51: khi GVCN/Hiệu trưởng phát hiện HS có dấu hiệu bị bạo hành / grooming / bullying, PH phải được thông báo realtime (trừ trường hợp PH là người vi phạm — chuyển safeguarding officer + công an)
  - **Test:** GVCN log "HS A có dấu hiệu bị bullying online từ HS C cùng lớp 15/10" → safeguarding workflow trigger: (1) ticket priority CRITICAL encrypted, (2) auto-assess "PH có khả năng là người vi phạm không?" (default No unless flagged) → No → notification PH chị Lan + bố HS A trong ≤5 phút: "🚨 Sự việc nghiêm trọng về HS A — vui lòng liên hệ GVCN cô Lan/Hiệu trưởng cô X ngay. Số điện thoại: ..." (cấp ưu tiên cao nhất Zalo + SMS + email + phone call); PH có thể join safeguarding meeting với officer trong 24h; full audit log + non-repudiation; nếu PH là người vi phạm → notification skip, chuyển safeguarding officer + công an + Tổng đài 111 per Đ.51
  - **Fail signal:** Không có realtime notification (vi phạm Đ.83 + Luật Trẻ em Đ.51 mandatory reporting), hoặc PH-as-perpetrator case không skip notification (alert tip-off the abuser), hoặc no audit log (không có evidence khi pháp lý), hoặc PH không có channel join safeguarding meeting
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**), Related: child-protection-policy.md §4.4 mandatory reporting

- [ ] **AC-COMM-005:** Complaint escalation hierarchy — PH có thể escalate complaint (về GVCN, GV bộ môn, Hiệu trưởng, ops) theo tier: Level 1 GVCN → L2 Phó CM → L3 Hiệu trưởng → L4 Phòng GD&ĐT (external); SLA mỗi level + auto-escalate; full audit trail
  - **Test:** PH chị Lan submit complaint "GVCN cô Lan đối xử bất công với HS A" → ticket Level 1 assigned cô Lan + Phó CM CC; SLA 5 ngày cô Lan reply; nếu PH unsatisfied → escalate Level 2 (Phó CM) SLA 5 ngày; tiếp Level 3 (Hiệu trưởng) SLA 7 ngày; tiếp Level 4 (Phòng GD) — system export gói tài liệu chuẩn (timeline + evidence + responses) cho PH gửi cơ quan ngoài; full audit trail preserved 7 years; nếu complaint là child safety → auto-route Level 1 trực tiếp safeguarding officer (skip GVCN nếu GVCN là người liên quan)
  - **Fail signal:** Không có escalation workflow (PH phải đến trường trực tiếp), hoặc no SLA per level (kéo dài vô tận), hoặc audit không lưu 7 năm (legal compliance), hoặc child safety không skip GVCN (retaliation risk)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy — escalation), GAP-186 (child safety routing), Related: child-protection-policy.md §4.4

---

## 5. Edge Cases AC

Forgot password, multi-tenant cross-school (con trường khác), divorced parents (joint custody), child safety as victim/perpetrator, account compromise.

- [ ] **AC-EDGE-001:** Forgot password — PH recover qua email + Zalo + SMS (multi-channel), SMS OTP backup nếu PH không truy cập email; recovery audit log; multi-child không lock con khi PH lose access
  - **Test:** PH chị Lan quên password → app/web "Forgot" → input SĐT 09xx → gửi OTP qua SMS + Zalo (parallel cho fail-safe); PH input OTP → reset password; nếu mất luôn SĐT (đổi số) → "Identity verification" workflow: upload CCCD scan + selfie + verify với admin văn thư qua phone call → 24h restore; trong khi recovery, con HS A vẫn login app riêng (con account không bị affect); audit log: parent_id, timestamp, IP, recovery method
  - **Fail signal:** Email-only recovery (PH mất email không khôi phục được), hoặc CCCD verify mất >7 ngày (PH miss đầu năm), hoặc con bị locked khi PH lose access (vi phạm child rights), hoặc no audit log (security incident untrackable)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — recovery), GAP-063 (multi-channel SMS+Zalo), GAP-186 (child rights independence)

- [ ] **AC-EDGE-002:** Divorced parents / joint custody — PH bố + PH mẹ cả 2 có account riêng cho cùng con; quyền access pari/per legal custody (full vs visitation); custody changes admin update timely
  - **Test:** PH chị Lan ly hôn anh Bình; cả 2 vẫn có quyền giám sát HS A theo bản án Tòa án (joint custody); cả 2 account active trên system với note "Joint custody — both parents have full access"; nếu Tòa án giao HS A 100% cho mẹ → PH chị Lan trình quyết định Tòa → admin văn thư update → bố giữ "Limited access — view-only học bạ + báo cáo MOET" (legal minimum), không nhận realtime alert; bulk notifications gửi cả 2 (default unless restricted); financial: cả 2 có thể thanh toán independent → tránh duplicate; all changes audit log
  - **Fail signal:** System chỉ support 1 PH per con (vi phạm Luật Hôn nhân & Gia đình 2014), hoặc không support custody updates (parent change ko reflect), hoặc duplicate fees (cả 2 trả → over-charge), hoặc no audit when custody changes (legal evidence)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — joint custody), GAP-058 (role hierarchy — admin update), Related: Luật Hôn nhân & Gia đình 2014

- [ ] **AC-EDGE-003:** Multi-tenant cross-school — PH có 2 con ở 2 trường khác (HS A trường THCS XYZ, HS B trường tiểu học ABC); 1 PH account aggregate view across tenants HOẶC 2 separate accounts với SSO
  - **Test:** PH chị Lan → option: SSO email cá nhân (chuyenLan@gmail.com) work across 2 tenant accounts (XYZ + ABC); 1 login → home screen 2 sections "Trường THCS XYZ — HS A" + "Trường Tiểu học ABC — HS B"; switch tenant với 1 tap; notifications consolidate (Zalo gửi tên trường rõ ràng); separate billing per tenant nhưng PH có thể combine view "Tổng học phí 2 con: 1.05M tháng 10"
  - **Fail signal:** PH phải tạo 2 accounts riêng cho 2 trường (UX terrible), hoặc no SSO (PH login 2 lần), hoặc notifications confused (không biết là trường nào), hoặc no consolidated billing view
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — multi-tenant), Related: GAP-051 (SSO architecture)

- [ ] **AC-EDGE-004:** Child safety as victim — PH report concern về con (con bị bạo hành ở nhà bạn / bị bạn bắt nạt online / nghi ngờ grooming) qua portal; ticket priority CRITICAL, encrypted, mandatory reporting per Luật Trẻ em Đ.51
  - **Test:** PH chị Lan worried "Con HS A có vẻ sợ đi học từ tuần qua, hỏi không nói gì, nghi ngờ bị bullying" → mở app "Báo cáo an toàn con" → form: child_id, concern category (bullying/abuse/grooming/khác), description, evidence optional → submit → ticket priority CRITICAL encrypted → safeguarding officer + Hiệu trưởng + designated counselor receive (KHÔNG GVCN nếu PH chọn "GVCN có thể liên quan"); 24h follow-up; nếu confirmed CSAM → mandatory reporting Tổng đài 111 + công an + MOLISA per Đ.51; full audit log + non-repudiation
  - **Fail signal:** No safety report channel cho PH (PH chỉ có thể đến công an trực tiếp), hoặc report đi qua GVCN (vi phạm trường hợp GVCN liên quan), hoặc không encrypted (leak risk), hoặc không có mandatory reporting suggestion (vi phạm pháp luật)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**), Related: child-protection-policy.md §4.4

- [ ] **AC-EDGE-005:** Child safety as perpetrator (PH bị nghi ngờ vi phạm) — khi GVCN/safeguarding officer phát hiện dấu hiệu bạo hành từ phía PH, system suppress notification cho PH đó (tip-off prevention) + chuyển công an + MOLISA + Tổng đài 111
  - **Test:** GVCN log "HS A có vết bầm tím + sợ về nhà — nghi ngờ bạo hành gia đình" → safeguarding workflow detect "Possible PH-as-perpetrator case" → flag for review → safeguarding officer + Hiệu trưởng + counselor receive ticket EXCLUDING PH chị Lan + bố Bình (suppress); officer interview HS A discreet; nếu confirmed: report Tổng đài 111 + công an khu vực + MOLISA per Luật Trẻ em Đ.51 + Đ.6; PH chị Lan không nhận notification về sự việc đó (cho đến khi công an cleared); full audit log với "PH-suppress" flag; emergency placement HS A (nếu cần) coordinate với MOLISA
  - **Fail signal:** Notification still gửi PH-perpetrator (tip-off the abuser → child further harm), hoặc không có "PH-as-perpetrator" assessment (default treats all PH as legal guardian), hoặc no integration với Tổng đài 111 / MOLISA / công an, hoặc no emergency placement workflow
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**), Related: child-protection-policy.md §4.4-4.5

---

## 6. Exit / Termination AC

Account deactivation timing (graduation, transfer, divorce custody change, voluntary withdrawal, school closure, parent death).

- [ ] **AC-EXIT-001:** Account deactivation timeline post child graduation — PH active suốt thời gian con học; sau con tốt nghiệp/transfer → 5y archive read-only access cho học bạ + transcript con + invoice history; sau 5y soft-delete với 6mo grace + warning; hard delete trừ legal-hold cases
  - **Test:** HS A tốt nghiệp 31/5/2027 → PH chị Lan account chuyển "Alumni Parent" status: 1/6/2027–31/5/2032 vẫn login được, view-only học bạ HS A + invoice history + biên bản họp PH; sau 31/5/2032 push notification + email "Tài khoản sẽ deactivate 1/12/2032 — vui lòng download tài liệu cuối"; 1/12/2032 hard delete trừ legal-hold (litigation, MOET request); audit log delete event preserved 7 năm; nếu PH có HS B vẫn ở trường → account stays active normally cho HS B, alumni status chỉ apply phần HS A
  - **Fail signal:** Account active forever (PDPL Art 19 minimization violation), hoặc delete sớm hơn 5y (vi phạm TT 32/2020 educational retention), hoặc no grace warning (PH mất tài liệu), hoặc multi-child PH bị deactivate hết khi 1 con tốt nghiệp (con khác vẫn cần access)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (data retention — 5y educational + 6mo soft-delete grace), Related: privacy-policy.md §PDPL Art 19

- [ ] **AC-EXIT-002:** Voluntary withdrawal — PH yêu cầu rút hoàn toàn dữ liệu (right to be forgotten) sau con không còn học tại trường; subject to 5y educational retention legal minimum; right to export tất cả data trước delete
  - **Test:** PH chị Lan submit "Yêu cầu xóa dữ liệu" 1 năm sau con HS A tốt nghiệp → admin DPO (Data Protection Officer) review → response "Educational records bắt buộc lưu 5 năm per TT 32/2020 — không thể delete sớm. Sensitive data (photos, biometric) có thể delete ngay if requested. Educational records sẽ tự động delete 31/5/2032." → PH chấp nhận → admin process: delete photos + biometric immediate; export full data package PDF (educational + financial + communication history) cho PH download trong 30 ngày; audit log
  - **Fail signal:** No right-to-export (vi phạm PDPL Art 18 portability), hoặc delete sensitive data takes >30 days (vi phạm reasonable timeline), hoặc educational retention không clear cho PH (confused về what can/can't delete), hoặc no audit log
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (retention + export), Related: privacy-policy.md §PDPL Art 16-18, data-retention-deletion-policy.md

- [ ] **AC-EXIT-003:** School closure scenario — trường giải thể (per AC-EXIT-004 P5 tenant), PH receive 6mo advance notice + transfer package generate cho con + access to MOET archive 30 năm
  - **Test:** Trường XYZ ra quyết định giải thể 30/6/2027 → 1/4/2027 (6 tháng trước) bulk notification 1200 PH qua Zalo + SMS + email + phone call cho elderly PH "Trường giải thể, con anh/chị chuyển sang trường ABC từ tháng 9/2027"; transfer package auto-generate cho 800 HS; PH có 60 ngày để decide (chấp nhận MOET phân công OR tự xin trường khác); sau 30/6/2027 PH vẫn login system (read-only) cho 30 năm để query học bạ qua MOET portal; financial cleanup: PH refund pro-rated nếu prepaid HP cả năm
  - **Fail signal:** Notification timing <3 months (PH không kịp transition con), hoặc no refund workflow (PH mất tiền prepaid), hoặc no archive access sau closure (PH mất tài liệu cấp 3 / xin việc cho con sau này), hoặc bulk notification mất >30 phút (deliver delays)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (30y retention school closure), Related: TT 32/2020 Đ.40, Luật Lưu trữ 2011

- [ ] **AC-EXIT-004:** Parent death / incapacitation — surviving PH (mẹ qua đời, bố tiếp quản fully) hoặc legal guardian (ông bà nội/ngoại) take over account; admin văn thư handle với evidence (giấy chứng tử + quyết định giám hộ); previous account archive
  - **Test:** PH chị Lan qua đời 5/3/2027; bố HS A là Bình submit "Yêu cầu chuyển quyền giám hộ" → upload giấy chứng tử của vợ + quyết định Tòa giao toàn quyền giám hộ HS A → admin văn thư + Hiệu trưởng review (Hiệu trưởng sign-off cho child safety verification) → 7 ngày approve → bố Bình account upgrade từ "Joint custody" → "Sole custody" với full access; chị Lan account → archive read-only 5 năm + memorial flag (cho purposes future legal queries); HS A receive sensitive notification (counselor support optional); audit log đầy đủ
  - **Fail signal:** Không có death/incapacitation workflow (con bị orphan trên system), hoặc admin approve mất >30 ngày (sole-parent cần access ngay), hoặc no Hiệu trưởng sign-off (child safety verification missing), hoặc audit thiếu (legal evidence khi tranh chấp thừa kế / quyền giám hộ)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — guardianship transfer), GAP-186 (child protection — guardian verification), Related: Luật Hôn nhân & Gia đình 2014

---

## Scoring

**Total ACs:** 29 (sum across 6 categories: 4 + 7 + 4 + 5 + 5 + 4 — exceeds 15-20 target reflecting LEGAL MANDATE complexity per Luật Giáo dục Đ.83 + Luật Trẻ em + PDPL parental consent + multi-fee + multi-child + joint-custody + safeguarding scenarios surface as discrete ACs)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / 29 × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Parent-in-P5 fully supported (production-ready, Đ.83 LEGAL MANDATE compliant, K-12 GA ready for parent layer) |
| 60-84% | ⚠️ Parent-in-P5 partially supported (legal exposure if PH complaints — GA risky, defer K-12) |
| 30-59% | 🔴 Parent-in-P5 NOT supported (LEGAL VIOLATION risk per Luật Giáo dục Đ.83 — block K-12 GA) |
| <30% | ❌ Parent-in-P5 NOT viable — vi phạm pháp luật + child protection — không thể deploy K-12 |

**Pre-review baseline:** Inherits P5 30% baseline + GAP-052 parent portal critical-block status. Parent-specific gaps likely critical: AC-COMM-004 (mandatory child safety notification), AC-COMM-005 (complaint escalation), AC-FIN-001 (multi-fee payment), AC-OPS-001 (real-time daily attendance push), AC-EDGE-005 (PH-as-perpetrator suppress).

---

## Gap Linkage Summary

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-051, GAP-052, GAP-063 | 🔵 OPEN | P0 |
| AC-ONBOARD-002 | TBD | GAP-186, GAP-184 | 🔵 OPEN | **P0 LEGAL** |
| AC-ONBOARD-003 | TBD | GAP-052, GAP-051, GAP-058 | 🔵 OPEN | P0 |
| AC-ONBOARD-004 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-001 | TBD | GAP-052, GAP-060, GAP-063 | 🔵 OPEN | **P0 LEGAL** |
| AC-OPS-002 | TBD | GAP-060, GAP-052 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-052, GAP-054, GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-052, GAP-054 | 🔵 OPEN | P0 |
| AC-OPS-005 | TBD | GAP-059, GAP-052 | 🔵 OPEN | **P0 LEGAL** |
| AC-OPS-006 | TBD | GAP-055, GAP-184 | 🔵 OPEN | **P0 LEGAL** |
| AC-OPS-007 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-062 | 🔵 OPEN | P0 |
| AC-FIN-002 | TBD | GAP-184 | 🔵 OPEN | P0 |
| AC-FIN-003 | TBD | GAP-063, GAP-186 | 🔵 OPEN | P0 |
| AC-FIN-004 | TBD | GAP-051 | 🔵 OPEN | P0 |
| AC-COMM-001 | TBD | GAP-063, GAP-052, GAP-056 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-186, GAP-052 | 🔵 OPEN | **P0 LEGAL** |
| AC-COMM-003 | TBD | GAP-052, GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-004 | TBD | GAP-186 | 🔵 OPEN | **P0 LEGAL CRITICAL** |
| AC-COMM-005 | TBD | GAP-058, GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-001 | TBD | GAP-052, GAP-063, GAP-186 | 🔵 OPEN | P0 |
| AC-EDGE-002 | TBD | GAP-052, GAP-058 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-003 | TBD | GAP-052, GAP-051 | 🔵 OPEN | P0 |
| AC-EDGE-004 | TBD | GAP-186 | 🔵 OPEN | **P0 LEGAL CRITICAL** |
| AC-EDGE-005 | TBD | GAP-186 | 🔵 OPEN | **P0 LEGAL CRITICAL** |
| AC-EXIT-001 | TBD | GAP-184 | 🔵 OPEN | **P0 LEGAL** |
| AC-EXIT-002 | TBD | GAP-184 | 🔵 OPEN | P0 |
| AC-EXIT-003 | TBD | GAP-184 | 🔵 OPEN | P1 |
| AC-EXIT-004 | TBD | GAP-052, GAP-186 | 🔵 OPEN | P1 |

**Legal-mandate ACs (LEGAL tag):** 11 / 29 — reflect Luật Giáo dục Đ.83 + Luật Trẻ em 2016 + PDPL Art 16 + child-protection-policy.md cross-cuts. **3 LEGAL CRITICAL** (AC-COMM-004, AC-EDGE-004, AC-EDGE-005) — vi phạm có thể truy tố hình sự per Luật Trẻ em Đ.51.

**New gaps surfaced:**
- **NEW-1:** Joint custody UX (2 PH accounts cho cùng con với granular permissions per court order) — AC-EDGE-002, sub-feature of GAP-052
- **NEW-2:** Multi-tenant SSO cho cross-school PH (1 PH email, 2+ tenant accounts) — AC-EDGE-003, sub-feature of GAP-052
- **NEW-3:** PH-as-perpetrator suppression workflow + MOLISA / Tổng đài 111 / công an integration — AC-EDGE-005, sub-feature of GAP-186 §4.4-4.5
- **NEW-4:** Hardship payment plan request workflow (giãn HP cho PH gặp khó khăn) — AC-FIN-003 dependency, sub-feature of pricing-model.md
- **NEW-5:** Death/guardianship transfer workflow — AC-EXIT-004, sub-feature of GAP-052 + GAP-186

---

## Cross-References

- **Parent persona:** [`../P5-k12-school.md`](../P5-k12-school.md) — tenant-level AC (36 ACs)
- **Sibling secondary docs:**
  - [`student-in-P5.md`](student-in-P5.md) — pair persona (HS K-12 minor — parental consent recipient)
  - [`teacher-employee-in-P5.md`](teacher-employee-in-P5.md) — GVCN + bộ môn (PH primary contact = GVCN)
  - [`admin-in-P5.md`](admin-in-P5.md) — văn phòng/giáo vụ (PH admin queries)
- **Persona catalog:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas — Parent"
- **Legal docs (CRITICAL — LEGAL MANDATE):**
  - [`../child-protection-policy.md`](../child-protection-policy.md) — child protection (mandatory reporting, parental consent, recording 1-to-1, PH-as-perpetrator suppression)
  - [`../privacy-policy.md`](../privacy-policy.md) — PDPL Art 16 minor data + parental consent + portability + right to be forgotten
  - [`../data-retention-deletion-policy.md`](../data-retention-deletion-policy.md) — 5y educational + 30y school closure + 7y consent audit + 6mo sensitive-minor
  - [`../terms-of-service.md`](../terms-of-service.md) — school-parent contract terms
  - [`../billing-terms.md`](../billing-terms.md) — payment terms, late fees, refunds, hardship
  - [`../refund-dispute-resolution-policy.md`](../refund-dispute-resolution-policy.md) — complaint escalation
  - [`../compliance-scope.md`](../compliance-scope.md) — Vietnam-only PDPL + MOET + Luật Trẻ em scope
- **Cross-linked gaps (12 total):** GAP-051 (bulk import + sibling detection), GAP-052 (parent portal — **CRITICAL**), GAP-053 (academic year), GAP-054 (multi-subject), GAP-055 (báo cáo MOET), GAP-056 (GVCN), GAP-058 (role hierarchy + custody), GAP-059 (conduct), GAP-060 (period attendance), GAP-062 (payment integration), GAP-063 (Zalo + multi-channel), GAP-184 (data retention), GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**)
- **MOET citations:** TT 22/2021 (đánh giá HS — formula + Phụ lục I học bạ), TT 32/2020 (quản lý nhà trường + 5y retention + parent meeting cadence), TT 78/2021 (e-invoice cho private school)
- **Luật citations:**
  - Luật Giáo dục 2019 Đ.83 (parental rights to monitor child academic data)
  - Luật Trẻ em 2016 Đ.6 (child protection principles), Đ.51 (mandatory reporting CSAM/abuse)
  - PDPL Decree 13/2023 Art 16 (parental consent for minor data), Art 18 (portability), Art 19 (minimization)
  - Luật Bảo vệ Quyền lợi NTD 2023 (financial transactions, marketing opt-out)
  - Luật Hôn nhân & Gia đình 2014 (joint custody, divorce, guardianship)
  - Luật Lưu trữ 2011 (30y school archive)
  - Decree 56/2017 (child protection coordination MOLISA + Tổng đài 111)

---

## Reviewer Hat (Phase 2 — for GAP-152 Round 1 multi-stakeholder review)

| Reviewer role | Critical responsibility | Sample stakeholder |
|---------------|------------------------|--------------------|
| **Real K-12 parent (multi-child)** | Validate AC-OPS-007 multi-child UX, AC-FIN-001 multi-fee, AC-COMM-001 Zalo bulk; thanh toán habits | Phụ huynh có 2-3 con cùng/khác trường, mobile-only, 35-45 tuổi |
| **Real K-12 parent (divorced/joint custody)** | Validate AC-EDGE-002 joint custody, AC-EXIT-004 guardianship transfer | Phụ huynh ly hôn với joint custody experience |
| **GVCN representative** | Validate AC-COMM-001 (bulk class), AC-COMM-002 (1-to-1), AC-COMM-005 (complaint chain GVCN level 1) | Tổ trưởng GVCN khối 7+ |
| **Legal counsel (PDPL + child protection)** | Validate AC-ONBOARD-002 parental consent, AC-COMM-004 mandatory reporting, AC-EDGE-005 PH-as-perpetrator, AC-EXIT-001..003 retention | Luật sư PDPL + Luật Trẻ em + MOLISA expert |
| **Product Owner (KiteClass)** | Cross-cut với parent-in-P2/P3 (deferred), identify shared logic vs P5-specific (legal mandate) | @nguyenvankiet acting PO |

**Review process estimate:** 5-7 days (29 ACs × 5 stakeholders, especially legal counsel cho LEGAL CRITICAL items) — defer to GAP-152 Round 1.

---

## How to Use This Doc

1. **Phase 1 (now — 2026-04-30):** AC framework drafted (this file v1, Agent B Wave Secondary-Persona-AC, GAP-153 Phase 1)
2. **Phase 2 (GAP-152 Round 1 review):** 5 stakeholders fill Status (PASS/PARTIAL/FAIL); LEGAL CRITICAL items mandatory review by counsel; new gaps filed for FAIL items not matching existing GAP
3. **Phase 3 (post-review):** ROADMAP updated; coverage % computed; if any LEGAL CRITICAL FAIL → block K-12 GA + escalate to legal review (cannot ship parent feature with mandatory reporting violation per Luật Trẻ em Đ.51)
4. **Phase 4 (quarterly re-review):** Re-score sau wave fix related gaps; track delta against P5 tenant AC + child-protection-policy.md compliance + PDPL evolution

**This doc reviewed every quarter** (per `business-logic-review.md` Quarterly cadence) — re-check Luật Trẻ em / PDPL amendments, MOET TT 22/2021 grade formula updates, Luật Hôn nhân & Gia đình joint custody case law evolution.

---

## Anti-Patterns (specific to parent-in-P5 ACs)

| ❌ Don't | ✅ Do |
|---------|------|
| Treat PH like generic adult user | Recognize LEGAL MANDATE per Đ.83 — PH có quyền pháp lý monitor child, không thể opt-out / hide từ trường |
| Skip parental consent / treat as bulk | Granular per-scope consent per PDPL Art 16 (mandatory vs optional items separated) |
| Single-fee payment + no multi-channel | Multi-fee structure + Vietnamese payment methods (VNPay/MoMo/ZaloPay) + Zalo notifications |
| Email-only complaint channel | Multi-tier escalation (GVCN → Phó CM → Hiệu trưởng → Phòng GD) với SLA + audit trail 7 năm |
| Always notify PH cho child safety incidents | Assess "PH-as-perpetrator" trước → suppress notification nếu confirmed → route công an + MOLISA + 111 per Đ.51 |
| Single PH per con (assume nuclear family) | Joint custody support (2 PH với granular permissions per Tòa) + sole-custody transition workflow |
| Lock con khi PH chậm thanh toán | Restrict features (no submit BTVN mới) but never full lockout — vi phạm child rights |
| Email-only password recovery | Multi-channel (SMS+Zalo+phone call) + CCCD verify cho identity recovery |
| Generic complaint workflow | Sensitive matters cho 1-to-1 với recording option per child-protection-policy.md §4.3 |
| Delete data immediately on request | Subject to 5y educational retention legal minimum + 30y school closure archive — clear comm với PH về what can/can't delete |

---

## Log

- **2026-04-30** — Initial AC set v1 created by Agent B (Wave Secondary-Persona-AC, GAP-153 Phase 1). 29 ACs across 6 categories (LARGEST secondary persona doc — reflects LEGAL MANDATE complexity per Luật Giáo dục Đ.83 + Luật Trẻ em 2016 + PDPL Art 16). Cross-referenced 13 GAP refs + 7 legal/BRD docs + extensive MOET/Luật/Decree citations. **11/20 ACs marked LEGAL** + **3 LEGAL CRITICAL** (mandatory child safety reporting + PH-as-perpetrator suppression) — non-negotiable for K-12 GA, vi phạm có thể truy tố hình sự. Multi-stakeholder Phase 2 review requirement documented (5 reviewer roles including specialized legal counsel cho PDPL + child protection).
- **TBD** (2026-Q3 target) — Phase 2 GAP-152 Round 1 review with multi-stakeholder sign-off (parent multi-child + parent divorced + GVCN + legal counsel + PO).
