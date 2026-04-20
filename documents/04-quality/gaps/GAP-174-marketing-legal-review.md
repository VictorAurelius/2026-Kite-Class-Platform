---
name: GAP-174 — Marketing + legal docs review process
description: Marketing copy + legal docs sent to tenants have no compliance review; establish legal counsel + dated signatures
type: gap
---

# GAP-174: Marketing + Legal Docs Review Process

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (meta — compliance)
**Domain:** Marketing / Legal / Compliance
**Found:** 2026-04-14 (output-review-mandate §4 Violation #5)
**Affects:** Every marketing asset + legal doc (TOS, privacy policy, DPA) sent to tenants

## Problem

Marketing copy (landing pages, blog posts, pricing explanations) and legal docs (terms of service, privacy policy, data processing agreements) go live without legal counsel review. Risk: misleading claims, legal non-compliance, outdated terms, no version control for what-tenant-agreed-to-when.

## Root Cause

No legal review process. Marketing/legal treated as marketing/engineer self-sign-off.

## Proposed Fix

1. Establish legal review workflow:
   - Marketing copy for customer-facing landing: brand + legal sign-off pre-publish
   - Legal docs (TOS, privacy, DPA): external legal counsel review + dated + version-controlled
2. Archive previous versions — tenant must always see version they agreed to (even if newer exists)
3. Versioning pattern: `documents/legal/tos-v2.0-2026-04-20.md` with superseded pointers
4. Change log with effective date + transition period notification to tenants
5. Compliance checklist per jurisdiction (VN is primary; SEA expansion may need more)

## Acceptance Criteria

- [ ] Legal review workflow documented in `.claude/rules/marketing-legal-review.md`
- [ ] All current TOS/privacy/DPA dated + versioned
- [ ] External legal counsel engagement confirmed (or documented as deferred to launch phase)
- [ ] Marketing copy review checklist exists
- [ ] Compliance matrix (GDPR, VN advertising law, CAN-SPAM) documented

## Related

- Parent violation: output-review-mandate §4 #5
- Sibling: GAP-173 (email templates)
- Pre-GA blocker — cannot ship TOS to real customers without review
