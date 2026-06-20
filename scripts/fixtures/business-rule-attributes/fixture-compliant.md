# Fixture: COMPLIANT business rule (all 5 attributes)

Synthetic fixture for `scripts/check-business-rule-attributes.sh` self-test.
Mirrors the GOOD example in `business-logic-review.md` §4.2. Expected verdict: PASS.

### TR-01: Trial duration

- **Value:** 14 days (config key: `kitehub.trial.duration-days`)
- **Source:** Competitor analysis (Hotmart 7d, Teachable 30d) + informed gut (no internal A/B yet)
- **Rationale:** 7d too short — onboarding avg 5-6d; 30d delays revenue. 14d = 2× onboarding window.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-21). Legal review N/A — trial mechanics unregulated.
- **Compliance check:** N/A — no PDPL / Consumer Protection trigger (free trial, no auto-renewal).
- **Review cadence:** Quarterly. **Next review:** 2026-09-21. Event triggers: competitor pricing change.
