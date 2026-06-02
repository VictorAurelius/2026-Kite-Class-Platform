---
audience: mixed
---

# Persona Simulation — Wave 11 Pre-Lock (Mobile OTP signup + Batch Invoice)

**Date:** 2026-06-02
**Scope:** Outside-in pre-scope-lock audit cho Wave 11 plan (FE features GAP-286 + GAP-297) per `.claude/rules/outside-in-coverage-trigger.md` v1.1.0 §3 Bước 4 (audit BEFORE wave plan lock)
**Methodology:** `.claude/skills/quality/persona-based-business-review/SKILL.md` — 5 personas × 2 features = 10 walk simulations
**Phase:** Phase 1 BETA (target soft-launch 2026-05-06+, P1+P2 cohorts)
**Status:** complete

---

## §1 Methodology

### 1.1 Personas simulated (5 — VN edu SaaS Phase 1 BETA target cohort)

| ID | Persona | Profile | Device | Tech savvy | Cultural anchor |
|---|---|---|---|---|---|
| P1 | **Trung Tâm Trần** (Owner) | Chủ trung tâm Anh ngữ "Smart English Hà Nội", 80 học viên, 6 lớp song song, đang dùng Google Sheets + Zalo group quản lý | Android (Samsung A52, 4G), thỉnh thoảng laptop Windows tại văn phòng | TB — biết Excel sâu, dùng Facebook Business, KHÔNG quen webhooks/API | Ưu tiên tiện lợi + tốc độ; dị ứng với app phải đăng ký nhiều bước |
| P2 | **Cô Hương** (Teacher/Admin) | Giáo viên kiêm phụ trách tuyển sinh, 32 tuổi, dạy 4 lớp/tuần + nhận đăng ký mới qua Zalo, làm việc 80% trên điện thoại | iPhone 13 (cá nhân), thỉnh thoảng iPad của trung tâm | TB-thấp — quen UI Zalo/Facebook; KHÔNG dùng email công việc (chỉ Gmail cá nhân hiếm khi mở) | Zalo là kênh chính 95% giao tiếp; coi email là "spam folder" |
| P3 | **Em Linh** (Student) | Sinh viên năm 2 ĐH Ngoại Thương, 19 tuổi, đăng ký lớp IELTS 6.5, trả học phí từ tiền tiêu vặt + việc làm thêm | iPhone SE 2020 (4G chạy chậm khu vực Cầu Giấy giờ cao điểm) | Cao — Gen Z dùng app trôi chảy, đa nhiệm, quen QR/biometric | Zalo + Instagram + TikTok dominant; coi SMS là spam OTP ngân hàng |
| P5 | **Anh Tuấn** (Parent) | PHHS lớp 8 ở Quận 7 TP.HCM, 42 tuổi, kỹ sư IT công ty Nhật, có 2 con học thêm Toán + tiếng Anh | iPhone 14 Pro + laptop công ty | Cao — dev background; nhưng vẫn ưu tiên Zalo cho việc gia đình | Trả tiền học phí kỳ 3 tháng/lần qua chuyển khoản Vietcombank; cần invoice PDF để báo cáo công ty nếu xin claim hỗ trợ giáo dục |
| P6 | **Chị Mai** (Accountant) | Kế toán part-time cho 3 trung tâm nhỏ, 38 tuổi, xử lý batch invoice cuối tháng, sổ sách trên Excel + MISA | Laptop Windows 10 + iPhone XR | TB-Cao — kế toán chuyên nghiệp; nhưng KHÔNG dev background | Quen workflow eInvoice MISA + VietQR cho thu học phí; nhạy cảm về định dạng VND |

### 1.2 Walk approach per persona × feature

Mỗi simulation cover 6 dimensions:

1. **Discovery path** — Persona biết feature exists qua kênh nào (Zalo share / Google search / direct invite link / nav menu)?
2. **Trust signals** — Có yếu tố nào khiến persona tin tưởng đăng ký / submit (verification badge / brand recognition / testimonial)?
3. **Friction points** — Step nào confusing / redundant / missing (vd: phải nhập email khi đã có phone)?
4. **Cultural fit** — Có khớp expectation VN edu context (Zalo > SMS > email priority; batch month-end cuối tháng dương lịch không phải cuối quý; cuối tuần T6-T7 vẫn làm việc)?
5. **Failure recovery** — Khi OTP fail / signup error / invoice send fail, persona có hiểu actionable next step không?
6. **Mental model gap** — Persona expects X (ví dụ "OTP qua Zalo"), system delivers Y (vd "OTP qua SMS") → friction.

### 1.3 Scope clarification — features audited

**GAP-286 Mobile OTP signup:**
- AC mandate: ≤10 phút wall-clock từ landing → dashboard ready
- OTP: Zalo OA primary + SMS fallback
- Phone format VN `0\d{9,10}`
- Rate limit 3/15min
- Mobile viewport iPhone 13 / Android equivalent

**GAP-297 Batch Invoice:**
- AC mandate: 60 invoices ≤5 sec; 300 ≤15 sec
- 2-step endpoint: batch-generate preview → batch-confirm persist + enqueue
- Idempotency: re-run same month NO duplicate
- Pro-rata mid-month (cross-link GAP-300)
- UI ≤3 clicks; mobile-friendly; preview before commit
- Notification: email + GAP-063 Zalo/SMS khi available

---

## §2 GAP-286 Mobile OTP signup — simulation findings (5 personas × walk)

### 2.1 P1 Trung Tâm Trần (Owner) walk — Android Samsung A52

**Bước 1 — Discovery:** Trần biết KiteHub qua post Facebook group "Chủ trung tâm Tiếng Anh Hà Nội" → click link `kitehub.me` trên smartphone giờ nghỉ trưa.

**Bước 2 — Landing page:** Mở `kitehub.me/dang-ky` (hoặc `/signup`). **Friction #1:** Nếu URL English-only → Trần expect tiếng Việt mặc định (đã quen Vietcombank/Tiki/Shopee VN default). Nếu landing chỉ có nút "Sign up" thay vì "Đăng ký miễn phí" → mất 2-3 giây parse.

**Bước 3 — Form đăng ký:** Trần nhập số `0912345678`. **Friction #2:** Nếu form có sẵn dropdown `+84` thì OK; nếu yêu cầu nhập `+84912345678` → người Việt KHÔNG quen prefix quốc tế cho số nội địa. **Trust signal #1:** Cần text rõ "Số điện thoại sẽ dùng để đăng nhập Zalo OA — không spam SMS".

**Bước 4 — OTP delivery:** Trần expect OTP qua **Zalo OA** (vì Facebook post đã nhấn "tích hợp Zalo"). **Mental model gap #1 CRITICAL:** Nếu OTP gửi qua SMS thay vì Zalo → Trần lo lắng "SMS có mất phí của tôi không?", "Có spam tin nhắn ngân hàng?". P1 owner persona: SMS = enterprise OTP banking (kèm phí ngầm). Zalo OA = friendly business chat. Default SHOULD BE Zalo OA push notification → SMS fallback explicit khi Zalo OA chưa add friend.

**Bước 5 — OTP input:** Code 6 số. **Friction #3:** Nếu Web OTP API (Android Chrome) không auto-fill → Trần phải đóng KiteHub → mở Zalo app → screenshot OTP → copy → quay lại browser → paste. Trên Android 4G chậm = 30-45 giây + frustration. **Recovery:** Cần button "Gửi lại OTP" với countdown 60s rõ ràng; KHÔNG ẩn button "Tôi không nhận được OTP".

**Bước 6 — Tenant provisioning + first login:** AC mandate <30s từ verify OTP → first login redirect. **Trust signal #2 CRITICAL:** Loading screen cần message progressive: "Đang tạo trung tâm của bạn... 30%" thay vì spinner trống. P1 owner KHÔNG kiên nhẫn quá 5 giây spinner trắng (đã quen Vietcombank cảnh báo "đang xử lý, vui lòng chờ").

**Bước 7 — Dashboard ready:** Landing trên `/dashboard/onboarding` (không phải raw dashboard). **Cultural fit #1:** Cần wizard 3 bước nhanh: "Tạo lớp đầu tiên / Thêm 5 học viên / Cấu hình lịch tuần" — KHÔNG dump 12 menu items.

**Top friction P1:** Bước 4 (OTP delivery channel mismatch) + Bước 5 (manual OTP copy on Android) + Bước 7 (wizard vs raw dashboard).

### 2.2 P2 Cô Hương (Teacher/Admin) walk — iPhone 13

**Bước 1 — Discovery:** Cô Hương được Trần (owner cùng trung tâm) share link Zalo "Em vào đây đăng ký tài khoản, anh đã tạo trung tâm rồi". KHÔNG phải signup tenant; là invited user signup. **Friction #1 CRITICAL:** GAP-286 spec hiện chỉ cover tenant-owner signup; KHÔNG có flow "invited teacher signup" rõ ràng. Cô Hương click link Zalo → fallback xuống tenant-owner form → confused (mã trung tâm? Không có!). → **Gap surfaced:** Cần invited-user signup flow song song.

**Bước 2 — Phone-first vs email-first:** Nếu form yêu cầu email "công việc" → Cô Hương dùng Gmail cá nhân `huong.tdy@gmail.com` (không kiểm tra). Cô Hương KHÔNG dùng email công việc 95% thời gian — Zalo là email. **Mental model gap #2:** Nếu hệ thống dùng email để gửi password reset → Cô Hương lock account trong 6 tháng.

**Bước 3 — OTP qua Zalo OA:** Cô Hương đã add KiteHub OA (vì owner Trần forward QR trong Zalo group). OTP push notification arrive → tap → auto-fill iOS Safari (nếu Web OTP API tích hợp). **Trust signal #3:** Avatar KiteHub OA verified blue tick → Cô Hương trust ngay.

**Bước 4 — Profile setup:** Sau OTP verify, cần điền tên + chức danh. **Cultural fit #2:** Field "Chức danh" cần dropdown VN edu context: `Giáo viên / Trợ giảng / Tư vấn tuyển sinh / Admin / Khác` — KHÔNG free-text "Job title" English.

**Top friction P2:** Bước 1 (invited-user flow gap) + Bước 2 (email-first vs phone-first mental model) + Bước 4 (VN-specific role dropdown).

### 2.3 P3 Em Linh (Student) walk — iPhone SE 2020 4G

**Bước 1 — Discovery:** Linh search Google "lớp IELTS 6.5 quận Cầu Giấy" → click ad Facebook → landing → click "Đăng ký lớp" → redirect signup.

**Bước 2 — Self-signup vs tenant-invited:** Linh là student của trung tâm Anh ngữ (tenant đã có); KHÔNG self-create tenant. **Friction #1 (recurrence):** Cùng issue P2 — GAP-286 chưa rõ "student signup" flow. Linh expect "Tôi đăng ký lớp" → enroll workflow, không phải tenant-owner-form.

**Bước 3 — OTP delivery:** Linh expect OTP qua Zalo (Gen Z dùng Zalo 100%). Mental model gap #3: Nếu fallback SMS → Linh KHÔNG check SMS (chỉ check khi reset password ngân hàng). SMS notification icon trên iPhone đầy spam ("Vietlott trúng 100k").

**Bước 4 — Form thanh toán học phí:** Linh expect option **VietQR pay** (Gen Z quen Momo/ZaloPay/MB Bank QR). Nếu chỉ chuyển khoản thủ công → Linh bỏ giữa chừng. → Out of GAP-286 scope nhưng critical cho conversion.

**Bước 5 — App vs web:** Linh expect tải app KiteClass (Gen Z mặc định install app cho mọi service). Nếu chỉ web → Linh add shortcut iOS home screen nhưng PWA experience yếu hơn native app. → Phase 1.5+ scope.

**Top friction P3:** Bước 2 (student signup flow gap) + Bước 3 (Zalo expected, SMS friction) + Bước 5 (PWA vs native app expectation).

### 2.4 P5 Anh Tuấn (Parent) walk — iPhone 14 Pro

**Bước 1 — Discovery:** Tuấn nhận tin nhắn Zalo từ Anh ngữ Bình Minh (trung tâm con học): "Anh vui lòng đăng ký tài khoản phụ huynh để xem điểm + lịch học của bé Bin". Link gắn invite token.

**Bước 2 — Parent signup invited:** **Friction #1 CRITICAL (recurrence P2+P3):** GAP-286 chưa cover parent invited signup flow. Tuấn click link → expect form pre-fill "Phụ huynh em: Nguyễn Bình", "Lớp 8A2" → chỉ điền số ĐT + OTP. Nếu fallback tenant-owner form → Tuấn confused, gọi Zalo hỏi cô Hương → friction.

**Bước 3 — OTP delivery:** Tuấn là dev background → ok cả SMS lẫn Zalo. **Trust signal #4:** Tuấn check headers email/Zalo verify sender (anti-phishing instinct). Nếu OTP arrive từ `noreply@kitehub.me` không có SPF/DKIM/DMARC pass → Tuấn mark spam.

**Bước 4 — Multi-tenant edge:** Tuấn có 2 con học 2 trung tâm khác nhau (Anh ngữ Bình Minh + Toán cô Phượng). Tuấn sẽ nhận 2 invite link Zalo riêng → 2 tài khoản? Hay 1 tài khoản multi-tenant? **Gap surfaced:** GAP-286 + GAP-287 cần clarify multi-tenant parent UX (sister rule `pre-handoff-self-test-completeness.md` §2.7).

**Bước 5 — PDPL consent:** Tuấn dev background → ĐỌC consent text. Nếu consent dài English-translated bản quyền → Tuấn skeptical. Cần consent VN-localized + plain-language: "Chúng tôi lưu số ĐT để gửi thông báo về việc học của bé; không bán dữ liệu cho bên thứ 3".

**Top friction P5:** Bước 2 (parent invited flow gap) + Bước 3 (email auth headers) + Bước 4 (multi-tenant parent edge) + Bước 5 (PDPL consent text quality).

### 2.5 P6 Chị Mai (Accountant) walk — Laptop Windows 10

**Bước 1 — Discovery:** Mai được hire làm kế toán part-time → owner Trần share login KiteHub có sẵn (KHÔNG signup mới). Out of GAP-286 scope cho Mai self-onboard; relevant cho "invite accountant" sub-flow.

**Bước 2 — Mobile-first vs desktop-first:** Mai làm 90% trên laptop (Excel + MISA). **Mental model gap #4:** GAP-286 spec mobile-first → desktop signup flow có same quality không? Nếu desktop fallback chỉ là responsive mobile view → friction (form fields nhỏ, OTP autofill không hoạt động).

**Bước 3 — OTP qua Zalo:** Mai dùng Zalo PC trên laptop → OK. Nhưng nếu Zalo PC chưa login → Mai phải mở Zalo mobile để scan QR → multi-device friction.

**Top friction P6:** Bước 2 (desktop signup parity) + Bước 3 (Zalo PC multi-device).

---

## §3 GAP-297 Batch Invoice — simulation findings (5 personas × walk)

### 3.1 P1 Trung Tâm Trần (Owner) walk — Android + occasional laptop

**Bước 1 — Discovery:** Trần expect button "Tạo hóa đơn tháng" trên dashboard owner. **Friction #1:** Nếu nằm trong submenu "Tài chính > Hóa đơn > Batch" → Trần KHÔNG tìm thấy lần đầu. Cần shortcut card trên dashboard tháng cuối (auto-show từ ngày 25 mỗi tháng).

**Bước 2 — Preview drawer:** Trần click "Tạo hóa đơn tháng 5" → preview list. **Trust signal #1:** Cần show:
- Tổng số HĐ: 58 (KHÔNG phải "60" theo enrollment count — phải loại trừ học viên đã nghỉ)
- Tổng doanh thu: 290,000,000đ (VN format dấu chấm, NOT 290,000,000 dấu phẩy)
- Breakdown per lớp: "Lớp Toán 6A: 12 HĐ × 800k = 9.6M"
- Pro-rata học viên mới (vd "Học viên A vào ngày 15/05 → tính 17/30 tháng")

**Friction #2:** Nếu preview chỉ show "58 invoices to create" → Trần KHÔNG verify được; expect breakdown chi tiết để check trước khi commit.

**Bước 3 — Confirm:** Click "Tạo + gửi". **Mental model gap #1 CRITICAL:** Trần expect 2 toast separate:
- "✅ 58 HĐ đã tạo lúc 14:32"
- "📨 Đã gửi Zalo cho 56/58 phụ huynh; 2 chưa add OA"

Nếu chỉ 1 toast "58 HĐ đã tạo, đang gửi" → Trần không biết delivery status → call cô Hương check.

**Bước 4 — Idempotency:** Trần ấn nút "Tạo hóa đơn tháng" lần 2 cùng tháng (tưởng chưa save). **Critical:** System cần show modal warning "Tháng 5 đã có 58 HĐ tạo lúc 14:32. Bạn muốn (a) Xem lại danh sách; (b) Tạo bổ sung HĐ cho học viên mới (nếu có); (c) Hủy" — KHÔNG silent dedupe (P1 owner sẽ wonder "có gì đó sai").

**Bước 5 — Mobile constraint:** Trần làm trên Android. **Friction #3:** Preview drawer scroll 58 rows + breakdown trên Android 4G = lag. Cần lazy-load + summary card trước, detail expand-on-tap.

**Top friction P1:** Bước 1 (discoverability shortcut) + Bước 2 (preview detail level) + Bước 3 (delivery status separate toast) + Bước 5 (mobile performance).

### 3.2 P2 Cô Hương (Teacher/Admin) walk — iPhone 13

**Bước 1 — Permission scope:** Cô Hương có role Teacher/Admin → có quyền tạo batch invoice không? **Friction #1:** GAP-297 spec không clarify RBAC. Cô Hương expect owner-only feature; nếu cô có button → confused (làm thay owner?).

**Bước 2 — Mobile UI:** Nếu Hương được phân quyền, làm trên iPhone tại trung tâm. **Friction #2:** Same với P1 Bước 5 — preview 58 rows lag.

**Top friction P2:** Bước 1 (RBAC scope unclear) + Bước 2 (mobile performance).

### 3.3 P3 Em Linh (Student) walk — iPhone SE 2020

**Bước 1 — N/A direct:** Linh là student → NHẬN invoice, không tạo. Walk shifts to "Receive invoice" flow.

**Bước 2 — Notification arrives:** Cuối tháng 5, Linh nhận Zalo push "Hóa đơn tháng 5: 1,500,000đ — hạn 10/06". **Trust signal #1:** Cần avatar KiteHub OA + verified tick + brand "Anh ngữ Smart English Hà Nội" trong message. KHÔNG raw "Invoice #INV-2026-05-178".

**Bước 3 — View invoice detail:** Click → mở Zalo Mini App / web. **Friction #1:** Nếu cần login lại trong web (vì Zalo browser sandbox) → Linh bỏ giữa chừng. Cần SSO Zalo OA → seamless.

**Bước 4 — Payment:** Linh expect VietQR pay nút trong invoice detail. **Cultural fit #1 CRITICAL:** Học sinh Gen Z trả qua MB Bank / Vietcombank / ZaloPay quét QR. Nếu chỉ ghi STK 12-số → Linh ngại copy chính xác → chuyển khoản trễ.

**Top friction P3:** Bước 2 (notification brand trust) + Bước 3 (SSO Zalo OA) + Bước 4 (VietQR payment expected).

### 3.4 P5 Anh Tuấn (Parent) walk — iPhone 14 Pro + laptop

**Bước 1 — Notification arrives:** Tuấn nhận Zalo notification + email parallel. **Trust signal #1:** Tuấn check email headers (DMARC pass). Nếu fail → spam folder → miss thanh toán → child class suspended.

**Bước 2 — Invoice format:** Tuấn cần invoice PDF với:
- Logo trung tâm
- Tên đầy đủ học sinh (con Tuấn)
- Mã số thuế trung tâm (để Tuấn claim hỗ trợ giáo dục từ công ty Nhật)
- Pháp lý: hóa đơn điện tử eInvoice (theo TCT Vietnam) hay invoice nội bộ?

**Mental model gap #1 CRITICAL:** Nếu KiteHub ship "invoice" mà KHÔNG là eInvoice TCT-compliant → Tuấn không claim được. **Gap surfaced:** GAP-297 cần clarify "internal billing receipt" vs "eInvoice TCT". Phase 1.5+ partnership MISA MeInvoice (per Wave 93 outside-in audit retro re-scoped GAP-185 từ self-build → MISA).

**Bước 3 — Payment qua chuyển khoản:** Tuấn quen Vietcombank app → quét VietQR. **Friction #1:** Cần VietQR có dynamic amount (1,500,000đ pre-filled) + memo "INV-2026-05-178 P.Tuấn" để Tuấn không phải gõ tay.

**Bước 4 — Multi-child:** Tuấn có 2 con học 2 trung tâm → expect 2 invoice riêng (KHÔNG merge). Cần UI parent dashboard list 2 invoices side-by-side.

**Top friction P5:** Bước 1 (email DMARC) + Bước 2 (eInvoice TCT compliance) + Bước 3 (VietQR dynamic) + Bước 4 (multi-child invoice).

### 3.5 P6 Chị Mai (Accountant) walk — Laptop Windows 10

**Bước 1 — Discovery + access:** Mai login với role Accountant → expect dashboard accountant-specific với menu "Hóa đơn tháng" prominent. **Friction #1:** Nếu Mai phải dùng owner dashboard (same Trần) → quá nhiều noise (lớp học, học sinh, lịch). Cần accountant view tinh giản.

**Bước 2 — Batch generate preview:** Mai click "Tạo hóa đơn tháng 5" → preview. **Critical reqs từ kế toán perspective:**
- Export Excel preview TRƯỚC khi confirm (Mai check chéo với MISA)
- Cột breakdown: Mã HV / Tên / Lớp / Học phí cơ bản / Pro-rata / Phụ phí / Tổng
- Đối chiếu enrollment status: chỉ ACTIVE, không INACTIVE/PAUSED

**Friction #2 CRITICAL:** Nếu preview chỉ in-browser → Mai không thể audit cross-system. Cần "Export Excel" button trên preview drawer.

**Bước 3 — Confirm + post to MISA:** Mai cần option "Xuất XML eInvoice → import MISA" hoặc API webhook MISA. **Mental model gap #1:** GAP-297 ship batch tạo internal record; KHÔNG mention MISA integration. Mai phải re-key thủ công 58 hóa đơn vào MISA → friction nghiêm trọng.

**Bước 4 — Reconciliation:** Cuối tháng Mai cần report "Đã thu 45/58, còn 13 chưa thu, total 65M outstanding". **Gap surfaced:** GAP-297 chưa cover collection tracking UX — đây là sister gap follow-up.

**Bước 5 — VND format:** Mọi number column PHẢI VND format `1.500.000đ` (dấu chấm phân cách nghìn, KHÔNG dấu phẩy English). Mai sensitive — wrong format = báo cáo sai → boss complain.

**Top friction P6:** Bước 1 (accountant view missing) + Bước 2 (Excel export preview) + Bước 3 (MISA integration gap) + Bước 4 (collection tracking gap) + Bước 5 (VND format).

---

## §4 Top 5 critical findings per feature (must-fix Phase 1 BETA)

### 4.1 GAP-286 Mobile OTP signup — Top 5 must-fix

| # | Finding | Personas affected | Severity | Recommendation |
|---|---|---|---|---|
| **C1** | **Zalo OA là default OTP channel, không phải SMS fallback** — P1+P3 mental model: Zalo = friendly, SMS = banking spam | P1, P3 (Owner, Student) | 🔴 P0 | Update GAP-286 §AC: Zalo OA push notification PRIMARY (>80% delivery); SMS fallback ONLY khi user chưa add KiteHub OA; UI rõ "Bạn sẽ nhận OTP qua Zalo OA — chưa add? Quét QR ngay" |
| **C2** | **Invited-user signup flow MISSING** (parent/teacher/student không phải tenant owner) | P2, P3, P5 (3/5 personas) | 🔴 P0 | Spawn new sub-gap GAP-286.1: Invited-user signup nhận invite token Zalo → form pre-fill (tên, role, tenant) → chỉ điền số ĐT + OTP. Wave 11 scope MUST include. |
| **C3** | **Phone format VN-friendly: dropdown +84 default + accept `0912345678` raw** — KHÔNG bắt nhập `+84` prefix | All 5 personas | 🟠 P1 | UI form: dropdown +84 preselect + input accept `09xxxxxxxx` HOẶC `+849xxxxxxxx` HOẶC `849xxxxxxxx`. Normalize backend tới E.164. |
| **C4** | **Loading screen progressive message khi provisioning tenant** — KHÔNG raw spinner 30s | P1 (Owner) | 🟠 P1 | Loading state với progress: "Đang tạo trung tâm 30% / Cấu hình lớp mẫu 60% / Sẵn sàng 100%". Tối đa 5s gap không có message → user lo lắng. |
| **C5** | **Email auth headers (SPF/DKIM/DMARC) pass cho `noreply@kitehub.me`** — anti-phishing instinct của P5 (dev background) | P5 (Parent dev) | 🟠 P1 | Verify Cloudflare DNS + Resend domain auth (per existing `secrets-seeding-runbook.md`). Smoke test gửi mail tới Gmail/Outlook → check Authentication-Results pass. |

### 4.2 GAP-297 Batch Invoice — Top 5 must-fix

| # | Finding | Personas affected | Severity | Recommendation |
|---|---|---|---|---|
| **C6** | **VND format mandatory `1.500.000đ`** — KHÔNG dấu phẩy English | P1, P5, P6 (Owner, Parent, Accountant) | 🔴 P0 | Frontend Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }). Backend trả raw number; FE format. Apply mọi UI invoice + preview + email + Zalo notification. |
| **C7** | **eInvoice TCT compliance clarification** — internal billing receipt vs TCT-compliant eInvoice | P5, P6 (Parent dev + Accountant) | 🔴 P0 | Wave 11 plan §Out-of-scope explicit: "Phase 1 BETA = internal billing receipt only; eInvoice TCT defer Phase 1.5 partnership MISA MeInvoice per Wave 93 GAP-185 re-scoped". Show UI badge "Hóa đơn nội bộ — không thay thế hóa đơn điện tử TCT" để P5 hiểu. |
| **C8** | **VietQR dynamic amount + memo pre-fill** trong invoice detail | P3, P5 (Student + Parent) | 🟠 P1 | Generate VietQR per invoice: amount = invoice total, memo = "INV-{id} {tenant} T{month}". Sister gap follow-up cho QR upload payment (per Wave 93 GAP-185 cluster). |
| **C9** | **Idempotency UI: modal warning thay vì silent dedupe** | P1 (Owner) | 🟠 P1 | Khi POST batch-confirm cho month đã có invoices, BE return 200 + `existing_count: 58, new_eligible: 2` → FE modal "Tháng 5 đã có 58 HĐ. Tạo bổ sung 2 HĐ cho học viên mới? [Yes] [No]". |
| **C10** | **Delivery status separate toast (Zalo OA + email)** sau batch-confirm | P1 (Owner) | 🟠 P1 | Sau enqueue notification jobs, poll job status (per `pre-handoff-self-test-completeness.md` §2.9 async flow checklist). UI 2 toasts riêng: "✅ 58 HĐ tạo" + "📨 56/58 đã gửi Zalo; 2 fallback email". Background polling 5s interval. |

---

## §5 Cross-cutting VN cultural blind spots (apply BOTH features)

5 issues affect cả GAP-286 + GAP-297:

### X1 — Zalo dominance > SMS > email priority hierarchy

**Pattern:** Mọi notification channel decision PHẢI follow priority: **Zalo OA push > SMS fallback > Email parallel (audit trail)**.

- Email là "spam folder" cho P2 (95%), P3 (80%), P1 (50%) — KHÔNG được rely on
- SMS associate với OTP banking — friction cảm giác "có phí" hay "spam"
- Zalo OA verified tick là trust anchor #1 trong VN edu SaaS

**Impact:** GAP-286 OTP delivery (X1 applies); GAP-297 invoice notification (X1 applies); GAP-063 SMS/Zalo gap dependency MUST land trước Wave 11 OR Wave 11 plan §Risks acknowledge "fallback email-only nếu GAP-063 chưa ship".

### X2 — Mobile-first + 4G performance constraints

**Pattern:** P1, P2, P3, P5 — 4/5 personas dùng smartphone PRIMARY. 4G connection chậm ở Hà Nội (Cầu Giấy 17h-19h), TP.HCM (Q7 18h-20h).

**Impact:**
- GAP-286 Web OTP API auto-fill phải hoạt động (Android Chrome + iOS Safari 14+)
- GAP-297 batch preview 58 rows lazy-load + summary card before detail
- Background image / asset cache aggressive
- Lighthouse mobile score ≥80 phải gate CI cho FE PRs touching signup/invoice flows

### X3 — VND format consistency `1.500.000đ`

**Pattern:** Format `1,500,000 VND` (English style) = friction cho mọi VN persona. Sister `vn-localization-audit-checklist.md` §2 section 1 mandate.

**Impact:** Apply mọi UI surface: invoice preview / email body / Zalo notification text / dashboard KPI cards / receipt PDF.

### X4 — VN authentic name samples (test fixtures, screenshots, demo content)

**Pattern:** Sample data cần `Nguyễn Văn An / Trần Thị Hồng / Lê Minh Châu` — KHÔNG `John Doe / Jane Smith` placeholder. Per `vn-localization-audit-checklist.md` §2 section 3.

**Impact:** Acceptance test fixtures (per `test-artifact-format-standard.md`) + Storybook stories + E2E test seed data + landing page mockups. Wave 11 plan task: rotate test fixtures Vietnamese names.

### X5 — Cuối tháng dương lịch (KHÔNG cuối quý / KHÔNG cuối tuần xa)

**Pattern:** VN edu fiscal cadence = **cuối tháng dương lịch** (ngày 28-30 mỗi tháng). KHÔNG cuối quý (Mỹ pattern) hoặc cuối tuần Sun (US weekend). Saturday vẫn là ngày làm việc thường ở VN.

**Impact:**
- GAP-297 batch invoice trigger auto-suggest "Hôm nay là ngày 28/05 — Tạo hóa đơn tháng 5?" từ ngày 25 mỗi tháng
- Cron schedule monthly batch fallback ngày 1 hàng tháng (sáng 6h GMT+7) cho enrollment closure cuối tháng
- Reminder Zalo OA: ngày 28-30 mỗi tháng push notification cho P1 owner: "Đã đến cuối tháng — nhớ tạo hóa đơn"
- KHÔNG default Saturday/Sunday làm "ngày nghỉ" cho batch jobs

---

## §6 Recommended bucket scope refinement (input cho Wave 11 plan §1 Brainstorm Q1)

### 6.1 Wave 11 cluster A — GAP-286 Mobile OTP signup (refined scope)

**Original GAP-286 scope:** tenant owner self-signup mobile OTP via Zalo/SMS.

**Audit-refined scope:** tách thành **3 sub-buckets** để cover full persona walks:

| Sub-bucket | Owner persona | New scope items |
|---|---|---|
| **A1 — Owner self-signup** (original) | P1 Trần | Phone form + Zalo OA OTP primary + SMS fallback + tenant provisioning <30s + onboarding wizard |
| **A2 — Invited-user signup** (NEW from C2) | P2 Hương, P5 Tuấn, P3 Linh | Invite token Zalo link → pre-fill form (tenant/role/name) → OTP only → join existing tenant. Teacher / Parent / Student variants. |
| **A3 — Cross-cutting polish** (C3+C4+C5+X1+X2) | All 5 personas | Phone format dropdown +84 / progressive loading message / email DMARC verify / Web OTP API auto-fill / Lighthouse mobile gate |

**Estimated scope expansion:** GAP-286 từ "P0 single mobile signup flow" → "P0 cluster 3 flows (owner/teacher/parent/student)". Wave 11 plan MUST chunk cluster A vào 3 buckets song song HOẶC pivot: ship A1 trong Wave 11, defer A2 sang Wave 12.

### 6.2 Wave 11 cluster B — GAP-297 Batch Invoice (refined scope)

**Original GAP-297 scope:** batch-generate + batch-confirm endpoints + UI owner dashboard.

**Audit-refined scope:** tách thành **4 sub-buckets**:

| Sub-bucket | Owner persona | New scope items |
|---|---|---|
| **B1 — Backend batch endpoints** (original) | (system) | POST /batch-generate preview + POST /batch-confirm persist + idempotency + pro-rata + outbox events |
| **B2 — Owner dashboard UI** (C9+C10) | P1 Trần | Discoverability shortcut card / preview drawer breakdown / modal idempotency warning / delivery status separate toasts / background job poll |
| **B3 — VN compliance + format** (C6+C7+X3) | P5 Tuấn, P6 Mai | VND format Intl.NumberFormat / UI badge "internal billing receipt" + Phase 1.5 eInvoice TCT defer / VietQR dynamic amount per invoice |
| **B4 — Accountant view + Excel export** (P6 Mai) | P6 Mai | Accountant-specific dashboard / Excel export preview / MISA integration acknowledgment (defer Phase 1.5+) / collection tracking placeholder UI |

**Estimated scope expansion:** GAP-297 từ "P0 batch invoice MVP" → "P0 cluster 4 buckets". Wave 11 plan MUST consider: ship B1+B2 trong Wave 11, defer B3+B4 sang Wave 12 (đặc biệt B4 cần partnership với MISA — outside-in audit per `outside-in-coverage-trigger.md` §2.1 architecture-decision keywords).

### 6.3 Wave 11 cluster C — Cross-cutting prerequisites (NEW)

5 cross-cutting findings X1-X5 cần dedicated bucket:

| Sub-bucket | Scope |
|---|---|
| **C1 — Zalo OA integration depth check** | Verify GAP-063 SMS/Zalo gap status; nếu KHÔNG ship Wave 11 → Wave 11 plan §Risks acknowledge "email fallback only" + UI message rõ ràng |
| **C2 — VN format library** | Util `vnFormat.vnd(number)` + `vnFormat.phone(string)` + `vnFormat.date(date)` + Storybook stories for VN format components |
| **C3 — Test fixtures Vietnamese names** | Rotate seed data + E2E fixtures + Storybook props |
| **C4 — Lighthouse mobile gate** | CI workflow add Lighthouse mobile-viewport audit cho signup + invoice pages; gate score ≥80 |

### 6.4 New gap candidates surfaced từ audit (Wave 12+ scope)

| Candidate | Source finding | Phase |
|---|---|---|
| GAP-NEW: Invited-user signup flow (parent/teacher/student variants) | C2 P2+P3+P5 walk | Phase 1 BETA cluster A2 |
| GAP-NEW: Multi-tenant parent UX (single user, multiple tenants) | P5 Bước 4 | Phase 1.5 paid |
| GAP-NEW: VietQR upload payment integration | P3+P5+P6 X1 | Phase 1.5 paid |
| GAP-NEW: MISA MeInvoice partnership integration (eInvoice TCT) | C7 + P6 walk | Phase 1.5+ (per Wave 93 GAP-185 re-scope) |
| GAP-NEW: Collection tracking dashboard (paid/unpaid invoice reconciliation) | P6 Bước 4 | Phase 1.5 paid |
| GAP-NEW: Accountant role + view + Excel export | P6 Bước 1+2 | Phase 1.5 paid |
| GAP-NEW: PWA install prompt / native KiteClass app shell | P3 Bước 5 | Phase 2 |

**Total surfaced:** 7 new gap candidates beyond Wave 11 scope. Coordinator decide nào ship Wave 11 vs defer.

### 6.5 Wave 11 plan §1 Brainstorm Q1 — recommended question framing

Coordinator should ask user trong wave plan §1:

**Q1 (Scope split)**: GAP-286 audit reveals 3 sub-flows (owner / invited-user / cross-cutting polish). Wave 11 ship all 3 sub-buckets song song (3 buckets parallel) OR ship A1 only + defer A2 sang Wave 12?

**Q2 (Dependency)**: GAP-297 batch invoice depends GAP-063 (SMS/Zalo notification). Wave 11 wait GAP-063 ship trước HAY ship GAP-297 với email fallback + UI message "Zalo notification coming Wave 12"?

**Q3 (eInvoice compliance)**: GAP-297 Phase 1 BETA ship "internal billing receipt" only (Phase 1.5+ partnership MISA MeInvoice). Wave 11 plan §Out-of-scope explicit acknowledge này; UI badge "Hóa đơn nội bộ" — user OK với approach này?

**Q4 (Persona breadth)**: Wave 11 originally target P1+P2 BETA cohort. Audit surface P3 (student) + P5 (parent) + P6 (accountant) friction. Wave 11 scope-expand cover P3+P5 invited-user flow OR keep narrow + defer P3/P5/P6 sang Wave 12+?

---

## §7 Audit summary metrics

- **Personas walked:** 5 (P1 Owner, P2 Teacher/Admin, P3 Student, P5 Parent, P6 Accountant)
- **Features audited:** 2 (GAP-286 Mobile OTP, GAP-297 Batch Invoice)
- **Walk simulations:** 10 (5 × 2)
- **Critical findings (must-fix Phase 1 BETA):** 10 (C1-C10, 5 per feature)
- **Cross-cutting findings:** 5 (X1-X5)
- **New gap candidates surfaced:** 7 (Wave 12+ scope)
- **Recommended bucket refinement:** Cluster A (3 sub-buckets), Cluster B (4 sub-buckets), Cluster C (4 sub-buckets cross-cutting prereqs)

---

## §8 References

- **Gap files audited:**
  - `documents/04-quality/gaps/phase-1-beta/GAP-286-mobile-otp-signup-zalo-sms.md`
  - `documents/04-quality/gaps/phase-1-beta/GAP-297-batch-monthly-invoice-generation.md`
- **Methodology skill:** `.claude/skills/quality/persona-based-business-review/SKILL.md`
- **Outside-in trigger rule:** `.claude/rules/outside-in-coverage-trigger.md` v1.1.0
- **VN localization checklist:** `.claude/rules/vn-localization-audit-checklist.md` v1.0.0
- **Sister dependencies:** GAP-063 (SMS/Zalo notification), GAP-287 (Skip wizard branding), GAP-300 (Mid-term class transfer prorate), GAP-185 (VAT/eInvoice — Wave 93 re-scope MISA partnership)
- **Pre-handoff verify checklists referenced:** `.claude/rules/pre-handoff-self-test-completeness.md` §2.7 (multi-tenant), §2.9 (background job/async)

---

## §9 Verdict — input cho Wave 11 plan coordinator

**Outside-in audit successful** — surfaced **3 P0 critical gaps** (C1, C2, C6) + **5 P1 must-fix** + **5 cross-cutting cultural blind spots** + **7 new gap candidates** beyond inside-out scope.

**Coordinator next step:** integrate findings vào Wave 11 plan §1 Brainstorm Q1 (recommend Q1-Q4 framing §6.5); decide bucket refinement (cluster A 3 sub / cluster B 4 sub / cluster C 4 sub); file new gap candidates per `audit-to-gap-pipeline.md` Step 3 cho Wave 12+ scope.

**Cost of outside-in audit:** ~1 agent run + this artifact (~1h coordinator review).
**Cost saved if findings caught at implementation time:** ~3-4 days re-work (mental model gap on Zalo OA + invited-user flow + VN format + eInvoice compliance — each surface late = 1 wave cycle).

→ **Net ROI ~10-20x** per outside-in audit per Wave 93 pattern evidence.
