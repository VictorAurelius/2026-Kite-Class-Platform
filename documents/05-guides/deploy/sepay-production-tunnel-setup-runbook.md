# SePay Production Tunnel Setup Runbook — webhook ingress qua Cloudflare Named Tunnel

**Last Updated:** 2026-06-10
**Audience:** dev / ops (solo-dev)
**Scope:** Cấu hình một **public HTTPS URL ổn định** để SePay gọi webhook thanh toán tới backend production, KHÔNG cần full AWS ALB + ACM stack đang chạy. Dùng **Cloudflare Named Tunnel** (`cloudflared`).
**Cross-link:** `account-prep/sepay-account-setup-runbook.md` (đăng ký merchant + API key + Test Mode) · `operations/sepay-webhook-local-verify-recipe.md` (verify local).

---

## 0. Khi nào dùng runbook này

SePay (https://sepay.vn) theo dõi tài khoản ngân hàng và **gọi webhook** của bạn mỗi khi có chuyển khoản vào. Để SePay gọi được, webhook PHẢI là một URL **public HTTPS ổn định**.

| Tình huống production | Cách trỏ webhook |
|---|---|
| Full AWS stack up (`kitehub.me` qua Cloudflare → ALB → EC2) | Trỏ thẳng `https://kitehub.me/api/platform/webhooks/payment` — **KHÔNG cần tunnel** |
| Stack AWS idle/stopped HOẶC chưa có ALB+ACM (Free Tier) | **Dùng tunnel** (runbook này) — `cloudflared` cho host đang chạy gateway một subdomain ổn định |
| Chỉ test logic (chưa đụng tiền thật) | Test Mode + quick tunnel — xem `account-prep/sepay-account-setup-runbook.md` §4.5 |

> ⚠️ **KHÔNG dùng quick tunnel (`cloudflared tunnel --url ...`) cho production.** URL quick-tunnel **đổi mỗi lần restart** → SePay webhook chết. Production PHẢI dùng **Named Tunnel** (hostname cố định).

---

## 1. Yêu cầu trước khi bắt đầu

- Domain `kitehub.me` đã thêm vào Cloudflare (zone active) — đã có per domain-setup.
- Host (EC2 hoặc máy chạy production-like) đang chạy **gateway** ở `:9000` (webhook đi qua gateway → kitehub-subscription).
- `SEPAY_API_KEY` đã tạo trên SePay dashboard (per `account-prep/sepay-account-setup-runbook.md` §3.1).
- Quyền Cloudflare account (login được `cloudflared`).

**Webhook contract (đã có sẵn trong code):**
- Endpoint: `POST /api/platform/webhooks/payment` (`PaymentWebhookController`).
- Auth: header `Authorization: Apikey <SEPAY_API_KEY>` (KHÔNG phải JWT, KHÔNG phải HMAC body-signature).
- Backend kỳ vọng trả `{"success": true}` — nếu khác, SePay đánh dấu giao hàng thất bại + retry.

---

## 2. Cài đặt cloudflared trên host

```bash
# Ubuntu/Debian (EC2 amd64)
curl -L --output cloudflared.deb \
  https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb
cloudflared --version
```

---

## 3. Tạo Named Tunnel

```bash
# 3.1 Đăng nhập Cloudflare (mở browser, chọn zone kitehub.me)
cloudflared tunnel login

# 3.2 Tạo tunnel có tên ổn định
cloudflared tunnel create kite-sepay
# → in ra Tunnel ID (UUID) + tạo credentials file:
#   ~/.cloudflared/<TUNNEL_ID>.json   (GIỮ BÍ MẬT — credential của tunnel)
```

---

## 4. Cấu hình route tunnel → gateway

Tạo `~/.cloudflared/config.yml`:

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /home/<user>/.cloudflared/<TUNNEL_ID>.json

ingress:
  # Chỉ expose path webhook (least-privilege) — mọi path khác trả 404.
  - hostname: webhook.kitehub.me
    path: ^/api/platform/webhooks/payment$
    service: http://localhost:9000
  # catch-all bắt buộc (cuối danh sách)
  - service: http_status:404
```

> Least-privilege: chỉ route đúng path webhook tới gateway. Không expose toàn bộ API qua tunnel này.

Gán DNS record (CNAME `webhook` → tunnel) qua Cloudflare:

```bash
cloudflared tunnel route dns kite-sepay webhook.kitehub.me
# Tạo CNAME webhook.kitehub.me -> <TUNNEL_ID>.cfargotunnel.com (proxied)
```

---

## 5. Chạy tunnel như service (persistent)

```bash
# Cài systemd service (đọc ~/.cloudflared/config.yml)
sudo cloudflared service install
sudo systemctl enable --now cloudflared
sudo systemctl status cloudflared    # active (running)

# Kiểm tra tunnel healthy
cloudflared tunnel info kite-sepay
```

Tunnel giờ phục vụ `https://webhook.kitehub.me/api/platform/webhooks/payment` (HTTPS do Cloudflare cấp, không cần ACM cert trên host).

---

## 6. Set SEPAY_API_KEY cho backend

Backend đọc `SEPAY_API_KEY` từ env (`kitehub.payment.sepay.api-key`). Trên production:

```bash
# Qua AWS Secrets Manager (per account-prep/sepay-account-setup-runbook.md §4):
aws secretsmanager put-secret-value \
  --secret-id kitehub/production/sepay-api-key \
  --secret-string "<sepay-api-key>"
# fetch-secrets.sh pull vào /etc/kite/.env dưới biến SEPAY_API_KEY lúc EC2 boot.

# Hoặc set trực tiếp env cho kitehub-subscription rồi restart service.
```

Restart `kitehub-subscription` để nạp key. (Nếu `SEPAY_API_KEY` rỗng, controller **reject mọi webhook** + log `SePay api-key not configured`.)

---

## 7. Cấu hình webhook trên SePay dashboard

SePay dashboard → **Webhooks** → thêm/sửa:

| Trường | Giá trị |
|---|---|
| Webhook URL | `https://webhook.kitehub.me/api/platform/webhooks/payment` |
| Method | `POST` |
| Kiểu xác thực | **API Key** → header `Authorization: Apikey <SEPAY_API_KEY>` (đúng key đã set ở §6) |
| Sự kiện | Có tiền vào (incoming transfer) |

---

## 8. Verify end-to-end

```bash
# 8.1 Tunnel reachable từ ngoài (no auth → 401, KHÔNG phải 404/timeout)
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  https://webhook.kitehub.me/api/platform/webhooks/payment \
  -H "Content-Type: application/json" -d '{}'
# Kỳ vọng: 401 (route OK, thiếu Apikey) — KHÔNG phải 404 (route sai) hay timeout (tunnel down)

# 8.2 Với Apikey đúng → 200 + {"success": true}
curl -s -X POST https://webhook.kitehub.me/api/platform/webhooks/payment \
  -H "Authorization: Apikey <SEPAY_API_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"id":"test-1","transferAmount":10000,"description":"KITE TEST"}'
# Kỳ vọng: {"success": true}
```

8.3 Dùng **nút Test webhook** trên SePay dashboard → backend log `Received SePay webhook: {...}` + trả `{"success": true}`.

8.4 Test giao dịch thật nhỏ (vd 2.000đ) → SePay gọi webhook → `paymentService.processSepayWebhook` match txnRef → subscription PAID.

---

## 9. Hardening production

- **Hostname cố định:** giữ `webhook.kitehub.me` (Named Tunnel) — không đổi → SePay không cần cấu hình lại.
- **Credential tunnel** (`<TUNNEL_ID>.json`): bí mật như secret; backup an toàn, không commit.
- **Rotate `SEPAY_API_KEY`:** đổi trên SePay dashboard + Secrets Manager đồng thời; restart subscription.
- **Restrict nguồn (tùy chọn):** Cloudflare WAF/Access cho phép chỉ IP SePay gọi `webhook.kitehub.me` (giảm bề mặt tấn công). Kiểm tra dải IP SePay trong docs của họ.
- **Idempotency:** đã có server-side (`processSepayWebhook` dùng `sepayId`) — webhook lặp không double-credit.
- **Monitoring:** alert khi `cloudflared` service down (systemd) + khi webhook reject tăng đột biến (log `SePay webhook rejected`).

---

## 10. Troubleshooting

| Triệu chứng | Nguyên nhân | Khắc phục |
|---|---|---|
| `404` ở §8.1 | Route path sai trong `config.yml` | Kiểm tra `path: ^/api/platform/webhooks/payment$` + catch-all 404 cuối danh sách |
| Timeout ở §8.1 | `cloudflared` down hoặc gateway `:9000` không chạy | `systemctl status cloudflared` + `curl localhost:9000/actuator/health` |
| `401` cả khi gửi Apikey đúng | `SEPAY_API_KEY` backend rỗng/lệch key dashboard | So khớp key §6 ↔ §7; restart subscription |
| SePay báo "delivery failed" dù 200 | Response không phải `{"success": true}` | Kiểm tra log `Failed to process SePay webhook` (payload thiếu field) |
| URL đổi sau restart | Lỡ dùng **quick tunnel** thay vì Named Tunnel | Dùng `cloudflared tunnel create` + systemd service (§3-§5) |
