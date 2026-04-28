# Troubleshooting & Quick Reference

> Pointer: read this when stuck — common git issues, quick fixes, aliases. Parent skill: `../SKILL.md`.

## Common Issues & Solutions

### Issue: Merge Conflicts

**Solution:**
```bash
# Update from develop
git checkout feature/your-branch
git fetch origin
git merge origin/develop

# Resolve conflicts in IDE
# Then commit
git add .
git commit -m "chore: resolve merge conflicts"
```

### Issue: Accidental Commit to Wrong Branch

**Solution:**
```bash
# Undo last commit (keep changes)
git reset HEAD~1 --soft

# Switch to correct branch
git checkout correct-branch

# Commit again
git add .
git commit -m "your message"
```

### Issue: Need to Amend Last Commit

**Solution:**
```bash
# Make additional changes
git add .

# Amend last commit (without editing message)
git commit --amend --no-edit

# Or amend with new message
git commit --amend
```

---

## 📊 Quick Reference

### Git Aliases (Recommended)

Add to `~/.gitconfig`:

```bash
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

### Common Commands

```bash
# Start new feature
git feature KC-123-new-feature

# View pretty history
git lg

# Check branch status
git status

# Quick commit all changes
git add -A && git commit -m "feat: quick description"

# ❌ AI CANNOT push - User only
# git push -u origin feature/KC-123-new-feature
```
