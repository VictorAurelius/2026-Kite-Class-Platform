# GAP-1057: SePay webhook OAuth2 bearer auth hardening (alternative to static Apikey)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-08 (SePay runbook Test Mode discovery — design review)
**Affects:** `kitehub-subscription` — `PaymentWebhookController.verifyApiKey`

## Problem

`PaymentWebhookController` hiện chỉ verify webhook qua static header `Authorization: Apikey <key>` (`verifyApiKey`, constant-time compare). SePay dashboard cũng hỗ trợ phương thức xác thực mạnh hơn: **OAuth2 client_credentials** — SePay lấy bearer token rồi đính kèm vào mỗi request webhook (token xoay vòng, không lộ static key cố định trong header).

Static Apikey rủi ro: key không xoay vòng, nếu lộ (log leak / MITM) thì hợp lệ vô thời hạn cho tới khi rotate thủ công. OAuth2 bearer giảm blast radius (token TTL ngắn). Đây là sister hardening với OWASP A02 (Cryptographic Failures) + A07 (Identification & Auth) per `pre-launch-owasp-rest-hardening-checklist.md`.

Phase 1 BETA: Apikey đủ (đã code, đã test — GAP-976). OAuth2 = hardening tier cao hơn, hợp lý cho Phase 1.5 paid khi lượng giao dịch + giá trị tiền tăng.

## Proposed Fix

Implement nhánh verify OAuth2 bearer trong `PaymentWebhookController` (hoặc filter riêng): chấp nhận `Authorization: Bearer <token>` + verify token theo SePay OAuth2 introspection / JWKS (tùy SePay cung cấp). Config flag chọn auth method (`kitehub.payment.sepay.auth-method: apikey|oauth2`). Giữ Apikey làm default Phase 1 BETA; bật OAuth2 Phase 1.5.

## Acceptance Criteria

- [ ] `PaymentWebhookController` chấp nhận `Authorization: Bearer <token>` khi `auth-method=oauth2`
- [ ] Bearer token verify đúng theo cơ chế SePay (introspection hoặc signature) — reject token sai/hết hạn → 401
- [ ] Config flag `kitehub.payment.sepay.auth-method` switch apikey ↔ oauth2 không cần code đổi
- [ ] IT cover cả 2 nhánh (valid bearer → 200, invalid/expired bearer → 401)
- [ ] SePay dashboard cấu hình OAuth2 → live verify Test Mode (per GAP-1058)

## Related

- Sister: GAP-976 (webhook Apikey auth + idempotency — Phase 1 BETA path, DONE-able với Apikey)
- Standard: `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.2 A02 + §2.8 A07
- Runbook: `documents/05-guides/account-prep/sepay-account-setup-runbook.md` §3.2 (Apikey vs OAuth2 note) + §7 (Phase 1.5 hardening defer)
- Discovered in: SePay runbook Test Mode design review 2026-06-08
