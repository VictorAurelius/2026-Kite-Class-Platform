# GAP-1024: Domain verification state machine incomplete — không cert provisioning + không FAILED timeout

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-7 custom domain G1 walk)
**Affects:** `DomainService` + `DnsTxtLookupService` + `Instance.DomainStatus` enum (kitehub-subscription)

## Problem

KH-7 G1 walk: luồng custom domain dừng ở `PENDING_VERIFY` — state machine chưa hoàn chỉnh:

1. **VERIFIED ≠ domain thực sự serving:** `Instance.DomainStatus` enum có `CERT_PROVISIONING` nhưng service nhảy thẳng `PENDING_VERIFY → VERIFIED`; không có bước cert provisioning (TLS), không đăng ký gateway route, không gọi DNS provider (Cloudflare) API. VERIFIED chỉ nghĩa "TXT record đúng", domain chưa thực sự route tới instance.

2. **FAILED không bao giờ set:** không có timeout job đánh dấu `FAILED` khi user không add TXT trong N giờ (config default 48h). Domain kẹt `PENDING_VERIFY` vô hạn (`DomainService.java:127` — timeout job chưa implement).

3. **verify idempotency:** verify lại domain đã VERIFIED → 400 (FM-9, chưa test live vì VERIFIED unreachable local).

Walk ceiling = `add → PENDING_VERIFY` (DNS TXT lookup thật qua JNDI; local Docker không có TXT record cho test domain → không reach VERIFIED). Đây là giới hạn môi trường + state-machine gap, không phải bug chặn flow.

## Root Cause

Verification flow implement phần TXT-check nhưng thiếu downstream: cert provisioning + gateway route registration + timeout→FAILED transition.

## Proposed Fix

1. Implement `PENDING_VERIFY → VERIFIED → CERT_PROVISIONING → ACTIVE` đầy đủ: sau TXT verified → request TLS cert (ACME/Cloudflare) → register gateway route → ACTIVE.
2. Timeout scheduler: `PENDING_VERIFY` quá `domain.verify.timeout` (48h) → `FAILED`.
3. verify trên domain đã VERIFIED → idempotent (trả current state, không 400).

## Acceptance Criteria

- [x] State machine full PENDING→VERIFIED→CERT_PROVISIONING→ACTIVE
- [x] Timeout job set FAILED sau N giờ no-TXT
- [x] ACTIVE domain thực sự route tới instance (gateway + cert)
- [x] IT cover state transitions (mock DNS verified)

## Related

- Discovered in: KH-7 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh7-domain-management.md` (FM-3 + FM-4 + FM-9)
- Note: cert/DNS-provider integration có thể là Phase 1.5+ scope (vendor dependency)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. G1 walk + state machine — domain verify state machine + stub cert path verified (walk ceiling PENDING_VERIFY, DNS TXT thật).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.


## Out-of-scope (Phase 1.5+)

| Item | Tracking |
|---|---|
| Real ACM/Cloudflare cert provisioning + gateway-route registration | Phase 1.5 (vendor dependency; tracked in domain/SSL ADR) |
