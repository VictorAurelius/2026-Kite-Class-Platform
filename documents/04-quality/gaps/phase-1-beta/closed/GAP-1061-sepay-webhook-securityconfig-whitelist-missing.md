# GAP-1061: SePay webhook `/api/platform/webhooks/payment` thiếu whitelist trong subscription SecurityConfig → 401

**Status:** 🟢 DONE
**Priority:** 🔴 P1 (production-blocking — toàn bộ SePay subscription reconciliation)
**Domain:** Backend
**Found:** 2026-06-08 (SePay Test-Mode logic verify — GAP-1058 execution)
**Affects:** `kitehub-subscription` — `config/SecurityConfig.java`

## Problem

Khi verify logic GAP-975/976 qua Test Mode (local POST → `/api/platform/webhooks/payment` với `Authorization: Apikey`), mọi request trả **401 body rỗng** (`Content-Length: 0`) — tức Spring Security chặn TRƯỚC khi tới `PaymentWebhookController.verifyApiKey` (controller trả `{"error":"Invalid API key"}` có body).

Root cause: `!test` SecurityFilterChain dùng `anyRequest().authenticated()` default-deny (GAP-552). Whitelist chỉ có `/api/v1/payments/webhook` (path CŨ, khác). Controller SePay thật ở `/api/platform/webhooks/payment` — **không có matcher** → fall vào default-deny → 401.

Gateway thì ĐÃ whitelist `/api/platform/webhooks/**` (`JwtAuthenticationGatewayFilter.isPublicPath`) nhưng subscription không mirror → production: SePay → gateway (pass) → subscription (401) → payment KHÔNG BAO GIỜ reconcile → subscription upgrade trial→paid không bao giờ apply.

**Vì sao IT tests miss:** `test` profile SecurityFilterChain = `anyRequest().permitAll()` (line 72-78) → controller test luôn reachable. Classic test-vs-prod security gap.

## Fix (shipped)

`SecurityConfig.java` thêm `.requestMatchers("/api/platform/webhooks/**").permitAll()` (mirror gateway whitelist; controller tự auth bằng Apikey). 

## Acceptance Criteria

- [x] `/api/platform/webhooks/**` permitAll trong subscription `!test` chain
- [x] Live verify: POST với sai Apikey → 401 từ **controller** (`{"error":"Invalid API key"}`), không phải Spring Security body-rỗng
- [x] Live verify: POST hợp lệ → reaches controller → 200/400 theo logic (8 nhánh PASS — xem recipe)

## Related

- Surfaced by: `documents/05-guides/operations/sepay-webhook-local-verify-recipe.md` (GAP-1058 execution)
- Discovered in: verify branch `verify/sepay-975-976-test-mode-logic` 2026-06-08
- Gateway parity: `JwtAuthenticationGatewayFilter.isPublicPath` (line 259) — already whitelisted
- Test-vs-prod gap class: `pre-handoff-self-test-completeness.md` (IT pass ≠ prod auth chain works)
