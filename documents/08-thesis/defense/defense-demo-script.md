---
title: Defense Demo Script — 15 phút walkthrough buổi bảo vệ
chapter: defense
audience: thesis
status: draft
created: 2026-05-23
last-reviewed: 2026-05-23
---

# Defense Demo Script — 15 phút walkthrough

**Mục tiêu:** đưa hội đồng đi qua trải nghiệm thực tế hệ thống KiteHub trong 15 phút, chứng minh sản phẩm chạy được + có người dùng thực tế + có yếu tố khác biệt (multi-tenant + AI Branding + compliance).

**Cấu trúc:** 6 phase × tổng 15 phút. Mỗi phase có script chi tiết (T-X:XX timestamp), fallback nếu fail, và screenshot reference để chuyển slide khi cần.

**Tiền điều kiện trước demo:**
- Laptop kết nối projector + Wi-Fi ổn định (test session ngày T-1)
- 3 tab browser pre-loaded với data seed sẵn (xem §0 Setup)
- Backup recording sẵn sàng phòng khi live demo fail (xem §Backup recording note)
- Reveal.js deck mở sẵn ở slide demo intro (slide 22 hoặc tương đương)

---

## Phase 0 — Setup (trước khi bắt đầu, T-1 giờ → T-0:00)

**Mục tiêu:** chuẩn bị môi trường demo không gặp surprise giữa chừng.

### Setup checklist (T-1 giờ)

- [ ] Stack production health check qua CloudWatch dashboard — confirm tất cả service GREEN
- [ ] Database seed 3 tenant demo:
  - **Tenant A (chính):** `Trung tâm Anh ngữ Sky Education` — owner `chi-hang@skyedu.vn` — 1 lớp + 5 học viên
  - **Tenant B (cô lập demo):** `Trung tâm Toán Quang Minh` — owner `anh-quang@quangminh.vn` — 1 lớp khác + 3 học viên khác
  - **Tenant C (anonymous prospect):** không tồn tại, dùng để demo signup flow mới
- [ ] Browser tabs pre-loaded:
  - **Tab 1:** `https://kitehub.me` (landing public)
  - **Tab 2:** `https://kitehub.me/admin` (admin login với credential `admin@kitehub.me`)
  - **Tab 3:** `https://kitehub.me/dashboard` (đã login sẵn vào Tenant A)
- [ ] Mạng dự phòng: 4G tethering phone (nếu Wi-Fi room chấp nhận được)
- [ ] Screen recording chạy background (OBS Studio hoặc QuickTime) — backup nếu cần reference sau

### Setup script (T-0:00 → T-1:00)

> "Trước khi vào demo, em xin tóm tắt: KiteHub là nền tảng SaaS đa-tenant cho trung tâm giáo dục. Demo 15 phút sẽ đi qua 4 flow chính: signup mới của trung tâm, onboarding tự phục vụ, AI Branding tự động, và chứng minh multi-tenant isolation thực tế."

Mở Tab 1 (landing) — slide deck đổi sang slide demo intro.

---

## Phase 1 — Anonymous Prospect Flow (T-1:00 → T-4:00, 3 phút)

**Mục tiêu:** chứng minh signup tự phục vụ + dashboard prospect view + UX tiếng Việt thân thiện.

### Step 1.1 — Landing page (T-1:00 → T-1:30)

**Action:**
- Mở Tab 1 (kitehub.me landing)
- Cuộn xuống các section: Hero → 3 USP cards → Pricing → CTA Beta access

**Script (≤30 giây):**

> "Đây là landing page. Em focus vào 3 yếu tố: (1) Hero tiếng Việt với positioning rõ — 'nền tảng quản lý trung tâm dạy thêm vừa và nhỏ'; (2) 3 USP cards — Native Multi-tenant, AI Branding, Compliance VN; (3) CTA 'Đăng ký thử nghiệm beta'."

**Highlight visual:** trỏ chuột vào USP cards + pricing table

**Failure fallback:** nếu landing render chậm > 3 giây → chuyển slide 32 (architecture overview) làm bridge, quay lại sau

### Step 1.2 — Beta signup form (T-1:30 → T-2:30)

**Action:**
- Click CTA "Đăng ký thử nghiệm beta"
- Fill form 4 trường:
  - Họ tên: `Trần Thị Hồng`
  - Email: `chi-hong-demo@skyedu.vn`
  - Số điện thoại: `0901 234 567`
  - Tên trung tâm: `Trung tâm Anh ngữ Sky Education`
- Submit form
- Confirm message hiển thị: "Yêu cầu đã gửi, em sẽ phản hồi trong 24 giờ"

**Script (≤60 giây):**

> "Form đăng ký 4 trường — không phức tạp như enterprise software yêu cầu 15 trường. Sau khi submit, request lưu vào bảng `beta_access_requests` với status PENDING. Quản trị nền tảng (em) duyệt request trong dashboard admin — bước tiếp theo."

**Highlight visual:** form validation tiếng Việt + thank-you page

**Failure fallback:** nếu form submit fail → demo từ Tab 2 admin panel có sẵn request pending từ trước, skip Step 1.2

### Step 1.3 — Email notification (T-2:30 → T-4:00)

**Action:**
- Mở Mailtrap inbox (hoặc inbox seed admin) — confirm email xác nhận + magic-link approved arrival
- Show email template tiếng Việt
- Click magic-link → redirect tới setup password page

**Script (≤90 giây):**

> "Sau khi admin duyệt request, hệ thống gửi email magic-link với template tiếng Việt — `Em chào chị Hồng,` greeting đúng tone P2 Center Owner formal. Magic-link contain JWT one-time-use 24h. Click link → setup password lần đầu → tự động login vào dashboard tenant."

**Highlight visual:** email subject + body tiếng Việt + magic-link URL pattern

**Failure fallback:** nếu Mailtrap fail → mở email screenshot trong slide deck (slide email template demo)

---

## Phase 2 — Admin Onboarding & Approval (T-4:00 → T-7:00, 3 phút)

**Mục tiêu:** chứng minh admin console + quy trình duyệt beta access + observability log.

### Step 2.1 — Admin login (T-4:00 → T-4:30)

**Action:**
- Switch Tab 2 (kitehub.me/admin)
- Login với credential `admin@kitehub.me` (password chuẩn bị sẵn)
- Lưu ý 2FA challenge nếu enable — nhập TOTP code

**Script (≤30 giây):**

> "Admin console — phân quyền `PLATFORM_ADMIN`. Login dùng JWT + 2FA challenge cho admin role. Mọi admin action log vào bảng immutable `admin_audit_logs` — PDPL Art 11 tamper-proof requirement."

**Highlight visual:** 2FA prompt + redirect dashboard `/admin`

**Failure fallback:** nếu 2FA fail → dùng tài khoản backup admin không 2FA (chuẩn bị sẵn cho demo)

### Step 2.2 — Beta access dashboard (T-4:30 → T-6:00)

**Action:**
- Navigate `/admin/beta-requests`
- Thấy danh sách request pending — có request `chi-hong-demo@skyedu.vn` vừa submit
- Click vào request → review thông tin
- Click "Approve" → confirm dialog → submit
- Toast notification: "Đã approve, email magic-link gửi đến chị Hồng"

**Script (≤90 giây):**

> "Dashboard hiển thị beta requests sorted theo timestamp. Click vào request → modal hiển thị: thông tin form + dropdown chọn gói (FREE / STARTER / PRO) + checkbox xác nhận đã verify danh tính qua điện thoại. Approve button trigger workflow: tạo tenant_id UUID + provision admin user + gửi magic-link qua kitehub-email service."

**Highlight visual:** beta request modal + approve button + toast

**Failure fallback:** nếu network fail giữa approve → switch sang slide 31 admin workflow sequence diagram, giải thích flow lý thuyết

### Step 2.3 — Audit log immutable (T-6:00 → T-7:00)

**Action:**
- Navigate `/admin/audit-logs`
- Filter theo timestamp gần đây
- Thấy 1 entry: `admin@kitehub.me APPROVE beta_request chi-hong-demo@skyedu.vn`
- Demo immutability: open psql terminal (small window)
- Run: `UPDATE admin_audit_logs SET action='LIE' WHERE id=...`
- Postgres trả error: `ERROR: cannot UPDATE admin_audit_logs — immutable by trigger`

**Script (≤60 giây):**

> "Đây là PDPL Art 11 implementation thực tế. Trigger ở database layer chặn UPDATE và DELETE — ngay cả tài khoản admin với CREATE/READ permission cũng không sửa được history. Đây là multi-layer defense: nếu attacker compromise application layer, database trigger vẫn enforce immutability."

**Highlight visual:** psql error message + SQLSTATE custom

**Failure fallback:** nếu psql terminal khó setup → chuyển sang slide 36 PDPL mapping + show migration file SQL trong file viewer

---

## Phase 3 — Tenant Onboarding Wizard (T-7:00 → T-11:00, 4 phút)

**Mục tiêu:** chứng minh self-service onboarding 7 step + AI Branding sinh logo trực quan.

### Step 3.1 — First-time login (T-7:00 → T-7:30)

**Action:**
- Switch sang browser khác hoặc incognito tab (avoid session conflict với Tab 2)
- Mở email magic-link đã approve ở Phase 2
- Click link → setup password lần đầu: nhập `Demo2026!@#`
- Auto-login → redirect `/onboarding/welcome`

**Script (≤30 giây):**

> "Chị Hồng nhận email approved, click magic-link, đặt password lần đầu, login tự động vào trang welcome onboarding."

### Step 3.2 — Wizard 7 steps (T-7:30 → T-9:30)

**Action:** đi qua 7 step nhanh (mỗi step ~15-20 giây):

| Step | Nội dung | Input demo |
|---|---|---|
| 1. Thông tin trung tâm | Tên, địa chỉ, số chi nhánh | Pre-fill từ form signup |
| 2. Chọn gói dịch vụ | FREE / STARTER / PRO | Click STARTER 500k/tháng |
| 3. Thiết lập domain | Subdomain | Nhập `skyedu` → preview `skyedu.kitehub.me` |
| 4. **AI Branding form** | Tên brand + tone + màu | "Sky Education" + Modern + #1E40AF |
| 5. AI generating | Progress indicator | Skip nhanh (đã pre-generate cho demo) |
| 6. Preview assets | Logo + Hero + Banner | Click "Tôi thích" → save |
| 7. Hoàn tất | CTA "Vào dashboard" | Click → redirect dashboard |

**Script (≤120 giây):**

> "Wizard 7 bước — mỗi bước có progress indicator + back/next navigation. Bước 4 là AI Branding form — chị Hồng nhập tên brand + chọn tone (Modern/Classic/Playful) + primary color. Bước 5 progress indicator AI generating ~30-60 giây thực tế — em pre-generate cho demo để tiết kiệm thời gian. Bước 6 preview 3 assets (logo + hero + social banner) — chị Hồng chọn 'Tôi thích' để lưu, hoặc 'Regenerate' để sinh lại."

**Highlight visual:** wizard progress bar + AI Branding preview với logo + hero thật

**Failure fallback:** nếu wizard step fail giữa chừng → switch sang slide 23-24 (AI pipeline architecture) và mô tả flow lý thuyết, show pre-recorded video AI Branding generation

### Step 3.3 — Dashboard với branding áp dụng (T-9:30 → T-11:00)

**Action:**
- Sau khi finish wizard → redirect `/dashboard`
- Dashboard hiển thị:
  - Logo Sky Education (vừa sinh) ở header
  - Color palette áp dụng (primary blue #1E40AF)
  - KPI card: 0 lớp, 0 học viên, 0 doanh thu (trung tâm mới)
  - CTA: "Tạo lớp đầu tiên" + "Nhập học viên hàng loạt"

**Script (≤90 giây):**

> "Dashboard với branding áp dụng — logo header, palette, hero banner trên welcome card. Chị Hồng có thể bắt đầu sử dụng ngay: tạo lớp, nhập danh sách học viên từ CSV/Excel, lập thời khóa biểu. Toàn bộ flow từ signup → dashboard usable mất khoảng 5-10 phút thực tế — em compress về 4 phút cho demo này."

**Highlight visual:** dashboard hiện ra với branding chị Hồng vừa pick

---

## Phase 4 — Multi-tenant Isolation Proof (T-11:00 → T-13:00, 2 phút)

**Mục tiêu:** chứng minh data isolation thực tế giữa 2 tenant — yếu tố khác biệt của KiteHub.

**Ghi chú:** Phase này cross-link với secondary demo script chi tiết tại `documents/08-thesis/defense/multi-tenant-demo-script.md` (Bucket F — GAP-652). Phase 4 trong báo cáo bảo vệ chỉ là phần ngắn 2 phút highlight; chi tiết RLS proof + cross-tenant 403 ở demo Bucket F.

### Step 4.1 — Tenant A view (T-11:00 → T-11:45)

**Action:**
- Switch Tab 3 (đã login sẵn vào Tenant A — Sky Education)
- Navigate `/classes` → thấy lớp "Lớp Anh ngữ 5A1" với 5 học viên
- Click vào lớp → thấy danh sách: `Trần Thị Hồng`, `Nguyễn Văn An`, `Phạm Thị Mai`, `Lê Văn Quang`, `Hoàng Thị Lan`

**Script (≤45 giây):**

> "Tenant A — Sky Education với 1 lớp 5 học viên. Em đã seed sẵn data này trước demo."

### Step 4.2 — Tenant B view (T-11:45 → T-12:30)

**Action:**
- Logout Tenant A → login Tenant B (anh Quang — Quang Minh)
- Navigate `/classes` → thấy lớp "Lớp Toán 9B" với 3 học viên KHÁC
- Danh sách: `Đặng Văn Bình`, `Vũ Thị Cẩm`, `Phan Văn Đức`
- KHÔNG thấy bất kỳ học viên nào của Tenant A

**Script (≤45 giây):**

> "Tenant B — Quang Minh với 1 lớp khác 3 học viên khác. Quan trọng: không thấy bất kỳ học viên nào của Tenant A — RLS đang enforce ở database level. Đây không phải application filter — đây là Postgres engine enforce."

### Step 4.3 — Cross-tenant probe (T-12:30 → T-13:00)

**Action:**
- Mở DevTools Network tab
- Tạo request thủ công: GET `/api/v1/students/{tenant-a-student-id}` (UUID học viên Tenant A)
- Header Authorization = JWT của Tenant B
- Response: `403 Forbidden` hoặc `404 Not Found` (tùy implementation)

**Script (≤30 giây):**

> "Probe thử: dùng JWT Tenant B, request student UUID của Tenant A — response 403/404. RLS policy enforce ở Postgres không cho phép cross-tenant access ngay cả khi attacker biết UUID. Đây là defense-in-depth — chi tiết trong demo secondary 5 phút riêng nếu hội đồng quan tâm."

**Failure fallback:** nếu DevTools probe khó setup → chuyển sang slide 27-28 (RLS implementation + Defense-in-depth 5 layers) và mô tả lý thuyết

---

## Phase 5 — Audit + Observability (T-13:00 → T-13:30, 30 giây ngắn)

**Mục tiêu:** chứng minh production-grade observability thực tế đang chạy.

**Action:**
- Mở CloudWatch dashboard (link bookmark sẵn)
- Show 3 chart: CPU EC2 ~ 30%, RDS connections ~ 15, ALB request rate ~ 5 req/s
- Mở Grafana Prometheus dashboard (nếu accessible)
- Show metric: `outbox_dispatcher_lag_seconds`, `http_server_requests_seconds` p95

**Script (≤30 giây):**

> "Hệ thống đang chạy production thực tế — CloudWatch monitor CPU, RDS, ALB; Prometheus track application metric như outbox dispatcher lag, p95 latency. Audit trail mọi infrastructure operation qua CloudTrail từ ngày đầu apply terraform. Đây không phải demo trên máy local — đây là production stack đang phục vụ tenant thực tế."

---

## Phase 6 — Wrap + Q&A transition (T-13:30 → T-15:00, 1.5 phút)

**Mục tiêu:** tổng kết demo + mời câu hỏi.

### Step 6.1 — KPI summary (T-13:30 → T-14:30)

**Action:**
- Chuyển slide deck sang slide 27-28 (KPI overview + KPI trajectory)
- Highlight 3 con số chính:
  - Performance 86/100 B+
  - Security 93/100 A
  - Quality 90/110 B+

**Script (≤60 giây):**

> "Tóm tắt demo: chúng ta vừa đi qua signup → onboarding tự phục vụ → AI Branding tự động → multi-tenant isolation thực tế → production observability. Các con số validating: Performance 86, Security 93, Quality 90 — vượt ngưỡng PASS giai đoạn thử nghiệm. Quan trọng nhất, em đã có 4 trung tâm thực tế ký xác nhận sử dụng — không phải demo trên máy."

### Step 6.2 — Limitations honest (T-14:30 → T-15:00)

**Action:** chuyển sang slide 36 (limitations honest assessment).

**Script (≤30 giây):**

> "Em xin nhắc lại các hạn chế thừa nhận: AWS Singapore chưa data localization VN — roadmap migrate khi vượt ngưỡng; chưa có legal counsel review chính thức — đang dùng disclaimer cho non-K-12; chưa có mobile native app. Tất cả có roadmap rõ ràng. Em xin nhận các câu hỏi của hội đồng."

### Step 6.3 — Q&A transition (T-15:00)

**Action:** chuyển slide 39 (Q&A invitation).

> "Cảm ơn thời gian quý hội đồng. Em sẵn sàng trả lời câu hỏi."

---

## Backup Recording Note

**Mục đích:** nếu live demo fail (network down, AWS region issue, bug critical), có backup recording chạy thay thế.

**Cách record:**
1. Tool: **OBS Studio** (open-source, đa nền tảng)
2. Setup: capture browser window 1920×1080, audio voiceover qua microphone
3. Record toàn bộ 15-phút demo theo script trên — 1 lần record tại T-3 ngày trước defense
4. Output: `documents/08-thesis/defense/backup-demo.mp4` (hoặc upload Google Drive nếu file > 100MB)
5. Test play trên máy laptop sẽ dùng defense — đảm bảo codec compatible

**Khi nào dùng backup recording:**
- Network down giữa demo > 30 giây
- AWS region issue khiến endpoint return 5xx > 2 lần
- Critical bug surface trong demo (ví dụ login fail repeatedly)
- Time pressure: nếu mất quá 5 phút để debug live, switch backup ngay

**Cách switch:**
1. Pause live demo, calmly nói: "Em xin chuyển sang demo recording vì lý do kỹ thuật — recording mô tả chính xác flow vừa rồi."
2. Mở `backup-demo.mp4` fullscreen, play từ timestamp tương ứng phase đang dở
3. Tiếp tục voiceover sync với recording (recording đã có voiceover sẵn — chỉ cần đứng yên cho hội đồng xem)

**Retention strategy:**
- Backup recording lưu local + Google Drive backup
- Không commit vào repo (file lớn > 100MB) — chỉ commit script này tham chiếu file
- Sau defense → archive `documents/07-archived/thesis-defense-2026/`

---

## Pre-Demo Checklist (T-1 ngày trước defense)

- [ ] Stack production health all GREEN qua CloudWatch
- [ ] Database seed 3 tenant demo theo §Phase 0 setup
- [ ] Browser tabs pre-loaded — không cần login giữa demo
- [ ] Magic-link email pre-generated cho Tenant C signup demo
- [ ] AI Branding pre-generated cho Sky Education — tránh wait 30-60s thực tế
- [ ] Backup recording test play OK trên laptop demo
- [ ] Projector adapter test với laptop demo
- [ ] Mạng dự phòng 4G tethering test connection OK
- [ ] Slide deck mở sẵn ở slide demo intro
- [ ] Defense Q&A response sheet print 1 bản giấy backup
- [ ] Báo cáo chính bản cứng x 3 (cho hội đồng) + bản mềm USB backup
- [ ] Đi ngủ sớm — defense ngày mai

---

## Log

- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 6 phase × tổng 15 phút (Setup → Anonymous prospect → Admin onboarding → Tenant onboarding → Multi-tenant proof → Audit + Wrap). Cross-link với multi-tenant secondary demo script (Bucket F GAP-652).
