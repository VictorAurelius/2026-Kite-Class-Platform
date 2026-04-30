# 15 — Error / Layout / Loading Inventory (cross-app)

**Total:** 15 files (5 KC + 10 KH) — error pages + layouts + loading states

**Use this when:** designing error UX or persona-specific layout. Round 2/3 HTML kits do NOT cover these system-level UI artifacts. Track 2 follow-up needed.

**Coverage legend:**
- ✅ explicit (kit has matching mock)
- ⚠️ implicit (covered indirectly)
- ❌ missing (no kit covers, follow-up GAP needed)

---

## KiteClass — 5 files

### Error pages

| File | Type | Scope | Persona | Kit-covered? |
|------|:----:|-------|:-------:|:------------:|
| `(public)/error.tsx` | Error boundary | Public-routes errors | Prospects | ❌ NONE — GAP-277 candidate |
| `(public)/not-found.tsx` | 404 | Public-routes 404 | Prospects | ❌ NONE — GAP-277 candidate |
| `(public)/loading.tsx` | Suspense | Public-routes loading | Prospects | ❌ NONE — implicit in skeletons but no dedicated kit |

### Layouts

| File | Scope | Persona | Kit-covered? |
|------|-------|:-------:|:------------:|
| `app/layout.tsx` | Root (RTL/LTR + theme provider) | All | ⚠️ implicit (kits set theme tokens) |
| `(auth)/layout.tsx` | Auth wrapper (centered card + brand mark) | All | ⚠️ implicit (kit login/register screens have own layout) |
| `(dashboard)/layout.tsx` | Sidebar + topbar shell (owner persona) | P2 Owner / Teacher | ✅ kiteclass-pro-v2 (sidebar + topbar shell visible) |

**Missing KC:** dashboard `error.tsx` + `loading.tsx` not present (graceful degradation gap). Auth-specific error/loading not present.

---

## KiteHub — 10 files

### Error pages

| File | Type | Scope | Persona | Kit-covered? |
|------|:----:|-------|:-------:|:------------:|
| `app/error.tsx` | App-level error boundary | All routes | All | ❌ NONE — GAP-277 candidate |
| `app/global-error.tsx` | Root-error fallback (production crash) | All | All | ❌ NONE — GAP-277 candidate |
| `app/not-found.tsx` | 404 | All routes | All | ❌ NONE — GAP-277 candidate |

**Note:** KH has both `error.tsx` (route-segment) AND `global-error.tsx` (app-level for runtime crashes). Best practice; KC is missing `global-error.tsx`.

### Layouts

| File | Scope | Persona | Kit-covered? |
|------|-------|:-------:|:------------:|
| `app/layout.tsx` | Root (theme provider + brand) | All | ⚠️ implicit |
| `(public)/layout.tsx` | Marketing site shell (header + footer) | Prospects | ❌ NONE — GAP-275 candidate |
| `(auth)/layout.tsx` | Auth wrapper | All | ⚠️ implicit |
| `(customer)/layout.tsx` | Owner self-service shell (sidebar + topbar) | P2 Owner | ✅ kitehub-pro-v2 |
| `(admin)/layout.tsx` | Platform admin shell (KH ops) | KH ops staff | ❌ NONE — GAP-278 candidate (different from kitehub-admin K-12 Principal kit) |

---

## Cross-app gaps summary

| Concern | KC | KH | Follow-up |
|---------|:--:|:--:|-----------|
| 404 page | ❌ public-only | ❌ app-level | GAP-277 |
| 500/error boundary | ❌ public-only | ❌ app + global | GAP-277 |
| Loading suspense | ❌ public-only | ❌ no file | GAP-277 (extend) |
| Auth layout | ⚠️ implicit | ⚠️ implicit | covered by GAP-276 (auth flows kit) |
| Public marketing layout | ⚠️ implicit (no `(public)/layout.tsx` in KC actually exists) | ❌ explicit gap | GAP-274 (KC) + GAP-275 (KH) |
| Platform admin layout | N/A (no platform admin in KC) | ❌ separate from K-12 | GAP-278 |

---

## Best-practice gaps (tech-debt, not coverage)

| Issue | Location | Severity |
|-------|----------|:--------:|
| KC missing `global-error.tsx` (root error boundary) | `kiteclass-frontend/src/app/` | 🟡 P2 |
| KC missing `not-found.tsx` at app root (only in `(public)/`) | KC dashboard 404 not caught | 🟡 P2 |
| KC missing route-segment `error.tsx` for `(dashboard)` and `(auth)` | Errors propagate to root | 🟡 P2 |
| KH missing `(admin)/error.tsx` | Platform admin runtime crash → root error | 🟡 P2 |
| Loading states inconsistent — only `(public)/loading.tsx` in KC, none in KH | Visible blank-screen during route change | 🟠 P1 |

These are tracked separately from kit-coverage gaps (they're product hardening, not UI design coverage).

---

## Log

- **2026-04-29:** Created during Wave UI Coverage Audit closure synthesis. Cross-app enumeration of error/layout/loading files. 15 files total. Best-practice gaps surfaced 5 tech-debt items (P1+P2) not catalogued elsewhere.
