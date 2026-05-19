---
id: GAP-685
phase: phase-1-beta
status: OPEN
priority: P1
domain: Meta
audience: dev
---

# GAP-685: Wave 101 post-wave audit suite (api-contract + business-logic + security)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META)
**Domain:** Meta — Audit governance
**Found:** 2026-05-19 (Wave 101 closure)
**Affects:** kitehub-branding (4 controllers + SecurityConfig + @PreAuthorize); kitehub-platform (admin role + onboarding); kitehub-frontend wizard
**Deadline:** 2026-05-22 (post-wave-audit-mandate.md §2.2 — 3-day window from final Wave 101 merge 2026-05-19)

## Problem

Wave 101 merged 4 buckets (A/B/C/D) covering FE+BE close-outs. Per `post-wave-audit-mandate.md` §2.1 file-pattern matrix, the following audits are MANDATORY within 3 days:

- **api-contract-audit /100** — Bucket B added @PreAuthorize annotations to 4 kitehub-branding controllers (AIBrandingController, BrandingJobController, BrandingWizardController, BrandingJobV1Controller). Changes Dto/Controller layer → triggers `quality/api-contract-audit/SKILL.md`.
- **business-logic-audit /100** — RBAC role separation enforced at BE layer (OWNER/STAFF/MANAGER/TEACHER/ACCOUNTANT canonical roles). Changes rules.md / use-cases.md interpretation → triggers `quality/business-logic-audit/SKILL.md`.
- **security-audit /100** — New SecurityConfig + spring-boot-starter-security dep + audit log entries on 403 → triggers `quality/security-audit/SKILL.md` (v2 audit format per GAP-564).

Post-merge hook flagged "Missing audits: api-contract-audit" on PR #1607 merge.

## Acceptance Criteria

- [ ] Run `/api-contract-audit` skill on kitehub-branding; score reported; gap files filed for any P0/P1 findings per `audit-to-gap-pipeline.md` §3.
- [ ] Run `/business-logic-audit` skill on kitehub-branding RBAC scope; score reported.
- [ ] Run `/security-audit` skill v2 format on kitehub-branding SecurityConfig + @PreAuthorize coverage.
- [ ] Update `output-review-mandate.md` §3 matrix scores per audit-skill-rubric mandate.
- [ ] Update `audits-index.csv` with 3 new AUDIT-2026-05-{??}-wave-101-* rows.

## Related

- Wave 101 plan: `documents/03-planning/waves/wave-2026-05-19-101-product-demo-blockers.md`
- Wave 101 closure PR: (this PR)
- Rule: `.claude/rules/post-wave-audit-mandate.md` §2.1
- Bucket B PR #1607 (merged commit 536ff075)
- Bucket A PR #1603 (admin role mismatch, also touches @PreAuthorize-adjacent surface)
- Sister gap: GAP-619 (Wave 92 post-wave audit suite — same pattern, completed 2026-05-18)
