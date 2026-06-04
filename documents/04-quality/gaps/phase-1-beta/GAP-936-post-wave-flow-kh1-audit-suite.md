# GAP-936: Post-Wave flow-kh1 audit suite — business-logic + ops-readiness (≤3 days)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (post-wave-audit-mandate compliance gate — deadline 2026-06-07)
**Domain:** Meta
**Found:** 2026-06-04 (PR #2147 squash-merged 07:42:28 UTC; audit-gate.py hook flagged 4 violations including 2 missing audits + business-logic docs gap)
**Affects:** `documents/04-quality/audits/business-logic/` + `documents/04-quality/audits/ops-readiness/`

## Problem

PR #2147 closed Wave flow-kh1 (G1+G2+G3 PASS for KH-1 + KH-2c). Per `post-wave-audit-mandate.md`, audit suite MUST run within 3 days of wave merge:
- business-logic-audit (changed BE service code in rollbackSignup + EmailEventEmitter + login flow)
- ops-readiness-audit (changed gateway circuit breaker config + TimeLimiter + production profile)
- Plus run `/wave-completion-check`

Hook compliance score: 1/5. Deadline: **2026-06-07 23:59 UTC**.

## Acceptance Criteria

- [ ] `/wave-completion-check` ran with verdict documented
- [ ] business-logic-audit run + report saved to `documents/04-quality/audits/business-logic/2026-06-XX-wave-flow-kh1.md`
- [ ] ops-readiness-audit run + report saved to `documents/04-quality/audits/ops-readiness/2026-06-XX-wave-flow-kh1.md`
- [ ] `audits-index.csv` updated with both audit rows per `meta-csv-index-pattern.md`
- [ ] CI on main green after #2147 merge
- [ ] `documents/01-business/kitehub/auth/api-contract.md` updated to reflect beta-signup error contract surfaced by GAP-926 (per audit hook flagged "Business logic changed but no 01-business/ docs updated")

## Related

- PR #2147 merge commit c9c2e3ed (2026-06-04 07:42:28 UTC)
- Per `post-wave-audit-mandate.md` 3-day gate
- Per `audit-to-gap-pipeline.md` audit suite ordering
- Per `meta-csv-index-pattern.md` audits CSV row mandate
