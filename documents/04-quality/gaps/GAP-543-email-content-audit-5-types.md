# GAP-543: Email content audit — 5 email types content/tone Vietnamese

**Status:** 🟡 PARTIAL (Wave 78 Bucket E — 5 audit notes shipped; 3 templates MISSING tracked follow-up; plain-text fallback + content rewrite defer Wave 79)
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
- [ ] Plain-text `.txt` fallback cho mỗi `.html` — defer (GAP-543.2 follow-up Wave 79)
- [ ] Footer support@kitehub.me + /beta-status link — defer (content rewrite wave riêng, sync GAP-539/540)
- [ ] HTML render verify ≥2 email clients — defer (GAP-543.3 follow-up Wave 79)
- [ ] Live email send smoke test — defer (sync GAP-527 Plan 1 invite)
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

- 2026-05-14 — Initial write-up (state-check completed; 5 template files partial; audit notes folder absent; Wave 78 Bucket E owner).
- **2026-05-14 (Wave 78 Bucket E):** PARTIAL — 5 audit notes ship: welcome PASS (8/10 tone, plain-text gap), beta-invite PASS (9/10 tone, security claim code OK), approve-tenant intentional consolidate với beta-invite (Option A), reset-password MISSING (P1 follow-up GAP-543.1.A), day-7-survey MISSING (sync GAP-542). Aggregate: 0 PII leak subject (an toàn); plain-text fallback 0/2 (gap); cross-client render 0/2 (gap). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — 3 template tạo mới + content rewrite + plain-text = wave riêng (Wave 79+).
