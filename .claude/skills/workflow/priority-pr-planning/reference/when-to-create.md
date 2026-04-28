# When to Create a Priority Plan + Related Skills

> Pointer: read this when deciding "do I need a priority plan, or just a normal PR?". Parent skill: `../SKILL.md`.

## 🎯 When to Create a Priority Plan

### ✅ CREATE Priority Plan When:
1. **Critical Bug Discovered**
   - Blocks production deployment
   - Breaks core functionality
   - Security vulnerability

2. **PR Becomes Unblocked**
   - Dependency PR completed
   - External service ready
   - Blocker resolved

3. **Technical Debt Critical**
   - Accumulated warnings breaking build
   - Deprecated APIs need immediate upgrade
   - Performance bottleneck urgent

4. **Manual Testing Reveals Issues**
   - Multi-tenant isolation broken
   - Cross-service integration failing
   - CI not catching real bugs

### ❌ DON'T CREATE Priority Plan When:
1. **Regular PR in Queue**
   - Follow master plan order
   - No urgent reason to skip ahead

2. **Minor Refactoring**
   - Can be bundled with next PR
   - No functional impact

3. **Documentation Only**
   - Use regular commit, no special plan needed

---

## 📚 Related Skills Reference

**MUST READ before creating priority plans:**
- `.claude/skills/development-workflow.md` - Git, commits, PRs <!-- TODO: verify against current state -->
- `.claude/skills/testing-guide.md` - Test patterns <!-- TODO: verify against current state -->
- `.claude/skills/code-style.md` - Coding standards <!-- TODO: verify against current state -->
- `.claude/skills/architecture-overview.md` - Multi-tenant, security <!-- TODO: verify against current state -->
- `.claude/skills/cross-service-data-strategy.md` - Integration patterns <!-- TODO: verify against current state -->

**Master Plan Location:**
- `documents/03-planning/implementation/kiteclass-implementation-plan.md` <!-- TODO: verify against current state -->

**Status Updates Location:**
- `documents/03-planning/implementation/STATUS-UPDATE-YYYY-MM-DD.md` <!-- TODO: verify against current state -->

**Gap-to-PR Integration (2026-04-14):**
Use `documents/04-quality/gaps/ROADMAP.md` as priority source (epics + sprints). Convert gap → PR via `.claude/skills/workflow/gap-to-pr-converter.md`.
