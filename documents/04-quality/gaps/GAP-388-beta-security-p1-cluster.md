# GAP-388: Beta security P1 cluster — honeypot logging + token plaintext + per-email rate limit

**Status:** 🟢 DONE 2026-05-07 (Wave 36 Bucket A)
**Priority:** 🟠 P1 (3 sub-issues bundled — security hardening cho beta invite flow, ship sau P0 GAP-384/385)
**Domain:** Backend / Security
**Found:** 2026-05-07 (Security /100 audit Wave 33 — agent a24fe574)
**Affects:** `kitehub-subscription` BetaAccessController + BetaAccessService + email template

## Problem (3 sub-issues)

### 388-A: Honeypot validation rejection silent
- `BetaRequestDto.honeypot` field has `@Size(max=0)` Jakarta validation
- Spring rejects với HTTP 400 nhưng KHÔNG application-level log → metric blind cho bot detection
- Không có Micrometer counter `beta.honeypot.rejections.total`
- Operator không có visibility cho bot abuse rate

### 388-B: Token plaintext leakage in beta-invite email
- Email template (Thymeleaf) chứa `<a th:href="${inviteUrl}">` với raw UUID token in href
- Email TLS assumed (SES) nhưng PDPL không guarantee trong transit (recipient mail server có thể downgrade)
- Email forwarding/printing/screenshot leaks full token
- Compromise: attacker có quyền email recipient → có quyền claim beta slot

### 388-C: Per-email rate limit missing on `/api/v1/auth/request-beta-access`
- Gateway rate-limit per IP (3/sec replenish, 5 burst)
- KHÔNG có per-email limit
- Single IP có thể spam unique emails → DDoS DB với duplicate PENDINGs (idempotency hash giảm impact nhưng vẫn waste DB)
- Pattern: bot generate `${random}@throwaway.com` × 10000 → 10k PENDING rows pollute coordinator queue

## Proposed Fix

### 388-A
- Explicit controller check: `if (StringUtils.hasText(dto.honeypot())) { honeypotCounter.increment(); return ResponseEntity.badRequest().build(); }` BEFORE Spring validation
- Counter `beta.honeypot.rejections.total` (related GAP-387 cluster)
- Log entry với `email` + `IP` cho post-incident triage

### 388-B
- **Option 1 (preferred, 2h):** Email contains short-lived "claim code" (6-digit) instead of UUID; user enters claim code on signup page → exchanges for full UUID server-side. Two-factor: must possess email + know claim code.
- **Option 2 (5h, future):** S/MIME email encryption — heavy lift, defer.

### 388-C
- Add per-email rate limit trong `BetaAccessService.submitRequest()`:
  - Redis key `beta:request:rate:{email_hash}` TTL 24h
  - Max 1 request per email per 24h
  - Idempotent: nếu existing PENDING for email → return existing (đã có)
  - 2nd attempt within 24h on different IP → reject HTTP 429 + audit log

## Acceptance Criteria

- [x] **388-A**: Honeypot rejection counter + log + integration test (POST với honeypot non-empty → 400 + counter incremented). `BetaAccessController.handleValidationException` wires `service.recordHoneypotRejection(email, ip)`; `BetaAccessControllerTest.submitRequestRejectsHoneypot` verifies via `Mockito.verify`.
- [x] **388-B**: Claim code 2FA flow implemented — V33 migration adds `claim_code` column (6-digit, unique partial index); `BetaAccessService.approveRequest` generates code + emits via outbox payload (not raw URL); new endpoint `POST /api/v1/auth/beta-signup/exchange-claim-code` returns invite_token + pre-fill; Thymeleaf template `beta-invite.html` shows 6-digit code instead of `inviteToken` (regression guard in `BetaEmailTemplateRenderTest`); claim_code cleared on `completeBetaSignup`.
- [x] **388-C**: Per-email rate limit (Caffeine in-memory; Redis multi-pod migration tracked as follow-up — mirrors GAP-132 pattern in same module) + 429 response (`BetaRateLimitExceededException` → `handleRateLimit` → ProblemDetail with `errorCode: BETA_EMAIL_RATE_LIMIT`) + audit log capturing first IP vs attempt IP. `beta_rate_limit_rejections_total` counter exposed.
- [x] All 3 sub-issues unit tested — 7 new tests added: 1× honeypot audit-context, 4× claim-code (approve emits + clears, exchange happy / wrong-code / expired), 2× rate-limit (rejects different IP / allows same IP).

## Out-of-scope (track separately)

| Item | Where |
| `documents/01-business/kitehub/beta-access/rules.md` BR entries (BR-BETA-HONEY / BR-BETA-CLAIM / BR-BETA-RATE) | Bucket C of Wave 36 owns the `beta-access/rules.md` bootstrap (via BR-LIFE/QUALITY 5-attribute compliance blocks). The 3 BR entries land in that PR — same wave, different bucket per `audit-to-gap-pipeline.md` §3 dependency split. Service-class javadoc + this gap §Proposed Fix carry the rule semantics in the interim. |
| Security audit re-run delta 72→≥80 | Tracked in Wave 36 §7 Closure Protocol (post-bucket aggregate audit per `post-wave-audit-mandate.md` §2.1). Not a per-bucket AC — it measures cluster impact. |
| Redis migration for multi-pod rate-limit coherence | Caffeine in-memory mirrors existing `CacheConfig` pattern (single-pod Phase 1 BETA acceptable). Multi-pod migration parallels GAP-132 — track as new gap when scaling beyond single replica. |

## Related

- Source audit: `documents/04-quality/audits/security/2026-05-07-wave-33-beta-deploy.md` (Findings #3, #4, #5)
- Parent gap: GAP-372 (beta invite — Wave 33)
- Blocked-by: GAP-384 (admin auth), GAP-385 (PDPL consent) — ship P0s first
- Related observability: GAP-387 (beta metric counters)

## Log

- **2026-05-07** Filed from Security /100 audit Wave 33. State-check: 0 existing gaps cover honeypot logging / token plaintext / per-email rate-limit. Bundled into single P1 cluster gap per `audit-to-gap-pipeline.md` §3 "Group by domain/priority, max 3-5 per PR" — 3 sub-issues thematically related (beta security hardening), can ship in 1 PR after P0s land.
- **2026-05-07** Wave 36 Bucket A shipped. 388-A: controller `@ExceptionHandler(MethodArgumentNotValidException)` wires honeypot detection → `service.recordHoneypotRejection(email, ip)` (closes GAP-387 dead-wire). 388-B: 6-digit `claim_code` column added (V33 migration) + `BetaAccessService.exchangeClaimCode` + new endpoint `POST /api/v1/auth/beta-signup/exchange-claim-code`; Thymeleaf template rewritten to display claim code instead of UUID; outbox payload now carries `claimCode`. 388-C: per-email rate-limit via Caffeine in-memory cache (24h window) + `BetaRateLimitExceededException` → HTTP 429 + `beta_rate_limit_rejections_total` counter + audit log. 7 new tests (1× controller honeypot wire-up regression guard + 6× service-layer claim-code + rate-limit). Verification artifact: this PR's `mvn -pl kitehub-subscription verify -P strict-warnings` BUILD SUCCESS. Per `gap-done-discipline.md` §3 PARTIAL exit ramp NOT triggered: BR docs + audit re-run moved to §Out-of-scope with explicit follow-up owners (Bucket C and Wave 36 closure protocol respectively), neither is a deferred AC of this PR.
