# GAP-354: Per-Kit Bundle Size Budget (Track 2 Production Port)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (good practice — performance baseline; NOT blocker but compounds without policy)
**Domain:** Frontend / Performance / Design System
**Found:** 2026-05-05 (simulation-gap-finder — Persona: End User × Stage: Daily Usage × Category: C4 Performance)
**Affects:** All 7 kit ports (GAP-266..272) + production routes consuming `@kite/shared-ui`

## Problem

GAP-349 (Track 2 Phase 2 wave-pack plan) defines per-component bundle budget (`<5KB gzip per component`). However, **per-kit total bundle budget is undefined** for the 7 kit ports (GAP-266..272). Without a per-kit ceiling:

- Each kit port PR judges performance in isolation
- Cumulative bundle bloat invisible until production Lighthouse drops
- No criteria for "kit too heavy → split route or lazy-load"
- No regression detection when adding new components/screens to existing kit

Especially relevant for mobile-PWA kits (kiteclass-parent + kiteclass-student) where 3G/4G performance directly impacts P5/Tier-2 personas.

## Current State (verified 2026-05-05)

| Check | Status |
|---|---|
| Per-component budget (GAP-349) | ✅ `<5KB gzip per component` |
| Per-kit total budget | ❌ 0 hits "bundle.budget"/"kit.*gzip" in GAP-266..272 + GAP-349 |
| Bundle analysis tooling | ⚠️ Next.js has built-in `@next/bundle-analyzer`; not configured |
| Bundle size CI gate | ❌ none |
| Lighthouse perf baseline per route | ⚠️ partial — `feedback_nextjs_rsc_array_regression.md` mentions GAP-127 code-splitting; no per-kit ceiling |

## Proposed Fix

**Tier 1 — Define budgets:**

| Kit | Persona | Network target | Initial JS budget (gzip) | First Load (kB gzip) |
|---|---|---|---|---|
| `kiteclass-parent` | Parent (Tier 2 KC + P5) — mobile primary | 3G/4G mobile | 80 kB | 150 kB |
| `kiteclass-student` | Student (Tier 2 + P5) — mobile primary | 3G/4G mobile | 80 kB | 150 kB |
| `kiteclass-teacher` | Teacher (P3 + P5 GVCN) — tablet primary | 4G/WiFi | 120 kB | 200 kB |
| `kiteclass-pro v2` | P2 Owner + P3 Director — desktop primary | WiFi/wired | 150 kB | 250 kB |
| `kitehub-pro v2` | P2 Owner KH SaaS — desktop | WiFi/wired | 150 kB | 250 kB |
| `kitehub-admin` | K-12 Principal P5 — tablet/desktop | 4G/WiFi | 120 kB | 200 kB |
| `ai-branding-wizard v2` | Owner provisioning — desktop | WiFi/wired | 150 kB | 250 kB |

(Numbers are *opening proposal* — calibrate after first port; freeze in ADR.)

**Tier 2 — Tooling:**
- `@next/bundle-analyzer` enabled in both Next.js apps (gated by `ANALYZE=true`)
- CI step on Track 2 port PRs: build → measure → report delta vs baseline
- Bundle-size-action GitHub Action gating PRs on budget violation

**Tier 3 — Policy:**
- ADR-026 documents budgets + rationale + escalation path
- Budget violation = require waiver (commit trailer `BUNDLE_BUDGET_OVERRIDE: <reason>`) OR fix
- Recalibrate quarterly (Track 2 evolves; budgets shift with persona insight)

## Acceptance Criteria

- [ ] `documents/02-architecture/adr/ADR-026-per-kit-bundle-budget.md` written + ACCEPTED with budget table calibrated against MVP first port (GAP-266 kiteclass-pro v2)
- [ ] `@next/bundle-analyzer` configured in both Next.js apps
- [ ] CI workflow `.github/workflows/bundle-size-check.yml` runs on PR touching `kiteclass-frontend/` or `kitehub-frontend/`
- [ ] First port PR (GAP-266 kiteclass-pro v2) passes baseline budget
- [ ] Per-kit budget cited in each kit port AC (GAP-266..272 update)
- [ ] Override trailer `BUNDLE_BUDGET_OVERRIDE:` documented
- [ ] Quarterly recalibration cadence in ADR-026 §Review

## Why P2

Per `meta-gap-priority.md` §3 — Feature/Performance tier. Not legal mandate, not persona blocker. But:
- Compounds silently — every PR adds without budget = 6mo later production is 2x bloated
- Mobile personas (parent/student) directly impacted on 3G/4G — Tier 2 in P5 K-12 = ~80% mobile
- Easy to file early (this gap), expensive to retrofit later

## Related

- GAP-349 per-component budget (sibling — this gap aggregates to per-kit)
- GAP-127 code-splitting (related but route-level, not kit-level)
- `feedback_nextjs_rsc_array_regression.md` (related perf regression history)
- Sister: GAP-352 WCAG audit also uses lighthouse-ci infra → consolidate workflow

## Effort estimate

~1-2 days. ADR-026 (~3 hr) + bundle-analyzer setup (~2 hr) + CI workflow (~3 hr) + first-port calibration (~3 hr).

## Log

- **2026-05-05:** Filed via simulation-gap-finder 3-axis matrix sweep. Discovered at End User × Daily Usage × C4 cell. State-check: 0 hits "bundle.budget"/"kit.*gzip" in GAP-26X/27X/349. P2 because not blocker for first port (GAP-266) but should land alongside it for ratchet baseline.
