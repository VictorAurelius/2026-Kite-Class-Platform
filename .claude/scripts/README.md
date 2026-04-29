# Skills Compliance Scripts

Automation scripts để đảm bảo code tuân thủ project skills.

## 📋 Available Scripts

### pre-commit-check.sh

**Purpose:** Kiểm tra skills compliance trước khi commit

**Checks:**
1. ✅ JavaDoc cho public methods
2. ✅ Error code usage (không hardcode messages)
3. ✅ @since annotations
4. ✅ Import ordering (không dùng wildcards)
5. ✅ Sensitive data detection
6. ✅ messages.properties updates

**Usage:**

```bash
# Run manually before commit
./pre-commit-check.sh

# Or install as git hook (runs automatically)
cd /path/to/repository
ln -s ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

**Output Example:**

```
🔍 Running Skills Compliance Check...

📝 Checking JavaDoc compliance...
✅ JavaDoc compliance OK

🚨 Checking error code usage...
✅ Error code usage OK

📅 Checking @since annotations...
✅ @since annotations OK

📦 Checking import ordering...
✅ Import ordering OK

🔐 Checking for sensitive data...
✅ No sensitive data detected

🌐 Checking messages.properties...
✅ Messages.properties check OK

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All checks passed! Safe to commit.
```

## 🛠️ Installation Guide

### Option 1: Manual Check

Chạy script thủ công mỗi lần trước commit:

```bash
cd /mnt/e/person/2026-Kite-Class-Platform
./.claude/scripts/pre-commit-check.sh
```

### Option 2: Git Hook (Recommended)

Tự động chạy mỗi lần `git commit`:

```bash
# Make script executable
chmod +x .claude/scripts/pre-commit-check.sh

# Create symlink to git hooks
ln -s ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit

# Test it
git commit -m "test"
```

### Option 3: IDE Integration

**IntelliJ IDEA:**
1. Settings → Tools → External Tools
2. Add New Tool:
   - Name: `Skills Compliance Check`
   - Program: `/bin/bash`
   - Arguments: `$ProjectFileDir$/.claude/scripts/pre-commit-check.sh`
   - Working Directory: `$ProjectFileDir$`
3. Assign keyboard shortcut (Keymap → External Tools)

**VS Code:**
1. Create task in `.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Skills Compliance Check",
      "type": "shell",
      "command": "${workspaceFolder}/.claude/scripts/pre-commit-check.sh",
      "problemMatcher": [],
      "group": {
        "kind": "build",
        "isDefault": false
      }
    }
  ]
}
```

2. Run: `Ctrl+Shift+P` → `Tasks: Run Task` → `Skills Compliance Check`

## 🔧 Customization

### Add New Checks

Edit `pre-commit-check.sh` and add new section:

```bash
# ==============================================================================
# 7. Check Your Custom Rule
# ==============================================================================
echo "🔍 Checking custom rule..."

# Your check logic here
if [ condition ]; then
    echo -e "${RED}❌ Violation found${NC}"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Custom check OK${NC}"
fi
echo ""
```

### Disable Specific Checks

Comment out the section you want to disable:

```bash
# ==============================================================================
# 3. Check @since Annotations (DISABLED)
# ==============================================================================
# echo "📅 Checking @since annotations..."
# ... (commented out)
```

## 📊 Compliance Report

Sau khi chạy script, nếu có violations:

```
❌ Found 3 compliance issue(s)

Please fix the issues above before committing.

Resources:
- Full checklist: .claude/skills/skills-compliance-checklist.md
- Code style: .claude/skills/code-style.md
- Error logging: .claude/skills/error-logging.md
```

## 🚫 Skip Check (Emergency Only)

Nếu thực sự cần commit without check (không khuyến khích):

```bash
git commit --no-verify -m "emergency fix"
```

## 📚 Related Skills

- `skills-compliance-checklist.md` - Complete checklist
- `code-style.md` - Code style guide
- `error-logging.md` - Error handling guide
- `development-workflow.md` - Git workflow

## 🤝 Contributing

To improve this script:
1. Test changes thoroughly
2. Update this README
3. Document new checks in skills-compliance-checklist.md

## ❓ Troubleshooting

### Script Not Running

```bash
# Make executable
chmod +x .claude/scripts/pre-commit-check.sh

# Check symlink
ls -la .git/hooks/pre-commit
```

### False Positives

If script detects false violations:
- Review the pattern in script
- Add exception handling
- Document known false positives

### Performance Issues

For large commits, the script may be slow. Options:
- Run checks only on staged files
- Use incremental checking
- Disable heavy checks for hotfixes

---

**Last Updated:** 2026-04-29
**Author:** KiteClass Team
**Related:** skills-compliance-checklist.md
