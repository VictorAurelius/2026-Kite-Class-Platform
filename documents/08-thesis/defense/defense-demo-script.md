---
title: Kịch bản demo bảo vệ — 15 phút đi qua hệ thống buổi bảo vệ
chapter: defense
audience: dev
status: draft
created: 2026-05-23
last-reviewed: 2026-05-23
---

# Kịch bản demo bảo vệ — 15 phút

**Mục tiêu:** đưa hội đồng đi qua trải nghiệm thực tế hệ thống KiteHub trong 15 phút, chứng minh sản phẩm chạy được + có người dùng thực tế + có yếu tố khác biệt (multi-tenant + AI Branding + compliance).

**Cấu trúc:** 6 pha × tổng 15 phút. Mỗi pha có văn nói chi tiết (mốc thời gian T-X:XX), phương án dự phòng nếu lỗi, và ảnh chụp màn hình tham chiếu để chuyển slide khi cần.

**Tiền điều kiện trước demo:**
- Laptop kết nối máy chiếu + Wi-Fi ổn định (buổi kiểm thử ngày T-1)
- 3 tab browser nạp sẵn với dữ liệu seed sẵn (xem §Pha 0)
- Bản ghi dự phòng sẵn sàng phòng khi demo trực tiếp lỗi (xem §Ghi chú bản ghi dự phòng)
- Reveal.js deck mở sẵn ở slide demo intro (slide 22 hoặc tương đương)

---

## Pha 0 — Chuẩn bị (trước khi bắt đầu, T-1 giờ → T-0:00)

**Mục tiêu:** chuẩn bị môi trường demo không gặp bất ngờ giữa chừng.

### Danh sách kiểm tra chuẩn bị (T-1 giờ)

- [ ] Kiểm tra sức khỏe stack production qua CloudWatch dashboard — xác nhận tất cả service GREEN
- [ ] Seed database 3 tenant demo:
  - **Tenant A (chính):** `Trung tâm Anh ngữ Sky Education` — owner `chi-hang@skyedu.vn` — 1 lớp + 5 học viên
  - **Tenant B (cô lập demo):** `Trung tâm Toán Quang Minh` — owner `anh-quang@quangminh.vn` — 1 lớp khác + 3 học viên khác
  - **Tenant C (khách tham quan):** không tồn tại, dùng để demo luồng signup mới
- [ ] Tab browser nạp sẵn:
  - **Tab 1:** `https://kitehub.me` (landing công khai)
  - **Tab 2:** `https://kitehub.me/admin` (đăng nhập admin với thông tin đăng nhập `admin@kitehub.me`)
  - **Tab 3:** `https://kitehub.me/dashboard` (đã đăng nhập sẵn vào Tenant A)
- [ ] Mạng dự phòng: 4G tethering từ điện thoại (nếu Wi-Fi phòng họp chấp nhận được)
- [ ] Bản ghi màn hình chạy nền (OBS Studio hoặc QuickTime) — dự phòng nếu cần tham chiếu sau

### Văn nói chuẩn bị (T-0:00 → T-1:00)

> "Trước khi vào demo, em xin tóm tắt: KiteHub là nền tảng SaaS đa-tenant cho trung tâm giáo dục. Demo 15 phút sẽ đi qua 4 luồng chính: signup mới của trung tâm, onboarding tự phục vụ, AI Branding tự động, và chứng minh multi-tenant isolation thực tế."

Mở Tab 1 (landing) — slide deck đổi sang slide demo intro.

---

## Pha 1 — Luồng khách tham quan (T-1:00 → T-4:00, 3 phút)

**Mục tiêu:** chứng minh signup tự phục vụ + dashboard cho khách tham quan + trải nghiệm người dùng tiếng Việt thân thiện.

### Bước 1.1 — Trang chủ landing (T-1:00 → T-1:30)

**Thao tác:**
- Mở Tab 1 (kitehub.me landing)
- Cuộn xuống các phần: Hero → 3 USP card → bảng giá → CTA đăng ký beta

**Văn nói (≤30 giây):**

> "Đây là trang chủ landing. Em tập trung vào 3 yếu tố: (1) Hero tiếng Việt với định vị rõ — 'nền tảng quản lý trung tâm dạy thêm vừa và nhỏ'; (2) 3 USP card — Native Multi-tenant, AI Branding, Compliance VN; (3) CTA 'Đăng ký thử nghiệm beta'."

**Hình ảnh nhấn:** trỏ chuột vào USP card + bảng giá

**Dự phòng khi lỗi:** nếu landing render chậm > 3 giây → chuyển slide 32 (tổng quan kiến trúc) làm cầu nối, quay lại sau

### Bước 1.2 — Form đăng ký beta (T-1:30 → T-2:30)

**Thao tác:**
- Click CTA "Đăng ký thử nghiệm beta"
- Điền form 4 trường:
  - Họ tên: `Trần Thị Hồng`
  - Email: `chi-hong-demo@skyedu.vn`
  - Số điện thoại: `0901 234 567`
  - Tên trung tâm: `Trung tâm Anh ngữ Sky Education`
- Gửi form
- Xác nhận thông báo hiển thị: "Yêu cầu đã gửi, em sẽ phản hồi trong 24 giờ"

**Văn nói (≤60 giây):**

> "Form đăng ký 4 trường — không phức tạp như phần mềm doanh nghiệp yêu cầu 15 trường. Sau khi gửi, yêu cầu lưu vào bảng `beta_access_requests` với trạng thái PENDING. Quản trị nền tảng (em) duyệt yêu cầu trong dashboard admin — bước tiếp theo."

**Hình ảnh nhấn:** kiểm tra hợp lệ form tiếng Việt + trang cảm ơn

**Dự phòng khi lỗi:** nếu gửi form lỗi → demo từ Tab 2 bảng quản trị có sẵn yêu cầu chờ duyệt từ trước, bỏ qua Bước 1.2

### Bước 1.3 — Thông báo qua email (T-2:30 → T-4:00)

**Thao tác:**
- Mở hộp thư Mailtrap (hoặc hộp thư seed admin) — xác nhận email xác nhận + magic-link đã duyệt về đến nơi
- Hiển thị mẫu email tiếng Việt
- Click magic-link → chuyển hướng tới trang đặt mật khẩu

**Văn nói (≤90 giây):**

> "Sau khi admin duyệt yêu cầu, hệ thống gửi email magic-link với mẫu tiếng Việt — `Em chào chị Hồng,` lời chào đúng tone P2 Center Owner trang trọng. Magic-link chứa JWT dùng một lần trong 24h. Click link → đặt mật khẩu lần đầu → tự động đăng nhập vào dashboard tenant."

**Hình ảnh nhấn:** tiêu đề email + nội dung tiếng Việt + mẫu URL magic-link

**Dự phòng khi lỗi:** nếu Mailtrap lỗi → mở ảnh chụp màn hình email trong slide deck (slide mẫu email demo)

---

## Pha 2 — Tiếp nhận & duyệt của quản trị (T-4:00 → T-7:00, 3 phút)

**Mục tiêu:** chứng minh bảng điều khiển admin + quy trình duyệt beta access + log observability.

### Bước 2.1 — Đăng nhập admin (T-4:00 → T-4:30)

**Thao tác:**
- Chuyển sang Tab 2 (kitehub.me/admin)
- Đăng nhập với thông tin đăng nhập `admin@kitehub.me` (mật khẩu chuẩn bị sẵn)
- Lưu ý thử thách 2FA nếu bật — nhập mã TOTP

**Văn nói (≤30 giây):**

> "Bảng điều khiển admin — phân quyền `PLATFORM_ADMIN`. Đăng nhập dùng JWT + thử thách 2FA cho vai trò admin. Mọi thao tác admin ghi log vào bảng bất biến `admin_audit_logs` — yêu cầu chống giả mạo theo PDPL Art 11."

**Hình ảnh nhấn:** hộp thoại 2FA + chuyển hướng dashboard `/admin`

**Dự phòng khi lỗi:** nếu 2FA lỗi → dùng tài khoản admin dự phòng không 2FA (chuẩn bị sẵn cho demo)

### Bước 2.2 — Bảng điều khiển beta access (T-4:30 → T-6:00)

**Thao tác:**
- Điều hướng `/admin/beta-requests`
- Thấy danh sách yêu cầu chờ duyệt — có yêu cầu `chi-hong-demo@skyedu.vn` vừa gửi
- Click vào yêu cầu → xem xét thông tin
- Click "Approve" → hộp thoại xác nhận → gửi
- Thông báo toast: "Đã duyệt, email magic-link gửi đến chị Hồng"

**Văn nói (≤90 giây):**

> "Dashboard hiển thị beta requests sắp xếp theo mốc thời gian. Click vào yêu cầu → modal hiển thị: thông tin form + dropdown chọn gói (FREE / STARTER / PRO) + checkbox xác nhận đã xác minh danh tính qua điện thoại. Nút Approve kích hoạt quy trình: tạo tenant_id UUID + cấp phát người dùng admin + gửi magic-link qua service kitehub-email."

**Hình ảnh nhấn:** modal beta request + nút approve + toast

**Dự phòng khi lỗi:** nếu mạng lỗi giữa lúc duyệt → chuyển sang slide 31 sequence diagram quy trình admin, giải thích luồng lý thuyết

### Bước 2.3 — Log kiểm toán bất biến (T-6:00 → T-7:00)

**Thao tác:**
- Điều hướng `/admin/audit-logs`
- Lọc theo mốc thời gian gần đây
- Thấy 1 bản ghi: `admin@kitehub.me APPROVE beta_request chi-hong-demo@skyedu.vn`
- Demo tính bất biến: mở terminal psql (cửa sổ nhỏ)
- Chạy: `UPDATE admin_audit_logs SET action='LIE' WHERE id=...`
- Postgres trả lỗi: `ERROR: cannot UPDATE admin_audit_logs — immutable by trigger`

**Văn nói (≤60 giây):**

> "Đây là cài đặt PDPL Art 11 thực tế. Trigger ở tầng database chặn UPDATE và DELETE — ngay cả tài khoản admin với quyền CREATE/READ cũng không sửa được lịch sử. Đây là phòng thủ nhiều lớp: nếu kẻ tấn công xâm nhập tầng ứng dụng, trigger database vẫn cưỡng chế tính bất biến."

**Hình ảnh nhấn:** thông báo lỗi psql + SQLSTATE tùy biến

**Dự phòng khi lỗi:** nếu terminal psql khó cài đặt → chuyển sang slide 36 ánh xạ PDPL + hiển thị SQL của file migration trong trình xem file

---

## Pha 3 — Wizard tiếp nhận tenant (T-7:00 → T-11:00, 4 phút)

**Mục tiêu:** chứng minh onboarding tự phục vụ 7 bước + AI Branding sinh logo trực quan.

### Bước 3.1 — Đăng nhập lần đầu (T-7:00 → T-7:30)

**Thao tác:**
- Chuyển sang browser khác hoặc tab ẩn danh (tránh xung đột session với Tab 2)
- Mở email magic-link đã duyệt ở Pha 2
- Click link → đặt mật khẩu lần đầu: nhập `Demo2026!@#`
- Tự động đăng nhập → chuyển hướng `/onboarding/welcome`

**Văn nói (≤30 giây):**

> "Chị Hồng nhận email đã duyệt, click magic-link, đặt mật khẩu lần đầu, đăng nhập tự động vào trang welcome onboarding."

### Bước 3.2 — Wizard 7 bước (T-7:30 → T-9:30)

**Thao tác:** đi qua 7 bước nhanh (mỗi bước ~15-20 giây):

| Bước | Nội dung | Đầu vào demo |
|---|---|---|
| 1. Thông tin trung tâm | Tên, địa chỉ, số chi nhánh | Điền sẵn từ form signup |
| 2. Chọn gói dịch vụ | FREE / STARTER / PRO | Chọn STARTER 500k/tháng |
| 3. Thiết lập domain | Subdomain | Nhập `skyedu` → xem trước `skyedu.kitehub.me` |
| 4. **Form AI Branding** | Tên thương hiệu + tone + màu | "Sky Education" + Modern + #1E40AF |
| 5. AI đang sinh | Thanh tiến trình | Bỏ qua nhanh (đã sinh sẵn cho demo) |
| 6. Xem trước assets | Logo + Hero + Banner | Chọn "Tôi thích" → lưu |
| 7. Hoàn tất | CTA "Vào dashboard" | Click → chuyển hướng dashboard |

**Văn nói (≤120 giây):**

> "Wizard 7 bước — mỗi bước có thanh tiến trình + điều hướng tới/lui. Bước 4 là form AI Branding — chị Hồng nhập tên thương hiệu + chọn tone (Modern/Classic/Playful) + màu chủ đạo. Bước 5 thanh tiến trình AI đang sinh ~30-60 giây thực tế — em sinh sẵn cho demo để tiết kiệm thời gian. Bước 6 xem trước 3 assets (logo + hero + social banner) — chị Hồng chọn 'Tôi thích' để lưu, hoặc 'Regenerate' để sinh lại."

**Hình ảnh nhấn:** thanh tiến trình wizard + xem trước AI Branding với logo + hero thật

**Dự phòng khi lỗi:** nếu bước wizard lỗi giữa chừng → chuyển sang slide 23-24 (kiến trúc pipeline AI) và mô tả luồng lý thuyết, hiển thị video quay sẵn quá trình sinh AI Branding

### Bước 3.3 — Dashboard với branding đã áp dụng (T-9:30 → T-11:00)

**Thao tác:**
- Sau khi hoàn tất wizard → chuyển hướng `/dashboard`
- Dashboard hiển thị:
  - Logo Sky Education (vừa sinh) ở header
  - Bảng màu đã áp dụng (xanh chủ đạo #1E40AF)
  - KPI card: 0 lớp, 0 học viên, 0 doanh thu (trung tâm mới)
  - CTA: "Tạo lớp đầu tiên" + "Nhập học viên hàng loạt"

**Văn nói (≤90 giây):**

> "Dashboard với branding đã áp dụng — logo header, bảng màu, hero banner trên welcome card. Chị Hồng có thể bắt đầu sử dụng ngay: tạo lớp, nhập danh sách học viên từ CSV/Excel, lập thời khóa biểu. Toàn bộ luồng từ signup → dashboard dùng được mất khoảng 5-10 phút thực tế — em nén lại còn 4 phút cho demo này."

**Hình ảnh nhấn:** dashboard hiện ra với branding chị Hồng vừa chọn

---

## Pha 4 — Chứng minh cô lập multi-tenant (T-11:00 → T-13:00, 2 phút)

**Mục tiêu:** chứng minh cô lập dữ liệu thực tế giữa 2 tenant — yếu tố khác biệt của KiteHub.

**Ghi chú:** Pha này liên kết chéo với kịch bản demo phụ trợ chi tiết tại `documents/08-thesis/defense/multi-tenant-demo-script.md` (Bucket F — GAP-652). Pha 4 trong báo cáo bảo vệ chỉ là phần ngắn 2 phút điểm nhấn; chi tiết chứng minh RLS + cross-tenant 403 ở demo Bucket F.

### Bước 4.1 — Góc nhìn Tenant A (T-11:00 → T-11:45)

**Thao tác:**
- Chuyển sang Tab 3 (đã đăng nhập sẵn vào Tenant A — Sky Education)
- Điều hướng `/classes` → thấy lớp "Lớp Anh ngữ 5A1" với 5 học viên
- Click vào lớp → thấy danh sách: `Trần Thị Hồng`, `Nguyễn Văn An`, `Phạm Thị Mai`, `Lê Văn Quang`, `Hoàng Thị Lan`

**Văn nói (≤45 giây):**

> "Tenant A — Sky Education với 1 lớp 5 học viên. Em đã seed sẵn dữ liệu này trước demo."

### Bước 4.2 — Góc nhìn Tenant B (T-11:45 → T-12:30)

**Thao tác:**
- Đăng xuất Tenant A → đăng nhập Tenant B (anh Quang — Quang Minh)
- Điều hướng `/classes` → thấy lớp "Lớp Toán 9B" với 3 học viên KHÁC
- Danh sách: `Đặng Văn Bình`, `Vũ Thị Cẩm`, `Phan Văn Đức`
- KHÔNG thấy bất kỳ học viên nào của Tenant A

**Văn nói (≤45 giây):**

> "Tenant B — Quang Minh với 1 lớp khác 3 học viên khác. Quan trọng: không thấy bất kỳ học viên nào của Tenant A — RLS đang cưỡng chế ở tầng database. Đây không phải bộ lọc tầng ứng dụng — đây là engine Postgres cưỡng chế."

### Bước 4.3 — Thử truy cập chéo tenant (T-12:30 → T-13:00)

**Thao tác:**
- Mở DevTools tab Network
- Tạo request thủ công: GET `/api/v1/students/{tenant-a-student-id}` (UUID học viên Tenant A)
- Header Authorization = JWT của Tenant B
- Response: `403 Forbidden` hoặc `404 Not Found` (tùy cài đặt)

**Văn nói (≤30 giây):**

> "Thử dò: dùng JWT Tenant B, request UUID học viên của Tenant A — response 403/404. RLS policy cưỡng chế ở Postgres không cho phép truy cập chéo tenant ngay cả khi kẻ tấn công biết UUID. Đây là phòng thủ theo chiều sâu — chi tiết trong demo phụ trợ 5 phút riêng nếu hội đồng quan tâm."

**Dự phòng khi lỗi:** nếu thử dò DevTools khó cài đặt → chuyển sang slide 27-28 (cài đặt RLS + phòng thủ theo chiều sâu 5 lớp) và mô tả lý thuyết

---

## Pha 5 — Kiểm toán + Observability (T-13:00 → T-13:30, 30 giây ngắn)

**Mục tiêu:** chứng minh observability cấp production thực tế đang chạy.

**Thao tác:**
- Mở CloudWatch dashboard (đường dẫn bookmark sẵn)
- Hiển thị 3 biểu đồ: CPU EC2 ~ 30%, RDS connections ~ 15, ALB request rate ~ 5 req/s
- Mở Grafana Prometheus dashboard (nếu truy cập được)
- Hiển thị metric: `outbox_dispatcher_lag_seconds`, `http_server_requests_seconds` p95

**Văn nói (≤30 giây):**

> "Hệ thống đang chạy production thực tế — CloudWatch giám sát CPU, RDS, ALB; Prometheus theo dõi metric ứng dụng như outbox dispatcher lag, độ trễ p95. Vết kiểm toán mọi thao tác hạ tầng qua CloudTrail từ ngày đầu apply terraform. Đây không phải demo trên máy local — đây là stack production đang phục vụ tenant thực tế."

---

## Pha 6 — Tổng kết + chuyển sang Q&A (T-13:30 → T-15:00, 1.5 phút)

**Mục tiêu:** tổng kết demo + mời câu hỏi.

### Bước 6.1 — Tóm tắt KPI (T-13:30 → T-14:30)

**Thao tác:**
- Chuyển slide deck sang slide 27-28 (tổng quan KPI + quỹ đạo KPI)
- Nhấn mạnh 3 con số chính:
  - Performance 86/100 B+
  - Security 93/100 A
  - Quality 90/110 B+

**Văn nói (≤60 giây):**

> "Tóm tắt demo: chúng ta vừa đi qua signup → onboarding tự phục vụ → AI Branding tự động → multi-tenant isolation thực tế → observability production. Các con số kiểm chứng: Performance 86, Security 93, Quality 90 — vượt ngưỡng PASS giai đoạn thử nghiệm. Quan trọng nhất, em đã có 4 trung tâm thực tế ký xác nhận sử dụng — không phải demo trên máy."

### Bước 6.2 — Hạn chế thừa nhận thẳng thắn (T-14:30 → T-15:00)

**Thao tác:** chuyển sang slide 36 (đánh giá hạn chế thừa nhận thẳng thắn).

**Văn nói (≤30 giây):**

> "Em xin nhắc lại các hạn chế thừa nhận: AWS Singapore chưa data localization VN — lộ trình di trú khi vượt ngưỡng; chưa có legal counsel review chính thức — đang dùng disclaimer cho non-K-12; chưa có ứng dụng mobile native. Tất cả có lộ trình rõ ràng. Em xin nhận các câu hỏi của hội đồng."

### Bước 6.3 — Chuyển sang Q&A (T-15:00)

**Thao tác:** chuyển slide 39 (lời mời Q&A).

> "Cảm ơn thời gian quý hội đồng. Em sẵn sàng trả lời câu hỏi."

---

## Ghi chú bản ghi dự phòng

**Mục đích:** nếu demo trực tiếp lỗi (mất mạng, sự cố AWS region, lỗi nghiêm trọng), có bản ghi dự phòng chạy thay thế.

**Cách quay:**
1. Công cụ: **OBS Studio** (mã nguồn mở, đa nền tảng)
2. Thiết lập: thu cửa sổ browser 1920×1080, lồng tiếng qua microphone
3. Quay toàn bộ demo 15 phút theo văn nói trên — 1 lần quay tại T-3 ngày trước bảo vệ
4. Kết quả: `documents/08-thesis/defense/backup-demo.mp4` (hoặc tải lên Google Drive nếu file > 100MB)
5. Thử phát trên laptop sẽ dùng khi bảo vệ — đảm bảo codec tương thích

**Khi nào dùng bản ghi dự phòng:**
- Mất mạng giữa demo > 30 giây
- Sự cố AWS region khiến endpoint trả về 5xx > 2 lần
- Lỗi nghiêm trọng nổi lên trong demo (ví dụ đăng nhập lỗi liên tục)
- Áp lực thời gian: nếu mất quá 5 phút để gỡ lỗi trực tiếp, chuyển bản dự phòng ngay

**Cách chuyển:**
1. Tạm dừng demo trực tiếp, bình tĩnh nói: "Em xin chuyển sang bản ghi demo vì lý do kỹ thuật — bản ghi mô tả chính xác luồng vừa rồi."
2. Mở `backup-demo.mp4` toàn màn hình, phát từ mốc thời gian tương ứng pha đang dở
3. Tiếp tục lồng tiếng đồng bộ với bản ghi (bản ghi đã có lồng tiếng sẵn — chỉ cần đứng yên cho hội đồng xem)

**Chiến lược lưu trữ:**
- Bản ghi dự phòng lưu local + sao lưu Google Drive
- Không commit vào repo (file lớn > 100MB) — chỉ commit kịch bản này để tham chiếu file
- Sau bảo vệ → lưu trữ `documents/07-archived/thesis-defense-2026/`

---

## Danh sách kiểm tra trước demo (T-1 ngày trước bảo vệ)

- [ ] Sức khỏe stack production toàn bộ GREEN qua CloudWatch
- [ ] Seed database 3 tenant demo theo §Pha 0 chuẩn bị
- [ ] Tab browser nạp sẵn — không cần đăng nhập giữa demo
- [ ] Email magic-link sinh sẵn cho demo signup Tenant C
- [ ] AI Branding sinh sẵn cho Sky Education — tránh chờ 30-60s thực tế
- [ ] Bản ghi dự phòng thử phát OK trên laptop demo
- [ ] Thử bộ chuyển đổi máy chiếu với laptop demo
- [ ] Mạng dự phòng 4G tethering thử kết nối OK
- [ ] Slide deck mở sẵn ở slide demo intro
- [ ] In 1 bản giấy dự phòng tờ trả lời Q&A bảo vệ
- [ ] Báo cáo chính bản cứng x 3 (cho hội đồng) + bản mềm USB dự phòng
- [ ] Đi ngủ sớm — mai bảo vệ

---

## Log

- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 6 phase × tổng 15 phút (Setup → Anonymous prospect → Admin onboarding → Tenant onboarding → Multi-tenant proof → Audit + Wrap). Cross-link với multi-tenant secondary demo script (Bucket F GAP-652).
