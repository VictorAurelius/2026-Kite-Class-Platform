# GAP-366: `frontend-standards.md` extend — Kit as Source of Truth + dossier cross-link

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 Meta (per `meta-gap-priority.md` §3 — meta gap on skill/rule force-multiplies every Track 2 port + future FE component PR)
**Domain:** Meta / Frontend governance / Skills
**Found:** 2026-05-06 (Wave 22 follow-up audit — coordinator review of UI kits standardization coverage)
**Affects:** `.claude/skills/frontend/frontend-standards.md` §3 Design System; downstream every new Track 2 port (GAP-269/271/etc.) and every new component PR

## Problem

User asked at Wave 22 (2026-05-06): "UI kits đã được chốt vẫn skill code, tạo mới frontend đã được cập nhật theo tiêu chuẩn UI của UI kits chưa?"

Audit found:

| Surface | Status |
|---|---|
| `frontend/frontend-standards.md` §3 Design System | ❌ Mentions Shadcn/Tailwind/Lucide; NO reference to `ui_kits/` as source-of-truth |
| Dossier cross-link in standards | ❌ No mention of `documents/02-architecture/design-system/dossier/` |
| Workflow "khi tạo component mới, match kit spec trước" | ❌ Not documented anywhere |
| `output-review-mandate.md` §3 row "Production FE port from kit" | ❌ Existing row is HTML kit prototypes only |

Without explicit standard, every Track 2 port + new component runs risk of drift from kit. Round 3 kits (kiteclass-student / kitehub-admin) shipped as canonical visual specs; production code doesn't reference them when generating new screens.

## Current State (verified 2026-05-06)

```bash
grep -i "ui_kits\|dossier\|kit-as-source\|design system" .claude/skills/frontend/frontend-standards.md
# → Only matches: "## 3. Design System" heading + "Design system / Shadcn" navigation entry
# → No mention of ui_kits, no dossier cross-link, no kit-first workflow
```

`design-layer-coverage.md` (project rule) mentions kit pointers but only governs scope-completeness audits, not per-PR FE workflow.

## Proposed Fix

Extend `.claude/skills/frontend/frontend-standards.md` §3 Design System with new subsection "3.1 Kit as Source of Truth" containing:

**a) Kit-first workflow** (mandatory step before creating new component or screen):
1. Check if `documents/02-architecture/design-system/ui_kits/{relevant-kit}/screens/` has the screen → match it
2. Check `dossier/04-component-gaps.md` for G* component → reuse spec if exists
3. If neither → file gap to add to kit FIRST, then port to production
4. Anti-pattern: "I'll just write a new component, kit can catch up later" — banned per this section

**b) Kit-to-production parity contract**:
- Visual hierarchy: production matches kit's layout grid + spacing scale (`_shared/colors_and_type.css` tokens)
- Color tokens: production uses HSL CSS vars derived from kit tokens (no hex literals in production except in token defs)
- Typography: production scale matches kit's type ramp
- Component library: production uses `@kite/shared-ui` package (per ADR-024 Track 2 Phase 1) for kit-derived components
- AC traceability: each AC item in kit `dossier/10-acceptance-criteria.md` → 1+ E2E test in production

**c) Cross-references** (added to standards doc):
- `documents/02-architecture/design-system/ui_kits/README.md` — kit catalog
- `documents/02-architecture/design-system/dossier/` — design system internal docs
- `.claude/rules/design-layer-coverage.md` — 4-layer scope completeness rule
- GAP-367 (companion gap — kit-production-parity skill)

**d) Workflow integration**:
- PR template checkbox for FE PRs touching `kiteclass-frontend/src/app/**` or `kitehub-frontend/src/app/**`: "Production code matches kit spec per `frontend-standards.md` §3.1; OR PR is kit-first (file kit gap before production)"
- Reviewer checklist: visual diff vs kit screenshot, AC traceability spot-check

## Acceptance Criteria

- [ ] `frontend-standards.md` §3.1 "Kit as Source of Truth" subsection added
- [ ] Kit-first workflow documented (4-step before creating new component)
- [ ] Kit-to-production parity contract documented (visual / color / type / component / AC)
- [ ] Cross-references to ui_kits/, dossier/, design-layer-coverage.md
- [ ] PR template extended with FE kit-parity checkbox
- [ ] `output-review-mandate.md` §3 row "Production FE port from kit" added (Standard = `frontend-standards.md` §3.1)
- [ ] Self-test: apply rule to GAP-269 spec phase (does GAP-269 AC list cite kit + dossier? — should after this gap)
- [ ] Memory entry `feedback_kit_as_source_of_truth.md` for session-loaded reinforcement

## Related

- Sister gap: **GAP-367** (kit-production-parity SKILL — companion automation layer to this manual standard)
- Existing rule: `.claude/rules/design-layer-coverage.md` (4-layer V-model — scope completeness)
- Existing rule: `.claude/rules/output-review-mandate.md` §3 (HTML/JSX prototypes row v1.2.0)
- Track 2 ports BLOCKED until this standard exists: GAP-269 (student), GAP-271 (admin), GAP-274/275 (marketing), GAP-266..280 (full Track 2)
- Visual regression drift policy: GAP-355 (covers prototype↔production sync, this gap covers the standard layer)

## Why Meta-P1 (not P0)

- Meta gap (skill/standard) — force-multiplies every future FE PR
- P1 (not P0) because no current production violation; future ports would drift without this. Track 2 Phase 2 (5 priority components) hasn't started yet — closing this gap before Phase 2 = ship right.
- Per `meta-gap-priority.md` §3 — Meta-P1 sits above Feature-P0 (priority 4 vs 6). Recommend close before any Track 2 port wave kicks off.

## Effort estimate

~6-8h. Single doc PR. Pair-eligible with GAP-367 (kit-production-parity skill) as 2-bucket meta wave.

## Log

- **2026-05-06:** Filed at Wave 22 closure per user question "UI kits đã được chốt, skill code/tạo frontend đã update theo chuẩn chưa?". Coordinator audit confirmed gap. Pair-recommended with GAP-367.
