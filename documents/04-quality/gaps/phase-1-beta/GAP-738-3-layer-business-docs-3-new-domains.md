# GAP-738: 3-layer business docs MISSING cho 3 new domains (reschedule + course-pricing + payment-record)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Meta (Living Docs governance)
**Found:** 2026-05-25 (Wave audit-1 Bucket B Business Logic audit)
**Affects:** Living Docs rule compliance; verification chain BR-xxx → UC-xxx → endpoint → @Test

## Problem

Per `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-1:

Wave beta-readiness-4 ship code cho 3 new domains nhưng KHÔNG ship 3-layer business docs (rules.md + use-cases.md + api-contract.md) tương ứng → vi phạm Living Docs rule (CLAUDE.md "Business Logic Documents — 3-Layer Structure" mandate).

3 new domains affected:
1. **reschedule** (PR #1781 Wave br-4 Bucket D) — booking/class reschedule flow
2. **course-pricing** (PR #1783 Bucket C) — pricing model PER_HOUR / COURSE_PACKAGE switch
3. **payment-record** (PR #1783 Bucket C paired GAP-292b) — payment recording

Pre-commit hook supposed to warn missing 3 files per domain — không catch hoặc developer skip warning.

## Root Cause

Bucket implementation race: focus on code + tests; docs treated as post-merge follow-up but never followed up. `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync rule (paired same-PR mandate) nominally applies but not enforced for Living Docs.

## Proposed Fix

Tạo 9 file (3 per domain × 3 domains):
1. `documents/01-business/kiteclass/reschedule/{rules,use-cases,api-contract}.md`
2. `documents/01-business/kiteclass/course-pricing/{rules,use-cases,api-contract}.md`
3. `documents/01-business/kiteclass/payment-record/{rules,use-cases,api-contract}.md`

Mỗi rules.md:
- BR-DOMAIN-NNN business rules với attributes (per `business-logic-review.md`)
- Config keys reference (vd `kiteclass.pricing.default-model`)

Mỗi use-cases.md:
- UC-DOMAIN-NNN actor + steps + errors + FE behavior

Mỗi api-contract.md:
- Endpoint declarations matching Java `@RequestMapping`
- Request/response schema
- Error codes

## Acceptance Criteria

- [ ] 9 files created với 3-layer structure mandate
- [ ] BR-DOMAIN-NNN business rules cited trong code (`@BusinessRule` annotation or comment)
- [ ] api-contract.md endpoints match Java controller (verified via `scripts/check-cross-layer-contract-drift.sh`)
- [ ] Pre-commit hook 3-layer check PASS post-commit
- [ ] `check-3-layer-completeness.sh` CI job green
- [ ] Business Logic audit re-run: P0-1 closed → Cat 1 Rule Coverage score +X điểm

## Related

- Audit report: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-1
- Rule: CLAUDE.md "CRITICAL: Business Logic Documents — 3-Layer Structure"
- Rule: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync
- Wave: planned `wave-beta-readiness-8`

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-1 finding. Wave beta-readiness-8 scope.
