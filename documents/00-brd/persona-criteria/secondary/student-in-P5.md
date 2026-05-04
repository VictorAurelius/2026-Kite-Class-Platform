# Acceptance Criteria — Student in P5 Public/Private K-12 School

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Student × P5
**Persona name (VN):** Học sinh trường tiểu học/THCS/THPT
**Persona name (EN):** Student in K-12 School
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent B (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real K-12 student rep + Parent rep + GVCN homeroom teacher + Legal counsel (child protection) + Product Owner (deferred to GAP-152 Round 1 — multi-stakeholder)
**Tier:** 1 Primary (USER PRIORITY tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152 → child-protection-policy.md cross-refs
**Tenant context:** Public/Private K-12 School (P5)
**Role:** Student (secondary persona, minor <16 most cases)
**Strategic priority:** USER PRIORITY persona — most painful K-12 user touches
**Legal compliance:** Luật Trẻ em 2016, PDPL Decree 13/2023 Art 16, MOET regulations TT 22/2021, TT 32/2020, Luật Giáo dục 2019

---

## 0. Context

### Persona scale (within tenant — see [`../P5-k12-school.md`](../P5-k12-school.md) §0 cho tenant-level)
- **Population per tenant:** 500–3000 học sinh / trường (cấp 1: ~500, cấp 2: ~800, cấp 3: ~1500); modal P5 = ~800 HS
- **Per-student data volume:** 12–15 môn × 6 cột điểm/kỳ = ~80–100 điểm / kỳ; 5–10 tiết/ngày × 35 tuần = ~1,250 tiết-điểm-danh / năm; 1 hạnh kiểm Tốt/Khá/TB/Yếu / kỳ; 4 buổi họp PH / năm; multiple BTVN / tuần × 12 môn
- **Usage pattern:**
  - **Daily:** mở app sáng (07:00) xem lịch tiết hôm nay → submit BTVN tối hôm trước → check thông báo GVCN → xem điểm mới
  - **Weekly:** xem hạnh kiểm tuần (T6–T7), nộp BTVN cuối tuần
  - **Semester peak:** cuối HK1 (12/12) + cuối HK2 (5/5) — xem học bạ + báo cáo gửi PH
  - **Annual:** đầu năm 8/9 nhận account từ trường (bulk import) hoặc PH consent grant; cuối lớp 9/12 — xem bằng tốt nghiệp + chuyển cấp

### Profile (typical học sinh K-12 VN)
- **Age:** 6–18 tuổi
  - Tiểu học (lớp 1–5): 6–10 tuổi — minor, parental consent required (PDPL Art 16), parent thay mặt cho hầu hết tương tác
  - THCS (lớp 6–9): 11–14 tuổi — minor, parental consent required, học sinh tự thao tác app cơ bản nhưng PH có quyền giám sát
  - THPT (lớp 10–12): 15–18 tuổi — phần lớn vẫn <16 (lớp 10 = 15t, lớp 11 = 16t), parental consent transitions; lớp 12 typically 17–18 = adult tự consent
- **Tech savviness:** smartphone đa số (Zalo/Facebook native), laptop ít hơn (cấp 3 nhiều hơn cấp 1–2), email không phải kênh quen
- **Primary interaction surface:** mobile app (90%+); web rare khi nộp file BTVN dài
- **Communication channels:** in-app push (notifications GVCN), Zalo (PH-mediated), KHÔNG cho phép DM teacher off-platform per child-protection-policy.md §4.2

### Tenant context dependencies
- **GVCN (Homeroom teacher) primary contact** — mỗi HS thuộc 1 GVCN cụ thể (lớp 7A1 → cô Lan), GVCN handle hạnh kiểm + liên lạc PH + điểm danh đầu giờ
- **Multiple bộ môn teachers** — 12–15 GV bộ môn / lớp / kỳ (Toán cô A, Văn thầy B, ...); HS tương tác bộ môn cho điểm số môn đó
- **Period-based schedule** — không phải "1 lớp, 1 buổi" mà 5–10 tiết / ngày, mỗi tiết 1 GV bộ môn khác → schedule view phải period-granular
- **Parent-mediated payment** — HS KHÔNG thanh toán học phí; PH (parent-in-P5) là financial actor; HS chỉ "see" status (paid/pending) cho transparency
- **Formal MOET artifacts:** học bạ (TT 22/2021 Phụ lục I), sổ điểm (TT 22/2021), bằng tốt nghiệp THCS/THPT (TT 22/2021 Phụ lục II)
- **Child protection cross-cuts:** không off-platform DM với GV (per child-protection-policy.md §4.2); recording option cho 1-to-1 calls; mandatory reporting nếu HS report bullying/grooming (Luật Trẻ em 2016 Đ.51)

### Critical concerns (top 6 — driving AC selection)
1. **Bulk-import account receipt** — HS không tự signup; PH consent → trường IT bulk provision → HS nhận credentials qua PH (cấp 1–2) hoặc qua giấy in tại lớp (cấp 3); first-login UX phải simple, không CCCD/email validation phức tạp
2. **Period-based schedule view** — UI phải hiển thị "tiết 1 / Toán / cô A / phòng B305" KHÔNG "Lớp 7A — buổi sáng"
3. **View formal report card (học bạ) + sổ điểm** — read-only, không thể edit, format MOET-compliant với watermark + electronic signature Hiệu trưởng để verify-able
4. **View conduct grade (hạnh kiểm)** — Tốt/Khá/TB/Yếu hiển thị mỗi kỳ + reasoning từ GVCN (HS phải hiểu vì sao "Khá" để improve)
5. **GVCN communication channel** — primary contact GVCN qua portal (HS reply được limited template messages, không free-form), KHÔNG private chat / off-platform
6. **Parent-mediated payment view** — HS xem status học phí (đã/chưa thanh toán) nhưng action thanh toán = chỉ PH; KHÔNG hiển thị credit card / bank info trong tài khoản HS (PII của parent)

---

## AC Categories (6 standardized)

Each AC: ID, Statement, Test scenario, Fail signal, Status (filled at review time), Linked gap.

---

## 1. Onboarding AC

How student receives credentials → first login → profile bootstrap. Note: bulk-import-driven (PH consent → trường IT provision), không self-signup.

- [ ] **AC-ONBOARD-001:** HS lớp 6–9 nhận credentials lần đầu qua kênh do trường + PH chọn (PH email/Zalo cấp 1–2; in giấy phát tại lớp cấp 3 / lớp 6 lần đầu); first-login complete trong ≤5 phút
  - **Test:** Trường bulk import 800 HS (per AC-ONBOARD-002 P5 tenant-level) → cấp 1–2: PH nhận email "Tài khoản con anh/chị: HS-NguyenVanA@truongabc.edu.vn / temp password Abc123" + Zalo notification → PH chuyển cho con; cấp 3: GVCN in 38 thẻ credential phát đầu năm tại lớp; HS đăng nhập app lần đầu → bắt buộc đổi password (min 6 ký tự, không CCCD-style validation phức tạp) + accept TOS phiên bản trẻ em (simplified) → done
  - **Fail signal:** First-login flow yêu cầu CCCD verification (HS <14 chưa có CCCD), hoặc gửi thẳng credentials qua SMS HS (vi phạm parental control), hoặc TOS không simplified-for-minor (HS không hiểu legalese), hoặc password complexity blocks tiểu học (lớp 1–2 không type được symbol)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import), GAP-052 (parent portal — credentials qua PH), GAP-186 (child protection — TOS simplified-for-minor)

- [ ] **AC-ONBOARD-002:** Profile setup — HS confirm thông tin cơ bản (tên, lớp, ảnh đại diện) + accept simplified TOS phiên bản trẻ em (vi phạm Luật Trẻ em 2016 nếu không có); HS <16 KHÔNG được tự edit thông tin nhạy cảm (ngày sinh, CCCD, địa chỉ) — chỉ PH/GVCN edit được
  - **Test:** HS A login lần đầu → wizard: confirm "Bạn là Nguyễn Văn A, lớp 7A1, GVCN cô Lan? [Yes/No]" → upload avatar (optional, max 2MB, image-only) → đọc TOS-minor 5 dòng (font lớn, ngôn ngữ học sinh hiểu) → click "Tôi đồng ý" → done; HS thử edit DOB → message "Thông tin nhạy cảm chỉ phụ huynh hoặc GVCN sửa được, vui lòng liên hệ"; audit log ghi consent acceptance with timestamp
  - **Fail signal:** Wizard cho HS edit DOB/CCCD (vi phạm child protection minor data), hoặc TOS không simplified (≥1000 từ legalese), hoặc consent audit không lưu (không thể chứng minh khi PDPL audit), hoặc avatar upload không size-cap → DDoS upload từ tiểu học
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — minor data edit lock), GAP-184 (consent audit retention 6mo sensitive-minor), Related: privacy-policy.md §PDPL Art 16

- [ ] **AC-ONBOARD-003:** HS có thể link tài khoản với 1–2 PH accounts (mỗi HS có 1–2 PH tùy gia đình); link được PH initiate trong onboarding bulk; HS read-only thấy "Phụ huynh của bạn: chị Nguyễn Thị Lan (mẹ), anh Nguyễn Văn Bình (bố)"
  - **Test:** Trường bulk import (per AC-ONBOARD-002 tenant-level) đã link parent_id ↔ student_id; HS A login → profile section thấy "Phụ huynh:" với 2 cards (tên + role mẹ/bố + masked SĐT 09**12**89); HS không thể unlink (chỉ admin văn thư + Hiệu trưởng có quyền unlink khi PH ly hôn/qua đời/quyền giám hộ thay đổi)
  - **Fail signal:** HS có thể tự unlink PH (vi phạm Luật Giáo dục Đ.83 quyền giám sát PH), hoặc không hiển thị PH info (HS không biết ai là PH legal trên system → bullying/scam risk), hoặc PII PH leak full SĐT (vi phạm PDPL Art 16 — minor không nên thấy full PII của adult)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — link), GAP-058 (role hierarchy — admin unlink), Related: privacy-policy.md §Minor data + PII masking

---

## 2. Daily Operations AC

Recurring HS workflows: lịch tiết hôm nay, BTVN, điểm số môn, hạnh kiểm view, exam results.

- [ ] **AC-OPS-001:** HS xem lịch tiết hôm nay với period-granular detail (tiết 1–10, môn + GV + phòng) trong ≤2 tap mở app, default to "today"
  - **Test:** HS A 7A1 mở app 07:00 → home screen ngay hiển thị "Hôm nay 15/10 — 8 tiết": Tiết 1 Toán (cô A) phòng B305 07:30–08:15, Tiết 2 Văn (thầy B) phòng A201 08:25–09:10, ..., Tiết 8 Sinh hoạt (cô Lan GVCN) 16:30–17:00; tap tiết → detail (mô tả bài học nếu GV đã upload, link giáo án nếu access cho HS); swipe để xem lịch ngày khác
  - **Fail signal:** Default to "tuần" view (HS phải tap thêm để thấy tiết hôm nay), hoặc UI hiển thị "Lớp 7A1 — buổi sáng" (per-day không period-based — vi phạm K-12 model), hoặc không có thông tin GV + phòng (HS không biết đến phòng nào), hoặc swipe lag trên mobile entry-level (Vivo Y20)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance — schedule view extension), GAP-053 (academic year — semester boundaries)

- [ ] **AC-OPS-002:** HS submit BTVN cho 12+ môn riêng biệt; mỗi BTVN có hạn nộp + status (chưa nộp/đã nộp đúng hạn/nộp trễ), HS upload file (PDF/image/Word, max 10MB) hoặc nhập text inline
  - **Test:** GV Toán assign "BTVN bài 5 trang 23" cho 7A1 hạn 17/10 23:59; HS A mở app tab "Bài tập" → list 8 BTVN due tuần này từ 12 môn → tap "Toán bài 5" → xem mô tả + (optional) file đề kèm theo do GV upload → upload bài làm (scan ảnh từ điện thoại 3MB hoặc PDF từ máy bố mẹ) → submit lúc 16/10 22:00 → status "Đã nộp đúng hạn ✓ 16/10 22:00"; nếu nộp 18/10 → status "Trễ hạn 1 ngày ⚠"; GVCN + PH thấy được
  - **Fail signal:** Không phân biệt 12+ môn (chỉ 1 BTVN list flat), hoặc upload file mất >2 phút (timeout 3G), hoặc không có status late/on-time, hoặc HS không thấy lại bài đã nộp (chỉ submit one-way), hoặc GV không thấy file submission (data lost)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject), Related: GAP-052 (parent portal — PH visibility)

- [ ] **AC-OPS-003:** HS xem điểm số 12+ môn theo cấu trúc TT 22/2021 (TX hệ số 1 + GK hệ số 2 + CK hệ số 3); auto-compute ĐTBmHK + ĐTBmCN; read-only, không edit; HS thấy điểm trong ≤24h sau khi GV publish (không trước approval)
  - **Test:** GV Toán nhập điểm GK1 7A1 vào HK1 → gửi Tổ trưởng duyệt (publishing window) → Tổ trưởng approve → 24h sau HS A mở "Bảng điểm" → "Toán HK1: TX1=7, TX2=8, TX3=8, TX4=9, GK=8, CK=chưa thi" → ĐTBmHK1 partial = (32/4 + 8×2) / 6 = 4.00 (chỉ tính khi đầy đủ); finalize sau CK; HS không thể edit, không thể thấy điểm trước approval
  - **Fail signal:** HS thấy điểm ngay sau GV nhập (vi phạm publishing approval workflow → leak điểm cho HS khác), hoặc công thức ĐTBm sai, hoặc UI không phân biệt TX/GK/CK (HS confused), hoặc không show partial calculation (HS không hiểu mình đang ở mức nào)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-054 (multi-subject), GAP-055 (báo cáo MOET format), Related: TT 22/2021 Đ.7 grade formula

- [ ] **AC-OPS-004:** HS xem hạnh kiểm hàng kỳ (Tốt/Khá/TB/Yếu) + lý do từ GVCN; weekly conduct entries visible (vi phạm + khen thưởng tích lũy); HS có quyền appeal qua GVCN nếu disagree
  - **Test:** Cuối HK1 GVCN cô Lan finalize hạnh kiểm 7A1 → HS A nhận "Hạnh kiểm HK1: Khá" với reasoning "5 vi phạm nội quy (đi muộn 3 lần, không đồng phục 2 lần) + 2 khen thưởng (giúp bạn ốm, hoàn thành tốt nghĩa vụ trực nhật)" — HS xem được full timeline; nếu disagree → button "Khiếu nại với GVCN" → form polite (template-based, không free-form để tránh harsh language) → GVCN nhận ticket trong app
  - **Fail signal:** Không hiển thị reasoning (HS chỉ thấy "Khá" black-box → không thể improve), hoặc không có appeal channel (vi phạm fairness + child rights), hoặc HS có thể edit hạnh kiểm (vi phạm GVCN authority), hoặc free-form complaint allows abusive language
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-059 (conduct tracking), Related: GAP-056 (GVCN), GAP-058 (role hierarchy)

- [ ] **AC-OPS-005:** HS xem kết quả thi giữa kỳ + cuối kỳ trong window publish (≤24h sau approval); xem được đề thi + bài chấm (nếu GV upload), không trước publishing window
  - **Test:** Sau exam window 15–25/12 (per P5 AC-OPS-005 tenant) → 28/12 Hiệu trưởng publish → HS A nhận push notification "Kết quả thi cuối HK1 đã có!" → mở "Kết quả thi" → list 12 môn với điểm + (nếu GV cho phép) link xem bài đã chấm; HS có thể download bài làm của mình PDF; cấm xem bài bạn khác (privacy)
  - **Fail signal:** HS thấy điểm thi trước publishing (leak từ GV nhập), hoặc không thấy bài chấm (HS không học từ lỗi), hoặc xem được bài bạn khác (vi phạm minor PII), hoặc download không hoạt động trên 3G (file size không optimized)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report MOET), Related: GAP-054 (multi-subject)

- [ ] **AC-OPS-006:** HS xem học bạ chính thức (formal report card) cuối năm + cuối cấp; read-only PDF với watermark trường + e-signature Hiệu trưởng; HS download được nhưng không edit
  - **Test:** 31/5/2027 cuối lớp 9 → HS A nhận push "Học bạ THCS đã ban hành" → mở "Tài liệu chính thức" tab → xem học bạ PDF format Phụ lục I TT 22/2021 với watermark "Trường THCS XYZ" + electronic signature Hiệu trưởng + dấu trường digital + QR verification code; download local (5MB) cho purposes thi cấp 3; HS thử screenshot share → cảnh báo "Tài liệu này có watermark cá nhân hóa, vui lòng chỉ dùng đúng mục đích pháp lý"
  - **Fail signal:** Học bạ chỉ HTML view (không downloadable PDF), hoặc không có watermark + signature (verify không được khi nộp trường cấp 3), hoặc HS edit được PDF (security violation), hoặc không dùng QR verification (manual phone call để verify chậm)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (official report card MOET), GAP-184 (5-year retention), Related: TT 22/2021 Phụ lục I

- [ ] **AC-OPS-007:** HS xem điểm danh hằng ngày của bản thân (period-granular từ GVCN + bộ môn); aggregation tuần / tháng / kỳ; phân biệt vắng có phép / không phép / muộn
  - **Test:** HS A mở "Điểm danh" tab → calendar tháng 10 với màu sắc per day (xanh = full attendance, vàng = vắng có phép, đỏ = vắng không phép, cam = muộn); tap day 15/10 → "Tiết 1 ✓, Tiết 2 ✓, Tiết 3 vắng có phép (giấy bệnh viện), Tiết 4 ✓, ..."; weekly summary: "Tuần 7: 35/40 tiết = 87.5%"; semester summary: "HK1: 580/630 tiết = 92%"
  - **Fail signal:** Single per-day attendance only (HS không thấy tiết nào vắng), hoặc không phân biệt 3 trạng thái (just present/absent), hoặc no aggregation (HS không hiểu mình đang ở mức nào), hoặc UI không mobile-friendly (calendar không scroll trên màn hình nhỏ)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance — student view), Related: GAP-052 (parent portal share)

---

## 3. Financial AC

HS không thanh toán trực tiếp; PH (parent-in-P5) là financial actor. HS chỉ "see" status để transparency.

- [ ] **AC-FIN-001:** HS xem status học phí + multi-fee structure (HP, bán trú, đồng phục, BHYT, BHTN, quỹ PH) per tháng / kỳ / năm; KHÔNG thấy credit card / bank account info của PH (PII protection); KHÔNG có nút "Thanh toán" trong app HS
  - **Test:** HS A mở "Học phí" tab → list các khoản: "HP tháng 10: 300k ✓ Đã thanh toán 5/10 (PH đã thanh toán)", "Bán trú tháng 10: 500k (25k × 20 buổi) ✓ Đã thanh toán", "Đồng phục năm: 800k ⏳ Còn nợ 200k", "BHYT 2026–2027: 950k ✓ Đã thanh toán"; HS thử tap khoản nợ → "Vui lòng nhắc phụ huynh thanh toán qua ứng dụng phụ huynh" (KHÔNG có nút pay direct), không hiển thị CC/bank của PH
  - **Fail signal:** HS có nút "Thanh toán" (vi phạm parent-mediated payment model — minor không nên handle financial transactions), hoặc thấy CC/bank PII (vi phạm PDPL Art 16), hoặc không thấy multi-fee breakdown (HS chỉ thấy total → không hiểu cụ thể), hoặc không thấy status (đã/chưa thanh toán → ko transparency)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md (multi-fee), privacy-policy.md (PII masking minor view), GAP-052 (parent portal — payment actor)

- [ ] **AC-FIN-002:** HS xem hóa đơn (invoice) + biên lai (receipt) đã thanh toán dưới dạng PDF; download được cho purposes scholarship application / chứng nhận thu nhập / bố mẹ kế toán
  - **Test:** Sau khi PH thanh toán HP tháng 10 thành công → HS A mở "Hóa đơn" tab → list invoices: "Invoice #INV-20261005-001 — HP tháng 10 — 300k — Đã thanh toán 5/10/2026" → tap → PDF download (1MB, format VAT-compliant per TT 78/2021 nếu trường tư) hoặc receipt simple format (nếu công lập); HS có thể email cho mình lưu trữ
  - **Fail signal:** Không có invoice download (HS phải xin văn thư), hoặc PDF không chuẩn format (scholarship application reject), hoặc không có history (chỉ tháng hiện tại)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md, GAP-051 (export), TT 78/2021 (e-invoice)

---

## 4. Communication AC

GVCN primary contact, multi-bộ môn limited, NO off-platform DM (per child-protection-policy.md), parent-mediated for sensitive matters.

- [ ] **AC-COMM-001:** HS nhận thông báo từ GVCN (announcements, conduct alerts, parent-meeting reminders) qua in-app push + email; HS reply được ngắn (template-based: "Đã nhận", "Có thắc mắc — sẽ hỏi PH/GVCN trên lớp"), KHÔNG free-form chat
  - **Test:** GVCN cô Lan gửi "Thông báo 7A1 ngày 15/10: Họp PH 18/10 18:00 — phụ huynh các em vui lòng tham dự. Các em mang sổ liên lạc về cho PH ký." → HS A nhận push notification + email; mở app → tab "Thông báo" → đọc → reply options: ["✓ Đã nhận", "❓ Em sẽ hỏi PH"] (template-based); HS không thể type free-form (per child-protection-policy.md §4.2 — preventing grooming risk)
  - **Fail signal:** HS có thể free-form DM GVCN (vi phạm child-protection-policy.md §4.2), hoặc không có push notification (HS miss thông báo quan trọng), hoặc không có read-receipt cho GVCN (không track ai đọc), hoặc reply options không cover edge cases (HS phải DM ngoài app → vi phạm)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (portal — student notif), GAP-063 (notification channels), GAP-186 (child protection — no off-platform DM)

- [ ] **AC-COMM-002:** HS có thể request 1-to-1 call với GVCN khi cần (counseling, academic concern); call được record với consent (per child-protection-policy.md §4.3 recording option for 1-to-1); KHÔNG private chat ngoài giờ
  - **Test:** HS A trong app → "Liên hệ GVCN" → "Tôi cần nói chuyện riêng với cô" → button "Đặt lịch gọi" → schedule slot trong office hours của GVCN (08:00–17:00 working days) → GVCN approve → call qua Zoom/Meet integrated, recording auto với prompt "Cuộc gọi này được ghi âm để bảo vệ học sinh và giáo viên — bạn đồng ý?" [Yes/No]; nếu HS chọn No → call vẫn diễn ra nhưng note "Không ghi âm" + cảnh báo nhẹ; ngoài office hours không request được (GVCN không bị quấy rối)
  - **Fail signal:** Không có 1-to-1 channel (HS chỉ có lựa chọn group chat), hoặc không có recording option (vi phạm child-protection-policy.md §4.3 — không có evidence khi tranh chấp), hoặc cho phép call ngoài office hours (vi phạm work-life balance + grooming risk), hoặc consent prompt không clear với minor
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — recording 1-to-1), Related: GAP-052 (parent portal share recording)

- [ ] **AC-COMM-003:** HS nhận thông báo từ GV bộ môn (lớp-level announcements: bài kiểm tra sắp tới, hủy lớp đột xuất, BTVN khẩn) qua in-app + push; cấm GV bộ môn DM HS riêng tư (per child-protection-policy.md)
  - **Test:** GV Toán cô A gửi 7A1 "Tiết Toán mai 16/10 sẽ kiểm tra 15 phút bài 5–6, các em ôn kỹ" → 42 HS 7A1 (gồm HS A) nhận push; HS A mở app thấy thông báo class-level; HS A thử tap "Reply riêng cô A" → blocked với message "Vui lòng liên hệ qua GVCN cô Lan hoặc dùng giờ học để hỏi"
  - **Fail signal:** GV bộ môn có thể DM riêng HS (vi phạm child-protection-policy.md §4.2 — chỉ GVCN có 1-to-1 channel — bộ môn class-level only), hoặc không có push (HS miss bài kiểm tra), hoặc thông báo không persistent (HS xóa tay được → mất evidence)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection), Related: GAP-052 (portal), GAP-058 (role hierarchy — bộ môn vs GVCN scope)

- [ ] **AC-COMM-004:** Parent-mediated communication for sensitive matters — khi GVCN/Hiệu trưởng cần discuss về HS (kỷ luật, hạnh kiểm, transfer), thông báo gửi cả HS + PH; HS không thể "hide" thông báo khỏi PH
  - **Test:** GVCN log incident "HS A đánh bạn 15/10" → workflow: notification gửi PH (qua app PH + Zalo + email — cấp trên), gửi HS (qua app — informed); HS thấy "GVCN đã thông báo phụ huynh về sự việc 15/10. Em sẽ trao đổi với phụ huynh và GVCN sớm." → HS không có nút "Xóa", "Đánh dấu spam"; chỉ "Đã đọc"; PH receive đồng thời và thấy timeline đầy đủ
  - **Fail signal:** HS có thể delete/hide notification từ PH view (vi phạm Luật Giáo dục Đ.83 — parent has right to know), hoặc PH không nhận được khi HS bị kỷ luật (PH bypass), hoặc thông báo không sync timing (HS biết trước PH 1 ngày → có thể "spin" câu chuyện)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — sync), GAP-186 (child protection — incident notification PH), Related: child-protection-policy.md §4.4

---

## 5. Edge Cases AC

Forgot password, transfer mid-year, graduation, child safety incident, account compromise.

- [ ] **AC-EDGE-001:** HS quên password → recovery qua PH (cấp 1–2) hoặc qua GVCN tại trường (cấp 3); KHÔNG email-based reset cho minor (PII risk + parent-not-aware)
  - **Test:** HS A 7A1 (lớp 7 = 12t = minor) quên password → "Forgot password" → option 1: "Nhờ phụ huynh reset (gửi yêu cầu qua app PH)" → PH nhận yêu cầu trong app PH → click "Approve reset" → temp password gửi PH → PH chuyển cho HS; option 2: "Đến gặp GVCN cô Lan tại trường" → GVCN trigger reset trong app GVCN → temp password phát giấy / qua PH; KHÔNG có nút "Email reset link to me" cho HS <16
  - **Fail signal:** Email-based reset cho minor (vi phạm PDPL Art 16 — minor data + email không thuộc minor một mình), hoặc HS có thể tự reset (vi phạm parental control), hoặc reset workflow mất >24h (HS miss class for days), hoặc không có audit log reset events (security risk)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — minor password recovery), GAP-052 (parent portal — reset workflow), Related: privacy-policy.md §Minor data

- [ ] **AC-EDGE-002:** Student transfer mid-year — HS chuyển trường, account old tenant chuyển sang archived state (read-only access còn 5 năm cho học bạ + transcript), HS download transcript trước transfer
  - **Test:** HS A 7A1 chuyển sang trường khác giữa HK1 (per AC-EDGE-001 P5 tenant) → trước close-out HS A mở app: "Trường XYZ — chuẩn bị chuyển trường" → download "Transfer package" PDF (gồm: học bạ partial, điểm danh chi tiết, hạnh kiểm tạm thời) → PH nhận hard copy có dấu trường; sau transfer-out date HS A login app cũ → "Tài khoản đã archive — bạn vẫn xem được dữ liệu cũ + download transcript đến 31/5/2031 (5 năm), không thể submit BTVN, không thể nhận thông báo mới"
  - **Fail signal:** Account locked hoàn toàn sau transfer (HS không thể access học bạ — vi phạm 5y retention), hoặc không có transfer package download cho HS, hoặc HS có thể submit BTVN sau transfer (data conflict với trường mới)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (export), GAP-184 (5y retention), Related: GAP-055 (học bạ format)

- [ ] **AC-EDGE-003:** Student graduation — cuối lớp 9 / lớp 12, HS nhận bằng tốt nghiệp PDF + transcript final + access archived 5 năm; alumni status với limited features
  - **Test:** 31/5/2027 cuối lớp 9 → HS A nhận push "Tốt nghiệp THCS! Tài liệu đã sẵn sàng" → mở app: download bằng tốt nghiệp THCS PDF (format Phụ lục II TT 22/2021, watermark + signature + QR), download học bạ final, download tổng kết hạnh kiểm 4 năm; account chuyển sang "Alumni" status cho 5 năm: vẫn login được nhưng chỉ xem documents + 1 channel hỏi văn thư (không submit BTVN, không nhận thông báo lớp); sau 5 năm soft-delete với 6 tháng grace + email warning
  - **Fail signal:** Không có alumni access (HS bị khóa ngay sau tốt nghiệp → mất tài liệu cho thi đại học), hoặc bằng tốt nghiệp format sai TT 22/2021 (verify cấp trên reject), hoặc không có 5y retention sau tốt nghiệp (vi phạm TT 32/2020)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (5y retention), GAP-055 (báo cáo), Related: TT 22/2021 Phụ lục II, TT 32/2020 Đ.40

- [ ] **AC-EDGE-004:** Child safety incident — HS report bullying / grooming / abuse qua app; ticket priority CRITICAL, encrypted, only safeguarding officer + Hiệu trưởng + designated counselor see; mandatory reporting per Luật Trẻ em 2016 Đ.51 nếu có CSAM
  - **Test:** HS A mở app → "Báo cáo an toàn" (button visible từ home screen, không bị nested) → form: "Em đang gặp vấn đề gì? [bị bắt nạt / bị đe dọa / bị quấy rối / khác]" → describe sự việc → optional: upload evidence (screenshot/photo) → submit → ticket priority CRITICAL với tag "Child safety" → notification gửi safeguarding officer + Hiệu trưởng + counselor (KHÔNG gửi GVCN nếu HS chọn "GVCN có thể là người liên quan" để tránh retaliation); evidence preserved encrypted; HS nhận confirmation "Tin nhắn của em đã được nhận và sẽ được xử lý bí mật trong 24h"
  - **Fail signal:** Không có safety report channel (HS không có cách an toàn để báo), hoặc report đi qua GVCN (vi phạm trường hợp GVCN là kẻ vi phạm), hoặc không encrypted (PII leak nếu hệ thống bị hack), hoặc không có mandatory reporting suggestion cho CSAM (vi phạm Luật Trẻ em 2016 Đ.51 + Decree 56/2017)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — **CRITICAL/LEGAL MANDATE**), Related: child-protection-policy.md §4.4 mandatory reporting

- [ ] **AC-EDGE-005:** Account compromise — HS tài khoản bị hack hoặc nghi ngờ session hijack (someone đăng nhập từ device lạ); HS hoặc PH có thể trigger emergency lockout; audit log event preserved 1 năm
  - **Test:** HS A bỗng thấy app hiển thị thông báo lạ không phải do mình → login từ thiết bị khác để check → "Account Activity" tab thấy login lạ từ IP xx.xx.xx.xx (location: Sài Gòn, A ở Hà Nội) → tap "This wasn't me" → confirm với PH password (parental authority for minor account) → emergency lockout: tất cả sessions force-logout, password auto-reset, notification GVCN + Hiệu trưởng + IT staff; audit log event lưu 1 năm cho investigation
  - **Fail signal:** Không có account activity view (HS không biết hack), hoặc HS có thể tự lockout không cần PH (vi phạm parental authority cho minor — risk false-positive khi HS giận dỗi), hoặc không có audit log preservation (mất evidence cho công an khi cần)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection), GAP-184 (audit log retention), Related: privacy-policy.md §Security incident

---

## 6. Exit / Termination AC

Account deactivation timing per HS lifecycle (graduation, transfer-out, voluntary withdraw, school closure).

- [ ] **AC-EXIT-001:** Account deactivation timeline — HS active suốt thời gian học tại trường; sau graduation/transfer/withdraw → 5y archive read-only (TT 32/2020 + 5y educational retention) → 6mo soft-delete grace với email notification → hard delete (sensitive minor data PDPL Art 16 — 6mo sau soft-delete trigger)
  - **Test:** HS A tốt nghiệp 31/5/2027 → 1/6/2027–31/5/2032 alumni read-only access (5y); 1/6/2032 push notification + PH email "Tài khoản sẽ bị xóa vĩnh viễn 1/12/2032 — vui lòng download tài liệu cuối"; 1/12/2032 hard delete trừ legal-hold cases (litigation, MOET request); audit log delete event preserved 7y per legal compliance
  - **Fail signal:** Account active forever (không có deletion → vi phạm PDPL Art 19 minimization), hoặc delete sớm hơn 5y (vi phạm TT 32/2020), hoặc không có 6mo grace warning (HS mất tài liệu mà không biết), hoặc delete event không audit log (không thể chứng minh khi PDPL audit)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (data retention — 5y educational + 6mo sensitive-minor), Related: data-retention-deletion-policy.md, privacy-policy.md §PDPL Art 16

- [ ] **AC-EXIT-002:** School closure scenario — trường giải thể (per AC-EXIT-004 P5 tenant), HS A account migrate sang trường mới (MOET phân công) HOẶC archive 30 năm trong MOET storage; HS access learning history qua MOET portal sau closure
  - **Test:** Trường XYZ giải thể 30/6/2027 → 6 tháng trước (1/4/2027) HS A nhận push "Trường giải thể, em chuyển sang trường ABC từ tháng 9/2027" + transfer package generate cho all 800 HS; sau 30/6/2027 account local tenant locked, data archive 30y (Luật Lưu trữ 2011); HS A query học bạ 2030 → MOET portal hỗ trợ search old archives qua mã HS + CCCD; transition seamless không bị mất
  - **Fail signal:** Data bị xóa khi trường giải thể (vi phạm Luật Lưu trữ 30y), hoặc HS không có channel access archive sau closure (mất tài liệu cấp 3 / xin việc), hoặc transfer workflow không integrate với MOET phân công (HS phải tự đi xin nhập học)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (retention 30y school closure), Related: TT 32/2020 Đ.40, Luật Lưu trữ 2011

---

## Scoring

**Total ACs:** 23 (sum across 6 categories: 3 + 7 + 2 + 4 + 5 + 2 — exceeds 15-20 target reflecting USER PRIORITY persona breadth: minor-specific constraints, MOET formal artifacts, child protection cross-cuts surface as discrete ACs not collapsible)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / 23 × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Student-in-P5 fully supported (production-ready for K-12 student UX) |
| 60-84% | ⚠️ Student-in-P5 partially supported (usable but with friction; defer GA full K-12) |
| 30-59% | 🔴 Student-in-P5 NOT supported (major UX gaps; not production-ready for student-facing K-12 deployment) |
| <30% | ❌ Student-in-P5 NOT viable (current K-12 30% baseline confirms student touchpoints critical-block) |

**Pre-review baseline:** Inherits P5 30% baseline — student-specific gaps likely in: AC-OPS-002 (multi-subject BTVN), AC-OPS-006 (formal report card), AC-COMM-002 (1-to-1 GVCN call), AC-EDGE-004 (child safety report), AC-EDGE-001 (minor password recovery).

---

## Gap Linkage Summary

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-051, GAP-052, GAP-186 | 🔵 OPEN | P0 |
| AC-ONBOARD-002 | TBD | GAP-186, GAP-184 | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | GAP-052, GAP-058 | 🔵 OPEN | P0 |
| AC-OPS-001 | TBD | GAP-060, GAP-053 | 🔵 OPEN | P0 |
| AC-OPS-002 | TBD | GAP-054, GAP-052 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-054, GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-059, GAP-056, GAP-058 | 🔵 OPEN | P0 |
| AC-OPS-005 | TBD | GAP-055, GAP-054 | 🔵 OPEN | P0 |
| AC-OPS-006 | TBD | GAP-055, GAP-184 | 🔵 OPEN | **P0 LEGAL** |
| AC-OPS-007 | TBD | GAP-060, GAP-052 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-FIN-002 | TBD | GAP-051 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-052, GAP-063, GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-COMM-002 | TBD | GAP-186, GAP-052 | 🔵 OPEN | **P0 LEGAL** |
| AC-COMM-003 | TBD | GAP-186, GAP-052, GAP-058 | 🔵 OPEN | P0 |
| AC-COMM-004 | TBD | GAP-052, GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-001 | TBD | GAP-186, GAP-052 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-002 | TBD | GAP-051, GAP-184, GAP-055 | 🔵 OPEN | P0 |
| AC-EDGE-003 | TBD | GAP-184, GAP-055 | 🔵 OPEN | P0 |
| AC-EDGE-004 | TBD | GAP-186 | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-005 | TBD | GAP-186, GAP-184 | 🔵 OPEN | P0 |
| AC-EXIT-001 | TBD | GAP-184 | 🔵 OPEN | **P0 LEGAL** |
| AC-EXIT-002 | TBD | GAP-184 | 🔵 OPEN | P1 |

**Legal-mandate ACs (LEGAL tag):** 7 / 23 — reflect Luật Trẻ em 2016 + PDPL Art 16 + child-protection-policy.md cross-cuts. These cannot ship "PARTIAL" for K-12 GA.

**New gaps surfaced:**
- **NEW-1:** Simplified TOS-for-minor (font lớn, ngôn ngữ HS hiểu) — AC-ONBOARD-002 dependency, sub-feature of GAP-186
- **NEW-2:** Template-based reply UX cho HS-GVCN (cấm free-form) — AC-COMM-001 dependency
- **NEW-3:** Recording option in 1-to-1 calls với consent prompt — AC-COMM-002 dependency, sub-feature of GAP-186 §4.3

---

## Cross-References

- **Parent persona:** [`../P5-k12-school.md`](../P5-k12-school.md) — tenant-level AC (36 ACs, 6 categories)
- **Sibling secondary docs:**
  - [`parent-in-P5.md`](parent-in-P5.md) — pair persona (parent legal mandate)
  - [`teacher-employee-in-P5.md`](teacher-employee-in-P5.md) — GVCN + bộ môn workflow
  - [`admin-in-P5.md`](admin-in-P5.md) — văn phòng/giáo vụ workflow
  - [`student-in-P2.md`](student-in-P2.md) / [`student-in-P3.md`](student-in-P3.md) — sibling student personas (different scale, no MOET parent legal mandate)
- **Persona catalog:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas — Student"
- **Legal docs:**
  - [`../child-protection-policy.md`](../child-protection-policy.md) — child protection (mandatory reporting, no off-platform DM, recording 1-to-1, parental consent)
  - [`../privacy-policy.md`](../privacy-policy.md) — PDPL Art 16 minor data + parental consent
  - [`../data-retention-deletion-policy.md`](../data-retention-deletion-policy.md) — 5y educational + 6mo sensitive-minor
  - [`../terms-of-service.md`](../terms-of-service.md) — school-parent contract (TOS-minor variant needed)
  - [`../compliance-scope.md`](../compliance-scope.md) — Vietnam-only PDPL + MOET + Luật Trẻ em scope
- **Cross-linked gaps (12 total):** GAP-051 (bulk import), GAP-052 (parent portal), GAP-053 (academic year), GAP-054 (multi-subject), GAP-055 (báo cáo MOET), GAP-056 (GVCN), GAP-058 (role hierarchy), GAP-059 (conduct), GAP-060 (period attendance), GAP-063 (SMS/Zalo), GAP-184 (data retention), GAP-186 (child protection — **CRITICAL**)
- **MOET citations:** TT 22/2021 (đánh giá HS — grade formula + Phụ lục I học bạ + Phụ lục II bằng tốt nghiệp), TT 32/2020 (quản lý nhà trường + 5y retention), TT 78/2021 (e-invoice for private school)
- **Luật citations:** Luật Trẻ em 2016 (Đ.51 mandatory reporting, Đ.25 staff vetting), PDPL Decree 13/2023 Art 16 (parental consent + minor data), Luật Giáo dục 2019 Đ.83 (parent monitoring rights), Luật Lưu trữ 2011 (30y school archive)

---

## Reviewer Hat (Phase 2 — for GAP-152 Round 1 multi-stakeholder review)

| Reviewer role | Critical responsibility | Sample stakeholder |
|---------------|------------------------|--------------------|
| **Real K-12 student rep** | Validate UX simplicity (cấp 1–2 typing, mobile-first, 3G performance) | HS lớp 7 + lớp 11, mobile-only access |
| **Real K-12 parent rep** | Validate parent-mediated payment + parental consent flow + AC-EDGE-001 password recovery | Phụ huynh có 2+ con cùng trường |
| **GVCN homeroom teacher** | Validate AC-COMM-001..004, AC-OPS-004 conduct visibility, AC-EDGE-005 account compromise alert flow | Tổ trưởng GVCN khối 7 |
| **Legal counsel (child protection)** | Validate AC-COMM-001..004 (no off-platform DM), AC-EDGE-004 (mandatory reporting), AC-EXIT-001 (PDPL retention) | Luật sư trẻ em + Luật Trẻ em 2016 expert |
| **Product Owner (KiteClass)** | Cross-cut với student-in-P2/P3 — identify shared logic vs K-12 minor-specific | @nguyenvankiet acting PO |

**Review process estimate:** 4-5 days (23 ACs × 5 stakeholders, simpler than tenant-level AC due to focused scope) — defer to GAP-152 Round 1.

---

## How to Use This Doc

1. **Phase 1 (now — 2026-04-30):** AC framework drafted (this file v1, Agent B Wave Secondary-Persona-AC, GAP-153 Phase 1)
2. **Phase 2 (GAP-152 Round 1 review):** 5 stakeholders fill Status (PASS/PARTIAL/FAIL); new gaps filed for FAIL items not matching existing GAP
3. **Phase 3 (post-review):** ROADMAP updated; coverage % computed; if <60% → block K-12 student-facing GA; if 60-84% → defer K-12 student feature priorities
4. **Phase 4 (quarterly re-review):** Re-score sau wave fix related gaps; track delta against P5 tenant AC coverage

**This doc reviewed every quarter** (per `business-logic-review.md` Quarterly cadence) — re-check Luật Trẻ em / PDPL amendment, MOET TT 22/2021 grade formula updates.

---

## Anti-Patterns (specific to student-in-P5 ACs)

| ❌ Don't | ✅ Do |
|---------|------|
| Treat HS K-12 like adult user (free-form chat, email reset, direct payment) | Recognize minor-specific constraints: parental consent, no off-platform DM, parent-mediated payment, parental password recovery |
| Skip parent visibility cho HS communications | AC-COMM-004 mandates parent-sync per Luật Giáo dục Đ.83 |
| Single per-day attendance UI cho HS | Period-based view (5–10 tiết/ngày) — fundamental K-12 model |
| Generic TOS cho minor accounts | Simplified TOS-for-minor (NEW-1) per child-protection-policy.md |
| Free-form DM giữa HS với GV bộ môn | Class-level announcements only, 1-to-1 chỉ qua GVCN với recording option |
| Email-based password reset cho HS <16 | Parental + GVCN-mediated recovery (AC-EDGE-001) per PDPL Art 16 |
| HS có nút "Pay" trong app | Parent-mediated payment — HS view-only AC-FIN-001 |

---

## Log

- **2026-04-30** — Initial AC set v1 created by Agent B (Wave Secondary-Persona-AC, GAP-153 Phase 1). 23 ACs across 6 categories (focused USER PRIORITY scope cho HS K-12 minor — bulk import receipt, period-based schedule, formal MOET artifacts, GVCN comm, parent-mediated payment, child protection cross-cuts). Cross-referenced 12 GAP refs + 6 legal/BRD docs + extensive MOET/Luật citations. 7/18 ACs marked LEGAL — non-negotiable for K-12 student-facing GA. Multi-stakeholder Phase 2 review requirement documented (5 reviewer roles).
- **TBD** (2026-Q3 target) — Phase 2 GAP-152 Round 1 review with multi-stakeholder sign-off (student rep + parent rep + GVCN + legal + PO).
