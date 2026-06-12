# Báo cáo: Gaps trong luồng Đăng ký & Onboarding KiteHub

**Ngày:** 2026-03-19
**Người viết:** Development Team
**Mục đích:** Báo cáo các vấn đề phát hiện trong luồng end-to-end, đề xuất giải pháp để leader review
**Trạng thái:** Chờ duyệt

---

## 1. Tổng quan vấn đề

Sau khi hoàn thành E2E local (63/63 API tests pass), phát hiện **4 gaps quan trọng** trong luồng đăng ký → sử dụng KiteHub mà chưa được xử lý:

| # | Vấn đề | Mức độ | Ảnh hưởng |
|---|--------|--------|-----------|
| 1 | Instance khởi tạo ngay khi đăng ký, không có bước xác nhận | 🟠 Trung bình | Tốn resource, user chưa hiểu flow |
| 2 | Không có onboarding hướng dẫn sau đăng ký | 🔴 Cao | User (IT thấp-trung bình) bỏ cuộc vì không biết làm gì |
| 3 | URL tenant không hoạt động ở local dev | 🟠 Trung bình | Dev không test được full flow |
| 4 | Không có cơ chế chống spam đăng ký | 🔴 Cao | Bị abuse tạo hàng trăm instances + databases |

---

## 2. Chi tiết từng vấn đề

### 2.1. Instance khởi tạo ngay khi đăng ký

**Hiện tại:**
```
User bấm "Đăng ký" → Tạo User → Tạo Instance (TRIAL) → Provision Database → Done
```

Toàn bộ xảy ra trong 1 API call. Instance có status TRIAL, database PostgreSQL được tạo ngay lập tức.

**Vấn đề:**
- Database được provision trước khi xác minh email → email fake vẫn tạo được DB
- Mỗi database tốn ~50MB disk + 1 connection pool
- User chưa hiểu mình vừa được gì, nhưng resource đã bị chiếm

**Đề xuất:**
- **Option A** (khuyến nghị): Tách thành 2 bước: Register tạo User + Instance (status=PENDING) → Verify email → Provision DB (status=TRIAL)
- **Option B**: Giữ nguyên nhưng thêm email verification, nếu không verify trong 24h → xóa instance + DB

---

### 2.2. Không có onboarding sau đăng ký

**Hiện tại:** Sau đăng ký, user được redirect về Dashboard. Dashboard hiển thị instance card nhưng không có hướng dẫn gì.

**Tệp khách hàng KiteHub:** Chủ trung tâm giáo dục VN, 28-50 tuổi, IT thấp-trung bình (dùng Facebook/Zalo thành thạo, ít dùng phần mềm quản lý).

**Vấn đề:** User sẽ không hiểu:
- "Instance" nghĩa là gì?
- URL `abc-center.kitehub.me` để làm gì?
- Bước tiếp theo là gì?
- Tính năng AI Branding hoạt động thế nào?

**Đề xuất:** Thêm onboarding wizard (3-4 bước) hiển thị lần đầu vào dashboard:

```
Bước 1: "Chúc mừng! Trung tâm của bạn đã sẵn sàng"
         → Hiển thị tên trung tâm + URL

Bước 2: "Đây là trang quản lý của bạn"
         → Giới thiệu Dashboard, Billing, Settings

Bước 3: "Truy cập trang web trung tâm"
         → Button mở URL KiteClass instance

Bước 4: "Tạo thương hiệu với AI"
         → Giới thiệu AI Branding wizard
```

---

### 2.3. URL tenant không hoạt động ở local

**Hiện tại:**
- Instance detail page có button "Truy cập KiteClass" → link `https://{subdomain}.kitehub.me`
- **Local:** Link này KHÔNG hoạt động vì không có DNS resolve `*.kitehub.me` → localhost
- **Production:** Cần wildcard DNS `*.kitehub.me` → server IP

**Vấn đề cho dev:** Không thể test full flow Register → Dashboard → Click vào instance → Thấy KiteClass frontend.

**Giải pháp cho local:**

| Approach | Ưu điểm | Nhược điểm |
|----------|---------|------------|
| **A. `*.localhost`** | Chrome tự resolve → 127.0.0.1, không cần config | Firefox/Safari có thể không hỗ trợ |
| **B. Header-based** | Gateway đã hỗ trợ `X-Tenant-Id` header | Cần sửa FE link, khác flow production |
| **C. Path-based** | `localhost:3000/t/demo` | Cần sửa KiteClass FE routing |
| **D. Port-based** | `localhost:3000` + tenant selector | Đơn giản nhất cho local |

**Đề xuất:** Option D cho local (đơn giản, không cần DNS). Dashboard link ở local trỏ về `localhost:3000` kèm query param `?tenant={subdomain}`, gateway đọc param này để resolve tenant.

**Cho production:** Wildcard DNS `*.kitehub.me` → Cloudflare → server. Gateway resolve subdomain như hiện tại.

---

### 2.4. Không có cơ chế chống spam đăng ký

**Hiện tại:** Ai cũng có thể đăng ký với email bất kỳ → tạo instance → database được provision. Không có kiểm tra nào.

**Hậu quả tiềm tàng:**
- Bot/attacker tạo hàng trăm accounts + instances
- Mỗi instance tạo 1 PostgreSQL database (~50MB + Flyway migrations)
- Disk đầy → toàn bộ platform down
- Connection pool cạn → các instance hợp lệ cũng bị ảnh hưởng

**Bối cảnh thị trường VN:**
- Payment gateway VN (VietQR, Momo, ZaloPay) **không hỗ trợ** card verification như Stripe
- Người dùng VN ít dùng thẻ credit, chủ yếu chuyển khoản ngân hàng
- → "Yêu cầu thẻ tín dụng" **không phù hợp** với tệp khách hàng

**Đề xuất chống spam (progressive):**

| Biện pháp | Effort | Hiệu quả | Giai đoạn |
|-----------|--------|-----------|-----------|
| Email verification (gửi link xác nhận) | 1 ngày | ⭐⭐⭐⭐ | Phase 1 |
| Rate limit per IP (max 3 đăng ký/giờ) | 2 giờ | ⭐⭐⭐ | Phase 1 |
| Max 2 instances miễn phí per account | 1 giờ | ⭐⭐⭐ | Phase 1 |
| Defer DB provisioning (chỉ provision sau verify) | 1 ngày | ⭐⭐⭐⭐⭐ | Phase 1 |
| reCAPTCHA/hCaptcha trên form đăng ký | 0.5 ngày | ⭐⭐⭐⭐ | Phase 2 |
| Phone OTP verification (qua Zalo/SMS) | 2-3 ngày | ⭐⭐⭐⭐⭐ | Phase 3 |

**Khuyến nghị Phase 1:** Email verification + defer DB provisioning + rate limit + instance limit. Kết hợp 4 biện pháp này chặn được ~95% spam mà không ảnh hưởng UX.

---

## 3. Đề xuất hành động

### Ưu tiên cao (trước khi launch)

| Hành động | Lý do |
|-----------|-------|
| Email verification + defer DB | Chặn spam, tiết kiệm resource |
| Onboarding wizard | User retention, giảm churn |

### Ưu tiên trung bình (sprint sau)

| Hành động | Lý do |
|-----------|-------|
| Local tenant URL fix | Dev experience |
| Rate limit + captcha | Defense in depth |
| Instance limit per account | Kiểm soát resource |

### Có thể defer

| Hành động | Lý do |
|-----------|-------|
| Phone OTP | Tốn chi phí SMS, chưa cần thiết giai đoạn đầu |
| Card verification (Stripe) | Không phù hợp thị trường VN |

---

## 4. Câu hỏi cần leader quyết định

1. **Có đồng ý defer DB provisioning** đến sau email verification không? (Thay đổi flow register hiện tại)
2. **Onboarding wizard** nên có mấy bước? Content cụ thể cần marketing team review không?
3. **Local URL strategy**: Dùng approach nào? (query param vs subdomain vs header)
4. **Captcha**: Dùng reCAPTCHA (Google) hay hCaptcha (privacy-friendly)?
5. **Timeline**: Các items trên cần hoàn thành trước ngày nào?

---

## 5. Tham khảo

- [Flow đăng ký hiện tại](../../kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java)
- [Dashboard hiện tại](../../kitehub/kitehub-frontend/src/app/(customer)/dashboard/page.tsx)
- [User persona](../ui-refactor-plan.md) - Chủ trung tâm giáo dục VN, IT thấp-trung bình
- [E2E test results](../../kitehub/scripts/test-api-e2e.sh) - 63/63 pass (chưa cover onboarding)
