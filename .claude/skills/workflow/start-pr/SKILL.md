---
name: start-pr
description: "Dùng khi user runs `/start-pr` (check current PR artifacts) hoặc `/start-pr PR-X.Y feature-name` (start new PR). Verifies Superpowers artifacts (brainstorm, task breakdown, TDD compliance, tests, docs) and either reports status or kicks off new feature with the methodology scaffolding."
disable-model-invocation: true
---

# Start PR / Check PR Quality

**Usage:**
- `/start-pr` - Check PR hiện tại (verify artifacts, quality)
- `/start-pr PR-5.9 feature-description` - Bắt đầu PR mới

**Tool preference:** GitHub MCP if connected (`claude mcp list`), else `gh` CLI. See `.claude/rules/mcp-first-with-fallback.md`.

---

## Instructions

Khi user invoke `/start-pr $ARGUMENTS`:

### Mode 1: Check PR hiện tại (không có argument)

**Bước 1: Detect context**
```bash
# Kiểm tra branch hiện tại
git branch --show-current

# Kiểm tra uncommitted changes
git status --short
```

**Bước 2: Verify Superpowers Artifacts**

Kiểm tra và báo cáo trạng thái của từng item:

1. **Brainstorm Document**
   - Tìm trong: PR description, commit messages, hoặc file riêng
   - Phải có: scope, risks, edge cases, alternatives considered
   - Status: ✅ Có / ❌ Thiếu / ⚠️ Không đầy đủ

2. **Task Breakdown**
   - Tìm trong: PR description, commit messages, hoặc file riêng
   - Phải có: list tasks cụ thể với estimates
   - Status: ✅ Có / ❌ Thiếu / ⚠️ Không đầy đủ

3. **TDD Compliance**
   - Kiểm tra git log: test commits trước implementation commits?
   - Kiểm tra file timestamps nếu cần
   - Status: ✅ Tests trước / ❌ Code trước / ⚠️ Không rõ

4. **Code Review**
   - Đã self-review theo `.claude/skills/two-stage-code-review.md`?
   - Status: ✅ Đã review / ❌ Chưa review

**Bước 3: Output Report**

```markdown
## PR Quality Check: [branch-name]

### Superpowers Compliance

| Step | Status | Notes |
|------|--------|-------|
| 1. Brainstorm | ✅/❌/⚠️ | [details] |
| 2. Task Breakdown | ✅/❌/⚠️ | [details] |
| 3. TDD (Tests First) | ✅/❌/⚠️ | [details] |
| 4. Code Review | ✅/❌/⚠️ | [details] |

### Actions Required

[List items cần fix trước khi merge]

### Recommendations

[Suggestions to improve PR quality]
```

---

### Mode 2: Bắt đầu PR mới (có argument)

**Input:** `/start-pr PR-5.9 admin-analytics`

**Bước 1: Tạo Feature Branch**
```bash
git checkout main
git pull origin main
git checkout -b feature/PR-5.9-admin-analytics
```

**Bước 2: Quick Brainstorm (output to console)**

Thực hiện brainstorm theo `.claude/skills/brainstorming-methodology.md`:

```markdown
## Quick Brainstorm: PR-5.9 admin-analytics

### Scope
- [What features/changes are included]
- [What is explicitly OUT of scope]

### Risks
- [Technical risks]
- [Integration risks]
- [Performance risks]

### Edge Cases
- [Edge case 1]
- [Edge case 2]

### Dependencies
- [Other PRs this depends on]
- [External services/APIs needed]

### Alternatives Considered
- [Option A - why/why not]
- [Option B - why/why not]

### Decision
[Chosen approach with brief rationale]
```

**Bước 3: Task Breakdown (output to console)**

Thực hiện breakdown theo `.claude/skills/task-breakdown-guide.md`:

```markdown
## Task Breakdown: PR-5.9 admin-analytics

### Tasks

| # | Task | Estimate | Type |
|---|------|----------|------|
| 1 | [Task description] | 30 min | Test |
| 2 | [Task description] | 1 hour | Implementation |
| 3 | [Task description] | 30 min | Test |
| ... | ... | ... | ... |

### Total Estimate: X hours

### Test Files to Create First (TDD)
- [ ] `src/.../Feature.test.tsx`
- [ ] `src/.../Feature.spec.ts`

### Implementation Order
1. Write tests (RED)
2. Implement to pass tests (GREEN)
3. Refactor if needed (REFACTOR)
4. Self-review before commit
```

**Bước 4: Required Audits (auto-detect từ scope)**

Dựa trên files sẽ thay đổi, liệt kê audits cần chạy TRƯỚC merge:

```markdown
## Required Audits

| Files Changed | Required Audit | Command |
|--------------|----------------|---------|
| *-frontend/** | UI Review /128 | `/ui-review` |
| rules.md, application.yml | Business Logic /100 | `/business-logic-audit` |
| *Controller.java, *Dto.java | API Contract /100 | `/api-contract-audit` |
| pom.xml, package.json | Security /100 | `/security-audit` |
| infrastructure/**, Dockerfile | Ops Readiness /100 | `/ops-readiness-audit` |

**Applicable for this PR:** [list based on scope from brainstorm]
```

Hook `.claude/hooks/audit-gate.py` sẽ nhắc lại khi merge nếu audit bị miss.

**Bước 5: TDD Reminder**

```markdown
## TDD Workflow Reminder

1. **RED**: Write failing tests first
2. **GREEN**: Write minimal code to pass
3. **REFACTOR**: Clean up while tests pass

### Commands
- Run tests: `pnpm test` (frontend) or `./scripts/test-local.sh` (backend)
- Watch mode: `pnpm test --watch`

### KHÔNG được:
- ❌ Viết implementation trước tests
- ❌ Commit code không có tests
- ❌ Skip code review
```

---

## Related Skills

- `.claude/skills/brainstorming-methodology.md` - Full brainstorm process
- `.claude/skills/task-breakdown-guide.md` - Detailed breakdown guide
- `.claude/skills/tdd-enforcement.md` - TDD patterns
- `.claude/skills/two-stage-code-review.md` - Self-review checklist
