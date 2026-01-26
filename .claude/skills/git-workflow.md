# Skill: Git Workflow

Quy trình làm việc với Git cho dự án KiteClass Platform.

## Mô tả

Tài liệu quy định:
- Branching strategy (Git Flow)
- Commit message conventions
- Pull Request process
- Code review guidelines
- Release process

## Trigger phrases

- "git workflow"
- "branching strategy"
- "commit message"
- "pull request"
- "code review"

---

## Branching Strategy

### Git Flow Model

```
main ────●────────────────●────────────────●────────────────●─────► (Production)
         │                ▲                ▲                ▲
         │                │                │                │
develop ─┼──●──●──●──●────┼────●──●──●─────┼────●──●────────┼─────► (Integration)
         │  │             │    │           │    │           │
         │  │  feature/   │    │           │    │           │
         │  └──KC-123 ────┘    │           │    │           │
         │                     │           │    │           │
         │        feature/     │           │    │           │
         │        KC-456 ──────┘           │    │           │
         │                                 │    │           │
         │                    hotfix/      │    │           │
         │                    KC-789 ──────┴────┘           │
         │                                                  │
         │                                      release/    │
         │                                      v1.0 ───────┘
```

### Branch Types

| Branch | Pattern | Từ | Merge vào | Mục đích |
|--------|---------|-----|-----------|----------|
| `main` | `main` | - | - | Production code |
| `develop` | `develop` | `main` | `main` | Integration branch |
| `feature` | `feature/KC-{id}-{desc}` | `develop` | `develop` | New features |
| `bugfix` | `bugfix/KC-{id}-{desc}` | `develop` | `develop` | Bug fixes |
| `hotfix` | `hotfix/KC-{id}-{desc}` | `main` | `main`, `develop` | Urgent fixes |
| `release` | `release/v{version}` | `develop` | `main`, `develop` | Release prep |

---

## Branch Naming

### Format

```
{type}/KC-{ticket-id}-{short-description}
```

### Examples

```bash
# Features
feature/KC-123-student-enrollment
feature/KC-456-qr-payment-integration
feature/KC-789-parent-notification

# Bug fixes
bugfix/KC-321-fix-attendance-calculation
bugfix/KC-654-invoice-duplicate-issue

# Hotfixes
hotfix/KC-999-security-vulnerability
hotfix/KC-888-payment-gateway-down

# Releases
release/v1.0.0
release/v1.1.0
```

### Rules

- Chỉ dùng lowercase
- Dùng dấu `-` thay cho space
- Giữ ngắn gọn (< 50 chars)
- Luôn có ticket ID

---

## Commit Messages

### Format (Conventional Commits)

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Types

| Type | Mô tả | Ví dụ |
|------|-------|-------|
| `feat` | Tính năng mới | `feat(student): add enrollment API` |
| `fix` | Sửa lỗi | `fix(attendance): correct calculation` |
| `docs` | Documentation | `docs(api): update swagger specs` |
| `style` | Formatting (không ảnh hưởng code) | `style: format with prettier` |
| `refactor` | Refactoring (không thêm feature/fix bug) | `refactor(billing): extract payment service` |
| `test` | Thêm/sửa tests | `test(student): add unit tests` |
| `chore` | Build, CI, tooling | `chore: update dependencies` |
| `perf` | Performance improvement | `perf(query): optimize student list` |

### Scopes (Optional)

```
student, class, attendance, billing, auth, api, ui, db, ci
```

### Examples

```bash
# Feature
feat(student): add bulk import from Excel

Implement Excel file upload and parsing for batch student creation.
- Support .xlsx and .xls formats
- Validate required fields
- Return detailed error report

Closes KC-123

# Bug fix
fix(attendance): correct percentage calculation

The attendance percentage was including cancelled sessions.
Now only counts active sessions.

Fixes KC-456

# Breaking change
feat(api)!: change pagination response format

BREAKING CHANGE: Pagination now uses `content` instead of `data`.
Migration guide in docs/migration/v2.md

# Multiple changes
feat(billing): add VNPay integration

- Add VNPay payment gateway
- Implement QR code generation
- Add payment callback handler
- Update invoice status on success

Closes KC-789, KC-790
```

### Rules

- Subject: Imperative mood, lowercase, no period (< 72 chars)
- Body: Wrap at 72 chars, explain what & why
- Footer: Reference tickets, breaking changes

---

## Pull Request Process

### 1. Tạo PR

```bash
# Sync với develop
git checkout develop
git pull origin develop

# Tạo feature branch
git checkout -b feature/KC-123-new-feature

# Code và commit
git add .
git commit -m "feat(module): implement feature"

# Push và tạo PR
git push -u origin feature/KC-123-new-feature
```

### 2. PR Template

```markdown
## Description
<!-- Mô tả ngắn gọn thay đổi -->

Implement student bulk import feature allowing admins to upload Excel files.

## Type of Change
- [x] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation update

## Related Tickets
- Closes KC-123
- Related to KC-456

## Changes Made
- Added Excel upload endpoint
- Implemented file parsing service
- Added validation for required fields
- Created error report generator

## Screenshots (if applicable)
<!-- Đính kèm screenshots cho UI changes -->

## Checklist
- [x] Code follows project style guidelines
- [x] Self-reviewed my code
- [x] Added/updated comments for complex logic
- [x] Updated documentation if needed
- [x] Added unit tests
- [x] All tests passing locally
- [ ] No new warnings

## Testing Instructions
1. Go to Students → Import
2. Upload the sample file from `test/fixtures/students.xlsx`
3. Verify import results

## Additional Notes
<!-- Ghi chú thêm cho reviewers -->
```

### 3. Review Process

```
┌─────────────────────────────────────────────────────────────┐
│                    PR REVIEW WORKFLOW                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  PR Created ──► CI Checks ──► Code Review ──► Approved       │
│       │             │              │              │          │
│       │             │              │              ▼          │
│       │             │              │         Merge to        │
│       │             │              │         develop         │
│       │             │              │                         │
│       │             ▼              ▼                         │
│       │        ❌ Failed      Request                       │
│       │             │         Changes                        │
│       │             │              │                         │
│       │             └──────────────┼──────────┐              │
│       │                            │          │              │
│       │                            ▼          │              │
│       └────────────────────► Fix & Push ──────┘              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4. Review Checklist

**Reviewer phải kiểm tra:**

- [ ] Code đúng requirement
- [ ] Không có security issues
- [ ] Xử lý error cases
- [ ] Có unit tests
- [ ] Code clean, readable
- [ ] Không có duplicate code
- [ ] Performance acceptable

### 5. Merge Strategy

| Branch | Merge Method | Lý do |
|--------|--------------|-------|
| feature → develop | **Squash merge** | Clean history |
| release → main | **Merge commit** | Preserve release commits |
| hotfix → main | **Merge commit** | Audit trail |
| main → develop | **Merge commit** | Sync changes |

---

## Release Process

### 1. Create Release Branch

```bash
# Từ develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0
```

### 2. Version Bump

```bash
# Update version
# pom.xml: <version>1.2.0</version>
# package.json: "version": "1.2.0"

git add .
git commit -m "chore: bump version to 1.2.0"
```

### 3. Release Notes

```markdown
# Release v1.2.0

## New Features
- KC-123: Student bulk import
- KC-456: QR payment integration

## Bug Fixes
- KC-789: Fix attendance calculation

## Breaking Changes
- None

## Migration Steps
1. Run database migration
2. Update environment variables
```

### 4. Merge to Main

```bash
# Merge to main
git checkout main
git merge release/v1.2.0 --no-ff -m "Release v1.2.0"
git tag -a v1.2.0 -m "Version 1.2.0"
git push origin main --tags

# Merge back to develop
git checkout develop
git merge release/v1.2.0 --no-ff
git push origin develop

# Delete release branch
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

---

## Hotfix Process

```bash
# 1. Create from main
git checkout main
git pull origin main
git checkout -b hotfix/KC-999-critical-fix

# 2. Fix and commit
git add .
git commit -m "fix(auth): patch security vulnerability"

# 3. Merge to main
git checkout main
git merge hotfix/KC-999-critical-fix --no-ff
git tag -a v1.2.1 -m "Hotfix v1.2.1"
git push origin main --tags

# 4. Merge to develop
git checkout develop
git merge hotfix/KC-999-critical-fix --no-ff
git push origin develop

# 5. Cleanup
git branch -d hotfix/KC-999-critical-fix
```

---

## Git Aliases (Recommended)

```bash
# ~/.gitconfig
[alias]
    co = checkout
    br = branch
    ci = commit
    st = status

    # Feature workflow
    feature = "!f() { git checkout develop && git pull && git checkout -b feature/$1; }; f"

    # Pretty log
    lg = log --oneline --graph --decorate --all

    # Amend without edit
    amend = commit --amend --no-edit

    # Undo last commit (keep changes)
    undo = reset HEAD~1 --soft
```

## Actions

### Tạo feature branch
```bash
git feature KC-123-my-feature
```

### Xem history đẹp
```bash
git lg
```
