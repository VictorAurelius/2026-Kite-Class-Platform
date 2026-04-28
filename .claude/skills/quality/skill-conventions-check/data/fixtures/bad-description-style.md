---
name: example-bad-description
description: "I am a skill that runs quality audits and produces a score. I have many useful features and you should always invoke me first thing in the morning."
user-invocable: false
---

# Example Skill With 1st-Person Description — synthetic FAIL fixture

> **Expected output:** FAIL — description has no trigger-condition keyword.

This fixture demonstrates the anti-pattern called out in
`.claude/rules/skill-conventions.md` §3:

> **BAD — human summary, no trigger context**

The description above is written in 1st-person prose ("I am a skill that..."),
the way a human reads. It contains no trigger-condition keyword
(`Use when`, `Dùng khi`, `When the user`, `Triggered`, `Apply when`, `Khi nào`,
`Auto-trigger`, etc.).

The conventions checker must report:

```
[FAIL]  .../bad-description-style.md: description has no trigger-condition keyword (Use when / Dùng khi / Khi nào / Triggered / Apply when / Auto-trigger / Use this / Use to)
```

## Process

This skill does nothing — it's a synthetic FAIL fixture.

## Gotchas

- Don't copy this description style into real skills. The model uses
  `description` to decide whether to activate; without trigger keywords,
  activation rate drops.
- Trigger keywords are language-aware (English + Vietnamese in this project)
  — keep both variants represented in real skills.

## Skill Contents

- `data/fixtures/bad-description-style.md` — this file
