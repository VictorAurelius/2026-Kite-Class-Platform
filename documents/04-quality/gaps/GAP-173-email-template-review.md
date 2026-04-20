---
name: GAP-173 — Email template review checklist
description: Customer-facing email templates have no brand/legal/i18n review; create review template + mandatory gate before send
type: gap
---

# GAP-173: Email Template Review Checklist

**Status:** 🔵 OPEN
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
