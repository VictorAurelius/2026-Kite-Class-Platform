# GAP-388: Beta security P1 cluster — honeypot logging + token plaintext + per-email rate limit

**Status:** 🔵 OPEN
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

- [ ] **388-A**: Honeypot rejection counter + log + integration test (POST với honeypot non-empty → 400 + counter incremented)
- [ ] **388-B**: Claim code 2FA flow implemented (FE form changes + BE token exchange endpoint)
- [ ] **388-C**: Per-email rate limit + Redis key + 429 response + audit log
- [ ] All 3 sub-issues unit + integration tested
- [ ] Update `documents/01-business/kitehub/beta-access/rules.md` (nếu tồn tại) với 3 BR-* entries (BR-BETA-HONEY / BR-BETA-CLAIM / BR-BETA-RATE)
- [ ] Security audit re-run delta: 72/100 → ≥80/100 (validate via spawn audit cluster)

## Related

- Source audit: `documents/04-quality/audits/security/2026-05-07-wave-33-beta-deploy.md` (Findings #3, #4, #5)
- Parent gap: GAP-372 (beta invite — Wave 33)
- Blocked-by: GAP-384 (admin auth), GAP-385 (PDPL consent) — ship P0s first
- Related observability: GAP-387 (beta metric counters)

## Log

- **2026-05-07** Filed from Security /100 audit Wave 33. State-check: 0 existing gaps cover honeypot logging / token plaintext / per-email rate-limit. Bundled into single P1 cluster gap per `audit-to-gap-pipeline.md` §3 "Group by domain/priority, max 3-5 per PR" — 3 sub-issues thematically related (beta security hardening), can ship in 1 PR after P0s land.
