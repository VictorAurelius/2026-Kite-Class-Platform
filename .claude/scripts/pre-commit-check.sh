#!/bin/bash
# Pre-commit Skills Compliance Check
# Auto-checks code compliance with project skills before committing
#
# Usage: Run this script before `git commit`
#   ./pre-commit-check.sh
#
# Or install as git hook:
#   ln -s ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit
#
# Requirements:
#   - Java 21 (JAVA_HOME must be set): ~/.local/java/jdk-21.0.5+11
#   - Maven: ~/.m2/wrapper/dists/apache-maven-3.9.6/bin/mvn
#   - Node.js + pnpm: for frontend checks

# NOTE: Do NOT use `set -e` here - we want to collect all violations
# set -e

echo "🔍 Running Skills Compliance Check..."
echo ""

# Auto-configure JAVA_HOME if not set
if [ -z "$JAVA_HOME" ] && [ -d "$HOME/.local/java/jdk-21.0.5+11" ]; then
    export JAVA_HOME="$HOME/.local/java/jdk-21.0.5+11"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

MAVEN_CMD="$HOME/.m2/wrapper/dists/apache-maven-3.9.6/bin/mvn"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VIOLATIONS=0

# ==============================================================================
# 1. Check JavaDoc for Public Methods
# ==============================================================================
echo "📝 Checking JavaDoc compliance..."

# Check new production Java files (not test) for missing JavaDoc on public classes
NEW_PROD_JAVA=$(git diff --cached --name-only --diff-filter=A | grep '\.java$' | grep -v '/test/' || true)
MISSING_JAVADOC=0
if [ -n "$NEW_PROD_JAVA" ]; then
    for jfile in $NEW_PROD_JAVA; do
        if [ -f "$jfile" ] && ! grep -q "/\*\*" "$jfile"; then
            echo -e "${YELLOW}⚠️  No JavaDoc found in new file: $jfile${NC}"
            MISSING_JAVADOC=$((MISSING_JAVADOC + 1))
        fi
    done
fi

if [ "$MISSING_JAVADOC" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  $MISSING_JAVADOC new production file(s) missing JavaDoc${NC}"
    echo "   Review: code-style.md lines 110-158"
    # Warning only, not blocking violation
else
    echo -e "${GREEN}✅ JavaDoc compliance OK${NC}"
fi
echo ""

# ==============================================================================
# 2. Check Error Code Usage
# ==============================================================================
echo "🚨 Checking error code usage..."

# Check for hardcoded error messages in ServiceImpl
HARDCODED_ERRORS=$(git diff --cached | grep -E '(throw new .*(Exception|Error)\("(?!.*_.*)")' | wc -l)

if [ "$HARDCODED_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $HARDCODED_ERRORS potential hardcoded error messages${NC}"
    echo "   Expected: throw new Exception(\"ERROR_CODE\", args)"
    echo "   Review: error-logging.md lines 24-157"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Error code usage OK${NC}"
fi
echo ""

# ==============================================================================
# 3. Check @since Annotations
# ==============================================================================
echo "📅 Checking @since annotations..."

# Get current branch to determine PR version
BRANCH=$(git branch --show-current)

# Check if new Java files have @since annotation
NEW_JAVA_FILES=$(git diff --cached --name-only --diff-filter=A | grep '\.java$' || true)

if [ -n "$NEW_JAVA_FILES" ]; then
    MISSING_SINCE=0
    for file in $NEW_JAVA_FILES; do
        if ! grep -q "@since" "$file"; then
            echo -e "${RED}❌ Missing @since in $file${NC}"
            MISSING_SINCE=$((MISSING_SINCE + 1))
        fi
    done

    if [ "$MISSING_SINCE" -gt 0 ]; then
        echo "   Review: code-style.md lines 111-169"
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        echo -e "${GREEN}✅ @since annotations OK${NC}"
    fi
else
    echo -e "${GREEN}✅ No new Java files to check${NC}"
fi
echo ""

# ==============================================================================
# 4. Check Import Ordering
# ==============================================================================
echo "📦 Checking import ordering..."

# Check for wildcard imports
WILDCARD_IMPORTS=$(git diff --cached | grep -E '^\+import .*\.\*;' | wc -l)

if [ "$WILDCARD_IMPORTS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $WILDCARD_IMPORTS wildcard imports${NC}"
    echo "   Avoid: import java.util.*"
    echo "   Use: import java.util.List"
    echo "   Review: code-style.md lines 286-309"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Import ordering OK${NC}"
fi
echo ""

# ==============================================================================
# 5. Check Sensitive Data
# ==============================================================================
echo "🔐 Checking for sensitive data..."

# Check for common sensitive patterns
SENSITIVE_PATTERNS=(
    "password.*=.*['\"]"
    "api[_-]?key.*=.*['\"]"
    "secret.*=.*['\"]"
    "token.*=.*['\"]"
    "jdbc:.*//.*:.*@"
)

SENSITIVE_FOUND=0
for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    # Exclude documentation files (.md, .txt) from sensitive data check
    MATCHES=$(git diff --cached --diff-filter=ACM -- '*.java' '*.ts' '*.tsx' '*.js' '*.jsx' '*.properties' '*.yml' '*.yaml' | grep -iE "$pattern" | grep -v "^-" | grep -iv "getItem\|removeItem\|localStorage\|process\.env\|interface\|type " | wc -l)
    if [ "$MATCHES" -gt 0 ]; then
        echo -e "${RED}❌ Potential sensitive data found: $pattern${NC}"
        SENSITIVE_FOUND=$((SENSITIVE_FOUND + 1))
    fi
done

if [ "$SENSITIVE_FOUND" -gt 0 ]; then
    echo "   Review staged files and remove sensitive data"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ No sensitive data detected${NC}"
fi
echo ""

# ==============================================================================
# 6. Check Messages.properties Updates
# ==============================================================================
echo "🌐 Checking messages.properties..."

# If exception classes changed, check if messages.properties updated
EXCEPTION_CHANGES=$(git diff --cached --name-only | grep -E '(Exception|Error)\.java$' | wc -l)
MESSAGES_CHANGES=$(git diff --cached --name-only | grep 'messages.*\.properties' | wc -l)

if [ "$EXCEPTION_CHANGES" -gt 0 ] && [ "$MESSAGES_CHANGES" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Exception classes changed but messages.properties not updated${NC}"
    echo "   Did you add new error codes to messages.properties?"
    echo "   Review: error-logging.md lines 24-106"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Messages.properties check OK${NC}"
fi
echo ""

# ==============================================================================
# 7. Frontend Code Quality Checks
# ==============================================================================
echo "⚛️  Checking Frontend code quality..."

# Check if there are staged frontend files
STAGED_FRONTEND=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(tsx?|jsx?)$' || true)

if [ -n "$STAGED_FRONTEND" ]; then
    # Check for 'any' type
    ANY_USAGE=$(echo "$STAGED_FRONTEND" | xargs grep -n ":\s*any" 2>/dev/null || true)
    if [ -n "$ANY_USAGE" ]; then
        echo -e "${RED}❌ Found 'any' type usage (forbidden):${NC}"
        echo "$ANY_USAGE" | head -5
        echo "   Review: frontend-code-quality.md Part 1"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi

    # Check for console.log
    CONSOLE_LOG=$(echo "$STAGED_FRONTEND" | xargs grep -n "console\.log" 2>/dev/null || true)
    if [ -n "$CONSOLE_LOG" ]; then
        echo -e "${YELLOW}⚠️  Found console.log statements:${NC}"
        echo "$CONSOLE_LOG" | head -3
        echo "   Consider using console.warn/error instead"
    fi

    # Check for missing React displayName on memo components
    MISSING_DISPLAY_NAME=$(echo "$STAGED_FRONTEND" | xargs grep -l "React\.memo" | while read file; do
        if ! grep -q "displayName" "$file"; then
            echo "$file"
        fi
    done)
    if [ -n "$MISSING_DISPLAY_NAME" ]; then
        echo -e "${YELLOW}⚠️  React.memo components missing displayName:${NC}"
        echo "$MISSING_DISPLAY_NAME"
    fi

    echo -e "${GREEN}✅ Frontend code quality checks completed${NC}"
else
    echo -e "${GREEN}✅ No frontend files to check${NC}"
fi
echo ""

# ==============================================================================
# 8. Check Git Author Name
# ==============================================================================
echo "👤 Checking git author name..."

AUTHOR_NAME=$(git config user.name)
if echo "$AUTHOR_NAME" | grep -iq "claude"; then
    echo -e "${RED}❌ Git author name contains 'claude': $AUTHOR_NAME${NC}"
    echo "   Please set your real name:"
    echo "   git config user.name \"Your Name\""
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Git author name OK${NC}"
fi
echo ""

# ==============================================================================
# 9. Check pom.xml Spring Boot Version (maven-dependencies.md)
# ==============================================================================
echo "📦 Checking pom.xml Spring Boot version..."

APPROVED_SB_VERSION="3.5.13"
POM_FILES_CHANGED=$(git diff --cached --name-only | grep "pom\.xml" || true)

if [ -n "$POM_FILES_CHANGED" ]; then
    SB_VERSION_VIOLATIONS=0
    for pom in $POM_FILES_CHANGED; do
        if [ -f "$pom" ]; then
            # Get version from staged content
            STAGED_VERSION=$(git show ":$pom" 2>/dev/null | grep -A2 "spring-boot-starter-parent" | grep "<version>" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')
            if [ -n "$STAGED_VERSION" ] && [ "$STAGED_VERSION" != "$APPROVED_SB_VERSION" ]; then
                echo -e "${RED}❌ pom.xml Spring Boot version mismatch: found $STAGED_VERSION, expected $APPROVED_SB_VERSION${NC}"
                echo "   File: $pom"
                echo "   Update to: <version>$APPROVED_SB_VERSION</version>"
                echo "   Skill: maven-dependencies.md"
                SB_VERSION_VIOLATIONS=$((SB_VERSION_VIOLATIONS + 1))
                VIOLATIONS=$((VIOLATIONS + 1))
            fi
        fi
    done
    if [ "$SB_VERSION_VIOLATIONS" -eq 0 ]; then
        echo -e "${GREEN}✅ Spring Boot version OK ($APPROVED_SB_VERSION)${NC}"
    fi
else
    echo -e "${GREEN}✅ No pom.xml changes to check${NC}"
fi
echo ""

# ==============================================================================
# 10. Java Compile + Checkstyle (IDE Problems Check)
# ==============================================================================
echo "☕ Checking Java compile + Checkstyle (IDE Problems)..."

JAVA_FILES_CHANGED=$(git diff --cached --name-only | grep "\.java$" || true)
STAGED_POM_CHANGED=$(git diff --cached --name-only | grep "pom\.xml" || true)

if [ -n "$JAVA_FILES_CHANGED" ] || [ -n "$STAGED_POM_CHANGED" ]; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ] && [ -f "$MAVEN_CMD" ]; then
        # Determine which service was modified
        CORE_CHANGED=$(echo "$JAVA_FILES_CHANGED $STAGED_POM_CHANGED" | tr ' ' '\n' | grep "kiteclass-core" | head -1 || true)

        _compile_service() {
            local SERVICE_NAME="$1"
            local POM_PATH="$2"
            echo "   Compiling $SERVICE_NAME..."
            local MVN_OUT
            MVN_OUT=$(mktemp)
            # Run without -q so showWarnings/showDeprecation in pom.xml takes effect
            if JAVA_HOME="$JAVA_HOME" bash "$MAVEN_CMD" \
                -f "$POM_PATH" \
                compile 2>&1 | tee "$MVN_OUT" > /dev/null; then
                # Check for deprecation/unchecked warnings in output
                local DEPR_WARN
                DEPR_WARN=$(grep -E "uses.*(deprecated|unchecked)|deprecated API" "$MVN_OUT" | grep -v "^Note:" || true)
                if [ -n "$DEPR_WARN" ]; then
                    echo -e "${RED}   ❌ $SERVICE_NAME: deprecated/unchecked API usage:${NC}"
                    echo "$DEPR_WARN" | head -10
                    VIOLATIONS=$((VIOLATIONS + 1))
                else
                    echo -e "${GREEN}   ✅ $SERVICE_NAME: 0 violations, 0 deprecation warnings${NC}"
                fi
            else
                echo -e "${RED}   ❌ $SERVICE_NAME compile/checkstyle FAILED:${NC}"
                grep "\[ERROR\].*\.java" "$MVN_OUT" | head -10
                VIOLATIONS=$((VIOLATIONS + 1))
            fi
            rm -f "$MVN_OUT"
        }

        if [ -n "$CORE_CHANGED" ]; then
            _compile_service "kiteclass-core" \
                "/mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-core/pom.xml"
        fi
    else
        echo -e "${YELLOW}⚠️  Java/Maven not configured — skipping compile check${NC}"
        echo "   Set JAVA_HOME to: ~/.local/java/jdk-21.0.5+11"
        echo "   See: .claude/skills/ide-problem-check.md"
    fi
else
    echo -e "${GREEN}✅ No Java files changed — skipping compile check${NC}"
fi
echo ""

# ==============================================================================
# 11. Check Commit Message Length (note: full check in commit-msg hook)
# ==============================================================================
echo "📏 Checking commit message length..."
echo -e "${GREEN}✅ Commit message will be validated in commit-msg hook${NC}"
echo ""

# ==============================================================================
# 12. TDD Timestamp Check (Superpowers-inspired, WARNING mode Week 1-4)
# ==============================================================================
echo "🧪 Checking TDD compliance (test-first development)..."

# Get modified Java files (excluding tests)
MODIFIED_JAVA=$(git diff --cached --name-only --diff-filter=ACM | grep "src/main/.*\.java$" || true)

if [ -n "$MODIFIED_JAVA" ]; then
    TDD_WARNINGS=0

    for java_file in $MODIFIED_JAVA; do
        # Find corresponding test file
        test_file=$(echo "$java_file" | sed 's/src\/main/src\/test/' | sed 's/\.java$/Test.java/')

        # Check if test file exists and was also modified
        if [ -f "$test_file" ]; then
            # Check if test file is staged
            if git diff --cached --name-only | grep -q "^$test_file$"; then
                # Both files modified - check which was modified first (timestamp in commit)
                java_time=$(git log -1 --format=%ct -- "$java_file" 2>/dev/null || echo 0)
                test_time=$(git log -1 --format=%ct -- "$test_file" 2>/dev/null || echo 0)

                # If code was modified after test (not TDD)
                if [ "$java_time" -gt "$test_time" ] && [ "$test_time" -ne 0 ]; then
                    echo -e "${YELLOW}⚠️  TDD Warning: Code modified after test (not RED-GREEN-REFACTOR)${NC}"
                    echo "   Code: $java_file"
                    echo "   Test: $test_file"
                    echo "   💡 TDD best practice: Write test FIRST (RED) → Implement (GREEN) → Refactor"
                    echo "   📖 See: .claude/skills/tdd-enforcement.md"
                    TDD_WARNINGS=$((TDD_WARNINGS + 1))
                fi
            else
                echo -e "${YELLOW}⚠️  TDD Warning: Code modified but test not staged${NC}"
                echo "   Code: $java_file"
                echo "   Test: $test_file (exists but not staged)"
                echo "   💡 Consider: Did you update the test for this code change?"
                TDD_WARNINGS=$((TDD_WARNINGS + 1))
            fi
        else
            echo -e "${YELLOW}⚠️  Missing test file: $test_file${NC}"
            echo "   Code: $java_file"
            echo "   💡 Consider: Add test for new code"
            TDD_WARNINGS=$((TDD_WARNINGS + 1))
        fi
    done

    if [ "$TDD_WARNINGS" -eq 0 ]; then
        echo -e "${GREEN}✅ TDD compliance looks good${NC}"
    else
        echo ""
        echo -e "${YELLOW}ℹ️  Found $TDD_WARNINGS TDD advisory notice(s)${NC}"
        echo "   NOTE: This is WARNING mode (Week 1-4) - commit NOT blocked"
        echo "   Week 5+ will switch to BLOCKING mode"
    fi
else
    echo -e "${GREEN}✅ No Java code changes — skipping TDD check${NC}"
fi
echo ""

# ==============================================================================
# 13. Two-Stage Review Reminder (Superpowers-inspired)
# ==============================================================================
echo "🔍 Checking for code review readiness indicators..."

# Check if commit message or branch name suggests PR is ready for review
COMMIT_MSG_FILE=".git/COMMIT_EDITMSG"
BRANCH_NAME=$(git symbolic-ref --short HEAD 2>/dev/null || echo "")

REVIEW_KEYWORDS="ready for review|please review|review this|pr review|code review"

# Check branch name or recent commit messages
NEEDS_REVIEW=false
if echo "$BRANCH_NAME" | grep -iE "(review|ready)" > /dev/null 2>&1; then
    NEEDS_REVIEW=true
elif git log -1 --pretty=%B 2>/dev/null | grep -iE "$REVIEW_KEYWORDS" > /dev/null 2>&1; then
    NEEDS_REVIEW=true
fi

if [ "$NEEDS_REVIEW" = true ]; then
    echo -e "${YELLOW}📋 Code Review Reminder: Use Two-Stage Review Process${NC}"
    echo ""
    echo "   Stage 1: Specification Compliance (15-20 min) 🔴 BLOCKING"
    echo "   ✓ Requirements match PR description"
    echo "   ✓ All acceptance criteria met"
    echo "   ✓ Edge cases covered"
    echo "   ✓ Tests prove requirements"
    echo ""
    echo "   Stage 2: Code Quality (20-30 min) 🟠🟡 GRADED"
    echo "   🔴 Critical: Security, data loss, breaking changes"
    echo "   🟠 Major: Performance, test coverage, error handling"
    echo "   🟡 Minor: Naming, duplication, docs"
    echo ""
    echo "   📖 See: .claude/skills/two-stage-code-review.md"
else
    echo -e "${GREEN}✅ No review indicators detected${NC}"
fi
echo ""

# ==============================================================================
# 14. Systematic Debugging Reminder (Superpowers-inspired)
# ==============================================================================
echo "🐛 Checking for bug fix commits..."

# Check if commit message suggests bug fix
COMMIT_KEYWORDS="fix|bug|debug|issue|error|crash|fail"

if git log -1 --pretty=%B 2>/dev/null | grep -iE "$COMMIT_KEYWORDS" > /dev/null 2>&1; then
    echo -e "${YELLOW}🔬 Debugging Reminder: Use Systematic 4-Phase Process${NC}"
    echo ""
    echo "   Phase 1: Reproduce (15-30 min)"
    echo "   ✓ Create failing test case"
    echo "   ✓ Document exact steps"
    echo "   ✓ Verify consistency (not flaky)"
    echo ""
    echo "   Phase 2: Trace (30-60 min)"
    echo "   ✓ Use debugger or logging"
    echo "   ✓ Identify divergence point"
    echo ""
    echo "   Phase 3: Root Cause (30-45 min)"
    echo "   ✓ Apply 5 Whys technique"
    echo "   ✓ Distinguish symptom from cause"
    echo ""
    echo "   Phase 4: Defensive Fix (1-2 hours)"
    echo "   ✓ Add regression test"
    echo "   ✓ Fix related scenarios"
    echo "   ✓ Update troubleshooting.md"
    echo ""
    echo "   📖 See: .claude/skills/systematic-debugging.md"
else
    echo -e "${GREEN}✅ No bug fix detected${NC}"
fi
echo ""

# ==============================================================================
# 14b. Check: Business docs accompany business logic changes
# ==============================================================================
echo "📄 Checking business doc accompanies business logic changes..."

KITEHUB_BIZ_CHANGED=$(git diff --cached --name-only | grep -E "^kitehub/kitehub-(subscription|branding|email|admin|gateway)/src/main/java/" | head -1 || true)
KITECLASS_BIZ_CHANGED=$(git diff --cached --name-only | grep -E "^kiteclass/kiteclass-core/src/main/java/" | head -1 || true)
BIZ_DOC_STAGED=$(git diff --cached --name-only | grep "documents/01-business/" | head -1 || true)

if [ -n "$KITEHUB_BIZ_CHANGED" ] || [ -n "$KITECLASS_BIZ_CHANGED" ]; then
    if [ -z "$BIZ_DOC_STAGED" ]; then
        echo -e "${YELLOW}⚠️  Business logic changed but no business doc updated${NC}"
        echo "   Changed: $KITEHUB_BIZ_CHANGED $KITECLASS_BIZ_CHANGED"
        echo "   Expected: update documents/01-business/ in same commit"
        echo "   Rule: Doc va code PHAI cung PR (CLAUDE.md)"
        # WARNING mode, not blocking
    else
        echo -e "${GREEN}✅ Business doc update detected${NC}"
    fi
else
    echo -e "${GREEN}✅ No business logic changes to check${NC}"
fi
echo ""

# ==============================================================================
# 15. Check New Folder Structure Compliance
# ==============================================================================
echo "📂 Checking folder structure compliance..."

# Allowed top-level directories (anything else = warning)
ALLOWED_ROOT_DIRS="\.claude|\.github|\.vscode|documents|infrastructure|kiteclass|kitehub|scripts|node_modules"

# Detect new directories being added at project root
NEW_ROOT_DIRS=$(git diff --cached --name-only --diff-filter=A | sed 's|/.*||' | sort -u | grep -v '^\.' | grep -v -E "^($ALLOWED_ROOT_DIRS)$" || true)
# Also check for dotfiles that create new root dirs (exclude known ones)
NEW_ROOT_DOTDIRS=$(git diff --cached --name-only --diff-filter=A | grep '^\..*/' | sed 's|/.*||' | sort -u | grep -v -E "^\.(claude|github|vscode|gitignore|gitattributes|log|docker-build-logs)$" || true)

FOLDER_ISSUES=0

if [ -n "$NEW_ROOT_DIRS" ]; then
    for dir in $NEW_ROOT_DIRS; do
        # Skip files (only flag directories)
        if echo "$dir" | grep -q '\.'; then
            continue
        fi
        echo -e "${YELLOW}⚠️  New top-level directory: $dir/${NC}"
        echo "   Allowed root dirs: documents/, infrastructure/, kiteclass/, kitehub/, scripts/"
        echo "   Infrastructure files → infrastructure/"
        echo "   Documentation → documents/"
        FOLDER_ISSUES=$((FOLDER_ISSUES + 1))
    done
fi

# Check for infra files placed outside infrastructure/
MISPLACED_INFRA=$(git diff --cached --name-only --diff-filter=A | grep -E "^(helm|k8s|terraform|terraform-)" || true)
if [ -n "$MISPLACED_INFRA" ]; then
    for f in $MISPLACED_INFRA; do
        echo -e "${RED}❌ Infrastructure file outside infrastructure/: $f${NC}"
        echo "   Move to: infrastructure/$f"
        FOLDER_ISSUES=$((FOLDER_ISSUES + 1))
        VIOLATIONS=$((VIOLATIONS + 1))
    done
fi

# Check for stale patterns: session files, action-*.md at wrong locations
STALE_PATTERNS=$(git diff --cached --name-only --diff-filter=A | grep -iE "(SESSION-STATUS|CURRENT-WORK|\.log/)" || true)
if [ -n "$STALE_PATTERNS" ]; then
    for f in $STALE_PATTERNS; do
        echo -e "${YELLOW}⚠️  Possible stale/misplaced file: $f${NC}"
        echo "   Session files should not be committed"
        echo "   Logs should go to infrastructure/logs/"
        FOLDER_ISSUES=$((FOLDER_ISSUES + 1))
    done
fi

# Check for docker-compose at project root (should be in kitehub/ or kiteclass/)
ROOT_COMPOSE=$(git diff --cached --name-only --diff-filter=A | grep -E "^docker-compose" || true)
if [ -n "$ROOT_COMPOSE" ]; then
    for f in $ROOT_COMPOSE; do
        echo -e "${YELLOW}⚠️  Docker Compose at project root: $f${NC}"
        echo "   Canonical compose files live in kitehub/ or kiteclass/"
        FOLDER_ISSUES=$((FOLDER_ISSUES + 1))
    done
fi

if [ "$FOLDER_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ Folder structure OK${NC}"
fi
echo ""

# ==============================================================================
# 16. Check Business Docs 3-Layer Structure
# ==============================================================================
echo "📋 Checking business docs 3-layer structure..."

BIZ_LAYER_ISSUES=0

# Check new domain folders have all 3 required files
NEW_BIZ_DIRS=$(git diff --cached --name-only --diff-filter=A | grep "^documents/01-business/" | sed 's|documents/01-business/[^/]*/\([^/]*\)/.*|\1|' | sort -u || true)

for domain in $NEW_BIZ_DIRS; do
    # Skip if it's a file not a folder (e.g., README.md)
    if echo "$domain" | grep -q '\.'; then continue; fi

    # Check each required layer
    for layer in rules.md use-cases.md api-contract.md; do
        STAGED=$(git diff --cached --name-only | grep "documents/01-business/.*/$domain/$layer" || true)
        if [ -z "$STAGED" ]; then
            # Check if file already exists on disk
            EXISTING=$(find documents/01-business -path "*/$domain/$layer" 2>/dev/null | head -1)
            if [ -z "$EXISTING" ]; then
                echo -e "${YELLOW}⚠️  Business domain '$domain' missing: $layer${NC}"
                echo "   3-layer required: rules.md, use-cases.md, api-contract.md"
                echo "   See: .claude/skills/reference/business-docs-3-layer.md"
                BIZ_LAYER_ISSUES=$((BIZ_LAYER_ISSUES + 1))
            fi
        fi
    done
done

if [ "$BIZ_LAYER_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ Business docs 3-layer OK${NC}"
fi
echo ""

# ==============================================================================
# Summary
# ==============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Safe to commit.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Stage your changes: git add -A"
    echo "2. Commit with proper format: git commit -m \"type(scope): description\""
    echo "3. Include Co-Authored-By line"
    exit 0
else
    echo -e "${RED}❌ Found $VIOLATIONS compliance issue(s)${NC}"
    echo ""
    echo "Please fix the issues above before committing."
    echo ""
    echo "Resources:"
    echo "- Full checklist: .claude/skills/skills-compliance-checklist.md"
    echo "- Backend: code-style.md, error-logging.md"
    echo "- Frontend: frontend-code-quality.md"
    echo ""
    echo "To skip this check (not recommended):"
    echo "  git commit --no-verify"
    exit 1
fi
