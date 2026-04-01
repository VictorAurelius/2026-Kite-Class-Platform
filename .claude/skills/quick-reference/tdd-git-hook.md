# TDD Git Hook — Implementation & Limitations

## Pre-commit Hook (`.claude/scripts/pre-commit-check.sh`)

### Current Mode: WARNING (Checks 1-4, Week 1-4)

```bash
# Section 12 in pre-commit-check.sh
MODIFIED_JAVA=$(git diff --cached --name-only --diff-filter=ACM | grep "src/main/.*\.java$")

for java_file in $MODIFIED_JAVA; do
    test_file=$(echo "$java_file" | sed 's/src\/main/src\/test/' | sed 's/\.java$/Test.java/')

    if [ -f "$test_file" ]; then
        java_time=$(git log -1 --format=%ct -- "$java_file" 2>/dev/null || echo 0)
        test_time=$(git log -1 --format=%ct -- "$test_file" 2>/dev/null || echo 0)

        if [ "$java_time" -gt "$test_time" ] && [ "$test_time" -ne 0 ]; then
            echo "⚠️ TDD Warning: Code modified after test"
        fi
    else
        echo "⚠️ Missing test file: $test_file"
    fi
done
# WARNING mode: commit is NOT blocked
```

### Blocking Mode (Week 5+)

Same detection logic but exits with code 1 if violations found:
```bash
if [ "$TDD_VIOLATIONS" -gt 0 ]; then
    echo "❌ $TDD_VIOLATIONS TDD violation(s) found — commit BLOCKED"
    exit 1
fi
```

---

## Known Limitations

### 1. Cannot Access Current Commit Message
Pre-commit runs BEFORE message is written. Review/debug reminders only check branch name or PREVIOUS commit message.

**Workaround:** Use descriptive branch names (`fix/bug-123`), or move keyword checks to `commit-msg` hook.

### 2. Timestamp Uses Git History (Not Filesystem)
New files (never committed) have timestamp=0 — may miss violation when test + code committed together.

**Workaround:** Commit test file first, then code file in separate commits.

### 3. Only Checks Java Files (Backend)
Current hook pattern: `src/main/.*\.java$`. Frontend TypeScript not checked.

**Future:** Extend to `.tsx` pattern: `src/components/*.tsx` → `src/__tests__/*.test.tsx`

### 4. Warning Mode Not Blocking (Week 1-4)
Warnings are advisory only — commit still succeeds.

**Mitigation:** Switch to BLOCKING mode Week 5+. Track compliance during warning period.

---

## Acceptable Design Decisions

| Limitation | Decision |
|-----------|----------|
| Pre-commit vs Commit-msg | Keep code checks in pre-commit; accept message limitation |
| Git history dependency | Acceptable — encourages incremental commits (test → code) |
| Backend-only initially | Start with Java, extend to frontend Week 3-4 |
