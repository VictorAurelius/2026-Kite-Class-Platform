---
title: Công thức G2 — KH-3 Đăng ký gói trả phí + chuyển khoản VietQR thủ công
audience: dev
created: 2026-06-04
scope: Bàn giao G2 thủ công cho luồng KH-3 (subscription create + trial→paid) thuộc Chiến dịch Xác minh Luồng
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-04-flow-kh3-subscription-trial-paid.md
  - documents/04-quality/audits/persona-review/2026-06-04-pre-walk-flow-kh3-subscription.md
  - documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md
---

# Công thức G2 — KH-3 Đăng ký gói trả phí (trial → paid)

> **Trạng thái:** ✅ **G1 PASS 2026-06-04** (coordinator-walked trên local Docker stack). G2 walk eligible khi 3 PR fix sau merge: **#2157** (V62 migration schema fix GAP-942) + **#2158** (VietQR YAML default GAP-943) + **#2161** (kitehub-admin endpoint collision GAP-941). Pre-walk PRs **#2151** (UC-SUB-01 SUB-20) + **#2152** (admin @PreAuthorize) + **#2153** (Payment account snapshot) đã merged main commit `1ce04fc0`.
>
> **G1 walk evidence:** Owner FREE/TRIAL → POST /api/platform/subscriptions BASIC → HTTP 201 `status=PENDING + pendingPaymentId` → admin confirm (X-User-Roles=PLATFORM_ADMIN) → subscription FREE→BASIC + PENDING→ACTIVE + email "Subscription đã kích hoạt - G2 Test Center" tới MailHog ✅. **3 catalog findings cataloged:** GAP-942 (P0 V62 schema), GAP-943 (P1 YAML default), GAP-941 (P1 admin endpoint collision) — all 3 fix PRs landed cùng wave.

## Mục tiêu G2

Bạn (người dùng) đóng vai **Owner Tuấn** (tenant `g2test-an-8` đã có sẵn từ chuỗi KH-1 + KH-2c) đi hết đường nâng cấp gói BASIC qua chuyển khoản VietQR thủ công, từ chỗ thấy nút "Nâng cấp", chọn gói, quét QR (giả lập), tới khi gói chuyển sang ACTIVE và nhận email xác nhận. Mục đích là xác nhận G1 (do tôi tự walk) không bỏ sót lỗi hiển thị / sai cảm nhận người dùng / chỗ Owner sẽ bối rối.

Thời lượng ước tính: ~15-20 phút (gồm cả bước giả lập admin xác nhận).

## Điều kiện đầu vào

- Stack Docker local đang chạy (đã verify lúc bắt đầu phiên: `kite-gateway`, `kitehub-frontend`, `kitehub-subscription`, `kite-postgres`, `kitehub-admin`, `kitehub-email`, `kite-mailhog` — tất cả `healthy`).
- Hai PR pre-walk đã merge và stack đã rebuild:
  - PR #2150 — `AdminPaymentController` + cấu hình `PAYMENT_MOCK_MODE=true` cho `kitehub-subscription`
  - PR #2149 — Trang nâng cấp FE bỏ Payment trùng + nhánh tạo subscription cho Owner FREE/TRIAL
- Owner Tuấn đã có sẵn trên DB:
  - Email: `g2test-an-8@example.com`
  - Mật khẩu: `WalkKh3@2026` (tôi reset trong session này để G2 dùng được)
  - Tenant: `g2test-an-8`, đang TRIAL/FREE, còn 14 ngày
- Trình duyệt sạch (không session cũ), DevTools mở tab Network bật log.

## Thiết lập trước khi bắt đầu

| Mục | Lệnh / hành động |
|---|---|
| Kiểm tra cổng gateway | `curl -sS http://localhost:9000/actuator/health` → phải trả `{"status":"UP",...}` (Gateway thật ở port **9000**, KHÔNG phải 8080 — đây là điểm khác KH-1 / KH-2c handoff cũ ghi 8080) |
| Mở hộp thư test | Trình duyệt → `http://localhost:8025/` (MailHog UI) — xóa hết thư cũ để dễ nhận biết thư mới |
| Xác nhận trạng thái Owner ban đầu | `docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT i.subdomain, i.tier, i.status, i.subscription_id FROM instances i WHERE i.subdomain = 'g2test-an-8';"` → kết quả mong đợi: `tier=FREE, status=TRIAL, subscription_id=null` |
| Admin auth setup (UPDATED per PR #2152 + G1 verify) | `X-Admin-Api-Key` đã DEPRECATED. Thay bằng JWT `PLATFORM_ADMIN` role qua gateway. **Phase 1 BETA workaround (G1 walk pattern):** curl direct vào subscription:8081 với header `X-User-Roles: PLATFORM_ADMIN` + `X-User-Id: <any UUID>` — simulate gateway-forward post-JWT-decode. Production-equivalent flow (Phase 1.5+): login admin user → JWT → gateway forwards `X-User-Roles` header per `pre-launch-auth-hardening-checklist.md`. |

## Các bước test

### Bước 1 — Đăng nhập Owner

**Hành động:**
1. Mở `http://localhost:3000/login` (FE Next.js của KiteHub)
2. Nhập email `g2test-an-8@example.com` + mật khẩu `WalkKh3@2026` → submit

**✅ Kỳ vọng (PASS):**
- Redirect tới trang dashboard Owner (`/dashboard` hoặc `/`)
- Header hiển thị tên trung tâm "G2 Test Center" + badge "Gói FREE" (hoặc tương đương)
- DevTools Network thấy `POST /api/auth/login` trả HTTP 200 + payload chứa `accessToken`, `instances[0].tier="FREE"`, `instances[0].status="TRIAL"`, `instances[0].trialDaysLeft=14`

**⚠️ Sad path:**
- Sai mật khẩu → toast tiếng Việt "Email hoặc mật khẩu không đúng" (HTTP 400 hoặc 401 đều chấp nhận, đây là GAP-917 chưa thống nhất spec)
- Gateway 503 cold-start (giống GAP-918 ở KH-1) → đợi 30 giây + thử lại

**🔍 Verify:** DevTools Application → Local Storage có key `accessToken` chứa JWT.

---

### Bước 2 — Vào trang quản lý gói

**Hành động:**
1. Trên dashboard, tìm điều hướng "Gói & Thanh toán" / "Billing" / icon ví → click
2. URL chuyển `/billing`

**✅ Kỳ vọng (PASS):**
- Trang hiển thị "Gói hiện tại: FREE" hoặc thông báo "Bạn đang dùng thử miễn phí"
- Hiện banner / nút "Nâng cấp gói trả phí"
- (đã biết thiếu — Finding #4 trong tài liệu pre-walk) **KHÔNG thấy đếm ngược "Còn 14 ngày dùng thử"**. Đây là gap đã biết, cần báo lại nếu thấy bất ngờ.

**⚠️ Sad path:**
- Trang trắng / spinner mãi mãi → finding #3 chưa fix đúng, báo lại tôi xem PR #2149 merge chưa.

---

### Bước 3 — Chọn gói nâng cấp

**Hành động:**
1. Click "Nâng cấp"
2. URL chuyển `/billing/upgrade`
3. Trang hiện danh sách bậc gói (BASIC / PREMIUM / ENTERPRISE) + chu kỳ (THÁNG / NĂM)
4. Chọn **BASIC + MONTHLY** → click "Xác nhận nâng cấp"

**✅ Kỳ vọng (PASS):**
- Trang hiện hộp thoại xác nhận với số tiền cụ thể (VND format `390.000đ` hoặc tương tự — chính xác cần đối chiếu `rules.md` SUB-pricing sau khi G1 verify)
- Click "Xác nhận" → DevTools Network thấy `POST /api/platform/subscriptions` (đường tạo mới — KHÔNG phải PATCH /upgrade) trả HTTP 201 + payload chứa `id` (subscription UUID) + `pendingPaymentId` (UUID khác)
- Redirect tự động sang `/billing/payment/{pendingPaymentId}`

**⚠️ Sad path:**
- Network trace thấy `PATCH /api/platform/subscriptions/{...}/upgrade` thay vì POST → PR #2149 fix sai, báo lại
- Network thấy 2 lệnh POST cùng lúc (tạo Payment lần 2) → finding #2 quay lại, báo lại
- Gateway 503 → đợi cold-start

**🔍 Verify:** Sau khi redirect, query DB
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT s.id, s.tier, s.status, s.pending_tier, s.pending_payment_id FROM subscriptions s JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8';"
```
→ phải có **đúng 1 dòng**: `tier=FREE`, `status=PENDING`, `pending_tier=BASIC`, `pending_payment_id` không null. Nếu thấy nhiều hơn 1 dòng → BR-SUB-17 idempotency vỡ.

---

### Bước 4 — Trang hiển thị mã QR VietQR

**Hành động:**
1. Sau redirect, trang `/billing/payment/{id}` mở
2. Quan sát nội dung trang

**✅ Kỳ vọng (PASS):**
- Hiện hình ảnh QR (mock-mode, có thể là placeholder hoặc QR generate offline)
- Thông tin chuyển khoản hiện rõ:
  - Ngân hàng (ví dụ "Vietcombank")
  - Số tài khoản
  - Tên chủ tài khoản
  - **Số tiền cần chuyển** (VND format)
  - **Nội dung chuyển khoản** (cụ thể, có chứa subscription id hoặc payment reference)
- Có hướng dẫn ngắn gọn tiếng Việt: "Sau khi chuyển khoản, vui lòng đợi xác nhận từ KiteHub trong vòng 24 giờ"
- (cần G1 verify) trạng thái polling `pending` hiện lên — trang tự refresh mỗi vài giây

**⚠️ Sad path:**
- QR trống / lỗi 500 → finding #5 (mock-mode chưa bật), chạy `docker exec kitehub-subscription printenv PAYMENT_MOCK_MODE` phải thấy `true`. Nếu thiếu → PR #2150 merge chưa hoặc stack chưa rebuild.

**🔍 Verify (giả lập chuyển khoản):**
Trong G2 không có hệ thống ngân hàng thật. Bạn coi như đã chuyển khoản và chuyển sang Bước 5 (admin xác nhận). KHÔNG cần thực hiện chuyển khoản thật.

---

### Bước 5 — Admin xác nhận thanh toán (giả lập)

**Hành động (qua curl thay vì admin UI cho gọn):**

1. Lấy lại `pendingPaymentId` từ DB (lệnh ở Bước 3 §Verify)
2. Chạy lệnh xác nhận (verified G1 walk pattern — direct service call với forged auth header):
   ```bash
   curl -sS -X POST http://localhost:8081/api/platform/admin/payments/<PAYMENT_ID>/confirm \
     -H "X-User-Id: 00000000-0000-0000-0000-000000000099" \
     -H "X-User-Roles: PLATFORM_ADMIN" \
     -H "Content-Type: application/json" \
     -d '{"transactionId":"WALK-KH3-G2-TEST-001"}' \
     -w "\nHTTP=%{http_code}\n"
   ```
   **Lưu ý:** port 8081 = subscription service direct (bypass gateway:9000 cho test scope; gateway path POST cũng work nhưng cần real JWT forwarding). `X-User-Roles` header simulate gateway-decoded JWT role per PR #2152 @PreAuthorize wiring. G1 walk verified: missing header → HTTP 401; with PLATFORM_ADMIN role → HTTP 200.

**✅ Kỳ vọng (PASS):**
- HTTP 200, payload trả về Payment với `status=COMPLETED` + `transactionId=WALK-KH3-G2-TEST-001`
- DB:
  ```sql
  SELECT s.tier, s.status, s.pending_tier, s.pending_payment_id, s.expires_at
  FROM subscriptions s JOIN instances i ON s.instance_id=i.id
  WHERE i.subdomain='g2test-an-8';
  ```
  → phải thấy `tier=BASIC` (đã chuyển), `status=ACTIVE`, `pending_tier=null`, `pending_payment_id=null`, `expires_at` ~ 1 tháng sau (vì MONTHLY).

**⚠️ Sad path:**
- HTTP 404 → `AdminPaymentController` chưa lên (PR #2150/#2152 chưa merge / chưa rebuild). Báo lại.
- HTTP 401 → header `X-User-Roles` thiếu / spelling sai (PR #2152 @PreAuthorize enforce). Verify: thử lại không header → expect 401 (smoke test confirm endpoint up).
- HTTP 403 → header có nhưng role không phải `PLATFORM_ADMIN` (vd `OWNER`). Verify: PR #2152 reject non-admin.
- HTTP 409 SQLState 42P10 / chk_subscription_status → PR #2157 V62 chưa merge / chưa apply (Flyway). Manual fallback: `docker exec kite-postgres psql -U kitehub -d kitehub -c "ALTER TABLE subscriptions ALTER COLUMN started_at DROP NOT NULL, ALTER COLUMN expires_at DROP NOT NULL; ALTER TABLE subscriptions DROP CONSTRAINT chk_subscription_status; ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','CANCELLED','EXPIRED'));"`. G1 walk surfaced this — GAP-942 fix PR #2157.
- DB thấy `tier` vẫn FREE sau khi confirm 200 → finding #2 quay lại (admin confirm Payment khác với `pendingPaymentId`).
- DB Payment account_number/account_name empty → PR #2158 (GAP-943 YAML default) chưa merge. Manual fallback: `UPDATE payments SET account_number='1234567890', account_name='CONG TY KITECLASS' WHERE id='<PAYMENT_ID>';`.

**🔍 Verify thêm — admin từ chối (test sad path khác):** *(tùy chọn)*
Trước khi confirm, có thể test reject path:
```bash
curl -sS -X POST http://localhost:9000/api/platform/admin/payments/<PAYMENT_ID>/reject \
  -H "X-Admin-Api-Key: <ADMIN_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Không tìm thấy giao dịch khớp"}' \
  -w "\nHTTP=%{http_code}\n"
```
→ DB phải thấy `pending_tier=null` + `pending_payment_id=null` + `tier` vẫn FREE. Owner có thể submit lại từ Bước 3.

---

### Bước 6 — Quay lại Owner check trạng thái

**Hành động:**
1. Quay lại trình duyệt Owner đang ở trang `/billing/payment/{id}`
2. Refresh (F5) hoặc đợi polling tự refresh

**✅ Kỳ vọng (PASS):**
- Trang chuyển sang thông báo "Thanh toán thành công, gói BASIC đã được kích hoạt" (hoặc tương đương)
- Hiển thị nút quay về `/billing`
- Vào lại `/billing` → "Gói hiện tại: BASIC" + ngày hết hạn ~ 1 tháng tới

**⚠️ Sad path:**
- Trang vẫn hiện "pending" sau khi DB đã `ACTIVE` → polling FE bị hỏng, báo lại để vá ở vòng sau.
- Refresh ra 404 → PR #2149 không cover sad path này.

**🔍 Verify email:**
Mở MailHog UI `http://localhost:8025/`:
- Phải có **1 thư mới** tới `g2test-an-8@example.com`
- Subject chứa "BASIC" hoặc "Gói trả phí đã kích hoạt" (tùy template)
- Body tiếng Việt, có chi tiết gói + ngày hết hạn

Nếu MailHog không có thư → đây là finding #8 trong tài liệu pre-walk (email send fire-and-forget), báo lại để vá vòng sau.

---

## Bước 7 — Sad path quét nhanh

Sau khi đường happy path PASS, thử nhanh các kịch bản này:

### 7.1 — Owner submit lần 2 với gói khác đang còn pending
1. Login Owner mới hoặc xóa subscription cũ bằng SQL
2. Submit upgrade BASIC → đợi pending
3. KHÔNG xác nhận, quay về `/billing/upgrade` chọn PREMIUM → submit
4. ✅ Kỳ vọng: BE trả HTTP 409 + message rõ ràng tiếng Việt "Gói nâng cấp đang chờ thanh toán, vui lòng hoàn tất giao dịch BASIC trước" hoặc tương đương. **KHÔNG được trả 400 generic** (finding #6).

### 7.2 — Trial gần hết hạn lúc đang pending payment
- Bỏ qua trong G2 thủ công (cần backdate timestamp DB, để vòng sau).

### 7.3 — Nội dung chuyển khoản có dấu tiếng Việt
- Quan sát Bước 4: nội dung chuyển khoản có dấu (ví dụ "Thanh toán gói BASIC trung tâm G2 Test Center") phải hiển thị đúng, không bị `&acirc;` mã hóa (finding #7).

---

## Cách báo kết quả G2

Sau khi xong (hoặc dừng giữa chừng vì gặp blocker nặng), trả về **một trong 4 trạng thái** sau:

| Trạng thái | Khi nào dùng | Cần gửi kèm |
|---|---|---|
| ✅ **FULL PASS** | Cả 6 bước happy path + ít nhất 7.1 sad path PASS, email tới MailHog | Xác nhận ngắn "Đi hết được", không cần evidence |
| ⚠️ **MOSTLY PASS có cosmetic** | Happy path xong nhưng có gap kiểu Trial countdown thiếu / copy chưa chuẩn / email subject lạ | Liệt kê 1 dòng cho mỗi cosmetic; tôi sẽ vá vòng sau |
| 🔴 **BLOCKING** | Có bước fail nặng (gateway 503 không khôi phục, BE 500, DB sai trạng thái) | Chụp Network tab + DB query output + bước số mấy. Tôi gom catalog + vá batch |
| ❓ **UNCLEAR** | Không chắc kết quả PASS hay FAIL, lạ một chỗ nào | Ping với screenshot + giải thích ngắn, tôi nhìn cùng |

Khi nhận báo cáo, tôi sẽ flip campaign §4 dòng KH-3: `🔄 walk-pass-pending-human` → `✅ G1+G2 chờ G3 production parity` (nếu PASS) hoặc thêm vòng vá (nếu BLOCKING).

---

## Khắc phục nhanh khi gặp sự cố

| Hiện tượng | Cách thử trước |
|---|---|
| Gateway timeout / 503 | `docker restart kite-gateway` + đợi 30-60 giây, retry. Đây là GAP-918 đã biết. |
| Login 401 | Mật khẩu Owner đã reset lại? Lấy lệnh reset từ tôi (BCrypt hash session-locked). |
| Trang trắng `/billing` | Verify PR #2149 đã merge + stack rebuild `bash kitehub/scripts/rebuild.sh kitehub-frontend` |
| Admin endpoint 404 | Verify PR #2150 đã merge + stack rebuild `bash kitehub/scripts/rebuild.sh kitehub-subscription` |
| QR trống | `docker exec kitehub-subscription printenv PAYMENT_MOCK_MODE` phải = `true`; nếu không, restart container |
| Email không tới MailHog | Đây là finding #8 (email fire-and-forget). Báo lại, không phải lỗi của bạn. |

---

## Lưu ý cho G3 (production parity, vòng sau)

G2 chạy trên stack local Docker. G3 sẽ chạy trên môi trường production-equivalent (AWS EC2 stack đang stopped per GAP-612, cần khởi động lại). G3 verify checklist:

### G3 prerequisites (blocker GAP-612 — AWS account suspended)
- AWS EC2 instances `kitehub-kh-backend` + `kitehub-kc-app` restarted post-suspension-restore
- RDS `kitehub-postgres` available + V62 migration applied (Flyway auto on container restart)
- ECR images: latest commit từ main pushed via `.github/workflows/docker-build-push.yml`

### G3 verify items (post-restore)
- **VietQR provider thật** (không mock-mode):
  - AWS Secrets Manager `kitehub/production/vietqr-api-key` populated với real API key (Phase 1.5 paid)
  - Override `PAYMENT_MOCK_MODE=false` via systemd env / fetch-secrets.sh
  - Real QR generated chứa account info đúng
- **VietQR account info real** (post PR #2158 + Secrets Manager):
  - AWS Secret `kitehub/production/vietqr-account-number` + `vietqr-account-name` populated (KiteHub thật, không phải dev default `1234567890` / `CONG TY KITECLASS`)
  - Production Payment row chứa real account info — Owner thấy QR scan-able với app banking thật
- **Email SES gửi tới hộp thư thật** (không MailHog):
  - SES domain `kitehub.me` verified + production-approved (out of sandbox)
  - DKIM signature pass (Gmail Inbox không Spam)
  - Subject "Subscription đã kích hoạt - <tenant>" render đúng diacritics
- **Admin auth real flow** (gateway → JWT → header forward):
  - Real PLATFORM_ADMIN user created (KHÔNG dev test admin)
  - Gateway forwards JWT role correctly to subscription:8080 (production port, không 8081 local)
  - End-to-end: admin login UI → confirm payment UI → state flip → email send
- **Trial countdown component** (Finding #4 pre-walk audit) shipped + visible Owner UI
- **Trial expiry scheduler** (Finding #9 pre-walk audit) hoạt động — cron trigger DB UPDATE + email send

### G3 blocker chain
- **GAP-612** — AWS account suspended; restore unblocks all G3 verify items above
- **GAP-820** — Phase 1.5+ paid VietQR API provisioning (out of scope Phase 1 BETA — sister gap defer)
- **GAP-943** PR #2158 — YAML default fix needs deploy + restart subscription service in production

**Status:** G3 deferred post-GAP-612 unblock + Phase 1.5 paid setup. KH-3 campaign §4 row sẽ flip `🔄 walk-pass-pending-human` → `✅ THÔNG (G1+G2)` sau khi user G2 PASS; `+ G3` chỉ flip khi production verify done. Per `feature-ship-runtime-walk-mandate.md` v1.1.0 §1 walk evidence model.
