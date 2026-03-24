# Portable Skill — adapt {project} placeholders to your project

# Skill: 2-5 Minute Task Breakdown

**Purpose:** Break work into bite-sized tasks for faster feedback, clearer progress, simpler reviews.

---

## When to Use

**Use for:** New features (Medium+ complexity), complex PRs (10+ steps), time estimation.

**Skip:** Simple bug fixes (1-2 obvious steps), typos, doc updates.

---

## When to Document

| Feature Size | Files | Complexity | Documentation |
|-------------|-------|------------|---------------|
| <10 min     | 1     | Low        | None (mental only) |
| 10-30 min   | 2-3   | Low-Medium | Inline (PR description) |
| 30-60 min   | 3-5   | Medium     | Light doc (task list + time) |
| >60 min     | 5+    | High       | Full doc (with code samples) |

**Rule of thumb:** If you'd forget the plan after lunch break, document it.

---

## Task Anatomy (5 Required Elements)

Each task MUST have:

1. **Exact file path** — no ambiguity about WHERE
   ```
   Bad:  "Update User entity"
   Good: "{project-path}/src/main/java/.../domain/User.java"
   ```

2. **Specific change description** — clear WHAT
   ```
   Bad:  "Add validation"
   Good: "Add @NotBlank on name, @Email on email, @Size(min=2,max=100) on role"
   ```

3. **Code sample** — copy-paste ready HOW

4. **Verification step** — how to prove it's done
   ```
   Run: ./mvnw compile (or pnpm build)
   Expected: Compilation successful, no errors
   ```

5. **Time estimate** — 2-5 min per task (max 10 for complex)

---

## Task Ordering

**Bottom-up (new features):**
Entity -> Repository -> Service -> Controller -> Tests

**Test-first (TDD):**
Test -> Entity -> Test -> Repository -> Test -> Service

**By risk (bug fixes):**
Reproduce -> Fix Root Cause -> Regression Test -> Update Docs

---

## Granularity Guidelines

- **Too small (<2 min):** "Add import statement" — combine into larger task
- **Just right (2-5 min):** "Create User entity with fields and validations"
- **Too large (>10 min):** "Implement full CRUD" — break into 10-15 smaller tasks

---

## Quick Reference Checklist

Before starting implementation, verify:

- [ ] Every task is 2-5 min (max 10)
- [ ] Exact file paths (no ambiguity)
- [ ] Code samples (copy-paste ready)
- [ ] Verification steps (how to test)
- [ ] Time estimates (realistic)
- [ ] Logical order (dependencies resolved)
- [ ] Total time reasonable (10-20 tasks for Medium, 20-40 for High)

---

## Success Metrics

- Planning accuracy: estimated vs actual time (target: 80%+)
- Task size distribution: most tasks 2-5 min
- Rework rate: <10% tasks need redo
