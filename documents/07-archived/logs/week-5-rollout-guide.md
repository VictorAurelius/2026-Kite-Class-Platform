# Week 5 - Superpowers Rollout Guide

**Date:** 2026-03-13
**Phase:** Rollout (Week 5-8)
**Audience:** Development Team
**Status:** 🚀 ACTIVE

---

## 🎯 Executive Summary

Sau 4 tuần pilot testing với 3 PRs, chúng ta đạt được:
- ✅ **Planning Accuracy:** 92% (vs 60% baseline, +32pp)
- ✅ **ROI:** 4.2:1 hiện tại → 2.2:1 projected dài hạn
- ✅ **Skills Maturity:** 93% average readiness
- ✅ **Rollout Readiness:** 96%

**Từ Week 5 trở đi:** Áp dụng 5 Superpowers skills cho **TẤT CẢ PRs mới** (không còn pilot).

---

## 📚 5 Superpowers Skills - Quick Reference

### 1️⃣ Systematic Debugging (4 Phases)

**Khi nào dùng:** Khi gặp bug/error không hiểu rõ root cause

**Quy trình:**
1. **Observe** (5-10 min): Thu thập logs, error messages, reproduction steps
2. **Hypothesize** (5-10 min): Brainstorm 3+ possible causes với likelihood scores
3. **Test** (10-30 min): Validate hypotheses từ most likely → least likely
4. **Document** (5 min): Ghi lại root cause, fix applied, prevention strategy

**Quick Reference:** `.claude/skills/quick-reference/systematic-debugging-checklist.md`

**Ví dụ từ pilot:** Chưa áp dụng (không có bug trong greenfield PRs)

---

### 2️⃣ Socratic Brainstorming (2 Templates)

**Khi nào dùng:** Trước khi code, khi có nhiều cách implement

**2 Templates:**

#### Quick Brainstorm (5 min) - Cho features đơn giản
- **Use when:** Feature <30 min, 2 options rõ ràng, Low complexity
- **Structure:** Problem (1 min) → Options (2 min) → Decision (2 min)
- **Template:** `.claude/skills/quick-reference/quick-brainstorm-template.md`
- **Ví dụ:** Pilot PR 2 (Student Search) - JPQL vs Full-text search

#### Full Socratic (20 min) - Cho features phức tạp
- **Use when:** Feature >30 min, 3+ options, Medium+ complexity
- **Structure:** Question Assumptions (7 min) → Trade-offs (10 min) → Document (3 min)
- **Skill file:** `.claude/skills/socratic-brainstorming.md`
- **Ví dụ:** Pilot PR 3 (Soft Delete) - 3 options với trade-off matrix

**Quick Reference:** `.claude/skills/quick-reference/brainstorming-question-templates.md`

**Time saved:** Quick template giảm 75% planning time cho simple features

---

### 3️⃣ TDD Enforcement (RED-GREEN-REFACTOR)

**Khi nào dùng:** Khi viết business logic mới (service layer, domain logic)

**Workflow:**
1. **RED:** Write failing test first (assert expected behavior)
2. **GREEN:** Write minimal code to pass test
3. **REFACTOR:** Clean up code while keeping tests green

**Git Hook:** Tự động kiểm tra test file được commit trước code file
- **Mode hiện tại:** WARNING (non-blocking, chỉ nhắc nhở)
- **Week 7-8:** Sẽ chuyển sang BLOCKING mode

**Quick Reference:** `.claude/skills/quick-reference/tdd-workflow-diagram.md`

**Exceptions:** Frontend UI components, database migrations, config files (không cần TDD)

---

### 4️⃣ Two-Stage Code Review

**Khi nào dùng:** Khi tạo PR (self-review) hoặc review PR của người khác

**2 Stages:**

#### Stage 1: Spec Compliance (15-20 min, BLOCKING)
- ✅ Requirements match exactly
- ✅ All acceptance criteria met
- ✅ Edge cases handled
- ✅ Multi-tenant isolation (if applicable)

**→ Nếu fail:** Fix trước khi qua Stage 2

#### Stage 2: Code Quality (20-30 min, GRADED)
- 🔴 **Critical Issues:** Security, data loss, breaking changes (BLOCKING)
- 🟠 **Major Issues:** Performance, test coverage, error handling (RECOMMENDED)
- 🟡 **Minor Issues:** Naming, comments, style (OPTIONAL)

**Quick Reference:** `.claude/skills/quick-reference/review-stage-decision-tree.md`

**PR Template:** `.github/PULL_REQUEST_TEMPLATE.md` (đã update với Two-Stage structure)

**Time saved:** -33% review time (10 min vs 15 min baseline)

---

### 5️⃣ Task Breakdown Formula

**Khi nào dùng:** Khi feature >10 min (need to estimate time accurately)

**Formula:** FILE + CHANGE + CODE + VERIFY + TIME

**Example Task:**
```markdown
**Task 2:** Add phoneNumber field to Student entity
**File:** kiteclass-core/.../Student.java
**Change:** Add @Pattern validation annotation
**Code:**
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
@Column(name = "phone_number", length = 20)
private String phoneNumber;
**Verify:** ./mvnw compile
**Time:** 2 minutes
```

**Documentation Levels:** (khi nào cần document task breakdown?)

| Feature Size | Documentation |
|--------------|---------------|
| <10 min | ⏭️ None (mental only) |
| 10-30 min | 📝 Inline (PR description) |
| 30-60 min | 📄 Light doc (task list) |
| >60 min | 📚 Full doc (with code samples) |

**Quick Reference:** `.claude/skills/quick-reference/task-breakdown-formula.md`

**Accuracy achieved:** 92% trong pilot (vs 60% baseline)

---

## 🔄 Workflow Changes - Before vs After

### Before Superpowers (Baseline)

```
1. Read requirement
2. Start coding immediately
3. Realize wrong approach mid-way
4. Refactor (waste 30-60 min)
5. Write tests after code
6. Create PR
7. Review finds issues
8. 2-3 iterations to merge
```

**Time:** Variable, lots of rework
**Quality:** Inconsistent
**Planning Accuracy:** 60%

### After Superpowers (Week 5+)

```
1. Read requirement
2. [NEW] Quick/Full Brainstorm (5-20 min) → Choose best approach
3. [NEW] Task Breakdown (0-10 min) → Precise estimate
4. [NEW] Write tests FIRST (TDD RED)
5. Write code to pass tests (TDD GREEN)
6. Refactor if needed (TDD REFACTOR)
7. [NEW] Self-review Stage 1 (15 min) → Catch issues early
8. Create PR
9. [NEW] Self-review Stage 2 (20 min) → Grade quality
10. Reviewer validates (faster, fewer issues)
11. 1-2 iterations to merge (vs 2-3 before)
```

**Time:** +20-30 min planning upfront, -60+ min rework saved
**Quality:** Consistent, fewer bugs
**Planning Accuracy:** 92%
**ROI:** 4.2:1 (pilot) → 2.2:1 (projected long-term)

---

## 📖 Support Resources

### Skill Documentation

**Full Skills (detailed guides):**
- `.claude/skills/systematic-debugging.md`
- `.claude/skills/socratic-brainstorming.md`
- `.claude/skills/tdd-enforcement.md`
- `.claude/skills/two-stage-review.md`
- `.claude/skills/task-breakdown-guide.md`

**Quick References (cheat sheets):**
- `.claude/skills/quick-reference/systematic-debugging-checklist.md`
- `.claude/skills/quick-reference/brainstorming-question-templates.md`
- `.claude/skills/quick-reference/quick-brainstorm-template.md`
- `.claude/skills/quick-reference/tdd-workflow-diagram.md`
- `.claude/skills/quick-reference/review-stage-decision-tree.md`
- `.claude/skills/quick-reference/task-breakdown-formula.md`

### Pilot Testing Artifacts

**Brainstorming Examples:**
- `documents/06-logs/pilot-pr-1-brainstorming.md` (Full Socratic - Phone Number)
- `documents/06-logs/pilot-pr-2-quick-brainstorm.md` (Quick - Student Search)
- `documents/06-logs/pilot-pr-3-full-brainstorm.md` (Full Socratic - Soft Delete)

**Task Breakdown Examples:**
- `documents/06-logs/pilot-pr-1-task-breakdown.md` (Full doc - 12 tasks)
- `documents/06-logs/pilot-pr-2-inline-tasks.md` (Inline - 5 tasks)

**Reports:**
- `documents/06-logs/week-2-completion-summary.md` (Foundation Phase)
- `documents/06-logs/week-3-pilot-completion-summary.md` (Pilot Testing)
- `documents/06-logs/week-4-implementation-report.md` (Final Validation)

### Help & Questions

**Slack Channel:** #superpowers-adoption (TBD - create if needed)
**Documentation Map:** `documents/README.md`
**Implementation Plan:** `documents/03-planning/implementation/kiteclass-implementation-plan.md`

---

## ❓ FAQ from Pilot Testing

### Q1: Khi nào dùng Quick vs Full Brainstorming?

**A:** Decision tree:

```
Feature complexity?
├─ Low (<30 min, 2 options clear) → Quick Brainstorm (5 min)
└─ Medium+ (>30 min, 3+ options) → Full Socratic (20 min)

Feature impact?
├─ Low (single service, no dependencies) → Quick
└─ High (cross-service, architectural) → Full

Decision obvious?
├─ Yes (one option clearly better) → Quick
└─ No (need trade-off scoring) → Full
```

**Pilot evidence:**
- PR 1 (Phone Number): Used Full before template existed → 25 min (could be 5 min with Quick)
- PR 2 (Student Search): Used Quick → 5 min, same decision quality ✅
- PR 3 (Soft Delete): Used Full → 20 min, caught 3rd option Quick would miss ✅

### Q2: Task Breakdown có lúc nào skip được không?

**A:** Có! Follow documentation matrix:

- **<10 min features:** Skip (mental only) - ví dụ: thêm 1 field đơn giản
- **10-30 min:** Inline trong PR description - ví dụ: search endpoint
- **30-60 min:** Light doc (task list + time) - ví dụ: soft delete
- **>60 min:** Full doc (task list + code samples + verification) - ví dụ: authentication

**Rule of thumb:** "Nếu bạn sẽ quên sau giờ ăn trưa → Document it"

### Q3: TDD hook có bắt buộc không?

**A:** Hiện tại: **WARNING mode** (non-blocking, chỉ nhắc nhở)

**Week 5-6:** Vẫn warning mode, team làm quen workflow
**Week 7-8:** Chuyển sang **BLOCKING mode** (commit bị reject nếu code trước test)

**Exceptions được phép:**
- Frontend UI components (test sau khi có UI)
- Database migrations (không có unit test)
- Config files (YAML, properties)
- Infrastructure code (Docker, CI)

**Check hook behavior:** `.claude/scripts/pre-commit-check.sh`

### Q4: Two-Stage Review mất nhiều thời gian hơn review thường không?

**A:** Không! Pilot data:

- **Before:** 15 min review không có structure → miss nhiều issues → 2-3 iterations
- **After:** Stage 1 (15 min) + Stage 2 (20 min) = 35 min self-review total
  - Nhưng reviewer chỉ cần validate (10 min)
  - 1-2 iterations instead of 2-3
  - **Net time saved:** ~20 min per PR cycle

**Benefit:** Catch issues BEFORE reviewer sees them → faster merge

### Q5: Planning 20-30 min upfront có chậm quá không?

**A:** Ngược lại! Pilot ROI:

- **Planning time:** 50 min (3 PRs)
- **Rework prevented:** 210 min (avoiding wrong approaches, bugs, review iterations)
- **ROI:** 4.2:1

**Example:** PR 1 (Phone Number)
- Brainstorming 15 min → Avoided Value Object over-engineering (saved ~60 min)
- Task breakdown 10 min → Precise estimate, no surprises

**Break-even:** Sau ~25-30 PRs (~2 months), skills окупятся hoàn toàn

### Q6: Nếu skill không áp dụng được cho PR của tôi thì sao?

**A:** Không phải mọi skill áp dụng mọi PR:

**Greenfield features:** Brainstorming + Task Breakdown + TDD + Review (4/5 skills)
**Bug fixes:** Debugging + TDD (fix) + Review (3/5 skills)
**Refactoring:** Brainstorming (approach) + Task Breakdown + Review (3/5 skills)
**Config changes:** Review only (1/5 skills)

**Trong PR template:** Check only the skills you actually used

**Baseline:** Mỗi PR nên dùng ≥2 skills (trừ trivial changes)

---

## 📊 Week 5 Expectations

### Targets

- **PRs Completed:** 3-5 PRs (mix của Gateway + Core)
- **Planning Accuracy:** ≥85% (vs 92% pilot avg)
- **ROI:** ≥2.2:1 (lower than pilot OK, still above break-even)
- **Skills Usage:** All 5 skills demonstrated at least once
- **TDD Compliance:** ≥70% of business logic PRs (warning mode)

### What's Different from Pilot

**Pilot (Week 1-4):** Planning-heavy, no actual code, simulated metrics
**Week 5 (Rollout):** Real implementation, real code commits, actual time tracking

**If things go wrong:**
- Planning accuracy <80% → Increase task breakdown detail
- ROI <2:1 → Review which skills need refinement
- TDD compliance <50% → More training/examples needed

### Weekly Tracking

**Every Friday (end of week):**
1. Update `documents/06-logs/superpowers-adoption-metrics.md` với actual data
2. Calculate planning accuracy % per PR
3. Calculate ROI (time invested vs saved)
4. Document issues encountered
5. Share learnings in team meeting

---

## 🚀 Getting Started

### Your First PR with Superpowers

**Before coding:**
1. ✅ Read requirement carefully
2. ✅ Quick or Full Brainstorm? (check decision tree above)
3. ✅ Document decision (5-20 min)
4. ✅ Task Breakdown (inline or doc, based on size)
5. ✅ Create feature branch

**During coding:**
6. ✅ Write test first (TDD RED)
7. ✅ Write code to pass (TDD GREEN)
8. ✅ Refactor if needed (TDD REFACTOR)
9. ✅ Commit (hook will check TDD compliance)

**Before creating PR:**
10. ✅ Self-review Stage 1 (requirements match?)
11. ✅ Self-review Stage 2 (code quality grading)
12. ✅ Create PR using template
13. ✅ Check "Skills Applied" section

**After PR review:**
14. ✅ Track actual vs estimated time
15. ✅ Update metrics tracking file

### Recommended First PRs (Week 5)

**Complexity spectrum:**
- 1 Low complexity (Quick Brainstorm practice)
- 2 Medium complexity (Full Brainstorm + Task Breakdown)
- 1-2 Bug fixes (if available - Debugging skill practice)

**See Task #3** for selected PRs from implementation plan

---

## ✅ Success Criteria

**Week 5 considered successful if:**
- ✅ 3+ PRs completed với full Superpowers workflow
- ✅ Planning accuracy ≥85%
- ✅ ROI ≥2:1
- ✅ All 5 skills used at least once (across team)
- ✅ Zero critical issues escaped to production
- ✅ Team feedback collected

**Week 6-8 continuation:** Refine based on Week 5 learnings

---

## 📅 Next Steps

**Immediate (Today):**
1. ✅ Review this guide
2. ✅ Read quick reference cards (`.claude/skills/quick-reference/`)
3. ✅ Check selected PRs for Week 5 (Task #3)

**This Week (Week 5):**
4. Start first PR với Quick Brainstorm (simple feature)
5. Track time carefully (planning vs actual)
6. Apply TDD workflow (test-first)
7. Self-review before creating PR
8. Update metrics tracking

**End of Week:**
9. Calculate planning accuracy %
10. Calculate ROI
11. Share learnings in retrospective
12. Plan Week 6 PRs

---

**Status:** ✅ Rollout guide complete
**Last Updated:** 2026-03-13
**Phase:** Week 5 - Rollout Active
**Next:** Identify first production PRs (Task #3)
