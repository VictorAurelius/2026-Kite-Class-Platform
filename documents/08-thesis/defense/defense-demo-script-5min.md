---
title: Kịch bản demo bảo vệ trực tiếp — 5 phút (AWS production thật)
chapter: defense
audience: dev
status: ready
created: 2026-06-17
last-reviewed: 2026-06-17
related: [defense-demo-script.md, defense-speaker-script-20slide.md, KiteHub-baove-khoaluan-20slide.pptx]
---

# Kịch bản demo bảo vệ trực tiếp — 5 phút (chèn tại slide 20)

> **Bản 5 phút này** là kịch bản demo CHÍNH cho buổi bảo vệ, chạy trên **hệ thống production thật tại AWS Singapore** (`kitehub.me`). Bản đầy đủ 15 phút (`defense-demo-script.md`) giữ làm **bản mở rộng/dự phòng** nếu hội đồng muốn xem sâu thêm.
>
> **Triết lý 5 phút:** không cố trình diễn mọi tính năng. Chỉ tập trung **hai đóng góp chính của đề tài** + bằng chứng "chạy thật":
> 1. **Cô lập đa-tenant gốc** (PostgreSQL RLS) — đóng góp kỹ thuật mạnh nhất, chứng minh ngay trên màn hình.
> 2. **Tự động hóa nhận diện thương hiệu (AI Branding)** — yếu tố khác biệt thị trường.
>
> Mọi thao tác đều trên hệ thống đang phục vụ thật, không phải mô hình chạy local.

---

## Tổng quan phân bổ thời gian (5:00)

| Đoạn | Thời lượng | Nội dung | Mục tiêu chứng minh |
|---|:---:|---|---|
| 0 | (trước demo) | Thiết lập + dự phòng | Không gặp bất ngờ giữa chừng |
| 1 | 0:00 → 0:30 | Trang chủ thương hiệu riêng (điểm neo) | Phân giải Tenant → Domain → Landing chạy thật |
| 2 | 0:30 → 2:15 | AI Branding — gói Miễn phí vs Trả phí | Đóng góp 2: tự động hóa nhận diện thương hiệu |
| 3 | 2:15 → 4:30 | Cô lập đa-tenant — 2 tài khoản + probe chéo | Đóng góp 1: RLS ép buộc ở tầng database |
| 4 | 4:30 → 5:00 | Audit log bất biến + chốt | PDPL Điều 11 + mời câu hỏi |

**Tổng nói khi demo:** ~5 phút. Nếu vượt giờ → đoạn 4 (audit log) là phần cắt được đầu tiên (chuyển thành câu nói 1 dòng).

---

## Đoạn 0 — Thiết lập (chuẩn bị trước, KHÔNG tính trong 5 phút demo)

### Tiền điều kiện AWS production (T-1 ngày)

> ⚠️ **Quan trọng:** hạ tầng AWS hiện ở trạng thái STOPPED để tiết kiệm Free Tier. PHẢI khởi động lại + để ổn định trước buổi bảo vệ.

- [ ] **T-1 ngày — khởi động stack:** `bash scripts/aws/start-stack.sh` (restart 2 EC2 + RDS). Chờ ~5-10 phút cho service healthy.
- [ ] **Kiểm tra sức khỏe:** CloudWatch dashboard — toàn bộ service GREEN; `curl -sI https://kitehub.me/` trả 200; đăng nhập thử 1 tenant thành công.
- [ ] **Để stack chạy liên tục từ T-1 đến hết buổi bảo vệ** (không stop giữa chừng).
- [ ] **Seed 2 tenant demo** (tên giả định, KHÔNG dùng dữ liệu người thật):
  - **Tenant A — gói Miễn phí:** `Trung tâm Anh ngữ Sao Mai (giả định)` — chủ `Nguyễn Thị Lan (giả định)` — 1 lớp + 5 học viên (tên giả định), branding theo **mẫu dựng sẵn**.
  - **Tenant B — gói Trả phí:** `Trung tâm Hóa học Minh Trí (giả định)` — chủ `Trần Văn Đức (giả định)` — 1 lớp khác + 3 học viên khác, branding **sinh tự động qua AI** (chủ đề Hóa học, tông màu khác hẳn).
- [ ] **Sinh sẵn AI Branding** cho Tenant B từ trước — tránh chờ 30-60 giây sinh ảnh trực tiếp.
- [ ] **Tải sẵn 3 tab trình duyệt** (đã đăng nhập sẵn, tránh gõ mật khẩu giữa demo):
  - Tab 1: `https://saomai.kitehub.me` (landing Tenant A) — hoặc subdomain tương ứng.
  - Tab 2: đăng nhập sẵn dashboard Tenant A.
  - Tab 3: đăng nhập sẵn dashboard Tenant B.
- [ ] **Mạng dự phòng:** 4G tethering điện thoại (nếu Wi-Fi phòng cho phép).
- [ ] **Bản ghi dự phòng** `backup-demo.mp4` sẵn sàng (xem cuối file) — phòng khi mạng/region trục trặc.
- [ ] **Deck mở sẵn** ở slide 20 (slide cầu nối demo).

### Câu chuyển từ slide 20 (deck) sang demo (≤15 giây)

> "Để chứng minh hệ thống chạy thật chứ không phải mô hình, em xin demo trực tiếp trên hệ thống production đang đặt tại AWS Singapore. Trong năm phút, em sẽ tập trung vào hai đóng góp chính: cô lập đa-tenant ở tầng database, và tự động hóa nhận diện thương hiệu."

Mở Tab 1 (landing thương hiệu riêng).

---

## Đoạn 1 — Trang chủ thương hiệu riêng (0:00 → 0:30)

**Mục tiêu:** chứng minh phân giải Tenant → Domain → Landing chạy thật — mỗi trung tâm có một trang chủ riêng theo thương hiệu.

**Thao tác:**
- Tab 1 đang ở `saomai.kitehub.me` (landing Tenant A).
- Trỏ chuột vào logo + tông màu + tên trung tâm trên hero.

**Văn nói (~30 giây):**

> "Đây là trang chủ của một trung tâm thật trên hệ thống, truy cập qua subdomain riêng. Khi người dùng vào địa chỉ này, hệ thống phân giải subdomain thành định danh tenant, rồi tải đúng bộ nhận diện thương hiệu của trung tâm đó — logo, tông màu, tên hiển thị. Cùng một mã nguồn, một hạ tầng, nhưng mỗi trung tâm thấy một thương hiệu hoàn toàn riêng. Đây là nền tảng cho mọi thứ tiếp theo em sắp trình bày."

**Hình ảnh nhấn:** logo + palette + tên trung tâm trên hero.

**Dự phòng:** nếu landing tải chậm > 3 giây → quay về deck slide kiến trúc tổng thể (C4) làm cầu nối, mô tả phân giải tenant bằng sơ đồ, rồi thử lại.

---

## Đoạn 2 — AI Branding: gói Miễn phí vs Trả phí (0:30 → 2:15)

**Mục tiêu:** chứng minh đóng góp 2 — tự động hóa nhận diện thương hiệu. So sánh trực quan 2 tenant: một dùng mẫu dựng sẵn, một sinh tự động qua AI.

**Thao tác:**
- Đặt 2 cửa sổ cạnh nhau (hoặc 2 tab chuyển nhanh): landing Tenant A (Miễn phí, mẫu) và landing Tenant B (Trả phí, AI — chủ đề Hóa học).
- Chỉ vào sự khác biệt: bố cục mẫu giống nhau (gói Miễn phí) vs bộ nhận diện riêng biệt theo chủ đề môn học (gói Trả phí).
- (Tùy chọn, nếu muốn trình bày pipeline) mở wizard onboarding bước AI Branding của một tenant mới: nhập tên thương hiệu + chọn tông + màu chủ đạo → hiển thị 3 tài nguyên đã sinh sẵn (logo + hero + banner).

**Văn nói (~105 giây):**

> "Yếu tố khác biệt thứ hai của đề tài là tự động hóa nhận diện thương hiệu. Bên trái là một trung tâm gói Miễn phí — hệ thống áp một bộ mẫu dựng sẵn, đủ chỉn chu để dùng ngay mà không tốn chi phí sinh ảnh.
>
> Bên phải là một trung tâm gói Trả phí. Toàn bộ bộ nhận diện này — logo, ảnh bìa, banner — được sinh tự động từ một mô tả ngắn của chủ trung tâm: tên thương hiệu, tông phong cách, và chủ đề. Vì đây là trung tâm Hóa học, hệ thống tạo ra bộ nhận diện với tông màu và hình ảnh gợi đúng chủ đề, khác hẳn trung tâm bên trái.
>
> Về kỹ thuật, quy trình ưu tiên mẫu trước, chỉ gọi AI khi thực sự cần, để kiểm soát chi phí. Mỗi ảnh sinh ra phải qua hai cổng kiểm soát bắt buộc trước khi hiển thị: đạt tiêu chuẩn tương phản WCAG AA, và qua bộ phân loại an toàn nội dung. Nhờ vậy chủ trung tâm không cần biết thiết kế vẫn có một thương hiệu số riêng chỉ trong vài phút — đây là rào cản mà các hệ thống tham khảo trên thị trường hiện chưa giải quyết ở phân khúc giá thấp."

**Hình ảnh nhấn:** 2 landing cạnh nhau (tương phản mẫu vs AI) + 3 tài nguyên trong wizard nếu trình bày pipeline.

**Dự phòng:** nếu wizard lỗi giữa chừng → bỏ phần wizard, chỉ so sánh 2 landing đã có (đủ chứng minh kết quả); hoặc chuyển deck slide AI Branding mô tả pipeline lý thuyết.

---

## Đoạn 3 — Cô lập đa-tenant: bằng chứng mạnh nhất (2:15 → 4:30)

**Mục tiêu:** chứng minh đóng góp 1 — RLS ép buộc cô lập ở tầng database, không phụ thuộc lập trình viên nhớ điều kiện lọc. Đây là phần trọng tâm của demo.

### 3.1 — Dữ liệu Tenant A (2:15 → 2:55)

**Thao tác:**
- Tab 2 (đã đăng nhập Tenant A — Sao Mai).
- Vào mục Lớp học → mở lớp → thấy danh sách 5 học viên (tên giả định).

**Văn nói (~40 giây):**

> "Em chuyển sang chứng minh cô lập dữ liệu — đóng góp kỹ thuật chính của đề tài. Đây là tài khoản của trung tâm thứ nhất. Trung tâm này có một lớp với năm học viên. Em đã chuẩn bị sẵn dữ liệu này trước buổi bảo vệ. Xin lưu ý danh sách học viên ở đây để lát nữa so sánh."

### 3.2 — Dữ liệu Tenant B hoàn toàn khác (2:55 → 3:35)

**Thao tác:**
- Chuyển Tab 3 (đã đăng nhập Tenant B — Minh Trí).
- Vào mục Lớp học → thấy lớp KHÁC với 3 học viên KHÁC. Không thấy bất kỳ học viên nào của Tenant A.

**Văn nói (~40 giây):**

> "Đây là tài khoản trung tâm thứ hai, hoàn toàn độc lập. Nó có lớp khác, học viên khác. Quan trọng nhất: không hề thấy một học viên nào của trung tâm thứ nhất. Hai trung tâm này dùng chung một cơ sở dữ liệu PostgreSQL, chung một bảng — nhưng dữ liệu được cô lập tuyệt đối. Cô lập này không phải do tầng ứng dụng tự lọc, mà do chính database engine ép buộc, qua cơ chế Row-Level Security."

### 3.3 — Probe chéo tenant (3:35 → 4:30)

**Thao tác:**
- Mở DevTools → tab Network.
- Đang ở phiên Tenant B, gửi một yêu cầu thủ công: `GET /api/v1/students/{UUID-học-viên-của-Tenant-A}` với JWT của Tenant B.
- Kết quả: `403 Forbidden` (hoặc `404 Not Found` tùy endpoint) — không trả dữ liệu.

**Văn nói (~55 giây):**

> "Em làm một thử nghiệm tấn công có chủ đích để chứng minh. Giả sử kẻ tấn công đã đăng nhập hợp lệ vào trung tâm thứ hai, và bằng cách nào đó biết được mã định danh của một học viên thuộc trung tâm thứ nhất. Em dùng đúng phiên đăng nhập của trung tâm thứ hai, gửi yêu cầu lấy học viên của trung tâm thứ nhất.
>
> Hệ thống trả về 403 — từ chối. Điểm mấu chốt: dù người gọi đã xác thực hợp lệ và biết chính xác mã định danh, database vẫn không trả dữ liệu, vì policy RLS gắn mỗi truy vấn với định danh tenant của phiên hiện tại. Đây là điều mà filter ở tầng ứng dụng không đảm bảo được — chỉ cần một lập trình viên quên một điều kiện WHERE là rò dữ liệu. Ở đây, ngay cả khi tầng ứng dụng có lỗi, phòng tuyến cuối ở database vẫn chặn. Đề tài đánh giá sáu mô hình đa-tenant và chọn mô hình này vì lý do đó."

**Hình ảnh nhấn:** request URL + JWT tenant B + response 403 trong Network tab.

**Dự phòng:** nếu thao tác DevTools khó thực hiện trực tiếp trên màn chiếu → chuyển deck slide Defense-in-depth (5 lớp) + slide RLS, mô tả cơ chế bằng sơ đồ; hoặc dùng probe đã thu sẵn trong bản ghi dự phòng.

---

## Đoạn 4 — Audit log bất biến + chốt (4:30 → 5:00)

**Mục tiêu:** chứng minh tuân thủ PDPL Điều 11 (nhật ký bất biến) ở tầng database — nếu còn thời gian.

**Thao tác (nhanh):**
- Vào dashboard quản trị → mục nhật ký audit → chỉ 1 dòng (vd thao tác duyệt tenant).
- (Nếu thuận tiện) mở một cửa sổ psql nhỏ, chạy `UPDATE admin_audit_logs SET ... WHERE id=...` → Postgres trả lỗi `cannot UPDATE — immutable by trigger`.

**Văn nói (~30 giây):**

> "Cuối cùng, mọi thao tác quản trị đều ghi vào một bảng nhật ký bất biến. Trigger ở tầng database chặn cả lệnh sửa và xóa — ngay cả tài khoản quản trị cũng không thể thay đổi lịch sử. Đây là cách em hiện thực hóa yêu cầu nhật ký không thể chối bỏ của Luật Bảo vệ dữ liệu cá nhân. Tóm lại, hệ thống vừa chứng minh là sản phẩm chạy thật trên hạ tầng production, với hai đóng góp chính: cô lập đa-tenant ở tầng database, và tự động hóa nhận diện thương hiệu. Em xin dừng demo tại đây và sẵn sàng nhận câu hỏi của hội đồng."

Chuyển deck sang slide kết luận / Q&A.

**Nếu hết giờ (cắt đoạn 4):** thay bằng 1 câu — *"Ngoài ra, mọi thao tác quản trị ghi vào nhật ký bất biến ở tầng database theo yêu cầu PDPL Điều 11; em xin trình bày thêm trong phần hỏi đáp nếu hội đồng quan tâm. Em xin dừng demo và sẵn sàng nhận câu hỏi."*

---

## Bản ghi dự phòng (dự phòng bắt buộc)

**Mục đích:** nếu demo trực tiếp trục trặc (mạng, region AWS, lỗi bất ngờ), phát bản ghi thay thế.

- **Quay:** OBS Studio, thu cửa sổ trình duyệt 1920×1080 + thuyết minh theo văn nói trên. Quay 1 lần tại **T-3 ngày**, đúng kịch bản 5 phút này.
- **Tệp xuất:** `backup-demo.mp4` (lưu local + Google Drive; KHÔNG commit file lớn vào repo).
- **Khi nào chuyển sang bản dự phòng:** mạng đứt > 30 giây / endpoint trả 5xx ≥ 2 lần / mất > 1 phút debug trực tiếp.
- **Cách chuyển (bình tĩnh):** *"Em xin chuyển sang bản ghi demo vì lý do kỹ thuật — bản ghi mô tả chính xác các bước vừa rồi."* Mở `backup-demo.mp4` toàn màn hình, phát từ đoạn đang dở.

---

## Danh sách kiểm tra T-1 ngày trước bảo vệ

- [ ] `bash scripts/aws/start-stack.sh` — stack GREEN trên CloudWatch.
- [ ] `curl -sI https://kitehub.me/` → 200; đăng nhập thử 2 tenant OK.
- [ ] 2 tenant demo seed xong (tên giả định) — A mẫu, B AI sinh sẵn.
- [ ] 3 tab tải sẵn (landing A + dashboard A + dashboard B), đã đăng nhập sẵn.
- [ ] Bản ghi dự phòng thử phát OK trên laptop sẽ dùng.
- [ ] Adapter máy chiếu thử với laptop.
- [ ] Mạng dự phòng 4G thử kết nối OK.
- [ ] Deck mở sẵn slide 20.
- [ ] Tài liệu Q&A (`defense-qa-response-sheet.md`) in 1 bản giấy.

---

## Quan hệ với các tài liệu khác

- **`defense-speaker-script-20slide.md`** — văn nói 22 slide; slide 20 là cầu nối sang demo này.
- **`defense-demo-script.md`** — bản demo đầy đủ 15 phút (6 phase); dùng làm bản mở rộng nếu hội đồng muốn xem sâu, hoặc nguồn cho bản ghi dự phòng dài.
- **`multi-tenant-demo-script.md`** — kịch bản chi tiết chứng minh cô lập RLS (mở rộng đoạn 3).
- **`defense-qa-response-sheet.md`** — câu hỏi phản biện dự kiến.

---

## Log

- **2026-06-17:** Tạo bản 5 phút (đóng góp 1 RLS + đóng góp 2 AI Branding + anchor landing + audit log) cho demo bảo vệ trên AWS production thật. Cô đọng từ bản 15 phút `defense-demo-script.md`. Tên dữ liệu mẫu dùng hậu tố "(giả định)"; chỉ 2 khái niệm thời gian "hiện tại / lộ trình phát triển sau"; không tham chiếu số slide tuyệt đối của deck cũ.
