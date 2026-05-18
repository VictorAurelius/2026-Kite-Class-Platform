# GAP-352: WCAG AA Third-Party Audit Before Track 2 Production Port

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (gates Track 2 Phase 4 production port — accessibility is legal-adjacent + persona-blocker)
**Domain:** Quality / Accessibility / Frontend
**Found:** 2026-05-05 (simulation-gap-finder — Persona: Platform Admin × Stage: Provisioning × Category: C6 Compliance)
**Affects:** All 7 kit ports (GAP-266..272) + `@kite/shared-ui` components (GAP-273)

## Problem

Kit-level acceptance criteria require "WCAG AA contrast 4.5:1 measured + documented in HTML comment" (`dossier/10-acceptance-criteria.md`) — but this is **agent self-report at HTML prototype time**, NOT formal third-party audit. WCAG AA conformance requires:

- Programmatic verification (axe-core, pa11y, lighthouse-ci)
- Manual screen-reader test (NVDA / JAWS / VoiceOver)
- Keyboard-only navigation full traversal
- Reduced-motion + reduced-transparency variants
- Focus-trap behavior in dialogs/modals

None of these have automated infra in the repo. Without a formal audit pre-port, production code ships with self-asserted accessibility — same calibration problem as GAP-348 (self-audit overstates 15-20 pts).

## Current State (verified 2026-05-05)

| Check | Status |
|---|---|
| axe-core in deps | ❌ 0 hits in `packages/`, `kiteclass-frontend/`, `kitehub-frontend/` |
| lighthouse-ci config | ❌ 0 hits |
| Manual a11y audit report | ❌ none in `documents/04-quality/audits/` |
| WCAG self-report in HTML kits | ✅ partial — kits have `<!-- WCAG: contrast 4.7:1 -->` comments but unverified externally |
| GAP-348 covers Round 3 review | ⚠️ scope = visual /128 + persona AC, NOT formal WCAG conformance |

## Proposed Fix

**Phase A — Tooling infra (~1 day):**
- Add `axe-core` + `@axe-core/playwright` to `packages/shared-ui/__tests__/`
- Add `lighthouse-ci` workflow `.github/workflows/lighthouse-ci.yml` running on PR touching `kiteclass-frontend` or `kitehub-frontend`
- Establish baseline a11y score per route (target ≥95 lighthouse a11y, 0 axe violations critical/serious)

**Phase B — HTML kit a11y audit (~1 day):**
- Run axe-core against each kit's `index.html` + screens via Playwright
- Manual screen-reader spot-check 1 representative screen per kit (5 critical user journeys total)
- Output `documents/04-quality/audits/accessibility/2026-05-XX-ui-kits-wcag-baseline.md`

**Phase C — Production port gating (per kit port PR):**
- Each kit port PR (GAP-266..272) must pass: 0 critical axe violations + lighthouse a11y ≥95
- CI gate via lighthouse-ci-action

## Acceptance Criteria

- [ ] axe-core + lighthouse-ci infra wired in monorepo
- [ ] Baseline a11y audit report `documents/04-quality/audits/accessibility/2026-05-XX-ui-kits-wcag-baseline.md` covers all 9 kits + 12 components
- [ ] Per-kit a11y score documented (target lighthouse ≥95, 0 critical/serious axe)
- [ ] Manual screen-reader spot-check covers 5 critical journeys (login, attendance entry, payment, parent invite, AI branding wizard)
- [ ] CI gate `wcag-aa-required` added to `audit-gate.py` AUDIT_RULES — blocks port PRs without lighthouse-ci pass
- [ ] Kit-level gaps filed for any kit failing baseline (likely 1-2 kits will fail focus-trap or screen-reader pass)
- [ ] Cross-link added to GAP-266..272 (production port AC = pass this gate)

## Why P1

Per `meta-gap-priority.md` §3 — Business-Logic / Compliance tier. Vietnam doesn't yet mandate WCAG by law (unlike EU EAA), but:
- K-12 schools (P5) increasingly require accessibility for special-needs students
- Public-sector tenants (future MoET partners) often require WCAG AA in procurement
- Self-report calibration (per `feedback_audit_calibration.md`) consistently overstates → trusting kit self-audits at port time = production a11y debt

## Related

- Sister: GAP-348 (Round 3 persona review — visual /128, this gap = formal WCAG layer)
- GAP-266..272 (kit ports — production-AC depends on this gate)
- `output-review-mandate.md` §3 row "HTML/JSX prototypes" — current standard is self-report; this gap upgrades to verified
- `feedback_audit_calibration.md` (self-report bias)

## Effort estimate

~3-4 days total (Phase A 1d + Phase B 1d + Phase C wiring 1d + report). Phase A + B parallelizable as 2-bucket wave-pack.

## Log

- **2026-05-05:** Filed via simulation-gap-finder 3-axis matrix sweep. Discovered at Platform Admin × Provisioning × C6 cell. State-check: 0 hits for "axe-core"/"lighthouse-ci"/"wcag.*audit" in monorepo. GAP-348 covers visual + persona /128 but NOT formal WCAG conformance. P1 because all 7 kit ports (GAP-266..272) inherit the gap.
