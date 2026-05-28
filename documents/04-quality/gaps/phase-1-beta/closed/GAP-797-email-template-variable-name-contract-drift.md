---
audience: dev
---

# GAP-797 — Email template variable-name contract drift → beta-invite signup info missing

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend / Email
**Found:** 2026-05-28 (Wave A flow-1 RST walk — user browser walk, MailHog `documents/image-7.png`)
**Phase:** phase-1-beta
**Affects:** beta-invite email (signup-blocking) + welcome email (degraded) — `EmailServiceClient` var keys ≠ Thymeleaf template placeholders

## Problem

`EmailServiceClient.sendBetaInviteEmail()` truyền variable keys KHÔNG khớp placeholder trong template → Thymeleaf fallback về default `?:` → email thiếu thông tin signup THẬT. User walk flow 1 (anonymous signup → admin approve → email) nhận email với:
- "Mã xác minh (6 chữ số): **`------`**" (placeholder default, không có mã thật)
- "Liên kết đăng ký: **`https://kitehub.me/beta/accept`**" (default, không token/code)

→ **Flow 1 die — user không thể signup** (không có mã 6 số + link sai). Evidence: `documents/image-7.png` (MailHog HTML tab).

### Root cause — variable-name mismatch

`EmailServiceClient.java:707-712` truyền:
```java
"claimCode",  claimCode    // template beta-invite.txt đọc ${verificationCode}
"signupUrl",  signupUrl    // template đọc ${inviteUrl}
"expiresAt",  expiresAt    // beta-invite.html đọc ${expiryDate}
```

Template `beta-invite.txt`:
```
[(${verificationCode ?: '------'})]                          ← sender gửi claimCode → fallback ------
[(${inviteUrl ?: 'https://kitehub.me/beta/accept'})]         ← sender gửi signupUrl → fallback /beta/accept
```

Bonus drift: `.txt` dùng `verificationCode`/`expiresAt`, `.html` dùng `claimCode`/`expiryDate` → 2 template lệch var với nhau (inconsistent contract).

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** `EmailServiceClient.variables(Map.of(...))` keys ≠ `${...}` placeholders trong matching template → silent fallback to `?:` default.

**Grep command run:**
```bash
# Per template .txt: extract ${var} placeholders, compare vs sender Map.of keys
for tpl in templates/emails/*.txt; do
  tvars=$(grep -oE '\$\{[a-zA-Z]+' "$tpl" | sed 's/${//' | sort -u)
  skeys=$(awk '/templateName("<name>")/../.build()/ {extract "key"}' EmailServiceClient.java)
  diff
done
```

**Sites found + verdict:**

| # | Email template | Sender keys | Template needs | Verdict |
|---|---|---|---|---|
| 1 | `beta-invite.txt` | claimCode, signupUrl, expiresAt, recipientName, orgName | verificationCode, inviteUrl, expiresAt, orgName, unsubscribeUrl | **FIX** — verificationCode + inviteUrl missing → P0 signup-blocking |
| 2 | `beta-invite.html` | (same) | claimCode✅, inviteUrl, expiryDate, orgName | **FIX** — inviteUrl + expiryDate missing |
| 3 | `welcome.{txt,html}` | organizationName, loginUrl, trialDays, expiryDate | + recipientName, docsUrl, unsubscribeUrl | **FIX** — recipientName (greeting) + docsUrl missing → P2 degraded |
| 4 | `invite-staff` | (matches) | (matches) | **EXEMPT** — ✅ contract khớp |
| 5 | other 15 templates (trial/subscription/dsar/...) | per-method | — | **DEFER** — sweep .txt only; những template kia chưa walk runtime; verify khi walk tương ứng (note: nhiều dùng stale domain `kitehub.com`/`kitehub.vn`/`kiteclass.vn` → separate bug class, file riêng) |

**Decision:** FIX beta-invite (P0) + welcome (P2) this gap; DEFER 15 others (chưa walk runtime, separate sweep). `unsubscribeUrl` NOT global-injected (renderer chỉ inject `branding` via instanceId) → genuinely missing.

## Proposed Fix

Reconcile var-name contract 3-way (sender ↔ .txt ↔ .html). Canonical names (pick one set):
- code: `claimCode` (sender already sends; fix .txt `verificationCode` → `claimCode`)
- link: `inviteUrl` (template name; fix sender `signupUrl` → `inviteUrl`, value = `/signup/beta?code=<claimCode>`)
- expiry: `expiresAt` (fix .html `expiryDate` → `expiresAt`)
- welcome: sender add `recipientName` + `docsUrl`
- Add regression test: render each template với sender Map → assert no `?:` default leaks (no `------`, no bare `/beta/accept`)

## Acceptance Criteria

- [ ] beta-invite email render mã 6 số THẬT (không `------`) + link `/signup/beta?code=<code>` (không `/beta/accept` default)
- [ ] .txt + .html dùng cùng var names (consistent contract)
- [ ] welcome email render recipientName greeting + docsUrl thật
- [ ] Regression test: `EmailTemplateRendererTest` assert sender-keys ⊇ template-placeholders cho mỗi template (no fallback leak)
- [ ] RST re-walk flow 1: user nhận mã + link → signup thành công (per `pre-handoff-self-test-completeness.md` §3 post-fix re-walk)
- [ ] Sweep 15 deferred templates trong follow-up (var contract + stale domain)

## Related

- Index: `documents/04-quality/audits/rst-html/2026-05-28-full-regression/INDEX.md`; flow-1 walk `../2026-05-28-wave-a-5-flow-walk.md`
- Evidence: `documents/image-7.png` (MailHog HTML tab)
- **GAP-793** (email provider routing) — different layer (provider select); GAP-797 = template var binding
- **GAP-787** (staff-invite publisher) — different (publish vs bind); invite-staff template var ✅ here
- **GAP-657** (email-layer-hardening) / **GAP-543** (email-content-audit-5-types) / **GAP-530** (email e2e verify) — related email scope
- Sweep methodology: `cross-flow-bug-class-sweep.md` §3
- Stale-domain bug class (kitehub.com/.vn in 15 templates) → separate follow-up gap (per `no-vercel-references.md` + kitehub.me migration)

## Log

- **2026-05-28:** Filed từ Wave A flow-1 RST walk (user browser, `image-7.png`). Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 found 3 templates (beta-invite .txt+.html + welcome) cùng var-contract-drift class. State-check: not duplicate GAP-793 (provider routing) / GAP-787 (publisher) / GAP-657 (hardening). P0 vì signup-blocking (flow 1 die). Blocks GAP-787/793 email-path full verify + Wave A flow-1 PASS.
