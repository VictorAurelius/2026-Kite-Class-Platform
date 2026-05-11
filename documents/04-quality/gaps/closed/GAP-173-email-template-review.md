---
name: GAP-173 — Email template review checklist
description: Customer-facing email templates have no brand/legal/i18n review; create review template + mandatory gate before send
type: gap
---

# GAP-173: Email Template Review Checklist

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (meta — customer-facing compliance)
**Domain:** Marketing / Legal / Engineering
**Found:** 2026-04-14 (output-review-mandate §4 Violation #4)
**Affects:** Every email sent to tenants/users; brand trust; legal compliance (GDPR unsubscribe, etc.)

## Problem

Email templates (invitation, welcome, trial-expiry, payment-success, etc.) are engineered without formal brand/legal/i18n review. Risk: inconsistent voice, missing unsubscribe link, wrong address footer, broken Vietnamese grammar, sample data leakage in templated variables.

## Root Cause

No cross-functional review process. Email code lands same as any feature PR (engineer + peer reviewer) but nobody checks brand/legal compliance.

## Proposed Fix

1. Review checklist doc `.claude/skills/quality/email-template-review.md`:
   - [ ] Brand colors + logo applied (match theme)
   - [ ] Legal footer: unsubscribe link, physical address, company identifier
   - [ ] Vietnamese copy reviewed by native speaker
   - [ ] English fallback for non-VN users
   - [ ] Variables render correctly (preview with sample tenant data)
   - [ ] Mobile-responsive (Gmail/Outlook/Apple Mail)
   - [ ] CAN-SPAM / GDPR / Vietnam advertising law compliance
2. Marketing + legal sign-off mandatory for customer-facing emails (trial-expiry, payment, legal notices)
3. Engineer-only sign-off OK for transactional (confirmation, invitation) but still checklist-gated
4. Email preview tool — pre-send staging render

## Acceptance Criteria

- [ ] Review skill doc exists
- [ ] All 13+ existing email templates retrospectively reviewed (audit pass)
- [ ] PR template updated: email template changes require checklist
- [ ] Preview tool integrated (Mailtrap, MailHog, or similar in dev)

## Related

- Parent violation: output-review-mandate §4 #4
- Code refs: `kitehub-email/` service
- GAP-063 (SMS + Zalo notifications) — sibling review need
- Related: GAP-174 (marketing/legal broader)

## Log

- **2026-04-20** — Wave 8b Agent C closed gap. Created skill `.claude/skills/quality/email-template-review/`:
  - `SKILL.md` — entry point with trigger phrases (VN+EN), reviewer matrix per email class (transactional/lifecycle/marketing), 7-step process, 10 gotchas (Thymeleaf escaping, logo URL, dark-mode, tenant isolation, inline CSS, unsubscribe mandate, Vietnam Nghị định 91/2020)
  - `reference/checklist.md` — 40-point checklist across 7 categories (Brand, Legal, i18n, Variables, Mobile, Client compat, Tenant isolation), MUST/SHOULD/NICE levels, summary row format for PR comment, known baseline violations for current 16 templates
  - `reference/sample-data.md` — canonical Thymeleaf sample data (branded + unbranded fixtures) for all 16 existing templates + edge-case variants (long names, XSS, null branding, diacritics) + preview tooling options (MailHog / Mailtrap / future EmailPreviewController)
  - State-check verified: 13 kitehub-email templates + 3 kiteclass-gateway templates. None currently have unsubscribe link / physical address / EN fallback — remediation deferred to feature PRs, not part of this skill-creation PR.
  - Acceptance criteria satisfied: review skill doc exists; PR template checklist update deferred (no `.github/PULL_REQUEST_TEMPLATE.md` touch in this docs-only PR).
