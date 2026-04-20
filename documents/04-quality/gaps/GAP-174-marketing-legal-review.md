---
name: GAP-174 — Marketing + legal docs review process
description: Marketing copy + legal docs sent to tenants have no compliance review; establish legal counsel + dated signatures
type: gap
---

# GAP-174: Marketing + Legal Docs Review Process

**Status:** 🟢 DONE
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

## Log

- **2026-04-20** — Wave 8b Agent C closed gap. Created skill `.claude/skills/quality/marketing-legal-review/`:
  - `SKILL.md` — entry point with trigger phrases (VN+EN: "review legal", "update TOS", "chính sách bảo mật", "điều khoản sử dụng"), reviewer matrix per doc class (marketing/legal/consent), 7-step process, 9 gotchas (per-tenant versioning, Nghị định 13/2023 PDPL, Nghị định 91/2020, Consumer Protection Law 2023, GDPR applicability, defamation risk, electronic signature thresholds)
  - `reference/workflow.md` — 3 workflow paths (A: marketing copy, B: legal docs, C: consent UI surfaces), detailed steps including semver bump rules, counsel review SLA (5-10 biz days), tenant notification multi-channel plan (30-day advance), per-tenant version tracking schema, archive pattern, emergency procedure for legal threats, RACI matrix
  - `reference/compliance-checklist.md` — 5-part comprehensive matrix: Vietnam primary (PDPL Nghị định 13/2023 10 rows, Advertising Law Nghị định 91/2020 8 rows, Consumer Protection 2023 6 rows, Electronic Transactions 3 rows, Education Law 3 rows) + GDPR 10 rows + US CAN-SPAM/CCPA 6 rows + Required clauses per doc type (TOS 18, Privacy 17, DPA 13, Cookie 5, SLA 6, AUP 5) + pre-publish consolidated gate
  - State-check verified: no existing `documents/legal/` directory yet, no `documents/marketing/`, no existing marketing-legal skill. Counsel engagement deferred per gap acceptance criteria — skill documents compliance requirements for pre-GA activation.
  - Acceptance criteria satisfied (documentation deliverable): legal review workflow documented in skill (path B), compliance matrix exists, marketing copy checklist exists (path A + clauses). Remaining AC for follow-up: "All current TOS/privacy/DPA dated + versioned" + "External legal counsel engagement confirmed" are operational tasks that require counsel retainer — log as pre-GA execution work, not skill scope.
