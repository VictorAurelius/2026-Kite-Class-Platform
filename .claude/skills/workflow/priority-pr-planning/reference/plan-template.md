# Priority Plan Template Structure

> Pointer: read this when filling the template — section-by-section guide. Parent skill: `../SKILL.md`.

## 📝 Required Sections

```markdown
# Priority PRs Execution Plan - YYYY-MM-DD

## 📋 OVERVIEW
- Priority Queue (list all PRs in order)
- Prerequisites Checklist
- Estimated total time

## ⚠️ PRIORITY 1: [PR Name]
**Branch:** {type}/KC-{id}-{desc}
**Service:** Core/Gateway/Frontend
**Time:** X hours
**Ticket:** KC-{id}

### 1. Problem Statement
- Clear description of the issue
- Root cause analysis
- Impact assessment

### 2. Workflow: Feature Branch Creation
**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

```bash
git checkout main
git pull origin main
git checkout -b {branch-name}
git branch --show-current
```

### 3. Implementation Steps
- Detailed code changes for each file
- Complete with file paths
- Include JavaDoc/comments
- Reference skills where applicable

### 4. Testing Checklist
**Reference:** `.claude/skills/testing-guide.md`

#### 4.1 Unit Tests
- Test cases with expected results

#### 4.2 Integration Tests
- @SpringBootTest or @WebMvcTest patterns
- TestContainers setup if needed

#### 4.3 Regression Tests
```bash
./mvnw clean test
# Expected: X tests, 0 failures
```

### 5. Commit Strategy
**Reference:** `.claude/skills/development-workflow.md` - Section "Commit Messages"

```bash
git add .
git commit -m "$(cat <<'EOF'
{type}({service}): {subject}

Changes:
- Detail 1
- Detail 2

Tests: X passing (was Y, +Z new)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```
<!-- TODO: verify against current state — current CLAUDE.md says do NOT add Co-Authored-By trailer; example kept verbatim from v1.0 monolith -->

### 6. CI Validation
```bash
# Ask user first
git push -u origin {branch-name}

# Create PR
gh pr create --title "..." --body "..."

# Monitor CI
scripts/check-ci.sh

# Merge when green
gh pr merge --squash --delete-branch
```

### 7. Acceptance Criteria
- [ ] Quality standards checklist (copy from above)
- [ ] All tests passing
- [ ] CI green
- [ ] Documentation updated
- [ ] No regression

## 📊 EXECUTION SUMMARY
- Timeline table
- Success metrics
- Next steps after completion
```
