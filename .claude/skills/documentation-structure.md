# Documentation & Reports Structure

## Overview

Quy tắc tổ chức files documentation, reports, scripts để tránh làm loãng folder gốc của project.

## Trigger Phrases

- "tạo report"
- "tạo documentation"
- "tạo md file"
- "tạo script"
- "documentation structure"
- "folder organization"

---

## 📁 Standard Folder Structure

### For Spring Boot Projects (Gateway, Core Service)

```
kiteclass-gateway/
├── docs/                           # All documentation files
│   ├── pr-summaries/               # Pull Request summaries
│   │   ├── PR-1.1-SUMMARY.md
│   │   ├── PR-1.2-SUMMARY.md
│   │   ├── PR-1.3-SUMMARY.md
│   │   └── PR-1.4-SUMMARY.md
│   │
│   ├── test-reports/               # Test execution reports
│   │   ├── TEST-RESULTS-FINAL.md
│   │   ├── TESTING-COMPLETE.md
│   │   └── coverage-report-{date}.md
│   │
│   ├── implementation/             # Implementation reports
│   │   ├── IMPLEMENTATION-COMPLETE-PR-1.4.md
│   │   └── COMMIT-HISTORY-PR-1.4.md
│   │
│   ├── guides/                     # User/Developer guides
│   │   ├── TESTING.md
│   │   ├── SETUP.md
│   │   └── API-DOCUMENTATION.md
│   │
│   └── architecture/               # Architecture diagrams/docs
│       ├── auth-flow.md
│       └── security-architecture.md
│
├── scripts/                        # Utility scripts
│   ├── test/                       # Test scripts
│   │   ├── test-auth-flow.sh
│   │   ├── test-user-api.sh
│   │   └── integration-test.sh
│   │
│   ├── setup/                      # Setup scripts
│   │   ├── setup-java.sh
│   │   └── setup-database.sh
│   │
│   └── utils/                      # Utility scripts
│       └── cleanup-logs.sh
│
├── src/                            # Source code (NO docs here)
├── pom.xml
├── README.md                       # Project overview (keep in root)
└── .gitignore
```

### For Frontend Projects (Next.js)

```
kiteclass-frontend/
├── docs/
│   ├── features/                   # Feature documentation
│   ├── components/                 # Component documentation
│   ├── api/                        # API integration docs
│   └── pr-summaries/
│
├── scripts/
│   ├── test/
│   ├── build/
│   └── deploy/
│
├── src/
├── package.json
└── README.md
```

---

## 📝 Naming Conventions

### PR Summaries

**Format:** `PR-{number}-SUMMARY.md`

**Example:**
- `PR-1.1-SUMMARY.md` - Project Setup
- `PR-1.4-SUMMARY.md` - Auth Module

**Location:** `docs/pr-summaries/`

### Test Reports

**Format:** `TEST-{type}-{date?}.md` or `TESTING-{context}.md`

**Examples:**
- `TEST-RESULTS-FINAL.md` - Final test results
- `TESTING-COMPLETE.md` - Testing completion summary
- `test-coverage-2026-01-26.md` - Coverage report with date

**Location:** `docs/test-reports/`

### Implementation Reports

**Format:** `IMPLEMENTATION-{context}-{PR?}.md` or `COMMIT-HISTORY-{PR}.md`

**Examples:**
- `IMPLEMENTATION-COMPLETE-PR-1.4.md`
- `COMMIT-HISTORY-PR-1.4.md`
- `REFACTORING-REPORT-2026-01.md`

**Location:** `docs/implementation/`

### Guides

**Format:** `{TOPIC}.md` (uppercase for important guides)

**Examples:**
- `TESTING.md` - How to run tests
- `SETUP.md` - Project setup guide
- `DEPLOYMENT.md` - Deployment guide
- `API-DOCUMENTATION.md` - API reference

**Location:** `docs/guides/`

### Scripts

**Format:** `{action}-{target}.sh` (lowercase with hyphens)

**Examples:**
- `test-auth-flow.sh`
- `setup-java.sh`
- `run-tests.sh`
- `deploy-production.sh`

**Location:** `scripts/{category}/`

---

## 🚫 Anti-Patterns (DON'T DO THIS)

### ❌ BAD: Files in Root

```
kiteclass-gateway/
├── PR-1.3-SUMMARY.md              # ❌ Bad
├── PR-1.4-SUMMARY.md              # ❌ Bad
├── TEST-RESULTS.md                 # ❌ Bad
├── TESTING.md                      # ❌ Bad
├── test-auth-flow.sh              # ❌ Bad
├── IMPLEMENTATION-COMPLETE.md      # ❌ Bad
├── src/
└── pom.xml
```

**Problems:**
- Root folder cluttered
- Hard to find specific docs
- No categorization
- Confusing for new developers

### ✅ GOOD: Organized Structure

```
kiteclass-gateway/
├── docs/
│   ├── pr-summaries/
│   │   ├── PR-1.3-SUMMARY.md      # ✅ Good
│   │   └── PR-1.4-SUMMARY.md      # ✅ Good
│   ├── test-reports/
│   │   └── TEST-RESULTS-FINAL.md  # ✅ Good
│   └── guides/
│       └── TESTING.md              # ✅ Good
├── scripts/
│   └── test/
│       └── test-auth-flow.sh      # ✅ Good
└── README.md                       # ✅ Keep in root
```

**Benefits:**
- Clean root folder
- Easy to navigate
- Clear categorization
- Professional structure

---

## 📋 Migration Process

When reorganizing existing files:

### Step 1: Create Folder Structure

```bash
mkdir -p docs/{pr-summaries,test-reports,implementation,guides,architecture}
mkdir -p scripts/{test,setup,utils}
```

### Step 2: Move Files

```bash
# Move PR summaries
git mv PR-*.md docs/pr-summaries/

# Move test reports
git mv TEST-*.md TESTING-*.md docs/test-reports/

# Move implementation reports
git mv IMPLEMENTATION-*.md COMMIT-HISTORY-*.md docs/implementation/

# Move guides
git mv TESTING.md docs/guides/

# Move scripts
git mv test-*.sh scripts/test/
git mv setup-*.sh scripts/setup/
```

### Step 3: Update References

Update all internal links in moved files:
- Relative paths to source code
- Links to other documentation
- README.md links

### Step 4: Commit

```bash
git add -A
git commit -m "docs: reorganize documentation and scripts into structured folders

- Move PR summaries to docs/pr-summaries/
- Move test reports to docs/test-reports/
- Move implementation reports to docs/implementation/
- Move guides to docs/guides/
- Move scripts to scripts/{category}/

This improves project organization and makes documentation easier to find.
"
```

---

## 🎯 When to Create Documentation

### Always Create in Correct Folder

**PR Summary:**
```bash
# ❌ Bad
touch PR-1.5-SUMMARY.md

# ✅ Good
touch docs/pr-summaries/PR-1.5-SUMMARY.md
```

**Test Report:**
```bash
# ❌ Bad
touch TEST-RESULTS.md

# ✅ Good
touch docs/test-reports/TEST-RESULTS-$(date +%Y-%m-%d).md
```

**Script:**
```bash
# ❌ Bad
touch deploy.sh

# ✅ Good
touch scripts/deploy/deploy-production.sh
chmod +x scripts/deploy/deploy-production.sh
```

---

## 📚 README.md Updates

After reorganization, update README.md with documentation links:

```markdown
# KiteClass Gateway

## Documentation

- [Testing Guide](docs/guides/TESTING.md)
- [Setup Guide](docs/guides/SETUP.md)
- [PR Summaries](docs/pr-summaries/)
- [Test Reports](docs/test-reports/)

## Scripts

- [Run Tests](scripts/test/run-tests.sh)
- [Setup Java](scripts/setup/setup-java.sh)
- [Test Auth Flow](scripts/test/test-auth-flow.sh)

## Pull Requests

- [PR 1.1: Project Setup](docs/pr-summaries/PR-1.1-SUMMARY.md)
- [PR 1.2: Common Components](docs/pr-summaries/PR-1.2-SUMMARY.md)
- [PR 1.3: User Module](docs/pr-summaries/PR-1.3-SUMMARY.md)
- [PR 1.4: Auth Module](docs/pr-summaries/PR-1.4-SUMMARY.md)
```

---

## 🔍 Quick Reference

| Type | Location | Format | Example |
|------|----------|--------|---------|
| PR Summary | `docs/pr-summaries/` | `PR-{n}-SUMMARY.md` | `PR-1.4-SUMMARY.md` |
| Test Report | `docs/test-reports/` | `TEST-{type}.md` | `TEST-RESULTS-FINAL.md` |
| Implementation | `docs/implementation/` | `IMPLEMENTATION-*.md` | `IMPLEMENTATION-COMPLETE-PR-1.4.md` |
| Guide | `docs/guides/` | `{TOPIC}.md` | `TESTING.md` |
| Architecture | `docs/architecture/` | `{topic}.md` | `auth-flow.md` |
| Test Script | `scripts/test/` | `test-{target}.sh` | `test-auth-flow.sh` |
| Setup Script | `scripts/setup/` | `setup-{target}.sh` | `setup-java.sh` |
| Utility Script | `scripts/utils/` | `{action}-{target}.sh` | `cleanup-logs.sh` |

---

## ✅ Checklist for New Documentation

Before creating any documentation file, ask:

- [ ] Is this a PR summary? → `docs/pr-summaries/`
- [ ] Is this a test report? → `docs/test-reports/`
- [ ] Is this implementation documentation? → `docs/implementation/`
- [ ] Is this a guide? → `docs/guides/`
- [ ] Is this a script? → `scripts/{category}/`
- [ ] Does the filename follow naming convention?
- [ ] Are all internal links relative and correct?
- [ ] Is README.md updated with link (if important)?

---

## 🚀 Benefits of This Structure

1. **Clean Root Folder**
   - Only essential files (README, pom.xml, package.json)
   - Professional appearance
   - Easy to navigate

2. **Easy to Find**
   - Categorized by type
   - Predictable locations
   - Searchable structure

3. **Scalable**
   - Add new categories easily
   - Supports growth
   - No folder bloat

4. **Professional**
   - Industry standard
   - Easy for new developers
   - Clear organization

5. **Git-Friendly**
   - Easy to track changes
   - Logical grouping
   - Clean diffs

---

## 📖 Examples from KiteClass

### Current (After Reorganization)

```
kiteclass-gateway/
├── docs/
│   ├── pr-summaries/
│   │   ├── PR-1.3-SUMMARY.md       # User Module summary
│   │   └── PR-1.4-SUMMARY.md       # Auth Module summary
│   ├── test-reports/
│   │   ├── TEST-RESULTS-FINAL.md   # Final test results
│   │   └── TESTING-COMPLETE.md     # Testing summary
│   ├── implementation/
│   │   ├── IMPLEMENTATION-COMPLETE-PR-1.4.md
│   │   └── COMMIT-HISTORY-PR-1.4.md
│   └── guides/
│       └── TESTING.md              # How to run tests
├── scripts/
│   ├── test/
│   │   ├── test-auth-flow.sh      # Auth flow testing
│   │   └── run-tests.sh           # Run all tests
│   └── setup/
│       └── setup-java.sh          # Java setup
├── src/
├── pom.xml
└── README.md
```

---

**Last Updated:** 2026-01-26
**Version:** 1.0.0
**Author:** VictorAurelius + Claude Sonnet 4.5
