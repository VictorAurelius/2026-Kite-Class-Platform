# Round 1 Design Bundle — Archive

**Archived:** 2026-04-29
**Source:** `claude.ai/design` handoff bundle (gzipped tar fetched 2026-04-29 to `/tmp/anthropic-design/kite-design-system/`)
**Reason:** Preserve historical reference. `/tmp/` is volatile across system restarts; this archive ensures Round 1 outputs survive.

## What's here

| Folder | Direction | Purpose | Reusability for Round 2 |
|--------|-----------|---------|------------------------|
| `kitehub-story/` | A — Marketing storytelling | Round 1 v1 of marketing landing with kite character + scroll storytelling | **Future Wave 3** Direction A polish (defer per `dossier/08-direction-decisions.md` §3) |
| `ai-branding/` | C — Original playground | Round 1 v1 of AI Branding playground (free-form prompt prototype) | Reference only — Round 2 refactors to 6-step wizard (NOT playground) per `ai-branding-guidelines.md` §4.1 |
| `mobile-app/` | D — Native mobile | Round 1 v1 of native iOS/Android frame mockups | Reference only — Round 2 pivots to web responsive PWA-grade (per `dossier/08-direction-decisions.md` §2). Native deferred until post-PMF. |

## What was NOT archived

- `kiteclass/` and `kitehub/` Round 1 recreations of production code → **discarded** (production Next.js code in `kiteclass/kiteclass-frontend/` and `kitehub/kitehub-frontend/` is authoritative)
- `kiteclass-pro/` Round 1 v1 → **moved to `ui_kits/kiteclass-pro-v2/_v1-baseline/`** as Agent A starting point for Round 2 v2
- `colors_and_type.css` and `assets/*.svg` → **promoted to `ui_kits/_shared/`** as single source of truth
- `preview/` 21 design system cards (HTML) → not archived (specs captured in `dossier/06-quality-bar.md` rubric)

## How to use this archive

When picking up Direction A or revisiting Direction C/D in future waves:
1. Read this folder's contents as design intent reference
2. Apply Round 2 dossier (`documents/02-architecture/design-system/dossier/`) for current personas / VN UX / quality bar
3. Build new kit fresh in `documents/02-architecture/design-system/ui_kits/{new-kit-name}/`
4. **Do NOT** symlink from this archive to active ui_kits/ folder — archive is read-only reference

## Original Round 1 bundle README

See `ROUND-1-BUNDLE-README.md` next to this file — verbatim copy of the original handoff README explaining how the bundle was meant to be consumed.

## Round 1 → Round 2 lineage

```
Round 1 (Claude Design 2026-04-29)
  ↓
  ├─ Direction A kitehub-story → ARCHIVED here (defer Wave 3)
  ├─ Direction B kiteclass-pro v1 → MIGRATED to ui_kits/kiteclass-pro-v2/_v1-baseline/ (Wave 1 Agent A extends)
  ├─ Direction C ai-branding playground → ARCHIVED here (Round 2 refactors to wizard, defer Wave 2)
  ├─ Direction D mobile-app → ARCHIVED here (Round 2 pivots to PWA, native deferred post-PMF)
  ├─ kiteclass/, kitehub/ recreations → DISCARDED (production code authoritative)
  └─ Shared assets (colors_and_type.css, *.svg) → PROMOTED to ui_kits/_shared/
```
