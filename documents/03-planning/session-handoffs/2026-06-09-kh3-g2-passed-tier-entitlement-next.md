---
audience: dev
---

# Session Handoff — 2026-06-09 — KH-3 G2 PASS + Bug E/F/D + next: tier entitlement UI

**Branch:** `main` (3 PR merged session này) — clean, synced.

---

## 1. Đã làm xong session này

### 1a. Bug E/F/D (KH-3 SePay walk) — PR #2275 MERGED (`24d20854`)
- **Bug E (GAP-1085 DONE):** `SubscriptionEventEmitter.emit()` stamp `dispatched_at` khi fast-path delivered → dispatcher không republish → email gửi đúng 1 lần (was 2). Unit test 11/11.
- **Bug F (GAP-1086 DONE):** subject EN→VN `Gói đăng ký đã kích hoạt`; tạo `subscription-created.txt` + `subscription-activated.txt`; `dashboardUrl/supportUrl` kitehub.vn→kitehub.me; info-box html khớp biến sender.
- **Bug D (GAP-1087 DONE):** contract `QR memo == paymentContent == txnRef == KH3SUB<8hex>` across 3 prod path (PaymentService.createPayment + SubscriptionRenewalService.createRenewalPayment + createPendingPayment). Test sweep green (Payment 11/11 + Subscription 13/13 + Renewal 10/10). **Lưu ý:** SePay walk trước để lại 3 stale test (api-contract-change-caller-sweep miss) — đã fix luôn trong gap này.

### 1b. KH-3 G2 FULL PASS — PR #2276 MERGED (`cb952351`)
- Real-transfer SePay walk: login test-8 :3001 → upgrade → QR MB 10k → chuyển khoản thật → SePay webhook auto-confirm → PREMIUM ACTIVE + activation email. Bug E/F/D verified live (MailHog total=1, subject VN, multipart, kitehub.me).
- Campaign §4 KH-3: `🔄 walk-pass-pending-human` → **✅ G1+G2 PASS — chờ G3 production parity (AWS)**.

### 1c. GAP-1089 filed — PR #2277 MERGED (`aebf10db`)
- Discovery từ câu hỏi entitlement: **tier entitlement KHÔNG enforce cho core product** — `PricingTier` enum định nghĩa maxStudents/maxTeachers/storageLimitMB nhưng kiteclass-core **0 reference subscription tier** (no cross-service propagation) → TRIAL vs PREMIUM core capability giống hệt. OPEN P1 phase-1.5-paid.

---

## 2. Việc CHƯA làm — pickup session sau (USER DIRECTIVE)

> **User 2026-06-09:** "session sau tôi muốn **fix hết gap để thể hiện rõ sự khác biệt của các gói**. Đặc biệt **thể hiện trên UI** — ví dụ tôi đang nâng gói rồi mà **AI branding và thanh thời gian vẫn hiển thị trial**. => **state-check trước.**"

### 2a. 🔴 FINDING MỚI cần state-check TRƯỚC (user-flagged, chưa file gap)
**Sau khi nâng gói PREMIUM, UI vẫn hiển thị trial:** (a) section AI branding hiển thị trạng thái trial; (b) "thanh thời gian" (trial countdown bar) vẫn hiện dù đã PREMIUM/ACTIVE.
- **State-check trước (per `design-first-investigation-order` + `audit-to-gap-pipeline` §2.5):** verify FE code `kitehub-frontend` (billing/dashboard) đọc tier/status từ đâu — có thể (1) stale FE cache/state sau upgrade, (2) FE đọc `instance.tier` (FREE — out of sync) thay vì `subscription.tier` (PREMIUM), (3) trial-countdown component không check subscription ACTIVE. KHÔNG assume — grep FE thật trước khi file gap.
- Nhắc: re-walk này tôi reset test-8 nhiều lần → `instance.tier=FREE` nhưng `subscription.tier=PREMIUM` (out-of-sync observed). FE có thể đọc nhầm field → đúng cái user thấy. Đây là manh mối state-check.

### 2b. Tier entitlement gaps (fix để gói "có teeth")
- **GAP-1089** (P1, phase-1.5-paid): cross-service tier propagation (gateway resolve tier server-side → kiteclass-core enforce caps) + entitlement matrix canonical trong `subscription-billing/rules.md`.
- **GAP-1020** (P1, phase-1-beta): branding `X-Subscription-Tier` client-spoof + RLS GUC không set → fix tier server-side resolve. **Liên quan trực tiếp 2a** (branding tier display/enforce).
- **GAP-260** (P2, phase-1.5): gateway tier-multiplier enforcement.
- **GAP-1078** (P2, phase-2): AI provider tier→model routing (FREE hit OpenAI thay Ollama).
- **GAP-974** (P1): activated-email emission cho upgrade-flow (khác Bug F content).
- **GAP-1088** (P2): email domain drift 10 site + 16 template thiếu .txt + branding emitter double-publish sweep.

### 2c. KH-3 G3 (sau cùng)
G3 production parity = AWS EC2 (đang stopped) + domain thật `api.kitehub.me` (CF→ALB→EC2, KHÔNG tunnel) + SePay webhook prod URL + real SES + idempotency dưới tải. cloudflared tunnel CHỈ là giải pháp G2.

---

## 3. State ephemeral (lưu ý)
- **cloudflared tunnel** `https://prepaid-training-protecting-chicago.trycloudflare.com` (→ :9000) chạy bg session này — **CHẾT khi session kết thúc**. Dựng lại: `~/.local/bin/cloudflared tunnel --url http://localhost:9000`.
- **kitehub-subscription** rebuilt với `PAYMENT_MOCK_MODE=false` + real MB env (BIN 970422 / 0988269432 / NGUYEN THUY DUONG) + beta 10k. Env này shell-only (không commit) — rebuild lại revert default. Nếu muốn về dev bình thường: rebuild không env (về mock-mode).
- **tenant test-8** giờ PREMIUM/ACTIVE (kết quả G2 thật) — `instance.tier=FREE` vs `subscription.tier=PREMIUM` out-of-sync (manh mối 2a). Reset: `DELETE payments/subscriptions WHERE instance_id='7862ab7e-a960-41db-b6d7-706ac9a544fa'; UPDATE instances SET tier='FREE',status='TRIAL',subscription_id=NULL` + clear email_sent_log nếu test email.
- Owner test-8: `g2test-an-8@example.com` / `WalkKh3@2026`.
- Local Docker stack: 13 container healthy.
