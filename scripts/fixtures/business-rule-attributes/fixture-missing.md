# Fixture: NON-COMPLIANT business rule (missing Compliance + Review cadence)

Synthetic fixture for `scripts/check-business-rule-attributes.sh` self-test.
Mirrors the BAD example in `business-logic-review.md` §4.1 (bare value, partial attrs).
Expected verdict: BLOCK when treated as ADDED (missing Compliance + Review-cadence);
downgraded to PASS(warn) when BUSINESS_RULE_OVERRIDE: trailer present.

### TR-01: Trial duration

- **Value:** 14 days
- **Source:** informed gut
- **Rationale:** seemed about right for onboarding.
- **Reviewer:** @nguyenvankiet (solo-dev).
