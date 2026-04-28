---
name: example-no-gotchas
description: "Use when user says 'no gotchas test', 'gotchas-missing fixture', or to verify the skill-conventions check FAILs on a SKILL.md missing its `## Gotchas` section."
user-invocable: false
---

# Example Skill Missing Gotchas — synthetic FAIL fixture

> **Expected output:** FAIL — body has no `## Gotchas`, `## Anti-patterns`, or `## Common Mistakes` section.

This fixture intentionally omits the project-specific gotchas section that
`.claude/rules/skill-conventions.md` §5 mandates. The conventions checker
must report:

```
[FAIL]  .../bad-no-gotchas.md: body missing '## Gotchas' OR '## Anti-patterns' OR '## Common Mistakes' section
```

The frontmatter is intentionally complete (so this fixture isolates check 3).
Description has trigger keywords; name is short. Only the gotchas section is
missing.

## Process

This skill does nothing — it's a synthetic FAIL fixture.

## Skill Contents

- `data/fixtures/bad-no-gotchas.md` — this file
