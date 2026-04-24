---
title: Dependabot Rollout — Pilot to All Ecosystems
status: active
created: 2026-04-24
updated: 2026-04-24
waves: []
gaps: [GAP-202, GAP-203, GAP-204]
---

# Dependabot Rollout — Pilot to All Ecosystems

## Purpose

Track the end-to-end rollout of Dependabot version-update PRs for the monorepo, from the 2026-04-21 flood incident through the same-day compressed pilot on 2026-04-24 to the full-ecosystem expansion.

This plan is the single source of truth for: what's enabled where, what Spring Boot / npm / actions versions each ecosystem currently holds, rollback criteria, and the post-expansion watch list.

Related analysis: [`documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`](../../04-quality/analyses/2026-04-21-dependabot-first-run-incident.md)

---

## 1. Timeline

| Date | Event | Artefact |
|------|-------|----------|
| 2026-04-21 | First-run flood — 28 version-update PRs in 3 minutes, un-grouped | Reverted config to security-only mode |
| 2026-04-24 (morning) | Pilot — kiteclass-core maven enabled with groups + semver-major ignore | PR #479 |
| 2026-04-24 (midday) | Pilot run — 5 grouped PRs created exactly as designed | PRs #480–#484 |
| 2026-04-24 (afternoon) | Pilot merged all green within ~1 h of creation | 5/5 merged |
| 2026-04-24 (later afternoon) | **Expansion** — all 6 ecosystems enabled using the pilot pattern + docker-build-push concurrency | PR #486 (this plan) |
| 2026-04-28 (Monday) | First scheduled full-ecosystem run at 02:00 Asia/Ho_Chi_Minh | — |

---

## 2. Ecosystem inventory

| # | Ecosystem | Directory | Limit | Current notable versions | Notes |
|:-:|-----------|-----------|:-----:|--------------------------|-------|
| 1 | maven | `/kitehub` (parent pom) | 5 | Boot **3.5.13** → bump to 3.5.14 pending | All `kitehub-*` children (admin, branding, email, gateway, platform, subscription) inherit Boot from this parent. |
| 2 | maven | `/kiteclass/kiteclass-core` | 5 | Boot **3.5.14** ✅ (via #480) | POI 5.5.1, openhtmltopdf 1.0.10, ognl 3.4.11. |
| 3 | maven | `/kiteclass/kiteclass-gateway` | 3 | Boot **3.5.13** → bump to 3.5.14 pending | Own `<parent>` block, not inherited. |
| 4 | npm | `/kitehub/kitehub-frontend` | 8 | next **15.1.6** (pinned, do NOT bump) | See `next` ignore — RSC regression in 15.1.7+. |
| 5 | npm | `/kiteclass/kiteclass-frontend` | 8 | next **15.1.5** (pinned) | Same `next` ignore. |
| 6 | github-actions | `/` | 3 | — | Single `actions` group covers all workflows. |

---

## 3. Group design (shared across ecosystems)

Pilot validated these groupings for kiteclass-core; expansion reuses the same pattern where applicable.

### Maven groups

| Group | Patterns |
|-------|----------|
| `spring-boot` | `org.springframework.boot*`, `org.springframework:*`, `org.springframework.security:*`, `org.springframework.cloud:*` (gateway only) |
| `apache-commons` | `commons-*`, `org.apache.commons:*` |
| `aws-sdk` | `software.amazon.awssdk:*` (kiteclass-core only) |
| `document-generation` | `com.openhtmltopdf:*`, `org.apache.poi:*`, `ognl:*` (kiteclass-core only) |
| `testing` | `org.testcontainers:*`, `org.junit.jupiter:*`, `org.assertj:*`, `org.mockito*`, `io.github.resilience4j:*`, `org.springframework.security:spring-security-test` |

### Npm groups

| Group | Patterns |
|-------|----------|
| `next-ecosystem` | `next-*`, `@next/*`, `@vercel/*` |
| `react-ecosystem` | `react`, `react-*`, `@react-*`, `@types/react*` |
| `testing` | `vitest`, `@vitest/*`, `@playwright/*`, `@testing-library/*`, `jsdom`, `msw` |
| `dev-tooling` | `eslint`, `eslint-*`, `@eslint/*`, `prettier`, `prettier-*`, `typescript`, `@types/*`, `tailwindcss`, `tailwindcss-*`, `@tailwindcss/*`, `autoprefixer`, `postcss`, `postcss-*` |

### github-actions group

Single `actions` group matching `*` (covers all third-party actions across all workflows).

---

## 4. Global ignores

All ecosystems ignore `version-update:semver-major` — major bumps stay on a manual review path, not auto-merged into the group PR.

**Named ignores:**

| Dependency | Reason | Source |
|------------|--------|--------|
| `next` (both frontends) | 15.1.7+ breaks JsonLd prerender via `Array.prototype.toJSON` regression | Memory `feedback_nextjs_rsc_array_regression.md` + GAP-204 |

---

## 5. Concurrency on `docker-build-push.yml`

Paired with expansion (same PR #486) because Monday morning with 3-8 Dependabot merges in ~5 min would otherwise fire 3-8 full Docker build-and-push runs. Only the latest ECR image matters.

```yaml
concurrency:
  group: docker-build-push-${{ github.ref }}
  cancel-in-progress: true
```

Grouping by `github.ref` means:
- Push on `main` cancels any in-progress run on `main`
- PR / tag runs are grouped independently (don't starve each other)

---

## 6. Projected Monday 2026-04-28 run

Conservative estimate based on current drift:

| Ecosystem | Expected PRs | Notes |
|-----------|:------------:|-------|
| `/kitehub` maven | 1–2 | At minimum the `spring-boot` group (3.5.13 → 3.5.14). |
| `/kiteclass/kiteclass-core` maven | 0–2 | Already caught up via pilot; could see small testing-group patches. |
| `/kiteclass/kiteclass-gateway` maven | 1–2 | `spring-boot` group (3.5.13 → 3.5.14). |
| `/kitehub/kitehub-frontend` npm | 2–4 | `dev-tooling` likely; `testing`, `react-ecosystem` possible. |
| `/kiteclass/kiteclass-frontend` npm | 2–4 | Same categories as above. |
| `/` github-actions | 0–1 | Workflows recently tuned; low drift. |

**Total projected:** ~6–15 PRs across 6 ecosystems, all grouped. If actual count exceeds 20, escalate to rollback (§8).

---

## 7. Watch list — known outdated versions (will be resolved Monday)

Per IDE `BOOT_VERSION_VALIDATION_CODE` diagnostics at expansion time:

- `kitehub/pom.xml` — Boot 3.5.13 → 3.5.14 available
- `kiteclass/kiteclass-gateway/pom.xml` — Boot 3.5.13 → 3.5.14 available
- (All `kitehub/kitehub-*` children inherit from `kitehub/pom.xml` → single bump covers them)

These are **expected** to land as Monday's first Dependabot PR batch (spring-boot group per ecosystem).

---

## 8. Rollback criteria

Revert PR #486 (or per-ecosystem flip back to `open-pull-requests-limit: 0`) if **any**:

| Trigger | Threshold |
|---------|:---------:|
| PR count on Monday exceeds projection | **>20 PRs** within 30 min of scheduled run |
| Breaking build introduced | **≥1** Dependabot-authored PR merged → main CI fails |
| Reviewer overload | Weekly Dependabot PR merge latency median **>3 days** for 2 weeks consecutive |
| Groups bundling wrong | A single group PR spans **>15** deps making review impractical |

Rollback path: revert commit on PR #486, or flip offending ecosystem's `open-pull-requests-limit` back to 0 and close outstanding PRs.

---

## 9. Follow-ups

| # | Item | Trigger |
|:-:|------|---------|
| 1 | Observe first Monday run + file retro | After 2026-04-28 |
| 2 | Tune groups if bundling wrong (per rollback trigger §8) | Case-by-case |
| 3 | Consider `@dependabot merge` auto-merge for patch-only groups with green CI | Post-retro, if Monday's PRs all merge clean |
| 4 | Review `next` ignore when GAP-204 / RSC regression upstream fix lands | Watch next.js release notes |
| 5 | Stop ignoring semver-major for selected low-risk ecosystems (github-actions, dev-tooling) | Post-retro, case-by-case |

---

## 10. Related

- PR #479 — Dependabot pilot (this plan's precursor)
- PR #485 — Docker workflow narrow PR trigger (related CI hygiene)
- PR #486 — Full expansion + concurrency (this plan backs)
- Analysis: `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`
- Rule: `.claude/rules/output-review-mandate.md` — health-check standards
- Memory: `feedback_dependabot_first_run.md` — 3-stage enable policy (complete)
- Memory: `feedback_nextjs_rsc_array_regression.md` — why `next` is ignored
- Memory: `feedback_dependabot_pnpm_transitive.md` — pnpm transitive caveat
- Gap: GAP-202 (`/repo-status` security checks)
- Gap: GAP-203 (pom.xml CVE fixes — original Dependabot driver)
- Gap: GAP-204 (npm alerts, next.js RSC regression)

---

## 11. Log

- 2026-04-24 — Plan created at expansion time (PR #486). Pilot closed successfully (5/5 PRs merged within 1 h). Expansion to 6 ecosystems + docker concurrency shipped in same PR.
