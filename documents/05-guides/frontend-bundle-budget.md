# Frontend Bundle Budget Guide

**Status:** Active (since 2026-04-26)
**Owner:** Frontend / Performance
**Applies to:** `kiteclass/kiteclass-frontend`, `kitehub/kitehub-frontend`
**Closes:** Part of GAP-236 (CI guardrail; page conversions deferred)
**Related:** GAP-127 (parent — bundle analyzer + 10 pages converted, Wave 7-Perf)

---

## 1. Why a budget?

GAP-127 already brought every route under 250 KB First Load JS via `optimizePackageImports`, AVIF/WEBP images, and 10 high-impact `next/dynamic` conversions. Without a CI guardrail, **future PRs can silently regress** a route past the budget — a single accidentally-static-imported chart library can add 100+ KB to a route.

This guide describes the budget guardrail that enforces a per-route ceiling at PR time so regressions get caught before merge.

---

## 2. The default

| Metric | Value |
|--------|-------|
| Default budget per route | **250 KB** First Load JS (gzipped) |
| Measurement | `gzip -6` of every JS chunk listed in `.next/(app-)build-manifest.json` for that route |
| When enforced | After `pnpm build` in `frontend-ci.yml` and `kitehub-frontend-ci.yml` |
| What fails CI | Any route exceeding its applicable budget |

The script lives at `kiteclass/kiteclass-frontend/scripts/check-bundle-budget.mjs` and `kitehub/kitehub-frontend/scripts/check-bundle-budget.mjs` (identical).

---

## 3. Quick commands

```bash
# Local — after building once
cd kiteclass/kiteclass-frontend
pnpm build
pnpm check:budget

# Equivalent
node scripts/check-bundle-budget.mjs

# Higher global budget for an experimental run
BUNDLE_BUDGET_KB=300 pnpm check:budget

# Machine-readable output (for follow-up tooling, dashboards, etc.)
node scripts/check-bundle-budget.mjs --json

# Run the unit tests (no build required)
pnpm test:budget
```

---

## 4. Tuning — three override layers

The script picks an effective budget for each route in this order (highest priority first):

1. **Per-route override** in `bundle-budget.json` at the FE app root
2. **Environment variable** `BUNDLE_BUDGET_KB`
3. **Config file `default`** in `bundle-budget.json`
4. Hardcoded **250 KB** default

### 4.1 Per-route override (recommended for legitimate exceptions)

Create `bundle-budget.json` at the FE app root (sibling of `package.json`):

```json
{
  "default": 250,
  "routes": {
    "/(public)/marketing/page": 350,
    "/(admin)/admin/payments/page": 320
  }
}
```

Use a per-route override **only when**:
- The route MUST stay synchronously imported (SEO / SSR / first-paint constraint)
- Page-level dynamic imports (`next/dynamic` for heavy children) won't fit (already tried)
- The added budget is documented in a gap with a deadline to reclaim it

The route key matches the path printed by `pnpm check:budget` (the App Router route, e.g. `/(public)/marketing/page`, NOT the URL `/marketing`).

### 4.2 Bumping the global default

Two paths:

- **Permanent (whole repo):** edit `bundle-budget.json` and set `"default": <kb>`. PR + reviewer sign-off. Update §5 baseline below.
- **Ephemeral (one CI run, debugging):** set `BUNDLE_BUDGET_KB=300` in the workflow step's `env`. Don't merge.

**Hard rule:** never raise the default above 300 KB without performance-audit approval. 300 KB First Load JS on 3G measures ~5s to interactive — past that, the budget stops protecting users.

---

## 5. Current baseline (2026-04-26)

Measured on `main` after a clean `pnpm build`, gzipped:

| App | Routes | Top route | Top 5 routes (KB) |
|-----|-------:|-----------|------------------|
| KiteClass FE | 52 | `/(dashboard)/courses/[id]/edit/page` (236.09 KB) | 236.09 / 235.80 / 235.51 / 234.80 / 234.21 |
| KiteHub FE | 38 | `/(admin)/admin/instances/page` (194.15 KB) | 194.15 / 173.66 / 170.37 / 169.65 / 169.00 |

All routes under 250 KB → CI green. **Headroom on KiteClass is ~14 KB** for the heaviest 10 routes — a single accidental sync import of `recharts` or `@radix-ui/react-popover` would push them over. That is exactly the regression class this guardrail catches.

---

## 6. CI integration

Both `frontend-ci.yml` and `kitehub-frontend-ci.yml` now run, after `pnpm build`:

```yaml
- name: Run bundle budget unit tests
  run: pnpm test:budget

- name: Check bundle budget (First Load JS per route)
  run: pnpm check:budget
```

`pnpm test:budget` runs the script's own unit tests via `node --test` (~13 cases). `pnpm check:budget` runs the script against the freshly built `.next/` and exits non-zero on any over-budget route. Either failing fails CI.

---

## 7. Troubleshooting

**Script reports way more KB than `next build` printed.** The Next CLI prints gzipped sizes; older versions of the script summed raw bytes. The current script gzips matching the CLI within ~3 % rounding. If you see >10 % drift, file a gap — likely a Next.js manifest schema change.

**`No build manifest found in .next/`.** You haven't run `pnpm build` yet, or you ran it from a different working directory. The script reads relative to its own location: `<fe-root>/.next/`.

**A new route regression-fails CI but the change is unrelated.** Run `pnpm check:budget --json` locally; compare against §5 baseline. Common causes:
- Accidental sync `import { Foo } from 'big-lib'` — convert to dynamic
- A shared layout pulled a new heavy dependency — affects every route
- A route that previously passed by 1-2 KB now fails because a transitive dep grew

Fix the root cause; if absolutely necessary, request a per-route override.

**The score the dashboard shows differs from `pnpm check:budget`.** The dashboard reports First Load JS as Next.js prints it (rounded to KB); the script prints with 2 decimals.

---

## 8. Out of scope (deferred per GAP-236)

- Conversion of remaining 44+ pages to `next/dynamic` for heavy modals/forms — separate PR
- Bundle analyzer baseline HTML reports committed to `documents/04-quality/audits/performance/` — separate PR
- Brotli measurement (CDN edge compresses brotli; gzip is the floor)

---

## 9. Log

- **2026-04-26** — Guide created as part of GAP-236 PARTIAL closure (CI guardrail). Foundation only; page conversions deferred to a follow-up PR. Baseline captured: KiteClass top 236 KB, KiteHub top 194 KB.
