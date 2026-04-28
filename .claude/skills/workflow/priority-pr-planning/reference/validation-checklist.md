# Validation Checklist for Priority Plans + Quality Gate Review

> Pointer: read this for self-review before posting the plan; ALL items must pass. Parent skill: `../SKILL.md`.

## 🔍 Validation Checklist for Priority Plans

Before executing a priority plan, verify:

### Plan Completeness
- [ ] All quality standards from master plan included
- [ ] All workflow references point to `.claude/skills/`
- [ ] Each PR has complete 7-section structure
- [ ] Testing checklist covers unit + integration + regression
- [ ] Commit messages follow Conventional Commits
- [ ] CI validation steps with gh CLI
- [ ] Acceptance criteria comprehensive

### Code Quality
- [ ] Migration files have comments and version numbers
- [ ] Repository methods have JavaDoc
- [ ] Service layer has proper error handling
- [ ] Tests cover happy path + edge cases + errors
- [ ] Multi-tenant isolation verified in tests

### Documentation
- [ ] Problem statement clear and actionable
- [ ] Implementation steps detailed enough to execute
- [ ] References to skills documented
- [ ] Next steps after completion defined

---

## ✅ Quality Gate: Plan Review Checklist

Before executing a priority plan, review:

### Compliance
- [ ] All quality standards from master plan copied verbatim
- [ ] All workflow steps reference `.claude/skills/` (not custom workflow)
- [ ] Branch naming follows convention: `{type}/KC-{id}-{desc}`
- [ ] Commit format uses Conventional Commits with HEREDOC
- [ ] PR creation uses `gh pr create` with detailed body

### Completeness
- [ ] Problem statement with root cause analysis
- [ ] Implementation steps with file paths and code
- [ ] Testing checklist: unit + integration + regression
- [ ] Commit strategy with HEREDOC example
- [ ] CI validation with gh CLI commands
- [ ] Acceptance criteria comprehensive (8+ items)

### Code Quality
- [ ] JavaDoc for all public methods
- [ ] Error codes from `messages.properties`
- [ ] Multi-tenant with `instance_id` + Hibernate filters
- [ ] Soft delete with `deleted` flag
- [ ] Audit fields: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- [ ] Input validation with Jakarta Bean Validation

### Testing
- [ ] Unit tests with mocked dependencies
- [ ] Integration tests with Testcontainers
- [ ] Multi-tenant isolation tests
- [ ] Error scenario tests (4xx, 5xx)
- [ ] Regression: all existing tests still pass

### Documentation
- [ ] Clear problem statement
- [ ] Detailed implementation steps
- [ ] References to skills documented
- [ ] Next steps defined
- [ ] Links to master plan PRs
