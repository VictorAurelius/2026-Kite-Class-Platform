# @kite/shared-ui

**Last Updated:** 2026-04-30
**Status:** Phase 1 — workspace bootstrap (empty scaffolding)

Cross-cutting UI components shared between `kiteclass-frontend` and `kitehub-frontend`. Decision: pnpm workspace package per [ADR-024](../../documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md).

## Install (consumed automatically via workspace)

Both frontends already declare `"@kite/shared-ui": "workspace:*"` in their `package.json` and have `transpilePackages: ['@kite/shared-ui']` in `next.config.js`. No additional setup needed in consumer apps — `pnpm install` from repo root resolves everything.

## Usage

```ts
// In kiteclass-frontend or kitehub-frontend code:
import { SHARED_UI_VERSION } from '@kite/shared-ui';

// Phase 2+ (when components added):
// import { ConfirmDialog } from '@kite/shared-ui';
```

## Tokens

```css
/* In app's globals.css */
@import '@kite/shared-ui/styles/tokens.css';
```

## Phase roadmap (per ADR-024 §Implementation Notes)

| Phase | Scope | Status |
|:-----:|-------|:------:|
| 1 | Workspace bootstrap (this PR) — empty package + wiring | 🟡 ACTIVE |
| 2 | First component end-to-end (validates workflow) | 🔵 OPEN |
| 3 | Bucket 1: G1 + G2 + G3 + G4 (KC teacher-facing + import) | 🔵 OPEN |
| 4 | Bucket 2: G5 + G6 + G7 + G8 (payment + invite + calendar) | 🔵 OPEN |
| 5 | Bucket 3: G9 + G10 + G11 + G12 (lifecycle + payment + theme + bulk) | 🔵 OPEN |
| 6+ | D1..D10 modal/dialog catalog (post-GAP-279) | 🔵 OPEN |

## Component organization (planned, per Phase 2+)

```
packages/shared-ui/
├── package.json           # @kite/shared-ui
├── tsconfig.json
├── README.md              # this file
├── src/
│   ├── index.ts           # public API barrel
│   ├── components/
│   │   ├── G1-bulk-import-dropzone/
│   │   │   ├── index.tsx
│   │   │   ├── states.tsx
│   │   │   └── spec.md         # mirrors ui_kits/components/G1/spec.md
│   │   ├── G2-attendance-roster/
│   │   └── ... (G3..G12, D1..D10 added per port wave)
│   ├── hooks/
│   ├── types/
│   └── styles/
│       └── tokens.css     # design tokens (mirrors ui_kits/_shared/colors_and_type.css)
└── vitest.config.ts       # added in Phase 2
```

## Gotchas

- **No build step in v1** — package consumed as TypeScript source via Next.js `transpilePackages`. Avoids tsc/build orchestration complexity. Storybook + tsup added later if external publishing emerges.
- **Worktree implications** — pnpm symlinks workspace deps. Agents in `isolation: worktree` should run `pnpm install --frozen-lockfile` after worktree creation (per `feedback_parallel_agent_strategy.md` rule #6).
- **No runtime in Phase 1** — package importable but only exports `SHARED_UI_VERSION` const. Calling code should not depend on Phase 1 surface.

## References

- ADR: `documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md`
- Tracking gap: `documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md`
- Component catalog (HTML source): `documents/02-architecture/design-system/ui_kits/components/G*/`
- Dossier: `documents/02-architecture/design-system/dossier/04-component-gaps.md` (G1..G12), `12-modal-dialog-inventory-{kc,kh}.md` (D-prefix)
- Governance: `.claude/rules/design-layer-coverage.md` v1.0.0 (4-layer V-model)
