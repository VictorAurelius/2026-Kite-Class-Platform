---
status: active
audience: tenant
last-updated: 2026-05-26
version: v0.9.0-beta (Phase 1 BETA)
wave: beta-prep-1-bucket-A
gaps: [GAP-PDPL-COMPLIANCE-MIN]
---

# Thông báo bảo mật dữ liệu cá nhân (Privacy Notice)

> ⚠️ **Disclaimer — Phase 1 BETA v1 pending counsel review:** Tài liệu này được Nguyễn Văn Kiệt (KiteHub solo founder) soạn thảo bản đầu để đáp ứng yêu cầu pháp lý tối thiểu trước hạn PDPL 2026-07-01. **Bản chính thức sẽ được luật sư chuyên ngành dữ liệu cá nhân rà soát trước Phase 2.** Nếu bạn phát hiện điểm chưa phù hợp, vui lòng email [support@kitehub.me](mailto:support@kitehub.me).

**Cập nhật lần cuối:** Thứ Ba, 26/05/2026 · Phiên bản KiteHub: v0.9.0-beta · Đọc khoảng **6 phút**

---

## TL;DR

Trang này giúp bạn hiểu KiteHub thu thập + xử lý dữ liệu cá nhân thế nào:

- ✅ Chúng tôi thu thập email, tên, số điện thoại, dữ liệu sử dụng (login, hành vi trên ứng dụng) — phục vụ vận hành nền tảng và cung cấp dịch vụ giáo dục
- ✅ Dữ liệu lưu trữ tại Singapore (AWS `ap-southeast-1`) — tuân thủ Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP
- ✅ Bạn có quyền truy cập, chỉnh sửa, xóa, rút lại đồng ý — liên hệ [support@kitehub.me](mailto:support@kitehub.me)
- ✅ Chúng tôi KHÔNG bán dữ liệu cá nhân cho bên thứ ba
- ⚠️ Phiên bản này là v1 pending counsel review — bản chính thức sẽ ship Phase 2

---

## 1. Phạm vi áp dụng

Thông báo này áp dụng cho mọi cá nhân sử dụng dịch vụ KiteHub (`https://kitehub.me`), bao gồm:

- **Chủ trung tâm** (Center Owner) — ví dụ chị Trần Thị Hồng quản lý `Trung tâm Anh ngữ Sky Education`
- **Quản lý trung tâm** (Center Manager)
- **Giáo viên** sử dụng KiteHub độc lập (Solo Teacher)
- **Khách tham khảo** (Anonymous Prospect) xem trang công khai `kitehub.me`

KiteHub hoạt động trên nền tảng đa khách (multi-tenant). Dữ liệu của học sinh + phụ huynh thuộc quyền sở hữu và xử lý của trung tâm (tenant) — KiteHub đóng vai trò Bên xử lý dữ liệu (Data Processor) theo Nghị định 13/2023/NĐ-CP Điều 3. Tenant đóng vai trò Bên kiểm soát dữ liệu (Data Controller).

## 2. Danh mục dữ liệu cá nhân chúng tôi thu thập

### 2.1 Dữ liệu cơ bản (Personal Identifiable Information — PII)

| Loại dữ liệu | Mục đích | Cơ sở pháp lý |
|---|---|---|
| Họ tên đầy đủ (ví dụ `Trần Thị Hồng`) | Định danh tài khoản, hiển thị UI, gửi email cá nhân hóa | Đồng ý + hợp đồng (PDPL Art 11) |
| Email (ví dụ `hong.tran@skyedu.vn`) | Đăng nhập, gửi hóa đơn, thông báo bảo mật | Đồng ý + hợp đồng |
| Số điện thoại (ví dụ `0901 234 567`) | Liên hệ khẩn cấp, OTP nếu kích hoạt Phase 2+ | Đồng ý (tùy chọn) |
| Mật khẩu (đã băm bcrypt) | Xác thực đăng nhập | Hợp đồng |
| Mã số thuế tổ chức (ví dụ `0312345678`) | Xuất hóa đơn điện tử theo TT 78/2021/TT-BTC | Nghĩa vụ pháp lý |

### 2.2 Dữ liệu sử dụng (Usage data)

- Lịch sử đăng nhập (IP, User-Agent, timestamp) — lưu trong `login_audit_log` cho mục đích bảo mật
- Hành vi sử dụng ứng dụng (route, click, thời lượng) — chỉ thu thập nếu bạn đồng ý chia sẻ dữ liệu phân tích
- Cookies — xem chính sách cookie tại `documents/01-business/cookie-consent/`

### 2.3 Dữ liệu nhạy cảm (Sensitive personal data — PDPL Art 7)

KiteHub Phase 1 BETA **KHÔNG** thu thập dữ liệu nhạy cảm theo định nghĩa PDPL Điều 7 (dữ liệu y tế, sinh trắc học, tài khoản ngân hàng, dân tộc, tôn giáo, định hướng giới tính, v.v.).

Nếu bạn upload tài liệu chứa dữ liệu nhạy cảm (ví dụ giấy khám sức khỏe học sinh), trung tâm (tenant) chịu trách nhiệm Bên kiểm soát dữ liệu — KiteHub khuyến cáo KHÔNG upload dữ liệu nhạy cảm trong Phase 1 BETA.

## 3. Mục đích xử lý dữ liệu

Chúng tôi xử lý dữ liệu cá nhân của bạn cho các mục đích:

1. **Cung cấp dịch vụ giáo dục:** quản lý lớp học, điểm danh, học phí, báo cáo
2. **Giao tiếp với bạn:** gửi email xác nhận, hóa đơn, thông báo bảo mật
3. **Cải thiện sản phẩm:** phân tích sử dụng (chỉ nếu bạn đồng ý)
4. **Tuân thủ pháp luật:** lưu trữ hồ sơ kế toán + thuế theo Luật Kế toán 2015 + TT 78/2021

## 4. Quyền của bạn theo PDPL Art 11

Bạn có các quyền sau đối với dữ liệu cá nhân của mình:

| Quyền | Cách thực hiện |
|---|---|
| **Quyền truy cập** | Email [support@kitehub.me](mailto:support@kitehub.me) yêu cầu bản sao dữ liệu — phản hồi trong 30 ngày |
| **Quyền chỉnh sửa** | Chỉnh trực tiếp trong ứng dụng (Cài đặt → Hồ sơ) hoặc email yêu cầu |
| **Quyền xóa (Right to be forgotten)** | Email yêu cầu xóa tài khoản — chúng tôi sẽ xóa trong 30 ngày, trừ trường hợp luật yêu cầu giữ (kế toán 10 năm, audit log 5 năm) |
| **Quyền hạn chế xử lý** | Email yêu cầu — chúng tôi sẽ tạm dừng xử lý non-essential |
| **Quyền phản đối xử lý** | Áp dụng cho marketing email — bạn có thể từ chối tại signup hoặc unsubscribe link trong email |
| **Quyền chuyển dữ liệu (data portability)** | Email yêu cầu — chúng tôi cung cấp file CSV trong 30 ngày |
| **Quyền rút lại đồng ý** | Email yêu cầu — không ảnh hưởng tới đồng ý trước đó |

## 5. Thời gian lưu trữ dữ liệu

Theo chính sách lưu trữ chi tiết tại [`data-retention-policy.md`](data-retention-policy.md):

| Loại dữ liệu | Thời gian lưu | Cơ sở |
|---|---|---|
| Tài khoản người dùng (sau deactivation) | 7 năm | Luật Kế toán 2015 |
| Audit log (login, admin action) | 5 năm | PDPL Art 11 + ND-53/2022 |
| Hồ sơ kế toán + hóa đơn | 10 năm | Luật Kế toán 2015 Art 41 |
| Đồng ý marketing | 1 năm sau hết hạn | PDPL Art 11 (định kỳ tái xác nhận) |
| Cookie consent | 12 tháng | Cookiebot/Osano industry standard |

## 6. Chia sẻ dữ liệu với bên thứ ba

KiteHub Phase 1 BETA chỉ chia sẻ dữ liệu cá nhân của bạn trong các trường hợp:

- **Nhà cung cấp dịch vụ:** AWS (Singapore region) cho hosting + lưu trữ; Resend cho gửi email transactional
- **Yêu cầu pháp lý:** cơ quan nhà nước có thẩm quyền yêu cầu bằng văn bản
- **Bảo vệ quyền lợi:** chống gian lận, lạm dụng dịch vụ
- **Tenant của bạn:** dữ liệu học sinh chia sẻ với trung tâm tenant (Data Controller chính)

KiteHub **KHÔNG** bán dữ liệu cá nhân cho bên thứ ba với mục đích marketing.

## 7. Bảo mật dữ liệu

KiteHub áp dụng các biện pháp bảo mật:

- **Mã hóa:** dữ liệu truyền (HTTPS/TLS 1.2+) và lưu trữ (AES-256 at-rest AWS RDS)
- **Kiểm soát truy cập:** least-privilege IAM role, JWT short-lived (15 phút access token), refresh token rotation
- **Audit log bất biến:** mọi hành động admin lưu trong `admin_audit_log` với SHA-256 hash chain (PDPL Art 11 tamper-proof)
- **Bảo mật mật khẩu:** băm bcrypt 12 rounds, không lưu plaintext
- **Khóa tài khoản:** 5 lần đăng nhập sai trong 15 phút → khóa 15 phút (chống credential stuffing)

## 8. Sự cố an ninh dữ liệu (Data breach notification)

Trong trường hợp sự cố an ninh dữ liệu cá nhân, KiteHub cam kết:

- **Thông báo cơ quan nhà nước** (Bộ Công an A05) trong **72 giờ** theo PDPL Art 13
- **Thông báo bạn** qua email + Zalo OA (nếu kích hoạt) trong **72 giờ** nếu sự cố có khả năng ảnh hưởng tới quyền lợi của bạn
- **Khắc phục sự cố** theo runbook [`breach-notification-sop.md`](../../05-guides/operations/breach-notification-sop.md)

## 9. Chính sách trẻ em (Children's privacy)

KiteHub Phase 1 BETA dành cho học viên ≥ 13 tuổi (theo PDPL Art 21). Nếu trung tâm thu thập dữ liệu của trẻ em < 13 tuổi, trung tâm (tenant) PHẢI có sự đồng ý của cha mẹ/người giám hộ — KiteHub đóng vai trò Bên xử lý dữ liệu.

Phase 3 sẽ hỗ trợ rõ hơn cho phân khúc K-12 (trẻ em) với DPIA + sự đồng ý phụ huynh chính thức.

## 10. Liên hệ

- 📧 Email DPO (Data Protection Officer): [support@kitehub.me](mailto:support@kitehub.me) (Phase 1 BETA — Nguyễn Văn Kiệt acting; Phase 2 sẽ chỉ định DPO chính thức)
- 🏢 Địa chỉ pháp lý: Phase 1 BETA chưa đăng ký pháp nhân, sẽ cập nhật khi đăng ký
- 📊 Trạng thái dịch vụ: `https://kitehub.me/beta-status` (sắp ra mắt)

## 11. Thay đổi thông báo này

KiteHub có thể cập nhật Thông báo bảo mật này. Phiên bản mới sẽ thông báo qua email + banner trong ứng dụng ít nhất **7 ngày** trước khi có hiệu lực. Phiên bản cũ lưu trong git history `documents/01-business/legal/privacy-notice.md`.

**Lịch sử phiên bản:**

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| v0.9.0-beta | 26/05/2026 | Phiên bản v1 pending counsel review (Wave beta-prep-1 Bucket A) |

## 12. Luật áp dụng

Thông báo này được điều chỉnh theo pháp luật Việt Nam:

- **Luật An toàn thông tin mạng 2015**
- **Luật An ninh mạng 2018** + **Nghị định 53/2022/NĐ-CP** (data localization)
- **Luật Bảo vệ Dữ liệu Cá nhân 2023 (PDPL)** + **Nghị định 13/2023/NĐ-CP** (Decree 13)
- **Luật Kế toán 2015** (lưu trữ hồ sơ tài chính)
- **Thông tư 78/2021/TT-BTC** (hóa đơn điện tử)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: sắp kích hoạt Phase 1.5+
- 🐛 Báo lỗi tài liệu này: [support@kitehub.me?subject=Lỗi tại /privacy](mailto:support@kitehub.me?subject=Lỗi%20tại%20/privacy)
- 📊 Trạng thái beta: `https://kitehub.me/beta-status`

**Phiên bản:** v0.9.0-beta v1 pending counsel review · **Cập nhật:** Thứ Ba, 26/05/2026 · **Wave:** beta-prep-1 Bucket A
