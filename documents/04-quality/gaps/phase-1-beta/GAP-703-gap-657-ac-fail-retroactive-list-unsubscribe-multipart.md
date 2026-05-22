---
gap_id: GAP-703
title: GAP-657 AC FAIL retroactive — List-Unsubscribe + multipart/alternative MISSING in live
status: OPEN
priority: P0
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket D live verify
---

# GAP-703 — GAP-657 AC FAIL retroactive — email hardening not actually wired

## Problem

Wave 98 Bucket B1 commit `8d0e4fb9` shipped với message "feat(wave-98 B1): GAP-657+659 email layer hardening — plain-text + Tone enum + headers" + claimed GAP-657 status 80% PARTIAL. Wave 102.9 Bucket D state-check retained 80% claim.

**Wave 103 Bucket D live verify (2026-05-22) finds AC actually FAIL on 2/3 hardening requirements:**

| AC | Wave 98 claim | Wave 103 live state | Verdict |
|---|---|---|---|
| Reply-To header | ✅ wired | ✅ `support@kitehub.me` set | ✅ PASS |
| List-Unsubscribe header | ✅ wired | ❌ MISSING from all email types tested | ❌ FAIL |
| Plain-text fallback (multipart/alternative) | ✅ wired | ❌ Email Content-Type = `multipart/mixed > multipart/related > text/html` (no text/plain part); service log confirms `"textBody present: false"` and `"text-part: no"` | ❌ FAIL |

**Evidence:**
- Mailhog API inspection of 2 password-reset messages 2026-05-22 03:48-03:49 UTC
- Both messages: Content-Type = `multipart/mixed; boundary="----=_Part_...` — NO `multipart/alternative` wrapper
- Headers: 0 `List-Unsubscribe`, 0 `List-Unsubscribe-Post`
- kitehub-email service log: `"Sending email to: a***@kitehub.com, subject: Đặt lại mật khẩu - KiteHub, textBody present: false"` and `"[SMTP] Email sent ... text-part: no"`

**Impact (P0 — PDPL + Email deliverability compliance):**
- Missing List-Unsubscribe = transactional emails treated as marketing by Gmail → higher spam rate
- Missing plain-text fallback = emails fail to render on text-only mail clients + accessibility tools
- Both required for production email deliverability + GDPR/PDPL good faith compliance

## Context

- Wave 98 commit claimed hardening shipped — possibly only EmailHeadersConfig.java class added but not wired into template build path
- OR conditional on `EMAIL_PROVIDER=ses` (production) vs `smtp` (local) — needs verification

## Proposed Fix

1. **Audit** `SESEmailService.send()` template builder to find why plain-text alternative not added (likely template renderer only produces HTML part)
2. **Audit** `EmailHeadersConfig.java` — is it actually applied to outbound MimeMessage? Likely defined but never injected
3. **Wire**:
   - Add `text/plain` rendered part to multipart/alternative wrapper for every email type
   - Apply `List-Unsubscribe` + `List-Unsubscribe-Post` headers in MimeMessage builder
4. **Verify** via Wave 103 Bucket D pattern: trigger email → Mailhog inspect headers + Content-Type

## Acceptance Criteria

- [ ] Every email (5 types) sent has `Content-Type: multipart/alternative` wrapper with both text/html + text/plain parts
- [ ] Every email has `List-Unsubscribe` header set (per IETF RFC 8058)
- [ ] Every email has `List-Unsubscribe-Post` header for 1-click unsubscribe (per RFC 8058)
- [ ] Live verify: curl trigger any email → Mailhog inspect → headers + multipart structure all PASS
- [ ] GAP-657 status flip: 40% (current live state) → 100% DONE on local verify
- [ ] Service log no longer emits `"text-part: no"` or `"textBody present: false"`

## Related

- [[GAP-657]] Original gap — Wave 98 B1 claimed 80% shipped; this gap = retroactive AC FAIL
- [[GAP-543]] 5 email types VN content (depends on this for proper delivery)
- [[GAP-702]] Approval email not firing (separate but same email pipeline)
- Wave 103 audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-email-mailhog-verify.md`
- Wave 98 commit: `8d0e4fb9 feat(wave-98 B1): GAP-657+659 email layer hardening`
- Rule cross-ref: `gap-done-discipline.md` §2 — DONE flip requires AC verified; this gap = AC verified retroactively failed
