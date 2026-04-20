---
name: gap-review
description: "Dùng khi PR thêm/sửa file trong `documents/04-quality/gaps/GAP-*.md`, 'review gap', 'peer-review gap', 'gap có hợp lệ không?', 'kiểm tra gap', hoặc trước khi đổi status 🔵 OPEN → 🟡 PLANNED. Peer-review checklist cho gap files."
user-invocable: true
---

# Gap Review Skill

Peer-review một gap file (`documents/04-quality/gaps/GAP-*.md`) trước khi nó chuyển từ 🔵 OPEN sang 🟡 PLANNED hoặc trước khi fix PR được tạo. Output VIOLATION #1 của `output-review-mandate.md` §4.

## When to use

- PR adds/modifies `documents/04-quality/gaps/GAP-*.md`
- Before moving gap status 🔵 OPEN → 🟡 PLANNED
- Before creating fix PR from a gap (via `workflow/gap-to-pr-converter.md`)
- Triage session reviewing newly filed gaps

## Process (≈5 min per gap)

1. **Read the gap file** + any `## Related` links
2. **Run through `reference/checklist.md`** — 10 criteria grouped in 4 sections
3. **Verify state-check** — gap must have been filed per `audit-to-gap-pipeline.md` §2.5. If missing, add a `## Current State (verified YYYY-MM-DD)` section before approving
4. **Sign off** — either:
   - Append `**Peer-reviewed:** YYYY-MM-DD by @reviewer — PASS` to gap `## Log`
   - Or list failures + request author fix
5. **Status gate** — gap cannot be 🟡 PLANNED without a passing peer review signature

## Skill contents

- `reference/checklist.md` — the full 10-point checklist with pass criteria and examples
- `documents/04-quality/gaps/_REVIEW-TEMPLATE.md` — the review sheet reviewer fills in + appends to gap Log

## Gotchas

- **State-check is non-negotiable** — a gap without §2.5 evidence is a guess, not a bug. Reject and ask author to re-file per `audit-to-gap-pipeline.md` Step 2.5.
- **Meta vs feature classification drives priority** — check `meta-gap-priority.md` §2-§3. A gap touching `.claude/skills/**` / `.claude/rules/**` / `.husky/` / `.github/workflows/` / `audit-gate.py` is Meta, not Feature, even if wording hides it.
- **Duplicate check** — reviewer re-runs `grep -rl "<keyword>" documents/04-quality/gaps/` before approval; authors sometimes miss existing gaps (see GAP-107 false-positive retraction 2026-04-20).
- **AC must be check-boxable** — "improve performance" fails; "p95 /api/students < 200ms measured via k6" passes.
- **ROADMAP registration** — per `audit-to-gap-pipeline.md` §Step 5, gap MUST be in ROADMAP.md. Reviewer confirms before sign-off.
- **Docs-only gaps still need review** — doc-only gaps are NOT exempt. A wrong rule doc force-multiplies; that is the original motivation for this skill.

## Related

- `.claude/rules/audit-to-gap-pipeline.md` — gap filing pipeline (Step 2.5 state-check mandatory)
- `.claude/rules/meta-gap-priority.md` — §3 priority matrix (Meta / Business-Logic / Feature tiers)
- `.claude/rules/output-review-mandate.md` §4 VIOLATION #1 — this skill closes that violation
- `.claude/skills/workflow/gap-to-pr-converter.md` — downstream consumer (refuses to convert gap without peer-review)
- `documents/04-quality/gaps/_REVIEW-TEMPLATE.md` — review sheet
