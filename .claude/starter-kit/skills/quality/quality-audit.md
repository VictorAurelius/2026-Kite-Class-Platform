# Portable Skill — adapt {project} placeholders

# /quality-audit — Danh gia chat luong toan dien

**Usage:** `/quality-audit [{project}|all]`

---

## Instructions

### Buoc 1: Thu thap du lieu

Chay song song de thu thap metrics:

```bash
# Git & PR stats
git log --oneline --since="30 days ago" | wc -l
gh pr list --state merged --limit 200 --json number --jq 'length'
gh pr list --state open --json number --jq 'length'

# CI status — PHAI dung script
scripts/check-ci.sh --status

# Tests — PHAI dung script
scripts/test-local.sh {project} all

# Docker status
scripts/status.sh

# Code stats (grep/find OK — chi dem, khong execute)
# Adapt paths to your project structure
```

**CRITICAL: KHONG chay lenh ad-hoc cho tests, CI, Docker. LUON dung scripts.**

### Buoc 2: Cham diem 10 categories (100 diem)

#### 1. E2E Functionality (10 diem)
- E2E tests pass 100% (4)
- No cold start issues (2)
- Critical flows work end-to-end (2)
- External integrations work (2)

#### 2. Security (10 diem)
- Authentication properly implemented (3)
- Rate limiting active (2)
- No hardcoded secrets in code (2)
- CORS configured correctly (1)
- Input validation on all endpoints (2)

#### 3. Backend Tests (10 diem)
- All modules build + test pass (4)
- 0 skipped tests (2)
- Test coverage >70% (2)
- Integration tests for critical paths (2)

#### 4. Frontend Tests (10 diem)
- Unit tests pass with <10% skipped (3)
- Build passes all pages (2)
- Component tests for critical pages (3)
- E2E browser tests exist (2)

#### 5. CI/CD (10 diem)
- All CI workflows green on main (4)
- 0 stale branches (2)
- 0 inactive open PRs (2)
- Clean CI history (2)

#### 6. UI/UX (10 diem)
- Consistent design system (3)
- Theme/styling system works (2)
- Responsive design (2)
- User onboarding/guidance (2)
- Accessibility basics (1)

#### 7. DevOps/Infrastructure (10 diem)
- All containers healthy (3)
- Production deployment plan exists (2)
- Backup strategy documented (2)
- Monitoring/alerting (2)
- Secrets management documented (1)

#### 8. Documentation (10 diem)
- Business docs exist for all domains (3)
- Business docs match code (config keys, rules) (2)
- Architecture + guides up-to-date (3)
- Plans have completion tracking (2)

> Score 0 if no business docs exist for implemented domains.

#### 9. Code Quality (10 diem)
- 0 TODO/FIXME/HACK in production code (2)
- 0 IDE warnings (2)
- Consistent coding style (linter enforced) (2)
- No dead code / unused imports (2)
- Dependencies on latest stable versions (2)

#### 10. Project Management (10 diem)
- All plans have completion status (3)
- PRs follow team methodology (3)
- Commit messages clean + meaningful (2)
- Issues/gaps tracked and prioritized (2)

### Buoc 3: Output Report

Save to `documents/04-quality/quality-audit-[date].md`. Include:
- Score table: 10 categories x 10 points, total /100
- Grade: A+ (95-100), A (90-94), B+ (85-89), B (80-84), C (70-79), D (<70)
- Strengths (8+/10), Needs Improvement (5-7), Critical Issues (<5)
- Improvement Roadmap: Quick Wins / Medium / Major effort
- Action Items table: Priority, Item, Score Impact, Effort
- Comparison with previous audit if exists

## Rules

- LUON chay tests that (khong doan)
- Cham diem dua tren evidence (test output, code check), khong cam tinh
- Neu khong the chay test (Docker down, etc.), ghi 0 diem + note ly do
- **CRITICAL: CI phai hoan thanh truoc khi cham diem** — khong gia dinh pass/fail khi CI dang chay
