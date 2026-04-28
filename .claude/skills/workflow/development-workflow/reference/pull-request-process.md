# Pull Request Process

> Pointer: read this when opening a PR or filling the PR template. Parent skill: `../SKILL.md`.

## 1. Creating a Feature Branch

```bash
# Sync with develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/KC-123-new-feature

# Make changes and commit
git add .
git commit -m "feat(module): implement feature"

# ❌ AI CANNOT push - User must do this manually
# git push -u origin feature/KC-123-new-feature

# ✅ AI stops here - inform user that feature is ready for push
```

## 2. PR Template

```markdown
## Description
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

## Checklist
- [x] Code follows project style guidelines
- [x] Self-reviewed my code
- [x] Added unit tests
- [x] All tests passing locally
- [x] Updated documentation

## Testing Instructions
1. Go to Students → Import
2. Upload the sample file from `test/fixtures/students.xlsx`
3. Verify import results
```

## 3. Merge Strategy

| Branch | Merge Method | Reason |
|--------|--------------|--------|
| feature → develop | **Squash merge** | Clean history |
| release → main | **Merge commit** | Preserve release commits |
| hotfix → main | **Merge commit** | Audit trail |
| main → develop | **Merge commit** | Sync changes |
