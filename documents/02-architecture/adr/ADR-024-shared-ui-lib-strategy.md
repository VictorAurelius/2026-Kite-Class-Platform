# ADR-024: Shared UI Library Strategy — pnpm Workspace Package

**Status:** PROPOSED
**Date:** 2026-04-30
**Deciders:** @nguyenvankiet (solo-dev)
**Related Gap(s):** GAP-273 (12 components shared lib — BLOCKING for 7 kit-port gaps GAP-266..272 + 6 audit-driven gaps GAP-274..280)

## Context

Track 2 production port (per `wave-2026-04-29-ui-kits-round-3.md` §"Deferred separate track" pattern) requires porting **12 G* components + ~10 D* modal/dialog catalog + 7 kit-specific UI sets** from HTML prototypes to React/Next.js. These components are inherently cross-cutting:

- G6 Invoice Detail used by KC `/billing/[id]`, KH `/billing/payment/[id]`, KH `/admin/payments`
- G7 Parent Invite used by KC `/parent-invite/[token]`, admin invite modal
- G9 Instance Lifecycle used by KH `/instances/[id]`, AI Branding wizard
- D1 Generic Confirm Dialog used by both apps

Per `design-layer-coverage.md` §2.4 (Track 2 production port checklist), Layer 3 (詳細設計 / Internal design) requires "shared lib decision (Track 2) — Option A/B/C" before kicking off port wave. Without ADR, bucket execution would diverge across worktrees.

**Repo state (verified 2026-04-30):**
- No root `package.json` workspace config exists
- Two standalone Next.js apps: `kiteclass/kiteclass-frontend/` + `kitehub/kitehub-frontend/`
- Each frontend uses pnpm (lockfiles present)
- Per `feedback_dependabot_pnpm_transitive.md`: pnpm transitive deps already a known concern — workspace setup must work with pnpm
- Round 2/3 HTML kits + dossier `04-component-gaps.md` + `12-modal-dialog-inventory-{kc,kh}.md` + `14-common-components-inventory-{kc,kh}.md` enumerate the components

**Forces:**
- 12 components × duplicate-per-app = 24 components → defeats Round 2/3 catalog purpose, drift inevitable
- Workspace tooling adds setup cost (~1-2 hours) but saves weeks of long-term drift
- Both apps already pnpm-based — workspace is the natural extension
- Next.js 15 supports workspaces well (transpilePackages config)
- Build orchestration needed (turborepo OR pnpm filter)
- TypeScript path aliases need workspace-aware setup

## Decision

**We will create `packages/shared-ui/` as a pnpm workspace package** containing all cross-cutting UI components ported from Round 2/3 HTML kits. Both `kiteclass-frontend` and `kitehub-frontend` consume it via workspace dependency.

**Concretely:**

1. **Repo-root `package.json`** with `"workspaces": ["kiteclass/kiteclass-frontend", "kitehub/kitehub-frontend", "packages/*"]`
2. **`pnpm-workspace.yaml`** mirroring the workspaces array
3. **`packages/shared-ui/`** with own `package.json` (`@kite/shared-ui`), `tsconfig.json`, `src/{components,hooks,types,styles}/`, `vitest.config.ts`
4. **Both frontends** add `"@kite/shared-ui": "workspace:*"` dependency
5. **Next.js config** in both frontends: `transpilePackages: ['@kite/shared-ui']`
6. **No build step on shared-ui** for v1 — consumed as TypeScript source via `transpilePackages` (avoids tsc/build orchestration complexity). Add `tsup` build only if needed for Storybook/external publishing later
7. **Imports** from frontends: `import { ConfirmDialog } from '@kite/shared-ui'` (ergonomic, IDE-friendly)
8. **CI:** existing pnpm install commands at frontend levels continue to work (workspace auto-detected)

**Component organization in `packages/shared-ui/src/`:**

```
packages/shared-ui/
├── package.json           # @kite/shared-ui
├── tsconfig.json
├── src/
│   ├── index.ts           # re-exports public API
│   ├── components/
│   │   ├── G1-bulk-import-dropzone/
│   │   │   ├── index.tsx
│   │   │   ├── states.tsx        # state demo helpers (Storybook later)
│   │   │   └── spec.md           # mirrors ui_kits/components/G1/spec.md
│   │   ├── G2-attendance-roster/
│   │   ├── ... (G3..G12, D1..D10 added per port wave)
│   ├── hooks/
│   │   ├── use-confirm-dialog.ts
│   │   └── ...
│   ├── types/
│   │   ├── currency.ts            # VN format types
│   │   └── ...
│   └── styles/
│       └── tokens.css             # mirrors ui_kits/_shared/colors_and_type.css
└── vitest.config.ts
```

## Consequences

### Positive

- **Single source of truth** — 12 components shared across both apps, no drift
- **Bug fixes apply 1x** — fix in `@kite/shared-ui`, both apps get it
- **Type safety across boundary** — TypeScript types exported from package consumed by both frontends with full IntelliSense
- **Refactor friendly** — rename a prop = TS error in both apps immediately
- **Storybook-ready** — single Storybook root for shared-ui (deferred but path is clear)
- **Test centralization** — Vitest at package root tests components once
- **Clear dependency direction** — frontends depend on shared-ui, not vice versa (no circular)
- **Aligns with R2/R3 dossier catalog** — `04-component-gaps.md` G* IDs map 1:1 to package directories

### Negative

- **Workspace setup cost** — ~1-2 hours for first-time wiring (`package.json` + `pnpm-workspace.yaml` + `transpilePackages` + path aliases + CI verification)
- **Build complexity slightly higher** — Next.js needs `transpilePackages` flag; future tooling decisions (turborepo/nx) deferred but possible
- **Cross-app type sharing** — must avoid leaking app-specific types into shared (boundary discipline needed)
- **CI configuration touch** — existing GitHub Actions workflows may need workspace awareness (e.g., dependency change detection)
- **Per `feedback_dependabot_pnpm_transitive.md`** — Dependabot at workspace root won't auto-fix transitive deps; manual `pnpm.overrides` still needed (existing concern, not new)
- **Worktree implications** — pnpm symlinks workspace deps; agents in `isolation: worktree` mode may see split node_modules. Mitigation: agents run `pnpm install --frozen-lockfile` after worktree creation

### Neutral

- **Next.js 15 transpilePackages** is a 1-line `next.config.js` change — already supported pattern in modern monorepos
- **No new external dependencies** for v1 — pure pnpm workspace, no turborepo/nx until needed
- **CI cache strategy** — pnpm-lock.yaml at repo root will be primary cache key; existing per-app caches still work

## Alternatives Considered

### Alternative B: Per-app duplication (no workspace)

Each frontend has its own copy of components in `kiteclass-frontend/src/components/shared/` + `kitehub-frontend/src/components/shared/`.

**Pros:**
- Simplest setup — no workspace tooling at all
- Each app evolves independently
- Existing Dependabot config requires no changes
- No cross-boundary type sharing risk

**Cons:**
- **12 → 24 components effective** (each shared piece duplicated)
- **Drift inevitable** — bug fix in KC version doesn't propagate to KH
- **Round 2/3 catalog purpose defeated** — `04-component-gaps.md` G* IDs become per-app, no longer shared identity
- **Maintenance burden compounds** — every Track 2 component-touch PR needs 2x review, 2x test
- **Anti-pattern per `design-patterns.md` §3.8 Shotgun Surgery** — change requires modifying ≥5 files

**Rejected because:** the trade-off "save ~2h setup vs. spend ~weeks of drift maintenance over Track 2's 15-20 week duration" is clearly net-negative. R2/R3 catalog is explicit cross-cutting — duplicate path defeats the entire 12-component shared-lib design intent.

### Alternative C: Server-component + client-component split (orthogonal)

Use Next.js 15 server-component pattern: shared server-only components in one location, client-only in another, both within each app's `src/`.

**Pros:**
- Clean Next.js 15 idiom alignment
- Server components cannot leak client-only imports

**Cons:**
- **Doesn't address WHERE shared lives** — components still need a home (per-app vs workspace)
- **Mostly orthogonal to A vs B** — useful pattern WITHIN whichever location chosen
- **G* components are mostly client (interactive)** — server/client split adds complexity without solving the drift problem

**Rejected because:** Option C is a complementary pattern, not an alternative location. Will adopt server/client distinction WITHIN shared-ui package as components warrant, but Option A (workspace location) is still required for the cross-cutting concern.

### Alternative D: Git submodule with separate repo

Extract `shared-ui` to its own git repo, included as submodule in main repo.

**Pros:**
- Fully decoupled lifecycle, can publish externally
- Clear ownership boundary

**Cons:**
- **Submodule ergonomics painful** — extra clone/update step, agents in worktrees may miss submodule sync
- **Cross-repo PR coordination** — every shared-ui change requires PR in 2 repos
- **CI complexity** — submodule init + cache + version pin
- **Premature optimization** — solo-dev mode + Track 2 in-progress, no external consumer

**Rejected because:** premature for solo-dev mode, ergonomics regression. Can revisit when external consumer or 3+ apps emerge.

## Implementation Notes

### Migration strategy (5 phases, can parallelize via wave-pack)

1. **Phase 1 — Workspace bootstrap** (foundation PR, ~1-2h)
   - Create `package.json` at repo root with workspaces array
   - Create `pnpm-workspace.yaml`
   - Create `packages/shared-ui/{package.json,tsconfig.json,src/index.ts}`
   - Add `transpilePackages: ['@kite/shared-ui']` to both `next.config.{ts,js}`
   - Add `"@kite/shared-ui": "workspace:*"` to both frontend package.json
   - Run `pnpm install` from repo root, verify both apps still build
   - Self-test: `pnpm --filter kiteclass-frontend build` + `pnpm --filter kitehub-frontend build` both pass
   - **No components yet** — just the empty package + wiring

2. **Phase 2 — First component (G1 or simplest)** (~1 wave-pack agent, ~50min)
   - Port one component (e.g., G1 Bulk Import Drop-zone) from HTML to React
   - Add to `packages/shared-ui/src/components/G1-bulk-import-dropzone/`
   - Re-export from `packages/shared-ui/src/index.ts`
   - Import + use in 1 page in `kitehub-frontend` (e.g., `(admin)/admin/instances` for bulk tenant import demo)
   - Vitest tests + Playwright/RTL coverage
   - Validates the workflow end-to-end

3. **Phases 3-5 — Wave-pack 4 components per wave**
   - Bucket 1: G1+G2+G3+G4 (KC teacher-facing + import)
   - Bucket 2: G5+G6+G7+G8 (payment + invite + calendar)
   - Bucket 3: G9+G10+G11+G12 (lifecycle + payment + theme + bulk)
   - Each bucket = 1 wave-pack agent (~50-75min)
   - Per `feedback_parallel_agent_strategy.md` rule #9: 3-bucket wave fits cleanly

4. **Phase 6+ — D* modal catalog (post-GAP-279 wave)**
   - 10 modals D1..D10 added to `packages/shared-ui/src/dialogs/`
   - Same wave-pack pattern

### Rollback plan

If workspace setup proves blocking:
- Phase 1 PR is reversible (delete `packages/shared-ui/`, remove workspaces from root package.json, revert transpilePackages, run `pnpm install`)
- Frontends pre-Phase-2 don't import from `@kite/shared-ui` → rollback is clean
- Fallback path: per-app duplication (Alternative B). Cost = manual sync going forward

### Feature flags

Not applicable (build-time decision).

### Monitoring / success criteria

- ✅ Both frontends build successfully after Phase 1 (verified via CI)
- ✅ First imported component (Phase 2) renders identically to HTML prototype (visual diff acceptable)
- ✅ Type errors propagate from `@kite/shared-ui` → both frontends (touch a prop type, see TS errors in both apps' `tsc --noEmit`)
- ✅ `pnpm install` from repo root resolves all workspace deps without error
- ✅ Existing CI workflows pass without major restructure (`shellcheck`, `ruff`, frontmatter checks orthogonal)
- ⚠️ Watch for: Next.js HMR with workspace deps (occasionally needs `--turbo` flag adjustment; mitigation = document)

## References

- Design pattern used: `.claude/rules/design-patterns.md` §1 (Strategy/Adapter for cross-app boundary), §3.8 (avoiding Shotgun Surgery)
- Governance rule: `.claude/rules/design-layer-coverage.md` §2.4 — Layer 3 (詳細設計) requires this ADR before Track 2 port
- Related ADRs:
  - ADR-016 (FE↔BE contract strategy) — orthogonal but adjacent FE concern
  - ADR-020 (vendor/client package naming) — naming pattern reused for `@kite/shared-ui`
  - ADR-021 (per-module outbox vs shared lib) — backend equivalent decision; precedent that "per-module" lost to "shared" when cross-cutting
- Related rules: `feedback_dependabot_pnpm_transitive.md` (pnpm overrides at root), `feedback_parallel_agent_strategy.md` rule #6 (worktree pnpm install)
- Related gaps: GAP-273 (BLOCKING — this ADR unblocks), GAP-266..272 (depend on shared-ui), GAP-274..280 (depend on shared-ui)
- Round 2/3 dossier sources:
  - `documents/02-architecture/design-system/dossier/04-component-gaps.md` — G1..G12 catalog
  - `documents/02-architecture/design-system/dossier/12-modal-dialog-inventory-{kc,kh}.md` — D* catalog seed
  - `documents/02-architecture/design-system/dossier/14-common-components-inventory-{kc,kh}.md`
  - `documents/02-architecture/design-system/ui_kits/components/G*/spec.md`
- External:
  - Next.js 15 `transpilePackages` docs: https://nextjs.org/docs/app/api-reference/next-config-js/transpilePackages
  - pnpm workspaces: https://pnpm.io/workspaces

## Log

- 2026-04-30 — Initial proposal. Triggered by `design-layer-coverage.md` §2.4 Layer 3 check on GAP-273: shared-lib strategy ADR was identified as ⚠️/❌ blocker before Track 2 port wave-pack can kick off. ADR proposes Option A (pnpm workspace package). User to review + flip Status to ACCEPTED before GAP-273 wave kickoff.
