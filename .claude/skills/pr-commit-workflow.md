# Skill: PR Commit Workflow

**IMPORTANT:** This skill MUST be followed for ALL code changes. Always commit code before session ends or context clears.

## Trigger Phrases
- "commit code"
- "git commit"
- "commit changes"
- "save changes"
- After completing implementation work
- Before session ends

## When to Use
- After completing any PR implementation
- Before asking user questions
- When ready to document changes
- At end of development session

## Workflow

### 1. Check Git Status
```bash
git status
```

### 2. Stage All Changes
```bash
git add -A
```

### 3. Commit with Conventional Commit Message

**Format:**
```
<type>(<scope>): <subject>

<body>

Features:
- Feature 1
- Feature 2

Changes:
- Change 1
- Change 2

Tests:
- Test results

Files: X files changed

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `test`: Adding tests
- `refactor`: Code refactoring
- `chore`: Build, config changes

**Example:**
```bash
git commit -m "$(cat <<'EOF'
feat(gateway): implement PR 1.5 - Email Service

Implement complete email service with password reset functionality.

Features:
- Email service with reactive patterns
- Password reset flow
- HTML email templates

Tests:
- 5 unit tests (all passing)
- 8 integration tests

Files: 19 files

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

### 4. Verify Commit
```bash
git log -1 --stat
```

## Rules

1. **ALWAYS commit after implementing features**
2. **NEVER skip commit step**
3. **ALWAYS use Co-Authored-By for AI assistance**
4. **ALWAYS include test results**
5. **ALWAYS list major changes**

## Checklist

- [ ] Code compiled successfully
- [ ] Tests passing
- [ ] Changes staged
- [ ] Commit message follows convention
- [ ] Commit includes Co-Authored-By
- [ ] Verified commit with git log
