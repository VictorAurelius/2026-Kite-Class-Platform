---
title: Persona Review — Phase 1 BETA walkthrough
status: complete
created: 2026-05-14
phase: pre-wave-73
personas: [P1_solo_lan, P1_solo_hung, P2_center_thuy, edge_guitar_bao]
related_waves: [73, 74]
related_gaps: [GAP-063, GAP-372, GAP-378, GAP-376]
---

# Persona Walkthrough — Phase 1 BETA

## Scope

4 nhân vật × 7 bước = 28 ô khảo sát. Mục tiêu: surface gap dev miss vì đã quá quen hệ thống. Đối chiếu với dev brainstorm hiện tại (Wave 73 = email audit, user manual, Tally feedback, UI smoke; Wave 74 = Stripe sandbox) để đánh giá xem scope dev đề xuất có đủ cho beta user thực sự không.

## Phương pháp

Đóng vai mỗi nhân vật một cách thực tâm trong 5-7 phút mỗi bước. Đặt 5 câu "tại sao?" mỗi điểm kẹt. Không cố gắng "fix bug" — chỉ ghi nhận trải nghiệm + lo lắng + kỳ vọng. Bao gồm cả tâm lý, văn hoá VN, tin tưởng, không chỉ UI.

Reference:
- `documents/00-brd/personas-catalog.md` §Tier 1 P1/P2
- `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` (126 dòng)
- `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action (Wave 72b candidate scope)

---

## 4 Nhân vật

### Persona 1 — Cô giáo Lan (42, IELTS online Hà Nội, 20 hs)
- Profile: đã có 20 học sinh trả 3-5tr/khoá; dùng Zalo group + Google Sheet trước đây
- Tech-savvy: trung bình — biết Gmail, Zalo, Google Drive, Excel cơ bản; không tự setup domain bao giờ
- Lý do dùng KiteHub: bạn (cô Hương) giới thiệu, hứa sẽ "đỡ tốn công quản lý"
- Lo lắng: mất data Google Sheet cũ; học sinh không quen app mới; có phải trả tiền không

### Persona 2 — Thầy Hùng (28, mới ra trường, lớp 10 môn Toán)
- Profile: 0 học sinh, đang xây thương hiệu cá nhân
- Tech-savvy: cao — biết code Python, dùng Notion, Discord, có YouTube channel
- Lý do dùng KiteHub: muốn có website + dashboard chuyên nghiệp để pitch phụ huynh
- Lo lắng: chưa có doanh thu nên không muốn cam kết tiền hàng tháng; cần "trông professional"

### Persona 3 — Cô Thuý (50, chủ trung tâm Anh ngữ 80 hs, Đà Nẵng)
- Profile: trung tâm 5 giáo viên + 80 học sinh; thu học phí theo quý
- Tech-savvy: thấp — dùng Excel + nhờ nhân viên gõ, từng dùng Misa Kế toán
- Lý do dùng KiteHub: con gái (nhân viên) bảo thử "vì rẻ hơn phần mềm trung tâm 1tr/tháng"
- Lo lắng: nhân viên không quen tool mới sẽ kêu; mất data sổ giấy 5 năm; phụ huynh hỏi hoá đơn đỏ

### Persona 4 — Anh Bảo (35, dạy guitar online YouTube, 200 sub)
- Profile: 5-10 học viên trả 200-500k/khoá; bán khoá ghi sẵn trên YouTube
- Tech-savvy: cao — biết OBS, Premiere, Stripe
- Lý do dùng KiteHub: bạn share trên Facebook group "Solo Educator VN" — tò mò
- Lo lắng: không biết app có support dạy nhạc không (vs. chỉ dạy academic); có thể tích hợp YouTube không

---

## Kết quả từng nhân vật

### Persona 1 — Cô giáo Lan

#### Bước 1 — Pre-invite (nhận email mời / nghe bạn giới thiệu)
**Tôi nghĩ gì:** Cô Hương gửi link `kitehub.me/beta` qua Zalo, nói "tao đang dùng, mày thử xem". Tôi click thử trên điện thoại Samsung.

**Vướng:** Trang chủ load chậm 3 giây trên 4G. Tôi đọc tagline "SaaS quản lý trung tâm giáo dục" — không hiểu "SaaS" là gì. "Trung tâm" — tôi không phải trung tâm, tôi gia sư cá nhân, có dùng được không?

**Gap surface:**
- **PR-LAN-1**: Landing page không có ngôn ngữ phù hợp với P1 Solo Teacher — toàn cụm từ "trung tâm/center/SaaS" → P1 nghĩ không phải target → bỏ ngang (P1)
- **PR-LAN-2**: Không có demo video 1-phút "đây là Lan, đây là cách tôi quản lý 20 hs" → P1 mới không hiểu giá trị (P2)

**Tại sao? × 5:** (1) Vì landing copy hướng B2B (2) Vì dev viết theo persona P2 default (3) Vì chưa có persona-specific landing variant (4) Vì chưa có visual proof (5) Vì assume user đọc kỹ — thực tế P1 lướt 3 giây.

---

#### Bước 2 — Beta access request
**Tôi nghĩ gì:** Bấm "Request Beta Access". Form xuất hiện: email, full name, organization, role, expected size, source, message.

**Vướng:**
- Field "Organization" — tôi không có tổ chức, gõ "cá nhân"? "Lớp IELTS Lan"? Bắt buộc?
- Field "Expected size" dropdown — chỉ có "1-50 / 50-200 / 200+" → tôi 20 hs, chọn 1-50 nhưng cảm thấy lạc lõng
- Submit → "Yêu cầu đã gửi, chờ admin duyệt 1-3 ngày làm việc" → 1-3 ngày?? Tôi muốn thử ngay
- Không nhận được email xác nhận đã submit → tôi sợ form fail, submit lại lần 2

**Gap surface:**
- **PR-LAN-3** (P0): Sau submit form, KHÔNG có email "đã nhận yêu cầu của bạn" → user mất tin tưởng, submit duplicate (P0)
- **PR-LAN-4** (P1): SLA "1-3 ngày" quá dài cho solo user impatient → cân nhắc auto-approve cho P1 (FREE tier, no risk)
- **PR-LAN-5** (P2): Form không có persona-specific copy — "Bạn dạy gia sư hay quản lý trung tâm?" để route flow

**Tại sao? × 5:** (1) SLA cài đặt theo thinking "admin sẽ duyệt" mà chưa có admin (2) Vì chưa có Resend transactional template cho auto-confirm (3) Vì không có rate-limit dedup bằng email → user spam (4) Vì assume admin sẽ click duyệt fast — thực tế dev solo (5) Vì chưa có FAQ "tại sao phải duyệt?"

---

#### Bước 3 — Email verify + provisioning
**Tôi nghĩ gì:** 2 ngày sau (cô Hương nhắn admin duyệt rồi), tôi nhận email "Welcome to KiteHub, click link to set password".

**Vướng:**
- Email vào tab "Spam" của Gmail → tôi suýt missed (vì sender `noreply@kitehub.me` chưa warm-up domain)
- Link → trang set password yêu cầu min 12 chars + symbol + digit → tôi quen password 8 chars, phải nghĩ mất 2 phút
- Sau set password → thấy luôn dashboard rỗng, không có hướng dẫn next step

**Gap surface:**
- **PR-LAN-6** (P0): Email deliverability — domain reputation chưa warm-up, vào spam (P0)
- **PR-LAN-7** (P1): Password complexity rule quá strict cho P1 (không phải PLATFORM_ADMIN) → consider giảm xuống 10 chars hoặc passphrase mode (P1)
- **PR-LAN-8** (P1): Welcome email không nhắc "check Spam folder nếu không thấy" (P2)
- **PR-LAN-9** (P0): Sau set password, dashboard không onboarding modal hỏi "Tên trung tâm? Loại hình? Số hs hiện tại?" → user lạc (P0)

**Tại sao? × 5:** (1) Vì domain mới chưa có DKIM/DMARC warm reputation (2) Vì password rule copy từ enterprise security best practice (3) Vì chưa có FE empty-state design cho dashboard (4) Vì onboarding wizard đã có (GAP-372) nhưng có thể chưa trigger đúng (5) Vì assume user sẽ tự khám phá

---

#### Bước 4 — Onboarding wizard (6 bước AI Branding)
**Tôi nghĩ gì:** Có 1 modal "Hãy thiết kế trung tâm của bạn!" với 6 bước. Tôi click bắt đầu.

**Vướng:**
- Bước 1 Welcome OK
- Bước 2 Upload logo — tôi không có logo, có thể skip? Nút "Bỏ qua" nhỏ phía cuối, tôi tưởng bắt buộc → mất 5 phút tìm logo Canva
- Bước 3 Audience: "Trẻ em / Thanh thiếu niên / Người lớn / Doanh nghiệp" — tôi dạy IELTS toàn 16-22 tuổi, chọn "Thanh thiếu niên" OR "Người lớn"? Confused
- Bước 4 Tone: "Professional / Friendly / Energetic / Luxurious" — toàn English, tôi không hiểu "Luxurious" = ? → google
- Bước 5 Template: 6 preview hiển thị bằng SVG generic, không giống IELTS center → tôi không cảm thấy "đây là mình"
- Bước 6 Preview — banner sinh ra có chữ "Welcome to YOUR CENTER" → đáng lẽ phải là tên tôi cho? Đâu rồi field tên?

**Gap surface:**
- **PR-LAN-10** (P1): Wizard "Bỏ qua" CTA không đủ visible — user feel locked-in (P1)
- **PR-LAN-11** (P2): Audience preset không cover "gia sư cá nhân + adult learner mix" — cần persona-aware preset (P2)
- **PR-LAN-12** (P1): Tone labels chưa có VN translation hoặc tooltip giải thích (P1)
- **PR-LAN-13** (P0): Template gallery không có industry-specific (IELTS / Toán / Anh ngữ / Nhạc) — toàn generic (P1)
- **PR-LAN-14** (P0): Banner generated text chưa pull từ user profile (tên trung tâm) → preview confusing (P0)

**Tại sao? × 5:** (1) Vì wizard design priority "speed" hơn "personalization" (2) Vì preset list dựa trên persona-catalog default thay vì user actual (3) Vì i18n strategy chưa cover wizard labels (4) Vì template library generic (GAP-011 P0 pending) (5) Vì wizard chưa request user info trước generate

---

#### Bước 5 — First lớp + first student
**Tôi nghĩ gì:** Dashboard có nút "Tạo lớp đầu tiên". Click → form xuất hiện.

**Vướng:**
- Form yêu cầu: tên lớp, môn học, lịch học (weekly), giáo viên (dropdown — chỉ có tôi), tuition, ngày bắt đầu
- "Môn học" dropdown — IELTS có không? "Anh văn" / "Tiếng Anh giao tiếp" / "Custom"? Tôi gõ "IELTS Speaking" custom → save
- "Lịch học weekly" — tôi dạy mỗi học sinh 2 buổi tự chọn theo lịch riêng → KiteHub assume cohort fix → tôi confuse, không biết gán per-student schedule như nào
- Tạo lớp xong → muốn thêm học sinh → form ask email học sinh → đa số học sinh 16-18 tuổi của tôi không có email riêng, chỉ Zalo → tôi stuck
- Bulk import students? Tìm trên menu — không thấy

**Gap surface:**
- **PR-LAN-15** (P0): Schedule model assume cohort-based, không support per-student timeslot (1-on-1 gia sư pattern phổ biến nhất VN) (P0)
- **PR-LAN-16** (P0): "Email học sinh" required nhưng K-12/teen VN không phổ biến email → cần option "Zalo phone" OR "guardian email" (P0)
- **PR-LAN-17** (P1): Subject preset không có IELTS / TOEIC / SAT — toàn generic "Anh văn" (P1)
- **PR-LAN-18** (P1): Bulk import students chưa visible cho P1 (GAP-051 marked K-12 only nhưng P1 cũng cần 20 hs at once) (P1)
- **PR-LAN-19** (P2): Tuition currency phải VND, không có USD/auto-convert (P2)

**Tại sao? × 5:** (1) Vì class entity design copy LMS Western (cohort) thay vì gia sư VN (1-on-1) (2) Vì email là user identifier convention — chưa support phone-based identity (3) Vì subject taxonomy chưa research field VN education (4) Vì GAP-051 scoped K-12 only — bỏ qua P1 use case (5) Vì assume user paid via gateway — chưa account cash collection

---

#### Bước 6 — Sử dụng hằng ngày (điểm danh, nhập điểm, thanh toán)
**Tôi nghĩ gì:** Sau 1 tuần tôi quay lại, muốn điểm danh buổi học chiều nay.

**Vướng:**
- Mobile dashboard chậm 4s khi load class list trên 4G (3 lớp × 20 hs)
- Điểm danh trên mobile — checkbox quá nhỏ trên Samsung Galaxy A series → bấm nhầm tên
- Sau điểm danh, không có notification gửi cho phụ huynh dù tôi đã add email guardian → nhưng chưa enable Zalo OA → user mong đợi tự động
- Nhập điểm — chỉ có thang điểm 10 → IELTS scale 0-9 không fit; tôi phải gõ "7.5" → app reject "max 10 OK but decimal limit 0.5" → confuse
- Thanh toán — học sinh trả tiền mặt cho tôi → tôi mark "Paid" thủ công → KiteHub mark là "cash" → không có invoice tự động → phụ huynh hỏi biên lai → tôi không có

**Gap surface:**
- **PR-LAN-20** (P0): Mobile performance — chậm trên 4G + small touch targets (P0)
- **PR-LAN-21** (P0): Notification engine chưa wire (GAP-063 P1→P0 cross-persona blocker) — email guardian works manual, Zalo OA chưa setup (P0)
- **PR-LAN-22** (P1): Grading scale rigid 10 — cần support IELTS (0-9), TOEFL (0-120), pass/fail, letter (A-F) (P1)
- **PR-LAN-23** (P0): Cash payment workflow chưa có invoice template tự động — phụ huynh hỏi biên lai mà không có (P0)
- **PR-LAN-24** (P2): Theo dõi "hôm nay dạy ai" — chưa có "Today" view cho teacher (S persona có nhưng teacher chưa) (P2)

**Tại sao? × 5:** (1) Vì mobile perf chưa optimize (lazy load list) (2) Vì notification = GAP-063 chưa ship, không có quick win Email (3) Vì grade scale design 1-table cho mọi subject (4) Vì invoice flow chỉ cho online payment gateway (5) Vì teacher persona = secondary, ít attention vs Owner

---

#### Bước 7 — Off-boarding hoặc retention (sau 2-4 tuần)
**Tôi nghĩ gì:** Sau 3 tuần, tôi cảm thấy không quen, muốn quay lại Google Sheet. Tôi vào Settings tìm "Xoá tài khoản" hoặc "Export data".

**Vướng:**
- Settings không có nút "Xuất dữ liệu" rõ ràng → tôi tìm 10 phút
- "Xoá tài khoản" → confirm dialog "Tất cả data sẽ bị xoá vĩnh viễn" → tôi sợ → bỏ qua
- Không có "Tạm dừng" option → tôi không muốn xoá hẳn, chỉ muốn nghỉ vài tháng
- Không nhận được "Cảm ơn bạn đã thử beta, mong feedback" email → cảm thấy bị bỏ rơi
- Cô Hương hỏi "sao bỏ?" — tôi không biết complain ở đâu cho dev biết

**Gap surface:**
- **PR-LAN-25** (P0): GDPR/PDPL data export chưa có self-service → user trapped (P0, compliance!)
- **PR-LAN-26** (P1): "Tạm dừng tài khoản" option chưa có — only binary active/delete (P1)
- **PR-LAN-27** (P1): Exit survey / feedback form khi user về xa → mất signal (P1) — overlap với Wave 73 Tally feedback
- **PR-LAN-28** (P2): No follow-up email sau churn 14 days → reactivation opportunity mất (P2)

**Tại sao? × 5:** (1) Vì PDPL compliance gate (GAP-201 tenant off-boarding) shipped runbook nhưng FE chưa wire (2) Vì lifecycle state chưa enum "PAUSED" (3) Vì exit feedback channel chưa standard (4) Vì email automation chỉ cover acquisition không retention (5) Vì assume churn = bug fix needed thay vì signal collection

---

### Persona 2 — Thầy Hùng

#### Bước 1 — Pre-invite
**Tôi nghĩ:** Thầy Hùng đọc tech blog, thấy KiteHub trên ProductHunt vietnam. Click vào landing trên Macbook. Đọc README nhanh, hiểu tagline ngay. Có GitHub icon? Không. OK, vẫn thử.

**Gap surface:**
- **PR-HUNG-1** (P2): Tech-savvy user mong đợi "View on GitHub" link cho transparency open-source-ness — current landing chưa có signal (P2)
- **PR-HUNG-2** (P2): API docs link không expose trên landing — Hùng muốn biết có thể extend không (P2)

---

#### Bước 2 — Beta access request
**Tôi nghĩ:** Form straightforward. Gõ thông tin, source = "ProductHunt VN", expected size = "1-50 (đang xây)".

**Vướng:**
- Form OK, submit OK
- Auto-confirm email — nhận sau 5s, content ngắn gọn OK
- Nhưng email không có "track your request" link, không biết khi nào duyệt

**Gap surface:**
- **PR-HUNG-3** (P1): Không có status page "your beta request: pending/approved" cho user check tự (P1)
- **PR-HUNG-4** (P2): Email auto-confirm OK cho Hùng, nhưng anh ấy mong magic link "trở lại" — chưa có (P2)

---

#### Bước 3 — Email verify + provisioning
**Tôi nghĩ:** 1 ngày sau (Hùng vào sớm trong queue) nhận email approve. Link → set password.

**Vướng:**
- Password 12 chars complex → Hùng đã có password manager, OK ngay
- Sau set password, vào dashboard → trống → Hùng vào DevTools network tab xem JWT, role, endpoints
- Phát hiện CORS preflight fail từ thầy thử fetch API → fail (Phase 1 BETA env config 6 P0 chưa fix)

**Gap surface:**
- **PR-HUNG-5** (P0): Tech-savvy user phát hiện CORS / API errors → mất tin tưởng (đã có rule `production-env-config-registry.md` v1.1 — verify post-deploy) (P0)
- **PR-HUNG-6** (P1): Public API docs / OpenAPI spec chưa expose → developer-user lack onboarding (P1)

---

#### Bước 4 — Onboarding wizard
**Tôi nghĩ:** Hùng skip nhanh, chọn "Energetic" tone + Y-Z template + custom logo upload từ Canva.

**Vướng:**
- Preview OK, "Deploy" nhanh
- Subdomain auto-assigned `hung-toan.kitehub.me` — Hùng muốn `thayhung.com` (custom domain) → tìm setting → "Coming Phase 2"
- Banner generated render lỗi font tiếng Việt diacritics (`Thầy Hùng` thành `Thay Hung`)

**Gap surface:**
- **PR-HUNG-7** (P1): Custom domain CNAME — Phase 1.5+ scope; user mong đợi từ ngày 1 cho branding professional (P1)
- **PR-HUNG-8** (P0): Diacritic rendering bug trong generated banner (AI Branding quality gate check 5 nhưng chưa test VN font) (P0)
- **PR-HUNG-9** (P2): Mong "preview live URL" rõ ràng trên dashboard (P2)

---

#### Bước 5 — First lớp + first student
**Tôi nghĩ:** Hùng tạo lớp Toán 10 hè. Chưa có học sinh thật — tạo 3 student giả để test.

**Vướng:**
- Email giả `test1@kitehub.dev` → KiteHub gửi welcome email thật cho test addr → bounce → kitehub-email service trip rate limit
- Schedule conflict detection: tạo 2 lớp cùng giờ → KiteHub không warn → confuse
- Lớp settings không có "Public landing page" — Hùng muốn share URL khoá học để phụ huynh đăng ký

**Gap surface:**
- **PR-HUNG-10** (P1): Test/sandbox mode cho user tạo data giả không trigger real email (P1)
- **PR-HUNG-11** (P1): Schedule conflict detection chưa có (cross-class same teacher same time) (P1)
- **PR-HUNG-12** (P0): Public landing page per-class với "Đăng ký học" CTA — chưa có (P0 cho persona Hùng buổi đầu) (P1)

---

#### Bước 6 — Sử dụng hằng ngày
**Tôi nghĩ:** Hùng đăng FB share, có 2 phụ huynh inquire. Hùng muốn auto-onboard.

**Vướng:**
- Inquiry form (public) → KiteHub chưa có public form generator → Hùng phải share Zalo manually
- Sau onboard 2 hs, doanh thu 600k/tháng — Hùng tracker analytics → KiteHub analytics dashboard chỉ có "Total students: 2" không có MRR/cohort/funnel
- Hùng muốn integrate Stripe sandbox để chấp nhận thẻ → Wave 74 candidate

**Gap surface:**
- **PR-HUNG-13** (P0): Public inquiry form generator chưa có (P0 cho growth flow)
- **PR-HUNG-14** (P1): Analytics dashboard chưa có cohort retention / MRR / funnel (P1)
- **PR-HUNG-15** (P0 Wave 74): Stripe sandbox cho payment thử nghiệm — đúng scope Wave 74 (P0)

---

#### Bước 7 — Off-boarding / Retention
**Tôi nghĩ:** Sau 2 tuần Hùng pivot, không muốn nữa. Nhưng muốn keep data export.

**Vướng:**
- Export → CSV download OK (giả định ship trong Wave 73?) — nhưng AI-generated branding assets (logo, banner) không có ZIP download
- Hùng muốn keep DNS subdomain `hung-toan.kitehub.me` redirect sang Substack mới → chưa có "redirect mode"

**Gap surface:**
- **PR-HUNG-16** (P1): Data export bao gồm AI-generated assets (logo PNG, banner) — chưa có ZIP bundle (P1)
- **PR-HUNG-17** (P2): Tenant subdomain redirect mode (instead of 404 after offboard) (P2)

---

### Persona 3 — Cô Thuý

#### Bước 1 — Pre-invite
**Tôi nghĩ:** Cô Thuý không vào website, con gái (Linh) vào hộ. Linh đọc landing, screen-share cho mẹ qua Zoom.

**Vướng:**
- Landing có nhiều English ("dashboard", "AI branding", "multi-tenant") → mẹ không hiểu, hỏi Linh
- Không có video demo VN voiceover → Linh phải tự explain 15 phút
- Không có "So sánh với Misa, KidsPay" — mẹ hỏi "khác gì phần mềm cũ 1tr/tháng?"

**Gap surface:**
- **PR-THUY-1** (P0): Landing copy quá nhiều English jargon — P3 50+ user lost (P0)
- **PR-THUY-2** (P1): Video demo VN voiceover cho non-tech persona (P1)
- **PR-THUY-3** (P1): Comparison page vs. Misa/KidsPay competitor (P1)

---

#### Bước 2 — Beta access request
**Tôi nghĩ:** Linh fill form hộ mẹ. Org = "Trung tâm Anh ngữ Hoa Sen", size = 50-200.

**Vướng:**
- Form không capture "tenant role" (Owner vs. Staff vs. Teacher) — Linh nên là Owner (mẹ) hay Staff (Linh)?
- 1-3 ngày SLA — mẹ sợ trễ kỳ học mới (1 tuần nữa)

**Gap surface:**
- **PR-THUY-4** (P1): Form chưa hỏi tenant role expected → admin duyệt khó (P1)
- **PR-THUY-5** (P1): Urgent path "đang chuẩn bị kỳ học, cần kích hoạt nhanh" → no fast-track (P1)

---

#### Bước 3 — Email verify + provisioning
**Tôi nghĩ:** Linh nhận email, click set password hộ mẹ. Vào dashboard.

**Vướng:**
- Dashboard mặc định English — Linh chuyển sang VN trong settings — nhưng nhiều label chưa dịch (button "Save", "Cancel", "Delete")
- Mẹ không biết đăng nhập riêng — chỉ có 1 account, không biết invite teacher staff

**Gap surface:**
- **PR-THUY-6** (P0): i18n coverage chưa 100% — mixed VN/EN buttons (P0)
- **PR-THUY-7** (P0): "Invite teacher/staff" workflow chưa visible từ dashboard (P0 cho center P2/P3)

---

#### Bước 4 — Onboarding wizard
**Tôi nghĩ:** Linh + mẹ làm wizard cùng.

**Vướng:**
- Logo upload — trung tâm có logo cũ 5 năm, file PNG 5MB → upload fail (size limit chưa rõ)
- Preview generated colors quá modern (neon) không hợp với "trung tâm Anh ngữ truyền thống" thuý muốn

**Gap surface:**
- **PR-THUY-8** (P1): Upload size limit + error message chưa clear (P1)
- **PR-THUY-9** (P1): Color palette presets chưa cover "traditional / academic" tone phù hợp 50+ persona (P1)

---

#### Bước 5 — First lớp + first student
**Tôi nghĩ:** Linh import 80 hs từ Excel mẹ đưa. Tìm "Import CSV/Excel"...

**Vướng:**
- Bulk import — không có visible upload Excel → Linh phải tạo từng học sinh một × 80 lần → bỏ ngang sau 5 hs
- Tạo class với 20 hs → form "add students to class" select 1 by 1 → 20 click each
- Mẹ hỏi "lưu được sổ giấy 5 năm cũ vào đâu?" — không có "Archive historical data" path

**Gap surface:**
- **PR-THUY-10** (P0): Bulk import xlsx — GAP-051 scoped K-12 only, nhưng P3 80hs trung tâm cũng critical (P0)
- **PR-THUY-11** (P1): Multi-select students cho class assignment (P1)
- **PR-THUY-12** (P1): Historical data import (closed classes from prior years) — flow chưa có (P1)

---

#### Bước 6 — Sử dụng hằng ngày
**Tôi nghĩ:** Cô Thuý không tự dùng, Linh + 2 nhân viên dùng.

**Vướng:**
- 3 nhân viên cùng edit attendance same class → conflict, last-write-wins → mẹ thấy data sai
- Permissions: 1 nhân viên kế toán không cần thấy student PII (chỉ cần thấy payment) — nhưng chưa có role-based access
- Mẹ hỏi "biên lai có dấu đỏ không?" — KiteHub generate PDF không có chữ ký + dấu → không đủ legal cho phụ huynh khai thuế

**Gap surface:**
- **PR-THUY-13** (P0): Concurrent edit conflict handling (P0)
- **PR-THUY-14** (P0): Role-based access (RBAC) — Admin/Teacher/Accountant separation chưa có (P0 cho P3)
- **PR-THUY-15** (P0): Invoice PDF với e-signature + e-stamp Tổng cục Thuế compliant — chưa có (P0 cho VN compliance, Phase 1.5+)

---

#### Bước 7 — Off-boarding
**Tôi nghĩ:** Sau 4 tuần, mẹ thấy "khó hơn Misa", muốn dừng. Linh export data.

**Vướng:**
- Export 80 hs × 6 lớp × payment history → bundle ~50MB → download chậm, browser timeout
- Mẹ lo "data có tự xoá khỏi server không?" — privacy policy chưa rõ
- Linh muốn báo cáo cho mẹ "đã hoạt động 4 tuần như nào" → no "summary report" generator

**Gap surface:**
- **PR-THUY-16** (P0): Large export bundle (>20MB) — cần async job + email link download (P0)
- **PR-THUY-17** (P0): Data deletion confirmation explicit cho user (PDPL 2023) (P0)
- **PR-THUY-18** (P1): Tenant activity summary report (PDF) — closing artifact (P1)

---

### Persona 4 — Anh Bảo

#### Bước 1 — Pre-invite
**Tôi nghĩ:** Bảo thấy KiteHub trên FB group, click vào landing.

**Vướng:**
- "SaaS giáo dục" — Bảo dạy guitar có phải giáo dục không? Tôi đoán có, nhưng landing không show "music / creative arts" use case
- Bảo tìm "demo creator instructor" → không có

**Gap surface:**
- **PR-BAO-1** (P2): Landing chưa có persona-spectrum "from solo creator to enterprise school" — solo creator feel out of scope (P2)
- **PR-BAO-2** (P3): Music/arts industry showcase — Phase 2+ scope (P3)

---

#### Bước 2 — Beta access request
**Tôi nghĩ:** Bảo fill form bình thường, source = "FB group Solo Educator VN".

**Vướng:** không có vướng đặc biệt ở bước này.

**Gap surface:** (none specific)

---

#### Bước 3 — Email verify + provisioning
**Tôi nghĩ:** Bảo verify nhanh, set password OK.

**Vướng:**
- Dashboard tạo subdomain `bao-guitar.kitehub.me` — Bảo OK nhưng muốn integrate YouTube channel link prominently

**Gap surface:**
- **PR-BAO-3** (P2): Tenant profile chưa cho user add external links (YouTube/Facebook/TikTok) hiển thị trên public landing (P2)

---

#### Bước 4 — Onboarding wizard
**Tôi nghĩ:** Bảo nhanh chóng làm wizard.

**Vướng:**
- Tone preset "Professional / Friendly / Energetic / Luxurious" — Bảo muốn "Artistic / Creative / Chill" — không có
- Template preview toàn academic (sách, bảng đen) — không có nhạc cụ / creative tone

**Gap surface:**
- **PR-BAO-4** (P2): Tone vocabulary chưa cover creative/arts education (P2)
- **PR-BAO-5** (P2): Template library expand cho non-academic verticals (P2 — Phase 2+)

---

#### Bước 5 — First lớp + first student
**Tôi nghĩ:** Bảo tạo class "Guitar đệm hát Beginner". Thêm 5 học viên.

**Vướng:**
- "Subject" dropdown chỉ có academic — Bảo custom "Music — Guitar" OK nhưng không có chuẩn taxonomy
- Lớp Bảo dạy theo level (Beginner/Intermediate/Advanced) không theo grade — UI hiển thị "Lớp 1 / Lớp 2..." dropdown không phù hợp
- Bảo muốn link YouTube video bài học vào từng buổi — không có "lesson materials" field

**Gap surface:**
- **PR-BAO-6** (P2): Subject taxonomy chưa cover music / arts (P2)
- **PR-BAO-7** (P1): Class metadata "level" thay "grade" tùy verticality (P1)
- **PR-BAO-8** (P0 cho creator): Lesson materials field (link YouTube/Drive/PDF) per session — chưa có (P1 cho P8 creator)

---

#### Bước 6 — Sử dụng hằng ngày
**Tôi nghĩ:** Bảo dạy 2 buổi/tuần qua Zoom. Sau buổi, muốn upload bài tập (PDF tab nhạc + video).

**Vướng:**
- Upload assignment — chưa có "video" type, chỉ có PDF
- Học viên submit lại — chưa có "audio recording submission"
- Bảo muốn bán khoá ghi sẵn (asynchronous course) — KiteHub chỉ cho live class, không có async/recorded

**Gap surface:**
- **PR-BAO-9** (P1): Multi-media assignment types (video/audio submit) — chưa có (P1)
- **PR-BAO-10** (P1 — P8 persona): Async/recorded course mode — out of Phase 1 scope (P1 future)
- **PR-BAO-11** (P2): YouTube/Vimeo embed support cho lesson preview (P2)

---

#### Bước 7 — Off-boarding
**Tôi nghĩ:** Bảo 1 tháng sau muốn pause. KiteHub không có pause → Bảo xoá.

**Vướng:**
- Export — Bảo muốn export "course intellectual property" (lesson materials uploaded) — KiteHub không bundle uploaded files với CSV → mất content
- Public landing `bao-guitar.kitehub.me` sau xoá → 404 → khách Bảo tin tưởng landing đó

**Gap surface:**
- **PR-BAO-12** (P0 cho creator): Export bundle phải include uploaded materials (PDF/video links) (P0)
- **PR-BAO-13** (P2): Subdomain 404 sau offboard — bad UX cho khách của Bảo (P2)

---

## Tổng hợp gap candidate

| ID | Nhân vật | Bước | Vấn đề | Mức độ | Đề xuất fix | Nhóm wave |
|---|---|---|---|---|---|---|
| PR-LAN-1 | Lan | 1 | Landing copy hướng B2B, P1 lost | P1 | Persona-specific landing variant + tagline VN | Wave 73 hoặc 74 |
| PR-LAN-3 | Lan | 2 | Sau submit form, không có email confirm | P0 | Auto-confirm transactional email | Wave 73 (email scope) |
| PR-LAN-4 | Lan | 2 | SLA 1-3 ngày quá dài | P1 | Auto-approve FREE tier OR fast-track | Wave 73 |
| PR-LAN-6 | Lan | 3 | Email vào Spam (deliverability) | P0 | DKIM/DMARC warm-up + sender reputation monitor | Wave 73 (email scope) |
| PR-LAN-9 | Lan | 3 | Sau set password, dashboard rỗng, no onboarding trigger | P0 | Auto-trigger onboarding wizard post-signup | Wave 73 |
| PR-LAN-14 | Lan | 4 | Generated banner không pull tên trung tâm | P0 | Wizard fetch user profile first → personalize preview | Wave 74 hoặc sau |
| PR-LAN-15 | Lan | 5 | Schedule model cohort, không support 1-on-1 gia sư | P0 | Per-student timeslot — model change | Wave 75+ (big) |
| PR-LAN-16 | Lan | 5 | Email học sinh required, VN teen không có | P0 | Phone/Zalo-based identity option | Wave 75+ |
| PR-LAN-20 | Lan | 6 | Mobile perf chậm + small touch targets | P0 | Mobile perf audit + responsive review | Wave 73 (UI smoke) |
| PR-LAN-21 | Lan | 6 | Notification engine chưa wire (GAP-063) | P0 | Email-first notification, Zalo later | Wave 75+ |
| PR-LAN-23 | Lan | 6 | Cash payment không có invoice tự động | P0 | Invoice template + manual receipt | Wave 74 (payment) |
| PR-LAN-25 | Lan | 7 | Data export chưa self-service (PDPL!) | P0 | Self-service export UI | Wave 73 |
| PR-HUNG-5 | Hùng | 3 | Tech user phát hiện CORS errors | P0 | Verify post-deploy production-env-config | Wave 73 (UI smoke) |
| PR-HUNG-8 | Hùng | 4 | Diacritic VN render lỗi trong banner | P0 | Test font VN + fix AI Branding quality gate | Wave 74 |
| PR-HUNG-10 | Hùng | 5 | Test/sandbox mode (chưa có) | P1 | Add `test_mode` flag bypass email send | Wave 74 |
| PR-HUNG-12 | Hùng | 5 | Public landing per-class chưa có | P0 | Public class page generator | Wave 75+ |
| PR-HUNG-13 | Hùng | 6 | Public inquiry form generator | P0 | Form builder UI | Wave 75+ |
| PR-HUNG-15 | Hùng | 6 | Stripe sandbox | P0 (Wave 74) | Stripe integration test | Wave 74 ✓ |
| PR-THUY-1 | Thuý | 1 | Landing English jargon | P0 | Full VN copy review | Wave 73 (user manual scope) |
| PR-THUY-6 | Thuý | 3 | i18n incomplete (mixed VN/EN buttons) | P0 | i18n coverage audit | Wave 73 |
| PR-THUY-7 | Thuý | 3 | Invite teacher/staff workflow not visible | P0 | Add "Team" tab in dashboard | Wave 75+ |
| PR-THUY-10 | Thuý | 5 | Bulk import xlsx — P3 cũng cần | P0 | Extend GAP-051 to P2/P3 scope | Wave 75+ |
| PR-THUY-13 | Thuý | 6 | Concurrent edit conflict | P0 | Optimistic locking + conflict UI | Wave 75+ |
| PR-THUY-14 | Thuý | 6 | RBAC Admin/Teacher/Accountant chưa có | P0 | Role permission matrix | Wave 75+ |
| PR-THUY-15 | Thuý | 6 | Invoice e-signature/e-stamp VN compliant | P0 | Phase 1.5 scope | Wave 75+ Phase 1.5 |
| PR-THUY-16 | Thuý | 7 | Large export bundle timeout | P0 | Async export job | Wave 75+ |
| PR-THUY-17 | Thuý | 7 | Data deletion confirmation (PDPL) | P0 | Explicit delete confirmation UI | Wave 73 |
| PR-BAO-8 | Bảo | 5 | Lesson materials field (creator persona) | P1 | Materials field per session | Wave 75+ |
| PR-BAO-9 | Bảo | 6 | Video/audio assignment | P1 | Multi-media submission | Wave 75+ |
| PR-BAO-12 | Bảo | 7 | Export bundle bao gồm uploaded files | P0 | Bundle materials in export | Wave 75+ |

**Tổng:** ~30 gap candidate. P0 = 18 / P1 = 9 / P2 = 3 (lower-priority).

---

## Khuyến nghị cho Wave 73 / 74

### Wave 73 (đã propose: email audit, user manual, Tally feedback, UI smoke)

**Đã cover bởi dev brainstorm hiện tại:**
- Email audit → cover PR-LAN-3 (auto-confirm) + PR-LAN-6 (deliverability) + PR-LAN-4 (SLA fast-track)
- User manual → cover PR-THUY-1 (VN copy) + PR-THUY-6 (i18n)
- UI smoke → cover PR-LAN-20 (mobile perf) + PR-HUNG-5 (CORS verify)
- Tally feedback → cover PR-LAN-27 (exit survey)

**Dev đã miss — nên đưa thêm vào Wave 73:**
- **PR-LAN-9** (P0) — Auto-trigger onboarding wizard post-signup (low effort, high impact)
- **PR-LAN-25** (P0) — Self-service data export (PDPL compliance critical!)
- **PR-THUY-17** (P0) — Explicit delete confirmation UI (PDPL)
- **PR-LAN-1** (P1) — Persona-specific landing copy variant cho P1 Solo Teacher (overlap với user manual scope)

### Wave 74 (đã propose: Stripe sandbox)

**Đã cover:**
- Stripe sandbox → cover PR-HUNG-15 ✓

**Dev đã miss — nên đưa thêm vào Wave 74:**
- **PR-LAN-14** (P0) — Onboarding wizard fetch user profile để personalize banner preview (block đầu trải nghiệm dùng AI)
- **PR-LAN-23** (P0) — Invoice template cho cash payment (đa số P1/P2 thu tiền mặt, gateway chỉ cho online users)
- **PR-HUNG-8** (P0) — Diacritic VN render trong banner (AI Branding quality gate gap)
- **PR-HUNG-10** (P1) — Test/sandbox mode flag (block dev/tester từ tạo data thật)

### Wave 75+ (out-of-scope Wave 73/74 nhưng critical P0)

- **PR-LAN-15** (P0) — 1-on-1 gia sư schedule model (lớn — model change)
- **PR-LAN-16** (P0) — Phone/Zalo-based identity (replace email-required)
- **PR-LAN-21** (P0) — Notification engine email-first (GAP-063 partial activation)
- **PR-THUY-7/10/13/14** (4 × P0) — Team invite + bulk import + concurrent edit + RBAC
- **PR-HUNG-12/13** (2 × P0) — Public class landing + inquiry form generator (growth flow)
- **PR-THUY-15** (P0) — VN invoice compliance (Phase 1.5)
- **PR-BAO-12** (P0 creator) — Export bundle includes materials

---

## Khác biệt vs dev brainstorm hiện tại

### Overlap (dev đã có):
- Email audit ⊃ PR-LAN-3 + PR-LAN-6 + PR-LAN-4 ✅
- User manual ⊃ PR-THUY-1 + PR-THUY-6 ✅
- UI smoke ⊃ PR-LAN-20 + PR-HUNG-5 ✅
- Tally feedback ⊃ PR-LAN-27 ✅
- Stripe sandbox = PR-HUNG-15 ✅

### Gap MỚI dev miss (Wave 73/74 nên thêm — Top 8 P0):
1. **PR-LAN-9** — Onboarding wizard auto-trigger post-signup (Wave 73)
2. **PR-LAN-25** — Self-service data export (PDPL!) (Wave 73)
3. **PR-THUY-17** — Explicit delete confirmation UI (PDPL!) (Wave 73)
4. **PR-LAN-14** — Personalized banner preview (Wave 74)
5. **PR-LAN-23** — Cash payment invoice template (Wave 74)
6. **PR-HUNG-8** — Diacritic VN font render bug (Wave 74)
7. **PR-LAN-1** — Persona-specific landing variant (Wave 73, overlap user manual)
8. **PR-HUNG-10** — Test/sandbox mode flag (Wave 74)

### Strategic gaps (Wave 75+, sẽ surface khi user actually use):
- 1-on-1 gia sư schedule model (PR-LAN-15) — block 60% P1 VN market
- Email/phone identity flex (PR-LAN-16) — block K-12/teen VN
- Notification engine (PR-LAN-21) — block all 4 personas
- Bulk import + RBAC + concurrent edit (PR-THUY-10/13/14) — block P3 trung tâm
- Public class landing + inquiry form (PR-HUNG-12/13) — block growth flow

### Risk surfaces user feedback CSV (126 dòng) sẽ tự bộc lộ:
- 28 cells × 4 personas = surface 30 gaps thực tế
- Dev brainstorm có 5 hạng mục (email/user manual/Tally/UI smoke/Stripe) = cover ~6/30 gap thực tế (~20% coverage)
- → Khuyến nghị Wave 73 mở rộng thêm 4 P0 + Wave 74 mở rộng thêm 4 P0 trước khi mời 5 beta tenant thực sự

---

## Lưu ý cuối — meta observation

3 vấn đề nền tảng dev đang quá quen hệ thống nên miss:

1. **Persona scope bias** — dev hiện tập trung P2 Center Owner (Phase 1 BETA chốt P1+P2 soft launch), nhưng landing + wizard + form đang phục vụ P2 default → P1 Solo Teacher (đa số người dùng đầu) lạc lõng. Cần persona-aware UI variants.

2. **VN cultural blind spots** — schedule cohort vs 1-on-1, email vs phone identity, cash vs gateway payment, invoice e-stamp compliance — tất cả critical nhưng được design theo SaaS Western default. Cần VN-first persona review per release.

3. **PDPL compliance gaps surface ở Bước 7 off-boarding** — không user nào complain ngay; nhưng audit + lawsuit risk cao. Wave 73 nên ưu tiên data export self-service + explicit delete (đã đề xuất ở trên).

---

## References

- `documents/00-brd/personas-catalog.md` — P1 Solo Teacher / P2 Small Center / P3 Medium Center / S Student / Tier-2 P8 Online Course Creator
- `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` — 126-row Vietnamese matrix
- `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action — Wave 72b candidate scope + Wave 73 stub
- GAP-063 (Zalo/SMS notification — cross-persona blocker, P1 → recommend P0)
- GAP-057 (Commission/payroll engine — P2/P3/P5 blocker)
- GAP-201 (Tenant off-boarding runbook — implementation FE chưa wire)
- GAP-051 (Bulk import xlsx — scoped K-12 only, cần extend P2/P3)
- GAP-372 (Beta tenant invite mechanism — DONE but onboarding wizard auto-trigger missing)
