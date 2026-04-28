# Synthetic Bad Rule — Missing Log Section

**Priority:** 🟢 ADVISORY — fixture only
**Version:** 1.0.0
**Created:** 2026-04-28
**Last-Reviewed:** 2026-04-28
**Reviewer-Approver:** @nguyenvankiet (fixture)
**Applies to:** Self-test FAIL case — this rule has frontmatter but no Log section

---

## 1. Purpose

This fixture is **non-compliant**: every frontmatter field is present, but the
file is missing the `## ... Log` heading. `rule-change-process.md` §7 requires
every rule to maintain an append-only changelog. The detector should FAIL here
with a "missing '## ... Log' heading" message.

---

## 2. Other Section (no Log section)

This file deliberately ends without a Log heading.
