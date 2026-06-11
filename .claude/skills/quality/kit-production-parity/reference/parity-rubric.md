# Kit ↔ Production Parity Rubric (4-layer V-model)

Reference for `quality/kit-production-parity/SKILL.md`. Per-layer thresholds + worked rows + report template. Read on demand when filling a parity report.

---

## Per-layer thresholds

### Layer 1 — 要件定義 (Requirements parity)

| Verdict | Condition |
|---|---|
| ✅ parity | Same persona(s) + use-case + business rule served on both sides (kit `dossier/01-personas.md` + `05-business-flows.md` ↔ production route + `documents/01-business/{domain}/use-cases.md`) |
| ⚠️ implicit | Persona served but not explicitly traced in dossier OR business rule covered via shared rule not per-screen |
| ❌ drift | One side serves a persona/use-case the other doesn't (scope mismatch) |

### Layer 2 — 基本設計 (Visual / external parity)

Visual diff via Playwright screenshot + Pixelmatch (when harness available), else documented manual browser walk.

| Pixel-diff % | Verdict |
|---|---|
| ≤ 15% | ✅ parity |
| 15–30% | ⚠️ implicit (acceptable polish drift — note items) |
| > 30% | ❌ drift (NEEDS REWORK) |

Visual parity ALSO requires (each a hard ❌ if violated):
- Color tokens derive from `_shared/colors_and_type.css` HSL vars — **no divergent hex literals** (GAP-1223).
- Spacing scale uses `gap-*` / `space-y-*` tokens (frontend-standards §10), not arbitrary px.
- State-set present both sides: default + (loading / empty / error / success as applicable per `dossier/04`).
- WCAG AA contrast holds (self-measure pointer; real measurement delegates GAP-352 axe-core).

### Layer 3 — 詳細設計 (AC + interaction parity)

| AC coverage % (kit AC items wired to a production E2E/test) | Verdict |
|---|---|
| 100% wired | ✅ parity |
| ≥ 70% wired, rest behaviour-present-but-untested | ⚠️ implicit |
| behaviour ABSENT for any AC | ❌ drift |

Compute: read `dossier/10-acceptance-criteria.md` for the screen → for each AC ID, grep production `e2e/**` for matching reference (AC-* ID or descriptive text). List missing items. Also verify state-machine / interaction order matches.

### Layer 4 — コンポーネント設計 (Component-spec parity)

| Verdict | Condition |
|---|---|
| ✅ parity | Production component interface (props / types / slots) matches kit `G*` spec in `dossier/04-component-gaps.md`; uses `@kite/shared-ui` where applicable |
| ⚠️ implicit | Component present + functional but interface not catalogued in `dossier/04` (flag for explicit conversion) |
| ❌ drift | Production composition diverges from kit G* spec (missing props / different slots / wrong primitive) |

---

## Overall recommendation mapping

| Layer verdicts | Recommendation |
|---|---|
| All ✅ (0 ❌, 0 ⚠️) | **PASS** |
| 0 ❌, ≥1 ⚠️ | **NEEDS POLISH** — note items, may merge with follow-up gap for ⚠️ → ✅ conversion |
| ≥1 ❌ at any layer | **NEEDS REWORK** — pair NOT parity; file gap per `audit-to-gap-pipeline.md`, do NOT fix inline |

---

## Worked example (production → kit back-port, `kiteclass-public` #2326)

| Layer | Inputs | Verdict | Note |
|---|---|---|---|
| 要件定義 | Prospect persona ↔ `(public)` routes | ✅ | both serve anonymous marketing visitor |
| 基本設計 | `kiteclass-public/screens/*.html` ↔ live public pages | ⚠️ | hero spacing drifted ~18% — kit predates live polish; back-port kit |
| 詳細設計 | public AC ↔ `e2e/public/**` | ⚠️ | CTA-click AC present in live, no E2E yet |
| コンポーネント設計 | shared public components ↔ `dossier/04` | ✅ | shared-ui primitives match |

**Direction:** production → kit (live shipped first). **Recommendation:** NEEDS POLISH — back-port kit hero spacing to match live (kit is the side that drifted); file follow-up gap for missing public E2E. Demonstrates: in back-port direction, the **kit** is reconciled to production, not vice-versa.

---

## Report template

```markdown
# Kit ↔ Production Parity — <kit>/<screen> (YYYY-MM-DD)

**Direction:** kit → production | production → kit (back-port)
**Pair:** kit `ui_kits/<kit>/screens/<screen>.html` ↔ production `<route>`
**Visual evidence:** Playwright Pixelmatch <%> | manual browser walk (screenshots: <paths>)
**Tokens read:** `_shared/colors_and_type.css` ✅

| Layer | Verdict | Drift items |
|---|:---:|---|
| 要件定義 Requirements | ✅/⚠️/❌ | ... |
| 基本設計 Visual | ✅/⚠️/❌ | diff %, token/spacing/state/WCAG notes |
| 詳細設計 AC | ✅/⚠️/❌ | AC coverage %, missing E2E list |
| コンポーネント設計 Component | ✅/⚠️/❌ | props/slots diff |

**Recommendation:** PASS / NEEDS POLISH / NEEDS REWORK
**Gaps filed (if any ❌/deferred ⚠️):** GAP-NNN ...
```
