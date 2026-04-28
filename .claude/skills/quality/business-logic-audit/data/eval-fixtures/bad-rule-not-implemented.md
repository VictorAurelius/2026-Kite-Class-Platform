# Eval Fixture — bad-rule-not-implemented.md

# Expected: FAIL — rules.md declares BR-ATT-005 but no Java/controller path implements it

**Skill:** `quality/business-logic-audit`
**Scenario:** Synthetic `attendance` domain where rules.md mentions a constraint
the code never enforces.
**Which check fires:** Category 1 — Rule Coverage (-4 per missing rule).
**Filed:** 2026-04-28 (GAP-253 pilot)

---

## Setup (synthetic)

Imagine `documents/01-business/kiteclass/attendance/rules.md` declaring:

```markdown
- **BR-ATT-005** A teacher may not modify attendance older than 7 days
  unless the user has role `ADMIN`. Configurable via
  `kiteclass.attendance.edit-window-days` (default 7).
```

But the controller has no enforcement:

```java
// AttendanceController.java
@PutMapping("/{id}")
public Result update(@PathVariable Long id, @RequestBody AttendanceRow row) {
  return service.update(id, row);  // ← no age check, no role guard
}
```

And `application.yml` has no `kiteclass.attendance.edit-window-days` key.

---

## Expected audit-report excerpt

```
## Category 1: Rule Coverage           16/20  (-4)

### Missing implementations:
- BR-ATT-005 (edit window + admin override) — no enforcement found in
  AttendanceController, AttendanceService, or any @Aspect.

### Recommended actions:
1. Add `@PreAuthorize("hasRole('ADMIN') or @attendanceWindow.canEdit(#id)")`
2. Wire `kiteclass.attendance.edit-window-days` into application.yml
3. Add UC-ATT-error path test: teacher tries to edit 8-day-old row → 403
```

A gap should be filed via `audit-to-gap-pipeline.md`:
`GAP-XXX-attendance-edit-window-not-enforced` (P0 — security adjacent).

---

## How to use this fixture

This fixture catches the most common business-logic regression — rule lands
in rules.md without paired code. Any time the audit skill is extended to
catch new violation patterns, verify this fixture STILL fires the missing-rule
check (don't accidentally exempt it).

The check is "grep for BR-ID in `*.java` and `application.yml`" — empty match
means uncovered rule. Cross-reference: `audit-to-gap-pipeline.md` Step 3
formats the gap.
