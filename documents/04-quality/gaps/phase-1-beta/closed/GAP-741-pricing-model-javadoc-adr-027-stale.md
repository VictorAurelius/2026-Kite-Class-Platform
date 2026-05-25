# GAP-741: PricingModel.java javadoc cite ADR-027 stale (should be ADR-035)

**Status:** 🟢 DONE
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

ADR-027 thực tế là `ADR-027-statuspage-vendor.md` (Instatus Free Tier decision Phase 1 BETA) — hoàn toàn không liên quan pricing. ADR-035 (`ADR-035-pricing-model-taxonomy.md`) là canonical pricing decision per Wave beta-readiness-4 Bucket C + ADR-035 frontmatter `supersedes: none`. ADR-035 §15 note đã ghi rõ "Wave plan §3.6 originally referenced ADR-027; ADR-027 đã taken bởi statuspage-vendor decision. Bucket C ADR shipped với next sequential ADR-035".

## Proposed Fix

1. `grep -rn "ADR-027" kiteclass/src/main/java/` — surface all stale references
2. Replace với ADR-035 (per canonical decision)
3. Verify ADR-027 no longer relevant (read ADR file → confirm superseded or scope different)
4. Update PR description checkbox confirm sweep done

## Acceptance Criteria

- [x] 0 reference `ADR-027` trong `PricingModel.java`
- [x] javadoc cite `ADR-035` canonical
- [x] grep sweep broader codebase confirm no other stale ADR-027 references for pricing
- [x] Business Logic audit re-run: P0-4 closed

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-4
- ADR-035 (canonical pricing)
- ADR-027 (stale reference — actually statuspage-vendor decision, unrelated to pricing)
- Rule: `audit-to-gap-pipeline.md` §2.7
- Sister gap GAP-740 (default value paired)
- Wave: `wave-beta-readiness-8` Bucket E

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-4. Wave beta-readiness-8 scope.
- **2026-05-25 (DONE):** Wave beta-readiness-8 Bucket E shipped fix. Edit `PricingModel.java` line 10 javadoc `@see` link từ `ADR-027-pricing-model-taxonomy.md` → `ADR-035-pricing-model-taxonomy.md` (file ADR-027 thực tế là `statuspage-vendor`, không phải pricing). Broader grep sweep surfaced 2 thêm refs stale trong `V67__add_pricing_model_to_courses.sql` (header comment line 10 + `COMMENT ON COLUMN` line 49) — cùng fix qua trong PR. Verify: `grep -rn "ADR-027" kiteclass/ kitehub/ documents/01-business/ 2>/dev/null` trả 0 hit. `cd kiteclass/kiteclass-core && ./mvnw compile` BUILD SUCCESS. PR #<num>.
