# Synthetic Bad Rule — Missing Version

**Priority:** 🟢 ADVISORY — fixture only
**Created:** 2026-04-28
**Last-Reviewed:** 2026-04-28
**Reviewer-Approver:** @nguyenvankiet (fixture)
**Applies to:** Self-test FAIL case — this rule lacks the **Version:** field

---

## 1. Purpose

This fixture is **non-compliant**: it omits the `**Version:**` line entirely.
`scripts/check-rule-frontmatter.sh --self-test` MUST report FAIL for this file
with a "missing or malformed **Version:**" message.

---

## 2. Log

- **2026-04-28** Fixture created — synthetic missing-Version case for GAP-250 detector self-test.
