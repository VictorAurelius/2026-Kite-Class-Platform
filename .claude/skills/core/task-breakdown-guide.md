---
description: "Dùng khi bắt đầu implement PR medium+, user nói 'chia nhỏ', 'breakdown', 'estimate', 'implementation plan', 'bắt đầu code feature này'. Mỗi task = 2-5 phút, có exact file path + code sample + verify step. Skip: bug fixes 1-2 steps rõ ràng, typos."
---

# Task Breakdown Guide

## Khi nào dùng

- Planning new features (Medium+ complexity)
- Breaking down complex PRs (10+ steps)
- Setting expectations for time estimates
- **Breaking down gap into PR tasks** (use with `gap-to-pr-converter.md`)

## Khi nào skip

- Simple bug fixes (1-2 steps obvious)
- Typo corrections, documentation-only updates

## From Gap File to Tasks

Gap files đã có structured sections → map thành tasks:

| Gap Section | Task Conversion |
|-------------|-----------------|
| **Current State (verified)** | **State-check task — re-verify before breakdown (see below)** |
| Problem | Context (không gen task) |
| Proposed Fix steps | Each step → 1-3 tasks |
| Acceptance Criteria | Each criterion → verification task |
| Dependencies | Block/unblock checks |

### State-Check First (BẮT BUỘC)

Before generating any task:
1. If gap has `## Current State (verified YYYY-MM-DD)` → grep same paths, compare. If divergent, the gap is stale — update it before task breakdown.
2. If gap lacks Current State section (old-format) → do a one-shot state-check per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Drop any task that would duplicate existing code. Narrow task list to the delta.

Skipping = tasks enumerate work already done → wasted agent-time.

Example GAP-007 (Resource Classification):
```
Gap Proposed Fix step 1 "Define enums" →
  Task 1: Write ResourceCategoryTest (RED)
  Task 2: Create ResourceCategory enum (GREEN)
  Task 3: Write unit tests for 4 scenarios
Gap Proposed Fix step 2 "Entity + DB" →
  Task 4: Write BrandingResource JPA mapping tests
  Task 5: Implement BrandingResource entity
  Task 6: Create Flyway V28 migration
...
```

## Documentation Decision

| Feature | Files | Complexity | Doc Level |
|---------|-------|------------|-----------|
| <10 min | 1 | Low | ⏭️ Mental only |
| 10-30 min | 2-3 | Low-Med | 📝 Inline (PR description) |
| 30-60 min | 3-5 | Medium | 📄 Task list + time |
| >60 min | 5+ | Med-High | 📚 Full doc với code samples |

**Rule:** Nếu quên plan sau lunch break → Document it.

## Task Anatomy (5 Elements BẮT BUỘC)

1. **Exact file path** — không ambiguity về WHERE
2. **Specific change** — không vague như "add feature"
3. **Code sample** — copy-paste ready
4. **Verification step** — cách test/verify task done
5. **Time estimate** — 2-5 phút (max 10 phút)

## Gotchas

- **Flyway migrations** — mỗi migration = 1 task riêng, KHÔNG gộp với entity changes trong cùng task
- **Business docs là task đầu tiên** nếu domain mới — chạy `pre-flight-check domain` trước khi breakdown
- **Wave tasks estimate x1.5** — integration overhead giữa các PRs trong wave
- **Task ordering KiteClass**: Entity → Repository (custom query, không findById) → Service → Controller → Tests

## Task Ordering

- **Bottom-up** (features mới): Entity → Repository → Service → Controller → Tests
- **Test-first** (TDD): Test → Code → Test → Code (xen kẽ)
- **By risk** (bug fixes): Reproduce → Fix → Regression Test → Docs

## Skill Contents

- `quick-reference/task-breakdown-formula.md` — 5-element template copy-paste ready
- `quick-reference/task-breakdown-examples.md` — Bad vs Good example: Student CRUD (9 tasks)

## Trigger Phrases

"chia nhỏ", "breakdown", "estimate", "implementation plan", "bắt đầu code", "Complexity: Medium/High"

## Quick Checklist

- [ ] Every task 2-5 min? (max 10 for complex)
- [ ] Exact file paths? (no ambiguity)
- [ ] Code samples? (copy-paste ready)
- [ ] Verification steps? (how to test)
- [ ] Time estimates? (realistic)
- [ ] Logical order? (dependencies resolved)

**If unclear:** 10 phút với `brainstorming-methodology.md` trước khi breakdown
