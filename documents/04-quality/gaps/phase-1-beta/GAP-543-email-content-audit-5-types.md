# GAP-543: Email content audit — 5 email types content/tone Vietnamese

**Status:** 🟡 PARTIAL 95% (Wave 98 B1 deliverability + Wave email-content-vn-audit content/tone fix — 5 critical types MailHog-verified VN-clean; remaining 5% = HTML render verify ≥2 email clients GAP-543.3 + live AWS send smoke GAP-527, both env-blocked)
**Priority:** 🔴 P0
**Domain:** Mixed (Content + Backend templates)
**Detected:** 2026-05-14
**Related PRs:** (Wave 78 plan PR pending)
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| 5 email template files (welcome / approve-tenant / reset-password / beta-invite / day-7-survey) | `kitehub/kitehub-email/src/main/resources/templates/` | ⚠️ partial — some templates exist (welcome, approval); others may be missing (beta-invite, day-7-survey) |
| Vietnamese tone consistency | template body content | ⚠️ unaudited — likely mixed VN/EN, machine-translated phrasing |
| Content audit doc | `documents/01-business/email/templates/` | ❌ missing (folder may not exist) |
| Subject line PII leak check | template subject fields | ⚠️ unaudited |
| Email render preview (HTML + plain-text fallback) | template `.html` + `.txt` pairs | ⚠️ unaudited |

**Grep commands run:**
```bash
ls kitehub/kitehub-email/src/main/resources/templates/ 2>&1
find kitehub/kitehub-email/src/main/resources -name "*.html" -o -name "*.txt" 2>&1
ls documents/01-business/email/ 2>&1
```

## Problem

Phase 1 BETA email customer-facing (5 critical types) cần audit content/tone tiếng Việt. Risk hiện tại: (1) machine-translation awkwardness; (2) subject line PII leak (e.g., user name trong subject làm spam filter trigger); (3) inconsistent tone giữa templates (welcome friendly vs approve-tenant formal vs reset-password technical); (4) HTML render broken trên email client (Gmail / Outlook); (5) plain-text fallback missing.

## Context

User confirm 2026-05-14: "Email content audit vào Wave 78" (1 trong 3 inside-out additions). Inside-out completeness audit Wave 77 surface email send foundation đóng (Bucket E SEND) nhưng content quality chưa audit. 5 email types critical cho Phase 1 BETA flow.

## Evidence

- User confirm 2026-05-14 inside-out audit
- Wave 77 SEND foundation đóng delivery (transport layer) nhưng content layer chưa audit
- Vietnamese-speaking beta tenants — email tone awkward = trust loss

## Proposed Fix

1. Create folder `documents/01-business/email/templates/` với audit notes mỗi template:
   - `welcome-audit.md` — review welcome email (first impression sau onboarding step 1)
   - `approve-tenant-audit.md` — review tenant approval email (admin approves request → notify Owner)
   - `reset-password-audit.md` — review password reset email
   - `beta-invite-audit.md` — review beta invite email (entry point Phase 1 BETA)
   - `day-7-survey-audit.md` — review day-7 survey email (sync với GAP-542)
2. Mỗi audit note covers:
   - **Tone**: friendly/formal/technical phù hợp context không
   - **Subject line**: ≤50 char + zero PII leak + Vietnamese natural
   - **Body content**: Vietnamese tone correct + no machine-translation awkwardness
   - **CTA button**: text Vietnamese + URL correct
   - **Footer**: support@kitehub.me + /beta-status link (sync GAP-540 + GAP-539)
   - **HTML render check**: render preview qua Email-on-Acid OR Litmus (free tier OK)
   - **Plain-text fallback**: `.txt` paired với `.html` (per RFC 2049 best practice)
3. Fix issues surfaced trong audit (edit template files trong cùng PR)
4. Email send smoke test (sync với GAP-527 E2E smoke) — verify rendered output match audited content
5. Reviewer checklist line trong PR template (per `output-review-mandate.md` §6.2 row "Email templates")

## Acceptance Criteria

- [x] `documents/01-business/kitehub/email/templates/` folder created với 5 audit notes + README
- [x] Mỗi audit note covers 7 dimensions (tone / subject / body / CTA / footer / HTML render / plain-text fallback)
- [x] Subject line ≤50 char + Vietnamese natural + zero PII (welcome + beta-invite PASS — 0 PII leak)
- [x] Audit note Vietnamese narrative (per `dev-readable-doc-language.md` §2)
- [ ] All 5 email types ship với content fix — defer (3 templates missing → GAP-543.1 follow-up Wave 79)
- [x] Plain-text `.txt` fallback cho mỗi `.html` — Wave 98 B1 shipped 5/5 critical (welcome / beta-invite / email-verification / password-reset / invite-staff per PR #1553)
- [ ] Footer support@kitehub.me + /beta-status link — defer (content rewrite wave riêng, sync GAP-539/540)
- [x] Content/tone fix 5 critical types (welcome / beta-invite / email-verification / password-reset / invite-staff) — MailHog-verified VN-clean (Wave email-content-vn-audit): 0 English residue + 0 wrong-domain support email + diacritics intact + variables substituted + multipart HTML+text both present
- [ ] HTML render verify ≥2 email clients — defer (GAP-543.3 follow-up; Email-on-Acid/Litmus env-blocked locally)
- [ ] Live email send smoke test — defer (sync GAP-527 Plan 1 invite; AWS SES blocked)
- [ ] Email i18n vi/en fallback — defer Wave 79+ (Phase 1 BETA Vietnamese-first, low priority)

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket E
- Sister gap GAP-527 (kitehub-email actuator + E2E smoke — same Bucket E)
- GAP-531 (tenant init handoff post-approve — references approve-tenant email)
- GAP-541 (customer-facing VN i18n — overlap on approval email subject)
- GAP-540 (support channel — footer support@ link)
- GAP-539 (beta disclaimer — /beta-status link)
- GAP-542 (feedback channel — day-7-survey email scheduler)
- Rules: `dev-readable-doc-language.md` v1.0.1 (§2 customer-facing scope); `pre-handoff-self-test-completeness.md` §2.3 (email-driven flow live verify); `output-review-mandate.md` §3 row "Email templates"
- User confirm 2026-05-14 inside-out audit

## Log

- **2026-06-02 (Wave email-content-vn-audit — PARTIAL 85% → 95%):** Content/tone fix shipped for 5 critical email types + MailHog live verify. State-check found 3 content/tone bug classes affecting the 5 types:
  1. **English residue** `All rights reserved` in `beta-invite.html` (footer) + 4 variant siblings (`welcome.formal/informal`, `invite-staff.formal/informal`) → fixed to `Bảo lưu mọi quyền`.
  2. **Wrong-domain support contact** `support@kiteclass.com` template fallback in `beta-invite.html` + the runtime-injected `TenantBranding.defaultBranding()` Java default (`contactEmail="support@kiteclass.com"`) → both fixed to `support@kitehub.me`. This was the source-of-truth bug: branding object injected at render time overrode template defaults, so all 5 types showed an unreachable support address in their footer even after template-level fixes.
  3. **Brand-default inconsistency** `KiteClass` defaults in `beta-invite.html` vs `KiteHub` in other 4 + `kitehub.me` footers same file → aligned to `KiteHub` (template default; runtime `branding.displayName` still injects the dual-brand value per deferred decision). Also fixed `giờ giờ` double-word typo in beta-invite HTML + txt.
  - **Verify:** `./mvnw -pl kitehub-email verify -P strict-warnings` BUILD SUCCESS (88 tests, 0 fail). Rebuilt kitehub-email image + restarted container. Triggered all 5 types via `POST /api/platform/emails/send` → MailHog capture. Per-type verification (post-fix): 0 English residue (HTML+text), 0 `kiteclass.com`, `support@kitehub.me` present, VN diacritics intact, no raw `${var}` leak, multipart HTML+text both present. ALL 5 PASS.
  - **Cross-flow sweep:** same `All rights reserved` + `support@kiteclass.com` bug class also present in non-critical templates (beta-request-confirmation, subscription-created, trial-expiration-warning, + ~20 others with `&copy; ... All rights reserved`). DEFER to follow-up gap GAP-543.4 (out of GAP-543's 5-critical-type scope). `AWS_SES_FROM_EMAIL=noreply@kiteclass.com` env (sender domain) = infra/env config, out of template-content scope.
  - Files: `beta-invite.{html,txt}`, `invite-staff.html`, `welcome.formal.html`, `welcome.informal.html`, `invite-staff.formal.html`, `invite-staff.informal.html`, `TenantBranding.java`. CSV `completion_pct` 85 → 95.
- **2026-06-01 (Wave email-finalize-1 Bucket B AC tick refresh):** Plain-text fallback AC ticked retroactively — Wave 98 B1 (PR #1553 2026-05-18) already shipped 5/5 `.txt` siblings at `kitehub/kitehub-email/src/main/resources/templates/emails/` (verified `find ... -name "*.txt"`). Gap Log 2026-05-18 documented evidence but checkbox state never updated until now. CSV `completion_pct` 80 → 85.
- **2026-05-21 (Wave 102.9 Bucket D fix-time state-check):** Per `audit-to-gap-pipeline.md` §2.8 verified Wave 98 B1 work intact — audit notes folder + 7-dimension notes + subject line PII check all shipped. Remaining AC (content fix + 3-template create + footer + 2-client render + live smoke + i18n) all deferred per existing follow-up gaps (GAP-543.1/2/3 Wave 79+ + Mailhog/AWS-blocked). Status PARTIAL 80% retained — no progress this wave; Bucket D scope reality-mismatched. State-check artifact: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.9-bucket-d-email-content-headers-state-check.md`. Sister to A+B+C state-check pattern.
- 2026-05-14 — Initial write-up (state-check completed; 5 template files partial; audit notes folder absent; Wave 78 Bucket E owner).
- **2026-05-14 (Wave 78 Bucket E):** PARTIAL — 5 audit notes ship: welcome PASS (8/10 tone, plain-text gap), beta-invite PASS (9/10 tone, security claim code OK), approve-tenant intentional consolidate với beta-invite (Option A), reset-password MISSING (P1 follow-up GAP-543.1.A), day-7-survey MISSING (sync GAP-542). Aggregate: 0 PII leak subject (an toàn); plain-text fallback 0/2 (gap); cross-client render 0/2 (gap). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — 3 template tạo mới + content rewrite + plain-text = wave riêng (Wave 79+).
- **2026-05-18 (Wave 98 B1 — PARTIAL 40% → 80%):** Deliverability + tone portions DONE via paired gaps GAP-657 + GAP-659:
  - 5/5 critical templates now have `.txt` plain-text siblings (welcome, beta-invite, email-verification, password-reset, invite-staff). `password-reset.html` newly created (was missing).
  - `Tone` enum + `EmailTemplateRenderer` central renderer wired for FORMAL_SAFE_DEFAULT salutation; per-tone variants Wave 99.
  - `SESEmailService.sendEmail(to, subject, html, text)` overload + ResendEmailService stub wire multipart/alternative + Reply-To + List-Unsubscribe headers per BR-EMAIL-002/003 (`documents/01-business/kitehub/email/rules.md`).
  - Business docs created: `documents/01-business/kitehub/email/{rules.md,api-contract.md}` (3-layer docs first 2 layers).
  - Tests: 5 new `EmailTemplateRendererTest` cases PASS via `mvnw verify -P strict-warnings`.
  - Remaining 20% (per-tone template variants + native VN copywriter pass) tracked Wave 99.

- **2026-05-18 (PR #1553 merged — Wave 98 B1 paired close)** — GAP-657 + GAP-659 sister gaps closed deliverability + content portions. GAP-543 status PARTIAL 40 → 80% (only manual 2-client render verify + per-tone variants Wave 99 remaining). Sync per `post-merge-sync-completeness.md` §4.
