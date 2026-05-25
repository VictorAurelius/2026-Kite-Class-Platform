# GAP-741: PricingModel.java javadoc cite ADR-027 stale (should be ADR-035)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (docs sync)
**Found:** 2026-05-25 (Wave audit-1 Bucket B Business Logic audit)
**Affects:** Code reference integrity; ADR cross-link traceability

## Problem

Per `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-4:

`PricingModel.java` javadoc trỏ tới ADR-027 (lạc hậu) thay vì ADR-035 (canonical pricing decision Phase 1 BETA).

Reader code đọc javadoc → click link → ADR-027 (irrelevant context) → confusion + wrong implementation guide.

## Root Cause

Code shipped trong Wave br-4 Bucket C dùng template/boilerplate javadoc từ Wave-27 era (~2026-04-26) → ADR reference không update khi pricing decision được supersede bởi ADR-035.

Per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — ADR-035 land → code references must sweep. Bucket C skip step này.

## Proposed Fix

1. `grep -rn "ADR-027" kiteclass/src/main/java/` — surface all stale references
2. Replace với ADR-035 (per canonical decision)
3. Verify ADR-027 no longer relevant (read ADR file → confirm superseded or scope different)
4. Update PR description checkbox confirm sweep done

## Acceptance Criteria

- [ ] 0 reference `ADR-027` trong `PricingModel.java`
- [ ] javadoc cite `ADR-035` canonical
- [ ] grep sweep broader codebase confirm no other stale ADR-027 references for pricing
- [ ] Business Logic audit re-run: P0-4 closed

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-4
- ADR-035 (canonical pricing)
- ADR-027 (stale reference)
- Rule: `audit-to-gap-pipeline.md` §2.7
- Sister gap GAP-740 (default value paired)
- Wave: planned `wave-beta-readiness-8`

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-4. Wave beta-readiness-8 scope.
