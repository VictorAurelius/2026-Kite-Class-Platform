# Skill: Log Management

Guidelines for managing CI logs, application logs, and temporary files in the project.

## Directory Structure

```
.log/
├── check-ci.sh         # ✅ COMMIT - Reusable CI status checker script
├── *.txt               # ❌ NO COMMIT - Downloaded CI logs (gitignored)
├── *.log               # ❌ NO COMMIT - Application logs (gitignored)
└── */                  # ❌ NO COMMIT - Temporary directories (gitignored)
```

## What to Commit

### ✅ DO Commit

1. **Scripts and Tools**
   - `.log/check-ci.sh` - CI status checker
   - Any reusable automation scripts in `.log/`

2. **Documentation**
   - `README.md` in `.log/` (if exists)
   - Scripts with usage instructions

### ❌ DO NOT Commit

1. **Downloaded Logs**
   - CI workflow logs (`*.txt`)
   - Test execution logs (`*.log`)
   - Error dumps (`*.dump`, `*.dumpstream`)

2. **Temporary Files**
   - Downloaded artifacts
   - Extracted files
   - Cache directories

3. **Large Files**
   - Any file > 1MB
   - Binary logs

## Log Retention Policy

### Local Development

**Keep for Reference:**
- Last 2 CI runs (for comparison)
- Current debug session logs

**Delete Regularly:**
- Logs older than 7 days
- Logs from fixed issues
- Temporary debug logs

### CI/CD

**GitHub Actions:**
- Logs automatically deleted after 90 days
- Download important logs before expiration
- Use `gh run download` for artifacts

**Commands:**
```bash
# Clean old logs (keep check-ci.sh)
rm -f .log/*.txt .log/*.log
rm -rf .log/auto .log/temp

# Or use find for > 7 days
find .log -type f \( -name "*.txt" -o -name "*.log" \) -mtime +7 -delete
```

## CI Log Access

### Using check-ci.sh

```bash
# Check latest CI run
./.log/check-ci.sh

# Check specific run
./.log/check-ci.sh 21810813911
```

### Using GitHub CLI

```bash
# List recent runs
gh run list --limit 10

# View run details
gh run view <run_id>

# View failed logs only
gh run view <run_id> --log-failed

# Download full logs
gh run download <run_id>
```

### Using GitHub Web UI

```
https://github.com/{owner}/{repo}/actions/runs/{run_id}
```

## Log Analysis Workflow

### 1. Initial Check

```bash
# Quick status
./.log/check-ci.sh

# Identify failed jobs
gh run view <run_id>
```

### 2. Download Failed Logs

```bash
# Option A: Via gh CLI
gh run view <run_id> --log-failed > .log/failed-$(date +%Y%m%d).log

# Option B: Full download
gh run download <run_id> --dir .log/run-$(date +%Y%m%d)
```

### 3. Analyze

```bash
# Search for errors
grep -i "error\|failure\|exception" .log/failed-*.log

# Extract test summary
grep "Tests run:" .log/failed-*.log

# Find specific test failures
grep -B5 -A10 "TestName" .log/failed-*.log
```

### 4. Cleanup After Fix

```bash
# Remove analyzed logs
rm .log/failed-*.log

# Keep check-ci.sh
ls -la .log/  # Should only show check-ci.sh
```

## .gitignore Rules

Ensure `.gitignore` contains:

```gitignore
# Logs
*.log
*.txt
*.dump
*.dumpstream

# But commit scripts
!.log/check-ci.sh
!.log/README.md

# Temporary directories in .log
.log/auto/
.log/temp/
.log/run-*/
```

## Common Patterns

### Pattern 1: Quick CI Check

```bash
# After push
git push origin main
sleep 30
./.log/check-ci.sh
```

### Pattern 2: Detailed Failure Analysis

```bash
# Get run ID from check-ci.sh output
RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')

# Download and analyze
gh run view $RUN_ID --log-failed > .log/analysis.log
grep -E "ERROR|FAILURE" .log/analysis.log

# After fixing
rm .log/analysis.log
```

### Pattern 3: Compare Two Runs

```bash
# Download two runs
gh run view <old_run> --log-failed > .log/run-old.log
gh run view <new_run> --log-failed > .log/run-new.log

# Compare failures
diff <(grep "ERROR" .log/run-old.log) <(grep "ERROR" .log/run-new.log)

# Cleanup
rm .log/run-*.log
```

## Best Practices

1. **Always use check-ci.sh first** - Fastest way to check status
2. **Download logs only when needed** - Avoid clutter
3. **Delete logs after analysis** - Keep .log/ clean
4. **Name logs with dates** - Easy to identify old files
5. **Use subdirectories for runs** - Organize large downloads
6. **Never commit log contents** - Only commit tools

## Troubleshooting

### Issue: .log directory too large

```bash
# Check size
du -sh .log

# Clean up (keep scripts)
find .log -type f \( -name "*.txt" -o -name "*.log" \) -delete
```

### Issue: Can't find recent CI logs

```bash
# Check if CI ran
gh run list --limit 5

# Check workflow status
gh workflow list
gh workflow view <workflow_name>
```

### Issue: Downloaded logs are empty

```bash
# Check run status
gh run view <run_id>

# Wait for completion
gh run watch <run_id>

# Then download
gh run download <run_id>
```

## Integration with MEMORY.md

Reference MEMORY.md for:
- CI/CD troubleshooting patterns
- Test failure patterns
- Known issues and fixes

**Example:**
```bash
# After analyzing logs, update MEMORY.md
echo "### New Issue Found" >> memory/MEMORY.md
echo "- Problem: [description]" >> memory/MEMORY.md
echo "- Solution: [fix]" >> memory/MEMORY.md
```

---

**Created**: 2026-02-09
**Last Updated**: 2026-02-09
**Author**: KiteClass Team
