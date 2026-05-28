---
audience: dev
---

# GAP-802 — BE↔FE contract drift detection: auto-catch URL-path / env-domain / FE-build class bugs

**Status:** 🔵 OPEN
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

- [ ] `scripts/smoke-email-links.sh` (#1) — extract + curl links from MailHog/sent emails, assert non-404 + non-prod-domain-on-local
- [ ] `scripts/check-be-fe-url-contract.sh` (#2) — BE URL-builder paths ↔ FE app-router routes; wire CI job
- [ ] FE-build local-verify rule (#4) — new/extended rule mandating `pnpm build` before FE PR push
- [ ] `audit-env-coverage.sh` extended (#5) — flag prod-domain defaults without local override
- [ ] E2E Flow-1 spec (#3) — deferred; file follow-up when FE E2E infra lands

## Related

- **GAP-797 / GAP-800 / GAP-801** — the 3 Flow 1 email-cascade bugs that motivated this
- `feature-ship-runtime-walk-mandate.md` (API verify ≠ feature verify — walk the FE leg)
- `e2e-rst-test-layer-boundary.md` §3 (RST→E2E promotion mandate)
- `api-contract-change-caller-sweep.md` (run tests/build not just compile/lint — extend to FE build)
- `production-env-config-registry.md` + `audit-env-coverage.sh` (#5 extension target)
- `local-fix-production-parity-check.md` (config-shape parity — GAP-801 env part)
- `meta-gap-priority.md` §3 (META force-multiplier — 1 check catches the class forever)

## Log

- **2026-05-28:** Filed from user frustration post-GAP-801: BE↔FE contract drift (URL path, env domain, field-name, FE build) repeatedly slips past API + unit + lint, caught only by manual walk. 4-5 detection mechanisms proposed; #1 (email-link smoke) + #2 (static BE-URL↔FE-route) + #4 (FE build local-verify) + #5 (env-default audit) are cheap CI/API-runnable catches; #3 (E2E) canonical but deferred. META force-multiplier: build once → catch the whole class prospectively.
