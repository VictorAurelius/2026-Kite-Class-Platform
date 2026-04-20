---
name: rule-review
description: "Dùng khi PR thêm/sửa file trong `.claude/rules/*.md`, 'review rule', 'kiểm tra rule change', 'rule PR'. ADR-like reviewer checklist cho rules docs."
user-invocable: true
---

# Rule Review Skill

Reviewer checklist for any PR touching `.claude/rules/*.md`. Enforces `.claude/rules/rule-change-process.md`.

## When to use

- PR touches any file under `.claude/rules/`
- Before approving rule changes
- Periodic audit of rule freshness (quarterly)

## Process

1. **Identify bump level** — read the diff, classify as PATCH / MINOR / MAJOR per `rule-change-process.md` §4
2. **Check minimum reviewers present** per §5 matrix (PATCH=1, MINOR=2, MAJOR=3+team heads-up)
3. **Run the 10-check list below**
4. **Sign off** — add a PR review comment citing bump level + checklist result, OR request changes

## Reviewer Checklist

- [ ] **Version bumped** — frontmatter `**Version:**` moved correctly per §4 semver
- [ ] **Last-Reviewed updated** — today's date in frontmatter
- [ ] **Reviewer-Approver populated** — @handles match PR reviewers
- [ ] **Log entry appended** — newest-first, has date + version + summary + motivation
- [ ] **Enforcement section present** — every rule has §Enforcement with hook / CI / template / audit / tracking gap
- [ ] **No contradiction with sibling rules** — reviewer grepped `.claude/rules/*.md` for conflicting clauses
- [ ] **Testable / auditable** — reviewer can imagine (or draft) a PR that would hit the rule; rule tells reviewer whether to PASS or FAIL
- [ ] **Scope tight** — rule doesn't overreach into another rule's domain (e.g. a meta-gap rule claiming jurisdiction over business docs)
- [ ] **PATCH honesty** — any change that could block a previously-passing PR is MINOR+, not PATCH
- [ ] **Relationship section updated** — if new rule or major change, adjacent rules' §Relationship sections reflect the change

## Gotchas

- **Self-referential rules** — `rule-change-process.md` itself was self-approved at v1.0 (bootstrap). Subsequent versions must follow its own process — reviewers enforce that even the process-rule PR follows the process.
- **Typo PRs still need log entries** — §12 exceptions allow 1-reviewer PATCH but NEVER skip the log entry; reviewer should push back if missing.
- **Frontmatter backfill** — old rules without `Version` field: the first edit after 2026-04-20 must backfill; don't let PR skate by "it was already like that".
- **Enforcement is not optional** — if a rule ships without §Enforcement and without a tracking gap citation, REJECT; advisory-only rules rot into fiction.
- **Cross-rule greps** — before approving, `grep -l "<keyword>" .claude/rules/*.md` to spot contradictions. A change to `meta-gap-priority.md` priority tiers almost always ripples to `audit-to-gap-pipeline.md` Step 6 ordering.
- **MAJOR bump requires team heads-up** — if reviewer sees MAJOR but no team-channel note in PR body, ask for one before approving.

## Related

- `.claude/rules/rule-change-process.md` — the rule this skill enforces
- `.claude/rules/output-review-mandate.md` §4 VIOLATION #2 — closed by this skill + the rule
- `.claude/skills/quality/gap-review/SKILL.md` — parallel skill for gap files
- `.claude/rules/skill-conventions.md` — related governance for `.claude/skills/**`
