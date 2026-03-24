# Báo cáo: Chống Spam Đăng Ký KiteHub

**Ngày:** 2026-03-19
**Người viết:** Development Team
**Mục đích:** Xin phê duyệt giải pháp chống spam trước khi launch
**Cần quyết định:** 1 câu hỏi

---

## 1. Vấn đề

Hiện tại, bất kỳ ai cũng có thể đăng ký tài khoản KiteHub với email bất kỳ (không cần xác thực). Mỗi lần đăng ký, hệ thống **tự động tạo 1 database PostgreSQL riêng** cho trung tâm đó.

### Luồng đăng ký hiện tại

```
Người dùng bấm "Đăng ký"
    → Tạo tài khoản (User)
    → Tạo trung tâm (Instance, status: TRIAL 14 ngày)
    → Tạo database PostgreSQL riêng (~50MB)
    → Chạy Flyway migrations (22 tables)
    → Hoàn tất
```

**Toàn bộ xảy ra trong 1 lần bấm nút.** Không có bước xác nhận email.

### Hậu quả nếu bị spam

| Kịch bản | Hậu quả |
|-----------|---------|
| Bot tạo 100 accounts trong 1 giờ | 100 databases × 50MB = **5GB disk bị chiếm** |
| Bot tạo 1,000 accounts | 50GB disk + connection pool cạn → **toàn bộ platform down** |
| Đối thủ cạnh tranh spam | Tốn resource, ảnh hưởng performance cho khách thật |

### Đã làm gì?

Đã triển khai 2 biện pháp (PR #150):
- **Rate limit:** Tối đa 3 lần đăng ký/giờ/IP (qua Redis)
- **Giới hạn instance:** Tối đa 2 trung tâm miễn phí/tài khoản

**Nhưng chưa đủ:** Bot có thể dùng nhiều IP (proxy/VPN) và nhiều email fake để bypass cả 2 biện pháp trên.

---

## 2. Giải pháp đề xuất

### Thay đổi luồng đăng ký:

```
HIỆN TẠI (không an toàn):
    Đăng ký → Tạo DB ngay → Xong

ĐỀ XUẤT (an toàn):
    Đăng ký → Gửi email xác nhận → User bấm link
                                         ↓
                                   Tạo DB lúc này → Xong
```

### Chi tiết kỹ thuật

1. User đăng ký → hệ thống tạo tài khoản (status: **CHƯA XÁC NHẬN**) + trung tâm (status: **CHỜ**)
2. Gửi email chứa link xác nhận (hết hạn sau 24 giờ)
3. User bấm link → hệ thống xác nhận email → **lúc này mới tạo database**
4. Trung tâm chuyển sang status **TRIAL 14 ngày**
5. Nếu không xác nhận trong 48 giờ → tự động xóa tài khoản + trung tâm

### Tại sao cách này hiệu quả?

| Biện pháp | Chặn bot? | Chặn email fake? | Tốn resource? |
|-----------|-----------|-------------------|---------------|
| Rate limit (đã có) | Một phần | Không | Vẫn tạo DB |
| Instance limit (đã có) | Một phần | Không | Vẫn tạo DB |
| **Email verification (đề xuất)** | **Có** | **Có** | **Không tạo DB cho email fake** |

**Kết hợp cả 3** → chặn ~99% spam:
- Rate limit chặn brute force
- Instance limit chặn abuse per account
- Email verify chặn email fake + không tốn DB resource

---

## 3. Tại sao không yêu cầu thẻ tín dụng?

Đã cân nhắc nhưng **không phù hợp** vì:

- **Tệp khách hàng:** Chủ trung tâm giáo dục VN, 28-50 tuổi, ít dùng thẻ credit
- **Payment gateway VN** (VietQR, Momo, ZaloPay) không hỗ trợ verify thẻ như Stripe
- Người dùng VN chủ yếu chuyển khoản ngân hàng
- Yêu cầu thẻ sẽ **giảm đáng kể tỷ lệ đăng ký** (conversion rate)

---

## 4. Ảnh hưởng đến trải nghiệm người dùng

### Trước (hiện tại)
```
Bấm "Đăng ký" → Vào dashboard ngay (3 giây)
```

### Sau (đề xuất)
```
Bấm "Đăng ký" → Trang "Kiểm tra email" → Bấm link trong email → Vào dashboard (30-60 giây)
```

**Đánh giá:**
- Thêm 1 bước nhưng **tiêu chuẩn ngành** (mọi SaaS đều làm)
- Người dùng quen với flow này (đã dùng ở Facebook, Zalo, Gmail...)
- **Lợi ích bảo mật >> bất tiện nhỏ**
- Có nút "Gửi lại email" nếu không nhận được

---

## 5. Effort ước tính

| Hạng mục | Thời gian |
|----------|-----------|
| Backend: email verification API + defer DB | 1 ngày |
| Frontend: trang "Kiểm tra email" + trang verify | 0.5 ngày |
| Kết nối email service (đã có kitehub-email) | 0.5 ngày |
| **Tổng** | **~2 ngày** |

---

## 6. Quyết định cần phê duyệt

> **Có đồng ý chuyển sang luồng "Đăng ký → Xác nhận email → Mới tạo database" không?**

| Lựa chọn | Hệ quả |
|-----------|--------|
| **Đồng ý** | Implement trong ~2 ngày. Chặn spam hiệu quả. Thêm 1 bước cho user. |
| **Không đồng ý** | Giữ nguyên. Chỉ dựa vào rate limit + instance limit. Rủi ro bị spam tạo DB. |

---

## 7. Tham khảo

- Luồng đăng ký hiện tại: `kitehub-subscription/service/AuthService.java`
- Rate limit đã triển khai: PR #150
- Plan đầy đủ: `documents/03-planning/kitehub-onboarding-security-plan.md`
