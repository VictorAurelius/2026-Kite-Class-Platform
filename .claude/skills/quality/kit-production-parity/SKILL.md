---
name: kit-production-parity
description: "Dùng khi 'kit parity', 'kit production parity', 'review Track 2 port', 'port theo ui kit', 'kit vs production', 'back-port kit', hoặc trước khi merge PR port kit → production / production → kit. Verifies 4-layer V-model parity (要件定義 / 基本設計 / 詳細設計 / コンポーネント設計) giữa HTML kit prototype và production Next.js code. Bidirectional: kit→production port AND production→kit back-port (production led). Output report /4-layer to documents/04-quality/audits/parity/."
user-invocable: true
---

# /kit-production-parity — Kit ↔ Production 4-Layer Parity Review

Tier 2 automation companion cho standard `frontend/frontend-standards.md` §3.1 "Kit as Source of Truth". Verifies parity between an HTML kit screen (`documents/02-architecture/design-system/ui_kits/**`) and its production Next.js counterpart, structured along the Japanese 4-layer V-model (per `.claude/rules/design-layer-coverage.md` §2). Sister to `quality/ui-review/SKILL.md` (scores ONE side /128 holistically) — this skill compares TWO sides for **parity** (a screen can score 110/128 yet have completely different UX vs kit).

## When to use

- **Track 2 port PR** (kit → production Next.js) — verify production matches kit spec.
- **Production-first → kit back-port PR** (production shipped first, e.g. Wave 78 public pages / `kiteclass-public` #2326) — verify kit reconciled to live production state.
- **Kit-derived component PR** — new production component matching a kit `G*` screen.
- **Port retro audit** — periodic drift check on already-ported screen ↔ kit pairs.

## When NOT to use

- Single-side visual score → `quality/ui-review/SKILL.md` (/128, Next.js dev server) or `quality/ui-review-prototype/SKILL.md` (/128, static kit HTML).
- Code-level component review → `core/two-stage-code-review.md`.
- AI-generated assets → `quality/ai-branding-quality-gate/SKILL.md`.

## Direction first (bidirectional)

Decide which side is canonical for THIS pair before reviewing — it changes who must change:

| Direction | Canonical (truth) | Other side reconciles | Typical trigger |
|---|---|---|---|
| **kit → production** (default) | kit HTML screen | production Next.js code | Track 2 port (most ports) |
| **production → kit** (back-port) | production live code | kit HTML | production shipped first (Wave 78 public, #2326) |

After reconcile, BOTH sides must be parity — no "one side correct, one side drifted". Record the chosen direction in the report header.

## The 4 layers (V-model parity check)

For the screen-pair, verify each layer. Per layer mark `✅ parity / ⚠️ implicit / ❌ drift` (3-state per `design-layer-coverage.md` §3). ANY `❌` at any layer = pair NOT parity → file gap per `audit-to-gap-pipeline.md` (do NOT fix inline).

| # | Layer (V-model) | Parity question | Inputs |
|---|---|---|---|
| 1 | **要件定義** Requirements | Same persona + use-case + business rule served on both sides? | kit `dossier/01-personas.md` + `05-business-flows.md` ↔ production route + `documents/01-business/{domain}/use-cases.md` |
| 2 | **基本設計** External / visual | Layout grid, spacing scale, color tokens, typography, state-set (default/loading/empty/error/success) match? Tokens derive from `_shared/colors_and_type.css` (no hex drift)? WCAG AA contrast holds? | kit `screens/*.html` ↔ production screenshot (Playwright when available; else manual browser walk) |
| 3 | **詳細設計** Internal | Each kit AC item (`dossier/10-acceptance-criteria.md`) wired to a production E2E/test? State-machine / interaction order match? | kit AC list ↔ production `e2e/**` + state transitions |
| 4 | **コンポーネント設計** Component | Production component interface (props/types/slots) matches kit `G*` spec (`dossier/04-component-gaps.md`)? Uses `@kite/shared-ui` where applicable? | kit `dossier/04-component-gaps.md` G* row ↔ production component source |

Detailed thresholds + worked rows → `reference/parity-rubric.md`.

## Process (4 steps)

1. **Pick the pair + direction.** Identify kit screen path + production route. Record canonical direction (table above).
2. **Read tokens FIRST.** Open `ui_kits/_shared/colors_and_type.css` before judging Layer 2 — token drift (kit hex vs shared HSL var) is the #1 parity miss (GAP-1223 lesson).
3. **Walk the 4 layers.** Fill the per-layer verdict table. Layer 2 visual = Playwright screenshot diff (Pixelmatch, threshold in `reference/parity-rubric.md`) when harness available, else documented browser walk. Layer 3 AC = grep production `e2e/**` for each kit AC ID. Layer 4 = diff component props vs G* spec.
4. **Save report.** Markdown to `documents/04-quality/audits/parity/YYYY-MM-DD-<kit>-<screen>.md`: header (pair + direction), per-layer verdict, drift items itemized, recommendation `PASS / NEEDS POLISH / NEEDS REWORK`. Any `❌` → file gap, link in report.

## Skill contents

- `SKILL.md` — this file (entry point)
- `reference/parity-rubric.md` — per-layer thresholds (visual diff %, AC coverage %, component-spec match) + worked example + report template

## Quick checklist

- [ ] Direction (kit→prod / prod→kit) recorded in report header
- [ ] `_shared/colors_and_type.css` read before Layer 2 verdict
- [ ] All 4 V-model layers have a verdict (✅/⚠️/❌)
- [ ] Any `❌` → gap filed per `audit-to-gap-pipeline.md` (NOT fixed inline)
- [ ] Report saved to `documents/04-quality/audits/parity/`

## Gotchas

- **Parity ≠ score.** A production screen can score 110/128 on `ui-review` and still FAIL parity (different layout/UX vs kit). Always compare TWO sides, never score ONE.
- **Direction matters for who-changes.** prod→kit back-port: kit is wrong, fix the kit; kit→prod: production is wrong, fix production. Don't reflexively "fix production".
- **Read tokens before visual judgment** — `_shared/colors_and_type.css` is canonical; kit hex literals that diverge from shared HSL vars are themselves drift (GAP-1223). Don't trust a kit's inline hex as ground truth without checking shared tokens.
- **`_shared/` and `_v1-baseline/` are NOT kits** — never use them as a parity target.
- **Component kit subfolders use bare state names** — `default.html` / `empty.html` directly inside `components/G2-attendance-roster/`, not flat `screens/`.
- **Missing E2E ≠ missing AC** — Layer 3 drift is "AC exists in kit but no production test wires it"; the AC itself may still be implemented. Verdict ⚠️ implicit (not ❌) when behaviour present but untested; ❌ only when behaviour absent.
- **Playwright/Pixelmatch is optional** — when no browser harness in scope, Layer 2 visual = documented manual browser walk (cite screenshots). Report stays valid; mark the visual evidence method.
- **WCAG real measurement** delegates to GAP-352 (axe-core / lighthouse-ci) — Layer 2 contrast is a self-measure pointer until that lands.

## Self-test (worked example)

`kiteclass-public` kit ↔ production public marketing pages (production-first, Wave 78 / #2326):

1. Direction = **production → kit back-port** (production shipped first).
2. Read `_shared/colors_and_type.css` → confirm public pages use shared `--primary` HSL var, not hex.
3. Walk 4 layers:
   - 要件定義: Prospect persona served both sides → ✅
   - 基本設計: kit `kiteclass-public/screens/` layout vs live `(public)` routes → diff verdict
   - 詳細設計: each public AC → production E2E → coverage %
   - コンポーネント設計: shared public components vs `dossier/04-component-gaps.md` → ✅/⚠️/❌
4. Save report `documents/04-quality/audits/parity/<date>-kiteclass-public-back-port.md`; any ❌ → gap.

Expected: skill correctly flags whether the kit was reconciled to the live public pages (back-port direction), not the other way around — demonstrating bidirectional handling.

## Related

- Standard: `.claude/skills/frontend/frontend-standards.md` §3.1 "Kit as Source of Truth"
- Rule: `.claude/rules/design-layer-coverage.md` §2 (4-layer V-model matrix)
- Sister skills: `quality/ui-review/SKILL.md` (Next.js /128), `quality/ui-review-prototype/SKILL.md` (static HTML /128)
- Component spec source: `documents/02-architecture/design-system/dossier/04-component-gaps.md`
- AC source: `documents/02-architecture/design-system/dossier/10-acceptance-criteria.md`
- Tokens: `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` (GAP-1223 lesson)
- WCAG measurement: GAP-352 (axe-core / lighthouse-ci — Layer 2 contrast delegation)
- Visual regression: GAP-355 (pixel-diff baseline tooling)
- Output standard: `.claude/rules/output-review-mandate.md` §3 row "Production FE port from kit"
- Gaps: GAP-367 (this skill — closes), GAP-366 (sister manual standard)
