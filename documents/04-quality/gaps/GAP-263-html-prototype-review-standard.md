# GAP-263: HTML/JSX Prototype Review Standard

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 Meta
**Domain:** Meta Governance / Output Review
**Found:** 2026-04-29 (Wave UI Kits Round 2 kickoff — Plan B from Claude Design block)
**Affects:** Every future HTML/JSX prototype shipped to `documents/02-architecture/design-system/ui_kits/**` (or any equivalent prototype path)

## Problem

`output-review-mandate.md` §3 Review Standards Matrix has **no row for "HTML/JSX prototype"**. Wave UI Kits Round 2 ships ~70-90 HTML files as design prototypes (kiteclass-pro v2 + kiteclass-parent + 5 components) — but there's no documented review standard answering:

1. **Who reviews?** Designer? PR reviewer? User vibe-check?
2. **What rubric?** `quality/ui-review/SKILL.md` /128 was designed for Next.js page captures, not standalone HTML mockups.
3. **What process?** Pre-merge? Post-merge? Quarterly?
4. **What evidence?** Captured screenshots? Score self-report? WCAG measurements in HTML comments?

Without a standard, future HTML prototype waves (Wave 2 Round 2: kiteclass-teacher + ai-branding-wizard-v2 + Direction A polish) will repeat the ad-hoc state Round 1 was in.

This is also the **gap that blocked Wave UI Kits Round 2 from running compliant Phase 0** — without a defined review standard, "what good looks like" remained implicit.

## Root Cause

Output review mandate was written 2026-04-14 covering 11 output types (code, business docs, audits, ADRs, etc.). Throwaway HTML/JSX prototypes for design iteration didn't exist as a project artifact category at that time. The category emerged 2026-04-29 when Claude Design (claude.ai/design) shipped Round 1 bundle and the team committed to Round 2 production-grade prototypes.

## Proposed Fix

### Phase 1 — Extend `output-review-mandate.md` §3 matrix (this gap, current wave)

Add new row to §3 Review Standards Matrix:

```markdown
| **HTML/JSX prototypes** (`documents/02-architecture/design-system/ui_kits/**`) | per-screen `/128` rubric (extended from `quality/ui-review/SKILL.md`) + WCAG AA self-measurement + 100-item AC checklist (`dossier/10-acceptance-criteria.md`) | Pre-merge (foundation kit) + per-deliverable PR self-report + user vibe-check | Author self-review + user accepts | ⚠️ PARTIAL (standard documented this PR; review skill extension pending Phase 2) |
```

Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new matrix row paired with same-PR enforcement: kiteclass-pro v2 + kiteclass-parent + 5 components Wave 1 will be the first kit to apply this standard).

Bump `output-review-mandate.md` v1.1.4 → v1.2.0 (MINOR — new standard covering previously-uncovered output type).

### Phase 2 — Extend `quality/ui-review/SKILL.md` for HTML prototype path (deferred)

Current `ui-review/SKILL.md` assumes Playwright captures of running Next.js dev server. HTML prototypes need a separate path:
- Capture from local static HTTP server (`http://127.0.0.1:9999/.../{kit}/screens/{screen}.html`) instead of dev server
- Skip Next.js-specific checks (no `next build`, no `getServerSideProps`)
- Add HTML-specific checks (proper `<link rel="stylesheet">` to shared tokens; no `<style>` overrides; no inline hex)

Track in **GAP-264** (file when Phase 2 starts).

### Phase 3 — Codify HTML prototype review skill (deferred)

`.claude/skills/quality/ui-review-prototype/SKILL.md` (new) — adapts ui-review for prototype path. Inherits rubric, swaps capture mechanism. Track in **GAP-265** (file when Phase 3 starts, post Wave 2 Round 2).

## Acceptance Criteria

- [ ] `output-review-mandate.md` §3 matrix gains "HTML/JSX prototypes" row
- [ ] `output-review-mandate.md` version bumps v1.1.4 → v1.2.0 with §11 Log entry
- [ ] Wave UI Kits Round 2 PRs reference this standard explicitly in PR description
- [ ] `dossier/10-acceptance-criteria.md` (already shipped PR #667) explicitly cited as the AC checklist
- [ ] No hook/CI enforcement YET (warn-mode acceptable Phase 1; hook to enforce will go in Phase 2 GAP-264)

## Phase 1 = this wave's foundation PR

The matrix-row edit + version bump land in this wave's foundation PR (`wave/round-2-ui-kits` branch) alongside the Wave 1 wave plan. Phase 2 + Phase 3 deferred to future gaps post Wave 1 acceptance.

## Related

- Source: `documents/03-planning/waves/wave-2026-04-29-ui-kits-round-2.md` (this wave)
- AC source: `documents/02-architecture/design-system/dossier/10-acceptance-criteria.md` (PR #667)
- Rubric source: `.claude/skills/quality/ui-review/SKILL.md` (existing)
- Parent rule: `.claude/rules/output-review-mandate.md`
- Future gap: GAP-264 (Phase 2 ui-review-prototype skill), GAP-265 (Phase 3 hook/CI enforcement)
- Triggered by: 2026-04-29 Phase 0 incident — started Phase 0 work without review standard, user flagged

## Log

- **2026-04-29:** Gap filed during Wave UI Kits Round 2 foundation PR. Phase 1 (matrix-row + version bump) lands same PR. Phases 2 + 3 deferred (will file GAP-264 + GAP-265 when respective phases start). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — this gap stays 🟡 PARTIAL after Phase 1 ships, transitions to 🟢 DONE only when Phase 2 + Phase 3 land.
