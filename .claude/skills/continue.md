# /continue - Execute Next Priority Action

When the user invokes /continue, follow this exact process:

## Step 1: Determine Priority Action

Read these planning documents in order to find the highest priority unfinished action:

1. `documents/03-planning/local-prod-separation-plan.md` - Track A/B/C status
2. `documents/03-planning/local-e2e-roadmap.md` - E2E roadmap status
3. `documents/03-planning/ui-refactor-plan.md` - UI PRs status
4. `documents/03-planning/kiteclass-theme-system-design.md` - Theme PRs status
5. `documents/03-planning/ai-local-implementation-plan.md` - AI PRs status

Priority order:
- 🔴 P0 items first (blocking, critical path)
- 🟠 P1 next (important, not blocking)
- 🟡 P2 last (nice to have)

If all plans are complete, check for:
- Open GitHub PRs that need merge
- CI failures that need fixing
- Stale branches to cleanup
- E2E tests to verify (run `./scripts/test-api-e2e.sh`)

## Step 2: Report to User

Before starting work, briefly report:
```
## Tiếp tục: [PR/Task name]
**Priority**: P0/P1/P2
**Plan**: [which plan document]
**Scope**: [1-2 sentence description]
```

## Step 3: Execute with Superpowers Methodology

MUST follow this process for every PR:

### 3.1 Quick Brainstorm
- Phân tích scope, risks, edge cases
- Xác định dependencies và blockers

### 3.2 Task Breakdown
- Chia nhỏ thành tasks cụ thể
- Estimate effort cho mỗi task

### 3.3 TDD - Test First (cho code changes)
- Viết tests TRƯỚC khi viết code
- Commit tests (RED phase)

### 3.4 Implementation
- Implement theo task breakdown
- Commit thường xuyên

### 3.5 Verify
- Rebuild services nếu Docker changes
- Run E2E tests: `./scripts/test-api-e2e.sh`
- Run unit tests nếu Java changes: `JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw clean test -pl <module> -am`
- PHẢI verify pass trước khi push

### 3.6 Push & PR
- Push branch
- Create PR with clear description
- Watch CI: `gh pr checks <number> --watch`
- Report PR URL to user
- KHÔNG tự merge - chờ user approve

## Step 4: Update Plan

After PR is created/merged, update the relevant plan document:
- Mark completed items ✅
- Add PR number
- Update status

## Rules
- ALWAYS communicate in Vietnamese
- ALWAYS create feature branch (never commit to main)
- ALWAYS run tests after code changes
- NEVER merge without user approval
- NEVER skip brainstorm/TDD steps
