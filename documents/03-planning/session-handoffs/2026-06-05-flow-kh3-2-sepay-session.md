---
title: Session handoff — Wave flow-kh3-2 SePay + G2/G3 wiring
audience: dev
created: 2026-06-05
scope: Bàn giao cuối phiên cho luồng KH-3 (subscription SePay integration)
---

# Session handoff 2026-06-05 — KH-3 SePay

## Đã ship (3 PR merged vào main @ 9f530526)

- **PR #2165** `feat(wave-flow-kh3-2)` — SePay integration Buckets A/B/C/D (làm **inline** vì agent song song dính rate-limit):
  - **A (GAP-975)**: `Payment.txnRef` + V63 migration (partial UNIQUE) + gen `KH3SUB<8hex>` + beta-amount override 10.000đ + `findByTxnRef` exact-match.
  - **B (GAP-976)**: rewrite `PaymentWebhookController` HMAC→`Authorization: Apikey` + SePay payload adapter + `processSepayWebhook` idempotency + V64 unique index trên `transaction_id`.
  - **C (GAP-974)**: `subscription-activated.html` + `EmailServiceClient.sendSubscriptionActivatedEmail` + wire `applyPendingUpgrade` nhánh upgrade (trước im lặng).
  - **D (GAP-977)**: `BetaModeBanner` env-gated + wire payment page; auto-detect dùng `usePayment` polling sẵn có (WS push defer — chưa có BE STOMP broker).
  - **Fix kèm**: `shouldCreatePendingSubscriptionForPaidTier` stub `existsById` thay `findById` — stale mock do #2160 đổi `createSubscription` findById→existsById mà không sweep test caller (api-contract-change-caller-sweep miss).
- **PR #2166** `docs(wave-flow-kh3-3)` — cập nhật G2 recipe `documents/05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md` sang luồng SePay thật (giữ mock-mode làm fallback).
- **PR #2167** `feat(wave-flow-kh3-3)` — G3 production wiring: gateway route `platform-webhooks` (`/api/platform/webhooks/**`) + whitelist trong `JwtAuthenticationGatewayFilter.isPublicPath()` + `secrets.tf` sepay-api-key + `fetch-secrets.sh` + compose env + env-vars-registry.

## Trạng thái gaps (PARTIAL — chờ walk)

GAP-974/975/976/977 = **🟡 PARTIAL** (code + unit test xong; **Bucket E** real-SePay walk chưa chạy). KH-3 campaign vẫn `🔄 walk-pass-pending-human`.

## Còn lại để KH-3 ✅ THÔNG

1. **Bucket E — G1 re-walk thật** (cần dev): SePay Free account + ngrok tunnel + chuyển khoản VCB 10k memo `KH3SUB<id>` → verify webhook→Payment COMPLETED→email. Theo G2 recipe (#2166).
2. **G2** — dev tự test local theo recipe.
3. **G3 AWS apply** — `terraform apply` cho sepay-api-key secret + gateway deploy — **gated GAP-612** (AWS account 906286017800 suspended).
4. Sau walk PASS → flip GAP-974/975/976/977 → DONE (per `gap-done-discipline` + `feature-ship-runtime-walk-mandate`).
5. GAP-944 (cross-module cache invalidation qua RabbitMQ, P2) vẫn OPEN — không chặn flow, defer.

## Lưu ý môi trường

- **Self-hosted CI runner OFFLINE** (`kite-dev-wsl-runner`, `NguyenVanKiet-runner-2`) → 2 workflow `Quality — Code` + `Quality — Database` kẹt queue cho MỌI PR. 3 PR trên merge qua **local-parity verify** (migration replay PASS + GitHub-hosted Test/Docker/FE PASS) per `ci-queue-local-runner-threshold`. **Bật lại runner** để CI self-hosted chạy: `sudo systemctl restart actions.runner.VictorAurelius-2026-Kite-Class-Platform.kite-dev-wsl-runner.service` (trên máy `nguyenvankiet`).
