---
persona: anonymous
topic: faq
last-updated: 2026-05-26
version: v0.9.0-beta
effort_minutes: 7
---

# Câu hỏi thường gặp — Anonymous Prospect

> 📅 Cập nhật lần cuối: **2026-05-26** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **7 phút**

## TL;DR

Tổng hợp 15 câu hỏi thường gặp nhất khi chưa đăng ký KiteHub, chia 4 nhóm:

- 🎯 **Sản phẩm (5 câu)** — KiteHub là gì, ai dùng, khác Misa thế nào
- 💰 **Giá + Beta (4 câu)** — chi phí, hoàn tiền, hết Beta thế nào
- 🔐 **Bảo mật (3 câu)** — dữ liệu lưu ở đâu, ai truy cập, có an toàn không
- 🛠️ **Kỹ thuật (3 câu)** — cấu hình tối thiểu, mobile, migration

---

## 1. Câu hỏi về sản phẩm

### 1.1 KiteHub khác Misa thế nào?

| Tiêu chí | KiteHub | Misa |
|---|---|---|
| Đối tượng chính | Trung tâm giáo dục ngoài chính khoá (Anh ngữ, Toán, Năng khiếu) | Trường K-12 chính khoá + nhiều ngành khác |
| Trải nghiệm | Web SaaS, không cần cài đặt | Desktop + Cloud |
| Giá khởi đầu | 990.000đ/tháng (PRO) | Cao hơn, theo licence |
| Vùng tập trung | Việt Nam, ưu tiên trung tâm SMB | Toàn quốc, đa ngành |
| Tích hợp Zalo | ✅ tự động gửi tin nhắn phụ huynh | ⚠️ tuỳ module |
| App mobile cho GV | ✅ điểm danh + nhắn PH trong app | ⚠️ tuỳ module |
| Khả năng tuỳ chỉnh thương hiệu | ✅ logo, màu, tên miền riêng (gói PREMIUM+) | Hạn chế |

### 1.2 KiteHub có hỗ trợ trường công lập (K-12) không?

Tạm thời **chưa hỗ trợ K-12 chính khoá** trong Phase 1 BETA. Lý do:

- Cần tham vấn pháp lý PDPL 2023 + Luật Giáo dục 2019
- Cần tích hợp với hệ thống quản lý điểm + học bạ của Bộ GD&ĐT (đang nghiên cứu)
- Cần đáp ứng yêu cầu MPS A05 về bảo mật trường học

Dự kiến hỗ trợ K-12 trong **Phase 3** (sau khi có counsel reviewed + DPO + DPIA).

### 1.3 KiteHub có làm trung tâm STEM / Năng khiếu không?

Có, đầy đủ. Module Lớp + Lịch + Học phí áp dụng tốt cho:

- ✅ Trung tâm Anh ngữ
- ✅ Trung tâm Toán-Lý-Hoá
- ✅ Trung tâm STEM (Robot, Coding, Khoa học)
- ✅ Trung tâm Năng khiếu (Nhạc, Vẽ, Múa, Võ)
- ✅ Mầm non tư thục (nếu <300 hs)

### 1.4 KiteHub có hỗ trợ trung tâm có nhiều chi nhánh không?

**Phase 1 BETA (đến hết tháng 11/2026): chưa hỗ trợ đa chi nhánh.**

Hiện tại KiteHub thiết kế cho trung tâm 1 chi nhánh để hoàn thiện quy trình cốt lõi (lớp + lịch + học phí + điểm danh) cùng nhóm trung tâm tiên phong. Tính năng đa chi nhánh sẽ ship **Quý 3 năm 2026** trong Phase 2, bao gồm:

- Dashboard tổng hợp doanh thu + sĩ số toàn hệ thống cho Chủ trung tâm
- Chuyển học sinh / giáo viên giữa các chi nhánh
- Phân quyền Quản lý chi nhánh riêng biệt
- Báo cáo cross-branch (so sánh hiệu quả từng chi nhánh)

Nếu trung tâm bạn có **≥ 2 chi nhánh**, vui lòng **đăng ký waitlist** ngay khi điền form Beta — chúng tôi sẽ thông báo qua email + Zalo khi đa chi nhánh sẵn sàng. Trong thời gian chờ, anh/chị có thể:

- Onboard chi nhánh chính trước, các chi nhánh khác chờ Q3 2026, hoặc
- Tham khảo giải pháp tạm thời: tạo 1 tenant KiteHub cho mỗi chi nhánh + reconcile báo cáo qua Excel ngoài hệ thống (không khuyến nghị dài hạn)

Quyết định defer đến Phase 2 được ghi nhận tại [ADR-036](../../../../02-architecture/adr/ADR-036-multi-branch-defer-phase-2.md).

### 1.5 Có App mobile cho phụ huynh không?

- **Phase 1 BETA:** chưa có app mobile cho phụ huynh; phụ huynh nhận tin nhắn qua Zalo / SMS + truy cập web portal
- **Phase 1.5 PAID:** đang phát triển app mobile (iOS + Android)

---

## 2. Câu hỏi về giá + Beta

### 2.1 Beta miễn phí 6 tháng là thật không?

Có. Trong Phase 1 BETA (đến hết tháng 11/2026), 5-20 trung tâm tiên phong được dùng **gói PRO miễn phí 6 tháng đầu**, không yêu cầu thẻ tín dụng.

Sau 6 tháng, bạn có thể:

- Tiếp tục PRO với **giảm 30% năm đầu** (~7 triệu đồng/năm)
- Hoặc downgrade về FREE (giữ tối đa 30 học sinh)
- Hoặc huỷ tài khoản (xuất dữ liệu trong 60 ngày)

### 2.2 Nếu tôi không hài lòng có hoàn tiền không?

- **Gói FREE:** miễn phí nên không có chuyện hoàn tiền
- **Gói trả phí (PRO/PREMIUM):** hoàn tiền theo tỷ lệ thời gian chưa dùng, trừ 10% phí xử lý
- **Beta:** không phát sinh chi phí, huỷ trong 30 ngày đầu xuất dữ liệu miễn phí

### 2.3 Beta kết thúc, tôi có bị tăng giá đột ngột không?

Không. KiteHub cam kết:

- 📧 Thông báo trước **30 ngày** trước khi kết thúc Beta
- 💸 Beta tester được **giảm 30% năm đầu** sau Beta
- 🔄 Sẵn sàng downgrade về FREE hoặc xuất dữ liệu nếu không tiếp tục

### 2.4 Tôi có thể bỏ qua Beta, mua ngay PRO không?

Có. Sau khi sản phẩm chính thức (v1.0.0, dự kiến tháng 12/2026):

- Đăng ký trực tiếp gói PRO với giá 990.000đ/tháng
- Hoặc gói FREE (≤30 học sinh) vĩnh viễn

Tuy nhiên, **tham gia Beta vẫn lợi hơn** (miễn phí 6 tháng + giảm 30% năm đầu).

---

## 3. Câu hỏi về bảo mật

### 3.1 Dữ liệu của tôi lưu ở đâu?

- 🌏 **AWS Singapore (ap-southeast-1)** — đáp ứng Luật An ninh mạng 2018 Decree 53/2022 về data localization Đông Nam Á
- 🔐 **Mã hoá TLS 1.3** khi truyền (HTTPS)
- 🔒 **Mã hoá AES-256** khi lưu trữ (RDS encryption-at-rest)
- 💾 **Backup hàng ngày** tự động (gói PRO trở lên)

### 3.2 Ai trong đội KiteHub có thể xem dữ liệu của tôi?

- **Mặc định:** không ai. Dữ liệu mã hoá; chỉ Chủ trung tâm + các vai trò bạn cấp quyền mới truy cập được
- **Trường hợp hỗ trợ kỹ thuật:** khi bạn yêu cầu help, nhân viên KiteHub yêu cầu quyền tạm thời (24 giờ) qua "Permission grant" trong Dashboard — bạn chấp thuận trước khi họ xem
- **Audit log:** mọi truy cập của nhân viên KiteHub đều ghi log, bạn xem được tại Dashboard → Cài đặt → Audit log

### 3.3 Nếu tài khoản bị hack thì sao?

KiteHub có nhiều lớp bảo vệ:

- 🔐 **2FA bắt buộc** cho Chủ trung tâm + Manager (TOTP, SMS, email OTP)
- 🚨 **Phát hiện đăng nhập bất thường** — gửi cảnh báo email + Zalo
- ⏱️ **Auto-logout** sau 30 phút không hoạt động
- 🛡️ **Rate limit** ngăn brute-force password (Wave 78 GAP-508 đã ship)

Nếu nghi ngờ bị hack:

1. Đổi mật khẩu ngay (Dashboard → Hồ sơ → Đổi mật khẩu)
2. Xem Audit log để biết có session lạ không
3. Liên hệ [security@kitehub.me](mailto:security@kitehub.me) để được hỗ trợ điều tra

---

## 4. Câu hỏi kỹ thuật

### 4.1 Cấu hình tối thiểu để dùng KiteHub?

KiteHub là web SaaS, không cần cài đặt:

- 💻 **Máy tính:** trình duyệt Chrome/Edge/Firefox/Safari phiên bản 2 năm gần nhất
- 📱 **Điện thoại:** iPhone iOS 14+ hoặc Android 9+ (cho giáo viên điểm danh)
- 🌐 **Internet:** băng thông 3G+ đủ dùng; 4G/WiFi cho trải nghiệm tốt

### 4.2 Tôi đang dùng Misa / KiteOS / Smile, chuyển sang KiteHub có dễ không?

Có. KiteHub hỗ trợ import miễn phí từ:

- **Misa** — xuất Excel danh sách học sinh + lớp + GV
- **KiteOS** — xuất Excel theo template KiteHub cung cấp
- **Smile** — xuất Excel (cần cleanup nhẹ)
- **KidsPay** — xuất Excel
- **Excel tự làm** — KiteHub có template chuẩn

Quy trình:

1. Xuất Excel từ phần mềm cũ
2. Tải template KiteHub tại [/help/anonymous/migration-template](/help/anonymous/migration-template) (Wave 80+ — đang phát triển)
3. Map cột Excel → cột template
4. Upload vào KiteHub Dashboard
5. KiteHub kiểm tra + báo lỗi nếu có

Thời gian: trung bình **2-4 giờ** cho trung tâm 100-300 học sinh.

### 4.3 KiteHub có API để tích hợp với phần mềm khác không?

- **Phase 1 BETA:** chưa có public API
- **Phase 1.5 PAID:** dự kiến mở API cho gói PREMIUM+ (Q3 2026)
- **Phase 2:** API đầy đủ + webhooks cho ENTERPRISE

Trong khi chờ API, có thể:

- Xuất Excel/CSV thủ công
- Tích hợp Zapier qua email parser
- Yêu cầu KiteHub xuất báo cáo tự động qua email lịch

---

## 5. Câu hỏi tôi không thấy ở đây, hỏi ai?

Gửi email [support@kitehub.me](mailto:support@kitehub.me) với:

- Câu hỏi cụ thể
- Tên trung tâm + Họ tên người liên hệ
- Số điện thoại / Zalo nếu muốn được gọi lại

Đội KiteHub trả lời trong **24-48 giờ hành chính** (Thứ Hai - Thứ Sáu, 9:00 - 17:00).

---

## 6. Tiếp theo

- 💰 **Xem chi tiết bảng giá:** [Bảng giá KiteHub](pricing.md)
- 🎫 **Đăng ký Beta miễn phí 6 tháng:** [Yêu cầu truy cập Beta](beta-access.md)
- 📄 **Đọc Điều khoản dịch vụ:** [Điều khoản dịch vụ](terms.md)
- 🏠 **Quay về trang chủ tài liệu:** [Chào mừng đến KiteHub](index.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi trang /help/anonymous/faq](mailto:support@kitehub.me?subject=Lỗi%20trang%20%2Fhelp%2Fanonymous%2Ffaq)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
