# GAP-462: Phase 4 milestone audit suite (UI /128 + Quality + Performance)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (mandate per `post-wave-audit-mandate.md` §2.4.2 — `phase-4-kit-ports` domain-milestone obligation; deferred as separate gap to keep Wave 50/51 closure tight)
**Domain:** Quality / audit
**Found:** 2026-05-10 (Wave 50 SHIPPED triggered phase-4-kit-ports milestone)
**Affects:** all 7 Phase 4 kits (kc-parent + kc-teacher + kc-student + kc-owner-pro + kh-pro + kh-admin + ai-branding-wizard-v2)

## Problem

Per `.claude/rules/post-wave-audit-mandate.md` §2.4 Domain-Milestone Audit Cadence + §2.4.1 registry, Wave 50 closure reaches the `phase-4-kit-ports` domain milestone. Required audit suite per §2.4.2:

- **UI Review /128** per kit (7 kits, ~144 screens total)
- **Quality Audit /110** (cross-system, FE-heavy focus)
- **Performance Audit /100** (FE bundle + API response)

Wave 50 closure PR (this session) defers the audit execution to keep closure tight. This gap tracks the deferred obligation.

## Current State (verified 2026-05-10)

Audit prep checklist already produced via Explore agent (Wave 50 prep task). Available at session log; key findings:

| Audit type | Estimated wall-clock | Blockers documented |
|---|---|---|
| UI /128 (144 screens, 7 kits) | ~50-80 min capture + ~80-120 min scoring (or ~30 min × 3 subagents parallel) | Lighthouse PWA ≥90 DEFERRED (HTTPS localhost blocker — separate follow-up post-HTTPS-staging) |
| Quality /110 | ~90-120 min (or ~40 min × 3 subagents parallel) | Cat 11 Persona Coverage = 5/10 placeholder pending GAP-152 first reports; E2E Cat 1 relies on manual or MSW mock |
| Performance /100 | ~30-45 min (DB grep + bundle analysis + config review) | None |
| **Total sequential** | **~5-6 hours** | — |
| **Total subagent-parallel (3 agents)** | **~1.5-2 hours** | — |

## Proposed Fix

1. Spawn audit suite via Wave 53+ (after brand pivot Wave 52 surface rebrand stabilizes)
2. Execute per audit prep checklist (`/ui-review`, `/quality-audit kitehub all` + `/quality-audit kiteclass all`, `/performance-audit`)
3. File reports under `documents/04-quality/audits/{ui,quality,performance}/`
4. File sub-gaps for any per-screen score <105/128 per `audit-to-gap-pipeline.md` §3
5. Wave closure PR includes `DOMAIN_MILESTONE_AUDIT: phase-4-kit-ports <report-paths>` trailer per `post-wave-audit-mandate.md` §3
6. Update `output-review-mandate.md` §3 matrix rows for affected categories

## Acceptance Criteria

- [ ] UI /128 audit run for all 7 Phase 4 kits with score per screen
- [ ] Quality /110 audit run (kitehub + kiteclass; FE-heavy Phase 4 focus)
- [ ] Performance /100 audit run (DB query / API response / FE bundle / cache)
- [ ] 3 reports filed under `documents/04-quality/audits/`
- [ ] Sub-gaps filed for findings <105/128 OR <70/100 OR P0/P1 issues
- [ ] ROADMAP §🚀 Status Snapshot updated with milestone audit conclusion
- [ ] `output-review-mandate.md` §3 rows refreshed if score deltas significant
- [ ] Closure PR commit body includes `DOMAIN_MILESTONE_AUDIT:` trailer

## Related

- Parent: Wave 50 closure (this session)
- Cross-link: `post-wave-audit-mandate.md` §2.4.2 (milestone obligation source)
- Cross-link: GAP-152 (Persona Coverage data-pending Cat 11 dependency)
- Audit prep checklist: produced 2026-05-10 by Explore agent (session log; ~5-6h sequential / ~1.5-2h subagent-parallel)
- Wave 53+ candidate work

## Log

- **2026-05-10**: Filed at Wave 50 closure as deferred milestone audit obligation. Per `post-wave-audit-mandate.md` §2.4.3 closure PR `AUDIT_DEFER_DOMAIN_MILESTONE` trailer norm: this gap = the named follow-up milestone wave 53+ is expected to close. 14-day staleness warning kicks in 2026-05-24 if no Wave 53+ milestone audit in flight.
