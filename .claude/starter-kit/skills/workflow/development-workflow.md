# Portable Skill — adapt {project} placeholders

# Development Workflow

**Purpose:** Quy trinh phat trien tu planning den deployment — git, commits, PR, testing, review.

---

## Git Branching Strategy

```
main ────●────────────●────────────●──► (Production)
         │            ▲            ▲
develop ─┼──●──●──●───┼──●──●─────┼──► (Integration)
         │  └─feature──┘  └─fix───┘
```

| Branch | Pattern | From | Merge To | Method |
|--------|---------|------|----------|--------|
| `main` | `main` | - | - | - |
| `develop` | `develop` | `main` | `main` | Merge commit |
| `feature` | `feature/{ticket}-{desc}` | `develop` | `develop` | Squash merge |
| `bugfix` | `bugfix/{ticket}-{desc}` | `develop` | `develop` | Squash merge |
| `hotfix` | `hotfix/{ticket}-{desc}` | `main` | `main` + `develop` | Merge commit |
| `release` | `release/v{version}` | `develop` | `main` + `develop` | Merge commit |

**Branch naming rules:**
- Lowercase only, dashes instead of spaces
- Always include ticket ID
- Keep short (< 50 chars)

---

## Commit Messages (Conventional Commits)

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `style` | Formatting (no logic change) |
| `refactor` | Refactoring |
| `test` | Tests |
| `chore` | Build, CI, tooling |
| `perf` | Performance |

**Rules:**
- Subject: imperative mood, lowercase, no period, < 72 chars
- Body: wrap at 72 chars, explain what & why
- Footer: reference tickets, breaking changes
- Co-Authored-By: include for AI assistance

**Complex commit example (HEREDOC):**
```bash
git commit -m "$(cat <<'EOF'
feat(module): implement feature X

Features:
- Feature detail 1
- Feature detail 2

Tests: 15 passing (10 unit + 5 integration)

Co-Authored-By: AI Assistant <noreply@example.com>
EOF
)"
```

---

## PR Workflow

1. **Create feature branch** from develop
2. **Implement** with regular commits
3. **Self-test locally** (REQUIRED before push)
4. **Push** to remote
5. **Create PR** via `gh pr create`
6. **Monitor CI** until green
7. **Review + merge**

### Self-Test Before Push (MANDATORY)

```bash
# ALWAYS use the project test script — do NOT run test commands ad-hoc
scripts/test-local.sh              # Auto-detect changed modules
scripts/test-local.sh --quick      # Compile + lint only
scripts/test-local.sh {project} all  # Full test suite
```

If tests fail, fix BEFORE pushing. CI is a safety net, not first-pass detection.

### Forbidden Git Operations

- `git push --force` (never without explicit user request)
- `git push origin main` (use PR workflow)
- `git reset --hard`, `git clean -f` (destructive)

---

## Development Phases

### Phase 1: Planning
- [ ] Database/API design mapped
- [ ] Tasks broken down with estimates
- [ ] Dependencies identified

### Phase 2: Implementation
- [ ] Code follows project style guidelines
- [ ] Comments for complex logic only (no obvious comments)
- [ ] No commented-out code

### Phase 3: Testing
- [ ] Unit tests >= 80% coverage
- [ ] Integration tests for critical paths
- [ ] All tests passing locally

### Phase 4: Documentation
- [ ] Implementation plan status updated
- [ ] Module/service docs updated if applicable
- [ ] API specs updated if endpoints changed

### Phase 5: Review & Commit
- [ ] Self-review completed (security, errors, performance)
- [ ] No compilation/lint warnings
- [ ] Conventional commit message with test results

### Phase 6: Merge
- [ ] PR created with description template
- [ ] CI pipeline green
- [ ] Code review approved

---

## PR Description Template

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation update

## Related Tickets
- Closes {TICKET-ID}

## Changes Made
- Change 1
- Change 2

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-reviewed
- [ ] Tests added/updated
- [ ] All tests passing locally
- [ ] Documentation updated
```

---

## Warning Policy

| Warning Type | Action |
|--------------|--------|
| Compilation warning | MUST fix before merge |
| Deprecated API | Must have upgrade plan |
| Security warning | CANNOT merge |
| Performance warning | Review and document |

---

## Hotfix Process

Only for critical production bugs:
1. Branch from `main`: `hotfix/{ticket}-{desc}`
2. Fix, test, commit
3. Merge to `main` with tag + merge back to `develop`
