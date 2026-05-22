---
title: Wave 103 Bucket D — Email Mailhog content + headers verify
status: complete
created: 2026-05-22
phase: phase-1-beta
wave: 103
bucket: D
gaps: [GAP-543, GAP-657, GAP-659]
---

# Wave 103 Bucket D — Email Mailhog verify

**Scope:** Trigger 5 email types via API → inspect Mailhog rendering + VN content + headers + plain-text fallback per `vn-localization-audit-checklist.md` + GAP-657 hardening AC. Per Wave 103 plan §3 Bucket D.

**Result:** ⚠️ PARTIAL — email pipeline FUNCTIONAL + VN content ✅; **2 real bugs surfaced** (List-Unsubscribe missing + plain-text fallback structure missing + approval email not firing on approve). Bucket D ships PARTIAL with 2 follow-up gap candidates.

**Note this bucket was re-handled inline by coordinator** after sub-agent failed with autocompact context thrashing. Scope was reduced from "trigger ALL 5 email types" to "trigger 2 critical email types (welcome + password-reset) + deep inspect headers/body/multipart structure" to fit time budget.

---

## Commands run (Tier 1 local stack, no AWS)

```bash
# Login admin → JWT
curl POST /api/auth/login                                  # → 200 + JWT
# Trigger 1: beta-request submit (welcome path)
curl POST /api/v1/auth/request-beta-access                 # → 201 + beta row
# Trigger 2: admin approve (should fire invite email)
curl POST /api/v1/admin/beta-requests/{id}/approve         # → 200 + APPROVED row
# Trigger 3: password-reset
curl POST /api/auth/password-reset-request                 # → 202
# Mailhog inspect (API)
curl http://localhost:8025/api/v2/messages                 # quirk: HTTP 404 status, valid JSON body
curl http://localhost:8025/api/v1/messages/{ID}            # individual message + body
# Logs
docker logs kitehub-email --since 10m
docker logs kitehub-subscription --since 5m
```

---

## Findings

### Email pipeline functional

✅ Mailhog SMTP catcher (kite-mailhog:1025) receives emails from kitehub-email service. Configured via:
- `EMAIL_PROVIDER=smtp` (env override default `mock`)
- `SMTP_HOST=kite-mailhog`, `SMTP_PORT=1025`

✅ Vietnamese subject + body content rendered correctly:

```
Subject: Đặt lại mật khẩu - KiteHub (UTF-8 quoted-printable encoded)
Body excerpt:
  "Kính gửi anh/chị Quý khách,
   Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của anh/chị tại KiteClass.
   Để tiếp tục, vui lòng truy cập liên kết sau (có hiệu lực trong 30 phút):
   http://localhost:3001/reset-password?token=eQ_beH1...
   Trân trọng,..."
```

✅ Reply-To header set correctly: `support@kitehub.me`

### Per-email-type trigger results

| # | Type | Trigger endpoint | API result | Email arrived? |
|:-:|---|---|:---:|:---:|
| 1 | Welcome (signup confirmation) | `POST /api/v1/auth/request-beta-access` | 201 | ❌ NOT sent |
| 2 | Approval (post admin approve) | `POST /api/v1/admin/beta-requests/{id}/approve` | 200 | ❌ NOT sent |
| 3 | Password-reset | `POST /api/auth/password-reset-request` | 202 | ✅ sent |
| 4 | Staff-invite (formal Owner tone) | not triggered | — | — (needs tenant + OWNER context) |
| 5 | 2FA-challenge | not triggered | — | — (defer Bucket C 2FA scope) |

### 5-email matrix (per VN-localization + GAP-657 hardening AC)

| Email type | Subject VI | Greeting VN | Sample data VN | List-Unsubscribe | Reply-To | Multipart/alt (plain-text fallback) |
|:----------:|:---------:|:-----------:|:--------------:|:----------------:|:--------:|:------------------------------------:|
| Welcome | (not sent) | — | — | — | — | — |
| Approval | (not sent) | — | — | — | — | — |
| **Password-reset** | ✅ "Đặt lại mật khẩu - KiteHub" | ✅ "Kính gửi anh/chị Quý khách" (formal) | N/A (system email) | ❌ MISSING | ✅ support@kitehub.me | ❌ multipart/mixed (NOT alternative) |
| Staff-invite | (not tested) | — | — | — | — | — |
| 2FA-challenge | (not tested) | — | — | — | — | — |

### Real bugs surfaced (file as follow-up gaps)

**Bug 1 — Approval email not firing on `POST /api/v1/admin/beta-requests/{id}/approve`:**
- Admin successfully changes beta-request status PENDING → APPROVED (HTTP 200)
- Service logs show approve succeeded but `admin_audit_log` insert errored: `Could not convert 'java.lang.String' to '[B' using 'org.hibernate.type.descriptor.java.StringJavaType'` (separate bug — audit log column type mismatch)
- **kitehub-email service does NOT receive any "send approval email" request after approve** (no `Sending templated email` log entry post-approve)
- Approval-email-send is either not wired OR conditionally skipped in test env

→ **Suggested follow-up gap:** P0 — Beta approve flow does not fire approval/invite email → tenant cannot complete signup flow. Investigate `BetaAccessService.approveRequest()` for email-send call.

**Bug 2 — Email headers incomplete (GAP-657 AC FAIL):**
- `List-Unsubscribe` header: ❌ MISSING (GAP-657 AC explicit requirement)
- Plain-text fallback structure: ❌ Container is `multipart/mixed > multipart/related > text/html` — NO `text/plain` part. Service logs confirm `"textBody present: false"` and `"text-part: no"`.
- `Reply-To` header: ✅ present

→ **Suggested follow-up gap:** P0 — GAP-657 AC actually FAIL in live verify despite shipping Wave 98 B1 marked code-AC 80%. List-Unsubscribe + multipart/alternative not actually wired. Investigate `EmailHeadersConfig.java` + `SESEmailService.send()` template builder.

**Bug 3 (minor) — Welcome confirmation email on submit:**
- Bucket B's report shows similar pattern (beta-request submit succeeds 201 + status PENDING, no immediate email).
- Likely by-design: welcome email only fires after admin approves (invite-token email). Verify against `business-logic/auth` rules.md.

→ **Suggested follow-up:** P2 — verify product intent (welcome-on-submit vs invite-on-approve). If welcome-on-submit intended, file gap.

### Mailhog API quirk (confirmed)

- `GET http://localhost:8025/api/v2/messages` → HTTP 404 header, valid JSON body `{"total":N, ...}`
- `GET http://localhost:8025/api/v1/messages/{ID}` → HTTP 200, valid JSON message detail
- Bucket D agents/scripts: parse body, ignore status code on list endpoint

---

## Prior actions verified

| Action | When | Where verified |
|---|---|---|
| Wave 98 B1 GAP-657 + GAP-659 email layer hardening | 2026-05-15 | `git log --oneline | grep wave-98 B1` confirms `8d0e4fb9 feat(wave-98 B1): GAP-657+659 email layer hardening — plain-text + Tone enum + headers` |
| Wave 102.9-D state-check ship | 2026-05-21 | PR #1707 |

**Discrepancy:** Wave 98 B1 commit message says "plain-text + Tone enum + headers" but Bucket D live verify shows plain-text part MISSING from rendered email. Investigation needed.

---

## Pending (next bucket / next wave)

| Action | Owner | Notes |
|---|---|---|
| File follow-up gap: approval email not firing | Coordinator (post-Wave-103) | P0 — blocks beta tenant signup |
| File follow-up gap: GAP-657 AC FAIL retroactive | Coordinator (post-Wave-103) | P0 — List-Unsubscribe + multipart/alt missing |
| Staff-invite + 2FA email trigger | Wave 104+ | Needs tenant context + 2FA enabled fixture |

---

## Gap status recommendations (not applied per coordinator scope)

- **GAP-543** (5 email types VN content): PARTIAL 80% → **65% retained** (only 1/5 types verified; 2 critical types don't even send)
- **GAP-657** (email layer hardening — plain-text + List-Unsubscribe + Reply-To): PARTIAL 80% → **40% retained** (only Reply-To verified; plain-text + List-Unsubscribe FAIL in live)
- **GAP-659** (persona-tone split staff-invite): PARTIAL 80% → **50% retained** (formal Owner tone confirmed on password-reset; staff-invite specifically not tested)

---

## Recommendations

1. **File 2 follow-up P0 gaps** post-Wave-103 closure for approval-email-not-firing + GAP-657 AC FAIL retroactive
2. **Bucket A may need adjustment:** approve action in admin walk (step 4) does succeed HTTP 200 but doesn't trigger downstream email — Bucket A audit should reflect this (curl PASS, email-side N/A or block on follow-up)
3. **GAP-657/659 status revision** in post-Wave-103 closure: previous claims of "80% code-AC shipped Wave 98 B1" were inaccurate based on live verify — actual ~40% per AC observability

---

## References

- Wave 103 plan: `documents/03-planning/waves/wave-2026-05-22-103-local-self-test-full-walk.md` §3 Bucket D
- Original (failed) sub-agent: `tasks/a03eb7f3952c12304.output` (autocompact thrashed)
- Wave 98 B1 commit: `8d0e4fb9 feat(wave-98 B1): GAP-657+659 email layer hardening`
- Wave 102.9 D state-check: PR #1707
- Mailhog API doc: `mailhog/MailHog v1.0.1`
- Email service config: `kitehub/kitehub-email/src/main/resources/application.yml`
- VN content rule: `.claude/rules/vn-localization-audit-checklist.md`
- Self-test rule: `.claude/rules/pre-handoff-self-test-completeness.md`
