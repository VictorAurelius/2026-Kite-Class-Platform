# Synthetic Compliant Rule — fixture for `scripts/check-rule-frontmatter.sh`

**Priority:** 🟢 ADVISORY — fixture only, not loaded by any skill
**Version:** 1.0.0
**Created:** 2026-04-28
**Last-Reviewed:** 2026-04-28
**Reviewer-Approver:** @nguyenvankiet (fixture — synthetic data)
**Applies to:** Self-test only; this file is excluded from the live `.claude/rules/` directory

---

## 1. Purpose

This fixture demonstrates a **fully compliant** rule file: every required
frontmatter field is present, the date is sane, the Reviewer-Approver has a
`@handle`, and the Log section has at least one entry.

`scripts/check-rule-frontmatter.sh --self-test` MUST report PASS for this file.

---

## 2. Log

- **2026-04-28** Fixture created as part of GAP-250 self-test (paired with bad-* fixtures). Demonstrates the all-fields-present happy path.
