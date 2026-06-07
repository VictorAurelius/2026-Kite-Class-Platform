# Session Handoff 2026-06-08 — Next: chi tiết cấu hình SePay

**Ngày:** 2026-06-08
**Next-session focus (user-directed):** Chi tiết cấu hình SePay.

---

## 1. Điểm bắt đầu next session — SePay

Code + IaC + runbook **đã sẵn** (ship phiên này, PR #2254). Next session đi sâu vào *cấu hình chi tiết* — chọn 1 hoặc cả 2 hướng:

### Hướng A — Walk code config SePay chi tiết (local-doable)
Đọc + verify từng tầng config:
- `kitehub/kitehub-subscription/.../controller/PaymentWebhookController.java` — endpoint `POST /api/platform/webhooks/payment`
- `kitehub/kitehub-subscription/.../service/PaymentService.java:291` — `processSepayWebhook(sepayId, transferAmountVnd, description)`
- `application.yml:214-225` — `kitehub.payment.sepay.api-key` (`SEPAY_API_KEY`) + `webhook-path`
- **txnRef** (GAP-975): format `KH3SUB<8 hex>`, regex `KH3SUB[A-F0-9]{8}`, `Payment.txnRef` + V63 migration + `findByTxnRef` exact-match
- **beta-mode** (GAP-975): `beta-mode.enabled` + `override-amount-vnd` (default 10000) — test rẻ
- **idempotency** (GAP-976): Apikey auth (constant-time `MessageDigest.isEqual`) + SePay payload adapter + replay guard (V64 partial UNIQUE index `payments.transaction_id`)
- Sad paths: 401 (sai Apikey) / 400 (orphan txnRef) / 200 (success/ignored/replay)

→ Có thể chạy local IT verify (Testcontainers) cho webhook flow nếu cần (GAP-975/976 đều PARTIAL 85%, residual = live verify vì `SEPAY_API_KEY` trống local).

### Hướng B — Cấu hình live (real-user action + wire)
Theo `documents/05-guides/account-prep/sepay-account-setup-runbook.md`:
1. (user) Đăng ký SePay merchant + liên kết bank (KYC) → lấy API key
2. (user) Cấu hình webhook URL `https://kitehub.me/api/platform/webhooks/payment` + chọn Apikey auth
3. (deferred AWS restore GAP-612) `terraform apply` tạo placeholder → set secret thật `kitehub/production/sepay-api-key` qua console
4. (Claude) wire + smoke test khi có key

---

## 2. Trạng thái SePay hiện tại

| Tầng | Trạng thái |
|---|---|
| Webhook controller + service | ✅ shipped (Wave flow-kh3-2) |
| Config keys (app.yml) | ✅ `SEPAY_API_KEY` + `SEPAY_WEBHOOK_PATH` |
| Terraform secret | ✅ `aws_secretsmanager_secret.sepay_api_key` (secrets.tf) |
| fetch-secrets.sh | ✅ pull `SEPAY_API_KEY` |
| Rotation | ✅ `manual_only` list (secrets-rotation.tf) |
| Runbook đăng ký | ✅ `account-prep/sepay-account-setup-runbook.md` (PR #2254) |
| **Còn lại** | API key thật (vendor KYC) + secret populate (AWS restore GAP-612) |

**Gap liên quan:** GAP-975 (VietQR txnRef + beta-amount, PARTIAL 85%) · GAP-976 (webhook Apikey + idempotency, PARTIAL 85%). Cả 2 code DONE, residual = live verify vendor.

---

## 3. Phiên 2026-06-08 đã ship (6 PR merged)

| PR | Nội dung |
|---|---|
| #2252 | Wave p0-local-1 — DB integrity sweep (GAP-877 PARTIAL 25% + GAP-885 PARTIAL 90% + GAP-1056 filed) |
| #2253 | Cleanup 4 unused imports |
| #2254 | Vendor production-prep — SePay + Zalo OA + Resend (IaC secret + fetch + rotation + SePay runbook) |
| #2255 | Concept-coverage — audits/waves README + runbook/skill/rule templates (đóng 2 docs-folder-structure violation) |
| #2256 | CI per-job path scoping (dorny/paths-filter) — quality-docs.yml chỉ chạy job liên quan |

**Insight:** P0 local-verifiable code-work gần cạn; tiến tiếp cần AWS restore (GAP-612) + vendor key (SePay/Zalo) + email warm-up (Resend GAP-533).

**Follow-up khả dĩ:** extend dorny/paths-filter sang `quality-code.yml` + `quality-rules-skills.yml` (2 workflow lãng phí nhất tiếp theo).

---

## 4. Sync state (per post-merge-sync-completeness.md)
- ✅ gap-status.csv (877/885/1056 synced #2252)
- ✅ wave-history.jsonl (p0-local-1 #2252)
- ✅ MEMORY (project-p0-priority-over-g2 updated #2252 session)
- ✅ session-handoff (file này)
- ROADMAP: waves synced per-PR; PR #2254-2256 = config/docs/CI (không ROADMAP-worthy)
