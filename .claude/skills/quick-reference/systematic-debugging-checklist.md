# Systematic Debugging Checklist

**Quick Reference** - 4-Phase Process

---

## Phase 1: Reproduce (15-30 min) ✅

- [ ] Create failing test case
- [ ] Document exact steps to trigger bug
- [ ] Verify consistency (runs fail 3+ times)
- [ ] Record environment details (versions, data state)

**Output:** Test that fails consistently

---

## Phase 2: Trace (30-60 min) 🔍

- [ ] Set debugger breakpoints at entry point
- [ ] Step through code execution (F7=into, F8=over)
- [ ] Add debug logging at key decision points
- [ ] Identify where behavior diverges from expected

**Output:** Exact line/method where bug occurs

---

## Phase 3: Root Cause (30-45 min) 🎯

Apply **5 Whys**:
1. Why did bug occur? → [Answer]
2. Why [answer from #1]? → [Answer]
3. Why [answer from #2]? → [Answer]
4. Why [answer from #3]? → [Answer]
5. Why [answer from #4]? → **ROOT CAUSE**

- [ ] Distinguish symptom from underlying cause
- [ ] Check MEMORY.md for similar issues
- [ ] Review recent related changes (git log)

**Output:** Root cause identified (not just symptom)

---

## Phase 4: Defensive Fix (1-2 hours) 🛡️

- [ ] Fix root cause (not symptom/workaround)
- [ ] Add regression test (from Phase 1)
- [ ] Fix related scenarios (same pattern elsewhere)
- [ ] Update troubleshooting.md with solution
- [ ] Update MEMORY.md if common pattern

**Output:** Fix committed + tests passing + docs updated

---

## Common Mistakes ❌

- ❌ Fixing symptom instead of root cause
- ❌ Not adding regression test
- ❌ Stopping at "Why?" #1 or #2 (too shallow)
- ❌ Forgetting to document in troubleshooting.md

## Success Criteria ✅

- ✅ Bug no longer reproduces
- ✅ Regression test added and passing
- ✅ Root cause understood and documented
- ✅ Similar issues prevented via defensive fixes

---

**Reference:** `.claude/skills/systematic-debugging.md`
**Target Time:** <2 hours total (down from 3+ hours ad-hoc)
