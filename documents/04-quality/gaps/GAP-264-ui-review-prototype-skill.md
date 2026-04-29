# GAP-264: ui-review-prototype Skill (Phase 2 of GAP-263)

**Status:** 🔵 OPEN — placeholder filed by foundation PR; full content shipped by Tier 2 agent in same wave (Wave Review Process Improvement)
**Priority:** 🟠 P1 Meta — review process enforcement
**Domain:** Meta / Quality / UI Review
**Found:** 2026-04-29 (Wave Review Process Improvement, Tier 2 — paired with GAP-263 Phase 1 + Tier 1 landing parity script)
**Affects:** All future HTML/JSX prototype waves under `documents/02-architecture/design-system/ui_kits/**`

---

## Problem

`output-review-mandate.md` v1.3.0 §3 row "HTML/JSX prototypes" Process column mandates integration smoke test + landing parity + per-screen state coverage check. Tier 1 ships `check-ui-kits-landing.sh` (basic landing parity). What's missing:

1. **Link checker** — verify every `<a href="...">` inside kit HTML resolves (no 404 on click-through during user vibe-check)
2. **Stricter landing parity** — Tier 1 checks card count + slug match; Tier 2 also verifies score numbers + persona pills + screen counts displayed in cards match actual kit READMEs
3. **State coverage** — verify each kit folder has all required state files per `dossier/10-acceptance-criteria.md` §4 (default / loading / empty / error / success / dark)
4. **Skill abstraction** — bundle the 3 scripts as a callable `quality/ui-review-prototype` skill so review reports + future waves consume one entry point

Without Tier 2 skill, reviewer manually runs scripts ad-hoc → drift risk over time (same incident pattern as 2026-04-29 landing-page miss).

## Root Cause

Phase 1 of GAP-263 (output-review-mandate v1.2.0 row addition) shipped 2026-04-29 morning but Phase 2 explicitly deferred. Wave UI Kits Round 2 closed without Phase 2 → user-flagged miss → ship Phase 2 immediately as Tier 2 of Wave Review Process Improvement.

## Proposed Fix

### Skill structure (per `skill-conventions.md`)

```
.claude/skills/quality/ui-review-prototype/
├── SKILL.md (entry point, <100 lines, trigger-keyword description)
├── scripts/
│   ├── link-checker.sh           # verify <a href> resolve in kit HTML
│   ├── landing-parity.sh         # stricter: cards match folders + scores + persona pills
│   └── state-coverage.sh         # each kit has default/loading/empty/error/success/dark
├── reference/
│   ├── scoring-guide.md          # extends ui-review/SKILL.md /128 rubric for static HTML
│   ├── integration-smoke-test.md # browser walk-through procedure (carved from review template §5.2)
│   └── kit-folder-conventions.md # _v1-baseline/ + screens/ + supporting files structure
└── data/
    └── runs.log                  # append-only log per skill run (timestamp + kit count + verdicts)
```

### Acceptance Criteria

- [ ] Skill `quality/ui-review-prototype/` created with SKILL.md + 3 scripts + reference docs
- [ ] All 3 scripts shellcheck-clean + executable
- [ ] All 3 scripts have `--help` + clear exit codes
- [ ] **Self-test:** run all 3 scripts against current `ui_kits/` (6 kits) → expected: link-checker exit 0, landing-parity exit 0 (matches Tier 1 result), state-coverage exit 0 (all 6 kits have at least default/loading/dark; missing states = warn, not fail)
- [ ] **Reproduce 2026-04-29 incident:** synthetically remove 1 card from `index.html` → run landing-parity.sh → expected exit 1 with specific FAIL message
- [ ] Skill index `_README-skills-index.md` updated with new skill row
- [ ] Review report TEMPLATE §5.3 placeholder filled with concrete script paths
- [ ] GAP-263 Log entry appended noting Phase 2 SHIPPED via this gap

## Related

- **Phase 1:** GAP-263 (output-review-mandate v1.2.0 row + this gap's parent)
- **Phase 3:** GAP-265 (hook/CI/lefthook enforcement)
- **Wave plan:** `documents/03-planning/waves/wave-2026-04-29-review-process-improvement.md`
- **Tier 1 script:** `documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh`
- **Memory:** `feedback_post_merge_doc_sync.md` extended 2026-04-29 with landing-page parity lesson

## Log

- **2026-04-29 (placeholder filed):** Created by foundation PR of Wave Review Process Improvement. Full skill content shipped by Tier 2 agent in same wave. Status will flip 🔵 OPEN → 🟢 DONE upon Agent A merge.
