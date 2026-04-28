---
name: example-good-skill
description: "Use when user says 'example check', 'sanity test', 'kiểm tra ví dụ', or to demonstrate skill-conventions compliance. Synthetic fixture for `scripts/check-skill-conventions.sh` self-test (GAP-251). Returns trivially-correct output."
user-invocable: false
---

# Example Good Skill — synthetic fixture for skill-conventions self-test

> **Expected output:** PASS — all 4 mandatory checks satisfied.

This fixture demonstrates a SKILL.md that conforms to `.claude/rules/skill-conventions.md`:

1. Has frontmatter with `name` (≤64 chars) AND `description` (≤1024 chars)
2. Description contains a trigger-condition keyword (`Use when`, `Dùng khi`, etc.)
3. Body has a `## Gotchas` section (project-specific failure points)
4. Body length is ≤500 lines

## Process

When invoked, this skill does nothing — it is a fixture, not a runtime skill.

## Gotchas

- This file is a fixture, not a real skill. Do not invoke it from production paths.
- Keep this file synthetic — never copy real PII, secrets, or business logic into fixtures.
- If the conventions check ever changes its trigger-keyword set, re-verify this fixture still passes.

## Skill Contents

- `data/fixtures/good.md` — this file (PASS scenario)
- `data/fixtures/bad-no-gotchas.md` — sibling fixture (FAIL on missing Gotchas)
- `data/fixtures/bad-description-style.md` — sibling fixture (FAIL on 1st-person description)
