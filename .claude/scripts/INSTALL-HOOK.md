# Git Hook Installation Guide

## ✅ INSTALLATION COMPLETE

Git hook đã được cài đặt thành công tại:

```
.git/hooks/pre-commit -> ../../.claude/scripts/pre-commit-check.sh
```

## 🚀 Cách Hoạt Động

Mỗi khi bạn chạy `git commit`, hook sẽ **tự động** chạy và kiểm tra:

1. ✅ **JavaDoc** - Tất cả public methods phải có JavaDoc
2. ✅ **Error Codes** - Không hardcode error messages
3. ✅ **@since Annotations** - Files mới phải có @since
4. ✅ **Import Ordering** - Không dùng wildcard imports
5. ✅ **Sensitive Data** - Không commit passwords, API keys
6. ✅ **Messages.properties** - Update khi exception thay đổi

## 📝 Output Ví Dụ

### ✅ Khi Tất Cả Checks Pass

```
🔍 Running Skills Compliance Check...

📝 Checking JavaDoc compliance...
✅ JavaDoc compliance OK

🚨 Checking error code usage...
✅ Error code usage OK

📅 Checking @since annotations...
✅ No new Java files to check

📦 Checking import ordering...
✅ Import ordering OK

🔐 Checking for sensitive data...
✅ No sensitive data detected

🌐 Checking messages.properties...
✅ Messages.properties check OK

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All checks passed! Safe to commit.

Next steps:
1. Stage your changes: git add -A
2. Commit with proper format: git commit -m "type(scope): description"
3. Include Co-Authored-By line

[feature/gateway-cross-service abc1234] feat(core): your commit message
 5 files changed, 120 insertions(+), 10 deletions(-)
```

Commit sẽ **thành công** và code của bạn đã tuân thủ tất cả skills! ✅

---

### ❌ Khi Có Violations

```
🔍 Running Skills Compliance Check...

📝 Checking JavaDoc compliance...
❌ Found 3 public methods potentially missing JavaDoc
   Review: code-style.md lines 110-158

🚨 Checking error code usage...
⚠️  Found 2 potential hardcoded error messages
   Expected: throw new Exception("ERROR_CODE", args)
   Review: error-logging.md lines 24-157

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ Found 2 compliance issue(s)

Please fix the issues above before committing.

Resources:
- Full checklist: .claude/skills/skills-compliance-checklist.md
- Code style: .claude/skills/code-style.md
- Error logging: .claude/skills/error-logging.md

To skip this check (not recommended):
  git commit --no-verify
```

Commit sẽ **BỊ CHẶN** cho đến khi bạn sửa các violations! ❌

---

## 🛠️ Các Lệnh Hữu Ích

### Kiểm Tra Hook Đã Cài Chưa

```bash
ls -la .git/hooks/pre-commit
# Output: lrwxrwxrwx 1 user user 41 Jan 28 07:33 .git/hooks/pre-commit -> ../../.claude/scripts/pre-commit-check.sh
```

### Chạy Thủ Công (Không Commit)

```bash
./.claude/scripts/pre-commit-check.sh
```

### Skip Hook (Khẩn Cấp ONLY)

```bash
# Nếu thực sự cần commit mà không check (KHÔNG KHUYẾN KHÍCH)
git commit --no-verify -m "emergency fix"
```

### Gỡ Hook

```bash
rm .git/hooks/pre-commit
```

### Cài Lại Hook

```bash
ln -sf ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit
```

---

## 🔍 Troubleshooting

### Hook Không Chạy?

```bash
# Kiểm tra symlink tồn tại
ls -la .git/hooks/pre-commit

# Kiểm tra script executable
ls -la .claude/scripts/pre-commit-check.sh

# Nếu không executable, fix:
chmod +x .claude/scripts/pre-commit-check.sh
```

### Hook Báo Lỗi Sai?

Hook có thể có false positives. Nếu bạn chắc code đúng:

**Option 1:** Fix pattern trong script
- Edit `.claude/scripts/pre-commit-check.sh`
- Adjust regex patterns

**Option 2:** Skip check tạm thời
```bash
git commit --no-verify -m "your message"
```

Sau đó báo với team để improve script.

---

## 🎯 Best Practices

### ✅ DO

- Let hook run và fix violations khi phát hiện
- Chạy `./.claude/scripts/pre-commit-check.sh` trước khi commit để check sớm
- Report false positives để improve script

### ❌ DON'T

- Dùng `--no-verify` thường xuyên (chỉ dùng khi khẩn cấp)
- Ignore warnings - hãy fix chúng
- Commit sensitive data (passwords, API keys)

---

## 📊 Hook Đã Cài Đặt

| Location | Status |
|----------|--------|
| `.git/hooks/pre-commit` | ✅ Installed (symlink) |
| Target | `.claude/scripts/pre-commit-check.sh` |
| Executable | ✅ Yes (chmod +x) |
| Checks | 6 (JavaDoc, Error Codes, @since, Imports, Sensitive Data, Messages) |

---

## 🎉 Congratulations!

Từ giờ tất cả commits của bạn sẽ tự động được check skills compliance!

Không còn lo quên JavaDoc, hardcode error messages, hay commit passwords nữa! 🚀

---

**Installed:** 2026-01-28
**Script:** `.claude/scripts/pre-commit-check.sh`
**Related:** `skills-compliance-checklist.md`, `README.md`
