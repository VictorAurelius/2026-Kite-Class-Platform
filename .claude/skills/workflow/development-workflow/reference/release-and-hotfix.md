# Release & Hotfix Processes

> Pointer: read this when cutting a release tag or handling a production hotfix. Parent skill: `../SKILL.md`.

## 🚀 Release Process

### 1. Create Release Branch

```bash
# From develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0
```

### 2. Version Bump

```bash
# Update version in:
# - pom.xml: <version>1.2.0</version>
# - package.json: "version": "1.2.0"

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
1. Run database migration: `flyway migrate`
2. Update environment variables (see `.env.example`)
```

### 4. Merge to Main & Tag

```bash
# Merge to main
git checkout main
git merge release/v1.2.0 --no-ff -m "Release v1.2.0"
git tag -a v1.2.0 -m "Version 1.2.0"

# ❌ AI stops here - User must push manually
# git push origin main --tags

# Merge back to develop
git checkout develop
git merge release/v1.2.0 --no-ff

# ❌ User must push manually
# git push origin develop

# Delete release branch (local only)
git branch -d release/v1.2.0

# ❌ User deletes remote branch
# git push origin --delete release/v1.2.0
```

---

## 🔥 Hotfix Process

**ONLY for critical production bugs!**

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

# ❌ AI stops here - User must push manually
# git push origin main --tags

# 4. Merge to develop
git checkout develop
git merge hotfix/KC-999-critical-fix --no-ff

# ❌ User must push manually
# git push origin develop

# 5. Cleanup (local only)
git branch -d hotfix/KC-999-critical-fix

# ❌ User deletes remote branch
# git push origin --delete hotfix/KC-999-critical-fix
```
