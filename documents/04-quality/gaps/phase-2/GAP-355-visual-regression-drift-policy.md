# GAP-355: Visual Regression Drift Policy — Prototype ↔ Production Sync

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (operations gap — multi-week impact when prototype iterates)
**Domain:** Quality / Design System / Operations
**Found:** 2026-05-05 (simulation-gap-finder — Persona: Developer × Stage: Evolution × Category: C10)
**Affects:** `packages/shared-ui/` Playwright baselines + future Round 4+ HTML prototype iterations + downstream ports (GAP-266..272)

## Problem

GAP-273 + GAP-349 capture **initial** visual regression baseline at component port time:
- "Visual regression baseline captured" (GAP-273)
- "Playwright visual regression baseline for default + dark + 3 viewports" (GAP-349)

But there is **no policy** for ongoing drift:

- When HTML prototype iterates Round 4+ (intentional design refresh), how does production component sync?
- When component receives MAJOR bump (per GAP-351 semver), is a new baseline implicit or explicit?
- Who owns updating baseline images when they drift legitimately?
- How is intentional drift (design refresh) distinguished from accidental drift (CSS regression)?

Without policy, production component diverges silently from prototype source-of-truth, OR baselines pile up stale and CI churns false positives.

## Current State (verified 2026-05-05)

| Artifact | Status |
|---|---|
| GAP-273 mention "baseline captured" | ✅ initial only |
| GAP-349 mention Playwright config + default+dark+viewports | ✅ initial only |
| Baseline UPDATE process documented | ❌ none |
| Drift detection rule (CI fail behavior) | ❌ none |
| Owner for baseline curation | ❌ undefined |
| Round 4+ prototype evolution path | ❌ undefined |

## Proposed Fix

ADR + governance + CI behavior:

**ADR-027 Visual Regression Drift Policy:**

1. **Baseline ownership:** Component author owns initial baseline; design-system reviewer (rotating) owns drift verdicts.
2. **Drift verdict types:**
   - **Accept** — drift is intentional (matches new prototype Round 4+); update baseline same PR; document reason in commit body.
   - **Reject** — drift is regression; fix code, no baseline update.
   - **Quarantine** — uncertain; mark test `.skip` + file follow-up gap (max 14-day quarantine).
3. **Prototype-as-source-of-truth:** When HTML prototype updates (e.g. Round 4 refresh), the prototype PR triggers component baseline regeneration in next port PR. Both PRs cross-reference.
4. **Baseline storage:** `packages/shared-ui/__tests__/__snapshots__/` (git LFS for PNG > 100KB).
5. **CI gate behavior:** Visual diff > pixel threshold (default 0.1%) → fail CI; require Accept|Reject|Quarantine in PR description.
6. **Baseline rot detection:** Quarterly cron job lists baselines untouched > 90 days → file gap if any underlying prototype changed.

**Process artifacts:**
- `packages/shared-ui/VISUAL-REGRESSION.md` runbook (how to triage drift)
- PR template checkbox: "If visual diff: state verdict (Accept|Reject|Quarantine)"
- `audit-gate.py` rule `visual-drift-verdict-required` — block PR with diff but no verdict cited

## Acceptance Criteria

- [ ] `documents/02-architecture/adr/ADR-027-visual-regression-drift-policy.md` written + ACCEPTED
- [ ] `packages/shared-ui/VISUAL-REGRESSION.md` runbook shipped
- [ ] PR template checkbox added under Output Review Checklist
- [ ] `audit-gate.py` rule + 3-fixture self-test (Accept/Reject/Quarantine)
- [ ] Cross-link to GAP-351 semver policy (MAJOR bump implies baseline regen)
- [ ] Quarterly baseline-rot cron documented (ship workflow OR defer to follow-up)
- [ ] First port (GAP-266 kiteclass-pro v2) follows the policy in its PR

## Why P2

Per `meta-gap-priority.md` §3 — Operations tier (Meta-cusp). Not blocker for first port; becomes painful at port #3+ when baselines accumulate. P2 = file now, execute alongside GAP-351 (semver) as paired governance wave-pack.

## Related

- GAP-273 (Phase 1 components — initial baseline)
- GAP-349 (Phase 2 wave-pack — initial Playwright config)
- GAP-351 (semver policy — MAJOR bump triggers baseline regen — paired)
- ADR-024 (shared-ui strategy — extends with versioning + drift discipline)
- `incident-to-rule-pipeline.md` (drift policy is meta-rule for design-system)

## Effort estimate

~1-2 days. ADR-027 (~3 hr) + runbook (~2 hr) + audit-gate rule + self-test (~2 hr) + PR template (~30 min) + first-port integration (~3 hr).

## Log

- **2026-05-05:** Filed via simulation-gap-finder 3-axis matrix sweep. Discovered at Developer × Evolution × C10 cell. State-check: GAP-273 + GAP-349 cover *initial* baseline only — 0 mentions of drift policy / baseline update process / verdict types. Paired with GAP-351 (semver policy) as governance wave-pack candidate — both same-PR ship discipline same kind (component lifecycle).
