---
name: ui-review-prototype
description: "Dùng khi user nói 'audit prototype', 'review ui kits', 'check landing parity', 'kiểm tra prototype HTML', hoặc trước khi merge wave UI kits. Extends quality/ui-review for static HTML prototype path (vs Next.js dev server). Runs 3 scripts: link-checker (no broken hrefs in kit HTML) + landing-parity (stricter than Tier 1: cards + scores + persona pills + screen counts) + state-coverage (default/loading/empty/error/success/dark per dossier §4). Codifies 2026-04-29 GAP-263 Phase 2 + closes GAP-264."
user-invocable: true
---

# /ui-review-prototype — HTML/JSX Prototype Review Skill

Tier 2 of `output-review-mandate.md` v1.3.0 §3 row "HTML/JSX prototypes". Sister to `quality/ui-review/SKILL.md` (which targets running Next.js dev server) — this skill targets static HTML kits under `documents/02-architecture/design-system/ui_kits/**`.

## When to use

- Wave UI Kits PR merge candidate review
- Closure PR for HTML prototype waves (sync READMEs + index.html)
- Pre-flight before user vibe-check
- Periodic drift check on shipped kits

## When NOT to use

- Running Next.js dev server screens → use `quality/ui-review/SKILL.md`
- Code-level component review → use `quality/two-stage-code-review.md`
- AI-generated assets → use `quality/ai-branding-quality-gate/SKILL.md`

## Process (3 steps)

### 1. Run 3 scripts (in any order — independent)

```bash
bash .claude/skills/quality/ui-review-prototype/scripts/link-checker.sh
bash .claude/skills/quality/ui-review-prototype/scripts/landing-parity.sh
bash .claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh
```

All three log to `data/runs.log` (append: ISO timestamp | script | exit | summary). Exit 0 = PASS, 1 = FAIL, 2 = ERROR.

### 2. Browser walk-through

Follow `reference/integration-smoke-test.md` — start static HTTP server, open landing, click each card, sample 3 screens per kit. Fill review template §5.2.

### 3. Save report

Use `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md`. Save instance to `documents/04-quality/audits/ui-review/{YYYY-MM-DD}-{wave-name}-review.md`. Wire script exit codes into §5.1 + §5.3.

## Skill contents

- `SKILL.md` — this file (entry point)
- `scripts/link-checker.sh` — no broken `<a href>` in kit HTML
- `scripts/landing-parity.sh` — stricter than Tier 1 (folder ↔ card + score + persona pill + screen-count match)
- `scripts/state-coverage.sh` — minimum required state files per kit per dossier §4
- `reference/scoring-guide.md` — /128 rubric extended for static HTML path
- `reference/integration-smoke-test.md` — browser walk-through procedure
- `reference/kit-folder-conventions.md` — `_v1-baseline/`, `_shared/`, `screens/`, `app.jsx`, `index.html` conventions
- `data/runs.log` — append-only run history (auto-managed by scripts)

## Trigger phrases (Vietnamese + English)

- "audit prototype", "review ui kits", "check landing parity"
- "kiểm tra prototype HTML", "kiểm tra ui kits", "review landing"
- "ui-review-prototype", "/ui-review-prototype"
- before merging closure PR for HTML prototype waves

## Quick checklist

- [ ] All 3 scripts exit 0 on current `ui_kits/`
- [ ] Browser walk-through documented in review template §5.2
- [ ] Review report saved to `documents/04-quality/audits/ui-review/`
- [ ] If failures → file gaps per `audit-to-gap-pipeline.md`, do NOT fix inline

## Gotchas

- **`_shared/` and `_v1-baseline/` are NOT kits** — scripts MUST exclude them (they're infra/baseline-reference, not deliverables)
- **`components/` kit is multi-folder** — its `screens/` are per-component subfolders (`G2-attendance-roster/`, etc.) with `default.html` / `empty.html` / etc. directly inside, NOT a flat `screens/` directory
- **Component subfolders use bare state names** — `default.html` (not `<screen>-default.html`) because each subfolder IS one screen
- **External `http(s)://` links** — link-checker SKIPS them (curl is slow + flaky; only resolve relative/anchor)
- **Score format varies** — `108.4/128`, `114/128 ⭐`, `115.6/128 ⭐⭐` — landing-parity tolerates trailing star markers
- **`success.html` ≠ `confetti.html`** — `success-confetti.html` in kiteclass-pro-v2 is a special variant; state-coverage doesn't require it (only basic `default` + 1-of `loading|empty|error`)
- **Vietnamese HTML comments** — `<!-- Persona: P2 ... -->` may contain non-ASCII; use UTF-8-safe grep
- **Exit codes:** 0 PASS / 1 FAIL / 2 ERROR (file not found, env broken)

## Self-test

Run on current `ui_kits/` (6 kits):

```bash
bash .claude/skills/quality/ui-review-prototype/scripts/link-checker.sh && echo OK1
bash .claude/skills/quality/ui-review-prototype/scripts/landing-parity.sh && echo OK2
bash .claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh && echo OK3
```

Expected: all OK1/OK2/OK3 print. To reproduce 2026-04-29 incident: temporarily delete a card from `ui_kits/index.html` → re-run landing-parity.sh → expect exit 1 with FAIL message naming the missing kit.

## Related

- Standard: `.claude/rules/output-review-mandate.md` v1.3.0 §3 row "HTML/JSX prototypes"
- Tier 1 (foundation): `documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh`
- Tier 3 (enforcement): GAP-265 (`audit-gate.py` + workflow + lefthook)
- Sister skill: `quality/ui-review/SKILL.md` (Next.js dev server path)
- Review template: `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md`
- Acceptance source: `documents/02-architecture/design-system/dossier/10-acceptance-criteria.md`
- Gaps: GAP-263 (Phase 1 standard), GAP-264 (this skill — closes), GAP-265 (Phase 3 enforcement)
