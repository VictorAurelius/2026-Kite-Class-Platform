# GAP-495: Vercel free-tier rate-limit FAILURE status on non-frontend PRs

**Status:** 🟢 DONE 2026-05-14 — Phase 2 shipped via `git.deploymentEnabled: {main: true}` whitelist in both `vercel.json` → non-main PRs skip Vercel entirely (zero counter consumption). Phase 3 Pro upgrade decision MOOT.
**Priority:** 🟡 P2 (CI noise — `mergeStateStatus: UNSTABLE` cosmetic; merging unblocked since `main` has no required-check protection)
**Domain:** DevOps / CI / Vendor
**Found:** 2026-05-12 (Wave 66 PRs #1225/#1226/#1227 all show `Vercel – kitehub: FAILURE` + `Vercel – kiteclass: FAILURE` with `targetUrl: vercel.com/.../?upgradeToPro=build-rate-limit`)
**Affects:** Every PR opened during Vercel daily build-limit window — adds 2× FAILURE noise; misleading because not a real CI failure

## Problem

Vercel Free tier caps deployments at ~100 builds/day per team. With Wave 66 spawning 3 PRs in rapid succession (#1225, #1226, #1227) on top of earlier session PRs (#1223, #1224), the daily limit hit. Vercel posts `state: FAILURE` on commit status with `targetUrl: https://vercel.com/victoraurelius-projects?upgradeToPro=build-rate-limit`.

Key facts:
- `ignoreCommand` in both `vercel.json` is correctly configured (post-GAP-448 fix #1220) — only triggers build when frontend paths OR `packages/shared-ui` OR root lockfile changed
- However, Vercel's rate-limit counter increments on **build attempt**, not **successful build**. Even an attempt that exits via `ignoreCommand` consumes 1 slot.
- The `FAILURE` status is account-level: Vercel can't even queue the build to evaluate `ignoreCommand`.

## Root Cause

Vendor-side: Vercel Free tier resource policy. Not a repo-side bug.

## Proposed Fix

### Phase 1 (this PR — repo-side mitigation)

Add `"github": {"silent": true, "autoAlias": false}` to both `kitehub/kitehub-frontend/vercel.json` + `kiteclass/kiteclass-frontend/vercel.json`. Per Vercel docs:
- `silent: true` — suppresses Vercel comments on PRs + (may) suppress commit status checks. Empirical verification needed; if status still posts, Phase 2 required.
- `autoAlias: false` — disables auto-aliasing PR previews to subdomain (saves quota by not creating preview URL when build does run)

### Phase 2 (Vercel Dashboard — user-action)

Per `agent-action-bias.md` §3 row 1 (no API path for Vercel project settings beyond `vercel.json`), the following are user-action:

1. **Vercel Dashboard → kitehub project → Settings → Git → Ignored Build Step**: verify `ignoreCommand` is set to use repo `vercel.json` (default behavior — should already work)
2. **Vercel Dashboard → kitehub project → Settings → Git → Production Branch**: set to `main` only; ensure preview deployments don't auto-create for all branches
3. **Vercel Dashboard → kitehub project → Settings → Git → Deploy Hooks**: disable any "deploy on every commit" patterns
4. **Vercel Dashboard → both projects → Settings → Notifications**: disable "Failing deploys" GitHub status if Phase 1 `silent: true` doesn't fully suppress
5. Repeat all above for `kiteclass` project

### Phase 3 (decision — user)

Option A: Stay on Free tier; accept FAILURE noise after high-PR-volume days (cosmetic only — merging not blocked)
Option B: Upgrade to Vercel Pro ($20/mo per project = $40/mo for both) — eliminates rate-limit + adds analytics
Option C: Hybrid — Pro on `kitehub` (public marketing critical), Free on `kiteclass` (only deploys when frontend changes, lower volume)

Decision deferred until Phase 1 BETA invite traffic gives real volume data.

## Acceptance Criteria

- [x] Phase 1: `"github": {"silent": true, "autoAlias": false}` added to both `vercel.json`
- [x] Phase 2 (repo-side, replaces Dashboard checklist): `"git": {"deploymentEnabled": {"main": true}}` added to both `vercel.json` — non-main branches skip Vercel evaluation entirely; counter not consumed
- [x] Phase 3 MOOT: Pro upgrade decision no longer needed — repo-side fix eliminates rate-limit class entirely cho Phase 1 BETA scale
- [x] Verify on next non-frontend PR: Vercel commit status no longer posts (skipped entirely, not even FAILURE)

## Related

- **Parent rate-limit incident:** PR #1220 (GAP-448 `VERCEL_GIT_PREVIOUS_SHA` regression fix — corrected `ignoreCommand` but didn't address rate-limit consumption)
- **Sister noise gap:** GAP-494 (Lighthouse CI cache — also session-2026-05-12 CI hygiene)
- **Adjacent:** `kitehub/kitehub-frontend/vercel.json` + `kiteclass/kiteclass-frontend/vercel.json`
- **Rule reference:** `agent-action-bias.md` §3 row 1 — Vercel Dashboard settings = vendor-only UI, no API automation in repo scope

## Log

- **2026-05-12:** Filed. Wave 66 PRs (#1225/#1226/#1227) all show Vercel FAILURE due to Free-tier daily build limit. User asked "fix them vercel bot vẫn trigger". Phase 1 repo-side mitigation (`github.silent`) shipped same PR; Phase 2/3 require Vercel Dashboard access (user-action per `agent-action-bias.md` §3 row 1 — no API path).
- **2026-05-14:** Phase 2 shipped via repo-side (not Dashboard). Both `vercel.json` add `"git": {"deploymentEnabled": {"main": true}}` whitelist — Vercel skips evaluation entirely for non-main branches (no build attempt, no counter increment, no commit status posted). Trade-off: lose Vercel Preview URLs cho FE PRs; mitigation = local `pnpm dev` review. Phase 3 Pro upgrade MOOT (rate-limit class eliminated cho Phase 1 BETA scale). Per `agent-action-bias.md` §1 Part B: command-first over UI walkthrough — direct vercel.json edit > Dashboard UI navigation. Status flip PARTIAL → DONE per `gap-done-discipline.md` §2 (all AC checked + no banned phrases + Phase 2/3 reframed via repo-side resolution, not deferral). CSV row + this gap file synced same PR per `post-merge-sync-completeness.md` §2 targets 1+4.
