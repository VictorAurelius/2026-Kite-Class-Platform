# GAP-283: Dependabot Pre-MVP Lock — Resume Minor Bumps Post-MVP

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (DevOps / Process — pre-MVP dependency stability vs continuous bumps trade-off)
**Domain:** DevOps / CI / Dependency Management
**Found:** 2026-04-30 (pre-MVP dependency lock decision after 4 simultaneous Dependabot PR failures)
**Affects:** Weekly Dependabot bump cadence; security patches still flow, minor bumps deferred

## Problem

Dependabot's weekly `all-deps` group bumps surface real breakage at minor-bump scale. Solo-dev capacity pre-MVP can't sustain weekly triage of 6 simultaneous PRs (one per ecosystem). Recent trigger: 2026-04-30 wave produced 4 failing PRs in same morning batch — investigation cost ~2-3h per PR.

**Failed PRs that triggered this lock (2026-04-30):**
- PR #715 — kitehub Maven AWS SDK group (BOM/s3/ses 2.42→2.44): broke Test KiteHub Admin Service (likely real S3/SES API change)
- PR #716 — GitHub Actions all-deps (6 updates): no CI checks reported (workflow path filter miss); risk silent break
- PR #717 — kitehub-frontend npm (8 deps incl. typescript-eslint, axios, react-hook-form): broke Lint, Lighthouse, Tests
- PR #718 — kiteclass-frontend npm: broke Frontend Tests & Build (likely Next.js RSC regression per `feedback_nextjs_rsc_array_regression.md` pattern)

## Action taken (2026-04-30)

1. **Closed all 4 failing PRs** with rationale comment
2. **Restricted all weekly ecosystems to `patch`-only** in `.github/dependabot.yml`:
   - kitehub Maven: `update-types: ["patch"]` (was `["minor", "patch"]`)
   - kiteclass-core Maven: same
   - kiteclass-gateway Maven: same
   - kitehub-frontend npm: same
   - kiteclass-frontend npm: same
   - GitHub Actions: kept as-is (`["minor", "patch", "major"]`) per existing comment about runner Node deprecation schedule

Patch + security advisories still flow through normally — only minor bumps blocked.

## Acceptance Criteria (resume condition post-MVP)

- [ ] MVP soft launch ready (~4-6 weeks per current ROADMAP estimate)
- [ ] Quarterly dependency review wave-pack scheduled (replace weekly cadence with monthly batch)
- [ ] Dedicated dep-triage skill (covers: which packages broke, isolated test runs, manual upgrade strategy)
- [ ] Restore `update-types: ["minor", "patch"]` in `.github/dependabot.yml` for all 5 weekly ecosystems
- [ ] Add specific package pins for known-breaking deps from 2026-04-30 batch:
  - AWS SDK kitehub: investigate KH Admin Service test failure, fix or pin pre-2.43.x
  - typescript-eslint kitehub-frontend: review which 8.59.x bump broke Lint
  - axios + react-hook-form kiteclass-frontend: check Next.js RSC compat
- [ ] Document monthly dep-triage runbook in `documents/05-guides/operations/`

## Out of Scope

- **Security advisories** — continue to flow regardless of lock (this is patch-tier already)
- **Major bump enabling** — separate decision post-MVP, requires per-ecosystem migration plans
- **Dependabot ecosystem additions** (Docker images, Terraform modules) — deferred separately

## Dependencies

- MVP soft launch milestone (per ROADMAP estimate ~4-6 weeks, 2026-06 target)
- Solo-dev capacity allocation post-MVP

## Related

- Memory: `feedback_dependabot_first_run.md` — 3-stage enable policy
- Memory: `feedback_dependabot_pnpm_transitive.md` — pnpm transitive limitation
- Memory: `feedback_nextjs_rsc_array_regression.md` — Next.js pin rationale
- Memory: `feedback_thymeleaf_ognl_pin.md` — ognl pin in kiteclass-core
- Memory: `feedback_dependabot_pin_violations.md` — every memory pin needs ignore entry
- Memory: `feedback_dependabot_alert_query.md` — security alert query format
- Plan: `documents/03-planning/plans/plan-dependabot-rollout-2026-04.md` — original rollout strategy
- Closed PRs: #715, #716, #717, #718 (2026-04-30 batch)

## Log

- **2026-04-30** — Created at pre-MVP dependency lock decision. Trigger: 4 Dependabot PRs (#715/#716/#717/#718) failed simultaneously after rebase, real breakage emerged (not just flaky), solo-dev capacity insufficient for weekly triage cycle pre-MVP. Conservative lock chosen over surgical per-package pins because: (a) MVP focus on feature/governance not dep triage, (b) patch-tier covers security advisories, (c) easier to restore single config flag post-MVP than maintain growing pin list. Resume estimate: post-MVP soft launch (~4-6 weeks).
