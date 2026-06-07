# SePay Account Setup Runbook — Phase 1 BETA Payment Reconciliation

**Audience:** Solo dev đăng ký SePay merchant lần đầu — đối soát chuyển khoản tự động cho **KiteHub subscription billing** (trung tâm trả phí gói cho KiteHub).
**Standards:** `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2 · `deployment-naming-convention.md` §2 (`account-prep/` — one-time per merchant account).
**Cross-link upstream:** Domain `kitehub.me` đã verify + HTTPS active (production deploy) để webhook URL reachable.
**Cross-link downstream:** Unblocks `PaymentWebhookController` (`POST /api/platform/webhooks/payment`) + `PaymentService.processSepayWebhook` + GAP-975 (dynamic VietQR txnRef) + GAP-976 (SePay webhook auth + idempotency).
**Estimated time:** ~30-45 phút (đăng ký + liên kết bank + cấu hình webhook).
**Last-Updated:** 2026-06-08

---

## 0. SCOPE — runbook này dành cho luồng nào?

> 🔴 **Runbook này = KiteHub subscription billing**, KHÔNG phải KiteClass tuition. Hai luồng thanh toán độc lập:

| Luồng | Module / webhook | txnRef / gateway | Ai trả → ai nhận | Runbook này? |
|---|---|---|---|---|
| **KiteHub subscription** | `kitehub-subscription`, `POST /api/platform/webhooks/payment` | `KH3SUB<8 hex>` + SePay | Trung tâm trả phí gói → **KiteHub (platform/bạn)** — `applyPendingUpgrade` nâng gói trial→paid | ✅ ĐÚNG |
| **KiteClass tuition** | `kiteclass-core/module/payment`, `ParentPaymentController` | MoMo gateway (+ VietQR/Casso Phase 1.5) | Phụ huynh trả học phí → **trung tâm** (per-tenant) | ❌ flow riêng — xem `kiteclass-core` payment module |

→ Tài khoản ngân hàng nhận trong runbook này = **tài khoản business của KiteHub/bạn** (1 tài khoản chung mọi trung tâm chuyển phí gói vào), KHÔNG phải tài khoản riêng của từng trung tâm.

---

## TL;DR

SePay (https://sepay.vn) đọc **biến động số dư** tài khoản ngân hàng của bạn rồi **POST webhook** về KiteHub mỗi khi có **chuyển khoản đến** (mọi giao dịch tiền vào, KHÔNG chỉ qua mã QR của SePay). KiteHub khớp `txnRef` (`KH3SUB<8 hex>`) nhúng trong nội dung chuyển khoản → đánh dấu payment PAID + `applyPendingUpgrade` nâng gói subscription. Free tier 50 giao dịch/tháng (đếm theo **tổng giao dịch tiền vào**) đủ cho Phase 1 BETA.

> 🔴 **BẮT BUỘC dùng TÀI KHOẢN NGÂN HÀNG RIÊNG (của KiteHub/bạn) để nhận phí subscription** — KHÔNG dùng chung tài khoản cá nhân. Lý do: SePay đếm hạn mức 50 giao dịch/tháng theo *mọi* tiền vào tài khoản. Giao dịch cá nhân lặt vặt (người nhà chuyển tiền, hoàn tiền, v.v.) sẽ đốt quota miễn phí dù chẳng liên quan thanh toán, và mỗi giao dịch lạ còn tạo webhook orphan (HTTP 400) gây nhiễu log. Xem §2.2.

3 việc cần làm:
1. Đăng ký SePay + liên kết **tài khoản ngân hàng riêng** của KiteHub/bạn (real-user action — KYC).
2. Cấu hình webhook URL `https://kitehub.me/api/platform/webhooks/payment` + copy API key.
3. Set API key vào AWS Secrets Manager `kitehub/production/sepay-api-key` (AWS account đã restore — GAP-612 DONE 2026-05-26; stack hiện idle/stopped → `bash scripts/aws/start-stack.sh` khi cần set + deploy).

**Code đã sẵn sàng** (Wave flow-kh3 + p0-local): terraform secret + `fetch-secrets.sh` pull + `application.yml` wiring đều xong. Runbook này chỉ là phần đăng ký + cấu hình dashboard.

> 💡 **Verify TRƯỚC bằng Test Mode (khuyến nghị mạnh):** SePay có **Chế độ thử nghiệm** (Test Mode) — tài khoản giả lập + mô phỏng giao dịch + webhook test, **không cần tiền thật, không cần tài khoản thật, không đốt quota 50**. Đây là cách verify logic webhook + txnRef matching + idempotency TRƯỚC khi đụng tài khoản thật. Xem §4.5.

---

## 1. Trước khi bắt đầu — chuẩn bị

| Cần có | Ghi chú |
|---|---|
| Tài khoản ngân hàng VN | SePay hỗ trợ đa số bank VN (VCB / TCB / MB / ACB / BIDV / VPBank...). Dùng **tài khoản business riêng của KiteHub/bạn** để nhận phí subscription (per §0 scope — KHÔNG phải tài khoản trung tâm). |
| Số điện thoại + email | Để đăng ký + xác thực SePay account. |
| Domain `kitehub.me` HTTPS live | Webhook URL phải reachable từ internet (SePay gọi POST). Production deploy phải xong + HTTPS active. |
| AWS Secrets Manager access | Để set API key. AWS account đã restore (GAP-612 DONE); stack có thể đang idle → start khi cần. |

✅ **AWS account đã restore** (GAP-612 DONE 2026-05-26 — Wave aws-restore-1). Stack hiện thường **idle/stopped** để tiết kiệm Free Tier; bước 4 (set secret + deploy) cần `bash scripts/aws/start-stack.sh` trước. Bước 1-3 (đăng ký + dashboard) làm trước được, không cần stack.

---

## 2. Đăng ký SePay merchant

### 2.1 Tạo tài khoản

1. Vào https://sepay.vn → **Đăng ký**.
2. Nhập email + số điện thoại + mật khẩu → xác thực OTP.
3. Đăng nhập dashboard.

### 2.2 Liên kết tài khoản ngân hàng — DÙNG TÀI KHOẢN RIÊNG

> 🔴 **BẮT BUỘC:** liên kết một **tài khoản ngân hàng business riêng của KiteHub/bạn để nhận phí subscription** (per §0 scope) — KHÔNG dùng tài khoản cá nhân / tài khoản chi tiêu hàng ngày.

**Vì sao bắt buộc tài khoản riêng:**

| Rủi ro khi dùng chung TK cá nhân | Hệ quả |
|---|---|
| SePay đếm quota theo **mọi giao dịch tiền vào** (xem §2.3) | Giao dịch cá nhân (người nhà chuyển, hoàn tiền, lương...) đốt quota 50 free dù không liên quan thanh toán |
| Mỗi tiền vào → 1 webhook POST về KiteHub | Giao dịch lạ không có `txnRef` khớp → KiteHub trả `400` orphan → nhiễu log + tốn xử lý |
| Lẫn dòng tiền cá nhân + kinh doanh | Khó đối soát kế toán + rủi ro lộ thông tin tài chính cá nhân qua webhook payload |

**Khuyến nghị:** mở 1 tài khoản ngân hàng mới (hoặc tài khoản doanh nghiệp/hộ kinh doanh) chỉ dùng để nhận phí subscription KiteHub. SePay hỗ trợ không giới hạn số tài khoản liên kết — có thể thêm sau khi mở rộng.

**Các bước:**

1. Dashboard → **Tài khoản ngân hàng** → **Thêm tài khoản**.
2. Chọn ngân hàng + nhập **số tài khoản RIÊNG dùng nhận thanh toán** (không phải TK cá nhân).
3. SePay hướng dẫn cấp quyền đọc biến động số dư (qua API ngân hàng hoặc kết nối Internet Banking — tùy bank). Làm theo hướng dẫn từng bank.
4. Chờ SePay xác nhận liên kết thành công (thường vài phút → vài giờ tùy bank).

### 2.3 Free tier — 50 giao dịch/tháng đếm theo TỔNG tiền vào

- Gói Free: **0đ/tháng, 50 giao dịch/tháng** — đủ cho Phase 1 BETA cohort nhỏ.
- ⚠️ **Cách đếm:** "tổng số lượng giao dịch **tiền vào** trong một tháng" — đếm MỌI khoản tiền vào tài khoản, KHÔNG phải chỉ giao dịch khớp invoice KiteHub. Đây là lý do §2.2 bắt buộc tài khoản riêng (TK sạch = quota chỉ tiêu cho thanh toán thật).
- Vượt 50 → vẫn dùng được nhưng tính **phí phụ trội** (overage), hoặc nâng gói trả phí:
  - **Startup**: 120.000đ/tháng — 180+ giao dịch
  - **Shop**: 99.000đ/tháng/điểm bán — không giới hạn giao dịch
- **Theo dõi đụng trần:** xem **tổng giao dịch tiền vào trên SePay dashboard** (con số này = chạm trần 50), KHÔNG chỉ `payment_records` count (chỉ đếm giao dịch khớp invoice — luôn thấp hơn tổng SePay thấy).
- Nguồn: https://sepay.vn/bang-gia.html · https://docs.sepay.vn/goi-dich-vu.html

---

## 3. Cấu hình webhook + API key

### 3.1 Tạo API key

1. Dashboard → **Cấu hình** → **API / Webhook** (hoặc **Tích hợp**).
2. Tạo **API Key** mới (đây là key dùng cho header `Authorization: Apikey <key>`).
3. **Copy + lưu tạm an toàn** (password manager) — sẽ dùng ở bước 4. KHÔNG commit vào repo.

### 3.2 Cấu hình webhook URL

1. Dashboard → **Webhooks** → **Thêm webhook**.
2. URL: `https://kitehub.me/api/platform/webhooks/payment`
3. Method: `POST`
4. Authentication: chọn **Apikey** (SePay gửi header `Authorization: Apikey <key>` — khớp với `kitehub.payment.sepay.api-key`).
5. Event: chọn **Giao dịch tiền vào** (incoming credit) — chỉ cần khi có tiền chuyển đến.
6. Lưu.

> ℹ️ **Auth method — Apikey vs OAuth2:** SePay hỗ trợ 2 cách xác thực webhook:
> - **Apikey** (static header `Authorization: Apikey <key>`) — **code KiteHub hiện chỉ implement cách này** (`PaymentWebhookController.verifyApiKey` constant-time compare). Dùng cho Phase 1 BETA.
> - **OAuth2 client_credentials** (SePay lấy bearer token rồi đính kèm mỗi request) — bảo mật hơn (token xoay vòng) nhưng KiteHub **chưa** implement verify bearer. Defer Phase 1.5+ hardening (xem note cuối runbook).
>
> → Phase 1 BETA: chọn **Apikey**. Đừng chọn OAuth2 vì controller sẽ reject (chưa có code verify bearer).

### 3.3 Định dạng nội dung chuyển khoản (txnRef)

KiteHub nhúng `txnRef` dạng `KH3SUB<8 hex>` (regex `KH3SUB[A-F0-9]{8}`) vào **nội dung chuyển khoản** của mã VietQR sinh cho mỗi invoice. SePay gửi nguyên `description` về webhook → `PaymentService` extract txnRef + khớp `Payment.findByTxnRef`.

> Không cần cấu hình gì thêm ở SePay cho txnRef — nó nằm trong description do KiteHub tự sinh khi tạo mã QR. Đảm bảo SePay forward đầy đủ field `description` trong payload webhook (mặc định có).

---

## 4. Set API key vào AWS Secrets Manager

AWS account 906286017800 đã restore (GAP-612 DONE). Khi stack đang idle → `bash scripts/aws/start-stack.sh` trước, rồi:

```bash
# 1. terraform apply tạo placeholder secret (đã declare ở infrastructure/terraform-aws/secrets.tf)
#    resource aws_secretsmanager_secret.sepay_api_key — name kitehub/production/sepay-api-key

# 2. Set giá trị thật qua AWS console:
#    Secrets Manager -> kitehub/production/sepay-api-key -> Retrieve secret value
#    -> Set new value -> plain string "<sepay-api-key-copy-từ-bước-3.1>"

# Hoặc qua CLI (khi có dev-admin creds):
aws secretsmanager put-secret-value \
  --secret-id kitehub/production/sepay-api-key \
  --secret-string '<sepay-api-key>' \
  --profile dev-admin --region ap-southeast-1
```

`fetch-secrets.sh` pull secret này lúc EC2 boot vào `/etc/kite/.env` dưới biến `SEPAY_API_KEY`. `lifecycle ignore_changes = [secret_string]` giữ giá trị thật qua các lần `terraform apply` sau.

---

## 4.5 Test Mode — verify logic TRƯỚC khi đụng tiền thật (khuyến nghị mạnh)

SePay có **Chế độ thử nghiệm** (Test Mode): tài khoản ngân hàng giả lập + mô phỏng giao dịch + webhook test + tạo QR test — **dữ liệu hoàn toàn tách biệt, không ảnh hưởng tài khoản thật, không tốn tiền thật, không đốt quota 50 giao dịch**.

### 4.5.1 Vì sao dùng Test Mode trước

| Verify bằng tiền thật (§5) | Verify bằng Test Mode |
|---|---|
| Cần tài khoản thật + liên kết KYC xong | Tài khoản giả lập tạo ngay |
| Cần chuyển khoản thật (tốn tiền, dù 10k) | Mô phỏng giao dịch 1 click, miễn phí |
| Cần production HTTPS domain live (cần start stack + deploy) | Có thể trỏ webhook về tunnel → local stack |
| Mỗi lần test đốt 1 quota | Không tính quota |

→ Test Mode **tách rời việc verify logic (txnRef matching + idempotency + orphan handling + PAID flip) khỏi việc phải start stack + deploy + tiền thật.** Đây là cách đóng phần "live verify 15% còn lại" của GAP-975 + GAP-976 mà không cần production stack (GAP-1058).

### 4.5.2 8 bước thiết lập Test Mode (theo dashboard SePay)

1. **Tạo tài khoản ngân hàng giả lập** — Dashboard (Test mode) → thêm tài khoản để nhận giao dịch thử.
2. **Cấu hình mã thanh toán** — cấu trúc mã nhận diện tự động (khớp regex `KH3SUB[A-F0-9]{8}` KiteHub sinh).
3. **Cấu hình webhook** — URL endpoint nhận thông báo:
   - Nếu test với **local stack**: dùng tunnel (`cloudflared tunnel --url http://localhost:9000` hoặc ngrok) → trỏ webhook về `https://<tunnel>/api/platform/webhooks/payment`.
   - Nếu test với **staging/production deployed**: trỏ thẳng `https://kitehub.me/api/platform/webhooks/payment`.
   - Auth: **Apikey** + dùng **API key của test mode** (xem bước 7) — KHÁC key production.
4. **Mô phỏng giao dịch** — tạo giao dịch thử để kích hoạt webhook (nhập số tiền + nội dung chứa `txnRef`).
5. **Kiểm tra log webhook** — xác nhận SePay gọi webhook đúng + KiteHub trả 200.
6. **Thử tạo mã QR** — tạo QR chuyển khoản từ tài khoản giả lập (mô phỏng quét mã tại trang Tạo QR).
7. **Tạo API key test mode** — key riêng cho test mode (set vào `SEPAY_API_KEY` của local/staging env).
8. **Thử kết nối API** — gọi thử endpoint API bằng API key test mode.

### 4.5.3 Ảnh hưởng đến config — dual credential

Code dùng 1 biến `SEPAY_API_KEY` (`application.yml:224` `${SEPAY_API_KEY:}`), **environment-switchable** — KHÔNG cần đổi code, chỉ set giá trị khác nhau theo môi trường:

| Môi trường | `SEPAY_API_KEY` value | Nguồn |
|---|---|---|
| Local / staging | **API key test mode** (bước 7) | `.env` local / staging secret |
| Production | **API key production** (§3.1) | AWS Secrets Manager `kitehub/production/sepay-api-key` |

→ Verify Test Mode trên local/staging với test-mode key; production giữ key thật. Không lẫn nhau.

### 4.5.4 Test Mode vs beta-mode override — bổ trợ, không trùng

| Cơ chế | Mục đích | Tiền thật? |
|---|---|---|
| **Test Mode (SePay)** | Verify logic webhook (dev/CI) — txnRef, idempotency, orphan, PAID flip | ❌ Không (giả lập) |
| **beta-mode override** (`application.yml:216`, `BETA_PAYMENT_OVERRIDE`) | Beta tester thật chuyển khoản thật số tiền nhỏ (10k) để sanity-check end-to-end với tài khoản thật | ✅ Có (10k thật) |

→ Dùng **Test Mode** cho verify logic không tốn kém; bật **beta-mode** khi cần 1 lần sanity-check tiền thật trước go-live.

---

## 5. Verify (sau khi live)

1. Tạo 1 invoice test → KiteHub sinh mã VietQR với `txnRef` `KH3SUB...` trong description.
2. Chuyển khoản số tiền nhỏ (vd 10.000đ) đúng nội dung từ tài khoản khác.
3. SePay phát hiện biến động → POST webhook về `/api/platform/webhooks/payment`.
4. Verify:
   - HTTP 200 từ webhook (SePay dashboard → webhook log).
   - DB: `payment_records` có row mới + invoice status → PAID.
   - Log kitehub-subscription: `Processing SePay webhook: id=..., amount=10000, description=...`.
5. Idempotency: SePay gửi lại cùng `id` → webhook trả 200 sớm, không double-process (GAP-976).

### Sad path
- Sai/thiếu API key → webhook trả `401`.
- txnRef không khớp invoice nào → `400` (orphan txnRef).
- `SEPAY_API_KEY` empty trong production → webhook reject toàn bộ (fail closed).

---

## 6. Beta-mode override (test rẻ)

`application.yml` có `beta-mode.enabled` + `override-amount-vnd` (default 10000) — khi bật, mọi invoice dùng số tiền override nhỏ để test thật không tốn nhiều. Tắt khi go-live thật.

---

## 7. Liên quan

- `PaymentWebhookController` + `PaymentService.processSepayWebhook` — `kitehub/kitehub-subscription`
- `infrastructure/terraform-aws/secrets.tf` — `aws_secretsmanager_secret.sepay_api_key`
- `scripts/fetch-secrets.sh` — pull `SEPAY_API_KEY` (line ~120)
- GAP-975 (dynamic VietQR txnRef + beta amount) · GAP-976 (webhook Apikey auth + idempotency)
- Sister vendor: `zalo-oa-setup-runbook.md` (notification) · `06-resend-account-setup.md` (email)

### Phase 1.5+ hardening (defer)

- **OAuth2 webhook auth** — SePay hỗ trợ `client_credentials` → bearer token thay cho static Apikey. Bảo mật hơn (token xoay vòng, không lộ static key trong header). KiteHub hiện chỉ verify Apikey (`PaymentWebhookController.verifyApiKey`). Implement OAuth2 bearer verify = Phase 1.5 security hardening (sister với OWASP A02/A07 per `pre-launch-owasp-rest-hardening-checklist.md`). Chưa file gap — đề xuất tạo nếu user muốn track.
