---
audience: dev
---

# GAP-802 — BE↔FE contract drift detection: auto-catch URL-path / env-domain / FE-build class bugs

**Status:** 🟡 PARTIAL (80% — #1/#2/#4/#5 shipped 2026-05-28; #3 E2E deferred)
**Priority:** 🟠 P1 (META — force-multiplier per `meta-gap-priority.md`)
**Domain:** Meta / Testing (cross-cutting BE + FE + CI)
**Found:** 2026-05-28 (user-flagged after GAP-801 — "sẽ còn rất nhiều lỗi tương tự đúng không? làm sao flow sau Claude bắt được lỗi FE khi chạy API/E2E?")
**Affects:** Every transactional-email link, BE-constructed FE URL, config default, FE PR build — the whole BE↔FE contract surface.

## Problem

The Flow 1 email cascade (GAP-797 var-name → GAP-800 html markup → GAP-801 URL-path + env-domain) + the GAP-801 FE-build Suspense failure are all the SAME class: **BE↔FE contract drift that no automated test asserts**, surfaced only by manual browser walk. API-layer tests + unit tests + `eslint` all passed while the actual user flow was broken.

Why each layer missed it:
1. **API tests isolated** — `exchange-claim-code` endpoint works; nothing follows the email link to the FE route → `/signup/beta` 404 invisible.
2. **Unit test codified the wrong value** — `BetaAccessServiceApprovalEmailTest` asserted `.startsWith("/signup/beta")` (the bug), not validated against the FE route registry.
3. **Seed/API walk skipped the FE leg** — API-layer verify ≠ feature verify (`feature-ship-runtime-walk-mandate.md`).
4. **`eslint` ≠ `next build`** — useSearchParams-without-Suspense passes lint, fails production build (caught by CI Docker build, not local lint).

## Proposed Fix — 4 detection mechanisms (build #1+#2+#4 first; #3 later)

| # | Mechanism | Catches | Cost | Notes |
|---|---|---|---|---|
| 1 | **Email-link resolvability smoke** — after triggering any transactional email, extract every link → `curl` → assert non-404 + (on local) non-prod-domain | URL path 404 (GAP-801) + env domain drift | Low | Add to seed script + a `scripts/smoke-email-links.sh`; runs in API flow, no browser |
| 2 | **Static BE-URL ↔ FE-route check** (CI, no stack) — grep BE URL builders (`String.format(".../...")`, email signupUrl/inviteUrl paths) → assert each path exists as a Next.js route under `kitehub-frontend/src/app/**` (+ kiteclass-frontend) | `/signup/beta` ≠ `/beta-signup/code` at CI time | Medium | `scripts/check-be-fe-url-contract.sh` |
| 3 | **E2E full-flow** (Playwright) — signup→approve→open email→click link→FE page render→form prefilled→submit | Whole deterministic flow (per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion: GAP-801 → 1 spec) | High | Needs FE E2E infra |
| 4 | **FE production-build local-verify mandate** — FE PR MUST run `pnpm --filter <fe> build` (not just `eslint`) before push | useSearchParams/Suspense + prerender errors (GAP-801 FE part) | Low | Extend `api-contract-change-caller-sweep.md` / add FE-specific rule; CI Docker-build already catches but local-verify saves round-trip |
| 5 | **Env-default audit extension** — `audit-env-coverage.sh` flag config default = PROD value (kitehub.me) with no local override → local dead-link | env domain class (GAP-801 part 3) | Low | Extend existing script |

## Acceptance Criteria

- [x] `scripts/smoke-email-links.sh` (#1) — extract + curl links from MailHog emails, assert non-404 + non-prod-domain-on-local. 5/5 fixture tests PASS, shellcheck clean.
- [x] `scripts/check-be-fe-url-contract.sh` (#2) — BE URL-builder paths ↔ FE app-router routes (route-group stripping + `[id]` wildcard); CI WARN job `be-fe-url-contract` in `quality-code.yml`. 11/11 tests PASS. Surfaced real finding `/reset-password` → GAP-803.
- [x] FE-build local-verify rule (#4) — `.claude/rules/fe-build-local-verify.md` v1.0.0 mandating `pnpm --filter <pkg> build` before FE PR push (catches Suspense/useSearchParams prerender bailout). rules-index.csv + output-review-mandate §3 rows added.
- [x] `audit-env-coverage.sh` extended (#5) — CHECK B flags prod-domain defaults without local override. 9/9 tests PASS. Surfaced 3 real local-deadlink vars → GAP-803.
- [ ] E2E Flow-1 spec (#3) — **DEFERRED** to GAP-803 / future wave when FE E2E (Playwright signup→approve→email→click→render→submit) infra lands. Per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion.

## Related

- **GAP-797 / GAP-800 / GAP-801** — the 3 Flow 1 email-cascade bugs that motivated this
- `feature-ship-runtime-walk-mandate.md` (API verify ≠ feature verify — walk the FE leg)
- `e2e-rst-test-layer-boundary.md` §3 (RST→E2E promotion mandate)
- `api-contract-change-caller-sweep.md` (run tests/build not just compile/lint — extend to FE build)
- `production-env-config-registry.md` + `audit-env-coverage.sh` (#5 extension target)
- `local-fix-production-parity-check.md` (config-shape parity — GAP-801 env part)
- `meta-gap-priority.md` §3 (META force-multiplier — 1 check catches the class forever)

## Log

- **2026-05-28 (PARTIAL 80%):** Shipped #1+#2+#4+#5 via 4 parallel Opus agents (worktree-disjoint, `agent-model-opus-default.md`). Tests: 5+11+9 = 25 fixture tests PASS, shellcheck clean. #2 CI wired WARN-mode (`be-fe-url-contract` job, quality-code.yml) — flips HARD STOP after GAP-803 findings resolved. #3 E2E deferred (FE E2E infra not ready) → GAP-803. **Detectors validated by surfacing 2 real findings** (filed GAP-803 per `audit-to-gap-pipeline.md`): (1) `/reset-password` BE link (`PasswordResetService:80`) has no kitehub-frontend route → 404 same class as GAP-801; (2) 3 env vars (`PARENT_PORTAL_REDEEM_BASE_URL`, `RESEND_FROM_EMAIL`, `KITEHUB_STAFF_INVITATION_BASE_URL`) default to prod domain without local override → local email dead-link. Note: `/signup/beta` + `signup-base-url` were false alarms on the agents' stale base (bd2d732e); resolved after rebase onto main `a0cb5b47` (#1956 GAP-801 fix). Stays PARTIAL per `gap-done-discipline.md` §3 (deferred AC #3 + findings tracked GAP-803).
- **2026-05-28:** Filed from user frustration post-GAP-801: BE↔FE contract drift (URL path, env domain, field-name, FE build) repeatedly slips past API + unit + lint, caught only by manual walk. 4-5 detection mechanisms proposed; #1 (email-link smoke) + #2 (static BE-URL↔FE-route) + #4 (FE build local-verify) + #5 (env-default audit) are cheap CI/API-runnable catches; #3 (E2E) canonical but deferred. META force-multiplier: build once → catch the whole class prospectively.
