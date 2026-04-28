# Commit Messages (Conventional Commits)

> Pointer: read this when writing a commit message or preparing a PR title. Parent skill: `../SKILL.md`.

<!-- TODO: verify against current state — CLAUDE.md says NO Co-Authored-By trailer; examples below still include it from legacy v2.0. Project rule overrides examples. -->

## Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]

Co-Authored-By: VictorAurelius <vankiet14491@gmail.com>
```

## Commit Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(student): add enrollment API` |
| `fix` | Bug fix | `fix(attendance): correct calculation` |
| `docs` | Documentation | `docs(api): update swagger specs` |
| `style` | Formatting | `style: format with prettier` |
| `refactor` | Refactoring | `refactor(billing): extract payment service` |
| `test` | Tests | `test(student): add unit tests` |
| `chore` | Build, CI, tooling | `chore: update dependencies` |
| `perf` | Performance | `perf(query): optimize student list` |

## Commit Examples

```bash
# Feature with body
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

Co-Authored-By: VictorAurelius <vankiet14491@gmail.com>

# Bug fix
fix(attendance): correct percentage calculation

The attendance percentage was including cancelled sessions.
Now only counts active sessions.

Fixes KC-456

# Breaking change
feat(api)!: change pagination response format

BREAKING CHANGE: Pagination now uses `content` instead of `data`.
Migration guide in docs/migration/v2.md
```

## Commit Message Rules

- **Subject**: Imperative mood, lowercase, no period (< 72 chars)
- **Body**: Wrap at 72 chars, explain what & why
- **Footer**: Reference tickets, breaking changes
- **Co-Authored-By**: ALWAYS include for AI assistance

## Using HEREDOC for Complex Commits

```bash
git commit -m "$(cat <<'EOF'
feat(core): implement PR 2.3 - Student Module

Complete Student module implementation with full CRUD operations.

Features:
- Student entity with soft delete
- CRUD endpoints with pagination
- Business rules validation
- Redis caching with 1-hour TTL

Tests:
- 35/41 tests passing (85% coverage)
- 6 tests pending Docker/security setup

Files: 15 files changed

Co-Authored-By: VictorAurelius <vankiet14491@gmail.com>
EOF
)"
```
